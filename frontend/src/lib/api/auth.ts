// API для работы с аутентификацией

import { apiClient } from '../api';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  birthOfDate: string; // YYYY-MM-DD
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface RegisterResponse {
  message?: string;
}

export const authApi = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    // nginx: /api/v1/auth/ -> auth_service/
    // контроллер: @RequestMapping("/auth")
    // запрос /api/v1/auth/auth/login -> auth_service/auth/login ✓
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/auth/login', credentials);
    if (response.token) {
      apiClient.setToken(response.token);
    }
    return response;
  },

  async register(data: RegisterRequest): Promise<RegisterResponse> {
    return apiClient.post<RegisterResponse>('/api/v1/auth/auth/register', data);
  },

  async logout(): Promise<void> {
    apiClient.setToken(null);
  },

  async oauth2Authorize(provider: string): Promise<void> {
    window.location.href = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:80'}/api/v1/auth/auth/oauth2/authorize/${provider}`;
  },
};

