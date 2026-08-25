import React from 'react';
import { FolderSearch } from 'lucide-react';

interface EmptyStateProps {
  title?: string;
  description?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title = 'No Evidence Found',
  description = 'No active cloud resources or findings observed in the selected scope.',
}) => (
  <div className="rounded-lg border border-slate-800 bg-slate-900/50 p-8 text-center text-slate-400">
    <FolderSearch className="w-10 h-10 mx-auto text-slate-600 mb-3" />
    <h4 className="font-medium text-slate-200">{title}</h4>
    <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">{description}</p>
  </div>
);