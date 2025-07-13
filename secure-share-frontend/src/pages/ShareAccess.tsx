import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

const ShareAccess = () => {
  const { token } = useParams();
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

 const handleAccessFile = async () => {
   setIsLoading(true);
   setError(''); // Clear previous errors
   try {
     const params = new URLSearchParams();
     if (password) params.append('password', password);

    // Verify the token format before making the request
     if (!token || token.split('-').length !== 5) {
       throw new Error('Invalid share token format. Please check the share link.');
     }

     const res = await fetch(`http://localhost:8080/api/v1/share/access/${token}?${params.toString()}`);

     if (!res.ok) {
       const errorText = await res.text();
       console.error('Access file error:', {
         status: res.status,
         errorText,
         token,
         hasPassword: !!password
       });
       throw new Error(errorText || 'Failed to access file');
     }

     const blob = await res.blob();
     const contentDisposition = res.headers.get('Content-Disposition');
     const filename = contentDisposition?.split('filename=')[1] || 'downloaded_file';

     // Create download link
     const url = window.URL.createObjectURL(blob);
     const a = document.createElement('a');
     a.href = url;
     a.download = filename;
     document.body.appendChild(a);
     a.click();
     window.URL.revokeObjectURL(url);
     a.remove();

   } catch (err) {
     setError((err as Error).message);
   } finally {
     setIsLoading(false);
   }
 };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
        <h1 className="text-2xl font-bold mb-6">Access Shared File</h1>

        {error && (
          <div className="mb-4 p-3 bg-red-100 text-red-700 rounded">
            {error}
          </div>
        )}

        <div className="mb-4">
          <label className="block text-sm font-medium mb-2">
            Password (if required)
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-3 py-2 border rounded"
          />
        </div>

        <button
          onClick={handleAccessFile}
          disabled={isLoading}
          className={`w-full py-2 px-4 rounded text-white ${
            isLoading ? 'bg-blue-400' : 'bg-blue-600 hover:bg-blue-700'
          }`}
        >
          {isLoading ? 'Downloading...' : 'Download File'}
        </button>
      </div>
    </div>
  );
};

export default ShareAccess;