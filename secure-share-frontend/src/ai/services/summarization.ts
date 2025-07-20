import { HfInference } from '@huggingface/inference';

// Better to use environment variable for the token
const HF_TOKEN = "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";
const hf = new HfInference(HF_TOKEN);

export const summarizeDocument = async (text: string) => {
  try {
   // Validate and clean the input text
    if (!text) throw new Error('No text provided for summarization');

    const cleanedText = text.trim();
    if (cleanedText.length < 50) {
      throw new Error('Text too short for summarization (minimum 50 characters)');
    }

    // Ensure text doesn't exceed model's maximum length
    const MAX_INPUT_LENGTH = 1024;
    const truncatedText = cleanedText.length > MAX_INPUT_LENGTH
      ? cleanedText.substring(0, MAX_INPUT_LENGTH)
      : cleanedText;

    const response = await hf.summarization({
      model: 'facebook/bart-large-cnn',
      inputs: text,
      parameters: {
        max_length: 150,
        min_length: 30,
        do_sample: false // For more deterministic results
      }
    });

     if (!response?.summary_text) {
          throw new Error('Invalid summary response from API');
     }

    return response.summary_text;
  } catch (error) {
    console.error('Summarization error:', error);
    throw new Error(`AI summary failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
  }
};