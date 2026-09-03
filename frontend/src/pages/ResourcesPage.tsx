import React, { useEffect, useState } from 'react';
import { cloudOpsApi } from '../api';
import { InventorySummary, CloudResource } from '../types/api';
import { useRegion } from '../context/RegionContext';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorBanner } from '../components/feedback/ErrorBanner';
import { EmptyState } from '../components/feedback/EmptyState';
import { StatusBadge } from '../components/ui/StatusBadge';
import { ResourceDetailDrawer } from '../components/drawer/ResourceDetailDrawer';
import { Search, Filter } from 'lucide-react';

export const ResourcesPage: React.FC = () => {
  const { currentRegion } = useRegion();
  const [summary, setSummary] = useState<InventorySummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedResource, setSelectedResource] = useState<CloudResource | null>(null);
  const [filterType, setFilterType] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  useEffect(() => {
    setLoading(true);
    setError(null);
    cloudOpsApi
      .discoverResources(currentRegion)
      .then((data) => {
        setSummary(data);
        setLoading(false);
      })
      .catch((err: Error) => {
        setError(err.message);
        setLoading(false);
      });
  }, [currentRegion]);

  const resources = summary?.resources || [];
  const resourceTypes = ['ALL', ...Array.from(new Set(resources.map((r) => r.resourceType)))];

  const filteredResources = resources.filter((r) => {
    const matchesType = filterType === 'ALL' || r.resourceType === filterType;
    const matchesSearch =
      r.resourceId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (r.name && r.name.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesType && matchesSearch;
  });

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold tracking-tight text-slate-100">AWS Resource Discovery</h2>
        <p className="text-xs text-slate-400 mt-1">
          Normalized live inventory covering EC2, S3, RDS, VPC, Subnets, Security Groups, and IAM Roles.
        </p>
      </div>

      {loading && <LoadingSpinner message="Querying live AWS inventory..." />}
      {error && <ErrorBanner message={error} />}

      {!loading && !error && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row gap-3 items-center justify-between">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="Search by ID or name..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-4 py-2 bg-slate-900 border border-slate-800 rounded-lg text-xs text-slate-200 focus:outline-none focus:border-sky-500"
              />
            </div>

            <div className="flex items-center space-x-2 w-full sm:w-auto">
              <Filter className="w-4 h-4 text-slate-500" />
              <select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none cursor-pointer"
              >
                {resourceTypes.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {filteredResources.length === 0 ? (
            <EmptyState title="No Matching Resources" description="Try adjusting your search query or type filter." />
          ) : (
            <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden shadow-sm">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-950/80 text-slate-400 border-b border-slate-800 uppercase tracking-wider font-semibold">
                  <tr>
                    <th className="px-4 py-3">Resource ID</th>
                    <th className="px-4 py-3">Type</th>
                    <th className="px-4 py-3">Name</th>
                    <th className="px-4 py-3">Region</th>
                    <th className="px-4 py-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60 font-mono">
                  {filteredResources.map((r) => (
                    <tr
                      key={r.resourceId}
                      onClick={() => setSelectedResource(r)}
                      className="hover:bg-slate-800/40 cursor-pointer transition-colors"
                    >
                      <td className="px-4 py-3 font-semibold text-slate-200 hover:text-sky-400">{r.resourceId}</td>
                      <td className="px-4 py-3 text-sky-400">{r.resourceType}</td>
                      <td className="px-4 py-3 text-slate-400">{r.name || '—'}</td>
                      <td className="px-4 py-3 text-slate-400">{r.region}</td>
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

      <ResourceDetailDrawer resource={selectedResource} onClose={() => setSelectedResource(null)} />
    </div>
  );
};