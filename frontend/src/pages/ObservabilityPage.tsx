import React, { useState } from 'react';
import { cloudOpsApi } from '../api';
import { TelemetryAggregationResult } from '../types/api';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorBanner } from '../components/feedback/ErrorBanner';
import { TelemetryChart } from '../components/charts/TelemetryChart';
import { Activity, Play } from 'lucide-react';

export const ObservabilityPage: React.FC = () => {
  const [resourceType, setResourceType] = useState('AWS::EC2::Instance');
  const [resourceId, setResourceId] = useState('i-0123456789abcdef0');
  const [metricName, setMetricName] = useState('CPUUtilization');
  const [result, setResult] = useState<TelemetryAggregationResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleQuery = (e: React.FormEvent) => {
    e.preventDefault();
    if (!resourceId) return;
    setLoading(true);
    setError(null);

    cloudOpsApi
      .getTelemetryMetrics(resourceType, [resourceId], [metricName])
      .then((data) => {
        setResult(data);
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
        <h2 className="text-xl font-bold tracking-tight text-slate-100">CloudWatch Observability & Telemetry</h2>
        <p className="text-xs text-slate-400 mt-1">
          Multi-resource metric batching, time-series aggregation, and period rollups.
        </p>
      </div>

      <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">
        <form onSubmit={handleQuery} className="grid grid-cols-1 sm:grid-cols-4 gap-3 text-xs">
          <div>
            <label className="text-slate-400 block mb-1">Resource Type</label>
            <select
              value={resourceType}
              onChange={(e) => setResourceType(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded px-3 py-2 text-slate-200"
            >
              <option value="AWS::EC2::Instance">AWS::EC2::Instance</option>
              <option value="AWS::RDS::DBInstance">AWS::RDS::DBInstance</option>
            </select>
          </div>
          <div>
            <label className="text-slate-400 block mb-1">Resource ID</label>
            <input
              type="text"
              value={resourceId}
              onChange={(e) => setResourceId(e.target.value)}
              placeholder="e.g. i-123456"
              className="w-full bg-slate-950 border border-slate-800 rounded px-3 py-2 text-slate-200 font-mono"
            />
          </div>
          <div>
            <label className="text-slate-400 block mb-1">Metric Name</label>
            <select
              value={metricName}
              onChange={(e) => setMetricName(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded px-3 py-2 text-slate-200 font-mono"
            >
              <option value="CPUUtilization">CPUUtilization</option>
              <option value="NetworkIn">NetworkIn</option>
              <option value="NetworkOut">NetworkOut</option>
              <option value="DatabaseConnections">DatabaseConnections</option>
            </select>
          </div>
          <div className="flex items-end">
            <button
              type="submit"
              disabled={loading}
              className="w-full px-4 py-2 rounded bg-sky-600 hover:bg-sky-500 text-white font-medium flex items-center justify-center space-x-2"
            >
              <Play className="w-3.5 h-3.5" />
              <span>{loading ? 'Querying...' : 'Run Telemetry Query'}</span>
            </button>
          </div>
        </form>
      </div>

      {loading && <LoadingSpinner message="Fetching CloudWatch metric telemetry..." />}
      {error && <ErrorBanner message={error} errorCode="CLOUDWATCH_STATUS" />}

      {result && result.series.length > 0 && (
        <div className="grid grid-cols-1 gap-4">
          {result.series.map((s, idx) => (
            <TelemetryChart key={idx} series={s} />
          ))}
        </div>
      )}

      {!result && !loading && !error && (
        <div className="rounded-xl border border-slate-800 bg-slate-900/30 p-8 text-center text-slate-500 text-xs">
          <Activity className="w-8 h-8 mx-auto text-slate-600 mb-2" />
          Select a resource and metric name above to query CloudWatch telemetry series.
        </div>
      )}
    </div>
  );
};