import type { ScanRecord, ExtractedContent, LearningPlan, ChatMessage } from './types';

const STORAGE_PREFIX = 'speaksnap_';

function safeGet<T>(key: string, fallback: T): T {
  if (typeof window === 'undefined') return fallback;
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + key);
    return raw ? JSON.parse(raw) : fallback;
  } catch { return fallback; }
}

function safeSet(key: string, value: unknown) {
  if (typeof window === 'undefined') return;
  localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(value));
}

export const storage = {
  // --- API Keys ---
  getOCRProvider: () => safeGet('ocr_provider', 'tongyi'),
  setOCRProvider: (v: string) => safeSet('ocr_provider', v),
  getOCRKey: () => safeGet('ocr_key', ''),
  setOCRKey: (v: string) => safeSet('ocr_key', v),
  getOCRModel: () => safeGet('ocr_model', 'qwen-vl-max'),
  setOCRModel: (v: string) => safeSet('ocr_model', v),

  getTextProvider: () => safeGet('text_provider', 'deepseek'),
  setTextProvider: (v: string) => safeSet('text_provider', v),
  getTextKey: () => safeGet('text_key', ''),
  setTextKey: (v: string) => safeSet('text_key', v),
  getTextModel: () => safeGet('text_model', 'deepseek-chat'),
  setTextModel: (v: string) => safeSet('text_model', v),

  getConvoProvider: () => safeGet('convo_provider', 'doubao'),
  setConvoProvider: (v: string) => safeSet('convo_provider', v),
  getConvoKey: () => safeGet('convo_key', ''),
  setConvoKey: (v: string) => safeSet('convo_key', v),
  getConvoModel: () => safeGet('convo_model', 'doubao-1.5-pro-32k'),
  setConvoModel: (v: string) => safeSet('convo_model', v),

  getDoubaoKey: () => safeGet('doubao_key', ''),
  setDoubaoKey: (v: string) => safeSet('doubao_key', v),
  getClaudeKey: () => safeGet('claude_key', ''),
  setClaudeKey: (v: string) => safeSet('claude_key', v),

  getUserName: () => safeGet('user_name', 'Alex'),
  setUserName: (v: string) => safeSet('user_name', v),

  // --- Data ---
  getScans: (): ScanRecord[] => safeGet('scans', [] as ScanRecord[]),
  setScans: (v: ScanRecord[]) => safeSet('scans', v),

  getConversation: (): ChatMessage[] => safeGet('conversation', [
    { role: 'ai', content: '你好！我是 AI 英语老师。上传课程材料，或直接开始对话！', timestamp: Date.now() }
  ] as ChatMessage[]),
  setConversation: (v: ChatMessage[]) => safeSet('conversation', v),

  // Generic access (used by components)
  get: (key: string, fallback: string) => safeGet(key, fallback),
  set: (key: string, v: string) => safeSet(key, v),

  // --- Export/Import for sync ---
  exportAll: () => {
    const data: Record<string, unknown> = {};
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (k?.startsWith(STORAGE_PREFIX)) {
        data[k.replace(STORAGE_PREFIX, '')] = JSON.parse(localStorage.getItem(k) || 'null');
      }
    }
    return JSON.stringify(data, null, 2);
  },
  importAll: (json: string) => {
    const data = JSON.parse(json);
    for (const [k, v] of Object.entries(data)) {
      safeSet(k, v);
    }
  },
};
