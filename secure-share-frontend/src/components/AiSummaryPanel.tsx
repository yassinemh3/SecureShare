import { Card, CardHeader, CardContent } from '@/components/ui/card';
import { Cross2Icon } from "@radix-ui/react-icons";
import { SparklesIcon } from 'lucide-react';

interface AiSummaryPanelProps {
  result: string | null;
  isProcessing: boolean;
  onRequestSummary: () => void;
  onClose: () => void;
}

export const AiSummaryPanel = ({ result, isProcessing, onRequestSummary, onClose }: AiSummaryPanelProps) => {
  return (
    <div className="w-full mt-2">
      <Card className="h-full">
        <CardHeader className="pb-2">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold">AI Summary</h3>
            <div className="flex gap-2">
              <button
                onClick={onRequestSummary}
                disabled={isProcessing}
                className="text-sm flex items-center gap-1 text-muted-foreground hover:text-primary transition-colors"
                title="Regenerate summary"
              >
                <SparklesIcon className="h-4 w-4" />
              </button>
              <button
                onClick={onClose}
                className="text-sm flex items-center gap-1 text-muted-foreground hover:text-primary transition-colors"
                title="Close panel"
              >
                <Cross2Icon className="h-4 w-4" />
              </button>
            </div>
          </div>
        </CardHeader>
        <CardContent className="pt-0">
          {result ? (
            <div className="text-sm text-gray-700 space-y-2">
              {result.split('\n').map((paragraph, index) => (
                <p key={index}>{paragraph}</p>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              {isProcessing ? 'Generating summary...' : 'Click the sparkle icon to generate a summary'}
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
export default AiSummaryPanel