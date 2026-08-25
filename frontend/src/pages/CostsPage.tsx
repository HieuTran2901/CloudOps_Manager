import React, { useEffect, useState } from 'react';
import { cloudOpsApi } from '../api';
import { CostAggregationResult } from '../types/api';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorBanner } from '../components/feedback/ErrorBanner';
import { PieChart } from 'lucide-react';

export const CostsPage: React.FC = () => {
  const [costs, setCosts] = useState<CostAggregationResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [granularity, setGranularity] = useState('MONTHLY');

  useEffect(() => {
    setLoading(true);
    cloudOpsApi
      .getCosts(granularity)
      .then((data) => {
        setCosts(data);
        setLoading(false);
      })
      .catch((err: Error) => {
        setError(err.message);
        setLoading(false);
      });
  }, [granularity]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-slate-100">AWS Cost Explorer & Financial Observability</h2>
          <p className="text-xs text-slate-400 mt-1">
            Exact BigDecimal arithmetic, daily/monthly rollups, and service breakdown analysis.
          </p>
        </div>
        <select
          value={granularity}
          onChange={(e) => setGranularity(e.target.value)}
          className="bg-slate-900 border border-slate-800 rounded px-3 py-1.5 text-xs text-slate-200 font-mono"
        >
          <option value="DAILY">Daily Granularity</option>
          <option value="MONTHLY">Monthly Granularity</option>
        </select>
      </div>

      {loading && <LoadingSpinner message="Querying AWS Cost Explorer data..." />}
      {error && <ErrorBanner message={error} errorCode="COST_EXPLORER_STATUS" />}

      {!loading && !error && costs && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-5 rounded-xl border border-slate-800 bg-slate-900/60">
              <span className="text-xs text-slate-500 uppercase">Total Unblended Cost</span>
              <p className="text-3xl font-bold text-emerald-400 mt-1">
                ${costs.totalAmount.toFixed(2)} <span className="text-xs text-slate-500 font-normal">{costs.currency}</span>
              </p>
            </div>
            <div className="p-5 rounded-xl border border-slate-800 bg-slate-900/60">
              <span className="text-xs text-slate-500 uppercase">Metric Granularity</span>
              <p className="text-xl font-bold text-slate-200 mt-1 font-mono">{costs.granularity}</p>
            </div>
            <div className="p-5 rounded-xl border border-slate-800 bg-slate-900/60">
              <span className="text-xs text-slate-500 uppercase">Arithmetic Precision</span>
              <p className="text-xl font-bold text-sky-400 mt-1">Exact BigDecimal</p>
            </div>
          </div>

          {costs.groups && costs.groups.length > 0 && (
            <div className="p-5 rounded-xl border border-slate-800 bg-slate-900/60 space-y-4">
              <h3 className="text-xs font-semibold uppercase text-slate-300 flex items-center space-x-2">
                <PieChart className="w-4 h-4 text-emerald-400" />
                <span>Service Cost Breakdown</span>
              </h3>
              <div className="space-y-3 font-mono text-xs">
                {costs.groups.map((g) => {
                  const pct = costs.totalAmount > 0 ? (g.amount / costs.totalAmount) * 100 : 0;
                  return (
                    <div key={g.groupKey} className="space-y-1">
                      <div className="flex justify-between text-slate-300">
                        <span>{g.groupKey}</span>
                        <span>${g.amount.toFixed(2)} ({pct.toFixed(1)}%)</span>
                      </div>
                      <div className="w-full h-1.5 rounded-full bg-slate-950 overflow-hidden">
                        <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${pct}%` }}></div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};