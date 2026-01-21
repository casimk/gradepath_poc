import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

export function OAuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState<'processing' | 'success' | 'error'>('processing');
  const [error, setError] = useState('');

  useEffect(() => {
    const handleOAuthCallback = async () => {
      const token = searchParams.get('token');

      if (token) {
        try {
          // Store the received token
          localStorage.setItem('access_token', token);
          setStatus('success');
          // Redirect to dashboard after a short delay
          setTimeout(() => {
            navigate('/', { replace: true });
          }, 1000);
        } catch (err) {
          setStatus('error');
          setError('Failed to process OAuth callback');
          setTimeout(() => {
            navigate('/login', { replace: true });
          }, 3000);
        }
      } else {
        setStatus('error');
        setError('No token received from OAuth provider');
        setTimeout(() => {
          navigate('/login', { replace: true });
        }, 3000);
      }
    };

    handleOAuthCallback();
  }, [searchParams, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="text-center">
        {status === 'processing' && (
          <>
            <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-indigo-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Processing your login...</p>
          </>
        )}
        {status === 'success' && (
          <>
            <div className="rounded-full bg-green-100 p-4 mx-auto w-16 h-16 flex items-center justify-center">
              <svg className="h-8 w-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <p className="mt-4 text-gray-600">Login successful! Redirecting...</p>
          </>
        )}
        {status === 'error' && (
          <>
            <div className="rounded-full bg-red-100 p-4 mx-auto w-16 h-16 flex items-center justify-center">
              <svg className="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <p className="mt-4 text-red-600">{error}</p>
            <p className="mt-2 text-gray-500">Redirecting to login page...</p>
          </>
        )}
      </div>
    </div>
  );
}
