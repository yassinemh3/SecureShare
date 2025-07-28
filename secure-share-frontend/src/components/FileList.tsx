import React from 'react';
import FileItem from './FileItem';
import { Card, CardContent } from "@/components/ui/card";
import { SharedLinkItem } from "@/components/SharedLinkItem";
import { Input } from "@/components/ui/input";
import { SearchIcon, FileIcon, LinkIcon } from "lucide-react";
import { useState } from 'react';

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
  onSearch?: (query: string) => void;
}

export const FileList = ({
  files = [],
  sharedLinks,
  onDownload,
  onDelete,
  onShare,
  onRevokeShare = () => {}
}: FileListProps) => {
  const safeSharedLinks = Array.isArray(sharedLinks) ? sharedLinks : [];
  const safeFiles = Array.isArray(files) ? files : [];

  const [searchQuery, setSearchQuery] = useState("");

  const filteredFiles = safeFiles.filter(file =>
    file.originalFilename.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Filter out shared links for deleted files
    const validSharedLinks = safeSharedLinks.filter(link =>
      safeFiles.some(file => file.id === link.fileId)
    );

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold flex items-center gap-2">
          <FileIcon className="h-5 w-5 text-blue-500" />
          Your Files
        </h2>
        <div className="relative w-64">
          <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Search files..."
            className="pl-10 rounded-lg"
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              if (onSearch) onSearch(e.target.value);
            }}
          />
        </div>
      </div>

      {filteredFiles.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground flex flex-col items-center gap-2">
          <FileIcon className="h-8 w-8 text-gray-300" />
          <p>{searchQuery ? "No matching files" : "No files uploaded yet"}</p>
        </div>
      ) : (
        <ul className="space-y-2">
          {filteredFiles.map(file => (
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
    </div>
  );
};

export default FileList;