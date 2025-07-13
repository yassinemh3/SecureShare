import React, { useEffect, useState } from 'react';
import FileUploadForm from '../components/FileUploadForm';
import FileList from '../components/FileList';
import StatusMessage from '../components/StatusMessage';

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

  useEffect(() => { fetchFiles(); }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSelectedFile(e.target.files?.[0] || null);
  };

    const handleUpload = async () => {
      if (!selectedFile) {
        setMessage('Please select a file.');
        return;
      }

      if (!token) {
        setMessage('Authentication token is missing.');
        return;
      }

      const formData = new FormData();
      formData.append('file', selectedFile);

      try {
        const res = await fetch('http://localhost:8080/api/v1/files/upload', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`
          },
          body: formData
        });

        if (res.ok) {
          // Try to parse as JSON, fall back to text if needed
          let responseData;
          try {
            responseData = await res.json();
          } catch (e) {
            responseData = { message: await res.text() };
          }

          setMessage(responseData.message || 'Upload successful!');
          setSelectedFile(null);
          await fetchFiles(); // Refresh list
        } else {
          // Handle error responses
          let errorMessage;
          try {
            const errorData = await res.json();
            errorMessage = errorData.message || res.statusText;
          } catch {
            errorMessage = await res.text();
          }
          setMessage(`Upload failed: ${errorMessage || 'Unknown error'}`);
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

      if (res.ok) {
        setMessage('File deleted.');
        await fetchFiles();
      } else {
        setMessage('Failed to delete.');
      }
    } catch {
      setMessage('Delete error.');
    }
  };


const handleShare = async (fileId: number, data: { password?: string; expiryMinutes?: number }): Promise<string> => {
  try {
    const params = new URLSearchParams();
    if (data.password) params.append('password', data.password);
    if (data.expiryMinutes) params.append('expiryMinutes', data.expiryMinutes.toString());

    const res = await fetch(`http://localhost:8080/api/v1/share/${fileId}?${params.toString()}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`  // Using the correct token prop
      }
    });

    if (!res.ok) {
      const errorText = await res.text();
      throw new Error(errorText || 'Failed to generate share link');
    }

    // Get the plain text token from response
    const shareToken = await res.text();

    // Construct the full shareable URL
     return `${window.location.origin}/share/access/${token}`;
  } catch (error) {
    setMessage('Error generating share link: ' + (error as Error).message);
    throw error;
  }
};


  return (
    <div className="min-h-screen bg-gray-100 flex flex-col items-center py-10">
      <div className="bg-white shadow-md rounded-lg p-6 w-full max-w-2xl">
        <h1 className="text-2xl font-semibold text-center mb-4">SecureShare Dashboard</h1>

        <FileUploadForm
          onFileChange={handleFileChange}
          onUpload={handleUpload}
          selectedFile={selectedFile}
        />

        <StatusMessage message={message} type={
          message.includes('success') || message.includes('deleted') ? 'success' : 'error'
        } />

        <FileList
          files={files}
          onDownload={handleDownload}
          onDelete={handleDelete}
          onShare={handleShare}
        />

        <button
          onClick={onLogout}
          className="mt-6 w-full bg-gray-500 text-white py-2 rounded hover:bg-gray-600 transition"
        >
          Logout
        </button>
      </div>
    </div>
  );
};

export default Home;