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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { toast } from "sonner";
import { AiSummaryPanel } from '@/components/AiSummaryPanel';
import { useAiProcessing } from '../ai/hooks/useAiProcessing';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import {
  FileText,
  Share2,
  Download,
  Trash2,
  Link2,
  Lock,
  Clock,
  MoreVertical,
  Copy,
  Sparkles
} from "lucide-react";

interface FileItemProps {
  file: {
    id: number;
    originalFilename: string;
    zke?: boolean;
    size: number;
    content?: string;
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
  const [showSummary, setShowSummary] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const { isProcessing, result, summarize } = useAiProcessing();

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

  const handleSummaryRequest = async () => {
    if (!file.content) return;
    await summarize(file.content);
    setShowSummary(true);
  };

  function formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  return (
    <div className="space-y-2">
      <Card className="p-3 hover:shadow-sm transition">
        <div className="flex items-center justify-between w-full">
          {/* File info - left aligned */}
          <div className="flex items-center gap-3 min-w-0 flex-1">
            <FileText className="h-5 w-5 text-blue-600 flex-shrink-0" />
            <div className="min-w-0">
              <p className="text-sm font-medium text-gray-800 truncate">
                {file.originalFilename}
              </p>
              <p className="text-xs text-muted-foreground">
                {formatFileSize(file.size)}
              </p>
            </div>
          </div>

          {/* Actions - right aligned */}
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => onDownload(file.id, file.originalFilename, file.zke)}
              className="h-8 w-8"
            >
              <Download className="h-4 w-4" />
            </Button>

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <MoreVertical className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => setShowShareDialog(true)}>
                  <Share2 className="mr-2 h-4 w-4" />
                  Share
                </DropdownMenuItem>
                {file.content && (
                  <DropdownMenuItem onClick={() => setShowSummary(!showSummary)}>
                    <Sparkles className="mr-2 h-4 w-4" />
                    {showSummary ? "Hide Summary" : "Show Summary"}
                  </DropdownMenuItem>
                )}
                <DropdownMenuItem
                  onClick={() => setShowDeleteDialog(true)}
                  className="text-red-500 focus:text-red-500 focus:bg-red-50"
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  Delete
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </Card>

      {/* Share Dialog */}
      <Dialog open={showShareDialog} onOpenChange={setShowShareDialog}>
        <DialogContent className="sm:max-w-md w-full max-w-[90vw] rounded-lg">
          <DialogHeader>
            <DialogTitle className="text-lg">Share: {file.originalFilename}</DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-4 w-full">
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
              <Link2 className="mr-2 h-4 w-4" />
              Generate Link
            </Button>
          </div>

          {shareLink && (
            <>
              <div className="border-t pt-4 w-full">
                <Card className="w-full">
                  <CardContent className="p-4 space-y-4 w-full">
                    <div className="flex items-start gap-2 w-full">
                      <div className="flex-1 bg-muted p-2 rounded-md overflow-hidden min-w-0 max-w-full">
                        <p className="text-sm font-mono break-all whitespace-pre-wrap">{shareLink}</p>
                      </div>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          navigator.clipboard.writeText(shareLink);
                          toast.success("Link copied to clipboard");
                        }}
                        className="flex-shrink-0 mt-0"
                      >
                        <Copy className="h-4 w-4" />
                      </Button>
                    </div>

                    <div className="flex items-center gap-2 text-xs text-muted-foreground flex-wrap">
                      {password && (
                        <Badge variant="outline" className="gap-1">
                          <Lock className="h-3 w-3" />
                          Protected
                        </Badge>
                      )}
                      {expiryMinutes && (
                        <Badge variant="outline" className="gap-1">
                          <Clock className="h-3 w-3" />
                          Expires in {expiryMinutes} mins
                        </Badge>
                      )}
                    </div>

                    {qrCodeUrl && (
                      <div className="mt-4 flex justify-center w-full">
                        <img
                          src={qrCodeUrl}
                          alt="QR Code"
                          className="w-32 h-32 border rounded-lg"
                        />
                      </div>
                    )}
                  </CardContent>
                </Card>
              </div>
              <DialogFooter className="w-full">
                <Button
                  variant="secondary"
                  onClick={() => setShowShareDialog(false)}
                  className="w-full"
                >
                  Close
                </Button>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently delete "{file.originalFilename}" and cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => onDelete(file.id)}
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Summary panel */}
      {showSummary && file.content && (
        <AiSummaryPanel
          result={result}
          isProcessing={isProcessing}
          onRequestSummary={handleSummaryRequest}
          onClose={() => setShowSummary(false)}
        />
      )}
    </div>
  );
};

export default FileItem;