import React from 'react';

interface StatusMessageProps {
  message: string;
  type?: 'error' | 'success';
}

const StatusMessage: React.FC<StatusMessageProps> = ({ message, type = 'error' }) => (
  message ? (
    <p className={`text-center text-sm mb-4 ${
      type === 'error' ? 'text-red-600' : 'text-green-600'
    }`}>
      {message}
    </p>
  ) : null
);

export default StatusMessage;