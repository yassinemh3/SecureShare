"use client"

import { Alert, AlertDescription } from "@/components/ui/alert"

export const StatusMessage = ({ message, type = "error" }: StatusMessageProps) => {
  if (!message) return null

  return (
    <Alert variant={type === "error" ? "destructive" : "default"}>
      <AlertDescription>
        {message}
      </AlertDescription>
    </Alert>
  )
}

export default StatusMessage;