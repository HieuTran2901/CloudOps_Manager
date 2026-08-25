import React, { useState, useEffect } from 'react';
import {
  LayoutDashboard,
  Compass,
  Layers,
  Activity,
  CheckCircle2,
  Shield,
  DollarSign,
  FileText,
  GitBranch,
  Share2,
  Lock,
  Download,
  Server,
  ChevronDown,
} from 'lucide-react';
import { cloudOpsApi } from '../../api';
import { DetailedHealthResponse } from '../../types/api';

export type NavTab =
  | 'dashboard'
  | 'resources'
  | 'observability'
  | 'costs'
  | 'cloudtrail'
  | 'compliance'
  | 'drift'
  | 'topology'
  | 'security'
  | 'forensics'
  | 'operations';

interface SidebarProps {
  activeTab: NavTab;
  onTabSelect: (tab: NavTab) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, onTabSelect }) => {
  const [healthData, setHealthData] = useState<DetailedHealthResponse | null>(null);

  useEffect(() => {
    cloudOpsApi.getDetailedHealth()
      .then(setHealthData)
      .catch(() => null);
  }, []);

  const navItems: Array<{ id: NavTab; label: string; icon: React.FC<{ className?: string }> }> = [
    { id: 'dashboard', label: 'Overview', icon: LayoutDashboard },
    { id: 'resources', label: 'AWS Discovery', icon: Compass },
    { id: 'resources', label: 'Resources', icon: Layers },
    { id: 'observability', label: 'Telemetry', icon: Activity },
    { id: 'compliance', label: 'Compliance', icon: CheckCircle2 },
    { id: 'security', label: 'Security', icon: Shield },
    { id: 'costs', label: 'Financials', icon: DollarSign },
    { id: 'cloudtrail', label: 'Audit Events', icon: FileText },
    { id: 'drift', label: 'Terraform Drift', icon: GitBranch },
    { id: 'topology', label: 'Topology Graph', icon: Share2 },
    { id: 'security', label: 'Blast Radius', icon: Lock },
    { id: 'forensics', label: 'Forensic Export', icon: Download },
    { id: 'operations', label: 'Operations & Health', icon: Server },
  ];

  const isHealthy = healthData?.status === 'UP';

  return (
    <aside className="w-64 border-r border-slate-800/80 bg-[#070b14] flex flex-col justify-between p-4 flex-shrink-0 select-none overflow-y-auto">
      <div className="space-y-1">
        <p className="px-3 py-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">
          CONTROL PLANE
        </p>
        {navItems.map((item, idx) => {
          const Icon = item.icon;
          const isActive =
            activeTab === item.id || (item.label === 'Overview' && activeTab === 'dashboard');

          return (
            <button
              key={`${item.label}-${idx}`}
              onClick={() => onTabSelect(item.id)}
              className={`w-full flex items-center space-x-3 px-3 py-2 rounded-xl text-xs font-semibold transition-all ${
                isActive
                  ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-[0_0_15px_rgba(37,99,235,0.4)]'
                  : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{item.label}</span>
            </button>
          );
        })}
      </div>

      {/* Bottom Health Widget & Profile */}
      <div className="space-y-4 pt-4 border-t border-slate-900">
        {/* System Health Card (Interactive & Evidence-Driven) */}
        <div
          onClick={() => onTabSelect('operations')}
          className="p-4 rounded-2xl border border-slate-800/80 bg-[#0a0f1d] shadow-lg flex flex-col items-center text-center cursor-pointer hover:border-sky-500/40 transition-all group"
        >
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-2 group-hover:text-sky-400 transition-colors">
            SYSTEM HEALTH
          </span>
          <div className="relative w-16 h-16 flex items-center justify-center">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="40" fill="none" stroke="#1e293b" strokeWidth="8" />
              <circle
                cx="50"
                cy="50"
                r="40"
                fill="none"
                stroke={isHealthy ? '#10b981' : '#f59e0b'}
                strokeWidth="8"
                strokeDasharray="246 251"
                strokeDashoffset="0"
                className={isHealthy ? "drop-shadow-[0_0_8px_rgba(16,185,129,0.8)]" : "drop-shadow-[0_0_8px_rgba(245,158,11,0.8)]"}
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-sm font-black text-slate-100 font-mono leading-none">
                {healthData ? (isHealthy ? '100' : '85') : '100'}
              </span>
              <span className={`text-[8px] font-semibold mt-0.5 ${isHealthy ? 'text-emerald-400' : 'text-amber-400'}`}>
                {healthData ? (isHealthy ? 'Healthy' : 'Degraded') : 'Active'}
              </span>
            </div>
          </div>
          <span className="text-[11px] font-semibold text-slate-200 mt-2">
            {healthData?.service || 'cloudops-manager'}
          </span>
          <span className="text-[10px] text-slate-400 mt-0.5 font-mono">
            {healthData?.version ? `v${healthData.version}` : 'v1.0.0'}
          </span>
        </div>

        {/* User / Account Card */}
        <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-900/60 border border-slate-800">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-white text-xs font-bold shadow-[0_0_10px_rgba(37,99,235,0.5)]">
              TA
            </div>
            <div className="text-left">
              <span className="text-xs font-bold text-slate-200 block">Test Account</span>
              <span className="text-[10px] text-slate-400 block font-mono">Local Account</span>
            </div>
          </div>
          <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
        </div>
      </div>
    </aside>
  );
};