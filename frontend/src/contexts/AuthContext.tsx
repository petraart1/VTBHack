import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { authApi, LoginRequest, RegisterRequest } from '../lib/api/auth';
import { apiClient } from '../lib/api';

interface User {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  signUp: (email: string, password: string, firstName: string, lastName: string, birthOfDate: string) => Promise<{ error: Error | null }>;
  signIn: (email: string, password: string) => Promise<{ error: Error | null }>;
  signInWithGitHub: () => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

// Функция для декодирования JWT токена (базовая версия)
function decodeJWT(token: string): any {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    return null;
  }
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Проверяем наличие токена в localStorage
    const token = apiClient.getToken();
    if (token) {
      try {
        const decoded = decodeJWT(token);
        if (decoded && decoded.sub) {
          // Извлекаем email из токена
          setUser({
            id: decoded.sub,
            email: decoded.email || decoded.sub,
            firstName: decoded.firstName,
            lastName: decoded.lastName,
          });
        }
      } catch (error) {
        console.error('Error decoding token:', error);
        apiClient.setToken(null);
      }
    }
    setLoading(false);
  }, []);

  const signUp = async (
    email: string,
    password: string,
    firstName: string,
    lastName: string,
    birthOfDate: string
  ): Promise<{ error: Error | null }> => {
    try {
      const registerData: RegisterRequest = {
        firstName,
        lastName,
        birthOfDate,
        email,
        password,
      };

      await authApi.register(registerData);
      return { error: null };
    } catch (error) {
      return { error: error as Error };
    }
  };

  const signIn = async (email: string, password: string): Promise<{ error: Error | null }> => {
    try {
      const loginData: LoginRequest = { email, password };
      const response = await authApi.login(loginData);

      if (response.token) {
        const decoded = decodeJWT(response.token);
        if (decoded && decoded.sub) {
          setUser({
            id: decoded.sub,
            email: decoded.email || email,
            firstName: decoded.firstName,
            lastName: decoded.lastName,
          });
        }
      }

      return { error: null };
    } catch (error) {
      return { error: error as Error };
    }
  };

  const signInWithGitHub = async (): Promise<void> => {
    await authApi.oauth2Authorize('github');
  };

  const signOut = async (): Promise<void> => {
    await authApi.logout();
    setUser(null);
  };

  const value = {
    user,
    loading,
    signUp,
    signIn,
    signInWithGitHub,
    signOut,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
