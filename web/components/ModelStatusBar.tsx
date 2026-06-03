'use client';
import { useState, useEffect, useCallback } from 'react';
import { checkAllModels, checkSingleModel } from '@/lib/ai-service';
import type { ModelStatus } from '@/lib/types';

export default function ModelStatusBar({ refreshKey }: { refreshKey?: number }) {
  const [models, setModels] = useState<ModelStatus[]>([]);
  const [expanded, setExpanded] = useState(true); // default open

  const check = useCallback(async () => {
    setModels([
      { provider: 'tongyi', name: '通义千问', status: 'checking' },
      { provider: 'deepseek', name: 'DeepSeek', status: 'checking' },
      { provider: 'doubao', name: '豆包', status: 'checking' },
      { provider: 'claude', name: 'Claude', status: 'checking' },
    ]);
    const results = await checkAllModels();
    setModels(results);
  }, []);

  // Auto-check on mount & when refreshKey changes (settings save triggers this)
  useEffect(() => { check(); }, [check, refreshKey]);

  const onlineCount = models.filter(m => m.status === 'online').length;
  const hasError = models.some(m => m.status === 'error' || m.status === 'offline');
  const allDone = models.length > 0 && models.every(m => m.status !== 'checking');

  return (
    <div className="card">
      <div className="flex items-center justify-between cursor-pointer select-none" onClick={() => setExpanded(!expanded)}>
        <span className="text-sm font-bold text-speaksnap-text">
          🔗 模型状态
          {allDone && (
            <span className={`ml-2 text-xs ${onlineCount === models.length ? 'text-speaksnap-success' : hasError ? 'text-speaksnap-warning' : 'text-speaksnap-error'}`}>
              {onlineCount}/{models.length} 在线
            </span>
          )}
        </span>
        <div className="flex items-center gap-2">
          {models.length === 0 && (
            <button onClick={e => { e.stopPropagation(); check(); }} className="text-xs text-speaksnap-secondary-sm hover:underline">点击检测</button>
          )}
          <span className="text-xs text-speaksnap-dim">{expanded ? '▲' : '▼'}</span>
        </div>
      </div>
      {expanded && models.length > 0 && (
        <div className="mt-3 space-y-2">
          {models.map(m => (
            <div key={m.provider} className="flex items-center justify-between text-xs py-0.5">
              <div className="flex items-center gap-2 min-w-0">
                <span className={m.status === 'online' ? 'text-speaksnap-success' : m.status === 'checking' ? 'text-speaksnap-warning animate-pulse' : 'text-speaksnap-error'}>●</span>
                <span className="text-speaksnap-text font-medium">{m.name}</span>
                <span className={`text-[10px] truncate ${
                  m.status === 'online' ? 'text-speaksnap-success' :
                  m.status === 'checking' ? 'text-speaksnap-warning' :
                  m.status === 'error' ? 'text-speaksnap-warning' : 'text-speaksnap-error'
                }`}>
                  {m.status === 'online' ? `${m.latency}ms` :
                   m.status === 'checking' ? '检测中...' :
                   m.error || '离线'}
                </span>
              </div>
              <button onClick={e => { e.stopPropagation(); checkSingleModel(m.provider); }} className="text-speaksnap-dim hover:text-speaksnap-secondary-sm shrink-0 ml-2">🔄</button>
            </div>
          ))}
          <button onClick={check} className="w-full text-center text-xs text-speaksnap-secondary-sm hover:underline pt-1">🔄 全部重测</button>
        </div>
      )}
    </div>
  );
}
