import { useState } from 'react';
import { summarizeDocument } from '../services/summarization';

export const useAiProcessing = () => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [result, setResult] = useState<string | null>(null);

  const summarize = async (text: string) => {
    setIsProcessing(true);
    try {
      const summary = await summarizeDocument(text);
      setResult(summary);
    } finally {
      setIsProcessing(false);
    }
  };

  return { isProcessing, result, summarize };
};