import React from 'react';
import { AlertTriangle } from 'lucide-react';

interface ErrorBannerProps {
  title?: string;
  message: string;
  errorCode?: string;
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({
  title = 'Operation Error',
  message,
  errorCode,
}) => (
  <div className="rounded-lg border border-red-500/30 bg-red-950/40 p-4 text-red-200">
    <div className="flex items-start space-x-3">
      <AlertTriangle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
      <div>
        <h4 className="font-semibold text-sm text-red-300">
          {title} {errorCode && <span className="text-xs font-mono bg-red-900/60 px-1.5 py-0.5 rounded ml-2">{errorCode}</span>}
        </h4>
        <p className="text-xs text-red-200/80 mt-1">{message}</p>
      </div>
    </div>
  </div>
);