import React, { useEffect, useState } from 'react';
import FileUploadForm from '../components/FileUploadForm';
import FileList from '../components/FileList';
import StatusMessage from '../components/StatusMessage';
import { Toaster } from "@/components/ui/sonner";
import { encryptFile } from "../lib/zkeEncrypt";
import { decryptFile } from "../lib/zkeDecrypt";
import DecryptModal from "../components/DecryptModal";

interface HomeProps {
  onLogout: () => void;
  token: string;
}

interface SharedLink {
  id: string;
  fileId: number;
  filename: string;
  url: string;
  expiresAt?: string;
  hasPassword: boolean;
}

interface FileMetadata {
  id: number;
  originalFilename: string;
  uploadedAt: string;
  size: number;
  zke?: boolean;
}

const Home: React.FC<HomeProps> = ({ onLogout, token }) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [message, setMessage] = useState('');
  const [files, setFiles] = useState<FileMetadata[]>([]);
  const [username, setUsername] = useState('');
  const [sharedLinks, setSharedLinks] = useState<SharedLink[]>([]);

  // Decryption Modal State
  const [decryptModalOpen, setDecryptModalOpen] = useState(false);
  const [pendingFile, setPendingFile] = useState<{ id: number; filename: string } | null>(null);


  const fetchFiles = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/v1/files', {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (res.ok) {
        const rawFiles = await res.json();

          // Infer `zke` from filename
          const processedFiles = rawFiles.map((f: any) => ({
            ...f,
            zke: f.originalFilename.endsWith('.enc'),
          }));

          setFiles(processedFiles);

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
      const fetchSharedLinks = async () => {
        try {
          const res = await fetch('http://localhost:8080/api/v1/share', {
            headers: { Authorization: `Bearer ${token}` }
          });
          if (res.ok) {
            const data = await res.json();
            setSharedLinks(Array.isArray(data) ? data : []);
          }
        } catch (error) {
          console.error("Failed to fetch shared links:", error);
          setSharedLinks([]); // Reset to empty array on error
        }
      };

      fetchSharedLinks();
    }, [token]);

    const handleSearch = async (query: string) => {
      try {
        const params = new URLSearchParams({ query });
        const res = await fetch(`http://localhost:8080/api/v1/files/search?${params}`, {
          headers: { Authorization: `Bearer ${token}` }
        });

        if (res.ok) {
          const results = await res.json();
          setFiles(results);
        }
      } catch (error) {
        setMessage("Search failed: " + (error as Error).message);
      }
    };

    const handleRevokeShare = async (shareId: string) => {
      try {
        // Extract token from URL (last segment after '/')
        const shareToken = sharedLinks.find(link => link.id === shareId)?.url.split('/').pop();
        if (!shareToken) {
          setMessage('Invalid share link');
          return;
        }

        const res = await fetch(`http://localhost:8080/api/v1/share/${shareToken}`, {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${token}` }
        });

        if (res.ok) {
          setSharedLinks(prev => prev.filter(link => link.id !== shareId));
          setMessage('Share link revoked successfully');
        }
      } catch (error) {
        setMessage('Failed to revoke share link');
      }
    };

  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/v1/auth/me', {
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

  const handleUpload = async (zkeOptions?: { useZKE: boolean; passphrase: string }) => {
    if (!selectedFile) {
      setMessage('Please select a file.');
      return;
    }

    const formData = new FormData();
    try {
      if (zkeOptions?.useZKE) {
        const encryptedBlob = await encryptFile(selectedFile, zkeOptions.passphrase);
        formData.append('file', encryptedBlob, selectedFile.name + ".enc");
        formData.append("zke", "true");
      } else {
        formData.append('file', selectedFile);
      }

      const res = await fetch('http://localhost:8080/api/v1/files/upload', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
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

    const handleDownload = async (fileId: number, filename: string, isZke?: boolean) => {

      if (isZke) {
        setPendingFile({ id: fileId, filename });
        setDecryptModalOpen(true);
      } else {
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
            setMessage("Download failed");
          }
        } catch {
          setMessage("Download error");
        }
      }
    };

  const handleConfirmDecryption = async (passphrase: string) => {
    if (!pendingFile) return;

    try {
      const res = await fetch(`http://localhost:8080/api/v1/files/${pendingFile.id}/download`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!res.ok) {
        setMessage("Download failed");
        return;
      }

      const encryptedBlob = await res.blob();
      const arrayBuffer = await encryptedBlob.arrayBuffer();

      // 🔓 Extract IV (first 12 bytes) and ciphertext (remaining bytes)
      const iv = new Uint8Array(arrayBuffer.slice(0, 12));
      const encryptedData = arrayBuffer.slice(12);

      // 🔐 Decrypt
      const decryptedArrayBuffer = await decryptFile(encryptedData, passphrase, iv);

      // 💾 Download decrypted file
      const decryptedBlob = new Blob([decryptedArrayBuffer]);
      const url = URL.createObjectURL(decryptedBlob);
      const a = document.createElement("a");
      a.href = url;
      a.download = pendingFile.filename.replace(/\.enc$/, "");
      a.click();
      URL.revokeObjectURL(url);

      setMessage("File decrypted and downloaded successfully!");
    } catch (err) {
      console.error(err);
      setMessage("Decryption failed. Maybe wrong passphrase.");
    } finally {
      setDecryptModalOpen(false);
      setPendingFile(null);
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

        const shareUrl = await res.text();
        const file = files.find(f => f.id === fileId);

        // Safely update sharedLinks
        setSharedLinks(prev => {
          const currentLinks = Array.isArray(prev) ? prev : [];
          return [
            ...currentLinks,
            {
              id: Date.now().toString(),
              fileId,
              filename: file?.originalFilename || '',
              url: shareUrl,
              expiresAt: data.expiryMinutes
                ? new Date(Date.now() + data.expiryMinutes * 60000).toISOString()
                : undefined,
              hasPassword: !!data.password
            }
          ];
        });

        return shareUrl;
      } catch (error) {
        setMessage('Error generating share link: ' + (error as Error).message);
        throw error;
      }
    };

  return (
    <div className="min-h-screen bg-gray-50">
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
            onSearch={handleSearch}
            sharedLinks={sharedLinks}
            onDownload={handleDownload}
            onDelete={handleDelete}
            onShare={handleShare}
            onRevokeShare={handleRevokeShare}
          />
        </div>
      </main>

      <DecryptModal
        open={decryptModalOpen}
        onClose={() => setDecryptModalOpen(false)}
        onConfirm={handleConfirmDecryption}
      />
    </div>
  );
};

export default Home;
