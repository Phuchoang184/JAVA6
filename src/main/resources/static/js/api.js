/* =========================================================
   api.js – Global Axios Instance with JWT Interceptor
   Shared across all Vue modules (cart, checkout, auth, etc.)
   ========================================================= */

(function () {
  'use strict';

  // ── Constants ─────────────────────────────────────────
  const TOKEN_KEY = 'leika_token';
  const USER_KEY  = 'leika_user';

  // ── Axios Instance ────────────────────────────────────
  const api = axios.create({
    baseURL: '/api',
    headers: { 'Content-Type': 'application/json' },
    timeout: 15000
  });

  // ── Request Interceptor: attach JWT ───────────────────
  api.interceptors.request.use(
    (config) => {
      const token = localStorage.getItem(TOKEN_KEY);
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  // ── Response Interceptor: unwrap ApiResponse & 401 ────
  api.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response) {
        const status = error.response.status;

        // Token expired or invalid → redirect to login
        if (status === 401) {
          AuthManager.logout();
          window.location.href = '/login';
        }

        // Forbidden
        if (status === 403) {
          console.warn('[API] Forbidden – insufficient permissions');
        }
      }
      return Promise.reject(error);
    }
  );

  // ── Auth Manager ──────────────────────────────────────
  const AuthManager = {
    /** Login and store credentials */
    async login(email, password) {
      const { data } = await api.post('/auth/login', { email, password });
      this._saveAuth(data);
      return data;
    },

    /** Register and store credentials */
    async register(payload) {
      const { data } = await api.post('/auth/register', payload);
      this._saveAuth(data);
      return data;
    },

    /** Clear local auth data */
    logout() {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      window.dispatchEvent(new CustomEvent('auth:changed'));
    },

    /** Check logged in */
    isAuthenticated() {
      return !!localStorage.getItem(TOKEN_KEY);
    },

    /** Get stored token */
    getToken() {
      return localStorage.getItem(TOKEN_KEY);
    },

    /** Get stored user profile */
    getUser() {
      try {
        return JSON.parse(localStorage.getItem(USER_KEY));
      } catch {
        return null;
      }
    },

    /** Internal: save auth response */
    _saveAuth(authResponse) {
      if (authResponse && authResponse.accessToken) {
        let existingUser = {};
        try {
          existingUser = JSON.parse(localStorage.getItem(USER_KEY) || '{}');
        } catch {
          existingUser = {};
        }

        localStorage.setItem(TOKEN_KEY, authResponse.accessToken);
        localStorage.setItem(USER_KEY, JSON.stringify({
          email: authResponse.email,
          fullName: authResponse.fullName,
          phoneNumber: authResponse.phoneNumber || existingUser.phoneNumber || '',
          role: authResponse.role
        }));
        window.dispatchEvent(new CustomEvent('auth:changed'));
      }
    }
  };

  // ── Expose globally ───────────────────────────────────
  window.LeikaAPI = api;
  window.AuthManager = AuthManager;
})();
