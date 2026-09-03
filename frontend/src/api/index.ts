import { apiFetch } from './client/apiClient';
import {
  CallerIdentity,
  InventorySummary,
  TelemetryAggregationResult,
  CostAggregationResult,
  CloudTrailEventResult,
  ComplianceReport,
  DriftReport,
  TopologyGraph,
  SecurityExposure,
  SecurityReachabilityResult,
  LateralMovementResult,
  BlastRadiusResult,
  DetailedHealthResponse,
  AwsOperationalStatus,
  OperationalEvent,
  AwsAccountContext,
  FederationRequest,
  FederationResult,
  DeploymentPreflightResult,
  ReleaseGateResult,
  IncidentRecord,
  EvidenceLifecycleRecord,
  OperationalResilienceEvaluation,
  VerificationScenarioResult,
  DashboardSnapshot,
  QuotaUtilizationReport,
  ServiceQuotaItem,
  RiskAssessmentReport,
  ImpactAnalysisResult,
} from '../types/api';

export const cloudOpsApi = {
  // Service Quotas & Capacity
  getQuotas: (region?: string) =>
    apiFetch<QuotaUtilizationReport>(`/quotas${region ? `?region=${region}` : ''}`),
  getServiceQuotas: (serviceCode: string, region?: string) =>
    apiFetch<ServiceQuotaItem[]>(`/quotas/${encodeURIComponent(serviceCode)}${region ? `?region=${region}` : ''}`),
  // STS / Identity
  getIdentity: () => apiFetch<CallerIdentity>('/sts/caller-identity'),

  // Discovery / Resources
  discoverResources: (region?: string) =>
    apiFetch<InventorySummary>(`/resources${region ? `?region=${region}` : ''}`),

  getEc2Detail: (id: string, region?: string) =>
    apiFetch<unknown>(`/resources/ec2/${id}${region ? `?region=${region}` : ''}`),

  getS3Detail: (name: string, region?: string) =>
    apiFetch<unknown>(`/resources/s3/${name}${region ? `?region=${region}` : ''}`),

  getRdsDetail: (id: string, region?: string) =>
    apiFetch<unknown>(`/resources/rds/${id}${region ? `?region=${region}` : ''}`),

  // Observability
  getTelemetryMetrics: (resourceType: string, resourceIds: string[], metricNames: string[], region?: string) => {
    const params = new URLSearchParams({
      resourceType,
      resourceIds: resourceIds.join(','),
      metricNames: metricNames.join(','),
    });
    if (region) params.append('region', region);
    return apiFetch<TelemetryAggregationResult>(`/observability/metrics?${params.toString()}`);
  },

  // Costs
  getCosts: (granularity = 'MONTHLY') =>
    apiFetch<CostAggregationResult>(`/costs?granularity=${granularity}`),

  // CloudTrail Audit
  getCloudTrailEvents: (maxResults = 50, region?: string) =>
    apiFetch<CloudTrailEventResult>(
      `/audit/cloudtrail/events?maxResults=${maxResults}${region ? `&region=${region}` : ''}`
    ),

  // Compliance
  getComplianceReport: (region?: string) =>
    apiFetch<ComplianceReport>(`/compliance${region ? `?region=${region}` : ''}`),

  // Drift
  getSupportedDriftTypes: () => apiFetch<string[]>('/drift/supported-resources'),
  evaluateDrift: (terraformJson: string, region?: string) =>
    apiFetch<DriftReport>(`/drift/evaluate${region ? `?region=${region}` : ''}`, {
      method: 'POST',
      body: terraformJson,
    }),

  // Topology
  getTopology: (region?: string) =>
    apiFetch<TopologyGraph>(`/topology${region ? `?region=${region}` : ''}`),

  // Security
  getExposures: (region?: string) =>
    apiFetch<SecurityExposure[]>(`/security/exposures${region ? `?region=${region}` : ''}`),

  getBlastRadius: (nodeId: string, maxDepth = 3, region?: string) =>
    apiFetch<BlastRadiusResult>(
      `/security/blast-radius/${encodeURIComponent(nodeId)}?maxDepth=${maxDepth}${
        region ? `&region=${region}` : ''
      }`
    ),

  getReachability: (from: string, to: string, maxDepth = 5, region?: string) =>
    apiFetch<SecurityReachabilityResult>(
      `/security/reachability?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&maxDepth=${maxDepth}${
        region ? `&region=${region}` : ''
      }`
    ),

  getLateralMovement: (maxDepth = 3, region?: string) =>
    apiFetch<LateralMovementResult[]>(
      `/security/lateral-movement?maxDepth=${maxDepth}${region ? `&region=${region}` : ''}`
    ),

  // Forensics
  downloadForensicExportUrl: (format: 'json' | 'csv' = 'json', region?: string) =>
    `/api/v1/aws/forensics/export?format=${format}${region ? `&region=${region}` : ''}`,

  // Operations & Health
  getDashboardSnapshot: (region?: string) =>
    apiFetch<DashboardSnapshot>(`/dashboard/snapshot${region ? `?region=${region}` : ''}`),
  refreshDashboardSnapshot: (region?: string) =>
    apiFetch<DashboardSnapshot>(`/dashboard/snapshot/refresh${region ? `?region=${region}` : ''}`, {
      method: 'POST',
    }),
  getDetailedHealth: () => apiFetch<DetailedHealthResponse>('/health'),
  getOperationalStatus: (region?: string) =>
    apiFetch<AwsOperationalStatus>(`/operations/status${region ? `?region=${region}` : ''}`),
  getOperationalEvents: () => apiFetch<OperationalEvent[]>('/operations/events'),
  getEvidenceFreshness: (region?: string) =>
    apiFetch<Record<string, unknown>>(`/operations/freshness${region ? `&region=${region}` : ''}`),

  // Multi-Account Federation
  assumeRoleFederation: (request: FederationRequest) =>
    apiFetch<FederationResult>('/federation/assume-role', {
      method: 'POST',
      body: JSON.stringify(request),
    }),
  getCurrentAccountContext: () => apiFetch<AwsAccountContext>('/federation/current-context'),
  getConfiguredAccounts: () => apiFetch<AwsAccountContext[]>('/federation/accounts'),

  // Preflight Deployment Verification
  getPreflightCheck: (region?: string) =>
    apiFetch<DeploymentPreflightResult>(`/preflight${region ? `?region=${region}` : ''}`),

  // Production Release Gate
  getReleaseGate: (region?: string) =>
    apiFetch<ReleaseGateResult>(`/release/gate${region ? `?region=${region}` : ''}`),

  // Phase 30: Resilience & Incident Detection
  getAllIncidents: () => apiFetch<IncidentRecord[]>('/operations/incidents'),
  getActiveIncidents: () => apiFetch<IncidentRecord[]>('/operations/incidents/active'),
  getResilienceEvaluation: (region?: string) =>
    apiFetch<OperationalResilienceEvaluation>(`/operations/resilience${region ? `?region=${region}` : ''}`),
  getEvidenceLifecycles: (accountId?: string, region?: string) => {
    const params = new URLSearchParams();
    if (accountId) params.append('accountId', accountId);
    if (region) params.append('region', region);
    return apiFetch<EvidenceLifecycleRecord[]>(`/operations/evidence?${params.toString()}`);
  },
  getVerificationScenarios: () => apiFetch<VerificationScenarioResult[]>('/operations/resilience/verification'),

  // Phase 51: Operational Risk & Action Intelligence
  getRisks: (region?: string, category?: string, severity?: string) => {
    const params = new URLSearchParams();
    if (region) params.append('region', region);
    if (category) params.append('category', category);
    if (severity) params.append('severity', severity);
    const query = params.toString() ? `?${params.toString()}` : '';
    return apiFetch<RiskAssessmentReport>(`/risks${query}`);
  },

  // Phase 53: Change Impact & Blast-Radius Intelligence
  getImpactBlastRadius: (resourceType: string, resourceId: string, region?: string, accountId?: string, maxDepth: number = 3) => {
    const params = new URLSearchParams();
    params.append('resourceType', resourceType);
    params.append('resourceId', resourceId);
    if (region) params.append('region', region);
    if (accountId) params.append('accountId', accountId);
    if (maxDepth !== undefined) params.append('maxDepth', maxDepth.toString());
    return apiFetch<ImpactAnalysisResult>(`/impact/blast-radius?${params.toString()}`);
  },
};