'use client';
import { useState, useEffect, useRef } from 'react';
import Sidebar from '@/components/Sidebar';
import ModelStatusBar from '@/components/ModelStatusBar';
import { generateConversation } from '@/lib/ai-service';
import { storage } from '@/lib/storage';
import type { ChatMessage } from '@/lib/types';

export default function ConversationPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMessages(storage.getConversation());
  }, []);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const send = async () => {
    const text = input.trim();
    if (!text || loading) return;
    const userMsg: ChatMessage = { role: 'user', content: text, timestamp: Date.now() };
    const newMsgs = [...messages, userMsg];
    setMessages(newMsgs);
    setInput('');
    setLoading(true);

    try {
      const reply = await generateConversation(null, newMsgs, text);
      const aiMsg: ChatMessage = { role: 'ai', content: reply, timestamp: Date.now() };
      const final = [...newMsgs, aiMsg];
      setMessages(final);
      storage.setConversation(final);
    } catch (e: any) {
      const errMsg: ChatMessage = { role: 'ai', content: `❌ 错误: ${e.message}\n请检查设置里的 API Key。`, timestamp: Date.now() };
      const final = [...newMsgs, errMsg];
      setMessages(final);
      storage.setConversation(final);
    }
    setLoading(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); }
  };

  const clear = () => {
    const init: ChatMessage[] = [{ role: 'ai', content: '对话已清空。有什么我可以帮你的？', timestamp: Date.now() }];
    setMessages(init);
    storage.setConversation(init);
  };

  return (
    <div className="flex min-h-screen bg-speaksnap-bg">
      <Sidebar />
      <main className="flex-1 p-6 flex flex-col max-w-2xl">
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-xl font-bold text-white">💬 AI 对话练习</h2>
          <button onClick={clear} className="text-xs text-speaksnap-dim hover:text-speaksnap-error">清空对话</button>
        </div>
        <ModelStatusBar />

        {/* Chat area */}
        <div className="flex-1 space-y-3 overflow-y-auto mb-4 min-h-[400px] max-h-[60vh]">
          {messages.map((m, i) => (
            <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              <div className={`max-w-[80%] px-4 py-2.5 rounded-2xl text-sm leading-relaxed ${
                m.role === 'user'
                  ? 'bg-speaksnap-primary text-white rounded-br-md'
                  : 'bg-speaksnap-surface border border-speaksnap-border text-speaksnap-text rounded-bl-md'
              }`}>
                <div className={`text-[10px] mb-1 ${m.role === 'user' ? 'text-white/60' : 'text-speaksnap-muted'}`}>
                  {m.role === 'user' ? '👤 You' : '🤖 AI Teacher'}
                </div>
                <div className="whitespace-pre-wrap">{m.content}</div>
              </div>
            </div>
          ))}
          {loading && (
            <div className="text-sm text-speaksnap-muted animate-pulse">🤔 AI 正在思考...</div>
          )}
          <div ref={chatEndRef} />
        </div>

        {/* Input */}
        <div className="flex gap-2 items-center bg-speaksnap-surface border border-speaksnap-border rounded-3xl px-3 py-2">
          <button className="w-10 h-10 rounded-full bg-speaksnap-bg flex items-center justify-center text-lg shrink-0" title="语音输入（即将支持）">🎤</button>
          <input
            className="flex-1 bg-transparent text-white text-sm px-2 outline-none placeholder:text-speaksnap-dim"
            placeholder="输入你的回复... (Enter 发送)"
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={loading}
          />
          <button
            onClick={send}
            disabled={loading || !input.trim()}
            className="w-10 h-10 rounded-full bg-speaksnap-primary flex items-center justify-center text-white shrink-0 disabled:opacity-50"
          >↑</button>
        </div>
        <div className="text-[10px] text-speaksnap-dim text-center mt-2">💡 支持 DeepSeek / 豆包 / Claude · 打字即可练习口语</div>
      </main>
    </div>
  );
}
