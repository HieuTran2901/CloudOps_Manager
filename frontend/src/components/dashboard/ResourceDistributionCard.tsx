import React, { useState } from 'react';
import { Server, Database, Box, Cpu, Network, Key, Layers, MoreHorizontal } from 'lucide-react';

export const ResourceDistributionCard: React.FC = () => {
  const [filter, setFilter] = useState('By Service');

  const services = [
    { name: 'EC2', count: 780, max: 2000, color: 'from-orange-500 to-red-500', icon: Server, iconColor: 'text-orange-400 bg-orange-950/40 border-orange-500/30' },
    { name: 'S3', count: 1240, max: 2000, color: 'from-emerald-500 to-teal-500', icon: Box, iconColor: 'text-emerald-400 bg-emerald-950/40 border-emerald-500/30' },
    { name: 'RDS', count: 1850, max: 2000, color: 'from-blue-500 to-indigo-500', icon: Database, iconColor: 'text-blue-400 bg-blue-950/40 border-blue-500/30' },
    { name: 'Lambda', count: 1420, max: 2000, color: 'from-amber-500 to-orange-500', icon: Cpu, iconColor: 'text-amber-400 bg-amber-950/40 border-amber-500/30' },
    { name: 'VPC', count: 960, max: 2000, color: 'from-purple-500 to-pink-500', icon: Network, iconColor: 'text-purple-400 bg-purple-950/40 border-purple-500/30' },
    { name: 'IAM', count: 1100, max: 2000, color: 'from-red-500 to-rose-600', icon: Key, iconColor: 'text-rose-400 bg-rose-950/40 border-rose-500/30' },
    { name: 'DynamoDB', count: 890, max: 2000, color: 'from-cyan-500 to-blue-500', icon: Layers, iconColor: 'text-cyan-400 bg-cyan-950/40 border-cyan-500/30' },
    { name: 'Others', count: 1500, max: 2000, color: 'from-indigo-500 to-purple-600', icon: MoreHorizontal, iconColor: 'text-indigo-400 bg-indigo-950/40 border-indigo-500/30' },
  ];

  return (
    <div className="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-5 shadow-lg flex flex-col justify-between">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-bold uppercase tracking-wider text-slate-300">
          RESOURCE DISTRIBUTION
        </span>
        <select
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1 text-[11px] text-slate-300 focus:outline-none cursor-pointer"
        >
          <option value="By Service">By Service</option>
          <option value="By Region">By Region</option>
        </select>
      </div>

      {/* Bar Chart Canvas */}
      <div className="my-3 flex items-end justify-between gap-2 h-44 pt-4 pb-1">
        {/* Y-Axis Ticks */}
        <div className="flex flex-col justify-between h-full text-[9px] text-slate-500 font-mono pr-1 border-r border-slate-800/60 select-none">
          <span>2K</span>
          <span>1.5K</span>
          <span>1K</span>
          <span>500</span>
          <span>0</span>
        </div>

        {/* Bars Container */}
        <div className="flex-1 flex items-end justify-around h-full gap-2 px-1">
          {services.map((s) => {
            const heightPct = Math.round((s.count / s.max) * 100);
            const Icon = s.icon;
            return (
              <div key={s.name} className="flex-1 flex flex-col items-center h-full justify-end group">
                {/* Bar */}
                <div className="w-full max-w-[22px] flex-1 flex items-end">
                  <div
                    className={`w-full rounded-t-md bg-gradient-to-t ${s.color} opacity-85 group-hover:opacity-100 transition-all shadow-[0_0_8px_rgba(59,130,246,0.2)]`}
                    style={{ height: `${heightPct}%` }}
                    title={`${s.name}: ${s.count}`}
                  />
                </div>
                {/* Icon & Label */}
                <div className="mt-2 flex flex-col items-center">
                  <div className={`w-5 h-5 rounded-md flex items-center justify-center border ${s.iconColor} text-[10px]`}>
                    <Icon className="w-3 h-3" />
                  </div>
                  <span className="text-[9px] text-slate-400 font-mono mt-1 group-hover:text-slate-200">
                    {s.name}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};