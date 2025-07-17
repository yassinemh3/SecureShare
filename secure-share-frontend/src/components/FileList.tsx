import React from 'react';
import FileItem from './FileItem';
import { Card, CardContent } from "@/components/ui/card";
import { SharedLinkItem } from "@/components/SharedLinkItem";

interface FileListProps {
  files: Array<{
    id: number;
    originalFilename: string;
    zke?: boolean;
    size: number;
  }>;
  sharedLinks?: Array<{
    id: string;
    fileId: number;
    filename: string;
    url: string;
    expiresAt?: string;
    hasPassword: boolean;
  }>;
  onDownload: (id: number, filename: string, isZke?: boolean) => void;
  onDelete: (id: number) => void;
  onShare: (fileId: number, data: { password?: string; expiryMinutes?: number }) => Promise<string>;
  onRevokeShare?: (id: string) => void;
}

export const FileList = ({
  files = [],
  sharedLinks,
  onDownload,
  onDelete,
  onShare,
  onRevokeShare = () => {}
}: FileListProps) => {
  // Safely handle the sharedLinks prop
  const safeSharedLinks = Array.isArray(sharedLinks) ? sharedLinks : [];
  const safeFiles = Array.isArray(files) ? files : [];

  return (
    <div className="space-y-6">
      {/* Your Files Section */}
      <Card className="mt-6">
        <CardContent className="p-6 space-y-4">
          <h2 className="text-xl font-semibold">Your Files</h2>
          {safeFiles.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">No files uploaded yet</p>
          ) : (
            <ul className="space-y-3">
              {safeFiles.map(file => (
                <FileItem
                  key={file.id}
                  file={file}
                  onDownload={onDownload}
                  onDelete={onDelete}
                  onShare={onShare}
                />
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      {/* Shared Links Section */}
      <Card>
        <CardContent className="p-6 space-y-4">
          <h2 className="text-xl font-semibold">Shared Links</h2>
          {safeSharedLinks.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">No active shared links</p>
          ) : (
            <ul className="space-y-3">
              {safeSharedLinks.map(link => (
                <SharedLinkItem
                  key={link.id}
                  link={link}
                  onRevoke={onRevokeShare}
                />
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default FileList;