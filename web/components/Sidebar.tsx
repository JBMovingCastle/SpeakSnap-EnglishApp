'use client';
import { usePathname } from 'next/navigation';

const pages = [
  { path: '/', label: '🏠 首页', key: '' },
  { path: '/upload', label: '📸 上传', key: 'upload' },
  { path: '/flashcards', label: '📖 背单词', key: 'flashcards' },
  { path: '/conversation', label: '💬 对话', key: 'conversation' },
  { path: '/settings', label: '⚙️ 设置', key: 'settings' },
];

export default function Sidebar() {
  const pathname = usePathname();
  return (
    <nav className="w-56 shrink-0 bg-speaksnap-surface border-r border-speaksnap-border p-4 flex flex-col gap-1 min-h-screen">
      <div className="gradient-header rounded-xl mb-4 text-center">
        <div className="text-lg font-bold">🏰 SpeakSnap</div>
        <div className="text-xs text-white/70">New Oriental · 早日退休</div>
      </div>
      {pages.map(p => (
        <a key={p.key} href={p.path} className={`flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm transition-colors ${
          pathname === p.path ? 'bg-speaksnap-primary/20 text-speaksnap-secondary-sm font-semibold' : 'text-speaksnap-muted hover:bg-speaksnap-card'
        }`}>{p.label}</a>
      ))}
    </nav>
  );
}
