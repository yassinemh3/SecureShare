import React, { ChangeEvent } from 'react';

interface FileUploadFormProps {
  onFileChange: (e: ChangeEvent<HTMLInputElement>) => void;
  onUpload: () => void;
  selectedFile: File | null;
}

const FileUploadForm: React.FC<FileUploadFormProps> = ({
  onFileChange,
  onUpload,
  selectedFile
}) => (
  <div className="mb-4">
    <input
      type="file"
      onChange={onFileChange}
      className="w-full px-3 py-2 border rounded-md"
    />
    <button
      onClick={onUpload}
      disabled={!selectedFile}
      className={`mt-2 w-full py-2 rounded transition ${
        selectedFile
          ? 'bg-blue-600 text-white hover:bg-blue-700'
          : 'bg-gray-300 text-gray-500 cursor-not-allowed'
      }`}
    >
      Upload File
    </button>
  </div>
);

export default FileUploadForm;