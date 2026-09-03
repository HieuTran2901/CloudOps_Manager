import React, { useState } from 'react';
import { TrendingUp, Box } from 'lucide-react';

interface BottomTrendCardsProps {
  monthlyCost?: number;
  openFindingsCount?: number;
  onViewAllRisks?: () => void;
}

export const BottomTrendCards: React.FC<BottomTrendCardsProps> = ({
  monthlyCost = 0,
  openFindingsCount = 0,
  onViewAllRisks,
}) => {
  const [costPeriod, setCostPeriod] = useState('Last 30 Days');
  const [secPeriod, setSecPeriod] = useState('Last 7 Days');

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      {/* 1. Cost Trend Card */}
      <div className="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-5 shadow-lg flex flex-col justify-between">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold uppercase tracking-wider text-slate-300">
            COST TREND
          </span>
          <select
            value={costPeriod}
            onChange={(e) => setCostPeriod(e.target.value)}
            className="bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1 text-[11px] text-slate-300 focus:outline-none cursor-pointer"
          >
            <option value="Last 30 Days">Last 30 Days</option>
            <option value="Last 90 Days">Last 90 Days</option>
          </select>
        </div>

        <div className="mt-4 flex items-baseline space-x-3">
          <span className="text-2xl font-black text-slate-100 font-mono tracking-tight">${monthlyCost.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
          <span className="text-emerald-400 text-xs font-semibold flex items-center">
            <TrendingUp className="w-3.5 h-3.5 mr-0.5" />
            <span>0.0%</span>
          </span>
        </div>

        {/* Mini Area Chart */}
        <div className="mt-3 h-12 w-full">
          <svg viewBox="0 0 200 40" className="w-full h-full">
            <defs>
              <linearGradient id="costGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.4" />
                <stop offset="100%" stopColor="#38bdf8" stopOpacity="0" />
              </linearGradient>
            </defs>
            <path d="M0,35 Q40,30 80,18 T160,22 T200,8 L200,40 L0,40 Z" fill="url(#costGrad)" />
            <path d="M0,35 Q40,30 80,18 T160,22 T200,8" fill="none" stroke="#38bdf8" strokeWidth="2" />
          </svg>
        </div>
      </div>

      {/* 2. Security Findings Card */}
      <div className="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-5 shadow-lg flex flex-col justify-between">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold uppercase tracking-wider text-slate-300">
            SECURITY FINDINGS
          </span>
          <select
            value={secPeriod}
            onChange={(e) => setSecPeriod(e.target.value)}
            className="bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1 text-[11px] text-slate-300 focus:outline-none cursor-pointer"
          >
            <option value="Last 7 Days">Last 7 Days</option>
            <option value="Last 30 Days">Last 30 Days</option>
          </select>
        </div>

        <div className="mt-4 flex items-baseline space-x-3">
          <span className="text-2xl font-black text-slate-100 font-mono tracking-tight">{openFindingsCount}</span>
          <span className="text-rose-400 text-xs font-semibold flex items-center">
            <TrendingUp className="w-3.5 h-3.5 mr-0.5" />
            <span>Active Findings</span>
          </span>
        </div>

        {/* Mini Line Chart */}
        <div className="mt-3 h-12 w-full">
          <svg viewBox="0 0 200 40" className="w-full h-full">
            <defs>
              <linearGradient id="secGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#f87171" stopOpacity="0.4" />
                <stop offset="100%" stopColor="#f87171" stopOpacity="0" />
              </linearGradient>
            </defs>
            <path d="M0,30 Q50,38 100,20 T150,32 T200,12 L200,40 L0,40 Z" fill="url(#secGrad)" />
            <path d="M0,30 Q50,38 100,20 T150,32 T200,12" fill="none" stroke="#f87171" strokeWidth="2" />
          </svg>
        </div>
      </div>

      {/* 3. Top Risks Card */}
      <div className="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-5 shadow-lg flex flex-col justify-between">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold uppercase tracking-wider text-slate-300">
            TOP RISKS
          </span>
          <button
            onClick={onViewAllRisks}
            className="text-xs text-sky-400 hover:text-sky-300 font-medium transition-colors"
          >
            View All
          </button>
        </div>

        <div className="mt-3 flex items-center justify-between p-3 rounded-xl bg-slate-900/60 border border-slate-800/60">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-lg bg-rose-950/60 border border-rose-500/30 text-rose-400">
              <Box className="w-4 h-4" />
            </div>
            <div>
              <h4 className="text-xs font-semibold text-slate-200">
                {openFindingsCount > 0 ? 'Unrestricted Administrative Ingress' : 'No Critical Security Risks'}
              </h4>
              <span className="inline-block mt-0.5 px-1.5 py-0.5 rounded text-[9px] font-bold uppercase tracking-wider bg-rose-950 text-rose-400 border border-rose-800">
                {openFindingsCount > 0 ? 'High' : 'Healthy'}
              </span>
            </div>
          </div>
          <span className="text-xs font-mono text-slate-400 font-medium">{openFindingsCount} Findings</span>
        </div>
      </div>
    </div>
  );
};