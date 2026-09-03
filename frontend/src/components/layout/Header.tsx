import React from 'react';
import { Shield, Bell, Activity, Search, ChevronDown } from 'lucide-react';
import { APP_CONFIG } from '../../config/env';

interface HeaderProps {
  currentRegion: string;
  onRegionChange: (region: string) => void;
  accountId?: string;
}

export const Header: React.FC<HeaderProps> = ({
  currentRegion,
  onRegionChange,
  accountId = '351405419700',
}) => {
  const regions = ['ap-southeast-2', 'us-east-1', 'us-west-2', 'eu-west-1', 'ap-southeast-1'];

  return (
    <header className="h-16 border-b border-slate-800/80 bg-[#090e1a]/90 backdrop-blur-md px-6 flex items-center justify-between sticky top-0 z-30">
      {/* Left: Brand Logo + Version */}
      <div className="flex items-center space-x-3">
        <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-sky-600 to-blue-500 flex items-center justify-center shadow-[0_0_15px_rgba(56,189,248,0.4)]">
          <Shield className="w-4 h-4 text-white" />
        </div>
        <span className="font-bold text-base text-slate-100 tracking-tight">CloudOps Manager</span>
        <span className="text-[10px] font-mono font-semibold bg-sky-950/80 border border-sky-800/60 text-sky-400 px-2 py-0.5 rounded">
          v{APP_CONFIG.version}
        </span>
      </div>

      {/* Center: Search Bar */}
      <div className="hidden md:flex items-center w-full max-w-md mx-6">
        <div className="relative w-full">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-2.5" />
          <input
            type="text"
            placeholder="Search resources, accounts, regions..."
            className="w-full pl-10 pr-9 py-2 bg-[#0d1424] border border-slate-800 rounded-xl text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500/60 shadow-inner"
          />
          <span className="absolute right-3 top-2 px-1.5 py-0.5 rounded bg-slate-800 text-[10px] text-slate-400 font-mono">
            /
          </span>
        </div>
      </div>

      {/* Right: Actions & Selectors */}
      <div className="flex items-center space-x-4 text-xs">
        <div className="flex items-center space-x-2">
          {/* Notifications */}
          <button className="relative p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200">
            <Bell className="w-4 h-4" />
            <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-rose-500 text-white font-bold text-[9px] flex items-center justify-center">
              3
            </span>
          </button>
          {/* Signal / Activity */}
          <button className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200">
            <Activity className="w-4 h-4" />
          </button>
        </div>

        {/* Account Selector */}
        <div className="flex flex-col bg-slate-900/90 px-3 py-1.5 rounded-xl border border-slate-800">
          <span className="text-[9px] text-slate-500 uppercase font-semibold">Account</span>
          <div className="flex items-center space-x-1 cursor-pointer">
            <span className="font-semibold text-slate-200 text-xs">{accountId}</span>
            <ChevronDown className="w-3 h-3 text-slate-400" />
          </div>
        </div>

        {/* Region Selector */}
        <div className="flex flex-col bg-slate-900/90 px-3 py-1.5 rounded-xl border border-slate-800">
          <span className="text-[9px] text-slate-500 uppercase font-semibold">Region</span>
          <select
            value={currentRegion}
            onChange={(e) => onRegionChange(e.target.value)}
            className="bg-transparent text-xs font-semibold text-slate-200 font-mono focus:outline-none cursor-pointer"
          >
            {regions.map((r) => (
              <option key={r} value={r} className="bg-slate-900 text-slate-200">
                {r}
              </option>
            ))}
          </select>
        </div>
      </div>
    </header>
  );
};