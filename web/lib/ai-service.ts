import type { ExtractedContent, CaseStudyItem, ChatMessage, ModelStatus } from './types';
import { storage } from './storage';

// ========== Model Status Checker — 用真实 chat 请求测通断 ==========
interface ProviderConfig {
  id: string;
  name: string;
  checkUrl: string;
  checkBody: object;
  keys: (() => string)[];  // Try all possible key slots
}

const PROVIDER_CONFIGS: ProviderConfig[] = [
  {
    id: 'tongyi', name: '通义千问',
    checkUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
    checkBody: { model: 'qwen-turbo', messages: [{ role: 'user', content: 'hi' }], max_tokens: 1 },
    keys: [() => storage.getOCRKey(), () => storage.getTextKey(), () => storage.getConvoKey(), () => storage.getDoubaoKey()],
  },
  {
    id: 'deepseek', name: 'DeepSeek',
    checkUrl: 'https://api.deepseek.com/v1/chat/completions',
    checkBody: { model: 'deepseek-chat', messages: [{ role: 'user', content: 'hi' }], max_tokens: 1 },
    keys: [() => storage.getTextKey(), () => storage.getOCRKey(), () => storage.getConvoKey(), () => storage.getDoubaoKey()],
  },
  {
    id: 'doubao', name: '豆包',
    checkUrl: 'https://ark.cn-beijing.volces.com/api/v3/chat/completions',
    checkBody: { model: 'doubao-1.5-lite-32k', messages: [{ role: 'user', content: 'hi' }], max_tokens: 1 },
    keys: [() => storage.getConvoKey(), () => storage.getDoubaoKey(), () => storage.getTextKey(), () => storage.getOCRKey()],
  },
  {
    id: 'claude', name: 'Claude',
    checkUrl: 'https://api.anthropic.com/v1/messages',
    checkBody: { model: 'claude-haiku-4-5-20251001', max_tokens: 1, messages: [{ role: 'user', content: 'hi' }] },
    keys: [() => storage.getClaudeKey(), () => storage.getOCRKey(), () => storage.getTextKey()],
  },
];

function findKey(config: ProviderConfig): string {
  for (const fn of config.keys) {
    const k = fn();
    if (k && k.trim().length > 5) return k.trim();
  }
  return '';
}

export async function checkAllModels(): Promise<ModelStatus[]> {
  const results: ModelStatus[] = [];

  for (const cfg of PROVIDER_CONFIGS) {
    const key = findKey(cfg);
    if (!key) {
      results.push({ provider: cfg.id, name: cfg.name, status: 'offline', error: '未配置 API Key' });
      continue;
    }

    results.push({ provider: cfg.id, name: cfg.name, status: 'checking' });
    const start = Date.now();
    try {
      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      if (cfg.id === 'claude') {
        headers['x-api-key'] = key;
        headers['anthropic-version'] = '2023-06-01';
      } else {
        headers['Authorization'] = `Bearer ${key}`;
      }

      const resp = await fetch(cfg.checkUrl, {
        method: 'POST',
        headers,
        body: JSON.stringify(cfg.checkBody),
        signal: AbortSignal.timeout(10000),
      });

      const latency = Date.now() - start;
      if (resp.ok) {
        results[results.length - 1] = {
          provider: cfg.id, name: cfg.name, status: 'online', latency,
        };
      } else {
        const txt = await resp.text().catch(() => '');
        const short = txt.slice(0, 120);
        results[results.length - 1] = {
          provider: cfg.id, name: cfg.name, status: 'error',
          error: short.includes('invalid') || short.includes('Incorrect') ? 'Key 无效' :
                 short.includes('quota') || short.includes('balance') ? '余额不足' :
                 short.includes('rate') ? '请求过快' :
                 `HTTP ${resp.status}`,
        };
      }
    } catch (e: any) {
      const msg = e.message || '';
      results[results.length - 1] = {
        provider: cfg.id, name: cfg.name, status: 'offline',
        error: msg.includes('timeout') || msg.includes('abort') ? '连接超时' :
               msg.includes('network') || msg.includes('fetch') ? '网络不通' : msg.slice(0, 60),
      };
    }
  }
  return results;
}

export async function checkSingleModel(provider: string): Promise<ModelStatus> {
  const results = await checkAllModels();
  return results.find(r => r.provider === provider) || { provider, name: provider, status: 'error', error: 'Unknown' };
}

