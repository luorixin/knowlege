import React, { useState } from 'react';
import {
  Activity, Play, Pause, RefreshCw, Terminal, Eye, CheckCircle2,
  AlertTriangle, Hourglass, Cpu, ChevronRight, X, Gauge, ShieldAlert
} from 'lucide-react';
import { TelemetryTask } from '../types';

interface TelemetryViewProps {
  tasks: TelemetryTask[];
  onTriggerTaskAction: (taskId: string, action: 'retry' | 'pause' | 'execute') => void;
}

export default function TelemetryView({ tasks, onTriggerTaskAction }: TelemetryViewProps) {
  const [selectedKbFilter, setSelectedKbFilter] = useState('All');
  const [selectedTypeFilter, setSelectedTypeFilter] = useState('All');
  const [selectedStatusFilter, setSelectedStatusFilter] = useState('All');
  
  // Console logging reader overlay
  const [activeLogTask, setActiveLogTask] = useState<TelemetryTask | null>(null);

  // Compute metrics dynamically from current state tasks
  // For the sake of matching Image 4 exactly, we can use seed totals and add our current items as offsets!
  const completedCount = 450 + tasks.filter((t) => t.status === 'Completed').length;
  const pendingCount = 120 + tasks.filter((t) => t.status === 'Pending').length;
  const runningCount = tasks.filter((t) => t.status === 'Running').length;
  const partialCount = 25; // Constant mock metric matching Image 4
  const failedCount = 15 + tasks.filter((t) => t.status === 'Failed').length;

  const handleAction = (taskId: string, action: 'retry' | 'pause' | 'execute') => {
    onTriggerTaskAction(taskId, action);
  };

  return (
    <div className="font-sans space-y-6">
      
      {/* Visual Hub Title Banner */}
      <div className="border-b border-white/[0.08] pb-4 mb-4">
        <h2 className="text-xs font-mono tracking-[0.25em] text-slate-400 uppercase">
          Next-Gen Task Telemetry Hub
        </h2>
        <h1 className="font-display text-4xl font-extrabold text-white tracking-tight mt-1">
          Task Centre
        </h1>
      </div>

      {/* Selectors Category Row - Matches Image 4 */}
      <div className="cyber-panel rounded-xl p-4 flex flex-wrap items-center gap-4 border border-white/[0.06] text-xs font-mono">
        <div className="flex items-center space-x-2">
          <span className="text-slate-400">Knowledge Base:</span>
          <select
            value={selectedKbFilter}
            onChange={(e) => setSelectedKbFilter(e.target.value)}
            className="bg-slate-950 px-3 py-1.5 rounded-lg border border-white/[0.08] text-neon-cyan focus:outline-none"
          >
            <option value="All">All KBs</option>
            <option value="Product">Product Documentation</option>
            <option value="Sales">Sales Playbook</option>
            <option value="Compliance">Global Enterprise Docs</option>
          </select>
        </div>

        <div className="flex items-center space-x-2">
          <span className="text-slate-400">Task Type:</span>
          <select
            value={selectedTypeFilter}
            onChange={(e) => setSelectedTypeFilter(e.target.value)}
            className="bg-slate-950 px-3 py-1.5 rounded-lg border border-white/[0.08] text-neon-cyan focus:outline-none"
          >
            <option value="All">All Types</option>
            <option value="Parsing">Parsing Node</option>
            <option value="Embedding">Embedding Matrix</option>
          </select>
        </div>

        <div className="flex items-center space-x-2">
          <span className="text-slate-400">Status:</span>
          <select
            value={selectedStatusFilter}
            onChange={(e) => setSelectedStatusFilter(e.target.value)}
            className="bg-slate-950 px-3 py-1.5 rounded-lg border border-white/[0.08] text-neon-cyan focus:outline-none"
          >
            <option value="All">All Statuses</option>
            <option value="Completed">Completed</option>
            <option value="Running">Running</option>
            <option value="Pending">Pending</option>
            <option value="Failed">Failed</option>
          </select>
        </div>
      </div>

      {/* Glow Metrics Row - Matches Image 4 layout exactly with corresponding beautiful custom animated wave representations */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        
        {/* Completed Card (Green Glow) */}
        <div className="bg-slate-950/60 border border-emerald-500/30 shadow-[inset_0_0_15px_rgba(16,185,129,0.06),0_4px_16px_rgba(0,0,0,0.4)] rounded-2xl p-4.5 flex items-center justify-between">
          <div>
            <span className="block text-[10px] font-mono uppercase text-emerald-400 font-bold tracking-widest">Completed</span>
            <span className="font-display text-2xl font-black text-white mt-1.5 block">{completedCount}</span>
          </div>
          {/* Wave SVG representation */}
          <div className="w-16 h-8 opacity-75">
            <svg viewBox="0 0 100 40" className="w-full h-full text-emerald-400" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M0,20 Q10,5 20,20 T40,20 T60,20 T80,20 T100,20" strokeLinecap="round" />
            </svg>
          </div>
        </div>

        {/* Pending Card (Amber Glow) */}
        <div className="bg-slate-950/60 border border-amber-500/30 shadow-[inset_0_0_15px_rgba(245,158,11,0.06),0_4px_16px_rgba(0,0,0,0.4)] rounded-2xl p-4.5 flex items-center justify-between">
          <div>
            <span className="block text-[10px] font-mono uppercase text-amber-400 font-bold tracking-widest">Pending</span>
            <span className="font-display text-2xl font-black text-white mt-1.5 block">{pendingCount}</span>
          </div>
          {/* Heart rate wave representation */}
          <div className="w-16 h-8 opacity-75">
            <svg viewBox="0 0 100 40" className="w-full h-full text-amber-400" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M0,20 L30,20 L35,5 L40,35 L45,20 L100,20" strokeLinejoin="miter" strokeLinecap="round" />
            </svg>
          </div>
        </div>

        {/* Running Card (Blue/Cyan Glow) */}
        <div className="bg-slate-950/60 border border-cyan-500/30 shadow-[inset_0_0_15px_rgba(6,182,212,0.06),0_4px_16px_rgba(0,0,0,0.4)] rounded-2xl p-4.5 flex items-center justify-between">
          <div>
            <span className="block text-[10px] font-mono uppercase text-cyan-400 font-bold tracking-widest">Running</span>
            <span className="font-display text-2xl font-black text-white mt-1.5 block">{runningCount}</span>
          </div>
          {/* Sine wave with pulse */}
          <div className="w-16 h-8 opacity-75">
            <svg viewBox="0 0 100 40" className="w-full h-full text-cyan-400" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M0,20 Q15,0 30,20 T60,20 T90,20 Q95,30 100,20" strokeLinecap="round" />
            </svg>
          </div>
        </div>

        {/* Partial Card (Orange Glow) */}
        <div className="bg-slate-950/60 border border-orange-500/30 shadow-[inset_0_0_15px_rgba(249,115,22,0.06),0_4px_16px_rgba(0,0,0,0.4)] rounded-2xl p-4.5 flex items-center justify-between">
          <div>
            <span className="block text-[10px] font-mono uppercase text-orange-400 font-bold tracking-widest">Partial</span>
            <span className="font-display text-2xl font-black text-white mt-1.5 block">{partialCount}</span>
          </div>
          {/* Stack bars sound representation */}
          <div className="w-16 h-8 opacity-75">
            <svg viewBox="0 0 100 40" className="w-full h-full text-orange-400" fill="none" stroke="currentColor" strokeWidth="3">
              <line x1="10" y1="20" x2="10" y2="30" />
              <line x1="25" y1="10" x2="25" y2="30" />
              <line x1="40" y1="5" x2="40" y2="30" />
              <line x1="55" y1="15" x2="55" y2="30" />
              <line x1="70" y1="8" x2="70" y2="30" />
              <line x1="85" y1="22" x2="85" y2="30" />
            </svg>
          </div>
        </div>

        {/* Failed Card (Red Glow) */}
        <div className="bg-slate-950/60 border border-red-500/30 shadow-[inset_0_0_15px_rgba(239,68,68,0.06),0_4px_16px_rgba(0,0,0,0.4)] rounded-2xl p-4.5 flex items-center justify-between">
          <div>
            <span className="block text-[10px] font-mono uppercase text-red-400 font-bold tracking-widest">Failed</span>
            <span className="font-display text-2xl font-black text-white mt-1.5 block">{failedCount}</span>
          </div>
          {/* Jagged sharp graph representation */}
          <div className="w-16 h-8 opacity-75">
            <svg viewBox="0 0 100 40" className="w-full h-full text-red-400" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M0,20 L20,32 L40,8 L60,35 L80,12 L100,23" strokeLinecap="round" />
            </svg>
          </div>
        </div>

      </div>

      {/* Ingestion Workflows Table - matching structure and colors of Image 4 */}
      <div className="cyber-panel rounded-2xl overflow-hidden border border-white/[0.06] shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[700px]">
            <thead>
              <tr className="border-b border-white/[0.08] bg-slate-950/40 text-[10px] font-mono uppercase text-slate-400 tracking-wider">
                <th className="py-3 px-4.5">Stage</th>
                <th className="py-3 px-4.5">Document Name</th>
                <th className="py-3 px-4.5">Status Badge</th>
                <th className="py-3 px-4.5">Animated Progress Bar</th>
                <th className="py-3 px-4.5">Model/Engine Details</th>
                <th className="py-3 px-4.5">Retry Count</th>
                <th className="py-3 px-4.5">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.05] text-xs">
              {tasks.map((task) => (
                <tr key={task.id} className="hover:bg-white/[0.02] transition-colors">
                  
                  {/* Stage */}
                  <td className="py-4.5 px-4.5 font-mono text-slate-200">
                    <span className="flex items-center gap-2">
                      <Terminal className="w-4 h-4 text-slate-500" />
                      {task.stage}
                    </span>
                  </td>

                  {/* Document Name */}
                  <td className="py-4.5 px-4.5 font-sans font-medium text-neon-cyan">
                    {task.documentName}
                  </td>

                  {/* Status Badge */}
                  <td className="py-4.5 px-4.5">
                    {task.status === 'Completed' && (
                      <span className="px-2.5 py-1 rounded-full border border-emerald-500/20 bg-emerald-950/20 text-emerald-400 text-[10px] font-mono flex items-center w-fit gap-1 bg-emerald-950/40 font-bold">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                        Completed
                      </span>
                    )}
                    {task.status === 'Running' && (
                      <span className="px-2.5 py-1 rounded-full border border-cyan-500/20 bg-cyan-950/20 text-cyan-400 text-[10px] font-mono flex items-center w-fit gap-1 bg-cyan-950/40 font-bold">
                        <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-ping" />
                        Running
                      </span>
                    )}
                    {task.status === 'Failed' && (
                      <span className="px-2.5 py-1 rounded-full border border-red-500/20 bg-red-950/20 text-red-500 text-[10px] font-mono flex items-center w-fit gap-1 bg-red-950/40 font-bold">
                        <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
                        Failed
                      </span>
                    )}
                    {task.status === 'Pending' && (
                      <span className="px-2.5 py-1 rounded-full border border-amber-500/20 bg-amber-950/20 text-amber-400 text-[10px] font-mono flex items-center w-fit gap-1 bg-amber-950/40 font-bold">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-400" />
                        Pending
                      </span>
                    )}
                  </td>

                  {/* Animated Progress Bar */}
                  <td className="py-4.5 px-4.5 text-center min-w-[140px]">
                    <div className="flex items-center space-x-3">
                      <div className="flex-1 h-3 bg-slate-950 rounded-full overflow-hidden border border-white/[0.04] relative">
                        {/* Shimmer glowing lightning spark during running */}
                        {task.status === 'Running' && (
                          <div className="absolute inset-0 bg-gradient-to-r from-transparent via-cyan-400/30 to-transparent w-1/3 animate-pulse" style={{ animationDuration: '1.2s' }} />
                        )}
                        <div
                          className={`h-full transition-all duration-500 ${
                            task.status === 'Completed'
                              ? 'bg-gradient-to-r from-emerald-500 to-teal-400 shadow-[0_0_8px_rgba(16,185,129,0.4)]'
                              : task.status === 'Running'
                              ? 'bg-gradient-to-r from-cyan-500 to-blue-400 shadow-[0_0_8px_rgba(6,182,212,0.4)]'
                              : task.status === 'Failed'
                              ? 'bg-gradient-to-r from-red-500 to-rose-400 shadow-[0_0_8px_rgba(239,68,68,0.4)]'
                              : 'bg-slate-700'
                          }`}
                          style={{ width: `${task.progress}%` }}
                        />
                      </div>
                      <span className="text-[10px] font-mono font-bold text-slate-300 w-8 text-right shrink-0">{task.progress}%</span>
                    </div>
                  </td>

                  {/* Model Name */}
                  <td className="py-4.5 px-4.5 font-mono text-slate-400">
                    {task.modelName}
                  </td>

                  {/* Retry Count */}
                  <td className="py-4.5 px-4.5 font-mono text-slate-400">
                    {task.retryCount}
                  </td>

                  {/* Actions (View Logs, retry, execution) */}
                  <td className="py-4.5 px-4.5">
                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => setActiveLogTask(task)}
                        className="p-1 px-2 border border-white/[0.06] hover:border-neon-cyan/40 hover:text-neon-cyan rounded text-[10px] font-mono uppercase tracking-wide bg-slate-950/60 text-slate-400 transition-all cursor-pointer flex items-center gap-1"
                        title="View raw ingestion logs"
                      >
                        <Eye className="w-3 h-3" />
                        <span>Logs</span>
                      </button>

                      {task.status === 'Failed' && (
                        <button
                          onClick={() => handleAction(task.id, 'retry')}
                          className="p-1 px-2 border border-red-500/20 hover:border-red-500/80 hover:text-red-400 rounded text-[10px] font-mono uppercase bg-red-950/20 text-red-500 transition-all cursor-pointer flex items-center gap-1"
                        >
                          <RefreshCw className="w-3 h-3" />
                          <span>Retry</span>
                        </button>
                      )}

                      {task.status === 'Running' && (
                        <button
                          onClick={() => handleAction(task.id, 'pause')}
                          className="p-1 px-2 border border-amber-500/20 hover:border-amber-500/80 hover:text-amber-400 rounded text-[10px] font-mono uppercase bg-amber-950/20 text-amber-500 transition-all cursor-pointer flex items-center gap-1"
                        >
                          <Pause className="w-3 h-3" />
                          <span>Pause</span>
                        </button>
                      )}

                      {(task.status === 'Pending') && (
                        <button
                          onClick={() => handleAction(task.id, 'execute')}
                          className="p-1 px-2 border border-cyan-500/20 hover:border-cyan-500/80 hover:text-cyan-400 rounded text-[10px] font-mono uppercase bg-cyan-950/20 text-cyan-500 transition-all cursor-pointer flex items-center gap-1"
                        >
                          <Play className="w-3 h-3" />
                          <span>Execute</span>
                        </button>
                      )}
                    </div>
                  </td>

                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Terminal logs View Modal - opens beautiful live console drawer */}
      {activeLogTask && (
        <div className="fixed inset-0 bg-black/85 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-slate-950 border border-white/10 rounded-2xl max-w-2xl w-full p-6 shadow-2xl relative">
            <div className="flex items-center justify-between border-b border-white/[0.08] pb-3 mb-4">
              <span className="text-[10px] font-mono uppercase tracking-widest text-slate-400 flex items-center gap-2">
                <Terminal className="text-neon-cyan w-4.5 h-4.5 animate-pulse" />
                <span>Console Core Log: {activeLogTask.documentName}</span>
              </span>
              <button
                onClick={() => setActiveLogTask(null)}
                className="text-slate-400 hover:text-white transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Terminal Body Screen */}
            <div className="bg-black/80 rounded-xl p-5 border border-white/[0.05] font-mono text-xs text-slate-300 space-y-2 h-72 overflow-y-auto">
              <div className="text-slate-500 pb-1.5 border-b border-white/[0.05] mb-2">
                // ACTIVE INGESTION INSTANCE FOR MODULE {activeLogTask.id.toUpperCase()}
              </div>
              
              {activeLogTask.logs.map((log, index) => {
                let color = 'text-slate-300';
                if (log.includes('[ERROR]')) color = 'text-red-400';
                if (log.includes('[SUCCESS]')) color = 'text-emerald-400';
                if (log.includes('[WARN]')) color = 'text-amber-400';
                if (log.includes('[PROGRESS]')) color = 'text-cyan-400 font-bold';
                return (
                  <p key={index} className={`${color}`}>
                    {log}
                  </p>
                );
              })}

              {activeLogTask.status === 'Running' && (
                <div className="flex items-center gap-2 text-cyan-400 pt-2 animate-pulse">
                  <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-ping" />
                  <span>[INGESTING] awaiting telemetry vector stream chunks...</span>
                </div>
              )}
            </div>

            <div className="flex justify-end mt-4">
              <button
                onClick={() => setActiveLogTask(null)}
                className="px-4 py-1.5 rounded-xl border border-white/[0.08] hover:border-white/15 text-xs font-mono text-slate-400 hover:text-white transition-all cursor-pointer"
              >
                Close Output
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
