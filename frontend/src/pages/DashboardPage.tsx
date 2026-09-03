import React, { useEffect, useState, useRef } from 'react';
import { cloudOpsApi } from '../api';
import { CloudResource, TopologyGraph, ComplianceReport, ComplianceFinding, CostAggregationResult, DashboardSnapshotStatus } from '../types/api';
import { useRegion } from '../context/RegionContext';
import { LiveConnectionBanner } from '../components/dashboard/LiveConnectionBanner';
import { TopMetricCards } from '../components/dashboard/TopMetricCards';
import { ComplianceOverviewCard } from '../components/dashboard/ComplianceOverviewCard';
import { ResourceDistributionCard } from '../components/dashboard/ResourceDistributionCard';
import { QuotaUtilizationCard } from '../components/dashboard/QuotaUtilizationCard';
import { OperationalRiskCenter } from '../components/dashboard/OperationalRiskCenter';
import { RecentAlertsCard, LiveAlert } from '../components/dashboard/RecentAlertsCard';
import { TopologyMapCard } from '../components/dashboard/TopologyMapCard';
import { QuickActionsCard } from '../components/dashboard/QuickActionsCard';
import { BottomTrendCards } from '../components/dashboard/BottomTrendCards';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorBanner } from '../components/feedback/ErrorBanner';

