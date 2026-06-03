'use client';
import { useState, useEffect } from 'react';
import Sidebar from '@/components/Sidebar';
import ModelStatusBar from '@/components/ModelStatusBar';
import { storage } from '@/lib/storage';
import type { ScanRecord } from '@/lib/types';

export default function HomePage() {
  const [scans, setScans] = useState<ScanRecord[]>([]);
  const [userName, setUserName] = useState('Alex');
  const [streak] = useState(21);
  const [showSync, setShowSync] = useState(false);
  const [syncData, setSyncData] = useState('');

  useEffect(() => {
    setScans(storage.getScans());
    setUserName(storage.getUserName());
  }, []);

  const totalWords = scans.reduce((s, r) => s + r.wordCount, 0);
  const totalPhrases = scans.reduce((s, r) => s + r.phraseCount, 0);
  const totalCases = scans.reduce((s, r) => s + r.caseStudyCount, 0);

  const exportData = () => {
    const data = storage.exportAll();
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = `speaksnap-backup-${new Date().toISOString().slice(0,10)}.json`; a.click();
    URL.revokeObjectURL(url);
  };

  const importData = () => {
    if (!syncData) return;
    storage.importAll(syncData);
    setScans(storage.getScans());
    setShowSync(false);
    alert('数据已导入！刷新页面查看');
  };

  return (
    <div className="flex min-h-screen bg-speaksnap-bg">
      <Sidebar />
      <main className="flex-1 p-6 space-y-4 max-w-3xl">
        <ModelStatusBar />

        {/* Greeting */}
        <div>
          <h2 className="text-2xl font-extrabold text-white">Hi, {userName} 👋</h2>
          <p className="text-speaksnap-muted text-sm">周三 · 2026年6月3日 · 学习第 {streak} 天</p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="card text-center"><div className="text-2xl font-extrabold text-speaksnap-secondary">{totalWords}</div><div className="text-xs text-speaksnap-muted">累计单词</div></div>
          <div className="card text-center"><div className="text-2xl font-extrabold text-speaksnap-secondary">{totalPhrases}</div><div className="text-xs text-speaksnap-muted">短语掌握</div></div>
          <div className="card text-center"><div className="text-2xl font-extrabold text-speaksnap-secondary">{totalCases}</div><div className="text-xs text-speaksnap-muted">Case Study</div></div>
          <div className="card text-center"><div className="text-2xl font-extrabold text-speaksnap-warning">{streak}🔥</div><div className="text-xs text-speaksnap-muted">连续打卡</div></div>
        </div>

        {/* Quick Actions */}
        <div className="grid grid-cols-4 gap-2">
          {[
            { icon: '📸', label: '上传课程', href: '/upload' },
            { icon: '📖', label: '背单词', href: '/flashcards' },
            { icon: '💬', label: 'AI 对话', href: '/conversation' },
            { icon: '⚙️', label: '设置', href: '/settings' },
          ].map(a => (
            <a key={a.href} href={a.href} className="card-clickable text-center py-5 flex flex-col items-center gap-2">
              <span className="text-3xl">{a.icon}</span>
              <span className="text-xs text-speaksnap-text font-semibold">{a.label}</span>
            </a>
          ))}
        </div>

        {/* Sync */}
        <div className="card space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-sm font-bold text-speaksnap-text">🔄 数据同步</span>
            <button onClick={() => setShowSync(!showSync)} className="text-xs text-speaksnap-secondary-sm hover:underline">{showSync ? '收起' : '展开'}</button>
          </div>
          {showSync && (
            <div className="space-y-3">
              <button onClick={exportData} className="btn-outline w-full text-sm">📤 导出数据到文件</button>
              <div>
                <textarea className="input-dark h-20 text-xs" placeholder="粘贴之前导出的 JSON 数据..." value={syncData} onChange={e => setSyncData(e.target.value)} />
                <button onClick={importData} className="btn-primary w-full mt-2 text-sm" disabled={!syncData}>📥 导入数据</button>
              </div>
              <p className="text-[10px] text-speaksnap-dim">💡 导出后可在另一台设备导入，实现数据同步。未来支持自动云同步。</p>
            </div>
          )}
        </div>

        {/* Recent */}
        <div>
          <h3 className="text-speaksnap-text font-bold mb-2">📚 最近课程</h3>
          {scans.length === 0 ? (
            <div className="card text-center py-10 text-speaksnap-muted text-sm">
              <span className="text-4xl block mb-2">📸</span>还没有课程记录<br/>点击「上传课程」开始
            </div>
          ) : (
            <div className="space-y-2">
              {scans.slice(0, 5).map(s => (
                <a key={s.id} href={`/results?id=${s.id}`} className="card-clickable flex items-center gap-3">
                  <span className="text-2xl">{s.imageData ? '📷' : '📝'}</span>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-semibold text-speaksnap-text truncate">{s.title}</div>
                    <div className="text-[10px] text-speaksnap-muted">
                      {new Date(s.createdAt).toLocaleDateString('zh-CN')} · {s.wordCount}词 · {s.phraseCount}短语 · {s.caseStudyCount}Case
                    </div>
                  </div>
                  <span className="tag shrink-0">{s.status === 'processed' ? '已整理' : '待处理'}</span>
                </a>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
