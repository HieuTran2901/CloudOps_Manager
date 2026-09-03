import React from 'react';
import { Box, ShieldCheck, Share2, Shield, DollarSign, TrendingUp, AlertTriangle, ShieldAlert } from 'lucide-react';

export type ApiMetricStatus = 'SUCCESS' | 'ERROR' | 'DENIED' | 'LOADING';

interface TopMetricCardsProps {
  totalResources?: number;
  complianceCount?: number;
  compliancePassRate?: number;
  monthlyCost?: number;
  securityScore?: number;
  topologyNodes?: number;
  topologyEdges?: number;

  resourcesStatus?: ApiMetricStatus;
  complianceStatus?: ApiMetricStatus;
  topologyStatus?: ApiMetricStatus;
  costStatus?: ApiMetricStatus;
}

export const TopMetricCards: React.FC<TopMetricCardsProps> = ({
  totalResources = 0,
  complianceCount = 0,
  compliancePassRate = 0,
  monthlyCost = 0,
  securityScore = 0,
  topologyNodes = 0,
  topologyEdges = 0,
  resourcesStatus = 'SUCCESS',
  complianceStatus = 'SUCCESS',
  topologyStatus = 'SUCCESS',
  costStatus = 'SUCCESS',
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
          {resourcesStatus === 'DENIED' ? (
            <span className="text-sm font-bold text-amber-400 flex items-center space-x-1">
              <ShieldAlert className="w-4 h-4 inline mr-1" /> Access Denied
            </span>
          ) : resourcesStatus === 'ERROR' ? (
            <span className="text-sm font-bold text-rose-400 flex items-center space-x-1">
              <AlertTriangle className="w-4 h-4 inline mr-1" /> Unavailable
            </span>
          ) : (
            <span className="text-2xl font-black text-slate-100 tracking-tight font-mono">
              {totalResources.toLocaleString()}
            </span>
          )}
        </div>
        <div className="mt-2 flex items-center justify-between text-[11px]">
          {resourcesStatus === 'SUCCESS' && totalResources === 0 ? (
            <span className="text-slate-400 font-medium italic">Empty Region</span>
          ) : (
            <span className="text-emerald-400 flex items-center space-x-0.5 font-medium">
              <TrendingUp className="w-3 h-3 inline mr-0.5" />
              <span>Live Discovered</span>
            </span>
          )}
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
          {complianceStatus === 'DENIED' ? (
            <span className="text-sm font-bold text-amber-400 flex items-center space-x-1">
              <ShieldAlert className="w-4 h-4 inline mr-1" /> Access Denied
            </span>
          ) : complianceStatus === 'ERROR' ? (
            <span className="text-sm font-bold text-rose-400 flex items-center space-x-1">
              <AlertTriangle className="w-4 h-4 inline mr-1" /> Unavailable
            </span>
          ) : (
            <>
              <span className="text-2xl font-black text-slate-100 tracking-tight font-mono">{complianceCount}</span>
              <div className="flex items-center justify-center w-9 h-9 rounded-full border-2 border-emerald-500/80 bg-emerald-950/40 text-[11px] font-bold text-emerald-400 font-mono shadow-[0_0_10px_rgba(16,185,129,0.25)]">
                {compliancePassRate}%
              </div>
            </>
          )}
        </div>
        <div className="mt-2 text-[11px] text-emerald-400 font-medium">
          {complianceStatus === 'SUCCESS' ? `${compliancePassRate}% Passing` : 'Rule Engine Status'}
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
          {topologyStatus === 'DENIED' ? (
            <span className="text-sm font-bold text-amber-400">Access Denied</span>
          ) : topologyStatus === 'ERROR' ? (
            <span className="text-sm font-bold text-rose-400">Unavailable</span>
          ) : (
            <span className="text-xl font-bold text-slate-100 tracking-tight">Operational</span>
          )}
        </div>
        <div className="mt-3 flex items-center justify-between text-[11px]">
          <span className="text-slate-400 font-mono">
            {topologyStatus === 'SUCCESS' ? `${topologyNodes} Nodes / ${topologyEdges} Edges` : 'Graph Engine'}
          </span>
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
          {complianceStatus === 'DENIED' || complianceStatus === 'ERROR' ? (
            <span className="text-sm font-bold text-slate-400">N/A</span>
          ) : (
            <>
              <span className="text-2xl font-black text-slate-100 tracking-tight font-mono">{securityScore}</span>
              <span className="text-xs text-slate-500 font-mono">/ 100</span>
            </>
          )}
        </div>
        <div className="mt-2 space-y-1.5">
          <div className="text-[11px] text-slate-400">
            {complianceStatus !== 'SUCCESS' ? 'Evaluating' : securityScore >= 80 ? 'Good' : securityScore >= 50 ? 'Warning' : 'Critical'}
          </div>
          <div className="w-full h-1.5 bg-slate-900 rounded-full overflow-hidden border border-slate-800">
            <div className="h-full bg-gradient-to-r from-purple-500 to-indigo-400 rounded-full shadow-[0_0_8px_rgba(168,85,247,0.5)]" style={{ width: `${Math.min(100, Math.max(0, securityScore))}%` }} />
          </div>
        </div>
      </div>

      {/* 5. Est. Monthly Cost (Account-Wide Scope Clarification) */}
      <div className="relative rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-4 shadow-lg overflow-hidden group hover:border-sky-500/40 transition-all">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">EST. MONTHLY COST</span>
          <div className="p-2 rounded-xl bg-sky-950/60 border border-sky-500/20 text-sky-400">
            <DollarSign className="w-4 h-4" />
          </div>
        </div>
        <div className="mt-2 flex items-baseline">
          {costStatus === 'DENIED' ? (
            <span className="text-sm font-bold text-amber-400">Access Denied</span>
          ) : costStatus === 'ERROR' ? (
            <span className="text-sm font-bold text-rose-400">Unavailable</span>
          ) : (
            <span className="text-2xl font-black text-slate-100 tracking-tight font-mono">
              ${monthlyCost.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
          )}
        </div>
        <div className="mt-2 flex items-center justify-between text-[11px]">
          <span className="text-sky-400 flex items-center space-x-0.5 font-medium text-[10px] bg-sky-950/80 px-1.5 py-0.5 rounded border border-sky-800/60">
            <span>Account-wide Unblended</span>
          </span>
          <svg className="w-16 h-6 text-sky-400 opacity-60" viewBox="0 0 60 20">
            <path d="M0,18 Q20,15 35,8 T60,2" fill="none" stroke="currentColor" strokeWidth="2" />
          </svg>
        </div>
      </div>
    </div>
  );
};