// ========== OCR: analyze image ==========
export async function analyzeImage(file: File): Promise<ExtractedContent> {
  const provider = storage.getOCRProvider();
  const key = storage.getOCRKey();
  const model = storage.getOCRModel();
  if (!key) throw new Error('请在设置中配置拍照OCR的 API Key');

  const base64 = await fileToBase64(file);

  switch (provider) {
    case 'tongyi': return callTongyi(key, model, base64, 'image');
    case 'deepseek': return callDeepSeek(storage.getTextKey() || key, storage.getTextModel() || model, base64, 'image');
    case 'claude': return callClaude(storage.getClaudeKey() || key, base64);
    default: return callTongyi(key, model, base64, 'image');
  }
}

// ========== Text analysis ==========
export async function analyzeText(text: string, title: string): Promise<ExtractedContent> {
  const provider = storage.getTextProvider();
  const key = storage.getTextKey();
  const model = storage.getTextModel();
  if (!key) throw new Error('请在设置中配置文本分析的 API Key');

  switch (provider) {
    case 'deepseek': return callDeepSeek(key, model, `Title: ${title}\n\nContent:\n${text}`, 'text');
    case 'tongyi': return callTongyi(storage.getOCRKey() || key, storage.getOCRModel() || 'qwen-max', `Title: ${title}\n\nContent:\n${text}`, 'text');
    case 'claude': return callClaudeText(storage.getClaudeKey() || key, text, title);
    default: return callDeepSeek(key, model, `Title: ${title}\n\nContent:\n${text}`, 'text');
  }
}

// ========== Conversation ==========
export async function generateConversation(
  caseStudy: CaseStudyItem | null,
  history: ChatMessage[],
  userMessage: string
): Promise<string> {
  const provider = storage.getConvoProvider();
  const key = storage.getConvoKey();
  const model = storage.getConvoModel();
  if (!key) throw new Error('请在设置中配置对话的 API Key');

  const systemPrompt = `You are a native English teacher. ${caseStudy ? `Scenario: ${caseStudy.title}. ${caseStudy.scenario}. Keywords: ${caseStudy.keyPoints.join(', ')}.` : 'Practice English conversation.'} 2-4 sentences, gently correct mistakes with 💡 Tip. Supportive tone.`;

  const messages: { role: string; content: string }[] = [
    { role: 'system', content: systemPrompt },
    ...history.map(h => ({ role: h.role === 'ai' ? 'assistant' as const : 'user' as const, content: h.content })),
    { role: 'user', content: userMessage },
  ];

  switch (provider) {
    case 'doubao': return callOpenAI('https://ark.cn-beijing.volces.com/api/v3/chat/completions', key, model, messages);
    case 'deepseek': return callOpenAI('https://api.deepseek.com/v1/chat/completions', key, model, messages);
    case 'claude': return callClaudeConvo(storage.getClaudeKey() || key, messages, caseStudy);
    default: return callOpenAI('https://ark.cn-beijing.volces.com/api/v3/chat/completions', key, model, messages);
  }
}

// ========== Provider implementations ==========

async function callOpenAI(baseUrl: string, key: string, model: string, messages: { role: string; content: string }[]): Promise<string> {
  const resp = await fetch(baseUrl, {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${key}` },
    body: JSON.stringify({ model, messages, max_tokens: 1024, temperature: 0.8 }),
    signal: AbortSignal.timeout(30000),
  });
  if (!resp.ok) {
    const txt = await resp.text().catch(() => '');
    throw new Error(`API error ${resp.status}: ${txt.slice(0, 200)}`);
  }
  const data = await resp.json();
  return data.choices?.[0]?.message?.content || '';
}

async function callTongyi(key: string, model: string, input: string, type: 'image' | 'text'): Promise<ExtractedContent> {
  const messages = type === 'image'
    ? [{ role: 'user', content: [{ type: 'image_url', image_url: { url: `data:image/jpeg;base64,${input}` } }, { type: 'text', text: 'Extract all English learning content as JSON' }] }]
    : [{ role: 'user', content: input }];

  const resp = await fetch('https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions', {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${key}` },
    body: JSON.stringify({ model, messages: [{ role: 'system', content: OCR_PROMPT }, ...messages], max_tokens: 4096 }),
    signal: AbortSignal.timeout(60000),
  });
  if (!resp.ok) throw new Error(`通义千问 HTTP ${resp.status}: ${(await resp.text()).slice(0, 150)}`);
  const data = await resp.json();
  return parseExtracted(data.choices?.[0]?.message?.content || '');
}

