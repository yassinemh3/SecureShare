import React, { useState } from 'react';

interface FileItemProps {
  file: {
    id: number;
    originalFilename: string;
  };
  onDownload: (id: number, filename: string) => void;
  onDelete: (id: number) => void;
  onShare: (fileId: number, data: { password?: string; expiryMinutes?: number }) => Promise<string>;
}

const FileItem: React.FC<FileItemProps> = ({ file, onDownload, onDelete, onShare }) => {
  const [password, setPassword] = useState('');
  const [expiryMinutes, setExpiryMinutes] = useState('');
  const [shareLink, setShareLink] = useState('');
  const [showShareDialog, setShowShareDialog] = useState(false);

  const handleShare = async () => {
    try {
      const data = {
        ...(password && { password }),
        ...(expiryMinutes && { expiryMinutes: Number(expiryMinutes) })
      };

      const link = await onShare(file.id, data);
      setShareLink(link);
    } catch (error) {
      console.error('Failed to generate share link:', error);
    }
  };

  return (
    <li className="py-2 flex justify-between items-center">
      <span className="text-gray-800">{file.originalFilename}</span>
      <div className="flex gap-2">
        <button
          onClick={() => setShowShareDialog(true)}
          className="bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700 text-sm"
        >
          Share
        </button>

        {showShareDialog && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white p-6 rounded-lg max-w-md w-full">
              <h3 className="text-lg font-semibold mb-4">Share "{file.originalFilename}"</h3>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Password (optional)</label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Leave empty for no password"
                    className="w-full px-3 py-2 border rounded"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-1">Expiry in minutes (optional)</label>
                  <input
                    type="number"
                    value={expiryMinutes}
                    onChange={(e) => setExpiryMinutes(e.target.value)}
                    placeholder="Leave empty for no expiry"
                    className="w-full px-3 py-2 border rounded"
                  />
                </div>

                <button
                  onClick={handleShare}
                  className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
                >
                  Generate Link
                </button>

                {shareLink && (
                  <div className="mt-4 p-3 bg-gray-100 rounded">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-mono break-all">{shareLink}</span>
                      <button
                        onClick={() => {
                          navigator.clipboard.writeText(shareLink);
                          alert('Link copied to clipboard!');
                        }}
                        className="p-1 rounded hover:bg-gray-200"
                      >
                        Copy
                      </button>
                    </div>
                    <p className="text-xs text-gray-500 mt-2">
                      {password ? 'Password protected' : 'No password'} |
                      {expiryMinutes ? ` Expires in ${expiryMinutes} minutes` : ' No expiry'}
                    </p>
                  </div>
                )}
              </div>

              <button
                onClick={() => {
                  setShowShareDialog(false);
                  setShareLink('');
                }}
                className="mt-4 w-full bg-gray-500 text-white py-2 rounded hover:bg-gray-600"
              >
                Close
              </button>
            </div>
          </div>
        )}

        <button
          onClick={() => onDownload(file.id, file.originalFilename)}
          className="bg-green-600 text-white px-3 py-1 rounded hover:bg-green-700 text-sm"
        >
          Download
        </button>
        <button
          onClick={() => onDelete(file.id)}
          className="bg-red-600 text-white px-3 py-1 rounded hover:bg-red-700 text-sm"
        >
          Delete
        </button>
      </div>
    </li>
  );
};

export default FileItem;