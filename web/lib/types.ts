export interface WordItem {
  word: string;
  phonetic: string;
  meaning: string;
  example: string;
}

export interface PhraseItem {
  phrase: string;
  meaning: string;
  usage: string;
}

export interface GrammarItem {
  title: string;
  explanation: string;
  example: string;
}

export interface CaseStudyItem {
  title: string;
  scenario: string;
  keyPoints: string[];
  starterDialogue: string;
}

export interface ExtractedContent {
  title: string;
  words: WordItem[];
  phrases: PhraseItem[];
  grammarPoints: GrammarItem[];
  caseStudies: CaseStudyItem[];
}

export interface ScanRecord {
  id: string;
  title: string;
  imageData?: string;
  extractedText: string;
  wordCount: number;
  phraseCount: number;
  grammarCount: number;
  caseStudyCount: number;
  status: 'draft' | 'processed';
  createdAt: number;
  content?: ExtractedContent;
}

export interface LearningPlan {
  id: string;
  scanId: string;
  dayNumber: number;
  title: string;
  description: string;
  taskType: 'word' | 'phrase' | 'dialogue' | 'review';
  estimatedMinutes: number;
  isCompleted: boolean;
}

export interface ChatMessage {
  role: 'ai' | 'user';
  content: string;
  timestamp: number;
}

export interface ModelStatus {
  provider: string;
  name: string;
  status: 'checking' | 'online' | 'offline' | 'error';
  latency?: number;
  error?: string;
}
