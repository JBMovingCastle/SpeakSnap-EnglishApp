'use client';
import { useState, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Sidebar from '@/components/Sidebar';
import ModelStatusBar from '@/components/ModelStatusBar';
import { analyzeImage, analyzeText } from '@/lib/ai-service';
import { storage } from '@/lib/storage';
import type { ScanRecord, ExtractedContent } from '@/lib/types';

type Status = 'idle' | 'processing' | 'done' | 'error';

export default function UploadPage() {
  const router = useRouter();
  const [status, setStatus] = useState<Status>('idle');
  const [progress, setProgress] = useState(0);
  const [progressText, setProgressText] = useState('');
  const [error, setError] = useState('');
  const [preview, setPreview] = useState<string | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [manualTitle, setManualTitle] = useState('');
  const [manualText, setManualText] = useState('');
  const [showManual, setShowManual] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFile = useCallback((f: File) => {
    if (!f.type.startsWith('image/')) { alert('请选择图片文件'); return; }
    setFile(f);
    const reader = new FileReader();
    reader.onload = e => setPreview(e.target?.result as string);
    reader.readAsDataURL(f);
  }, []);

  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault(); setDragOver(false);
    const f = e.dataTransfer.files[0];
    if (f) handleFile(f);
  }, [handleFile]);

  const process = async () => {
    setStatus('processing'); setProgress(0.1); setProgressText('准备中...');
    try {
      let content: ExtractedContent;
      if (file) {
        setProgress(0.2); setProgressText('上传图片...');
        setProgress(0.35); setProgressText('AI 识别单词中...');
        content = await analyzeImage(file);
      } else if (manualText) {
        setProgress(0.3); setProgressText('AI 分析文本中...');
        content = await analyzeText(manualText, manualTitle || '手动输入');
      } else {
        throw new Error('请选择文件或输入文本');
      }
      setProgress(0.7); setProgressText('保存结果...');

      const scan: ScanRecord = {
        id: Date.now().toString(),
        title: content.title || manualTitle || `课程 ${new Date().toLocaleDateString('zh-CN')}`,
        imageData: preview || undefined,
        extractedText: manualText,
        wordCount: content.words.length,
        phraseCount: content.phrases.length,
        grammarCount: content.grammarPoints.length,
        caseStudyCount: content.caseStudies.length,
        status: 'processed',
        createdAt: Date.now(),
        content,
      };
      const scans = storage.getScans();
      scans.unshift(scan);
      storage.setScans(scans);

      setProgress(1); setProgressText('✅ 完成！');
      setStatus('done');
      setTimeout(() => router.push(`/results?id=${scan.id}`), 500);
    } catch (e: any) {
      setStatus('error'); setError(e.message || '处理失败');
    }
  };

  return (
    <div className="flex min-h-screen bg-speaksnap-bg">
      <Sidebar />
      <main className="flex-1 p-6 space-y-4 max-w-2xl">
        <ModelStatusBar />

        <h2 className="text-xl font-bold text-white">📸 上传课程内容</h2>

        {/* Upload zone */}
        <div
          className={`upload-zone ${dragOver ? 'border-speaksnap-primary bg-speaksnap-card' : ''}`}
          onDragOver={e => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={onDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          {preview ? (
            <img src={preview} alt="Preview" className="max-h-48 mx-auto rounded-lg mb-2" />
          ) : (
            <div className="text-5xl mb-3">📤</div>
          )}
          <div className="text-speaksnap-text font-bold">{file ? file.name : '拖拽文件到这里 / 点击选择'}</div>
          <div className="text-speaksnap-dim text-xs mt-1">支持图片 (JPG/PNG) · 最大 10MB</div>
          <input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={e => e.target.files?.[0] && handleFile(e.target.files[0])} />
        </div>

        {/* Other options */}
        <div className="grid grid-cols-3 gap-2">
          <button onClick={() => fileInputRef.current?.click()} className="card-clickable text-center py-4">
            <div className="text-2xl">📷</div><div className="text-[10px] text-speaksnap-text font-semibold mt-1">选择图片文件</div>
          </button>
          <button onClick={() => setShowManual(!showManual)} className="card-clickable text-center py-4">
            <div className="text-2xl">📝</div><div className="text-[10px] text-speaksnap-text font-semibold mt-1">手动输入笔记</div>
          </button>
          <button onClick={() => alert('即将支持拍摄功能')} className="card-clickable text-center py-4">
            <div className="text-2xl">📋</div><div className="text-[10px] text-speaksnap-text font-semibold mt-1">拍摄目录</div>
          </button>
        </div>

        {/* Manual input */}
        {showManual && (
          <div className="card space-y-3">
            <h3 className="text-speaksnap-text font-bold">📝 手动输入笔记</h3>
            <input className="input-dark" placeholder="课程名称（如：Week 12 · Business English）" value={manualTitle} onChange={e => setManualTitle(e.target.value)} />
            <textarea className="input-dark h-32" placeholder="粘贴笔记内容...&#10;例如：&#10;- 新单词：negotiate, compromise, deadline&#10;- 短语：reach an agreement&#10;- 语法点：虚拟语气" value={manualText} onChange={e => setManualText(e.target.value)} />
          </div>
        )}

        {/* Process button */}
        <button onClick={process} disabled={status === 'processing' || (!file && !manualText)} className="btn-primary w-full disabled:opacity-50">
          🚀 开始 AI 整理
        </button>

        {/* Progress */}
        {status === 'processing' && (
          <div className="card text-center space-y-3">
            <div className="text-3xl animate-pulse">⚙️</div>
            <div className="text-sm text-speaksnap-text font-bold">AI 正在整理...</div>
            <div className="text-xs text-speaksnap-muted">{progressText}</div>
            <div className="h-2 bg-speaksnap-surface rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-speaksnap-primary to-speaksnap-accent rounded-full transition-all duration-500" style={{ width: `${progress * 100}%` }} />
            </div>
            <div className="text-xs text-speaksnap-dim">{Math.round(progress * 100)}%</div>
          </div>
        )}

        {status === 'done' && (
          <div className="card text-center text-speaksnap-success font-bold">✅ 整理完成！跳转中...</div>
        )}
        {status === 'error' && (
          <div className="card border-speaksnap-error/30 bg-speaksnap-error/10">
            <div className="text-speaksnap-error font-bold">❌ {error}</div>
            <button onClick={() => setStatus('idle')} className="btn-outline mt-2 text-sm">重试</button>
          </div>
        )}
      </main>
    </div>
  );
}
