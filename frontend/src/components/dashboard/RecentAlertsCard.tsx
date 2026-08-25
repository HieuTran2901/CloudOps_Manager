import React from 'react';
import { AlertTriangle, ShieldAlert, Info, GitBranch } from 'lucide-react';

interface RecentAlertsCardProps {
  onViewAll?: () => void;
}

export const RecentAlertsCard: React.FC<RecentAlertsCardProps> = ({ onViewAll }) => {
  const alerts = [
    {
      id: '1',
      title: 'High severity finding detected',
      detail: 'S3 bucket publicly accessible',
      time: '2m ago',
      icon: AlertTriangle,
      iconColor: 'text-rose-400 bg-rose-950/60 border-rose-500/30',
      titleColor: 'text-rose-300',
    },
    {
      id: '2',
      title: 'Compliance rule failed',
      detail: 'IAM password policy not enforced',
      time: '15m ago',
      icon: ShieldAlert,
      iconColor: 'text-amber-400 bg-amber-950/60 border-amber-500/30',
      titleColor: 'text-amber-300',
    },
    {
      id: '3',
      title: 'New resource detected',
      detail: 'EC2 instance i-0a1b2c3d4e5f6',
      time: '22m ago',
      icon: Info,
      iconColor: 'text-sky-400 bg-sky-950/60 border-sky-500/30',
      titleColor: 'text-sky-300',
    },
    {
      id: '4',
      title: 'Drift detected',
      detail: 'Security group rule modified',
      time: '1h ago',
      icon: GitBranch,
      iconColor: 'text-purple-400 bg-purple-950/60 border-purple-500/30',
      titleColor: 'text-purple-300',
    },
  ];

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
        {alerts.map((a) => {
          const Icon = a.icon;
          return (
            <div
              key={a.id}
              className="flex items-start justify-between p-2.5 rounded-xl bg-slate-900/50 hover:bg-slate-900 border border-slate-800/60 transition-colors"
            >
              <div className="flex items-start space-x-3">
                <div className={`p-1.5 rounded-lg border flex-shrink-0 mt-0.5 ${a.iconColor}`}>
                  <Icon className="w-3.5 h-3.5" />
                </div>
                <div>
                  <h4 className={`text-xs font-semibold ${a.titleColor}`}>{a.title}</h4>
                  <p className="text-[11px] text-slate-400 mt-0.5 font-mono">{a.detail}</p>
                </div>
              </div>
              <span className="text-[10px] text-slate-500 font-mono flex-shrink-0 ml-2">{a.time}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
};