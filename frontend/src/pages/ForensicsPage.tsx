import React from 'react';
import { Download, FileJson, FileSpreadsheet, ShieldCheck } from 'lucide-react';
import { cloudOpsApi } from '../api';

export const ForensicsPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold tracking-tight text-slate-100">Forensic Audit & Evidence Export</h2>
        <p className="text-xs text-slate-400 mt-1">
          Export tamper-evident cryptographic evidence bundles with SHA-256 integrity verification.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="p-6 rounded-xl border border-slate-800 bg-slate-900/50 flex flex-col justify-between space-y-4">
          <div>
            <FileJson className="w-8 h-8 text-sky-400 mb-2" />
            <h3 className="font-semibold text-slate-200">Canonical JSON Forensic Bundle</h3>
            <p className="text-xs text-slate-400 mt-1">
              Consolidated, reproducible JSON artifact encompassing live resource descriptors, compliance evaluations, directed topology relationships, and cryptographic integrity digests.
            </p>
          </div>
          <a
            href={cloudOpsApi.downloadForensicExportUrl('json')}
            download
            className="inline-flex items-center justify-center space-x-2 px-4 py-2.5 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold transition-colors"
          >
            <Download className="w-4 h-4" />
            <span>Download JSON Evidence Bundle</span>
          </a>
        </div>

        <div className="p-6 rounded-xl border border-slate-800 bg-slate-900/50 flex flex-col justify-between space-y-4">
          <div>
            <FileSpreadsheet className="w-8 h-8 text-emerald-400 mb-2" />
            <h3 className="font-semibold text-slate-200">Tabular CSV Forensic Export</h3>
            <p className="text-xs text-slate-400 mt-1">
              Multi-section tabular CSV export suitable for external audit compliance reviews, spreadsheets, and offline digital evidence preservation.
            </p>
          </div>
          <a
            href={cloudOpsApi.downloadForensicExportUrl('csv')}
            download
            className="inline-flex items-center justify-center space-x-2 px-4 py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold transition-colors"
          >
            <Download className="w-4 h-4" />
            <span>Download CSV Evidence Export</span>
          </a>
        </div>
      </div>

      <div className="p-5 rounded-xl border border-slate-800 bg-slate-900/40 space-y-3">
        <div className="flex items-center space-x-2 text-xs font-semibold text-slate-300">
          <ShieldCheck className="w-4 h-4 text-sky-400" />
          <span>Cryptographic Integrity Verification</span>
        </div>
        <p className="text-xs text-slate-400 leading-relaxed">
          Every forensic artifact is hashed at generation using standard <strong className="text-slate-200">SHA-256</strong>. The hexadecimal hash is included directly inside the payload metadata and transmitted via the <code className="text-sky-300 bg-slate-950 px-1.5 py-0.5 rounded border border-slate-800">X-Forensic-SHA256-Digest</code> HTTP response header to detect any tampering or transport corruption.
        </p>
      </div>
    </div>
  );
};