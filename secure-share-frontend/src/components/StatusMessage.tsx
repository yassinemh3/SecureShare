"use client";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { ExclamationTriangleIcon, CheckCircledIcon, InfoCircledIcon } from "@radix-ui/react-icons";

interface StatusMessageProps {
  message: string;
  type?: "error" | "success" | "info";
  className?: string;
}

export const StatusMessage = ({ message, type = "error", className = "" }: StatusMessageProps) => {
  if (!message) return null;

  const iconMap = {
    error: <ExclamationTriangleIcon className="h-4 w-4" />,
    success: <CheckCircledIcon className="h-4 w-4" />,
    info: <InfoCircledIcon className="h-4 w-4" />,
  };

  const variantMap = {
    error: "destructive",
    success: "default",
    info: "default",
  } as const;

  return (
    <Alert variant={variantMap[type]} className={`mt-4 ${className}`}>
      <AlertTitle className="flex items-center gap-2">
        {iconMap[type]}
        {type === "error" ? "Error" : type === "success" ? "Success" : "Info"}
      </AlertTitle>
      <AlertDescription className="mt-1 text-sm">
        {message}
      </AlertDescription>
    </Alert>
  );
};

export default StatusMessage;