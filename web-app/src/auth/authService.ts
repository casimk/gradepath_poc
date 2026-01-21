import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';

export interface User {
  id: string;
  email: string;
  username?: string;
  displayName?: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  sessionId?: string;
  csrfToken?: string;
}

export interface LoginCredentials {
  username?: string;
  email?: string;
  password: string;
}

export interface RegisterData {
  username: string;
  email: string;
  password: string;
  displayName?: string;
}

/**
 * Cookie helper functions for parsing browser cookies
 */
class CookieHelper {
  static getCookie(name: string): string | null {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop()?.split(';').shift() || null;
    }
    return null;
  }

  static setCookie(name: string, value: string, maxAge: number): void {
    document.cookie = `${name}=${value}; max-age=${maxAge}; path=/; sameSite=strict`;
  }

  static deleteCookie(name: string): void {
    document.cookie = `${name}=; max-age=0; path=/; sameSite=strict`;
  }
}

class AuthService {
  private client: AxiosInstance;
  private refreshPromise: Promise<AuthResponse> | null = null;
  // Store sessionId and csrfToken in memory (cleared on page refresh)
  private sessionId: string | null = null;
  private csrfToken: string | null = null;

  constructor() {
    this.client = axios.create({
      baseURL: `${API_URL}/auth`,
      headers: {
        'Content-Type': 'application/json',
      },
      // Enable credentials to send cookies with requests
      withCredentials: true,
    });

    // Request interceptor to add CSRF tokens and Authorization header
    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        // For state-changing requests, add CSRF headers
        const method = config.method?.toUpperCase();
        if (method === 'POST' || method === 'PUT' || method === 'DELETE' || method === 'PATCH') {
          if (this.csrfToken && config.headers) {
            config.headers['X-CSRF-Token'] = this.csrfToken;
          }
          if (this.sessionId && config.headers) {
            config.headers['X-Session-Id'] = this.sessionId;
          }
        }

        // Try to get access token from cookie or use in-memory token
        const token = this.getAccessToken();
        if (token && config.headers) {
          config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor to handle token refresh
    this.client.interceptors.response.use(
      (response: AxiosResponse) => response,
      async (error) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          // If already refreshing, wait for that to complete
          if (this.refreshPromise) {
            try {
              await this.refreshPromise;
              return this.client(originalRequest);
            } catch {
              // Refresh failed, logout will be triggered
              return Promise.reject(error);
            }
          }

          // Start token refresh
          this.refreshPromise = this.refreshTokens();

          try {
            await this.refreshPromise;
            return this.client(originalRequest);
          } catch (refreshError) {
            // Refresh failed, clear state and redirect
            this.clearAuthState();
            window.location.href = '/login';
            return Promise.reject(refreshError);
          } finally {
            this.refreshPromise = null;
          }
        }

        return Promise.reject(error);
      }
    );
  }

  /**
   * Get access token from cookie (for Authorization header)
   * Note: Cookies are automatically sent by browser due to withCredentials=true
   * This method is primarily for setting the Authorization header explicitly
   */
  private getAccessToken(): string | null {
    // Try to get from in-memory state first
    // Otherwise, check if there's a token in cookies (for API calls)
    return CookieHelper.getCookie('access_token');
  }

  /**
   * Store session and CSRF tokens in memory
   */
  private setSessionData(sessionId?: string, csrfToken?: string): void {
    if (sessionId) {
      this.sessionId = sessionId;
    }
    if (csrfToken) {
      this.csrfToken = csrfToken;
    }
  }

  /**
   * Clear all authentication state (memory only - cookies are httpOnly)
   */
  private clearAuthState(): void {
    this.sessionId = null;
    this.csrfToken = null;
    // Cookies are httpOnly and will be cleared by server on logout
  }

  /**
   * Check if user has valid authentication
   */
  isAuthenticated(): boolean {
    // Check if we have session data and a cookie
    const hasCookie = CookieHelper.getCookie('access_token') !== null;
    const hasSession = this.sessionId !== null || CookieHelper.getCookie('csrf_session') !== null;
    return hasCookie && hasSession;
  }

  /**
   * Initialize session from cookies on page load
   * Call this on app startup to restore session from cookies
   */
  initializeFromCookies(): void {
    const sessionCookie = CookieHelper.getCookie('csrf_session');
    const tokenCookie = CookieHelper.getCookie('csrf_token');

    if (sessionCookie) {
      this.sessionId = sessionCookie;
    }
    if (tokenCookie) {
      this.csrfToken = tokenCookie;
    }
  }

  // Auth methods
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await this.client.post<AuthResponse>('/login', credentials);
    // Store sessionId and csrfToken in memory
    this.setSessionData(response.data.sessionId, response.data.csrfToken);
    return response.data;
  }

  async register(data: RegisterData): Promise<AuthResponse> {
    const response = await this.client.post<AuthResponse>('/register', data);
    // Store sessionId and csrfToken in memory
    this.setSessionData(response.data.sessionId, response.data.csrfToken);
    return response.data;
  }

  async refreshTokens(): Promise<AuthResponse> {
    const response = await this.client.post<AuthResponse>('/refresh', {});
    // Update sessionId and csrfToken on refresh (token rotation)
    this.setSessionData(response.data.sessionId, response.data.csrfToken);
    return response.data;
  }

  async logout(): Promise<void> {
    try {
      await this.client.post('/logout', {});
    } finally {
      this.clearAuthState();
    }
  }

  async getCurrentUser(): Promise<User> {
    const response = await this.client.get<User>('/me');
    return response.data;
  }

  /**
   * Get a new CSRF token for authenticated users
   */
  async getCsrfToken(): Promise<{ token: string; sessionId: string }> {
    const response = await this.client.get<{ token: string; sessionId: string }>('/csrf-token');
    this.setSessionData(response.data.sessionId, response.data.token);
    return response.data;
  }

  // OAuth methods
  async initiateOAuth(provider: 'google' | 'github' | 'apple' | 'facebook'): Promise<void> {
    const redirectUri = `${window.location.origin}/auth/callback/${provider}`;
    const authUrl = `${API_URL}/auth/oauth/${provider}?redirect_uri=${encodeURIComponent(redirectUri)}`;
    window.location.href = authUrl;
  }

  // Generic HTTP methods
  get(url: string, config?: InternalAxiosRequestConfig) {
    return this.client.get(url, config);
  }

  post(url: string, data?: any, config?: InternalAxiosRequestConfig) {
    return this.client.post(url, data, config);
  }

  put(url: string, data?: any, config?: InternalAxiosRequestConfig) {
    return this.client.put(url, data, config);
  }

  delete(url: string, config?: InternalAxiosRequestConfig) {
    return this.client.delete(url, config);
  }
}

export const authService = new AuthService();
