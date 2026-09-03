import React from 'react';

interface ComplianceOverviewCardProps {
  totalRules?: number;
  passed?: number;
  warning?: number;
  failed?: number;
  ignored?: number;
  onViewAll?: () => void;
}

export const ComplianceOverviewCard: React.FC<ComplianceOverviewCardProps> = ({
  totalRules = 0,
  passed = 0,
  warning = 0,
  failed = 0,
  ignored = 0,
  onViewAll,
}) => {
  const total = totalRules || 1;
  const passPct = Math.round((passed / total) * 100);
  const warnPct = Math.round((warning / total) * 100);
  const failPct = Math.round((failed / total) * 100);
  const ignPct = Math.round((ignored / total) * 100);

  const circumference = 238;
  const passDash = Math.round((passed / total) * circumference);
  const warnDash = Math.round((warning / total) * circumference);
  const failDash = Math.round((failed / total) * circumference);

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
            {/* Passed */}
            <circle
              cx="50"
              cy="50"
              r="38"
              fill="none"
              stroke="#10b981"
              strokeWidth="11"
              strokeDasharray={`${passDash} ${circumference}`}
              strokeDashoffset="0"
              className="drop-shadow-[0_0_6px_rgba(16,185,129,0.6)]"
            />
            {/* Warning */}
            <circle
              cx="50"
              cy="50"
              r="38"
              fill="none"
              stroke="#f59e0b"
              strokeWidth="11"
              strokeDasharray={`${warnDash} ${circumference}`}
              strokeDashoffset={`-${passDash}`}
            />
            {/* Failed */}
            <circle
              cx="50"
              cy="50"
              r="38"
              fill="none"
              stroke="#ef4444"
              strokeWidth="11"
              strokeDasharray={`${failDash} ${circumference}`}
              strokeDashoffset={`-${passDash + warnDash}`}
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
            <span className="text-2xl font-black text-slate-100 font-mono tracking-tight leading-none">{totalRules}</span>
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
            <span className="font-mono text-slate-200">{passed} ({passPct}%)</span>
          </div>

          <div className="flex items-center justify-between">
            <span className="flex items-center space-x-2 text-slate-300">
              <span className="w-2 h-2 rounded-full bg-amber-400" />
              <span>Warning</span>
            </span>
            <span className="font-mono text-slate-200">{warning} ({warnPct}%)</span>
          </div>

          <div className="flex items-center justify-between">
            <span className="flex items-center space-x-2 text-slate-300">
              <span className="w-2 h-2 rounded-full bg-rose-500" />
              <span>Failed</span>
            </span>
            <span className="font-mono text-slate-200">{failed} ({failPct}%)</span>
          </div>

          <div className="flex items-center justify-between">
            <span className="flex items-center space-x-2 text-slate-400">
              <span className="w-2 h-2 rounded-full bg-slate-600" />
              <span>N/A / Ignored</span>
            </span>
            <span className="font-mono text-slate-400">{ignored} ({ignPct}%)</span>
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