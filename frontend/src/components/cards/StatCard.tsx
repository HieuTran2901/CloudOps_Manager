import React from 'react';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  variant?: 'default' | 'success' | 'warning' | 'danger';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon: Icon,
  variant = 'default',
}) => {
  const getIconColor = () => {
    switch (variant) {
      case 'success': return 'text-emerald-400 bg-emerald-950/40 border-emerald-500/20';
      case 'warning': return 'text-amber-400 bg-amber-950/40 border-amber-500/20';
      case 'danger': return 'text-rose-400 bg-rose-950/40 border-rose-500/20';
      default: return 'text-sky-400 bg-sky-950/40 border-sky-500/20';
    }
  };

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-medium uppercase tracking-wider text-slate-400">{title}</p>
          <p className="mt-1 text-2xl font-bold text-slate-100">{value}</p>
          {subtitle && <p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>}
        </div>
        <div className={`p-3 rounded-lg border ${getIconColor()}`}>
          <Icon className="w-5 h-5" />
        </div>
      </div>
    </div>
  );
};