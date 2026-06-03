'use client';
import { useState, useEffect } from 'react';
import Sidebar from '@/components/Sidebar';
import { storage } from '@/lib/storage';
import type { WordItem, ScanRecord } from '@/lib/types';

export default function FlashcardsPage() {
  const [words, setWords] = useState<WordItem[]>([]);
  const [index, setIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [mistakeWords, setMistakeWords] = useState<WordItem[]>([]);
  const [showMistakes, setShowMistakes] = useState(false);
  const [scans, setScans] = useState<ScanRecord[]>([]);

  useEffect(() => {
    const s = storage.getScans();
    setScans(s);
    // Collect all words from processed scans
    const allWords: WordItem[] = [];
    s.filter(r => r.status === 'processed' && r.content).forEach(r => {
      allWords.push(...(r.content!.words || []));
    });
    setWords(allWords);
    // Load mistake words from localStorage
    const stored = storage.get('_mistakes', '[]');
    try { setMistakeWords(JSON.parse(stored)); } catch {}
  }, []);

  const word = words[index];
  const total = words.length;

  const flip = () => setFlipped(!flipped);
  const next = () => { if (index < total - 1) { setIndex(index + 1); setFlipped(false); } };
  const prev = () => { if (index > 0) { setIndex(index - 1); setFlipped(false); } };

  const markHard = () => {
    if (!word) return;
    const updated = [...mistakeWords.filter(w => w.word !== word.word), word];
    setMistakeWords(updated);
    localStorage.setItem('_mistakes', JSON.stringify(updated));
    next();
  };

  const speak = (text: string) => {
    if ('speechSynthesis' in window) {
      const u = new SpeechSynthesisUtterance(text);
      u.lang = 'en-US'; u.rate = 0.85;
      speechSynthesis.speak(u);
    }
  };

  return (
    <div className="flex min-h-screen bg-speaksnap-bg">
      <Sidebar />
      <main className="flex-1 p-6 max-w-lg mx-auto space-y-4">
        <h2 className="text-xl font-bold text-white">📖 背单词</h2>

        {!word ? (
          <div className="card text-center py-16 text-speaksnap-muted">
            <div className="text-5xl mb-4">📖</div>
            <div>还没有单词<br/>请先上传课程材料</div>
            <a href="/upload" className="btn-primary inline-block mt-4 text-sm">📸 去上传</a>
          </div>
        ) : (
          <>
            {/* Progress */}
            <div className="flex items-center justify-between text-xs text-speaksnap-muted">
              <span>{index + 1} / {total}</span>
              <span className="text-speaksnap-dim">👆 点击卡片翻转</span>
            </div>
            <div className="flex gap-1 justify-center">
              {words.slice(0, Math.min(total, 18)).map((_, i) => (
                <div key={i} className={`w-2 h-2 rounded-full ${i < index ? 'bg-speaksnap-success' : i === index ? 'bg-speaksnap-secondary-sm shadow-[0_0_6px_#818CF8]' : 'bg-speaksnap-border'}`} />
              ))}
            </div>

            {/* Card */}
            <div
              onClick={flip}
              className="card min-h-[260px] flex flex-col items-center justify-center cursor-pointer select-none hover:border-speaksnap-primary transition-all"
            >
              {!flipped ? (
                <>
                  <div className="flex items-center gap-2">
                    <div className="text-4xl font-extrabold text-white">{word.word}</div>
                    <button onClick={e => { e.stopPropagation(); speak(word.word); }} className="w-10 h-10 rounded-full bg-speaksnap-primary/20 border border-speaksnap-primary/30 text-speaksnap-secondary-sm text-lg flex items-center justify-center hover:bg-speaksnap-primary/30">🔊</button>
                  </div>
                  {word.phonetic && <div className="text-speaksnap-secondary-md mt-2">{word.phonetic}</div>}
                </>
              ) : (
                <>
                  <div className="text-xl text-speaksnap-text text-center">{word.meaning}</div>
                  {word.example && <div className="text-sm text-speaksnap-muted italic mt-4 text-center">"{word.example}"</div>}
                </>
              )}
            </div>

            {/* Actions */}
            <div className="flex gap-2">
              <button onClick={markHard} className="flex-1 py-3 rounded-xl border border-speaksnap-error/30 text-speaksnap-error text-sm font-bold hover:bg-speaksnap-error/10">😣 记不住</button>
              <button onClick={next} className="flex-1 py-3 rounded-xl border border-speaksnap-primary/30 text-speaksnap-secondary-sm text-sm font-bold hover:bg-speaksnap-primary/10">👍 知道了</button>
              <button onClick={next} className="flex-1 py-3 rounded-xl border border-speaksnap-success/30 text-speaksnap-success text-sm font-bold hover:bg-speaksnap-success/10">✅ 太简单</button>
            </div>

            {/* Mistake book */}
            <button onClick={() => setShowMistakes(!showMistakes)} className="text-xs text-speaksnap-dim hover:text-speaksnap-secondary-sm block mx-auto">
              📝 错题本 ({mistakeWords.length})
            </button>
            {showMistakes && mistakeWords.length > 0 && (
              <div className="card space-y-2">
                {mistakeWords.map((w, i) => (
                  <div key={i} className="flex items-center justify-between text-sm">
                    <div>
                      <span className="text-speaksnap-text font-semibold">{w.word}</span>
                      {w.phonetic && <span className="text-speaksnap-dim ml-2 text-xs">{w.phonetic}</span>}
                    </div>
                    <span className="text-xs text-speaksnap-muted">{w.meaning}</span>
                  </div>
                ))}
                <button onClick={() => { setIndex(0); setShowMistakes(false); }} className="btn-outline w-full text-xs">🔄 复习错题</button>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
