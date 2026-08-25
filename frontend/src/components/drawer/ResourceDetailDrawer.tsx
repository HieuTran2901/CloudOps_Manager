import React from 'react';
import { X, Server, ShieldCheck, Tag } from 'lucide-react';
import { CloudResource } from '../../types/api';
import { StatusBadge } from '../ui/StatusBadge';

interface ResourceDetailDrawerProps {
  resource: CloudResource | null;
  onClose: () => void;
}

export const ResourceDetailDrawer: React.FC<ResourceDetailDrawerProps> = ({ resource, onClose }) => {
  if (!resource) return null;

  return (
    <div className="fixed inset-y-0 right-0 w-full max-w-md bg-slate-900 border-l border-slate-800 shadow-2xl z-50 flex flex-col">
      <div className="p-5 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
        <div className="flex items-center space-x-3">
          <div className="p-2 rounded bg-sky-950 border border-sky-800 text-sky-400">
            <Server className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-bold text-sm text-slate-100">{resource.resourceId}</h3>
            <p className="text-xs font-mono text-sky-400">{resource.resourceType}</p>
          </div>
        </div>
        <button
          onClick={onClose}
          className="p-1.5 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-5 space-y-5 text-xs">
        <div className="grid grid-cols-2 gap-3 p-3 rounded-lg bg-slate-950 border border-slate-800/80">
          <div>
            <span className="text-slate-500 block">Status</span>
            <div className="mt-1">
              <StatusBadge status={resource.status} />
            </div>
          </div>
          <div>
            <span className="text-slate-500 block">Region</span>
            <span className="font-mono text-slate-200 mt-1 block">{resource.region}</span>
          </div>
          <div>
            <span className="text-slate-500 block">Account ID</span>
            <span className="font-mono text-slate-200 mt-1 block">{resource.accountId}</span>
          </div>
          <div>
            <span className="text-slate-500 block">Discovered</span>
            <span className="font-mono text-slate-400 mt-1 block truncate">
              {new Date(resource.discoveredAt).toLocaleTimeString()}
            </span>
          </div>
        </div>

        {resource.name && (
          <div>
            <span className="text-slate-400 font-semibold block mb-1">Resource Name</span>
            <p className="p-2.5 rounded bg-slate-950 border border-slate-800 text-slate-200 font-mono">
              {resource.name}
            </p>
          </div>
        )}

        {resource.arn && (
          <div>
            <span className="text-slate-400 font-semibold block mb-1">Amazon Resource Name (ARN)</span>
            <p className="p-2.5 rounded bg-slate-950 border border-slate-800 text-slate-400 font-mono text-[11px] break-all select-all">
              {resource.arn}
            </p>
          </div>
        )}

        {resource.tags && Object.keys(resource.tags).length > 0 && (
          <div>
            <span className="text-slate-400 font-semibold flex items-center space-x-1.5 mb-2">
              <Tag className="w-3.5 h-3.5 text-sky-400" />
              <span>Observed AWS Tags</span>
            </span>
            <div className="p-2.5 rounded bg-slate-950 border border-slate-800 space-y-1 font-mono">
              {Object.entries(resource.tags).map(([k, v]) => (
                <div key={k} className="flex justify-between py-0.5 border-b border-slate-900 last:border-0">
                  <span className="text-slate-500">{k}:</span>
                  <span className="text-slate-300 truncate max-w-[200px]">{v}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="p-3 rounded-lg border border-slate-800 bg-slate-950/40 text-slate-400">
          <div className="flex items-center space-x-2 text-slate-300 font-medium">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>Factual Evidence Provenance</span>
          </div>
          <p className="mt-1 text-[11px] text-slate-500">
            Discovered directly from live AWS API response headers and normalized descriptor payloads.
          </p>
        </div>
      </div>
    </div>
  );
};