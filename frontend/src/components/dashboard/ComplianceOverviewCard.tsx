import React from 'react';

interface ComplianceOverviewCardProps {
  onViewAll?: () => void;
}

export const ComplianceOverviewCard: React.FC<ComplianceOverviewCardProps> = ({ onViewAll }) => {
  return (
    <div className="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-5 shadow-lg flex flex-col justify-between">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-bold uppercase tracking-wider text-slate-300">
          COMPLIANCE OVERVIEW
        </span>
      </div>

      <div className="my-4 flex flex-col sm:flex-row items-center justify-between gap-6">
        {/* Donut Gauge Chart */}
        <div className="relative w-36 h-36 flex items-center justify-center flex-shrink-0">
          <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
            {/* Background Track */}
            <circle cx="50" cy="50" r="38" fill="none" stroke="#1e293b" strokeWidth="11" />
            {/* Passed: 92% (Green) */}
            <circle
              cx="50"
              cy="50"
              r="38"
              fill="none"
              stroke="#10b981"
              strokeWidth="11"
              strokeDasharray="220 238"
              strokeDashoffset="0"
              className="drop-shadow-[0_0_6px_rgba(16,185,129,0.6)]"
            />
            {/* Warning: 5% (Amber) */}
            <circle
              cx="50"
              cy="50"
              r="38"
              fill="none"
              stroke="#f59e0b"
              strokeWidth="11"
              strokeDasharray="12 238"
              strokeDashoffset="-220"
            />
            {/* Failed: 3% (Red) */}
            <circle
              cx="50"
              cy="50"
              r="38"
              fill="none"
              stroke="#ef4444"
              strokeWidth="11"
              strokeDasharray="8 238"
              strokeDashoffset="-232"
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
            <span className="text-2xl font-black text-slate-100 font-mono tracking-tight leading-none">156</span>
            <span className="text-[10px] text-slate-400 font-medium mt-1">Total Rules</span>
          </div>
        </div>

        {/* Legend */}
        <div className="space-y-2 text-xs w-full max-w-[150px]">
          <div className="flex items-center justify-between">
            <span className="flex items-center space-x-2 text-slate-300">
              <span className="w-2 h-2 rounded-full bg-emerald-400 shadow-[0_0_6px_#10b981]" />
              <span>Passed</span>
            </span>
            <span className="font-mono text-slate-200">144 (92%)</span>
          </div>

          <div className="flex items-center justify-between">
            <span className="flex items-center space-x-2 text-slate-300">
              <span className="w-2 h-2 rounded-full bg-amber-400" />
              <span>Warning</span>
            </span>
            <span className="font-mono text-slate-200">8 (5%)</span>
          </div>

          <div className="flex items-center justify-between">
            <span className="flex items-center space-x-2 text-slate-300">
              <span className="w-2 h-2 rounded-full bg-rose-500" />
              <span>Failed</span>
            </span>
            <span className="font-mono text-slate-200">4 (3%)</span>
          </div>

          <div className="flex items-center justify-between">
            <span className="flex items-center space-x-2 text-slate-400">
              <span className="w-2 h-2 rounded-full bg-slate-600" />
              <span>Ignored</span>
            </span>
            <span className="font-mono text-slate-400">0 (0%)</span>
          </div>
        </div>
      </div>

      <button
        onClick={onViewAll}
        className="w-full py-2 rounded-xl bg-slate-900/90 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-xs font-semibold text-slate-200 transition-colors text-center"
      >
        View All Rules
      </button>
    </div>
  );
};