'use client';
import { useState, useEffect } from 'react';
import Sidebar from '@/components/Sidebar';
import ModelStatusBar from '@/components/ModelStatusBar';
import { storage } from '@/lib/storage';

const PROVIDERS = [
  { id: 'tongyi', name: '☁️ 通义千问', models: ['qwen-vl-max', 'qwen-vl-plus', 'qwen-max'] },
  { id: 'deepseek', name: '🦈 DeepSeek', models: ['deepseek-chat', 'deepseek-reasoner'] },
  { id: 'doubao', name: '🫘 豆包', models: ['doubao-1.5-pro-32k', 'doubao-1.5-lite-32k'] },
  { id: 'claude', name: '🧠 Claude', models: ['claude-sonnet-4-20250514', 'claude-opus-4-20250514', 'claude-haiku-4-5-20251001'] },
];

export default function SettingsPage() {
  const [ocrProvider, setOcrProvider] = useState('tongyi');
  const [ocrKey, setOcrKey] = useState('');
  const [ocrModel, setOcrModel] = useState('qwen-vl-max');
  const [textProvider, setTextProvider] = useState('deepseek');
  const [textKey, setTextKey] = useState('');
  const [textModel, setTextModel] = useState('deepseek-chat');
  const [convoProvider, setConvoProvider] = useState('doubao');
  const [convoKey, setConvoKey] = useState('');
  const [convoModel, setConvoModel] = useState('doubao-1.5-pro-32k');
  const [doubaoKey, setDoubaoKey] = useState('');
  const [claudeKey, setClaudeKey] = useState('');
  const [userName, setUserName] = useState('Alex');
  const [saved, setSaved] = useState(false);
  const [modelRefreshKey, setModelRefreshKey] = useState(0);

  useEffect(() => {
    setOcrProvider(storage.getOCRProvider());
    setOcrKey(storage.getOCRKey());
    setOcrModel(storage.getOCRModel());
    setTextProvider(storage.getTextProvider());
    setTextKey(storage.getTextKey());
    setTextModel(storage.getTextModel());
    setConvoProvider(storage.getConvoProvider());
    setConvoKey(storage.getConvoKey());
    setConvoModel(storage.getConvoModel());
    setDoubaoKey(storage.getDoubaoKey());
    setClaudeKey(storage.getClaudeKey());
    setUserName(storage.getUserName());
  }, []);

  const save = () => {
    storage.setOCRProvider(ocrProvider); storage.setOCRKey(ocrKey); storage.setOCRModel(ocrModel);
    storage.setTextProvider(textProvider); storage.setTextKey(textKey); storage.setTextModel(textModel);
    storage.setConvoProvider(convoProvider); storage.setConvoKey(convoKey); storage.setConvoModel(convoModel);
    storage.setDoubaoKey(doubaoKey); storage.setClaudeKey(claudeKey);
    storage.setUserName(userName);
    setSaved(true);
    setModelRefreshKey(k => k + 1);  // trigger re-check
    setTimeout(() => setSaved(false), 2000);
  };

  const ProviderCard = ({ title, hint, provider, keyVal, model, onProvider, onKey, onModel, color }: {
    title: string; hint: string; provider: string; keyVal: string; model: string;
    onProvider: (v: string) => void; onKey: (v: string) => void; onModel: (v: string) => void; color: string;
  }) => (
    <div className="card space-y-3" style={{ borderColor: color + '33' }}>
      <div><div className="text-white font-bold text-sm">{title}</div><div className="text-speaksnap-muted text-[10px]">{hint}</div></div>
      <div className="flex flex-wrap gap-1.5">
        {PROVIDERS.map(p => (
          <button key={p.id} onClick={() => onProvider(p.id)}
            className={`text-[11px] px-2.5 py-1 rounded-full transition-colors ${provider === p.id ? 'text-white font-semibold' : 'text-speaksnap-muted bg-speaksnap-bg'}`}
            style={{ backgroundColor: provider === p.id ? color : undefined }}
          >{p.name}</button>
        ))}
      </div>
      <input className="input-dark text-xs" type="password" placeholder={`输入 API Key...`} value={keyVal} onChange={e => onKey(e.target.value)} />
      <div className="flex flex-wrap gap-1.5">
        {(PROVIDERS.find(p => p.id === provider)?.models || []).map(m => (
          <button key={m} onClick={() => onModel(m)}
            className={`text-[10px] px-2 py-1 rounded-full ${model === m ? 'bg-speaksnap-primary/20 text-speaksnap-secondary-sm font-semibold' : 'text-speaksnap-dim'}`}
          >{m}</button>
        ))}
      </div>
    </div>
  );

  return (
    <div className="flex min-h-screen bg-speaksnap-bg">
      <Sidebar />
      <main className="flex-1 p-6 max-w-xl space-y-4">
        <ModelStatusBar refreshKey={modelRefreshKey} />

        <h2 className="text-xl font-bold text-white">⚙️ 多模型配置</h2>
        <p className="text-xs text-speaksnap-muted -mt-2">每个任务独立选择服务商，自动检测通断状态</p>

        <ProviderCard title="📸 拍照OCR · 图片识别" hint="推荐：阿里通义千问 Qwen-VL-Max" provider={ocrProvider} keyVal={ocrKey} model={ocrModel}
          onProvider={setOcrProvider} onKey={setOcrKey} onModel={setOcrModel} color="#4F46E5" />
        <ProviderCard title="📖 文本分析 · 内容提取" hint="推荐：DeepSeek V3 · ¥1/百万token" provider={textProvider} keyVal={textKey} model={textModel}
          onProvider={setTextProvider} onKey={setTextKey} onModel={setTextModel} color="#22C55E" />
        <ProviderCard title="💬 AI 对话 · 语音练习" hint="推荐：豆包 Doubao · TTS语音合成" provider={convoProvider} keyVal={convoKey} model={convoModel}
          onProvider={setConvoProvider} onKey={setConvoKey} onModel={setConvoModel} color="#F59E0B" />

        <div className="card space-y-3">
          <div className="text-white font-bold text-sm">🔑 备用 Key</div>
          <input className="input-dark text-xs" type="password" placeholder="🫘 豆包 API Key" value={doubaoKey} onChange={e => setDoubaoKey(e.target.value)} />
          <input className="input-dark text-xs" type="password" placeholder="🧠 Claude API Key" value={claudeKey} onChange={e => setClaudeKey(e.target.value)} />
          <input className="input-dark text-xs" placeholder="👤 你的名字" value={userName} onChange={e => setUserName(e.target.value)} />
        </div>

        <button onClick={save} className={saved ? 'btn-primary w-full text-sm opacity-70' : 'btn-primary w-full text-sm'}>
          {saved ? '✅ 已保存' : '💾 保存所有配置'}
        </button>

        <div className="card space-y-1 text-[10px] text-speaksnap-dim">
          <div className="text-speaksnap-text font-bold text-xs mb-1">🔗 API Key 获取地址</div>
          <div>☁️ 通义千问: bailian.console.aliyun.com</div>
          <div>🦈 DeepSeek: platform.deepseek.com/api_keys</div>
          <div>🫘 豆包: console.volcengine.com/ark</div>
          <div>🧠 Claude: console.anthropic.com</div>
        </div>

        <div className="card text-center text-xs text-speaksnap-dim">
          🏰 SpeakSnap v1.0.0 · New Oriental · 早日退休<br/>
          <span className="text-speaksnap-dim/50">数据存储在浏览器 localStorage · 导出导入实现跨设备同步</span>
        </div>
      </main>
    </div>
  );
}
