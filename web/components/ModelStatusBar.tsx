'use client';
import { useState, useEffect, useCallback } from 'react';
import { checkAllModels, checkSingleModel } from '@/lib/ai-service';
import type { ModelStatus } from '@/lib/types';
import { storage } from '@/lib/storage';

export default function ModelStatusBar() {
  const [models, setModels] = useState<ModelStatus[]>([]);
  const [expanded, setExpanded] = useState(false);

  const check = useCallback(async () => {
    // Show checking state immediately
    setModels([
      { provider: 'tongyi', name: '通义千问', status: 'checking' },
      { provider: 'deepseek', name: 'DeepSeek', status: 'checking' },
      { provider: 'doubao', name: '豆包', status: 'checking' },
      { provider: 'claude', name: 'Claude', status: 'checking' },
    ]);
    const results = await checkAllModels();
    setModels(results);
  }, []);

  useEffect(() => { check(); }, [check]);

  const icon = (s: string) => s === 'online' ? '🟢' : s === 'checking' ? '🟡' : '🔴';

  return (
    <div className="card">
      <div className="flex items-center justify-between cursor-pointer" onClick={() => setExpanded(!expanded)}>
        <span className="text-sm font-bold text-speaksnap-text">🔗 模型状态检测</span>
        <div className="flex items-center gap-2">
          {models.length === 0 ? (
            <button onClick={(e) => { e.stopPropagation(); check(); }} className="text-xs text-speaksnap-secondary-sm hover:underline">点击检测</button>
          ) : (
            <span className="text-xs text-speaksnap-muted">
              {models.filter(m => m.status === 'online').length}/{models.length} 在线
            </span>
          )}
          <span className="text-xs text-speaksnap-dim">{expanded ? '▲' : '▼'}</span>
        </div>
      </div>
      {expanded && models.length > 0 && (
        <div className="mt-3 space-y-2">
          {models.map(m => (
            <div key={m.provider} className="flex items-center justify-between text-xs">
              <div className="flex items-center gap-2">
                <span className={m.status === 'online' ? 'text-speaksnap-success' : m.status === 'checking' ? 'text-speaksnap-warning animate-pulse' : 'text-speaksnap-error'}>●</span>
                <span className="text-speaksnap-text">{m.name}</span>
                <span className={`text-[10px] ${m.status === 'online' ? 'text-speaksnap-success' : m.status === 'checking' ? 'text-speaksnap-warning' : 'text-speaksnap-error'}`}>
                  {m.status === 'online' ? `${m.latency}ms` : m.status === 'checking' ? '检测中...' : m.error || '离线'}
                </span>
              </div>
              <button onClick={() => checkSingleModel(m.provider)} className="text-speaksnap-dim hover:text-speaksnap-secondary-sm">🔄</button>
            </div>
          ))}
          <button onClick={check} className="w-full text-center text-xs text-speaksnap-secondary-sm hover:underline mt-2">🔄 全部重测</button>
        </div>
      )}
    </div>
  );
}
