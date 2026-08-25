import React from 'react';
import clsx from 'clsx';

interface StatusBadgeProps {
  status: string;
  className?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, className }) => {
  const normalized = status.toUpperCase();

  const getVariant = () => {
    switch (normalized) {
      case 'PASS':
      case 'RUNNING':
      case 'AVAILABLE':
      case 'IN_SYNC':
      case 'NOT_EXPOSED':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'FAIL':
      case 'EXPOSED':
      case 'DRIFTED':
      case 'STOPPED':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
      case 'INSUFFICIENT_EVIDENCE':
      case 'WARNING':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
      default:
        return 'bg-slate-700/20 text-slate-400 border-slate-700/30';
    }
  };

  return (
    <span
      className={clsx(
        'inline-flex items-center px-2 py-0.5 rounded text-xs font-medium border font-mono',
        getVariant(),
        className
      )}
    >
      {normalized}
    </span>
  );
};