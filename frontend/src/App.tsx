import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { ErrorBoundary } from './components/ErrorBoundary';
import { LoadingSpinner } from './components/LoadingSpinner';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { UserRole } from './types/auth';
import { AppLayout } from './components/AppLayout';
import './App.css';

// Lazy load pages for better performance
const Dashboard = lazy(() => import('./pages/Dashboard').then((m) => ({ default: m.Dashboard })));
const Profile = lazy(() => import('./pages/Profile').then((m) => ({ default: m.Profile })));
const ReadingList = lazy(() => import('./pages/ReadingList').then((m) => ({ default: m.ReadingList })));
const ReadingEntryForm = lazy(() => import('./pages/ReadingEntryForm').then((m) => ({ default: m.ReadingEntryForm })));
const Shelf = lazy(() => import('./pages/Shelf').then((m) => ({ default: m.Shelf })));
const ShelfItemForm = lazy(() => import('./pages/ShelfItemForm').then((m) => ({ default: m.ShelfItemForm })));
const ShelfItemDetail = lazy(() => import('./pages/ShelfItemDetail').then((m) => ({ default: m.ShelfItemDetail })));
const Loans = lazy(() => import('./pages/Loans').then((m) => ({ default: m.Loans })));
const BookDetail = lazy(() => import('./pages/BookDetail').then((m) => ({ default: m.BookDetail })));
const AdminUsers = lazy(() => import('./pages/admin/Users').then((m) => ({ default: m.AdminUsers })));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AuthProvider>
            <Toaster
              position="top-right"
              toastOptions={{
                duration: 4000,
                style: { background: '#333', color: '#fff' },
                success: { duration: 3000, iconTheme: { primary: '#10b981', secondary: '#fff' } },
                error: { duration: 5000, iconTheme: { primary: '#ef4444', secondary: '#fff' } },
              }}
            />
            <Suspense fallback={<LoadingSpinner fullScreen />}>
              <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route
                  element={
                    <ProtectedRoute>
                      <AppLayout />
                    </ProtectedRoute>
                  }
                >
                  <Route path="/" element={<Dashboard />} />
                  <Route path="/read" element={<ReadingList />} />
                  <Route path="/read/new" element={<ReadingEntryForm />} />
                  <Route path="/read/:id/edit" element={<ReadingEntryForm />} />
                  <Route path="/shelf" element={<Shelf />} />
                  <Route path="/shelf/new" element={<ShelfItemForm />} />
                  <Route path="/shelf/:id" element={<ShelfItemDetail />} />
                  <Route path="/loans" element={<Loans />} />
                  <Route path="/books/:id" element={<BookDetail />} />
                  <Route path="/profile" element={<Profile />} />
                  <Route
                    path="/admin/users"
                    element={
                      <ProtectedRoute requireRole={UserRole.ADMIN}>
                        <AdminUsers />
                      </ProtectedRoute>
                    }
                  />
                </Route>
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Suspense>
          </AuthProvider>
        </BrowserRouter>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
