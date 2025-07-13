"use client"

import { useCallback, useState } from "react"
import { useDropzone } from "react-dropzone"
import { Button } from "@/components/ui/button"
import { UploadIcon } from "lucide-react"

const FileUploadForm = ({ onFileChange, onUpload, selectedFile }: FileUploadFormProps) => {
  const onDrop = useCallback((acceptedFiles: File[]) => {
    onFileChange({ target: { files: acceptedFiles } } as any)
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    maxFiles: 1
  })

  return (
    <div {...getRootProps()} className={`border-2 border-dashed rounded-lg p-6 text-center ${
      isDragActive ? "border-primary bg-primary/10" : "border-muted-foreground/30"
    }`}>
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
      <Button
        type="button"
        onClick={(e) => {
          e.stopPropagation()
          onUpload()
        }}
        disabled={!selectedFile}
        className="mt-4 w-full"
      >
        Upload File
      </Button>
    </div>
  )
}
export default FileUploadForm;