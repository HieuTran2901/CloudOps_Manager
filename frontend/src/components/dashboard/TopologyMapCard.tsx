import React, { useState } from 'react';
import { Maximize2 } from 'lucide-react';
import { INITIAL_3D_NODES, INITIAL_3D_LINKS, project3DPoint } from './topology3dData';

export const TopologyMapCard: React.FC = () => {
  const [isInteractive, setIsInteractive] = useState(true);
  const [viewMode, setViewMode] = useState<'3D View' | '2D View'>('3D View');
  const [rotation, setRotation] = useState({ rotX: 20, rotY: -15 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [selectedNode, setSelectedNode] = useState<string | null>(null);

  const nodes = INITIAL_3D_NODES;
  const links = INITIAL_3D_LINKS;

  const handleMouseDown = (e: React.MouseEvent) => {
    if (!isInteractive || viewMode === '2D View') return;
    setIsDragging(true);
    setDragStart({ x: e.clientX, y: e.clientY });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return;
    const deltaX = e.clientX - dragStart.x;
    const deltaY = e.clientY - dragStart.y;
    setRotation((prev) => ({
      rotX: Math.max(-45, Math.min(65, prev.rotX - deltaY * 0.4)),
      rotY: prev.rotY + deltaX * 0.4,
    }));
    setDragStart({ x: e.clientX, y: e.clientY });
  };

  const handleMouseUp = () => setIsDragging(false);

  const projectedNodes = nodes
    .map((n) => ({ ...n, ...project3DPoint(n.x, n.y, n.z, rotation.rotX, rotation.rotY, viewMode) }))
    .sort((a, b) => a.depth - b.depth);

  const nodeMap = new Map(projectedNodes.map((n) => [n.id, n]));

  return (
    <div className="rounded-2xl border border-slate-800/80 bg-gradient-to-b from-[#0e1628] to-[#0a0f1d] p-5 shadow-lg flex flex-col justify-between select-none">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-2.5">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-200">
            TOPOLOGY MAP
          </span>
          <span className="px-2 py-0.5 rounded bg-sky-950 text-sky-400 border border-sky-800 text-[10px] font-mono font-semibold">
            {nodes.length} Nodes • {links.length} Edges
          </span>
        </div>

        <div className="flex items-center space-x-3 text-xs">
          <div className="flex items-center space-x-2">
            <span className="text-slate-400 text-xs">Interactive</span>
            <button
              onClick={() => setIsInteractive(!isInteractive)}
              className={`w-9 h-5 rounded-full p-0.5 transition-colors ${
                isInteractive ? 'bg-sky-600 shadow-[0_0_10px_rgba(2,132,199,0.5)]' : 'bg-slate-800'
              }`}
              title="Toggle drag to rotate in 3D"
            >
              <div className={`w-4 h-4 rounded-full bg-white transition-transform ${isInteractive ? 'translate-x-4' : 'translate-x-0'}`} />
            </button>
          </div>

          <select
            value={viewMode}
            onChange={(e) => setViewMode(e.target.value as '3D View' | '2D View')}
            className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-200 font-medium focus:outline-none cursor-pointer"
          >
            <option value="3D View">3D View</option>
            <option value="2D View">2D View</option>
          </select>

          <button
            onClick={() => setRotation({ rotX: 20, rotY: -15 })}
            className="p-1.5 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
            title="Reset 3D camera angle"
          >
            <Maximize2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        className={`my-3 relative w-full h-72 sm:h-80 rounded-2xl bg-gradient-to-b from-[#040711] via-[#060c1c] to-[#03050c] border border-slate-800/90 overflow-hidden shadow-[inset_0_0_30px_rgba(0,0,0,0.8)] flex items-center justify-center ${
          isInteractive && viewMode === '3D View' ? 'cursor-grab active:cursor-grabbing' : ''
        }`}
      >
        <div
          className="absolute inset-0 opacity-30 pointer-events-none transition-transform duration-300"
          style={{
            backgroundImage: `radial-gradient(circle at 50% 50%, rgba(56, 189, 248, 0.2) 0%, transparent 70%),
              linear-gradient(to right, rgba(51, 65, 85, 0.4) 1px, transparent 1px),
              linear-gradient(to bottom, rgba(51, 65, 85, 0.4) 1px, transparent 1px)`,
            backgroundSize: '100% 100%, 36px 36px, 36px 36px',
            transform: `perspective(600px) rotateX(${Math.max(15, rotation.rotX * 0.7)}deg) rotateY(${rotation.rotY * 0.5}deg) translateY(55px)`,
          }}
        />

        <div className="absolute w-96 h-48 bg-sky-500/15 rounded-full blur-3xl pointer-events-none" />

        <svg viewBox="0 0 820 270" className="w-full h-full relative z-10">
          <defs>
            <linearGradient id="edgeGlow" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.9" />
              <stop offset="50%" stopColor="#818cf8" stopOpacity="0.6" />
              <stop offset="100%" stopColor="#ec4899" stopOpacity="0.9" />
            </linearGradient>
            <filter id="neonGlow" x="-30%" y="-30%" width="160%" height="160%">
              <feGaussianBlur stdDeviation="3.5" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>

          {links.map((link, idx) => {
            const src = nodeMap.get(link.from);
            const dst = nodeMap.get(link.to);
            if (!src || !dst) return null;

            return (
              <g key={`edge-${idx}`}>
                <line x1={src.screenX} y1={src.screenY} x2={dst.screenX} y2={dst.screenY} stroke="#020617" strokeWidth={4} />
                <line x1={src.screenX} y1={src.screenY} x2={dst.screenX} y2={dst.screenY} stroke="url(#edgeGlow)" strokeWidth={2} strokeDasharray="5 3" opacity={0.8} />
                <circle r={3.5} fill="#38bdf8" opacity={0.95} filter="url(#neonGlow)">
                  <animateMotion path={`M ${src.screenX} ${src.screenY} L ${dst.screenX} ${dst.screenY}`} dur={`${2.2 + (idx % 3) * 0.7}s`} repeatCount="indefinite" />
                </circle>
              </g>
            );
          })}

          {projectedNodes.map((n) => {
            const isSelected = selectedNode === n.id;
            const size = Math.round(36 * n.scale);
            const Icon = n.icon;

            return (
              <g key={n.id} transform={`translate(${n.screenX}, ${n.screenY})`} onClick={() => setSelectedNode(isSelected ? null : n.id)} className="cursor-pointer group">
                {isSelected && <circle r={size * 1.35} fill="none" stroke={n.color} strokeWidth={2.5} className="animate-ping opacity-60" />}
                <rect x={-size / 2} y={-size / 2} width={size} height={size} rx={size * 0.28} fill="#0b1122" stroke={n.color} strokeWidth={isSelected ? 3 : 2} style={{ filter: `drop-shadow(0 0 10px ${n.glow})` }} className="transition-transform duration-200 group-hover:scale-115" />
                <foreignObject x={-size / 2 + 4} y={-size / 2 + 4} width={size - 8} height={size - 8} className="pointer-events-none">
                  <div className="w-full h-full flex items-center justify-center text-white" style={{ color: n.color }}>
                    <Icon className="w-full h-full p-0.5" />
                  </div>
                </foreignObject>
                <text y={size / 2 + 13 * n.scale} textAnchor="middle" fill={isSelected ? '#38bdf8' : '#e2e8f0'} fontSize={Math.round(9.5 * n.scale)} fontFamily="monospace" fontWeight="bold" className="select-none group-hover:fill-sky-300 transition-colors">
                  {n.id}
                </text>
                <text y={size / 2 + 22 * n.scale} textAnchor="middle" fill="#94a3b8" fontSize={Math.round(8 * n.scale)} fontFamily="sans-serif" fontWeight="500" className="select-none">
                  {n.type}
                </text>
              </g>
            );
          })}
        </svg>

        <div className="absolute bottom-3 left-4 px-3 py-1 rounded-xl bg-slate-950/90 border border-slate-800 text-[10px] text-slate-300 font-mono shadow-md pointer-events-none flex items-center space-x-2">
          <span className="w-1.5 h-1.5 rounded-full bg-sky-400 animate-pulse"></span>
          <span>Click & Drag to rotate 3D • Click node to inspect</span>
        </div>
      </div>
    </div>
  );
};