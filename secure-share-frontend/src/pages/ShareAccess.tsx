import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { QrCode } from 'lucide-react';
import { Card } from '@/components/ui/card';

const ShareAccess = () => {
  const { token } = useParams();
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showQr, setShowQr] = useState(false);
  const [fileInfo, setFileInfo] = useState<{
    filename: string;
    hasPassword: boolean;
    expiresAt?: string;
  } | null>(null);

  // Check share validity on component mount
 useEffect(() => {
   const checkShareValidity = async () => {
     try {
       const res = await fetch(`http://localhost:8080/api/v1/share/info/${token}`);
       if (res.ok) {
         const data = await res.json();
         setFileInfo({
           filename: data.filename,
           hasPassword: data.hasPassword,
           expiresAt: data.expiresAt
         });

         // If password is required, show the field immediately
         if (data.hasPassword) {
           setError('This file is password protected');
         }
       } else {
         const errorText = await res.text();
         setError(errorText || 'This share link is invalid or expired');
       }
     } catch (err) {
       setError('Failed to verify share link');
     }
   };

   if (token) {
     checkShareValidity();
   }
 }, [token]);

  const handleAccessFile = async () => {
    setIsLoading(true);
    setError('');
    try {
      const params = new URLSearchParams();
      if (password) params.append('password', password);

      // First check if password is required but not provided
      if (fileInfo?.hasPassword && !password) {
        setError('Password is required for this file');
        setIsLoading(false);
        return;
      }

      const res = await fetch(
        `http://localhost:8080/api/v1/share/access/${token}?${params.toString()}`
      );

      if (!res.ok) {
        const errorText = await res.text();
        if (res.status === 403) {
          setError('Invalid password or access denied');
        } else if (res.status === 401) {
          setError('Password is required for this file');
        } else {
          setError(errorText || 'Failed to access file');
        }
        return;
      }

      // Handle successful download...
      const blob = await res.blob();
      const contentDisposition = res.headers.get('Content-Disposition');
      let filename = 'downloaded_file';

      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
        if (filenameMatch && filenameMatch[1]) {
          filename = filenameMatch[1].replace(/['"]/g, '');
        }
      }

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = decodeURIComponent(filename);
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

  if (!token) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <Card className="p-6 w-full max-w-md">
          <Alert variant="destructive">
            <AlertDescription>
              Missing share token. Please check your link and try again.
            </AlertDescription>
          </Alert>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
      <Card className="p-6 w-full max-w-md">
        <h1 className="text-2xl font-bold mb-4">Access Shared File</h1>

        {fileInfo && (
          <div className="mb-4 space-y-2">
            <p className="font-medium">File: {fileInfo.filename}</p>
            {fileInfo.expiresAt && (
              <p className="text-sm text-muted-foreground">
                Expires: {new Date(fileInfo.expiresAt).toLocaleString()}
              </p>
            )}
          </div>
        )}

        {error && (
          <Alert variant="destructive" className="mb-4">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

       {(fileInfo?.hasPassword || error.includes('password') || error.includes('Password is required')) && (
         <div className="mb-4 space-y-2">
           <Input
             type="password"
             value={password}
             onChange={(e) => setPassword(e.target.value)}
             placeholder="Enter password"
             className="w-full"
           />
           <p className="text-sm text-muted-foreground">
             {error.includes('Invalid password')
               ? 'Incorrect password, please try again'
               : 'This file is password protected'}
           </p>
         </div>
       )}

        <div className="flex gap-2">
          <Button
            onClick={handleAccessFile}
            disabled={isLoading}
            className="flex-1"
          >
            {isLoading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Downloading...
              </>
            ) : (
              'Download File'
            )}
          </Button>

          <Button
            variant="outline"
            onClick={() => setShowQr(!showQr)}
            className="px-3"
          >
            <QrCode className="h-4 w-4" />
          </Button>
        </div>

        {showQr && (
          <div className="mt-4 flex flex-col items-center">
            <img
              src={`http://localhost:8080/api/v1/share/qr/${token}`}
              alt="QR Code"
              className="w-48 h-48 border rounded-lg"
            />
            <p className="mt-2 text-sm text-muted-foreground">
              Scan to access this file
            </p>
          </div>
        )}
      </Card>
    </div>
  );
};

export default ShareAccess;