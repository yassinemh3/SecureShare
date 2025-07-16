import React, { useEffect, useState } from 'react';
import FileUploadForm from '../components/FileUploadForm';
import FileList from '../components/FileList';
import StatusMessage from '../components/StatusMessage';
import { Toaster } from "@/components/ui/sonner";

interface HomeProps {
  onLogout: () => void;
  token: string;
}

interface FileMetadata {
  id: number;
  originalFilename: string;
  uploadedAt: string;
  size: number;
}

const Home: React.FC<HomeProps> = ({ onLogout, token }) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [message, setMessage] = useState('');
  const [files, setFiles] = useState<FileMetadata[]>([]);
  const [username, setUsername] = useState('');

  const fetchFiles = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/v1/files', {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (res.ok) {
        setFiles(await res.json());
      } else if (res.status === 401) {
        onLogout();
      } else {
        setMessage('Failed to fetch files.');
      }
    } catch {
      setMessage('Error loading files.');
    }
  };

  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/v1/auth/me', {
          method: 'GET',
          headers: { Authorization: `Bearer ${token}` }
        });

        if (res.ok) {
          const data = await res.json();
          setUsername(data.firstname || data.email || 'User');
        } else {
          setUsername('User');
        }
      } catch {
        setUsername('User');
      }

      fetchFiles();
    };

    fetchUserInfo();
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSelectedFile(e.target.files?.[0] || null);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage('Please select a file.');
      return;
    }

    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const res = await fetch('http://localhost:8080/api/v1/files/upload', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formData
      });

      const responseData = await res.json().catch(async () => ({
        message: await res.text()
      }));

      if (res.ok) {
        setMessage(responseData.message || 'Upload successful!');
        setSelectedFile(null);
        await fetchFiles();
      } else {
        setMessage(`Upload failed: ${responseData.message || 'Unknown error'}`);
      }
    } catch (err) {
      setMessage('Upload error: ' + (err as Error).message);
    }
  };

  const handleDownload = async (fileId: number, filename: string) => {
    try {
      const res = await fetch(`http://localhost:8080/api/v1/files/${fileId}/download`, {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (res.ok) {
        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        window.URL.revokeObjectURL(url);
      } else {
        throw new Error('Download failed.');
      }
    } catch {
      setMessage('Download error.');
    }
  };

  const handleDelete = async (fileId: number) => {
    try {
      const res = await fetch(`http://localhost:8080/api/v1/files/${fileId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      });

      if (res.status === 403) {
        setMessage('You are not authorized to delete this file.');
        return;
      }

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        setMessage(errorData.message || 'Failed to delete file.');
        return;
      }

      setMessage('File deleted successfully.');
      fetchFiles();
    } catch (error) {
      setMessage('Error deleting file: ' + (error as Error).message);
    }
  };

  const handleShare = async (fileId: number, data: { password?: string; expiryMinutes?: number }): Promise<string> => {
    try {
      const params = new URLSearchParams();
      if (data.password) params.append('password', data.password);
      if (data.expiryMinutes) params.append('expiryMinutes', data.expiryMinutes.toString());

      const res = await fetch(`http://localhost:8080/api/v1/share/${fileId}?${params.toString()}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      });

      if (!res.ok) {
        const errorText = await res.text();
        throw new Error(errorText || 'Failed to generate share link');
      }

      return await res.text();
    } catch (error) {
      setMessage('Error generating share link: ' + (error as Error).message);
      throw error;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Sticky Header */}
      <header className="sticky top-0 z-10 bg-white shadow p-4 flex justify-between items-center">
        <h2 className="text-lg font-semibold">Hi, {username}</h2>
        <button
          onClick={onLogout}
          className="bg-red-500 text-white py-1 px-4 rounded hover:bg-red-600 transition"
        >
          Logout
        </button>
      </header>

      <main className="flex flex-col items-center px-4 pt-6 pb-12">
        <div className="w-full max-w-2xl bg-white shadow-md rounded-lg p-6 space-y-6">
          <Toaster position="top-center" richColors />

          <h1 className="text-2xl font-bold text-center">SecureShare Dashboard</h1>

          <FileUploadForm
            onFileChange={handleFileChange}
            onUpload={handleUpload}
            selectedFile={selectedFile}
          />

          <StatusMessage
            message={message}
            type={message.includes('success') || message.includes('deleted') ? 'success' : 'error'}
          />

          <FileList
            files={files}
            onDownload={handleDownload}
            onDelete={handleDelete}
            onShare={handleShare}
          />
        </div>
      </main>
    </div>
  );
};

export default Home;
