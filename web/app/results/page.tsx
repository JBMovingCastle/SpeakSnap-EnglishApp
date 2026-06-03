'use client';
import { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Sidebar from '@/components/Sidebar';
import { storage } from '@/lib/storage';
import type { ScanRecord } from '@/lib/types';

function ResultsContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [scan, setScan] = useState<ScanRecord | null>(null);
  const id = searchParams.get('id');

  useEffect(() => {
    if (id) {
      const scans = storage.getScans();
      setScan(scans.find(s => s.id === id) || null);
    }
  }, [id]);

  if (!scan) return <div className="card text-center py-10"><span className="text-speaksnap-muted">加载中...</span></div>;
  if (!scan.content) return <div className="card text-center py-10"><span className="text-speaksnap-muted">此记录暂无内容，请重新上传</span></div>;

  const c = scan.content;
  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold text-white">📋 {c.title}</h2>

      {/* Overview */}
      <div className="card">
        <div className="text-sm font-bold text-speaksnap-secondary mb-2">📊 内容概览</div>
        <div className="flex flex-wrap gap-1.5">
          <span className="tag">📖 {c.words.length} 单词</span>
          <span className="tag">💬 {c.phrases.length} 短语</span>
          <span className="tag">📐 {c.grammarPoints.length} 语法点</span>
          <span className="tag">🎯 {c.caseStudies.length} Case Study</span>
        </div>
      </div>

      {/* Words */}
      {c.words.length > 0 && (
        <div className="card">
          <div className="text-sm font-bold text-speaksnap-text mb-2">📖 核心词汇 ({c.words.length})</div>
          <div className="flex flex-wrap gap-2">
            {c.words.map((w, i) => (
              <div key={i} className="bg-speaksnap-surface border border-speaksnap-border rounded-full px-3 py-1.5 text-xs text-speaksnap-text cursor-pointer hover:bg-speaksnap-border">{w.word}{w.phonetic && <span className="text-speaksnap-dim ml-1">{w.phonetic}</span>}</div>
            ))}
          </div>
        </div>
      )}

      {/* Phrases */}
      {c.phrases.length > 0 && (
        <div className="card">
          <div className="text-sm font-bold text-speaksnap-text mb-2">💬 短语 ({c.phrases.length})</div>
          {c.phrases.map((p, i) => (
            <div key={i} className="text-sm text-speaksnap-text py-1">• <b>{p.phrase}</b> — {p.meaning}</div>
          ))}
        </div>
      )}

      {/* Grammar */}
      {c.grammarPoints.length > 0 && (
        <div className="card">
          <div className="text-sm font-bold text-speaksnap-text mb-2">📐 语法 ({c.grammarPoints.length})</div>
          {c.grammarPoints.map((g, i) => (
            <div key={i} className="text-sm text-speaksnap-text py-1.5">
              <div className="font-bold">{i+1}️⃣ {g.title}</div>
              <div className="text-speaksnap-muted text-xs">{g.explanation}</div>
              {g.example && <div className="text-speaksnap-dim text-xs italic pl-2">{g.example}</div>}
            </div>
          ))}
        </div>
      )}

      {/* Case Studies */}
      {c.caseStudies.length > 0 && (
        <div className="card">
          <div className="text-sm font-bold text-speaksnap-text mb-2">🎯 Case Study ({c.caseStudies.length})</div>
          {c.caseStudies.map((cs, i) => (
            <div key={i} className="bg-speaksnap-surface rounded-lg p-3 mb-2 border-l-2 border-speaksnap-primary">
              <div className="text-sm font-bold text-speaksnap-text">{cs.title}</div>
              <div className="text-xs text-speaksnap-muted">{cs.scenario}</div>
              {cs.starterDialogue && <div className="text-xs text-speaksnap-dim italic mt-1">"{cs.starterDialogue}"</div>}
              <button onClick={() => router.push('/conversation')} className="mt-2 text-xs text-speaksnap-secondary-sm hover:underline">💬 以此场景开始对话 →</button>
            </div>
          ))}
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-3">
        <button onClick={() => router.push('/flashcards')} className="btn-primary flex-1 text-sm">📖 开始背单词</button>
        <button onClick={() => router.push('/conversation')} className="btn-outline flex-1 text-sm">💬 AI 对话</button>
      </div>

      {/* JSON export */}
      <details className="card">
        <summary className="text-xs text-speaksnap-dim cursor-pointer">🔍 查看原始 JSON</summary>
        <pre className="text-[10px] text-speaksnap-muted mt-2 overflow-auto max-h-64">{JSON.stringify(c, null, 2)}</pre>
      </details>
    </div>
  );
}

export default function ResultsPage() {
  return (
    <div className="flex min-h-screen bg-speaksnap-bg">
      <Sidebar />
      <main className="flex-1 p-6 max-w-3xl">
        <Suspense fallback={<div className="card text-center py-10 text-speaksnap-muted">加载中...</div>}>
          <ResultsContent />
        </Suspense>
      </main>
    </div>
  );
}
