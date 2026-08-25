import React, { useEffect, useState } from 'react';
import { cloudOpsApi } from '../api';
import { CloudTrailEventResult } from '../types/api';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorBanner } from '../components/feedback/ErrorBanner';
import { Search } from 'lucide-react';

export const CloudTrailPage: React.FC = () => {
  const [result, setResult] = useState<CloudTrailEventResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');

  useEffect(() => {
    cloudOpsApi
      .getCloudTrailEvents()
      .then((data) => {
        setResult(data);
        setLoading(false);
      })
      .catch((err: Error) => {
        setError(err.message);
        setLoading(false);
      });
  }, []);

  const events = result?.events || [];
  const filteredEvents = events.filter(
    (e) =>
      e.eventName.toLowerCase().includes(search.toLowerCase()) ||
      (e.username && e.username.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold tracking-tight text-slate-100">CloudTrail Operational Audit History</h2>
        <p className="text-xs text-slate-400 mt-1">
          Immutable operational events, administrative actions, and user activity auditing.
        </p>
      </div>

      {loading && <LoadingSpinner message="Querying CloudTrail event history..." />}
      {error && <ErrorBanner message={error} errorCode="CLOUDTRAIL_STATUS" />}

      {!loading && !error && (
        <div className="space-y-4">
          <div className="relative w-full sm:w-80">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
            <input
              type="text"
              placeholder="Search event or username..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-4 py-2 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-200 focus:outline-none focus:border-amber-500"
            />
          </div>

          <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden">
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-slate-950/80 text-slate-400 border-b border-slate-800 uppercase font-sans font-semibold">
                <tr>
                  <th className="px-4 py-3">Event Name</th>
                  <th className="px-4 py-3">Username</th>
                  <th className="px-4 py-3">Source</th>
                  <th className="px-4 py-3">Region</th>
                  <th className="px-4 py-3">Event Time</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredEvents.map((evt) => (
                  <tr key={evt.eventId} className="hover:bg-slate-800/30">
                    <td className="px-4 py-3 text-amber-400 font-semibold">{evt.eventName}</td>
                    <td className="px-4 py-3 text-slate-300">{evt.username || 'AWS Service'}</td>
                    <td className="px-4 py-3 text-slate-500">{evt.eventSource}</td>
                    <td className="px-4 py-3 text-slate-400">{evt.awsRegion}</td>
                    <td className="px-4 py-3 text-slate-400">{new Date(evt.eventTime).toLocaleString()}</td>
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