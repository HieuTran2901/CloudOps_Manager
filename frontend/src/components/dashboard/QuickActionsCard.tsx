import React from 'react';
import { Cloud, Share2, FileCheck, Shield, Download, PieChart } from 'lucide-react';
import { NavTab } from '../layout/Sidebar';

interface QuickActionsCardProps {
  onActionSelect?: (tab: NavTab) => void;
}

export const QuickActionsCard: React.FC<QuickActionsCardProps> = ({ onActionSelect }) => {
  const actions = [
    {
      id: 'resources' as NavTab,
      label: 'Run Discovery',
      icon: Cloud,
      color: 'text-sky-400 bg-sky-950/40 border-sky-500/20 hover:border-sky-400',
    },
    {
      id: 'topology' as NavTab,
      label: 'View Topology',
      icon: Share2,
      color: 'text-emerald-400 bg-emerald-950/40 border-emerald-500/20 hover:border-emerald-400',
    },
    {
      id: 'compliance' as NavTab,
      label: 'Compliance Report',
      icon: FileCheck,
      color: 'text-purple-400 bg-purple-950/40 border-purple-500/20 hover:border-purple-400',
    },
    {
      id: 'security' as NavTab,
      label: 'Security Scan',
      icon: Shield,
      color: 'text-rose-400 bg-rose-950/40 border-rose-500/20 hover:border-rose-400',
    },
    {
      id: 'forensics' as NavTab,
      label: 'Export Evidence',
      icon: Download,
      color: 'text-amber-400 bg-amber-950/40 border-amber-500/20 hover:border-amber-400',
    },
    {
      id: 'costs' as NavTab,
      label: 'Cost Analysis',
      icon: PieChart,
      color: 'text-teal-400 bg-teal-950/40 border-teal-500/20 hover:border-teal-400',
    },
  ];

  return (
    <div className="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-5 shadow-lg flex flex-col justify-between">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-bold uppercase tracking-wider text-slate-300">
          QUICK ACTIONS
        </span>
      </div>

      <div className="my-3 grid grid-cols-2 sm:grid-cols-3 gap-3">
        {actions.map((act) => {
          const Icon = act.icon;
          return (
            <button
              key={act.label}
              onClick={() => onActionSelect && onActionSelect(act.id)}
              className={`p-3 rounded-xl border flex flex-col items-center justify-center space-y-2 transition-all group ${act.color}`}
            >
              <Icon className="w-5 h-5 group-hover:scale-110 transition-transform" />
              <span className="text-[11px] font-semibold text-slate-200">{act.label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
};