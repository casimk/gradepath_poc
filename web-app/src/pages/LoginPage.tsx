import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, initiateOAuth } = useAuth();

  const [formData, setFormData] = useState({
    identifier: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const from = (location.state as any)?.from?.pathname || '/';

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const isEmail = formData.identifier.includes('@');
      await login({
        [isEmail ? 'email' : 'username']: formData.identifier,
        password: formData.password,
      });
      navigate(from, { replace: true });
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleOAuthLogin = (provider: 'google' | 'github' | 'apple' | 'facebook') => {
    initiateOAuth(provider);
  };

  return (
    <div className="auth-container">
      {/* Decorative grid pattern overlay */}
      <div className="auth-grid-overlay" aria-hidden="true">
        <div className="auth-grid-line auth-grid-line-1" />
        <div className="auth-grid-line auth-grid-line-2" />
        <div className="auth-grid-line auth-grid-line-3" />
      </div>

      {/* Side panel - brand */}
      <div className="auth-brand-panel">
        <div className="auth-brand-content">
          <div className="auth-logo">
            <svg viewBox="0 0 60 60" fill="none" xmlns="http://www.w3.org/2000/svg" className="auth-logo-icon">
              <path d="M30 5L55 20V40L30 55L5 40V20L30 5Z" fill="currentColor" className="auth-logo-fill"/>
              <path d="M30 15L45 25V35L30 45L15 35V25L30 15Z" fill="#FAF8F5"/>
              <circle cx="30" cy="30" r="8" fill="currentColor" className="auth-logo-accent"/>
            </svg>
            <span className="auth-logo-text">GradePath</span>
          </div>

          <div className="auth-brand-message">
            <h1 className="auth-brand-title">Your Learning Journey, Charted.</h1>
            <p className="auth-brand-subtitle">
              Track progress, discover insights, and achieve your academic goals with intelligent analytics designed for modern learners.
            </p>
          </div>

          <div className="auth-brand-stats">
            <div className="auth-stat">
              <span className="auth-stat-number">10K+</span>
              <span className="auth-stat-label">Active Students</span>
            </div>
            <div className="auth-stat-divider" />
            <div className="auth-stat">
              <span className="auth-stat-number">95%</span>
              <span className="auth-stat-label">Success Rate</span>
            </div>
            <div className="auth-stat-divider" />
            <div className="auth-stat">
              <span className="auth-stat-number">24/7</span>
              <span className="auth-stat-label">Progress Tracking</span>
            </div>
          </div>
        </div>
      </div>

      {/* Side panel - form */}
      <div className="auth-form-panel">
        <div className="auth-form-wrapper">
          {/* Form header */}
          <div className="auth-form-header">
            <h2 className="auth-form-title">Welcome Back</h2>
            <p className="auth-form-subtitle">
              New to GradePath?{' '}
              <Link to="/signup" className="auth-form-link">
                Create an account
              </Link>
            </p>
          </div>

          {/* OAuth buttons */}
          <div className="auth-oauth-section">
            <button
              type="button"
              onClick={() => handleOAuthLogin('google')}
              className="auth-oauth-button"
              aria-label="Continue with Google"
            >
              <svg className="auth-oauth-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              <span className="auth-oauth-text">Google</span>
            </button>

            <button
              type="button"
              onClick={() => handleOAuthLogin('github')}
              className="auth-oauth-button"
              aria-label="Continue with GitHub"
            >
              <svg className="auth-oauth-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M12 0C5.374 0 0 5.373 0 12c0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23A11.509 11.509 0 0112 5.803c1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576C20.566 21.797 24 17.3 24 12c0-6.627-5.373-12-12-12z"/>
              </svg>
              <span className="auth-oauth-text">GitHub</span>
            </button>

            <button
              type="button"
              onClick={() => handleOAuthLogin('apple')}
              className="auth-oauth-button"
              aria-label="Continue with Apple"
            >
              <svg className="auth-oauth-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
              </svg>
              <span className="auth-oauth-text">Apple</span>
            </button>
          </div>

          {/* Divider */}
          <div className="auth-divider">
            <span className="auth-divider-text">or continue with email</span>
          </div>

          {/* Login form */}
          <form className="auth-form" onSubmit={handleSubmit}>
            {error && (
              <div className="auth-error" role="alert">
                <svg className="auth-error-icon" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd"/>
                </svg>
                <span className="auth-error-text">{error}</span>
              </div>
            )}

            <div className="auth-field-group">
              <label htmlFor="identifier" className="auth-field-label">
                Email or Username
              </label>
              <input
                id="identifier"
                name="identifier"
                type="text"
                required
                className="auth-field-input"
                placeholder="Enter your email or username"
                value={formData.identifier}
                onChange={(e) => setFormData({ ...formData, identifier: e.target.value })}
              />
            </div>

            <div className="auth-field-group">
              <label htmlFor="password" className="auth-field-label">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                className="auth-field-input"
                placeholder="Enter your password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              />
            </div>

            <div className="auth-form-actions">
              <Link to="/forgot-password" className="auth-forgot-link">
                Forgot password?
              </Link>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="auth-submit-button"
            >
              {isLoading ? (
                <>
                  <svg className="auth-spinner" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" className="auth-spinner-track"/>
                    <path d="M12 2C6.477 2 2 6.477 2 12" stroke="currentColor" strokeWidth="4" strokeLinecap="round" className="auth-spinner-path"/>
                  </svg>
                  Signing in...
                </>
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          {/* Footer */}
          <div className="auth-footer">
            <p className="auth-footer-text">
              By continuing, you agree to our{' '}
              <Link to="/terms" className="auth-footer-link">Terms of Service</Link>
              {' '}and{' '}
              <Link to="/privacy" className="auth-footer-link">Privacy Policy</Link>
            </p>
          </div>
        </div>
      </div>

      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Crimson+Pro:wght@400;500;600;700&family=Inter:wght@400;500;600;700&display=swap');

        .auth-container {
          min-height: 100vh;
          display: flex;
          font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
          background: #FAF8F5;
          position: relative;
          overflow: hidden;
        }

        /* Grid overlay pattern */
        .auth-grid-overlay {
          position: absolute;
          inset: 0;
          pointer-events: none;
          opacity: 0.4;
        }

        .auth-grid-line {
          position: absolute;
          background: linear-gradient(180deg, transparent 0%, rgba(26, 54, 93, 0.03) 50%, transparent 100%);
        }

        .auth-grid-line-1 {
          left: 15%;
          width: 1px;
          height: 100%;
        }

        .auth-grid-line-2 {
          left: 50%;
          width: 1px;
          height: 100%;
        }

        .auth-grid-line-3 {
          left: 85%;
          width: 1px;
          height: 100%;
        }

        /* Brand panel - left side */
        .auth-brand-panel {
          flex: 1;
          background: linear-gradient(135deg, #1A365D 0%, #0F2744 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 3rem;
          position: relative;
          overflow: hidden;
        }

        .auth-brand-panel::before {
          content: '';
          position: absolute;
          inset: 0;
          background: url("data:image/svg+xml,%3Csvg viewBox='0 0 400 400' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
          opacity: 0.03;
          pointer-events: none;
        }

        .auth-brand-content {
          position: relative;
          max-width: 480px;
          width: 100%;
        }

        .auth-logo {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          margin-bottom: 4rem;
        }

        .auth-logo-icon {
          width: 48px;
          height: 48px;
          color: #C4A962;
        }

        .auth-logo-fill {
          color: #C4A962;
          opacity: 0.9;
        }

        .auth-logo-accent {
          color: #C4A962;
        }

        .auth-logo-text {
          font-family: 'Crimson Pro', Georgia, serif;
          font-size: 1.75rem;
          font-weight: 600;
          color: #FAF8F5;
          letter-spacing: 0.02em;
        }

        .auth-brand-message {
          margin-bottom: 3rem;
        }

        .auth-brand-title {
          font-family: 'Crimson Pro', Georgia, serif;
          font-size: 2.75rem;
          font-weight: 600;
          line-height: 1.2;
          color: #FAF8F5;
          margin-bottom: 1.25rem;
          letter-spacing: -0.01em;
        }

        .auth-brand-subtitle {
          font-size: 1rem;
          line-height: 1.7;
          color: rgba(250, 248, 245, 0.75);
        }

        .auth-brand-stats {
          display: flex;
          align-items: center;
          gap: 2rem;
          padding-top: 2rem;
          border-top: 1px solid rgba(250, 248, 245, 0.1);
        }

        .auth-stat {
          display: flex;
          flex-direction: column;
          gap: 0.25rem;
        }

        .auth-stat-number {
          font-family: 'Crimson Pro', Georgia, serif;
          font-size: 1.5rem;
          font-weight: 600;
          color: #C4A962;
        }

        .auth-stat-label {
          font-size: 0.8125rem;
          color: rgba(250, 248, 245, 0.65);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }

        .auth-stat-divider {
          width: 1px;
          height: 2.5rem;
          background: rgba(250, 248, 245, 0.1);
        }

        /* Form panel - right side */
        .auth-form-panel {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 2rem;
        }

        .auth-form-wrapper {
          width: 100%;
          max-width: 440px;
        }

        .auth-form-header {
          margin-bottom: 2rem;
          text-align: center;
        }

        .auth-form-title {
          font-family: 'Crimson Pro', Georgia, serif;
          font-size: 2.25rem;
          font-weight: 600;
          color: #1A365D;
          margin-bottom: 0.75rem;
          letter-spacing: -0.01em;
        }

        .auth-form-subtitle {
          font-size: 0.9375rem;
          color: #5A6B7C;
        }

        .auth-form-link {
          color: #1A365D;
          font-weight: 600;
          text-decoration: none;
          position: relative;
          transition: color 0.2s ease;
        }

        .auth-form-link:hover {
          color: #0F2744;
        }

        .auth-form-link::after {
          content: '';
          position: absolute;
          bottom: -2px;
          left: 0;
          width: 100%;
          height: 1px;
          background: currentColor;
          opacity: 0;
          transition: opacity 0.2s ease;
        }

        .auth-form-link:hover::after {
          opacity: 1;
        }

        /* OAuth buttons */
        .auth-oauth-section {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 0.75rem;
          margin-bottom: 1.5rem;
        }

        .auth-oauth-button {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 0.5rem;
          padding: 1rem 0.75rem;
          background: #FFFFFF;
          border: 1px solid #E8E4DD;
          border-radius: 8px;
          cursor: pointer;
          transition: all 0.2s ease;
        }

        .auth-oauth-button:hover {
          border-color: #C4A962;
          box-shadow: 0 4px 12px rgba(196, 169, 98, 0.15);
          transform: translateY(-1px);
        }

        .auth-oauth-icon {
          width: 24px;
          height: 24px;
          color: #1A365D;
        }

        .auth-oauth-text {
          font-size: 0.75rem;
          font-weight: 500;
          color: #5A6B7C;
        }

        /* Divider */
        .auth-divider {
          position: relative;
          text-align: center;
          margin: 1.5rem 0;
        }

        .auth-divider::before {
          content: '';
          position: absolute;
          top: 50%;
          left: 0;
          right: 0;
          height: 1px;
          background: #E8E4DD;
        }

        .auth-divider-text {
          position: relative;
          display: inline-block;
          padding: 0 1rem;
          background: #FAF8F5;
          font-size: 0.8125rem;
          color: #8B95A5;
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }

        /* Form fields */
        .auth-form {
          display: flex;
          flex-direction: column;
          gap: 1.25rem;
        }

        .auth-field-group {
          display: flex;
          flex-direction: column;
          gap: 0.5rem;
        }

        .auth-field-label {
          font-size: 0.875rem;
          font-weight: 500;
          color: #1A365D;
        }

        .auth-field-input {
          padding: 0.875rem 1rem;
          font-size: 0.9375rem;
          font-family: inherit;
          background: #FFFFFF;
          border: 1px solid #D4CEC2;
          border-radius: 6px;
          color: #1A365D;
          transition: all 0.2s ease;
        }

        .auth-field-input::placeholder {
          color: #AAB4BE;
        }

        .auth-field-input:focus {
          outline: none;
          border-color: #C4A962;
          box-shadow: 0 0 0 3px rgba(196, 169, 98, 0.15);
        }

        /* Error state */
        .auth-error {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          padding: 0.875rem 1rem;
          background: #FEF3F2;
          border: 1px solid #FEB5B5;
          border-radius: 6px;
          color: #B91C1C;
        }

        .auth-error-icon {
          width: 18px;
          height: 18px;
          flex-shrink: 0;
        }

        .auth-error-text {
          font-size: 0.875rem;
        }

        /* Form actions */
        .auth-form-actions {
          display: flex;
          justify-content: flex-end;
        }

        .auth-forgot-link {
          font-size: 0.8125rem;
          color: #5A6B7C;
          text-decoration: none;
          transition: color 0.2s ease;
        }

        .auth-forgot-link:hover {
          color: #1A365D;
        }

        /* Submit button */
        .auth-submit-button {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 0.5rem;
          padding: 0.875rem 1.5rem;
          font-size: 0.9375rem;
          font-weight: 600;
          font-family: inherit;
          background: #1A365D;
          color: #FAF8F5;
          border: none;
          border-radius: 6px;
          cursor: pointer;
          transition: all 0.2s ease;
          margin-top: 0.5rem;
        }

        .auth-submit-button:hover:not(:disabled) {
          background: #0F2744;
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(26, 54, 93, 0.25);
        }

        .auth-submit-button:disabled {
          opacity: 0.7;
          cursor: not-allowed;
        }

        /* Spinner animation */
        .auth-spinner {
          width: 18px;
          height: 18px;
          animation: spin 0.8s linear infinite;
        }

        .auth-spinner-track {
          color: rgba(250, 248, 245, 0.3);
        }

        .auth-spinner-path {
          color: #FAF8F5;
        }

        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }

        /* Footer */
        .auth-footer {
          margin-top: 2rem;
          text-align: center;
        }

        .auth-footer-text {
          font-size: 0.75rem;
          color: #8B95A5;
        }

        .auth-footer-link {
          color: #5A6B7C;
          text-decoration: none;
          transition: color 0.2s ease;
        }

        .auth-footer-link:hover {
          color: #1A365D;
        }

        /* Responsive */
        @media (max-width: 1024px) {
          .auth-brand-panel {
            display: none;
          }
        }

        @media (max-width: 640px) {
          .auth-form-panel {
            padding: 1.5rem;
          }

          .auth-form-title {
            font-size: 1.75rem;
          }

          .auth-oauth-section {
            grid-template-columns: repeat(3, 1fr);
          }
        }
      `}</style>
    </div>
  );
}
