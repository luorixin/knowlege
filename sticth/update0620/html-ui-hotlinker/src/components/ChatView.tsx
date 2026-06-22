import React, { useState, useEffect, useRef } from 'react';
import {
  MessageSquare, Plus, Search, Settings, Send, Paperclip, ChevronDown, Check,
  Play, Pause, Volume2, FileText, Globe, ArrowRight, CornerDownLeft, Filter, X
} from 'lucide-react';
import { Conversation, KnowledgeBase, EvidenceSource, ChatMessage } from '../types';

interface ChatViewProps {
  conversations: Conversation[];
  knowledgeBases: KnowledgeBase[];
  evidenceSources: EvidenceSource[];
  activeKbId: string;
  onAddConversation: (kbId: string) => void;
  onSendMessage: (conversationId: string, text: string) => void;
}

export default function ChatView({
  conversations,
  knowledgeBases,
  evidenceSources,
  activeKbId,
  onAddConversation,
  onSendMessage,
}: ChatViewProps) {
  const [activeChatId, setActiveChatId] = useState(conversations[0]?.id || '');
  const [chatSearch, setChatSearch] = useState('');
  const [messageText, setMessageText] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  
  // Sidebar state management for clean adaptive layout
  const [showHistory, setShowHistory] = useState(true);
  const [showEvidence, setShowEvidence] = useState(true);

  // Auto-collapse sidebars on small screens
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 1280) {
        setShowHistory(false);
        setShowEvidence(false);
      } else {
        setShowHistory(true);
        setShowEvidence(true);
      }
    };
    handleResize(); // trigger on initial mount
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Document Type checkboxes
  const [docTypes, setDocTypes] = useState({
    PDF: true,
    Word: true,
    Excel: false,
    Webpage: true,
  });

  // Industry checkboxes
  const [industries, setIndustries] = useState({
    Technology: true,
    Finance: false,
    Healthcare: false,
    Legal: false,
  });

  // Year checkboxes
  const [years, setYears] = useState({
    '2023': true,
    '2022': false,
    '2021': false,
    '2020+': false,
  });

  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const [audioProgress, setAudioProgress] = useState(30);
  const audioIntervalRef = useRef<number | null>(null);

  // References scrolling or highlight state
  const [highlightedEvidenceId, setHighlightedEvidenceId] = useState<string | null>(null);

  const activeChat = conversations.find((c) => c.id === activeChatId) || conversations[0];

  // Filters check count
  const activeFiltersCount = 
    Object.values(docTypes).filter(Boolean).length +
    Object.values(industries).filter(Boolean).length +
    Object.values(years).filter(Boolean).length;

  // Handlers
  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!messageText.trim() || !activeChat) return;

    onSendMessage(activeChat.id, messageText);
    setMessageText('');

    // If typing simulated text, reset audio progress and playing state
    setIsPlayingAudio(false);
    setAudioProgress(0);
  };

  const toggleDocType = (key: keyof typeof docTypes) => {
    setDocTypes({ ...docTypes, [key]: !docTypes[key] });
  };

  const toggleIndustry = (key: keyof typeof industries) => {
    setIndustries({ ...industries, [key]: !industries[key] });
  };

  const toggleYear = (key: keyof typeof years) => {
    setYears({ ...years, [key]: !years[key] });
  };

  // Simulated Moving waveform when audio plays
  useEffect(() => {
    if (isPlayingAudio) {
      audioIntervalRef.current = window.setInterval(() => {
        setAudioProgress((prev) => {
          if (prev >= 100) {
            setIsPlayingAudio(false);
            if (audioIntervalRef.current) clearInterval(audioIntervalRef.current);
            return 100;
          }
          return prev + 1;
        });
      }, 100);
    } else {
      if (audioIntervalRef.current) {
        clearInterval(audioIntervalRef.current);
      }
    }
    return () => {
      if (audioIntervalRef.current) clearInterval(audioIntervalRef.current);
    };
  }, [isPlayingAudio]);

  const handleCitationClick = (citationId: string) => {
    // If the citation panel is collapsed, auto-open it so the user can see!
    setShowEvidence(true);
    setHighlightedEvidenceId(citationId);
    setTimeout(() => {
      setHighlightedEvidenceId(null);
    }, 2500);
  };

  // Dynamic layout calculations
  const leftSpan = showHistory ? 3 : 0;
  const rightSpan = showEvidence ? 3 : 0;
  const centerSpan = 12 - leftSpan - rightSpan;

  const centerColSpanClass = 
    centerSpan === 12 ? 'lg:col-span-12' :
    centerSpan === 9 ? 'lg:col-span-9' : 
    'lg:col-span-6';

  return (
    <div className="font-sans flex flex-col h-[calc(100vh-140px)] min-h-[500px]">
      
      {/* Outer Banner title */}
      <div className="text-center pb-4 border-b border-white/[0.08] mb-4">
        <h1 className="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase">
          Advanced AI Command Chat
        </h1>
      </div>

      {/* Primary Layout splits 3 panels: conversations sidebar, active Chat area, Evidence tracing (Image 3) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5 flex-1 overflow-hidden relative">
        
        {/* Left Panel: Conversations List (Cols 3) */}
        {showHistory && (
          <div className="lg:col-span-3 cyber-panel rounded-2xl p-4 flex flex-col overflow-hidden h-full">
            <button
              onClick={() => onAddConversation(activeKbId)}
              className="w-full py-2.5 px-4 rounded-xl border border-dashed border-cyan-400/30 hover:border-neon-cyan/80 hover:bg-cyan-950/20 text-neon-cyan hover:text-white text-xs font-mono tracking-wider transition-all flex items-center justify-center gap-2 mb-4 active:scale-95 cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>New Chat</span>
            </button>

            {/* Chat Search */}
            <div className="relative mb-4">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
              <input
                type="text"
                placeholder="Search conversations..."
                value={chatSearch}
                onChange={(e) => setChatSearch(e.target.value)}
                className="w-full text-xs text-white pl-9 pr-3 py-2 rounded-lg border border-white/[0.06] bg-slate-950/80 focus:border-neon-cyan focus:outline-none"
              />
            </div>

            {/* Conversation list with category groups */}
            <div className="flex-1 overflow-y-auto space-y-4 pr-1">
              <div className="space-y-1">
                <span className="block text-[9px] font-mono uppercase tracking-widest text-slate-500 px-2 mb-2">History</span>
                
                {conversations
                  .filter((c) => c.title.toLowerCase().includes(chatSearch.toLowerCase()))
                  .map((chat) => {
                    const isActive = chat.id === activeChatId;
                    return (
                      <button
                        key={chat.id}
                        onClick={() => setActiveChatId(chat.id)}
                        className={`w-full text-left p-3 rounded-xl transition-all border flex flex-col justify-between cursor-pointer ${
                          isActive
                            ? 'bg-cyan-950/30 border-neon-cyan/40 shadow-[0_0_12px_rgba(0,240,255,0.06)]'
                            : 'bg-transparent border-transparent hover:bg-white/[0.02]'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-1">
                          <span className={`text-xs font-medium truncate tracking-wide ${isActive ? 'text-white font-semibold' : 'text-slate-300'}`}>
                            {chat.title}
                          </span>
                          {isActive && <div className="w-1.5 h-1.5 rounded-full bg-neon-cyan shadow-[0_0_6px_#00f0ff]" />}
                        </div>
                        <div className="flex items-center justify-between mt-1 text-[10px] font-mono text-slate-500">
                          <span>{chat.time}</span>
                          <span className="text-[9px] opacity-70">
                            {knowledgeBases.find((k) => k.id === chat.selectedKb)?.name || 'Default'}
                          </span>
                        </div>
                      </button>
                    );
                  })}
              </div>
            </div>
          </div>
        )}

        {/* Center Panel: Active Chats & Inputs (Flexible cols based on viewport panel display) */}
        <div className={`${centerColSpanClass} flex flex-col h-full overflow-hidden relative`}>
          
          {/* Header selectors of chat */}
          <div className="cyber-panel rounded-2xl p-4 mb-3 border border-white/[0.06] flex flex-col">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="flex items-center space-x-2">
                <span className="text-xs font-mono text-slate-400">Knowledge Base:</span>
                <div className="relative">
                  <select
                    value={activeChat?.selectedKb || 'kb-4'}
                    onChange={(e) => {
                      if (activeChat) {
                        activeChat.selectedKb = e.target.value;
                      }
                    }}
                    className="bg-slate-950 px-3 py-1.5 rounded-lg border border-white/[0.08] text-xs font-mono text-neon-cyan focus:outline-none pr-8 appearance-none cursor-pointer"
                  >
                    {knowledgeBases.map((kb) => (
                      <option key={kb.id} value={kb.id}>
                        {kb.name}
                      </option>
                    ))}
                  </select>
                  <ChevronDown className="absolute right-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-neon-cyan pointer-events-none" />
                </div>
              </div>

              {/* Dynamic sidebar toggles and filters */}
              <div className="flex items-center space-x-2 shrink-0">
                {/* Toggle Chat History */}
                <button
                  type="button"
                  onClick={() => setShowHistory(!showHistory)}
                  className={`py-1.5 px-3 rounded-lg border text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer ${
                    showHistory
                      ? 'border-neon-cyan bg-cyan-950/20 text-neon-cyan font-bold animate-pulse'
                      : 'border-white/[0.08] hover:border-white/20 text-slate-400'
                  }`}
                  title="Toggle Chat History panel"
                >
                  <MessageSquare className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">History</span>
                </button>
                
                {/* Toggle Evidence Panel */}
                <button
                  type="button"
                  onClick={() => setShowEvidence(!showEvidence)}
                  className={`py-1.5 px-3 rounded-lg border text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer ${
                    showEvidence
                      ? 'border-neon-cyan bg-cyan-950/20 text-neon-cyan font-bold animate-pulse'
                      : 'border-white/[0.08] hover:border-white/20 text-slate-400'
                  }`}
                  title="Toggle Evidence tracing panel"
                >
                  <FileText className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">Evidence</span>
                </button>

                {/* Filter toggle */}
                <button
                  type="button"
                  onClick={() => setShowFilters(!showFilters)}
                  className={`py-1.5 px-3 rounded-lg border text-xs font-mono transition-all flex items-center gap-1.5 cursor-pointer ${
                    showFilters
                      ? 'border-neon-cyan bg-cyan-950/20 text-neon-cyan'
                      : 'border-white/[0.08] hover:border-white/25 text-slate-300'
                  }`}
                  title="Toggle advanced document filters"
                >
                  <Filter className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline">Filters</span>
                  {activeFiltersCount > 0 && (
                    <span className="px-1.5 py-0.2 text-[9px] font-mono font-bold bg-neon-cyan text-slate-950 rounded-full">
                      {activeFiltersCount}
                    </span>
                  )}
                </button>
              </div>
            </div>

            {/* Inline expandable Filters drawer area - absolutely no absolute floats to avoid overlap! */}
            {showFilters && (
              <div className="border-t border-white/[0.08] mt-4 pt-4 grid grid-cols-1 sm:grid-cols-3 gap-4 text-left animate-fadeIn">
                
                {/* Doc Type column */}
                <div className="space-y-2">
                  <span className="block text-[9px] font-mono uppercase tracking-wider text-slate-500 font-bold">Doc Type</span>
                  <div className="space-y-1.5">
                    {Object.keys(docTypes).map((type) => (
                      <label key={type} className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer select-none hover:text-white">
                        <input
                          type="checkbox"
                          checked={docTypes[type as keyof typeof docTypes]}
                          onChange={() => toggleDocType(type as keyof typeof docTypes)}
                          className="w-3.5 h-3.5 rounded border-white/10 text-cyan-500 focus:ring-0 cursor-pointer"
                        />
                        <span className="font-mono">{type}</span>
                      </label>
                    ))}
                  </div>
                </div>

                {/* Industry column */}
                <div className="space-y-2">
                  <span className="block text-[9px] font-mono uppercase tracking-wider text-slate-500 font-bold">Industry</span>
                  <div className="space-y-1.5">
                    {Object.keys(industries).map((ind) => (
                      <label key={ind} className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer select-none hover:text-white">
                        <input
                          type="checkbox"
                          checked={industries[ind as keyof typeof industries]}
                          onChange={() => toggleIndustry(ind as keyof typeof industries)}
                          className="w-3.5 h-3.5 rounded border-white/10 text-cyan-500 focus:ring-0 cursor-pointer"
                        />
                        <span>{ind}</span>
                      </label>
                    ))}
                  </div>
                </div>

                {/* Year column */}
                <div className="space-y-2">
                  <span className="block text-[9px] font-mono uppercase tracking-wider text-slate-500 font-bold">Year</span>
                  <div className="space-y-1.5">
                    {Object.keys(years).map((yr) => (
                      <label key={yr} className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer select-none hover:text-white">
                        <input
                          type="checkbox"
                          checked={years[yr as keyof typeof years]}
                          onChange={() => toggleYear(yr as keyof typeof years)}
                          className="w-3.5 h-3.5 rounded border-white/10 text-cyan-500 focus:ring-0 cursor-pointer"
                        />
                        <span className="font-mono">{yr}</span>
                      </label>
                    ))}
                  </div>
                </div>

              </div>
            )}
          </div>

          {/* Message log content list */}
          <div className="flex-1 overflow-y-auto space-y-4 pr-1 scrollbar-thin mb-3">
            {activeChat?.messages.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center p-8">
                <MessageSquare className="w-10 h-10 text-slate-600 mb-2 animate-bounce" />
                <p className="text-slate-400 text-sm">Send a request payload to begin vector parsing.</p>
                <p className="text-[10px] text-slate-500 font-mono mt-1">E.G. &quot;What is the efficiency curve?&quot;</p>
              </div>
            ) : (
              activeChat?.messages.map((m) => {
                const isUser = m.sender === 'user';
                return (
                  <div key={m.id} className={`flex flex-col ${isUser ? 'items-end' : 'items-start'}`}>
                    
                    {/* Message Box */}
                    {m.isAudio ? (
                      /* Audio waveform message visual - matches Image 3 purple bar exactly */
                      <div className="cyber-panel-purple cyber-glow-purple rounded-2xl p-4 max-w-[85%] w-full">
                        <div className="flex items-center justify-between mb-2 pb-2 border-b border-white/[0.05]">
                          <span className="text-[10px] font-mono uppercase tracking-widest text-[#d858ff] flex items-center gap-1.5 select-none font-bold">
                            <Volume2 className="w-3.5 h-3.5 animate-pulse" />
                            Audio Synthesis Wave
                          </span>
                          <span className="text-[9px] font-mono text-slate-500">Live Stream Output</span>
                        </div>
                        
                        {/* Interactive Speech waveform canvas */}
                        <div className="flex items-center space-x-3.5">
                          <button
                            type="button"
                            onClick={() => setIsPlayingAudio(!isPlayingAudio)}
                            className="p-2.5 rounded-full bg-[#bd00ff]/20 border border-[#bd00ff]/50 text-white hover:bg-[#bd00ff]/40 transition-all flex items-center justify-center active:scale-95 cursor-pointer"
                          >
                            {isPlayingAudio ? <Pause className="w-4.5 h-4.5" /> : <Play className="w-4.5 h-4.5 translate-x-0.5" />}
                          </button>

                          {/* SVG Waveform - animates and stretches */}
                          <div className="flex-1 flex items-center justify-center h-9 px-1 gap-[1px] md:gap-[2px] overflow-hidden">
                            {Array.from({ length: 42 }).map((_, index) => {
                              // Dynamic random scaling when active
                              let scale = isPlayingAudio 
                                ? Math.sin(index * 0.3 * (Date.now() % 20)) * 0.8 + 1 
                                : Math.sin(index * 0.3) * 0.5 + 0.6;
                              if (scale < 0.2) scale = 0.2;
                              return (
                                <span
                                  key={index}
                                  className={`w-0.5 rounded-full transition-all duration-100 shrink-0 ${
                                    isPlayingAudio ? 'bg-[#d260ff]' : 'bg-[#e29bff]/40'
                                  }`}
                                  style={{ height: `${Math.round(20 * scale)}px` }}
                                />
                              );
                            })}
                          </div>
                        </div>

                        {/* Player state progress feedback */}
                        <div className="mt-2.5 flex items-center justify-between text-[9px] font-mono text-slate-400">
                          <span>0:{audioProgress < 10 ? `0${audioProgress}` : audioProgress}</span>
                          <div className="flex-1 mx-3 h-1 bg-white/5 rounded-full overflow-hidden">
                            <div className="h-full bg-gradient-to-r from-neon-purple to-neon-pink" style={{ width: `${audioProgress}%` }} />
                          </div>
                          <span>0:59</span>
                        </div>
                      </div>

                    ) : (
                      /* Standard messages */
                      <div
                        className={`rounded-xl p-4 text-xs leading-relaxed max-w-[90%] border shadow-md ${
                          isUser
                            ? 'bg-cyan-950/20 border-cyan-500/20 text-slate-100 font-sans'
                            : 'cyber-panel hover:bg-slate-900/60 transition-all'
                        }`}
                      >
                        <span className="block text-[10px] font-mono tracking-wider uppercase text-slate-500 mb-1">
                          {isUser ? 'User:' : 'AI:'}
                        </span>

                        {/* Content text - with citation markers parsed as clickable pills */}
                        <div className="font-sans whitespace-pre-line text-slate-200">
                          {m.content.split(/(\[\d+\])/g).map((chunk, idx) => {
                            const match = chunk.match(/\[(\d+)\]/);
                            if (match) {
                              const citationNum = match[1];
                              return (
                                <button
                                  key={idx}
                                  onClick={() => handleCitationClick(citationNum)}
                                  className="inline-block mx-1 font-mono font-bold text-[10px] bg-cyan-950 text-neon-cyan border border-neon-cyan/40 px-1 py-0.2 rounded hover:bg-neon-cyan hover:text-slate-950 transition-colors cursor-pointer select-none shadow-[0_0_6px_rgba(0,240,255,0.2)]"
                                >
                                  {citationNum}
                                </button>
                              );
                            }
                            return chunk;
                          })}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>

          {/* Message input bar with paperclip and send action - matching Image 3 bottom */}
          <form onSubmit={handleSend} className="cyber-panel rounded-2xl p-2 relative flex items-center gap-2 border border-white/[0.08]">
            <button
              type="button"
              onClick={() => {
                alert('Attachment upload window allocated. Import external vectors inside Knowledge Base grid with "Upload" button.');
              }}
              className="p-2 border border-white/[0.05] hover:border-neon-cyan/30 bg-slate-950/80 rounded-xl text-slate-400 hover:text-white transition-all cursor-pointer"
              title="Add context attachment"
            >
              <Paperclip className="w-4 h-4 text-slate-400" />
            </button>
            
            <input
              type="text"
              placeholder="Ask anything about enterprise knowledge..."
              value={messageText}
              onChange={(e) => setMessageText(e.target.value)}
              className="flex-1 bg-transparent px-3 text-xs text-white placeholder:text-slate-600 focus:outline-none"
            />

            <button
              type="submit"
              className="py-2 px-3.5 rounded-xl bg-gradient-to-r from-cyan-500 to-cyan-400 text-slate-950 hover:from-neon-cyan hover:to-cyan-300 transition-all cursor-pointer flex items-center justify-center shrink-0 shadow-[0_0_12px_rgba(0,240,255,0.35)]"
            >
              <Send className="w-3.5 h-3.5" />
            </button>
          </form>
        </div>

        {/* Right Panel: Evidence Tracing (Cols 3) - matches design of Image 3 */}
        {showEvidence && (
          <div className="lg:col-span-3 cyber-panel rounded-2xl p-4 flex flex-col overflow-hidden h-full">
            <div className="flex items-center justify-between pb-3 border-b border-white/[0.08] mb-4">
              <h3 className="text-xs font-mono uppercase tracking-widest text-slate-400">
                Evidence Tracing
              </h3>
              <span className="text-[9px] font-mono text-slate-600 uppercase">Interactive Log</span>
            </div>

            {/* List of citations from matching search */}
            <div className="flex-1 overflow-y-auto space-y-4 pr-1">
              {evidenceSources.map((source) => {
                const isTargetHighlight = highlightedEvidenceId === source.id;
                
                return (
                  <div
                    key={source.id}
                    className={`group p-3.5 rounded-xl border transition-all relative ${
                      isTargetHighlight
                        ? 'border-neon-cyan bg-cyan-950/30 scale-[1.02] shadow-[0_0_20px_rgba(0,240,255,0.25)]'
                        : 'border-white/[0.06] bg-slate-950/40 hover:border-white/15'
                    }`}
                  >
                    {/* Glowing background ripple */}
                    {isTargetHighlight && (
                      <div className="absolute inset-0 bg-cyan-500/[0.02] animate-pulse rounded-xl pointer-events-none" />
                    )}

                    {/* Top line of Evidence source card */}
                    <div className="flex items-start justify-between mb-1.5 gap-2">
                      <span className="flex items-center gap-1.5">
                        {/* Badge indicator */}
                        <span className="font-mono font-bold text-[10px] bg-cyan-950 text-neon-cyan border border-neon-cyan/30 px-1.5 py-0.2 rounded shrink-0">
                          [{source.id}]
                        </span>
                        <span className="text-xs font-semibold text-slate-100 truncate tracking-wide">
                          {source.title}
                        </span>
                      </span>
                      <span className="text-[9px] font-mono text-slate-500 uppercase shrink-0">
                        {source.type}
                      </span>
                    </div>

                    {/* Metadata spec details */}
                    <span className="block text-[8px] font-mono text-slate-500 uppercase tracking-widest mb-2.5">
                      {source.pages} {source.pages > 1 ? 'pages' : 'page'} • Verified Ingestion
                    </span>

                    {/* Excerpt body sentence */}
                    <p className="text-slate-300 text-[10.5px] leading-relaxed mb-3 italic bg-slate-900/40 p-2 rounded-lg border border-white/[0.03]">
                      &ldquo;{source.excerpt}&rdquo;
                    </p>

                    {/* View Source callback link */}
                    <button
                      onClick={() => {
                        alert(`Opening master source stream of [${source.id}] in virtual viewer cell. Ingest verified.`);
                      }}
                      className="text-[9px] font-mono text-neon-cyan/80 group-hover:text-neon-cyan hover:underline transition-all flex items-center justify-end w-full gap-1 cursor-pointer select-none"
                    >
                      <span>View Source</span>
                      <ArrowRight className="w-2.5 h-2.5 group-hover:translate-x-0.5 transition-transform" />
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
