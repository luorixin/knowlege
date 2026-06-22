import React, { useState } from 'react';
import { Shield, ShieldAlert, Plus, Search, Trash2, Edit2, CheckCircle2, UserPlus, FileText, Lock, Users } from 'lucide-react';
import { TeamMember } from '../types';

interface PermissionsViewProps {
  members: TeamMember[];
  onAddMember: (member: Omit<TeamMember, 'id'>) => void;
  onUpdateMember: (member: TeamMember) => void;
  onRemoveMember: (memberId: string) => void;
}

export default function PermissionsView({
  members,
  onAddMember,
  onUpdateMember,
  onRemoveMember,
}: PermissionsViewProps) {
  const [activeTab, setActiveTab] = useState<'members' | 'roles' | 'policies'>('members');
  const [searchTerm, setSearchTerm] = useState('');
  
  // New Member Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newUsername, setNewUsername] = useState('');
  const [newDisplayName, setNewDisplayName] = useState('');
  const [useAdmin, setUseAdmin] = useState(false);
  const [useEditor, setUseEditor] = useState(false);
  const [useViewer, setUseViewer] = useState(true);

  // Edit Member Modal State
  const [editingMember, setEditingMember] = useState<TeamMember | null>(null);

  const filteredMembers = members.filter(
    (m) =>
      m.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      m.displayName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleCreateMemberSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUsername.trim() || !newDisplayName.trim()) return;

    const roles: ('Admin' | 'Content Editor' | 'Viewer')[] = [];
    if (useAdmin) roles.push('Admin');
    if (useEditor) roles.push('Content Editor');
    if (useViewer) roles.push('Viewer');

    if (roles.length === 0) roles.push('Viewer');

    onAddMember({
      username: newUsername.trim().toLowerCase(),
      displayName: newDisplayName.trim(),
      roles,
      status: 'Active',
    });

    setNewUsername('');
    setNewDisplayName('');
    setUseAdmin(false);
    setUseEditor(false);
    setUseViewer(true);
    setIsModalOpen(false);
  };

  const handleEditMemberSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingMember) return;
    onUpdateMember(editingMember);
    setEditingMember(null);
  };

  return (
    <div className="font-sans space-y-6">
      
      {/* Visual Title Header (Matches Image 5) */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/[0.08] pb-6">
        <div>
          <h1 className="font-display text-2xl md:text-3xl font-bold text-white tracking-wide">
            Cybersecurity Permissions Console
          </h1>
          <p className="text-xs text-slate-400 font-mono uppercase tracking-wider mt-1 flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-neon-purple animate-pulse"></span>
            Virtual Guard Node: <span className="text-neon-purple">ACTIVE-WG-09</span>
          </p>
        </div>

        {/* Neon Cyan "Add Member" Button (Image 5) */}
        <button
          onClick={() => setIsModalOpen(true)}
          className="self-start sm:self-center px-4.5 py-2 rounded-xl bg-cyan-400 text-slate-950 font-display font-bold text-xs tracking-wider transition-all hover:bg-neon-cyan hover:shadow-[0_0_15px_rgba(0,240,255,0.4)] active:scale-95 cursor-pointer flex items-center gap-1.5"
        >
          <UserPlus className="w-4.5 h-4.5" />
          <span>Add Member</span>
        </button>
      </div>

      {/* Segmented Controller navigation tabs (Image 5) */}
      <div className="flex items-center space-x-1 border-b border-white/[0.05]">
        <button
          onClick={() => setActiveTab('members')}
          className={`py-3 px-4.5 text-xs font-mono tracking-wider transition-all relative border-b-2 cursor-pointer ${
            activeTab === 'members'
              ? 'border-neon-cyan text-neon-cyan font-bold'
              : 'border-transparent text-slate-400 hover:text-white'
          }`}
        >
          Space Members
        </button>
        <button
          onClick={() => setActiveTab('roles')}
          className={`py-3 px-4.5 text-xs font-mono tracking-wider transition-all relative border-b-2 cursor-pointer ${
            activeTab === 'roles'
              ? 'border-neon-cyan text-neon-cyan font-bold'
              : 'border-transparent text-slate-400 hover:text-white'
          }`}
        >
          Roles
        </button>
        <button
          onClick={() => setActiveTab('policies')}
          className={`py-3 px-4.5 text-xs font-mono tracking-wider transition-all relative border-b-2 cursor-pointer ${
            activeTab === 'policies'
              ? 'border-neon-cyan text-neon-cyan font-bold'
              : 'border-transparent text-slate-400 hover:text-white'
          }`}
        >
          Policies
        </button>
      </div>

      {/* Space Members Tab (Matches screenshot 5 layout) */}
      {activeTab === 'members' && (
        <div className="space-y-4">
          
          {/* Member Search filter */}
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
            <input
              type="text"
              placeholder="Filter members by name..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full text-xs text-white pl-9 pr-4 py-2 rounded-xl border border-white/[0.08] bg-slate-950/80 focus:border-neon-cyan focus:outline-none placeholder:text-slate-500"
            />
          </div>

          {/* Members Grid/List Panel */}
          <div className="cyber-panel rounded-2xl overflow-hidden border border-white/[0.06] shadow-xl">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse min-w-[650px]">
                <thead>
                  <tr className="border-b border-white/[0.08] bg-slate-950/40 text-[10px] font-mono uppercase text-slate-400 tracking-wider">
                    <th className="py-3.5 px-4.5">Username</th>
                    <th className="py-3.5 px-4.5">Display Name</th>
                    <th className="py-3.5 px-4.5">Roles</th>
                    <th className="py-3.5 px-4.5">Status</th>
                    <th className="py-3.5 px-4.5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/[0.05] text-xs">
                  {filteredMembers.map((m) => (
                    <tr
                      key={m.id}
                      className="hover:bg-white/[0.015] transition-colors group relative"
                    >
                      {/* Active border row highlight - matches selection in screenshot 5 */}
                      <td className="py-4.5 px-4.5 font-mono text-neon-cyan font-semibold">
                        {m.username}
                      </td>

                      <td className="py-4.5 px-4.5 text-slate-200">
                        {m.displayName}
                      </td>

                      {/* Role Pill badges formatted inside turquoise outline/badges */}
                      <td className="py-4.5 px-4.5">
                        <div className="flex flex-wrap gap-1.5">
                          {m.roles.map((role) => (
                            <span
                              key={role}
                              className={`px-2 py-0.5 rounded text-[10px] font-mono border ${
                                role === 'Admin'
                                  ? 'border-indigo-500/35 text-indigo-400 bg-indigo-950/15'
                                  : role === 'Content Editor'
                                  ? 'border-cyan-500/30 text-cyan-400 bg-cyan-950/15'
                                  : 'border-slate-500/25 text-slate-400 bg-slate-950/20'
                              }`}
                            >
                              {role}
                            </span>
                          ))}
                        </div>
                      </td>

                      {/* Active/Inactive status with dynamic bullet animation */}
                      <td className="py-4.5 px-4.5">
                        <span className="flex items-center gap-1.5 font-mono">
                          <span
                            className={`w-2 h-2 rounded-full ${
                              m.status === 'Active'
                                ? 'bg-emerald-500 shadow-[0_0_8px_#10b981] animate-pulse'
                                : 'bg-red-500 shadow-[0_0_8px_#ef4444]'
                            }`}
                          />
                          <span className={m.status === 'Active' ? 'text-emerald-400' : 'text-red-400'}>
                            {m.status}
                          </span>
                        </span>
                      </td>

                      {/* Edit/Remove buttons */}
                      <td className="py-4.5 px-4.5 text-right font-mono">
                        <div className="flex items-center justify-end space-x-2">
                          <button
                            onClick={() => setEditingMember(m)}
                            className="px-2.5 py-1 rounded border border-white/[0.06] hover:border-cyan-500/40 text-slate-450 hover:text-white transition-all text-[10px] cursor-pointer"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => onRemoveMember(m.id)}
                            className="px-2.5 py-1 rounded border border-red-950/40 hover:border-red-500 text-red-500 hover:text-red-400 hover:bg-red-950/15 transition-all text-[10px] cursor-pointer"
                          >
                            Remove
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination controls from Image 5 */}
            <div className="bg-slate-950/40 px-6 py-4 border-t border-white/[0.05] flex items-center justify-between text-xs font-mono text-slate-500">
              <span>Showing 1-{filteredMembers.length} of 25</span>
              <div className="flex items-center space-x-1.5">
                <button className="px-2.5 py-1 rounded bg-cyan-950/40 text-neon-cyan border border-neon-cyan/30">1</button>
                <button className="px-2.5 py-1 rounded hover:bg-slate-800 hover:text-white transition-all">2</button>
                <button className="px-2.5 py-1 rounded hover:bg-slate-800 hover:text-white transition-all">3</button>
                <button className="px-2.5 py-1.5 rounded text-[10px] hover:text-white transition-all flex items-center gap-0.5">
                  <span>Next</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Roles & Polices tab visual mockups */}
      {activeTab === 'roles' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="cyber-panel rounded-2xl p-6 border-white/[0.06]">
            <h3 className="text-sm font-mono uppercase text-neon-cyan font-semibold mb-2">Admin</h3>
            <p className="text-slate-350 text-xs leading-relaxed mb-4">
              Full administrative override. Capacity to create knowledge directories, configure vector configurations, manage space access, and purge logs.
            </p>
            <span className="text-[10px] font-mono text-slate-500">2 members assigned</span>
          </div>
          <div className="cyber-panel rounded-2xl p-6 border-white/[0.06]">
            <h3 className="text-sm font-mono uppercase text-neon-purple font-semibold mb-2">Content Editor</h3>
            <p className="text-slate-350 text-xs leading-relaxed mb-4">
              Authorized to upload raw files, trigger telemetry ingestion logs, edit document scopes, and initiate embedding runs.
            </p>
            <span className="text-[10px] font-mono text-slate-500">3 members assigned</span>
          </div>
          <div className="cyber-panel rounded-2xl p-6 border-white/[0.06]">
            <h3 className="text-sm font-mono uppercase text-slate-400 font-semibold mb-2">Viewer</h3>
            <p className="text-slate-350 text-xs leading-relaxed mb-4">
              Access limited to executing RAG chats, reading evidence traces, inspecting basic telemetry stats, and checking system health.
            </p>
            <span className="text-[10px] font-mono text-slate-500">20 members assigned</span>
          </div>
        </div>
      )}

      {activeTab === 'policies' && (
        <div className="space-y-4">
          <div className="cyber-panel rounded-2xl p-6 border-white/[0.06] flex items-center justify-between">
            <div>
              <h3 className="text-xs font-mono uppercase tracking-wider text-white font-bold mb-1">Double-Factor Auth Gate</h3>
              <p className="text-slate-400 text-xs">Forced OTP checks before Admin credentials can establish SSH partitions.</p>
            </div>
            <span className="px-2.5 py-1 rounded bg-emerald-950/45 text-emerald-400 font-mono text-[9px] font-bold border border-emerald-500/20 uppercase tracking-widest">ENABLED</span>
          </div>
          <div className="cyber-panel rounded-2xl p-6 border-white/[0.06] flex items-center justify-between">
            <div>
              <h3 className="text-xs font-mono uppercase tracking-wider text-white font-bold mb-1">Vector Anonymization Scrape</h3>
              <p className="text-slate-400 text-xs">Automatically redact PII (SSNs, Phone integers, emails) prior to embedding ingest vectors.</p>
            </div>
            <span className="px-2.5 py-1 rounded bg-amber-950/45 text-amber-400 font-mono text-[9px] font-bold border border-amber-500/20 uppercase tracking-widest">GATING ROUTE</span>
          </div>
        </div>
      )}

      {/* Add Member Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-white/10 rounded-2xl max-w-md w-full p-6 md:p-8 shadow-2xl relative">
            <h2 className="font-display text-xl font-bold text-white mb-4 flex items-center gap-2">
              <UserPlus className="text-cyan-400 w-5 h-5" />
              <span>Add Member to Virtual Space</span>
            </h2>
            
            <form onSubmit={handleCreateMemberSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1">
                  Username
                </label>
                <input
                  type="text"
                  value={newUsername}
                  onChange={(e) => setNewUsername(e.target.value)}
                  placeholder="e.g. alice.wonder"
                  className="w-full text-sm text-white px-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none font-mono"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1">
                  Display Name
                </label>
                <input
                  type="text"
                  value={newDisplayName}
                  onChange={(e) => setNewDisplayName(e.target.value)}
                  placeholder="e.g. Alice Wonder"
                  className="w-full text-sm text-white px-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-2">
                  Assign Cyber Roles
                </label>
                <div className="space-y-2 bg-slate-950/60 p-3 rounded-lg border border-white/[0.05]">
                  <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={useAdmin}
                      onChange={(e) => setUseAdmin(e.target.checked)}
                      className="rounded text-cyan-500 focus:ring-0 bg-slate-900"
                    />
                    <span>Admin Override</span>
                  </label>
                  <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={useEditor}
                      onChange={(e) => setUseEditor(e.target.checked)}
                      className="rounded text-cyan-500 focus:ring-0 bg-slate-900"
                    />
                    <span>Content Editor Ingestion</span>
                  </label>
                  <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={useViewer}
                      onChange={(e) => setUseViewer(e.target.checked)}
                      className="rounded text-cyan-500 focus:ring-0 bg-slate-900"
                    />
                    <span>Viewer</span>
                  </label>
                </div>
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
                  Authorize Unit
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Member Modal */}
      {editingMember && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-white/10 rounded-2xl max-w-md w-full p-6 md:p-8 shadow-2xl relative">
            <h2 className="font-display text-xl font-bold text-white mb-4">Edit Space Member Settings</h2>
            
            <form onSubmit={handleEditMemberSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1">
                  Display Name
                </label>
                <input
                  type="text"
                  value={editingMember.displayName}
                  onChange={(e) => setEditingMember({ ...editingMember, displayName: e.target.value })}
                  className="w-full text-sm text-white px-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-1000 focus:border-neon-cyan focus:outline-none"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-1">
                  Status
                </label>
                <select
                  value={editingMember.status}
                  onChange={(e) => setEditingMember({ ...editingMember, status: e.target.value as 'Active' | 'Inactive' })}
                  className="w-full text-sm text-white px-4 py-2.5 rounded-xl border border-white/[0.08] bg-slate-950 focus:border-neon-cyan focus:outline-none text-cyan-400 font-mono"
                >
                  <option value="Active">Active Duty</option>
                  <option value="Inactive">Suspended Node</option>
                </select>
              </div>

              <div className="flex items-center justify-end space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => setEditingMember(null)}
                  className="px-4 py-2 rounded-xl border border-white/[0.08] text-xs font-mono text-slate-400 hover:text-white tracking-wider cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl cyber-btn-cyan text-xs font-mono tracking-wider cursor-pointer"
                >
                  Confirm Policy Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
