"use client";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { ExclamationTriangleIcon, CheckCircledIcon } from "@radix-ui/react-icons";

export const StatusMessage = ({ message, type = "error" }: StatusMessageProps) => {
  if (!message) return null;

  return (
    <Alert variant={type === "error" ? "destructive" : "default"} className="mt-4">
      <AlertTitle className="flex items-center gap-2">
        {type === "error" ? (
          <ExclamationTriangleIcon className="h-4 w-4" />
        ) : (
          <CheckCircledIcon className="h-4 w-4" />
        )}
        {type === "error" ? "Error" : "Success"}
      </AlertTitle>
      <AlertDescription className="mt-1 text-sm">
        {message}
      </AlertDescription>
    </Alert>
  );
};

export default StatusMessage;