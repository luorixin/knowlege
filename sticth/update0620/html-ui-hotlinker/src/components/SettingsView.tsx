import React, { useState } from 'react';
import { Settings, Shield, Sliders, Database, Key, HelpCircle, HardDrive, Cpu, RefreshCw } from 'lucide-react';

export default function SettingsView() {
  const [modelType, setModelType] = useState('Gemini 2.5 Flash');
  const [temp, setTemp] = useState(0.2);
  const [enableOCR, setEnableOCR] = useState(true);
  const [chunkOverlap, setChunkOverlap] = useState(128);
  const [chunkSize, setChunkSize] = useState(512);

  return (
    <div className="font-sans space-y-6 max-w-4xl">
      <div className="border-b border-white/[0.08] pb-4 mb-4">
        <h2 className="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase">
          System Control
        </h2>
        <h1 className="font-display text-4xl font-extrabold text-white tracking-tight mt-1">
          Settings Console
        </h1>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        
        {/* LLM & Model Engine settings */}
        <div className="cyber-panel rounded-2xl p-6 border-white/[0.07] space-y-4">
          <h3 className="text-sm font-mono uppercase tracking-wider text-neon-cyan flex items-center gap-2 font-bold select-none">
            <Cpu className="w-4.5 h-4.5" />
            AI Model Engine Settings
          </h3>

          <div className="space-y-1.5">
            <label className="block text-xs font-mono uppercase text-slate-400">Default Processing Model</label>
            <select
              value={modelType}
              onChange={(e) => setModelType(e.target.value)}
              className="w-full text-xs text-white px-3 py-2 rounded-lg border border-white/[0.08] bg-slate-950/80 focus:outline-none focus:border-neon-cyan text-neon-cyan"
            >
              <option value="Gemini 2.5 Flash">Gemini 2.5 Flash (Preferred)</option>
              <option value="Gemini 2.5 Pro">Gemini 2.5 Pro (Precision)</option>
              <option value="Llama-3-8B-Instruct">Llama 3 8B Instruct (Offline)</option>
              <option value="Mistral-7B-v0.1">Mistral 7B (Legacy)</option>
            </select>
          </div>

          <div className="space-y-1.5 pt-2">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-mono uppercase text-slate-400">Temperature Override</label>
              <span className="text-xs font-mono text-neon-cyan font-bold">{temp}</span>
            </div>
            <input
              type="range"
              min="0"
              max="1"
              step="0.05"
              value={temp}
              onChange={(e) => setTemp(parseFloat(e.target.value))}
              className="w-full h-1.5 bg-slate-950 rounded-lg appearance-none cursor-pointer accent-neon-cyan"
            />
            <div className="flex items-center justify-between text-[9px] font-mono text-slate-500">
              <span>Precise / Codified</span>
              <span>Creative / Loose</span>
            </div>
          </div>
        </div>

        {/* Dense Ingestion / Chunk settings */}
        <div className="cyber-panel rounded-2xl p-6 border-white/[0.07] space-y-4">
          <h3 className="text-sm font-mono uppercase tracking-wider text-neon-purple flex items-center gap-2 font-bold select-none">
            <Database className="w-4.5 h-4.5" />
            Vector Parsing Settings
          </h3>

          <div className="space-y-2 pt-1">
            <label className="flex items-center space-x-2.5 text-xs text-slate-200 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={enableOCR}
                onChange={(e) => setEnableOCR(e.target.checked)}
                className="rounded border-white/10 text-cyan-500 focus:ring-0 bg-slate-950"
              />
              <span className="font-mono">Enable High-Precision OCR</span>
            </label>
            <p className="text-[10px] text-slate-500 pl-7">
              Triggers visual text extraction on embedded charts, diagram scans, and table structures inside PDFs.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-4 pt-1">
            <div className="space-y-1.5">
              <label className="block text-[10px] font-mono uppercase text-slate-400">Chunk Size (Tokens)</label>
              <input
                type="number"
                value={chunkSize}
                onChange={(e) => setChunkSize(parseInt(e.target.value) || 256)}
                className="w-full text-xs text-white px-3 py-1.5 rounded-lg border border-white/[0.08] bg-slate-950 focus:outline-none focus:border-neon-cyan"
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-[10px] font-mono uppercase text-slate-400">Chunk Overlap</label>
              <input
                type="number"
                value={chunkOverlap}
                onChange={(e) => setChunkOverlap(parseInt(e.target.value) || 0)}
                className="w-full text-xs text-white px-3 py-1.5 rounded-lg border border-white/[0.08] bg-slate-950 focus:outline-none focus:border-neon-cyan"
              />
            </div>
          </div>
        </div>

      </div>

      {/* Database Node Stats panel */}
      <div className="cyber-panel rounded-2xl p-6 border-white/[0.07] space-y-4 container mx-auto">
        <h3 className="text-sm font-mono uppercase tracking-wider text-slate-350 flex items-center gap-2 font-bold select-none">
          <HardDrive className="w-4.5 h-4.5 text-slate-500" />
          Hardware Allocations
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-center">
          <div className="bg-slate-950/60 p-3.5 rounded-xl border border-white/[0.04]">
            <span className="block text-[9px] font-mono uppercase tracking-widest text-slate-500 mb-1">Server Ingress</span>
            <span className="text-xs font-bold text-white font-mono">1.25 GB/s</span>
          </div>
          <div className="bg-slate-950/60 p-3.5 rounded-xl border border-white/[0.04]">
            <span className="block text-[9px] font-mono uppercase tracking-widest text-slate-500 mb-1">Vector Index Space</span>
            <span className="text-xs font-bold text-neon-cyan font-mono">5.4 / 10 GB</span>
          </div>
          <div className="bg-slate-950/60 p-3.5 rounded-xl border border-white/[0.04]">
            <span className="block text-[9px] font-mono uppercase tracking-widest text-slate-500 mb-1">Response Latency</span>
            <span className="text-xs font-bold text-emerald-400 font-mono">&#126;140ms</span>
          </div>
          <div className="bg-slate-950/60 p-3.5 rounded-xl border border-white/[0.04]">
            <span className="block text-[9px] font-mono uppercase tracking-widest text-slate-500 mb-1">Worker Partition</span>
            <span className="text-xs font-bold text-white font-mono">NODE_9X9_PART</span>
          </div>
        </div>
      </div>

    </div>
  );
}
