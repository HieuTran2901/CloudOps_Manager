import React, { useState } from 'react';
import { cloudOpsApi } from '../api';
import { DriftReport } from '../types/api';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorBanner } from '../components/feedback/ErrorBanner';
import { StatusBadge } from '../components/ui/StatusBadge';
import { Play } from 'lucide-react';

export const DriftPage: React.FC = () => {
  const [terraformJson, setTerraformJson] = useState('{\n  "version": 4,\n  "terraform_version": "1.5.0",\n  "resources": []\n}');
  const [report, setReport] = useState<DriftReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleEvaluate = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    cloudOpsApi
      .evaluateDrift(terraformJson)
      .then((data) => {
        setReport(data);
        setLoading(false);
      })
      .catch((err: Error) => {
        setError(err.message);
        setLoading(false);
      });
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold tracking-tight text-slate-100">Terraform Read-Only IaC Drift Detection</h2>
        <p className="text-xs text-slate-400 mt-1">
          Pure parser-based drift comparison between desired Terraform state and live normalized AWS evidence.
        </p>
      </div>

      <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 space-y-3">
        <label className="text-xs font-semibold text-slate-300 block">Terraform State JSON v4</label>
        <textarea
          rows={5}
          value={terraformJson}
          onChange={(e) => setTerraformJson(e.target.value)}
          className="w-full p-3 rounded-lg bg-slate-950 border border-slate-800 text-xs font-mono text-slate-300 focus:outline-none focus:border-purple-500"
        ></textarea>
        <div className="flex justify-end">
          <button
            onClick={handleEvaluate}
            disabled={loading}
            className="px-4 py-2 rounded bg-purple-600 hover:bg-purple-500 text-white text-xs font-medium flex items-center space-x-2"
          >
            <Play className="w-3.5 h-3.5" />
            <span>{loading ? 'Evaluating Drift...' : 'Run Drift Evaluation'}</span>
          </button>
        </div>
      </div>

      {loading && <LoadingSpinner message="Parsing state and comparing live evidence..." />}
      {error && <ErrorBanner message={error} errorCode="DRIFT_PARSER_STATUS" />}

      {report && (
        <div className="space-y-4">
          <div className="grid grid-cols-3 gap-4">
            <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/50">
              <span className="text-xs text-slate-500 uppercase">Drift Status</span>
              <div className="mt-1">
                <StatusBadge status={report.status} />
              </div>
            </div>
            <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/50">
              <span className="text-xs text-slate-500 uppercase">Total Resources</span>
              <p className="text-2xl font-bold text-slate-200 mt-1">{report.totalResources}</p>
            </div>
            <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/50">
              <span className="text-xs text-slate-500 uppercase">Drifted Resources</span>
              <p className="text-2xl font-bold text-rose-400 mt-1">{report.driftedResources}</p>
            </div>
          </div>

          {report.resources && report.resources.length > 0 && (
            <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden">
              <table className="w-full text-left text-xs font-mono">
                <thead className="bg-slate-950/80 text-slate-400 border-b border-slate-800 uppercase font-sans font-semibold">
                  <tr>
                    <th className="px-4 py-3">Resource Address</th>
                    <th className="px-4 py-3">Type</th>
                    <th className="px-4 py-3">Resource ID</th>
                    <th className="px-4 py-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {report.resources.map((r, idx) => (
                    <tr key={idx} className="hover:bg-slate-800/30">
                      <td className="px-4 py-3 text-purple-400">{r.resourceAddress}</td>
                      <td className="px-4 py-3 text-slate-400">{r.resourceType}</td>
                      <td className="px-4 py-3 text-slate-200">{r.resourceId}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={r.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
};