async function callDeepSeek(key: string, model: string, input: string, type: 'image' | 'text'): Promise<ExtractedContent> {
  const messages = type === 'image'
    ? [{ role: 'user', content: [{ type: 'image_url', image_url: { url: `data:image/jpeg;base64,${input}` } }, { type: 'text', text: 'Extract all English learning content as JSON' }] }]
    : [{ role: 'user', content: input }];

  const resp = await fetch('https://api.deepseek.com/v1/chat/completions', {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${key}` },
    body: JSON.stringify({ model, messages: [{ role: 'system', content: OCR_PROMPT }, ...messages], max_tokens: 4096 }),
    signal: AbortSignal.timeout(60000),
  });
  if (!resp.ok) throw new Error(`DeepSeek HTTP ${resp.status}: ${(await resp.text()).slice(0, 150)}`);
  const data = await resp.json();
  return parseExtracted(data.choices?.[0]?.message?.content || '');
}

async function callClaude(key: string, base64: string): Promise<ExtractedContent> {
  const resp = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'x-api-key': key, 'anthropic-version': '2023-06-01' },
    body: JSON.stringify({
      model: 'claude-sonnet-4-20250514', max_tokens: 4096, system: OCR_PROMPT,
      messages: [{ role: 'user', content: [
        { type: 'image', source: { type: 'base64', media_type: 'image/jpeg', data: base64 } },
        { type: 'text', text: 'Extract all English learning content as JSON.' },
      ] }],
    }),
    signal: AbortSignal.timeout(60000),
  });
  if (!resp.ok) throw new Error(`Claude HTTP ${resp.status}`);
  const data = await resp.json();
  return parseExtracted(data.content?.[0]?.text || '');
}

async function callClaudeText(key: string, text: string, title: string): Promise<ExtractedContent> {
  const resp = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'x-api-key': key, 'anthropic-version': '2023-06-01' },
    body: JSON.stringify({
      model: 'claude-sonnet-4-20250514', max_tokens: 4096, system: TEXT_PROMPT,
      messages: [{ role: 'user', content: [{ type: 'text', text: `Title: ${title}\n\nContent:\n${text}` }] }],
    }),
    signal: AbortSignal.timeout(60000),
  });
  if (!resp.ok) throw new Error(`Claude HTTP ${resp.status}`);
  const data = await resp.json();
  return parseExtracted(data.content?.[0]?.text || '');
}

async function callClaudeConvo(key: string, messages: { role: string; content: string }[], cs: CaseStudyItem | null): Promise<string> {
  const resp = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'x-api-key': key, 'anthropic-version': '2023-06-01' },
    body: JSON.stringify({
      model: 'claude-sonnet-4-20250514', max_tokens: 1024,
      system: `Native English teacher. ${cs ? `Scenario: ${cs.title}. ${cs.scenario}.` : ''} 2-4 sentences, gentle corrections, 💡 Tips.`,
      messages: messages.filter(m => m.role !== 'system').map(m => ({ role: m.role, content: m.content })),
    }),
    signal: AbortSignal.timeout(30000),
  });
  if (!resp.ok) throw new Error(`Claude HTTP ${resp.status}`);
  const data = await resp.json();
  return data.content?.[0]?.text || '';
}

// ========== Helpers ==========

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve((reader.result as string).split(',')[1]);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

function parseExtracted(text: string): ExtractedContent {
  try {
    const cleaned = text.replace(/```json\s*/g, '').replace(/```\s*/g, '').trim();
    const j = JSON.parse(cleaned);
    return { title: j.title || '', words: j.words || [], phrases: j.phrases || [], grammarPoints: j.grammar_points || [], caseStudies: j.case_studies || [] };
  } catch {
    return { title: '解析中...', words: [{ word: 'parse_error', phonetic: '', meaning: text.slice(0, 300), example: '' }], phrases: [], grammarPoints: [], caseStudies: [] };
  }
}

const OCR_PROMPT = `You are an expert English teacher. Extract ALL English learning content from the image/text. Return ONLY valid JSON:
{
  "title": "Unit title",
  "words": [{"word":"...","phonetic":"/.../","meaning":"Chinese meaning","example":"Example sentence"}],
  "phrases": [{"phrase":"...","meaning":"Chinese meaning","usage":"When to use"}],
  "grammar_points": [{"title":"Grammar","explanation":"Chinese explanation","example":"Example"}],
  "case_studies": [{"title":"Case","scenario":"Scenario description","key_points":["..."],"starter_dialogue":"Opening line"}]
}
Extract EVERY word visible. Include phonetics. Chinese meanings required. Only JSON.`;

const TEXT_PROMPT = `You are an expert English teacher. Structure the text into learning content. Return ONLY valid JSON with words, phrases, grammar_points, case_studies. All items must have Chinese meanings.`;
