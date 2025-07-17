import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { toast } from "sonner";

import {
  Share1Icon,
  DownloadIcon,
  TrashIcon,
  Link1Icon,
  LockClosedIcon,
  ClockIcon,
} from "@radix-ui/react-icons";

interface FileItemProps {
  file: {
    id: number;
    originalFilename: string;
    zke?: boolean;
    size: number;
  };
  onDownload: (id: number, filename: string, isZke?: boolean) => void;
  onDelete: (id: number) => void;
  onShare: (fileId: number, data: { password?: string; expiryMinutes?: number }) => Promise<string>;
}

const FileItem: React.FC<FileItemProps> = ({ file, onDownload, onDelete, onShare }) => {
  const [password, setPassword] = useState("");
  const [expiryMinutes, setExpiryMinutes] = useState("");
  const [shareLink, setShareLink] = useState("");
  const [showShareDialog, setShowShareDialog] = useState(false);
  const [qrCodeUrl, setQrCodeUrl] = useState("");

  const handleShare = async () => {
    try {
      const data = {
        ...(password && { password }),
        ...(expiryMinutes && { expiryMinutes: Number(expiryMinutes) }),
      };
      const link = await onShare(file.id, data);
      setShareLink(link);

      const token = link.split("/").pop();
      setQrCodeUrl(`http://localhost:8080/api/v1/share/qr/${token}`);
    } catch (error) {
      console.error("Failed to generate share link:", error);
    }
  };

  function formatFileSize(bytes: number): string {
      if (bytes === 0) return '0 Bytes';

      const k = 1024;
      const sizes = ['Bytes', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));

      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

  return (
    <li className="flex flex-col sm:flex-row sm:items-center justify-between p-4 bg-muted rounded-md border hover:shadow-sm transition">
      <span className="text-sm font-medium text-gray-800 mb-2 sm:mb-0 break-all">
        {file.originalFilename}
        <span className="text-xs text-muted-foreground ml-2">
            ({formatFileSize(file.size)})
        </span>
      </span>

      <div className="flex flex-wrap gap-2 justify-end">
        {/* Share Button with Dialog */}
        <Dialog
          open={showShareDialog}
          onOpenChange={(open) => {
            setShowShareDialog(open);
            if (!open) {
              setPassword("");
              setExpiryMinutes("");
              setShareLink("");
              setQrCodeUrl("");
            }
          }}
        >
          <Button variant="default" size="sm" onClick={() => setShowShareDialog(true)}>
            <Share1Icon className="mr-1 h-4 w-4" /> Share
          </Button>

          <DialogContent className="max-w-md p-6 space-y-4">
            <DialogHeader>
              <DialogTitle>Share: {file.originalFilename}</DialogTitle>
            </DialogHeader>

            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="password">Password (optional)</Label>
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Add protection"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="expiry">Expiry in minutes (optional)</Label>
                <Input
                  id="expiry"
                  type="number"
                  value={expiryMinutes}
                  onChange={(e) => setExpiryMinutes(e.target.value)}
                  placeholder="e.g. 60"
                />
              </div>

              <Button onClick={handleShare} className="w-full">
                <Link1Icon className="mr-2 h-4 w-4" />
                Generate Link
              </Button>

              {shareLink && (
                <Card>
                  <CardContent className="space-y-2 pt-4">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-sm font-mono break-all">{shareLink}</span>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          navigator.clipboard.writeText(shareLink);
                          toast("Share link copied to clipboard");
                        }}
                      >
                        Copy
                      </Button>
                    </div>

                    <div className="flex items-center text-xs text-muted-foreground gap-4">
                      {password && (
                        <span className="flex items-center gap-1">
                          <LockClosedIcon className="h-3 w-3" /> Password protected
                        </span>
                      )}
                      {expiryMinutes && (
                        <span className="flex items-center gap-1">
                          <ClockIcon className="h-3 w-3" /> Expires in {expiryMinutes} mins
                        </span>
                      )}
                    </div>

                    {qrCodeUrl && (
                      <div className="flex justify-center mt-2">
                        <img
                          src={qrCodeUrl}
                          alt="QR Code"
                          className="w-36 h-36 border rounded shadow"
                        />
                      </div>
                    )}
                  </CardContent>
                </Card>
              )}
            </div>

            <DialogFooter>
              <Button variant="secondary" onClick={() => setShowShareDialog(false)} className="w-full">
                Close
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        <Button
          className="bg-blue-600 hover:bg-blue-700 text-white"
          size="sm"
          onClick={() => onDownload(file.id, file.originalFilename, file.zke)}
        >
          <DownloadIcon className="mr-1 h-4 w-4" /> Download
        </Button>

        <Button
          variant="destructive"
          size="sm"
          onClick={() => onDelete(file.id)}
        >
          <TrashIcon className="mr-1 h-4 w-4" /> Delete
        </Button>
      </div>
    </li>
  );
};

export default FileItem;
