import React from 'react';
import { TelemetrySeries } from '../../types/api';

interface TelemetryChartProps {
  series: TelemetrySeries;
}

export const TelemetryChart: React.FC<TelemetryChartProps> = ({ series }) => {
  const points = series.datapoints || [];

  if (points.length === 0) {
    return (
      <div className="h-44 rounded-lg border border-slate-800 bg-slate-950 flex items-center justify-center text-xs text-slate-500">
        No metric datapoints observed in time window.
      </div>
    );
  }

  const values = points.map((p) => p.value);
  const maxVal = Math.max(...values, 1);
  const minVal = Math.min(...values, 0);
  const range = maxVal - minVal || 1;

  const svgWidth = 500;
  const svgHeight = 140;

  const polylinePoints = points
    .map((p, idx) => {
      const x = (idx / Math.max(points.length - 1, 1)) * (svgWidth - 40) + 20;
      const y = svgHeight - 20 - ((p.value - minVal) / range) * (svgHeight - 40);
      return `${x},${y}`;
    })
    .join(' ');

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 space-y-2">
      <div className="flex items-center justify-between text-xs">
        <div>
          <span className="font-semibold text-slate-200">{series.metricName}</span>
          <span className="text-slate-500 ml-2 font-mono">({series.resourceId})</span>
        </div>
        <span className="font-mono text-sky-400 bg-sky-950/60 border border-sky-800/80 px-2 py-0.5 rounded text-[11px]">
          Max: {maxVal.toFixed(2)} {series.unit || ''}
        </span>
      </div>

      <div className="relative h-36 bg-slate-950 rounded-lg border border-slate-800/80 p-2 overflow-hidden">
        <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} className="w-full h-full">
          {/* Grid lines */}
          <line x1="20" y1="20" x2={svgWidth - 20} y2="20" stroke="#1e293b" strokeWidth="1" strokeDasharray="3 3" />
          <line x1="20" y1={svgHeight / 2} x2={svgWidth - 20} y2={svgHeight / 2} stroke="#1e293b" strokeWidth="1" strokeDasharray="3 3" />
          <line x1="20" y1={svgHeight - 20} x2={svgWidth - 20} y2={svgHeight - 20} stroke="#1e293b" strokeWidth="1" />

          {/* Metric line */}
          <polyline fill="none" stroke="#38bdf8" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" points={polylinePoints} />

          {/* Dots */}
          {points.map((p, idx) => {
            const x = (idx / Math.max(points.length - 1, 1)) * (svgWidth - 40) + 20;
            const y = svgHeight - 20 - ((p.value - minVal) / range) * (svgHeight - 40);
            return <circle key={idx} cx={x} cy={y} r="3" fill="#38bdf8" className="hover:r-5 transition-all" />;
          })}
        </svg>
      </div>
    </div>
  );
};