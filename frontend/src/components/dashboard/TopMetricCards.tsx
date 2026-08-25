import React from 'react';
import { Box, ShieldCheck, Share2, Shield, DollarSign, TrendingUp } from 'lucide-react';

interface TopMetricCardsProps {
  totalResources?: number;
  complianceCount?: number;
  compliancePassRate?: number;
  monthlyCost?: number;
}

export const TopMetricCards: React.FC<TopMetricCardsProps> = ({
  totalResources = 8742,
  complianceCount = 156,
  compliancePassRate = 92,
  monthlyCost = 12845,
}) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
      {/* 1. Discovered Resources */}
      <div className="relative rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-4 shadow-lg overflow-hidden group hover:border-sky-500/40 transition-all">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">DISCOVERED RESOURCES</span>
          <div className="p-2 rounded-xl bg-sky-950/60 border border-sky-500/20 text-sky-400">
            <Box className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 flex items-baseline justify-between">
          <span className="text-2xl font-black text-slate-100 tracking-tight font-mono">
            {totalResources.toLocaleString()}
          </span>
        </div>
        <div className="mt-2 flex items-center justify-between text-[11px]">
          <span className="text-emerald-400 flex items-center space-x-0.5 font-medium">
            <TrendingUp className="w-3 h-3 inline mr-0.5" />
            <span>12.5% vs last 24h</span>
          </span>
          <svg className="w-16 h-6 text-sky-400 opacity-60" viewBox="0 0 60 20">
            <path d="M0,15 Q15,5 30,12 T60,4" fill="none" stroke="currentColor" strokeWidth="2" />
          </svg>
        </div>
      </div>

      {/* 2. Compliance Rules */}
      <div className="relative rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-4 shadow-lg overflow-hidden group hover:border-emerald-500/40 transition-all">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">COMPLIANCE RULES</span>
          <div className="p-2 rounded-xl bg-emerald-950/60 border border-emerald-500/20 text-emerald-400">
            <ShieldCheck className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 flex items-center justify-between">
          <span className="text-2xl font-black text-slate-100 tracking-tight font-mono">{complianceCount}</span>
          <div className="flex items-center justify-center w-9 h-9 rounded-full border-2 border-emerald-500/80 bg-emerald-950/40 text-[11px] font-bold text-emerald-400 font-mono shadow-[0_0_10px_rgba(16,185,129,0.25)]">
            {compliancePassRate}%
          </div>
        </div>
        <div className="mt-2 text-[11px] text-emerald-400 font-medium">
          {compliancePassRate}% Passing
        </div>
      </div>

      {/* 3. Topology Status */}
      <div className="relative rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-4 shadow-lg overflow-hidden group hover:border-emerald-500/40 transition-all">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">TOPOLOGY STATUS</span>
          <div className="p-2 rounded-xl bg-emerald-950/60 border border-emerald-500/20 text-emerald-400">
            <Share2 className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2">
          <span className="text-xl font-bold text-slate-100 tracking-tight">Operational</span>
        </div>
        <div className="mt-3 flex items-center justify-between text-[11px]">
          <span className="text-slate-400">Deterministic BFS</span>
          <svg className="w-16 h-6 text-emerald-400 opacity-80" viewBox="0 0 60 20">
            <path d="M0,12 Q15,18 30,8 T60,6" fill="none" stroke="currentColor" strokeWidth="2" />
          </svg>
        </div>
      </div>

      {/* 4. Security Score */}
      <div className="relative rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-4 shadow-lg overflow-hidden group hover:border-purple-500/40 transition-all">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">SECURITY SCORE</span>
          <div className="p-2 rounded-xl bg-purple-950/60 border border-purple-500/20 text-purple-400">
            <Shield className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 flex items-baseline space-x-1.5">
          <span className="text-2xl font-black text-slate-100 tracking-tight font-mono">85</span>
          <span className="text-xs text-slate-500 font-mono">/ 100</span>
        </div>
        <div className="mt-2 space-y-1.5">
          <div className="text-[11px] text-slate-400">Good</div>
          <div className="w-full h-1.5 bg-slate-900 rounded-full overflow-hidden border border-slate-800">
            <div className="h-full bg-gradient-to-r from-purple-500 to-indigo-400 rounded-full shadow-[0_0_8px_rgba(168,85,247,0.5)]" style={{ width: '85%' }} />
          </div>
        </div>
      </div>

      {/* 5. Est. Monthly Cost */}
      <div className="relative rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-4 shadow-lg overflow-hidden group hover:border-sky-500/40 transition-all">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">EST. MONTHLY COST</span>
          <div className="p-2 rounded-xl bg-sky-950/60 border border-sky-500/20 text-sky-400">
            <DollarSign className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 flex items-baseline">
          <span className="text-2xl font-black text-slate-100 tracking-tight font-mono">
            ${monthlyCost.toLocaleString()}
          </span>
        </div>
        <div className="mt-2 flex items-center justify-between text-[11px]">
          <span className="text-emerald-400 flex items-center space-x-0.5 font-medium">
            <TrendingUp className="w-3 h-3 inline mr-0.5" />
            <span>8.2% vs last month</span>
          </span>
          <svg className="w-16 h-6 text-sky-400 opacity-60" viewBox="0 0 60 20">
            <path d="M0,18 Q20,15 35,8 T60,2" fill="none" stroke="currentColor" strokeWidth="2" />
          </svg>
        </div>
      </div>
    </div>
  );
};