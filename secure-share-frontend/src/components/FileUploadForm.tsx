"use client"

import { useCallback, useState } from "react"
import { useDropzone } from "react-dropzone"
import { Button } from "@/components/ui/button"
import { UploadIcon } from "lucide-react"
import { Switch } from "@/components/ui/switch"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"

interface FileUploadFormProps {
  onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onUpload: (zkeOptions?: { useZKE: boolean; passphrase: string }) => void;
  selectedFile: File | null;
}

const FileUploadForm = ({ onFileChange, onUpload, selectedFile }: FileUploadFormProps) => {
  const [useZKE, setUseZKE] = useState(false);
  const [passphrase, setPassphrase] = useState("");

  const onDrop = useCallback((acceptedFiles: File[]) => {
    onFileChange({ target: { files: acceptedFiles } } as any);
  }, [onFileChange]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    maxFiles: 1
  });

  return (
    <div className="space-y-4">
      {/* Toggle and passphrase ABOVE drop zone */}
      <div className="space-y-2">
        <div className="flex items-center space-x-2">
          <Switch id="zke-toggle" checked={useZKE} onCheckedChange={setUseZKE} />
          <Label htmlFor="zke-toggle" className="flex items-center gap-1">
            <span role="img" aria-label="lock">🔒</span> Enable Zero-Knowledge Encryption
          </Label>
        </div>

        {useZKE && (
          <Input
            type="password"
            placeholder="Enter encryption passphrase"
            value={passphrase}
            onChange={(e) => setPassphrase(e.target.value)}
            required
          />
        )}
      </div>

      {/* 🗂 Drag & drop area */}
      <div
        {...getRootProps()}
        className={`border-2 border-dashed rounded-lg p-6 text-center transition ${
          isDragActive ? "border-primary bg-primary/10" : "border-muted-foreground/30"
        }`}
      >
        <input {...getInputProps()} />
        <div className="flex flex-col items-center gap-2">
          <UploadIcon className="h-10 w-10 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">
            {isDragActive ? "Drop the file here" : "Drag & drop files here, or click to select"}
          </p>
          {selectedFile && (
            <p className="text-sm font-medium mt-2">{selectedFile.name}</p>
          )}
        </div>
      </div>

      {/* Upload Button */}
      <Button
        type="button"
        onClick={() => onUpload(useZKE ? { useZKE, passphrase } : undefined)}
        disabled={!selectedFile || (useZKE && !passphrase)}
        className="w-full"
      >
        Upload File
      </Button>
    </div>
  );
};

export default FileUploadForm;
