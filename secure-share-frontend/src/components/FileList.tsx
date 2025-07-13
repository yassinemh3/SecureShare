import React from 'react';
import FileItem from './FileItem';
import { Card, CardContent } from "@/components/ui/card"

interface FileListProps {
  files: Array<{
    id: number;
    originalFilename: string;
  }>;
  onDownload: (id: number, filename: string) => void;
  onDelete: (id: number) => void;
  onShare: (fileId: number, data: { password?: string; expiryMinutes?: number }) => Promise<string>;
}

export const FileList = ({ files, onDownload, onDelete, onShare }: FileListProps) => {
  return (
    <Card>
      <CardContent className="p-6">
        <h2 className="text-xl font-semibold mb-4">Your Files</h2>
        <div className="space-y-4">
          {files.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">No files uploaded yet</p>
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
        </div>
      </CardContent>
    </Card>
  )
}

export default FileList;