export const DashboardPage: React.FC = () => {
  const { currentRegion } = useRegion();
  const [resources, setResources] = useState<CloudResource[]>([]);
  const [topology, setTopology] = useState<TopologyGraph | null>(null);
  const [compliance, setCompliance] = useState<ComplianceReport | null>(null);
  const [cost, setCost] = useState<CostAggregationResult | null>(null);

  const [resourcesStatus, setResourcesStatus] = useState<'SUCCESS' | 'ERROR' | 'DENIED' | 'LOADING'>('LOADING');
  const [topologyStatus, setTopologyStatus] = useState<'SUCCESS' | 'ERROR' | 'DENIED' | 'LOADING'>('LOADING');
  const [complianceStatus, setComplianceStatus] = useState<'SUCCESS' | 'ERROR' | 'DENIED' | 'LOADING'>('LOADING');
  const [costStatus, setCostStatus] = useState<'SUCCESS' | 'ERROR' | 'DENIED' | 'LOADING'>('LOADING');

  const [snapshotState, setSnapshotState] = useState<DashboardSnapshotStatus>('LIVE');
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [lastSync, setLastSync] = useState<string>('Just now');

  // Stale request protection
  const activeRegionRef = useRef<string>(currentRegion);
  activeRegionRef.current = currentRegion;

  useEffect(() => {
    let isMounted = true;
    const requestedRegion = currentRegion;
    setLoading(true);
    setError(null);
    setResourcesStatus('LOADING');
    setTopologyStatus('LOADING');
    setComplianceStatus('LOADING');
    setCostStatus('LOADING');

    cloudOpsApi.getDashboardSnapshot(requestedRegion)
      .then((snapshot) => {
        if (!isMounted || activeRegionRef.current !== requestedRegion) return;

        if (snapshot.resources?.data) {
          setResources(snapshot.resources.data.resources || []);
          setResourcesStatus(snapshot.resources.status === 'DENIED' ? 'DENIED' : snapshot.resources.status === 'ERROR' ? 'ERROR' : 'SUCCESS');
        } else {
          setResources([]);
          setResourcesStatus(snapshot.resources?.status === 'DENIED' ? 'DENIED' : 'ERROR');
        }

        if (snapshot.topology?.data) {
          setTopology(snapshot.topology.data);
          setTopologyStatus(snapshot.topology.status === 'DENIED' ? 'DENIED' : snapshot.topology.status === 'ERROR' ? 'ERROR' : 'SUCCESS');
        } else {
          setTopology(null);
          setTopologyStatus(snapshot.topology?.status === 'DENIED' ? 'DENIED' : 'ERROR');
        }

        if (snapshot.compliance?.data) {
          setCompliance(snapshot.compliance.data);
          setComplianceStatus(snapshot.compliance.status === 'DENIED' ? 'DENIED' : snapshot.compliance.status === 'ERROR' ? 'ERROR' : 'SUCCESS');
        } else {
          setCompliance(null);
          setComplianceStatus(snapshot.compliance?.status === 'DENIED' ? 'DENIED' : 'ERROR');
        }

        if (snapshot.costs?.data) {
          setCost(snapshot.costs.data);
          setCostStatus(snapshot.costs.status === 'DENIED' ? 'DENIED' : snapshot.costs.status === 'ERROR' ? 'ERROR' : 'SUCCESS');
        } else {
          setCost(null);
          setCostStatus(snapshot.costs?.status === 'DENIED' ? 'DENIED' : 'ERROR');
        }

        setSnapshotState(snapshot.snapshotStatus);
        setLastSync(snapshot.generatedAt ? new Date(snapshot.generatedAt).toLocaleTimeString() : new Date().toLocaleTimeString());
        setLoading(false);
      })
      .catch((err: Error) => {
        if (isMounted && activeRegionRef.current === requestedRegion) {
          setError(err.message || 'Failed to fetch live AWS dashboard snapshot');
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [currentRegion]);

  if (loading) {
    return (
      <div className="py-12 flex justify-center items-center">
        <LoadingSpinner message="Ingesting live AWS dashboard analytics..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <ErrorBanner title="Live Dashboard Error" message={error} />
      </div>
    );
  }

  // Derive metrics dynamically from live API state
  const totalResources = resources.length;
  const totalRules = compliance?.totalRulesEvaluated || 0;
  const passCount = compliance?.passCount || 0;
  const failCount = compliance?.failCount || 0;
  const insufficientCount = compliance?.insufficientEvidenceCount || 0;
  const warningCount = insufficientCount;

  const passRate = totalRules > 0 ? Math.round((passCount / totalRules) * 100) : 0;
  const securityScore = totalRules > 0 ? Math.max(0, Math.round(100 - (failCount * 15))) : 100;
  const monthlyCostAmount = cost?.totalAmount || 0;

  const topologyNodes = topology?.nodes?.length || 0;
  const topologyEdges = topology?.edges?.length || 0;

  // Build live alert list from failed compliance rules
  const findingsList: ComplianceFinding[] = compliance?.results || compliance?.findings || [];
  const liveAlerts: LiveAlert[] = findingsList
    .filter((f: ComplianceFinding) => f.status === 'FAIL')
    .map((f: ComplianceFinding, idx: number) => ({
      id: `alert-${idx}-${f.ruleId}`,
      title: `${f.category || 'Security'}: ${f.title}`,
      detail: f.explanation || f.message || f.description || 'Finding detected',
      time: 'Observed Live',
      severity: 'HIGH' as const,
    }));

  return (
    <div className="space-y-5">
      {/* 1. Live Connection Status Banner */}
      <LiveConnectionBanner accountId="351405419700" region={currentRegion} lastSync={lastSync} />

      {/* Background Revalidation SWR Indicator */}
      {snapshotState === 'STALE' && (
        <div className="px-4 py-2 rounded-xl bg-sky-950/40 border border-sky-800/60 text-sky-300 text-xs font-mono flex items-center justify-between shadow-sm">
          <span className="flex items-center space-x-2">
            <span className="w-2 h-2 rounded-full bg-sky-400 animate-ping mr-1"></span>
            <span>Serving cached analytical snapshot • Revalidating live AWS data in background...</span>
          </span>
          <span className="text-[10px] text-slate-400">Instant Render (SWR)</span>
        </div>
      )}

      {/* 2. Top 5 Metric Cards */}
      <TopMetricCards
        totalResources={totalResources}
        complianceCount={totalRules}
        compliancePassRate={passRate}
        monthlyCost={monthlyCostAmount}
        securityScore={securityScore}
        topologyNodes={topologyNodes}
        topologyEdges={topologyEdges}
        resourcesStatus={resourcesStatus}
        complianceStatus={complianceStatus}
        topologyStatus={topologyStatus}
        costStatus={costStatus}
      />

      {/* 3. Middle 3-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <ComplianceOverviewCard
          totalRules={totalRules}
          passed={passCount}
          warning={warningCount}
          failed={failCount}
          ignored={0}
        />
        <ResourceDistributionCard resources={resources} />
        <QuotaUtilizationCard region={currentRegion} />
      </div>

      {/* 4. Operational Risk & Action Intelligence Center */}
      <div className="w-full">
        <OperationalRiskCenter region={currentRegion} />
      </div>

      {/* 5. Alerts, Topology Map & Quick Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2">
          <RecentAlertsCard alerts={liveAlerts} />
        </div>
        <div>
          <QuickActionsCard />
        </div>
      </div>

      <div className="w-full">
        <TopologyMapCard />
      </div>

      {/* 5. Bottom Trend & Risk Cards */}
      <BottomTrendCards monthlyCost={monthlyCostAmount} openFindingsCount={failCount} />
    </div>
  );
};