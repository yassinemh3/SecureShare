import React from 'react';
import FileItem from './FileItem';

interface FileListProps {
  files: Array<{
    id: number;
    originalFilename: string;
  }>;
  onDownload: (id: number, filename: string) => void;
  onDelete: (id: number) => void;
  onShare: (fileId: number, data: { password?: string; expiryMinutes?: number }) => Promise<string>;
}

const FileList: React.FC<FileListProps> = ({ files, onDownload, onDelete, onShare }) => (
  <>
    <h2 className="text-xl font-medium mb-2">Your Files</h2>
    <ul className="divide-y divide-gray-200">
      {files.length === 0 ? (
        <p className="text-gray-500">No files uploaded.</p>
      ) : (
        files.map(file => (
          <FileItem
            key={file.id}
            file={file}
            onDownload={onDownload}
            onDelete={onDelete}
            onShare={onShare}
          />
        ))
      )}
    </ul>
  </>
);

export default FileList;