import React, { useEffect, useState } from 'react';
import { cloudOpsApi } from '../api';
import { ComplianceReport } from '../types/api';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorBanner } from '../components/feedback/ErrorBanner';
import { StatusBadge } from '../components/ui/StatusBadge';
import { Search, Filter } from 'lucide-react';

export const CompliancePage: React.FC = () => {
  const [report, setReport] = useState<ComplianceReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pillarFilter, setPillarFilter] = useState('ALL');
  const [search, setSearch] = useState('');

  useEffect(() => {
    cloudOpsApi
      .getComplianceReport()
      .then((data) => {
        setReport(data);
        setLoading(false);
      })
      .catch((err: Error) => {
        setError(err.message);
        setLoading(false);
      });
  }, []);

  const results = report?.results || [];
  const filteredResults = results.filter((r) => {
    const matchesPillar = pillarFilter === 'ALL' || r.category === pillarFilter;
    const matchesSearch =
      r.ruleId.toLowerCase().includes(search.toLowerCase()) ||
      r.title.toLowerCase().includes(search.toLowerCase());
    return matchesPillar && matchesSearch;
  });

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold tracking-tight text-slate-100">AWS Well-Architected & Compliance Rules</h2>
        <p className="text-xs text-slate-400 mt-1">
          Deterministic evaluation engine across Security, Reliability, Cost, and Performance pillars.
        </p>
      </div>

      {loading && <LoadingSpinner message="Evaluating compliance rules against live AWS evidence..." />}
      {error && <ErrorBanner message={error} />}

      {!loading && !error && report && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/50">
              <span className="text-xs text-slate-500 uppercase">Passed</span>
              <p className="text-2xl font-bold text-emerald-400 mt-1">{report.passCount}</p>
            </div>
            <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/50">
              <span className="text-xs text-slate-500 uppercase">Failed</span>
              <p className="text-2xl font-bold text-rose-400 mt-1">{report.failCount}</p>
            </div>
            <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/50">
              <span className="text-xs text-slate-500 uppercase">Insufficient Evidence</span>
              <p className="text-2xl font-bold text-amber-400 mt-1">{report.insufficientEvidenceCount}</p>
            </div>
            <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/50">
              <span className="text-xs text-slate-500 uppercase">Total Evaluated</span>
              <p className="text-2xl font-bold text-slate-100 mt-1">{report.totalRulesEvaluated}</p>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-3 items-center justify-between">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="Search rules..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full pl-9 pr-4 py-2 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-200 focus:outline-none focus:border-sky-500"
              />
            </div>

            <div className="flex items-center space-x-2">
              <Filter className="w-4 h-4 text-slate-500" />
              <select
                value={pillarFilter}
                onChange={(e) => setPillarFilter(e.target.value)}
                className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none cursor-pointer"
              >
                <option value="ALL">All Categories</option>
                <option value="SECURITY">Security</option>
                <option value="RELIABILITY">Reliability</option>
                <option value="COST_OPTIMIZATION">Cost Optimization</option>
                <option value="PERFORMANCE_EFFICIENCY">Performance Efficiency</option>
              </select>
            </div>
          </div>

          <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950/80 text-slate-400 border-b border-slate-800 uppercase tracking-wider font-semibold">
                <tr>
                  <th className="px-4 py-3">Rule ID</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3">Title</th>
                  <th className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredResults.map((res) => (
                  <tr key={res.ruleId} className="hover:bg-slate-800/30">
                    <td className="px-4 py-3 font-mono text-sky-400">{res.ruleId}</td>
                    <td className="px-4 py-3 text-slate-400">{res.category}</td>
                    <td className="px-4 py-3 text-slate-200">
                      <div>
                        <span>{res.title}</span>
                        <p className="text-[11px] text-slate-500 mt-0.5">{res.explanation}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={res.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};