import { Button } from "@/components/ui/button";
import { CopyIcon, TrashIcon, ClockIcon, LockClosedIcon } from "@radix-ui/react-icons";
import { toast } from "sonner";

interface SharedLinkItemProps {
  link: {
    id: string;
    fileId: number;
    filename: string;
    url: string;
    expiresAt?: string;
    hasPassword: boolean;
  };
  onRevoke: (id: string) => void;
}

export const SharedLinkItem = ({ link, onRevoke }: SharedLinkItemProps) => {
  return (
    <li className="flex flex-col p-4 bg-muted rounded-md border">
      <div className="flex justify-between items-start">
        <div>
          <p className="font-medium">{link.filename}</p>
          <div className="text-sm text-muted-foreground mt-1 flex items-center gap-2">
            {link.hasPassword && <LockClosedIcon className="h-3 w-3" />}
            {link.expiresAt && (
              <span className="flex items-center gap-1">
                <ClockIcon className="h-3 w-3" />
                Expires {new Date(link.expiresAt).toLocaleString()}
              </span>
            )}
          </div>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              navigator.clipboard.writeText(link.url);
              toast.success("Link copied to clipboard");
            }}
          >
            <CopyIcon className="mr-1 h-4 w-4" />
            Copy
          </Button>
          <Button
            variant="destructive"
            size="sm"
            onClick={() => onRevoke(link.id)}
          >
            <TrashIcon className="mr-1 h-4 w-4" />
            Revoke
          </Button>
        </div>
      </div>
      <div className="mt-2">
        <p className="text-xs font-mono break-all bg-background p-2 rounded">
          {link.url}
        </p>
      </div>
    </li>
  );
};