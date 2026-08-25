import React from 'react';
import { Server, Database, Box, Network, Shield, Key, Cpu } from 'lucide-react';

export interface Node3D {
  id: string;
  name: string;
  type: string;
  x: number;
  y: number;
  z: number;
  color: string;
  glow: string;
  icon: React.FC<{ className?: string }>;
}

export interface Link3D {
  from: string;
  to: string;
}

export const INITIAL_3D_NODES: Node3D[] = [
  { id: 'vpc-01', name: 'vpc-main (10.0.0.0/16)', type: 'VPC', x: -330, y: -40, z: -50, color: '#38bdf8', glow: 'rgba(56,189,248,0.6)', icon: Network },
  { id: 'sub-pub', name: 'sub-public-1a', type: 'Subnet', x: -210, y: -90, z: 30, color: '#34d399', glow: 'rgba(52,211,153,0.6)', icon: Network },
  { id: 'sub-priv', name: 'sub-private-1b', type: 'Subnet', x: -210, y: 45, z: -45, color: '#34d399', glow: 'rgba(52,211,153,0.6)', icon: Network },
  { id: 'ec2-web', name: 'i-web-prod-01', type: 'EC2', x: -70, y: -100, z: 70, color: '#fb923c', glow: 'rgba(251,146,60,0.7)', icon: Server },
  { id: 'ec2-api', name: 'i-api-backend-02', type: 'EC2', x: -60, y: -20, z: 0, color: '#fb923c', glow: 'rgba(251,146,60,0.7)', icon: Server },
  { id: 'lambda-fn', name: 'fn-auth-handler', type: 'Lambda', x: -70, y: 75, z: -70, color: '#f59e0b', glow: 'rgba(245,158,11,0.7)', icon: Cpu },
  { id: 'rds-pg', name: 'rds-aurora-cluster', type: 'RDS', x: 95, y: -35, z: 50, color: '#818cf8', glow: 'rgba(129,140,248,0.7)', icon: Database },
  { id: 's3-data', name: 's3-audit-logs', type: 'S3', x: 105, y: 65, z: -35, color: '#10b981', glow: 'rgba(16,185,129,0.7)', icon: Box },
  { id: 'sg-web', name: 'sg-web-ingress', type: 'SecurityGroup', x: 235, y: -80, z: 25, color: '#f87171', glow: 'rgba(248,113,113,0.7)', icon: Shield },
  { id: 'sg-db', name: 'sg-db-internal', type: 'SecurityGroup', x: 245, y: 15, z: 55, color: '#f87171', glow: 'rgba(248,113,113,0.7)', icon: Shield },
  { id: 'iam-role', name: 'role-app-executor', type: 'IAM', x: 360, y: -25, z: -25, color: '#ec4899', glow: 'rgba(236,72,153,0.7)', icon: Key },
];

export const INITIAL_3D_LINKS: Link3D[] = [
  { from: 'vpc-01', to: 'sub-pub' },
  { from: 'vpc-01', to: 'sub-priv' },
  { from: 'sub-pub', to: 'ec2-web' },
  { from: 'sub-priv', to: 'ec2-api' },
  { from: 'sub-priv', to: 'lambda-fn' },
  { from: 'ec2-web', to: 'ec2-api' },
  { from: 'ec2-api', to: 'rds-pg' },
  { from: 'lambda-fn', to: 's3-data' },
  { from: 'ec2-api', to: 's3-data' },
  { from: 'ec2-web', to: 'sg-web' },
  { from: 'rds-pg', to: 'sg-db' },
  { from: 'sg-web', to: 'iam-role' },
  { from: 'sg-db', to: 'iam-role' },
];

export function project3DPoint(
  x: number,
  y: number,
  z: number,
  rotX: number,
  rotY: number,
  viewMode: '3D View' | '2D View'
) {
  if (viewMode === '2D View') {
    return { screenX: x + 410, screenY: y + 140, scale: 1.1, depth: 0 };
  }

  const radX = (rotX * Math.PI) / 180;
  const radY = (rotY * Math.PI) / 180;

  const x1 = x * Math.cos(radY) + z * Math.sin(radY);
  const z1 = -x * Math.sin(radY) + z * Math.cos(radY);

  const y2 = y * Math.cos(radX) - z1 * Math.sin(radX);
  const z2 = y * Math.sin(radX) + z1 * Math.cos(radX);

  const fov = 600;
  const scale = fov / (fov + z2 + 200);
  const screenX = x1 * scale * 1.05 + 410;
  const screenY = y2 * scale * 1.05 + 135;

  return { screenX, screenY, scale: Math.max(0.8, Math.min(scale * 1.15, 1.45)), depth: z2 };
}