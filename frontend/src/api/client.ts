import axios from 'axios';
import { readViewedUser, sendsViewAsHeader, VIEW_AS_ENDED_CODES, VIEW_AS_HEADER, writeViewedUser } from '../utils/viewAs';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor to attach JWT token and, for an admin previewing another account, the view-as header
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    const viewedUser = readViewedUser();
    if (viewedUser && sendsViewAsHeader(config.url)) {
      config.headers[VIEW_AS_HEADER] = viewedUser.id;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // The previewed account is gone (or this session may not preview): end the preview and start over
    if (VIEW_AS_ENDED_CODES.has(error.response?.data?.code) && readViewedUser()) {
      writeViewedUser(null);
      window.location.href = '/admin/users';
      return Promise.reject(error);
    }

    // If 401 and we haven't already retried, attempt token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }

        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
          refreshToken,
        });

        const { accessToken } = response.data;
        localStorage.setItem('accessToken', accessToken);

        // Retry original request with new token
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh failed - clear tokens (and any admin preview) and redirect to login
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        writeViewedUser(null);
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
