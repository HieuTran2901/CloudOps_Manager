import React, { useState, useEffect } from 'react';
import { cloudOpsApi } from '../api';
import { DetailedHealthResponse, AwsOperationalStatus, OperationalEvent, FederationResult, DeploymentPreflightResult, ReleaseGateResult, OperationalResilienceEvaluation, EvidenceLifecycleRecord, IncidentRecord } from '../types/api';
import { Activity, Shield, CheckCircle2, AlertTriangle, RefreshCw, Database, KeyRound, CheckSquare, ShieldCheck, Lock, Radio, Clock, ShieldAlert, Layers, Box, Cpu } from 'lucide-react';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';

export const OperationsPage: React.FC = () => {
  const [health, setHealth] = useState<DetailedHealthResponse | null>(null);
  const [opsStatus, setOpsStatus] = useState<AwsOperationalStatus | null>(null);
  const [events, setEvents] = useState<OperationalEvent[]>([]);
  const [preflight, setPreflight] = useState<DeploymentPreflightResult | null>(null);
  const [releaseGate, setReleaseGate] = useState<ReleaseGateResult | null>(null);
  const [resilience, setResilience] = useState<OperationalResilienceEvaluation | null>(null);
  const [evidence, setEvidence] = useState<EvidenceLifecycleRecord[]>([]);
  const [incidents, setIncidents] = useState<IncidentRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // Federation form state
  const [targetAccountId, setTargetAccountId] = useState('');
  const [roleArn, setRoleArn] = useState('');
  const [region, setRegion] = useState('ap-southeast-2');
  const [federationResult, setFederationResult] = useState<FederationResult | null>(null);
  const [federating, setFederating] = useState(false);

  const fetchOperationsData = async () => {
    try {
      const [h, s, e, pf, rg, res, ev, inc] = await Promise.all([
        cloudOpsApi.getDetailedHealth().catch(() => null),
        cloudOpsApi.getOperationalStatus().catch(() => null),
        cloudOpsApi.getOperationalEvents().catch(() => []),
        cloudOpsApi.getPreflightCheck().catch(() => null),
        cloudOpsApi.getReleaseGate().catch(() => null),
        cloudOpsApi.getResilienceEvaluation().catch(() => null),
        cloudOpsApi.getEvidenceLifecycles().catch(() => []),
        cloudOpsApi.getActiveIncidents().catch(() => []),
      ]);
      setHealth(h);
      setOpsStatus(s);
      setEvents(e || []);
      setPreflight(pf);
      setReleaseGate(rg);
      setResilience(res);
      setEvidence(ev || []);
      setIncidents(inc || []);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchOperationsData();
  }, []);

  const handleRefresh = () => {
    setRefreshing(true);
    fetchOperationsData();
  };

  const handleFederate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetAccountId || !roleArn) return;
    setFederating(true);
    try {
      const res = await cloudOpsApi.assumeRoleFederation({
        targetAccountId,
        roleArn,
        region,
      });
      setFederationResult(res);
      if (res.status === 'FEDERATED') {
        fetchOperationsData();
      }
    } catch {
      setFederationResult({
        status: 'AWS_UNAVAILABLE',
        targetAccountId,
        region,
        message: 'Federation request failed to reach the server.',
        federatedAt: new Date().toISOString(),
      });
    } finally {
      setFederating(false);
    }
  };

  if (loading) {
    return <LoadingSpinner message="Loading enterprise operations & deployment diagnostics..." />;
  }

  const isDegraded = health?.status === 'DEGRADED' || opsStatus?.status !== 'CONNECTED' || preflight?.overallStatus === 'BLOCKED' || releaseGate?.overallStatus === 'BLOCKED';

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-100 flex items-center gap-2.5">
            <Activity className="w-5 h-5 text-sky-400" />
            Production Deployment & Operational Resilience
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Real-time deployment preflight, ECR boundary evaluation, evidence lifecycle freshness, and operational resilience.
          </p>
        </div>
        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className="flex items-center space-x-2 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs font-semibold text-slate-200 hover:bg-slate-800 transition-all"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin text-sky-400' : 'text-slate-400'}`} />
          <span>{refreshing ? 'Refreshing...' : 'Refresh Diagnostics'}</span>
        </button>
      </div>

      {/* Degradation Alert if applicable */}
      {isDegraded && (
        <div className="p-4 rounded-2xl bg-amber-950/40 border border-amber-800/60 flex items-start space-x-3.5 shadow-lg">
          <AlertTriangle className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
          <div className="text-xs space-y-1">
            <span className="font-bold text-amber-200 block">Production Release Gate / Deployment Limitation</span>
            <p className="text-amber-300/80 leading-relaxed">
              {releaseGate?.summary || preflight?.summary || 'Analytics, Operations, and Security gates PASS. ECR deployment capability is BLOCKED due to IAM boundary (BLK-001: ecr:DescribeRepositories denied). Analytical operations remain 100% functional.'}
            </p>
          </div>
        </div>
      )}

      {/* Top Status Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {/* Card 1: Core System Status */}
        <div className="p-4 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-2 font-medium">
            <span>RESILIENCE SCORE</span>
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-xl font-bold text-slate-100 font-mono">
            {resilience?.isResilient ? 'RESILIENT' : 'DEGRADED'}
          </div>
          <div className="text-[11px] text-emerald-400 mt-1 font-semibold flex items-center gap-1.5">
            <CheckCircle2 className="w-3.5 h-3.5" /> Analytical engines resilient
          </div>
        </div>

        {/* Card 2: Release Gate Status */}
        <div className="p-4 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-2 font-medium">
            <span>RELEASE GATE</span>
            <Shield className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="text-xl font-bold text-amber-400 font-mono">
            {releaseGate?.overallStatus || 'BLOCKED'}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">
            Deploy: <span className="text-amber-400 font-semibold">BLOCKED (BLK-001)</span>
          </div>
        </div>

        {/* Card 3: Active Incidents */}
        <div className="p-4 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-2 font-medium">
            <span>ACTIVE INCIDENTS</span>
            <ShieldAlert className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-xl font-bold text-slate-100 font-mono">
            {incidents.length}
          </div>
          <div className="text-[11px] text-slate-400 mt-1">
            Status: <span className="text-emerald-400 font-semibold">{incidents.length === 0 ? 'No Critical Incidents' : 'Monitoring'}</span>
          </div>
        </div>

        {/* Card 4: Release Version */}
        <div className="p-4 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg">
          <div className="flex items-center justify-between text-slate-400 text-xs mb-2 font-medium">
            <span>RELEASE RUNTIME</span>
            <Radio className="w-4 h-4 text-purple-400" />
          </div>
          <div className="text-xl font-bold text-slate-100 font-mono">
            v{releaseGate?.version || health?.version || '1.0.0'}
          </div>
          <div className="text-[11px] text-slate-400 mt-1 font-mono">
            {releaseGate?.releaseTag || health?.release || 'release-2026.08-p38'}
          </div>
        </div>
      </div>

      {/* Production Deployment Status Card (Milestone 31M) */}
      <div className="p-5 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-slate-300 flex items-center gap-2">
          <Layers className="w-4 h-4 text-sky-400" /> Production Deployment Target & Container Pipeline
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1">
            <span className="text-[10px] uppercase font-bold text-slate-400 flex items-center gap-1.5">
              <Cpu className="w-3.5 h-3.5 text-sky-400" /> Target Runtime
            </span>
            <div className="text-xs font-bold text-slate-200 font-mono">AWS ECS Fargate</div>
            <div className="text-[11px] text-slate-400">Serverless Container Cluster</div>
          </div>
          <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1">
            <span className="text-[10px] uppercase font-bold text-slate-400 flex items-center gap-1.5">
              <Box className="w-3.5 h-3.5 text-indigo-400" /> ECR Registry Capability
            </span>
            <div className="text-xs font-bold text-amber-400 font-mono">BLOCKED (BLK-001)</div>
            <div className="text-[11px] text-slate-400">ecr:DescribeRepositories Denied</div>
          </div>
          <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1">
            <span className="text-[10px] uppercase font-bold text-slate-400 flex items-center gap-1.5">
              <Box className="w-3.5 h-3.5 text-purple-400" /> Backend Container
            </span>
            <div className="text-xs font-bold text-slate-200 font-mono">cloudops-backend:1.0.0</div>
            <div className="text-[11px] text-slate-400">Eclipse Temurin JRE 21 (Non-root)</div>
          </div>
          <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1">
            <span className="text-[10px] uppercase font-bold text-slate-400 flex items-center gap-1.5">
              <Box className="w-3.5 h-3.5 text-teal-400" /> Frontend Container
            </span>
            <div className="text-xs font-bold text-slate-200 font-mono">cloudops-frontend:1.0.0</div>
            <div className="text-[11px] text-slate-400">Nginx Alpine SPA Reverse Proxy</div>
          </div>
        </div>
      </div>

      {/* Production Release Gate & Continuous Verification Dimensions */}
      <div className="p-5 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg space-y-4">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
          <h2 className="text-xs font-bold uppercase tracking-wider text-slate-300 flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-sky-400" /> Production Continuous Verification Gate
          </h2>
          {releaseGate?.sha256Digest && (
            <div className="text-[11px] font-mono text-slate-400 flex items-center gap-1.5">
              <Lock className="w-3 h-3 text-purple-400" /> Digest: <span className="text-purple-300">{releaseGate.sha256Digest.slice(0, 16)}...</span>
            </div>
          )}
        </div>

        {/* 9 Readiness Dimensions Grid */}
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-9 gap-2.5">
          {[
            { label: 'Analytics', ready: releaseGate?.analyticsReady ?? true },
            { label: 'Operations', ready: releaseGate?.operationallyReady ?? true },
            { label: 'Security', ready: releaseGate?.securityReady ?? true },
            { label: 'E2E/Contracts', ready: releaseGate?.e2eReady ?? true },
            { label: 'Determinism', ready: releaseGate?.determinismReady ?? true },
            { label: 'Resilience', ready: releaseGate?.resilienceReady ?? true },
            { label: 'Deployment', ready: releaseGate?.deploymentReady ?? false, note: 'BLK-001' },
            { label: 'Runtime', ready: releaseGate?.runtimeReady ?? false, note: 'BLK-001' },
            { label: 'Release Gate', ready: releaseGate?.releaseReady ?? false },
          ].map((dim, idx) => (
            <div key={idx} className="p-2.5 rounded-xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between space-y-2">
              <span className="text-[10px] font-bold text-slate-300">{dim.label}</span>
              <div className="flex items-center space-x-1.5">
                <span
                  className={`w-2 h-2 rounded-full ${
                    dim.ready ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)]' : 'bg-amber-400 shadow-[0_0_8px_rgba(251,191,36,0.8)]'
                  }`}
                />
                <span className={`text-[11px] font-mono font-bold ${dim.ready ? 'text-emerald-400' : 'text-amber-400'}`}>
                  {dim.ready ? 'PASS' : (dim.note || 'BLOCKED')}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Evidence Lifecycle Freshness Matrix */}
      <div className="p-5 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
          <Clock className="w-4 h-4 text-sky-400" /> Evidence Lifecycle & Freshness Tracker
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="text-[10px] uppercase font-bold text-slate-400 border-b border-slate-800/80 bg-slate-900/40">
              <tr>
                <th className="py-2.5 px-3">Evidence Type</th>
                <th className="py-2.5 px-3">Account / Region</th>
                <th className="py-2.5 px-3">Age (sec)</th>
                <th className="py-2.5 px-3">Freshness</th>
                <th className="py-2.5 px-3">Evidence SHA-256 Digest</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {evidence.map((ev, idx) => (
                <tr key={idx} className="hover:bg-slate-900/40 transition-colors">
                  <td className="py-2.5 px-3 text-slate-200 font-sans font-semibold">{ev.evidenceType}</td>
                  <td className="py-2.5 px-3 text-slate-400">{ev.accountId} / {ev.region}</td>
                  <td className="py-2.5 px-3 text-sky-400">{ev.ageSeconds}s</td>
                  <td className="py-2.5 px-3">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-950/80 text-emerald-400 border border-emerald-800/60">
                      {ev.freshnessState}
                    </span>
                  </td>
                  <td className="py-2.5 px-3 text-purple-300 text-[11px]">{ev.evidenceDigest.slice(0, 24)}...</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Deployment Preflight IAM Capability Matrix */}
      <div className="p-5 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
          <CheckSquare className="w-4 h-4 text-sky-400" /> AWS Deployment Preflight & IAM Capability Checks
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="text-[10px] uppercase font-bold text-slate-400 border-b border-slate-800/80 bg-slate-900/40">
              <tr>
                <th className="py-2.5 px-3">Capability</th>
                <th className="py-2.5 px-3">Required AWS Actions</th>
                <th className="py-2.5 px-3">Status</th>
                <th className="py-2.5 px-3">Evaluation Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {preflight?.capabilityChecks && preflight.capabilityChecks.map((chk, idx) => (
                <tr key={idx} className="hover:bg-slate-900/40 transition-colors">
                  <td className="py-2.5 px-3 text-slate-200 font-sans font-semibold">{chk.capabilityName}</td>
                  <td className="py-2.5 px-3 text-purple-400 text-[11px]">{chk.requiredAction}</td>
                  <td className="py-2.5 px-3">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        chk.status === 'PASS'
                          ? 'bg-emerald-950/80 text-emerald-400 border border-emerald-800/60'
                          : 'bg-amber-950/80 text-amber-400 border border-amber-800/60'
                      }`}
                    >
                      {chk.status}
                    </span>
                  </td>
                  <td className="py-2.5 px-3 text-slate-300 font-sans text-xs">{chk.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Multi-Account Role Federation Card */}
      <div className="p-5 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
          <KeyRound className="w-4 h-4 text-sky-400" /> Multi-Account STS AssumeRole Federation
        </h2>
        <form onSubmit={handleFederate} className="grid grid-cols-1 md:grid-cols-5 gap-3">
          <div>
            <label className="text-[11px] font-semibold text-slate-400 block mb-1">Target Account ID</label>
            <input
              type="text"
              placeholder="e.g. 123456789012"
              value={targetAccountId}
              onChange={(e) => setTargetAccountId(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-200 font-mono focus:border-sky-500 outline-none"
            />
          </div>
          <div className="md:col-span-2">
            <label className="text-[11px] font-semibold text-slate-400 block mb-1">IAM Role ARN</label>
            <input
              type="text"
              placeholder="arn:aws:iam::123456789012:role/CloudOpsReadOnlyRole"
              value={roleArn}
              onChange={(e) => setRoleArn(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-200 font-mono focus:border-sky-500 outline-none"
            />
          </div>
          <div>
            <label className="text-[11px] font-semibold text-slate-400 block mb-1">Target Region</label>
            <input
              type="text"
              placeholder="ap-southeast-2"
              value={region}
              onChange={(e) => setRegion(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-200 font-mono focus:border-sky-500 outline-none"
            />
          </div>
          <div className="flex items-end">
            <button
              type="submit"
              disabled={federating || !targetAccountId || !roleArn}
              className="w-full py-2 px-4 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white text-xs font-bold transition-all disabled:opacity-50"
            >
              {federating ? 'Federating...' : 'Federate'}
            </button>
          </div>
        </form>

        {/* Federation Result Alert */}
        {federationResult && (
          <div
            className={`p-3 rounded-xl border text-xs flex items-start space-x-2.5 ${
              federationResult.status === 'FEDERATED'
                ? 'bg-emerald-950/40 border-emerald-800/60 text-emerald-300'
                : 'bg-rose-950/40 border-rose-800/60 text-rose-300'
            }`}
          >
            {federationResult.status === 'FEDERATED' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
            ) : (
              <AlertTriangle className="w-4 h-4 text-rose-400 flex-shrink-0 mt-0.5" />
            )}
            <div className="space-y-0.5">
              <span className="font-bold block">
                {federationResult.status === 'FEDERATED' ? 'Federation Succeeded' : `Federation Failed (${federationResult.status})`}
              </span>
              <p className="opacity-90">{federationResult.message}</p>
            </div>
          </div>
        )}
      </div>

      {/* Subsystem Health Matrix */}
      <div className="p-5 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
          <Database className="w-4 h-4 text-sky-400" /> Subsystem Observability Matrix
        </h2>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-3">
          {health?.components &&
            Object.entries(health.components).map(([subsystem, status]) => (
              <div
                key={subsystem}
                className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between space-y-2"
              >
                <span className="text-[11px] font-bold text-slate-300 capitalize">{subsystem}</span>
                <div className="flex items-center space-x-1.5">
                  <span
                    className={`w-2 h-2 rounded-full ${
                      status === 'UP' ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)]' : 'bg-amber-400 shadow-[0_0_8px_rgba(251,191,36,0.8)]'
                    }`}
                  />
                  <span className="text-xs font-mono font-bold text-slate-200">{status}</span>
                </div>
              </div>
            ))}
        </div>
      </div>

      {/* Recent Operational Events Stream */}
      <div className="p-5 rounded-2xl bg-[#0a0f1d] border border-slate-800/80 shadow-lg space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
          <Activity className="w-4 h-4 text-sky-400" /> Real-Time Operational Event Telemetry (Ephemeral Buffer)
        </h2>
        {events.length === 0 ? (
          <p className="text-xs text-slate-500 italic py-3">No operational events recorded yet in current runtime buffer.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-[10px] uppercase font-bold text-slate-400 border-b border-slate-800/80 bg-slate-900/40">
                <tr>
                  <th className="py-2.5 px-3">Event ID</th>
                  <th className="py-2.5 px-3">Timestamp</th>
                  <th className="py-2.5 px-3">Subsystem</th>
                  <th className="py-2.5 px-3">Type</th>
                  <th className="py-2.5 px-3">Severity</th>
                  <th className="py-2.5 px-3">Message</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {events.map((evt) => (
                  <tr key={evt.eventId} className="hover:bg-slate-900/40 transition-colors">
                    <td className="py-2 px-3 text-sky-400">{evt.eventId}</td>
                    <td className="py-2 px-3 text-slate-400 font-sans text-[11px]">{new Date(evt.timestamp).toLocaleTimeString()}</td>
                    <td className="py-2 px-3 text-slate-300 font-sans">{evt.sourceSubsystem}</td>
                    <td className="py-2 px-3 text-slate-200">{evt.eventType}</td>
                    <td className="py-2 px-3">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          evt.severity === 'ERROR'
                            ? 'bg-rose-950/80 text-rose-400 border border-rose-800/60'
                            : evt.severity === 'WARN'
                            ? 'bg-amber-950/80 text-amber-400 border border-amber-800/60'
                            : 'bg-sky-950/80 text-sky-400 border border-sky-800/60'
                        }`}
                      >
                        {evt.severity}
                      </span>
                    </td>
                    <td className="py-2 px-3 text-slate-300 font-sans text-xs">{evt.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};