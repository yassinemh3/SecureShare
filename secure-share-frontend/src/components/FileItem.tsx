import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { toast } from 'sonner';

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

      // Extract token from the link
      const token = link.split("/").pop();
      const qrUrl = `http://localhost:8080/api/v1/share/qr/${token}`; // Update port if different
      setQrCodeUrl(qrUrl);
    } catch (error) {
      console.error("Failed to generate share link:", error);
    }
  };

  return (
    <li className="py-2 flex justify-between items-center border-b">
      <span className="text-gray-800">{file.originalFilename}</span>

      <div className="flex gap-2">
        <Dialog open={showShareDialog} onOpenChange={(open) => {
          setShowShareDialog(open);
          if (!open) {
            setPassword("");
            setExpiryMinutes("");
            setShareLink("");
          }
        }}>
          <Button variant="default" size="sm" onClick={() => setShowShareDialog(true)}>
            Share
          </Button>

          <DialogContent className="max-w-md p-6">
            <DialogHeader>
              <DialogTitle>Share "{file.originalFilename}"</DialogTitle>
            </DialogHeader>

            <div className="space-y-4">
               <div className="space-y-2">
                <Label htmlFor="password">Password (optional)</Label>
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Leave empty for no password"
                  className="w-full"
                />
              </div>

               <div className="space-y-2">
                <Label htmlFor="expiry">Expiry in minutes (optional)</Label>
                <Input
                  id="expiry"
                  type="number"
                  value={expiryMinutes}
                  onChange={(e) => setExpiryMinutes(e.target.value)}
                  placeholder="Leave empty for no expiry"
                  className="w-full"
                />
              </div>

              <Button onClick={handleShare} className="w-full">
                Generate Link
              </Button>

              {shareLink && (
                <Card>
                  <CardContent className="pt-4 space-y-2">
                    <div className="flex items-center gap-2">
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
                    <p className="text-xs text-gray-500">
                      {password ? "Password protected" : "No password"} |{" "}
                      {expiryMinutes ? `Expires in ${expiryMinutes} minutes` : "No expiry"}
                    </p>
                      {/* QR Code Display */}
                      {qrCodeUrl && (
                        <div className="flex justify-center">
                          <img
                            src={qrCodeUrl}
                            alt="QR Code"
                            className="w-40 h-40 border rounded shadow"
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
          variant="secondary"
          size="sm"
          onClick={() => onDownload(file.id, file.originalFilename)}
        >
          Download
        </Button>

        <Button
          variant="destructive"
          size="sm"
          onClick={() => onDelete(file.id)}
        >
          Delete
        </Button>
      </div>
    </li>
  );
};

export default FileItem;
