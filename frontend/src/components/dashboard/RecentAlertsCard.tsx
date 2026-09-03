import React from 'react';
import { AlertTriangle, ShieldAlert } from 'lucide-react';

export interface LiveAlert {
  id: string;
  title: string;
  detail: string;
  time: string;
  severity?: 'HIGH' | 'MEDIUM' | 'LOW';
}

interface RecentAlertsCardProps {
  alerts?: LiveAlert[];
  onViewAll?: () => void;
}

export const RecentAlertsCard: React.FC<RecentAlertsCardProps> = ({ alerts = [], onViewAll }) => {

  return (
    <div className="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-5 shadow-lg flex flex-col justify-between">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-bold uppercase tracking-wider text-slate-300">
          RECENT ALERTS
        </span>
        <button
          onClick={onViewAll}
          className="text-xs text-sky-400 hover:text-sky-300 font-medium transition-colors"
        >
          View All
        </button>
      </div>

      <div className="my-2 space-y-3">
        {alerts.length === 0 ? (
          <div className="p-4 text-center text-xs text-slate-400 font-medium">
            No active live alerts in current scope
          </div>
        ) : (
          alerts.map((a) => {
            const Icon = a.severity === 'HIGH' ? AlertTriangle : ShieldAlert;
            const iconColor = a.severity === 'HIGH' ? 'text-rose-400 bg-rose-950/60 border-rose-500/30' : 'text-amber-400 bg-amber-950/60 border-amber-500/30';
            const titleColor = a.severity === 'HIGH' ? 'text-rose-300' : 'text-amber-300';
            return (
              <div
                key={a.id}
                className="flex items-start justify-between p-2.5 rounded-xl bg-slate-900/50 hover:bg-slate-900 border border-slate-800/60 transition-colors"
              >
                <div className="flex items-start space-x-3">
                  <div className={`p-1.5 rounded-lg border flex-shrink-0 mt-0.5 ${iconColor}`}>
                    <Icon className="w-3.5 h-3.5" />
                  </div>
                  <div>
                    <h4 className={`text-xs font-semibold ${titleColor}`}>{a.title}</h4>
                    <p className="text-[11px] text-slate-400 mt-0.5 font-mono">{a.detail}</p>
                  </div>
                </div>
                <span className="text-[10px] text-slate-500 font-mono flex-shrink-0 ml-2">{a.time}</span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};