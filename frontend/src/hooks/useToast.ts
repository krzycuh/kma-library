import toast from 'react-hot-toast';

/**
 * Custom hook for toast notifications
 * Provides consistent toast styling and easy-to-use methods
 */
export function useToast() {
  return {
    success: (message: string) => {
      toast.success(message, {
        duration: 3000,
        position: 'top-right',
      });
    },
    
    error: (message: string) => {
      toast.error(message, {
        duration: 5000,
        position: 'top-right',
      });
    },
    
    info: (message: string) => {
      toast(message, {
        duration: 4000,
        position: 'top-right',
        icon: 'ℹ️',
      });
    },
    
    loading: (message: string) => {
      return toast.loading(message, {
        position: 'top-right',
      });
    },
    
    dismiss: (toastId?: string) => {
      toast.dismiss(toastId);
    },
  };
}
