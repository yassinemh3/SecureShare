import React from 'react';
import FileItem from './FileItem';
import { Card, CardContent } from "@/components/ui/card";
import { SharedLinkItem } from "@/components/SharedLinkItem";
import { Input } from "@/components/ui/input";
import { SearchIcon } from "lucide-react";
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
  // Safely handle the sharedLinks prop
  const safeSharedLinks = Array.isArray(sharedLinks) ? sharedLinks : [];
  const safeFiles = Array.isArray(files) ? files : [];

  const [searchQuery, setSearchQuery] = useState("");

  // Filter files based on search query
  const filteredFiles = safeFiles.filter(file =>
    file.originalFilename.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Search Bar */}
      <div className="relative">
        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          type="text"
          placeholder="Search files..."
          className="pl-10"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      {/* Your Files Section */}
      <Card className="mt-2"> {/* Reduced margin-top */}
        <CardContent className="p-6 space-y-4">
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-semibold">Your Files</h2>
            {searchQuery && (
              <span className="text-sm text-muted-foreground">
                {filteredFiles.length} results
              </span>
            )}
          </div>

          {filteredFiles.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">
              {searchQuery ? "No matching files" : "No files uploaded yet"}
            </p>
          ) : (
            <ul className="space-y-3">
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