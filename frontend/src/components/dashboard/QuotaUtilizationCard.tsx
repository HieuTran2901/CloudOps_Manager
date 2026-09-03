import React, { useEffect, useState } from 'react';
import { Gauge, AlertTriangle, AlertOctagon, CheckCircle2, HelpCircle, RefreshCw } from 'lucide-react';
import { cloudOpsApi } from '../../api';
import { QuotaUtilizationReport, ServiceQuotaItem } from '../../types/api';

interface QuotaUtilizationCardProps {
  region?: string;
}

export const QuotaUtilizationCard: React.FC<QuotaUtilizationCardProps> = ({ region }) => {
  const [report, setReport] = useState<QuotaUtilizationReport | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchQuotas = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await cloudOpsApi.getQuotas(region);
      if (data) {
        setReport(data);
      } else {
        setError('Failed to retrieve service quotas.');
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Error querying quota utilization.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQuotas();
  }, [region]);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'CRITICAL':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-950/70 border border-rose-500/40 text-rose-300">
            <AlertOctagon className="w-3 h-3 text-rose-400" />
            CRITICAL
          </span>
        );
      case 'WARNING':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-950/70 border border-amber-500/40 text-amber-300">
            <AlertTriangle className="w-3 h-3 text-amber-400" />
            WARNING
          </span>
        );
      case 'NORMAL':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-emerald-950/40 border border-emerald-500/30 text-emerald-300">
            <CheckCircle2 className="w-3 h-3 text-emerald-400" />
            NORMAL
          </span>
        );
      case 'UNKNOWN':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-900 border border-slate-700 text-slate-400">
            <HelpCircle className="w-3 h-3 text-slate-400" />
            UNKNOWN
          </span>
        );
    }
  };

  const getProgressBarColor = (status: string) => {
    switch (status) {
      case 'CRITICAL':
        return 'bg-gradient-to-r from-rose-500 to-red-600 shadow-[0_0_8px_rgba(244,63,94,0.4)]';
      case 'WARNING':
        return 'bg-gradient-to-r from-amber-500 to-orange-500 shadow-[0_0_8px_rgba(245,158,11,0.4)]';
      case 'NORMAL':
        return 'bg-gradient-to-r from-cyan-500 to-blue-500';
      case 'UNKNOWN':
      default:
        return 'bg-slate-800';
    }
  };

  return (
    <div className="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-5 shadow-lg flex flex-col justify-between">
      {/* Header */}
      <div className="flex items-center justify-between pb-3 border-b border-slate-800/60">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-lg bg-blue-950/60 border border-blue-500/30 flex items-center justify-center">
            <Gauge className="w-3.5 h-3.5 text-blue-400" />
          </div>
          <div>
            <h3 className="text-[11px] font-bold uppercase tracking-wider text-slate-200">
              SERVICE QUOTAS & CAPACITY
            </h3>
            <p className="text-[10px] text-slate-500">
              Region: <span className="font-mono text-slate-400">{report?.region || region || 'ap-southeast-2'}</span>
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {report && (
            <div className="flex items-center gap-1.5 text-[11px]">
              {report.criticalCount > 0 && (
                <span className="px-2 py-0.5 rounded-md bg-rose-500/20 border border-rose-500/40 text-rose-300 font-bold font-mono">
                  {report.criticalCount} Critical
                </span>
              )}
              {report.warningCount > 0 && (
                <span className="px-2 py-0.5 rounded-md bg-amber-500/20 border border-amber-500/40 text-amber-300 font-bold font-mono">
                  {report.warningCount} Warn
                </span>
              )}
              {report.unknownCount > 0 && (
                <span className="px-2 py-0.5 rounded-md bg-slate-800 border border-slate-700 text-slate-400 font-mono text-[10px]">
                  {report.unknownCount} Unmonitored
                </span>
              )}
            </div>
          )}
          <button
            onClick={fetchQuotas}
            disabled={loading}
            className="p-1 rounded-md text-slate-400 hover:text-slate-200 hover:bg-slate-800/50 transition-colors"
            title="Refresh quotas"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="my-3 flex-1 flex flex-col justify-center">
        {loading ? (
          <div className="space-y-3 py-2">
            {[1, 2, 3].map((i) => (
              <div key={i} className="animate-pulse space-y-1.5">
                <div className="h-3 bg-slate-800 rounded w-1/3"></div>
                <div className="h-2 bg-slate-800/60 rounded w-full"></div>
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="p-3 rounded-lg bg-rose-950/30 border border-rose-800/40 text-[11px] text-rose-300 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
        ) : !report || !report.quotas || report.quotas.length === 0 ? (
          <div className="py-6 text-center text-slate-500 text-[11px]">
            No service quota data available.
          </div>
        ) : (
          <div className="space-y-3.5 max-h-56 overflow-y-auto pr-1">
            {report.quotas.map((item: ServiceQuotaItem) => {
              const hasUsage = item.currentUsage !== null && item.utilizationPercentage !== null && item.status !== 'UNKNOWN';
              const pct = hasUsage ? Math.min(100, Math.max(0, item.utilizationPercentage || 0)) : 0;

              return (
                <div key={item.quotaCode} className="space-y-1.5 bg-slate-900/40 p-2.5 rounded-xl border border-slate-800/50 hover:border-slate-700/60 transition-colors">
                  <div className="flex items-center justify-between text-[11px]">
                    <div className="flex items-center gap-1.5 truncate max-w-[65%]">
                      <span className="font-semibold text-slate-200 truncate" title={item.quotaName}>
                        {item.quotaName}
                      </span>
                      <span className="text-[9px] uppercase font-mono px-1.5 py-0.2 rounded bg-slate-800 text-slate-400">
                        {item.serviceCode}
                      </span>
                    </div>
                    <div>{getStatusBadge(item.status)}</div>
                  </div>

                  {/* Progress Bar */}
                  <div className="w-full bg-slate-950 h-2 rounded-full overflow-hidden border border-slate-800/80">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${getProgressBarColor(item.status)}`}
                      style={{ width: `${hasUsage ? pct : 0}%` }}
                    />
                  </div>

                  {/* Metrics Footer */}
                  <div className="flex items-center justify-between text-[10px] text-slate-400 font-mono">
                    <span>
                      {hasUsage
                        ? `${item.currentUsage} / ${item.appliedLimit} ${item.unit || ''}`
                        : `Limit: ${item.appliedLimit || 'N/A'} (${item.usageSource})`}
                    </span>
                    <span className={`font-bold ${item.status === 'CRITICAL' ? 'text-rose-400' : item.status === 'WARNING' ? 'text-amber-400' : item.status === 'NORMAL' ? 'text-emerald-400' : 'text-slate-400'}`}>
                      {hasUsage ? `${item.utilizationPercentage?.toFixed(1)}%` : 'N/A'}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
