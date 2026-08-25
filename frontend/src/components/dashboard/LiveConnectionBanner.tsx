import React from 'react';
import { Globe } from 'lucide-react';

interface LiveConnectionBannerProps {
  onViewConnection?: () => void;
}

export const LiveConnectionBanner: React.FC<LiveConnectionBannerProps> = ({ onViewConnection }) => {
  return (
    <div className="relative rounded-2xl border border-slate-800/80 bg-gradient-to-r from-[#0d1527] via-[#0f1b33] to-[#0a1020] p-5 shadow-2xl overflow-hidden backdrop-blur-md">
      {/* Background ambient glow */}
      <div className="absolute -left-10 -top-10 w-48 h-48 bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute right-1/3 -bottom-10 w-48 h-48 bg-purple-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="relative flex flex-col md:flex-row items-start md:items-center justify-between gap-6 z-10">
        {/* Left: Holographic Orb + Text */}
        <div className="flex items-center space-x-5">
          {/* Glowing Hologram Orb */}
          <div className="relative flex-shrink-0 w-16 h-16 rounded-full bg-gradient-to-tr from-sky-600/30 via-indigo-500/20 to-purple-600/30 border border-sky-400/40 flex items-center justify-center shadow-[0_0_25px_rgba(56,189,248,0.25)]">
            <div className="absolute inset-1 rounded-full border border-sky-400/20 animate-spin" style={{ animationDuration: '8s' }} />
            <div className="absolute inset-2 rounded-full border border-indigo-400/30 animate-pulse" />
            <Globe className="w-8 h-8 text-sky-300 drop-shadow-[0_0_8px_rgba(56,189,248,0.8)]" />
          </div>

          <div className="space-y-1">
            <div className="flex items-center space-x-2.5">
              <span className="flex items-center space-x-1.5 text-xs font-semibold text-slate-200">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping inline-block" />
                <span className="w-2 h-2 rounded-full bg-emerald-400 inline-block -ml-3.5" />
                <span>Live Connection</span>
              </span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold tracking-wider uppercase bg-emerald-950/80 text-emerald-400 border border-emerald-500/30 shadow-[0_0_10px_rgba(16,185,129,0.2)]">
                CONNECTED
              </span>
            </div>
            <p className="text-xs text-slate-300/80">
              All systems are online and collecting data in real-time.
            </p>
          </div>
        </div>

        {/* Center-Right: Metadata & Action */}
        <div className="flex flex-wrap items-center gap-6 text-xs">
          <div>
            <span className="text-[11px] text-slate-400 block">Data Source</span>
            <span className="font-medium text-slate-200 mt-0.5 block">AWS Evidence Service</span>
          </div>

          <div className="hidden sm:block w-px h-8 bg-slate-800" />

          <div>
            <span className="text-[11px] text-slate-400 block">Last Sync</span>
            <span className="font-medium text-slate-200 mt-0.5 block font-mono">34s ago</span>
          </div>

          <button
            onClick={onViewConnection}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-200 bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700/80 hover:border-sky-500/50 shadow-md transition-all flex items-center space-x-2"
          >
            <span>View Connection</span>
          </button>
        </div>
      </div>
    </div>
  );
};