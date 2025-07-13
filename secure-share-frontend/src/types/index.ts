// Types used across multiple features
export interface User {
  id: string;
  firstname: string;
  lastname: string;
  email: string;
}

// API response wrapper
export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}