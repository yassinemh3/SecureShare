import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Copy, Trash2, Clock, Lock, MoreVertical, QrCode } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Card } from "@/components/ui/card";
import { useState } from "react";

interface SharedLinkItemProps {
  link: {
    id: string;
    fileId: number;
    filename: string;
    url: string;
    qrCodeUrl: string;
    expiresAt?: string;
    hasPassword: boolean;
  };
  onRevoke: (id: string) => void;
}

export const SharedLinkItem = ({ link, onRevoke }: SharedLinkItemProps) => {
  const [showQr, setShowQr] = useState(false);

  return (
    <Card className="p-4 rounded-xl hover:shadow-md transition">
      <div className="flex justify-between items-start gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-2">
            <p className="font-medium truncate">{link.filename}</p>
            <div className="flex items-center gap-1">
              {link.expiresAt && (
                <Badge variant="outline" className="text-xs gap-1 py-0 px-1.5">
                  <Clock className="h-3 w-3" />
                  Expires {new Date(link.expiresAt).toLocaleDateString()}
                </Badge>
              )}
            </div>
            {link.hasPassword && (
              <Badge variant="outline" className="text-xs gap-1 py-0 px-1.5 bg-yellow-50 border-yellow-200 text-yellow-800">
                <Lock className="h-3 w-3" />
                Password Protected
              </Badge>
            )}
          </div>

          <div className="flex items-center gap-2 mb-2">
            <div className="flex-1 bg-muted p-2 rounded-md overflow-hidden">
              <p className="text-xs font-mono truncate">{link.url}</p>
            </div>
            <div className="flex gap-1">
              <Button
                variant="outline"
                size="icon"
                onClick={() => {
                  navigator.clipboard.writeText(link.url);
                  toast.success("Link copied to clipboard");
                }}
                className="rounded-lg h-8 w-8"
              >
                <Copy className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="icon"
                onClick={() => setShowQr(!showQr)}
                className="rounded-lg h-8 w-8"
              >
                <QrCode className="h-4 w-4" />
              </Button>
            </div>
          </div>

          {showQr && (
            <div className="flex justify-center pt-2">
              <img
                src={link.qrCodeUrl}
                alt="QR Code"
                className="w-32 h-32 border rounded-lg"
              />
            </div>
          )}
        </div>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="h-8 w-8">
              <MoreVertical className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem
              onClick={() => onRevoke(link.id)}
              className="text-red-500"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              Revoke
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </Card>
  );
};

export default SharedLinkItem;