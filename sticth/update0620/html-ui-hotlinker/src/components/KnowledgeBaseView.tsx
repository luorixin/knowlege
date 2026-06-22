import React, { useState } from 'react';
import { Database, Plus, Search, RefreshCw, UploadCloud, MessageSquare, Shield, Globe, HardDrive, CheckCircle2 } from 'lucide-react';
import { KnowledgeBase } from '../types';

interface KnowledgeBaseViewProps {
  knowledgeBases: KnowledgeBase[];
  onAddKnowledgeBase: (kb: { name: string; description: string; isPrivate: boolean }) => void;
  onRefreshKbs: () => void;
  onTriggerChat: (kbId: string) => void;
  onTriggerUpload: (kb: KnowledgeBase, file: File) => void;
}

export default function KnowledgeBaseView({
  knowledgeBases,
  onAddKnowledgeBase,
  onRefreshKbs,
  onTriggerChat,
  onTriggerUpload,
}: KnowledgeBaseViewProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newKbName, setNewKbName] = useState('');
  const [newKbDesc, setNewKbDesc] = useState('');
  const [newKbPrivate, setNewKbPrivate] = useState(false);
  const [activeUploadKb, setActiveUploadKb] = useState<KnowledgeBase | null>(null);
  const [uploadProgress, setUploadProgress] = useState(-1);
  const [dragActive, setDragActive] = useState(false);

  const filteredKbs = knowledgeBases.filter(
    (kb) =>
      kb.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      kb.description.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleSubmitNewKb = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newKbName.trim()) return;
    onAddKnowledgeBase({
      name: newKbName,
      description: newKbDesc,
      isPrivate: newKbPrivate,
    });
    setNewKbName('');
    setNewKbDesc('');
    setNewKbPrivate(false);
    setIsModalOpen(false);
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0] && activeUploadKb) {
      const file = e.dataTransfer.files[0];
      handleUploadStep(activeUploadKb, file);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0] && activeUploadKb) {
      const file = e.target.files[0];
      handleUploadStep(activeUploadKb, file);
    }
  };

  const handleUploadStep = (kb: KnowledgeBase, file: File) => {
    setUploadProgress(0);
    const interval = setInterval(() => {
      setUploadProgress((prev) => {
        if (prev >= 100) {
          clearInterval(interval);
          setTimeout(() => {
            onTriggerUpload(kb, file);
            setActiveUploadKb(null);
            setUploadProgress(-1);
          }, 600);
          return 100;
        }
        return prev + 25;
      });
    }, 200);
  };

  return (
    <div className="space-y-6 font-sans">
      {/* Header section (Matches Image 2) */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/[0.08] pb-6">
        <div>
          <h1 className="font-display text-2xl md:text-3xl font-bold text-white tracking-wide flex items-center gap-2">
            <span className="text-neon-cyan drop-shadow-[0_0_8px_rgba(0,240,255,0.4)]">Knowledge Base List</span>
          </h1>
          <p className="text-xs text-slate-400 font-mono uppercase tracking-wider mt-1.5 flex items-center gap-2">
            <span className="inline-block w-1.5 h-1.5 rounded-full bg-neon-cyan animate-pulse"></span>
            Tenant ID: <span className="text-neon-cyan">T-98765</span>
          </p>
        </div>

        {/* Buttons */}
        <div className="flex items-center space-x-3 self-start sm:self-center">
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-4 py-2 border border-neon-cyan/40 hover:border-neon-cyan rounded-xl bg-cyan-950/20 text-neon-cyan text-sm font-medium transition-all shadow-lg shadow-cyan-950/40 flex items-center gap-2 active:scale-95 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>New Knowledge Base</span>
          </button>
          
          <button
            onClick={onRefreshKbs}
            className="p-2.5 border border-white/10 hover:border-neon-cyan/30 rounded-xl bg-slate-900/60 text-slate-400 hover:text-white transition-all cursor-pointer"
            title="Refresh Grid"
          >
            <RefreshCw className="w-4.5 h-4.5" />
          </button>
        </div>
      </div>

      {/* Search Bar section */}
      <div className="relative max-w-sm">
        <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4.5 h-4.5 text-slate-500" />
        <input
          type="text"
          placeholder="Filter repositories..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full text-sm text-white pl-10 pr-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-950/60 focus:border-neon-cyan focus:outline-none transition-all placeholder:text-slate-500"
        />
      </div>

      {/* Grid view of cards - matches layouts of Image 2 */}
      {filteredKbs.length === 0 ? (
        <div className="cyber-panel rounded-xl p-12 text-center border border-white/[0.05]">
          <Database className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <p className="text-slate-400 text-sm font-medium">No knowledge bases found matching the query.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredKbs.map((kb) => (
            <div
              key={kb.id}
              className="group relative cyber-panel rounded-2xl p-6 border border-white/[0.07] transition-all duration-300 hover:translate-y-[-2px] hover:border-neon-cyan/40 hover:shadow-[0_8px_30px_rgb(0,0,0,0.4)] flex flex-col justify-between"
            >
              {/* Highlight background element */}
              <div className="absolute inset-0 bg-gradient-to-br from-cyan-500/[0.02] to-transparent rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none" />

              <div>
                {/* Visual Header of Card */}
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center space-x-3">
                    <div className="p-2.5 rounded-xl bg-cyan-950/40 text-neon-cyan group-hover:text-white group-hover:bg-gradient-to-r group-hover:from-cyan-600 group-hover:to-cyan-400 transition-all duration-300">
                      <Database className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="font-display font-semibold text-white tracking-wide text-md">
                        {kb.name}
                      </h3>
                      <p className="text-[11px] text-slate-500 font-mono mt-0.5 uppercase tracking-wider">
                        {kb.documentCount} Documents
                      </p>
                    </div>
                  </div>

                  {/* Active Radio Ripple indicator - Matches top right of Image 2 */}
                  {kb.activeStatus ? (
                    <div className="relative flex h-3 w-3 mr-1 mt-1">
                      <span className="animate-signal-wave absolute inline-flex h-full w-full rounded-full bg-cyan-400/60 opacity-75"></span>
                      <span className="relative inline-flex rounded-full h-3 w-3 bg-neon-cyan border border-cyan-300/40"></span>
                    </div>
                  ) : (
                    <span className="block w-2.5 h-2.5 rounded-full bg-slate-700 border border-slate-500" />
                  )}
                </div>

                {/* Description */}
                <p className="text-slate-300 text-xs leading-relaxed mb-6 font-sans">
                  {kb.description}
                </p>
              </div>

              {/* Badges & Action Buttons */}
              <div className="mt-auto space-y-4">
                {/* Privacy indicator */}
                <div className="flex items-center text-[11px] font-mono text-slate-400 uppercase tracking-widest gap-2 pb-4 border-b border-white/[0.05]">
                  {kb.isPrivate ? (
                    <>
                      <Shield className="w-3.5 h-3.5 text-neon-purple" />
                      <span>Private Archive</span>
                    </>
                  ) : (
                    <>
                      <Globe className="w-3.5 h-3.5 text-neon-cyan" />
                      <span>Tenant Public</span>
                    </>
                  )}
                </div>

                {/* Hot Buttons Grid - matches [Upload] [Chat] layout exactly */}
                <div className="grid grid-cols-2 gap-3">
                  <button
                    onClick={() => setActiveUploadKb(kb)}
                    className="flex items-center justify-center space-x-1 px-3 py-2 border border-white/[0.08] hover:border-cyan-500/40 hover:bg-cyan-950/20 rounded-xl text-xs font-mono text-slate-300 hover:text-white transition-all duration-200 cursor-pointer"
                  >
                    <UploadCloud className="w-3.5 h-3.5 text-slate-500 group-hover:text-white" />
                    <span>Upload</span>
                  </button>
                  <button
                    onClick={() => onTriggerChat(kb.id)}
                    className="flex items-center justify-center space-x-1 px-3 py-2 border border-white/[0.08] hover:border-purple-500/40 hover:bg-purple-950/20 rounded-xl text-xs font-mono text-slate-300 hover:text-white transition-all duration-200 cursor-pointer"
                  >
                    <MessageSquare className="w-3.5 h-3.5 text-slate-500" />
                    <span>Chat</span>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* New Knowledge Base Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-white/10 rounded-2xl max-w-md w-full p-6 md:p-8 shadow-2xl relative">
            <h2 className="font-display text-xl font-bold text-white mb-4">Create Knowledge Base</h2>
            <form onSubmit={handleSubmitNewKb} className="space-y-4">
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1">
                  Name
                </label>
                <input
                  type="text"
                  value={newKbName}
                  onChange={(e) => setNewKbName(e.target.value)}
                  placeholder="e.g. Legal Agreements"
                  className="w-full text-sm text-white px-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1">
                  Description
                </label>
                <textarea
                  value={newKbDesc}
                  onChange={(e) => setNewKbDesc(e.target.value)}
                  placeholder="Summarize the files and scope of this directory..."
                  rows={3}
                  className="w-full text-sm text-white px-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none"
                  required
                />
              </div>

              <div className="flex items-center space-x-3 pt-2">
                <input
                  type="checkbox"
                  id="privacy-toggle"
                  checked={newKbPrivate}
                  onChange={(e) => setNewKbPrivate(e.target.checked)}
                  className="w-4.5 h-4.5 rounded text-neon-cyan bg-slate-950 border-white/[0.08] focus:ring-0 focus:ring-offset-0"
                />
                <label htmlFor="privacy-toggle" className="text-xs font-mono uppercase text-slate-300 tracking-wide cursor-pointer user-select-none">
                  Make Private (Gated Access)
                </label>
              </div>

              <div className="flex items-center justify-end space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl border border-white/[0.08] text-xs font-mono text-slate-400 hover:text-white tracking-wider cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl cyber-btn-cyan text-xs font-mono tracking-wider cursor-pointer"
                >
                  Create Repository
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* File Upload Modal Drawer */}
      {activeUploadKb && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-white/10 rounded-2xl max-w-lg w-full p-6 shadow-2xl relative overflow-hidden">
            <h2 className="font-display text-xl font-bold text-white mb-2 flex items-center gap-2">
              <UploadCloud className="text-neon-cyan w-5 h-5" />
              <span>Ingest Document to {activeUploadKb.name}</span>
            </h2>
            <p className="text-xs text-slate-400 mb-6">
              Uploaded articles trigger live indexing and telemetry vectors that can be verified inside the <span className="text-neon-cyan font-semibold uppercase font-mono">Telemetry tab</span>.
            </p>

            {uploadProgress === -1 ? (
              <div
                onDragEnter={handleDrag}
                onDragOver={handleDrag}
                onDragLeave={handleDrag}
                onDrop={handleDrop}
                className={`border-2 border-dashed rounded-2xl p-8 text-center transition-all ${
                  dragActive ? 'border-neon-cyan bg-cyan-950/20' : 'border-white/[0.08] hover:border-neon-cyan/40 bg-slate-950/40'
                }`}
              >
                <HardDrive className="w-10 h-10 text-slate-500 mx-auto mb-4" />
                <p className="text-xs text-slate-300 font-medium mb-1">
                  Drag and drop file here, or click to browse
                </p>
                <p className="text-[10px] text-slate-500 font-mono uppercase mb-4">
                  PDF, DOCX, XLSX up to 50MB
                </p>
                <label className="inline-block px-4 py-2 rounded-xl border border-white/[0.1] text-xs font-mono text-white hover:border-neon-cyan bg-slate-900 cursor-pointer active:scale-95 transition-all">
                  Browse Files
                  <input
                    type="file"
                    className="hidden"
                    onChange={handleFileSelect}
                    accept=".pdf,.docx,.xlsx,.txt"
                  />
                </label>
              </div>
            ) : (
              <div className="p-8 text-center space-y-4">
                <div className="relative w-16 h-16 mx-auto flex items-center justify-center">
                  <div className="absolute inset-0 rounded-full border-4 border-white/5" />
                  <div
                    className="absolute inset-0 rounded-full border-4 border-neon-cyan border-t-transparent animate-spin"
                    style={{ animationDuration: '0.8s' }}
                  />
                  <span className="text-xs font-mono font-bold text-neon-cyan">{uploadProgress}%</span>
                </div>
                <p className="text-xs font-mono uppercase text-slate-300 tracking-wider">
                  Transferring raw byte stream...
                </p>
                <p className="text-[10px] text-slate-500">
                  Allocating memory cells on remote worker node
                </p>
              </div>
            )}

            <div className="flex justify-end mt-6">
              <button
                type="button"
                onClick={() => setActiveUploadKb(null)}
                className="px-4 py-2 rounded-xl border border-white/[0.08] text-xs font-mono text-slate-400 hover:text-white tracking-wider cursor-pointer"
                disabled={uploadProgress > -1}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
