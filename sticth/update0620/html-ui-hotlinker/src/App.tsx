import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Database, MessageSquare, Shield, Settings, LogOut, Cpu, Activity,
  ChevronDown, HelpCircle, Power, UserCheck, Menu, X
} from 'lucide-react';

// View Imports
import LoginView from './components/LoginView';
import KnowledgeBaseView from './components/KnowledgeBaseView';
import ChatView from './components/ChatView';
import TelemetryView from './components/TelemetryView';
import PermissionsView from './components/PermissionsView';
import SettingsView from './components/SettingsView';

// Seed Mock Data & Types
import {
  KnowledgeBase, Conversation, EvidenceSource, TelemetryTask, TeamMember, ChatMessage
} from './types';
import {
  INITIAL_KNOWLEDGE_BASES, INITIAL_CONVERSATIONS, INITIAL_EVIDENCE_SOURCES,
  INITIAL_TELEMETRY_TASKS, INITIAL_TEAM_MEMBERS
} from './mockData';

export default function App() {
  // Session Authentication state
  const [user, setUser] = useState<string | null>(null);

  // Responsive mobile menu toggle state
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  // Tab Sidebar states
  // 'KB' (Knowledge Base), 'Chat' (RAG Q&A), 'Telemetry' (Task Telemetry), 'Permissions', 'Settings'
  const [activeTab, setActiveTab] = useState<'KB' | 'Chat' | 'Telemetry' | 'Permissions' | 'Settings'>('KB');

  // Application database list states
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>(INITIAL_KNOWLEDGE_BASES);
  const [telemetryTasks, setTelemetryTasks] = useState<TelemetryTask[]>(INITIAL_TELEMETRY_TASKS);
  const [conversations, setConversations] = useState<Conversation[]>(INITIAL_CONVERSATIONS);
  const [evidenceSources, setEvidenceSources] = useState<EvidenceSource[]>(INITIAL_EVIDENCE_SOURCES);
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>(INITIAL_TEAM_MEMBERS);
  const [chatActiveKbId, setChatActiveKbId] = useState<string>('kb-4');

  // Global Toast Alert Feedback System
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'warn' | 'info' } | null>(null);

  const showToast = (message: string, type: 'success' | 'warn' | 'info' = 'success') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast(null);
    }, 3500);
  };

  // Automated incremental progress updates for live "Running" telemetry tasks
  useEffect(() => {
    const progressTimer = setInterval(() => {
      setTelemetryTasks((prevTasks) =>
        prevTasks.map((task) => {
          if (task.status === 'Running') {
            const nextProgress = task.progress + Math.floor(Math.random() * 8) + 3;
            if (nextProgress >= 100) {
              // Task completes! Let's schedule a toast and chain next stages
              setTimeout(() => {
                showToast(`[TELEMETRY] ${task.documentName} ingestion task successfully compiled at 100%.`, 'success');
              }, 200);

              // If completed Parsing, sometimes chain an embedding queue!
              if (task.stage === 'Parsing') {
                // Return completed layout and spawn next stage later
                return {
                  ...task,
                  progress: 100,
                  status: 'Completed',
                  logs: [
                    ...task.logs,
                    `[SUCCESS] - Document raw byte trees fully parsed and mapped.`,
                  ],
                };
              }

              return {
                ...task,
                status: 'Completed',
                progress: 100,
                logs: [...task.logs, `[SUCCESS] - Target node coordinates registered on workspace database.`],
              };
            }

            // Still running - append logs occasionally
            const newLogs = [...task.logs];
            if (Math.random() > 0.75) {
              newLogs.push(`[INDEXING] Chunk generation offset block [${task.progress}%] processed.`);
            }

            return {
              ...task,
              progress: nextProgress,
              logs: newLogs,
            };
          }
          return task;
        })
      );
    }, 1800);

    return () => clearInterval(progressTimer);
  }, []);

  // Handlers
  const handleLogin = (username: string) => {
    setUser(username);
    showToast(`Access granted. Virtual cell allocated for user "${username}"`, 'success');
  };

  const handleAddKnowledgeBase = (newKb: { name: string; description: string; isPrivate: boolean }) => {
    const id = `kb-${Date.now()}`;
    const added: KnowledgeBase = {
      id,
      name: newKb.name,
      description: newKb.description,
      isPrivate: newKb.isPrivate,
      activeStatus: true,
      documentCount: 0,
    };
    setKnowledgeBases((prev) => [added, ...prev]);
    showToast(`Created Knowledge Base "${newKb.name}" successfully`, 'success');
  };

  const handleRefreshKbs = () => {
    setKnowledgeBases((prev) =>
      prev.map((kb) => ({
        ...kb,
        activeStatus: Math.random() > 0.15, // fluctuate status for dynamic feeling
      }))
    );
    showToast('Knowledge indices synchronized with virtual clusters.', 'info');
  };

  const handleTriggerChatFromKb = (kbId: string) => {
    setChatActiveKbId(kbId);
    setActiveTab('Chat');
  };

  const handleTriggerUploadFromKb = (kb: KnowledgeBase, file: File) => {
    // Increment file count in KB list
    setKnowledgeBases((prev) =>
      prev.map((k) => (k.id === kb.id ? { ...k, documentCount: k.documentCount + 1 } : k))
    );

    // Create a new telemetry worker parsing task
    const taskId = `task-${Date.now()}`;
    const newTask: TelemetryTask = {
      id: taskId,
      stage: 'Parsing',
      documentName: file.name,
      status: 'Running',
      progress: 0,
      modelName: 'Llama-3-8B-Instruct',
      retryCount: 0,
      logs: [
        `[INFO] - Initializing telemetry stream for ingested file: ${file.name}`,
        `[INFO] - File size reported: ${(file.size / 1024).toFixed(2)} KB`,
        `[INFO] - Running byte integrity verification.`,
      ],
    };

    setTelemetryTasks((prev) => [newTask, ...prev]);
    showToast(`Document ingestion task scheduled for "${file.name}"`, 'success');
  };

  // Chat Responses Answering Engine - high fidelity mockup with smart citations
  const handleChatSendMessage = (conversationId: string, text: string) => {
    // 1. Appends your user message
    const userMsg: ChatMessage = {
      id: `usr-${Date.now()}`,
      sender: 'user',
      content: text,
    };

    setConversations((prev) =>
      prev.map((c) => (c.id === conversationId ? { ...c, messages: [...c.messages, userMsg] } : c))
    );

    // 2. Scheduled Clever Simulated response depending on query words
    setTimeout(() => {
      let replyText = "Understood. Searching database index records... I could not find a exact keyword match but I will compile general compliance logs.";
      let citations: string[] = [];

      const query = text.toLowerCase();
      if (query.includes('findings') || query.includes('findings') || query.includes('project x') || query.includes('report')) {
        replyText = "Ingestion analysis indicates efficiency gains of 15% were registered under the phase 2 protocol [1]. However, database and cluster connection timeouts [2] were reported under elevated peak stress.";
        citations = ['1', '2'];
      } else if (query.includes('onboard') || query.includes('benefit') || query.includes('hr')) {
        replyText = "According to HR onboarding logs [3], all employee packages contain medical coverage details and standard operational guidelines. Key protocols specify local storage of digital files.";
        citations = ['3'];
      } else if (query.includes('security') || query.includes('compliance') || query.includes('tls')) {
        replyText = "All multi-tenant queues are gated with TLS 1.3 encryption matrices [4] to secure parsing offsets. Direct client extraction units operate with virtual security contexts.";
        citations = ['4'];
      } else if (query.includes('hi') || query.includes('hello')) {
        replyText = "Greetings. I am your RAG Knowledge Base intelligence agent. You can ask me to fetch and summarize key parameters from Product Documentation, Sales Playbooks, or Global compliance archives.";
      } else {
        replyText = "Based on indexed records in this Knowledge Base [1], we have synchronized active files to minimize query loops. Let me know if you would like me to compile details of these coordinates.";
        citations = ['1'];
      }

      const aiMsg: ChatMessage = {
        id: `ai-${Date.now()}`,
        sender: 'assistant',
        content: replyText,
        citations,
      };

      setConversations((prev) =>
        prev.map((c) => (c.id === conversationId ? { ...c, messages: [...c.messages, aiMsg] } : c))
      );
      showToast('AI response generated with evidence tracing citations.', 'info');
    }, 1500);
  };

  const handleAddNewConversation = (kbId: string) => {
    const convoId = `chat-${Date.now()}`;
    const newConvo: Conversation = {
      id: convoId,
      title: `Virtual Search Node #${conversations.length + 1}`,
      time: 'Just now',
      selectedKb: kbId,
      messages: [],
    };
    setConversations((prev) => [newConvo, ...prev]);
    showToast('New search log node successfully allocated.', 'success');
  };

  // Telemetry row actions
  const handleTriggerTaskAction = (taskId: string, action: 'retry' | 'pause' | 'execute') => {
    setTelemetryTasks((prev) =>
      prev.map((task) => {
        if (task.id === taskId) {
          if (action === 'retry') {
            return {
              ...task,
              status: 'Running',
              progress: 10,
              retryCount: task.retryCount + 1,
              logs: [...task.logs, `[RETRY RUN] - Client node re-assignment initiated. Flushing corrupt buffer pools.`],
            };
          }
          if (action === 'pause') {
            return {
              ...task,
              status: 'Pending',
              logs: [...task.logs, `[PAUSED] - Core processing unit paused action.`],
            };
          }
          if (action === 'execute') {
            return {
              ...task,
              status: 'Running',
              progress: Math.max(5, task.progress),
              logs: [...task.logs, `[RESUMED] - Telemetry runner worker thread re-allocated.`],
            };
          }
        }
        return task;
      })
    );
    showToast(`Task modification protocol completed.`, 'info');
  };

  // Permissions management callbacks
  const handleAddMember = (m: Omit<TeamMember, 'id'>) => {
    const newMember: TeamMember = {
      ...m,
      id: `member-${Date.now()}`,
    };
    setTeamMembers((prev) => [...prev, newMember]);
    showToast(`Member "${m.username}" authorized on security policy directories.`, 'success');
  };

  const handleUpdateMember = (m: TeamMember) => {
    setTeamMembers((prev) => prev.map((old) => (old.id === m.id ? m : old)));
    showToast(`Security policies modified for "${m.username}".`, 'success');
  };

  const handleRemoveMember = (memberId: string) => {
    const target = teamMembers.find((m) => m.id === memberId);
    setTeamMembers((prev) => prev.filter((old) => old.id !== memberId));
    if (target) {
      showToast(`Member "${target.username}" access revoked on workspace nodes.`, 'warn');
    }
  };

  // Render Login view directly if the user is not authenticated yet
  if (!user) {
    return <LoginView onLoginSuccess={handleLogin} />;
  }

  return (
    <div className="relative min-h-screen bg-[#02050f] text-slate-100 flex flex-col md:flex-row overflow-hidden font-sans select-none cyber-grid-overlay">
      
      {/* Mobile Sticky Header Bar - Only visible on small/medium screens to conserve space */}
      <div className="md:hidden flex items-center justify-between p-4 bg-[#050b18] border-b border-[#00f0ff]/10 w-full shrink-0 z-40 relative">
        <div className="flex items-center space-x-3 select-none">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center border border-neon-cyan/20">
            <Cpu className="w-4 h-4 text-neon-cyan animate-pulse" />
          </div>
          <div>
            <span className="block font-display font-black text-xs tracking-wider text-white uppercase">Enterprise KB</span>
          </div>
        </div>
        <button
          onClick={() => setIsSidebarOpen(!isSidebarOpen)}
          className="p-1.5 border border-[#00f0ff]/20 bg-slate-950/80 rounded-lg text-neon-cyan hover:text-white hover:border-neon-cyan/50 transition-all cursor-pointer flex items-center justify-center select-none"
          title="Toggle Navigation Menu"
        >
          {isSidebarOpen ? <X className="w-4.5 h-4.5" /> : <Menu className="w-4.5 h-4.5" />}
        </button>
      </div>

      {/* Sidebar - Matching Layout side panel design inside image 2,4,5 */}
      <aside className={`w-full md:w-64 bg-[#050b18] border-r border-[#00f0ff]/10 flex-col shrink-0 z-30 transition-all duration-200 ${
        isSidebarOpen ? 'flex' : 'hidden md:flex'
      }`}>
        
        {/* Branding header block */}
        <div className="p-6 pb-2 border-b border-white/[0.05] flex items-center space-x-3 select-none">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-cyan-500/20 to-purple-500/20 flex items-center justify-center border border-neon-cyan/20">
            <Cpu className="w-5.5 h-5.5 text-neon-cyan animate-pulse" />
          </div>
          <div>
            <span className="block font-display font-black text-sm tracking-widest text-white uppercase">Enterprise KB</span>
            <span className="block text-[9px] font-mono text-neon-cyan/85 tracking-widest uppercase mt-0.5">Console Unit</span>
          </div>
        </div>

        {/* Sidebar nav menu links */}
        <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
          
          <button
            onClick={() => {
              setActiveTab('KB');
              setIsSidebarOpen(false);
            }}
            className={`w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer ${
              activeTab === 'KB'
                ? 'bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border border-neon-cyan/3 dashtabs text-white font-bold'
                : 'text-slate-400 hover:text-white hover:bg-white/[0.015] border border-transparent'
            }`}
          >
            <Database className={`w-4.5 h-4.5 ${activeTab === 'KB' ? 'text-neon-cyan' : 'text-slate-500'}`} />
            <span>Knowledge Base</span>
          </button>

          <button
            onClick={() => {
              setActiveTab('Chat');
              setIsSidebarOpen(false);
            }}
            className={`w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer ${
              activeTab === 'Chat'
                ? 'bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border border-neon-cyan/3 dashtabs text-white font-bold'
                : 'text-slate-400 hover:text-white hover:bg-white/[0.015] border border-transparent'
            }`}
          >
            <MessageSquare className={`w-4.5 h-4.5 ${activeTab === 'Chat' ? 'text-neon-cyan' : 'text-slate-500'}`} />
            <span>RAG Q&A</span>
          </button>

          <button
            onClick={() => {
              setActiveTab('Telemetry');
              setIsSidebarOpen(false);
            }}
            className={`w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer ${
              activeTab === 'Telemetry'
                ? 'bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border border-neon-cyan/3 dashtabs text-white font-bold'
                : 'text-slate-400 hover:text-white hover:bg-white/[0.015] border border-transparent'
            }`}
          >
            <Activity className={`w-4.5 h-4.5 ${activeTab === 'Telemetry' ? 'text-neon-cyan' : 'text-slate-500'}`} />
            <span>Task Telemetry</span>
          </button>

          <button
            onClick={() => {
              setActiveTab('Permissions');
              setIsSidebarOpen(false);
            }}
            className={`w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer ${
              activeTab === 'Permissions'
                ? 'bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border border-neon-cyan/3 dashtabs text-white font-bold'
                : 'text-slate-400 hover:text-white hover:bg-white/[0.015] border border-transparent'
            }`}
          >
            <Shield className={`w-4.5 h-4.5 ${activeTab === 'Permissions' ? 'text-neon-cyan' : 'text-slate-500'}`} />
            <span>Permissions</span>
          </button>

          <button
            onClick={() => {
              setActiveTab('Settings');
              setIsSidebarOpen(false);
            }}
            className={`w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-xs font-mono tracking-wider transition-all cursor-pointer ${
              activeTab === 'Settings'
                ? 'bg-gradient-to-r from-cyan-950/40 to-cyan-500/[0.05] border border-neon-cyan/3 dashtabs text-white font-bold'
                : 'text-slate-400 hover:text-white hover:bg-white/[0.015] border border-transparent'
            }`}
          >
            <Settings className={`w-4.5 h-4.5 ${activeTab === 'Settings' ? 'text-neon-cyan' : 'text-slate-500'}`} />
            <span>Settings</span>
          </button>

        </nav>

        {/* Sidebar Footer User detail drawer panel */}
        <div className="p-4.5 border-t border-white/[0.05] bg-[#030612]/60 mt-auto text-xs font-mono text-slate-400">
          <div className="flex items-center justify-between mb-3">
            <span className="block text-[9px] uppercase tracking-wider text-slate-500">Security Sector</span>
            <span className="flex h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-500" />
          </div>
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-cyan-500 to-purple-600 border border-white/[0.05] flex items-center justify-center font-bold text-white uppercase text-xs">
              {user.slice(0, 2)}
            </div>
            <div className="flex-1 truncate">
              <span className="block text-white font-sans font-medium truncate">{user}</span>
              <span className="block text-[9px] truncate text-slate-500 font-mono">luorixin1@gmail.com</span>
            </div>
          </div>
          <button
            onClick={() => {
              setUser(null);
              showToast('Session session terminated.', 'info');
            }}
            className="w-full mt-4 flex items-center justify-center space-x-2 py-2 border border-white/[0.06] hover:border-red-400 rounded-xl hover:text-red-400 transition-all cursor-pointer active:scale-95 text-[11px] bg-slate-950/40 font-semibold"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Log Out</span>
          </button>
        </div>
      </aside>

      {/* Main Viewing Area panel - contains appropriate tab selectors inside dynamic transitions */}
      <main className="flex-1 p-6 md:p-8 overflow-y-auto max-h-screen relative z-10">
        <AnimatePresence mode="wait">
          <motion.div
            key={activeTab}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.22, ease: 'easeInOut' }}
            className="w-full max-w-7xl mx-auto h-full"
          >
            {activeTab === 'KB' && (
              <KnowledgeBaseView
                knowledgeBases={knowledgeBases}
                onAddKnowledgeBase={handleAddKnowledgeBase}
                onRefreshKbs={handleRefreshKbs}
                onTriggerChat={handleTriggerChatFromKb}
                onTriggerUpload={handleTriggerUploadFromKb}
              />
            )}

            {activeTab === 'Chat' && (
              <ChatView
                conversations={conversations}
                knowledgeBases={knowledgeBases}
                evidenceSources={evidenceSources}
                activeKbId={chatActiveKbId}
                onAddConversation={handleAddNewConversation}
                onSendMessage={handleChatSendMessage}
              />
            )}

            {activeTab === 'Telemetry' && (
              <TelemetryView
                tasks={telemetryTasks}
                onTriggerTaskAction={handleTriggerTaskAction}
              />
            )}

            {activeTab === 'Permissions' && (
              <PermissionsView
                members={teamMembers}
                onAddMember={handleAddMember}
                onUpdateMember={handleUpdateMember}
                onRemoveMember={handleRemoveMember}
              />
            )}

            {activeTab === 'Settings' && <SettingsView />}
          </motion.div>
        </AnimatePresence>
      </main>

      {/* Custom Global Floating Toast Indicators */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 p-4 max-w-sm rounded-xl border bg-[#060b18]/95 backdrop-blur-md shadow-2xl transition-all flex items-center gap-3 animate-bounce"
             style={{
               animationDuration: '1s',
               borderColor: toast.type === 'success' ? 'rgba(57,255,20,0.35)' : toast.type === 'warn' ? 'rgba(239, 68, 68, 0.4)' : 'rgba(0, 240, 255, 0.4)'
             }}>
          <span className="flex h-2.5 w-2.5 shrink-0 relative">
            <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${
              toast.type === 'success' ? 'bg-emerald-400' : toast.type === 'warn' ? 'bg-red-400' : 'bg-cyan-400'
            }`}></span>
            <span className={`relative inline-flex rounded-full h-2.5 w-2.5 ${
              toast.type === 'success' ? 'bg-emerald-500' : toast.type === 'warn' ? 'bg-red-500' : 'bg-cyan-500'
            }`}></span>
          </span>
          <p className="text-xs font-mono text-slate-200 leading-snug">
            {toast.message}
          </p>
        </div>
      )}

    </div>
  );
}
