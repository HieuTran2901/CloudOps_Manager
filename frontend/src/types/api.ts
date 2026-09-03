export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: string;
}

export interface ApiError {
  success: false;
  errorCode: string;
  message: string;
  timestamp: string;
}

export interface CallerIdentity {
  accountId: string;
  arn: string;
  userId: string;
}

export interface InventorySummary {
  accountId: string;
  region: string;
  totalResources: number;
  typeCounts: Record<string, number>;
  resources: CloudResource[];
  discoveredAt: string;
}

export interface CloudResource {
  resourceId: string;
  resourceType: string;
  name?: string;
  region: string;
  accountId: string;
  status: string;
  arn?: string;
  tags?: Record<string, string>;
  discoveredAt: string;
}

export interface TelemetryDatapoint {
  timestamp: string;
  value: number;
}

export interface TelemetrySeries {
  metricName: string;
  resourceId: string;
  datapoints: TelemetryDatapoint[];
  unit?: string;
}

export interface TelemetryAggregationResult {
  resourceType: string;
  region: string;
  series: TelemetrySeries[];
}

export interface CostPeriod {
  startDate: string;
  endDate: string;
  amount: number;
  currency: string;
}

export interface CostGroup {
  groupKey: string;
  amount: number;
  currency: string;
}

export interface CostAggregationResult {
  granularity: string;
  metric: string;
  totalAmount: number;
  currency: string;
  periods?: CostPeriod[];
  groups?: CostGroup[];
}

export interface CloudTrailEvent {
  eventId: string;
  eventName: string;
  eventTime: string;
  eventSource: string;
  awsRegion: string;
  username: string;
  sourceIpAddress: string;
  readOnly?: boolean;
}

export interface CloudTrailEventResult {
  events: CloudTrailEvent[];
  totalEvents: number;
}

export interface ComplianceFinding {
  ruleId: string;
  title: string;
  category?: string;
  severity?: string;
  status: 'FAIL' | 'PASS' | 'WARNING' | 'INSUFFICIENT_EVIDENCE' | string;
  resourceId?: string;
  resourceType?: string;
  message?: string;
  description?: string;
  explanation?: string;
  remediationGuidance?: string;
}

export interface ComplianceReport {
  accountId: string;
  region: string;
  evaluationTimestamp?: string;
  status?: string;
  passCount?: number;
  failCount?: number;
  insufficientEvidenceCount?: number;
  totalRulesEvaluated?: number;
  results?: ComplianceFinding[];
  findings?: ComplianceFinding[];
}

export interface DriftItem {
  resourceAddress?: string;
  resourceType: string;
  resourceId: string;
  driftType?: string;
  status: string;
  details?: string;
  differences?: Record<string, { expected: unknown; actual: unknown }>;
}

export interface DriftReport {
  accountId: string;
  region: string;
  evaluatedAt?: string;
  status: string;
  driftStatus?: string;
  totalResources?: number;
  totalExpected?: number;
  totalLive?: number;
  driftedResources?: number;
  resources?: DriftItem[];
  items?: DriftItem[];
}

export interface TopologyNode {
  nodeId: string;
  resourceType: string;
  resourceId: string;
  label?: string;
  accountId: string;
  region: string;
  status?: string;
  attributes: Record<string, unknown>;
  metadata?: Record<string, unknown>;
}

export interface TopologyEdge {
  edgeId?: string;
  sourceNodeId: string;
  targetNodeId: string;
  relationshipType: string;
  evidence?: Record<string, unknown>;
  metadata?: Record<string, unknown>;
}

export interface TopologyGraph {
  accountId: string;
  region: string;
  generatedAt: string;
  nodeCount: number;
  edgeCount: number;
  nodes: TopologyNode[];
  edges: TopologyEdge[];
}

export interface SecurityExposure {
  nodeId: string;
  resourceId: string;
  resourceType: string;
  status: 'EXPOSED' | 'NOT_EXPOSED' | 'INSUFFICIENT_EVIDENCE';
  exposureEvidence: Record<string, unknown>;
}

export interface SecurityPath {
  sourceNodeId: string;
  targetNodeId: string;
  nodeIds: string[];
  edges: TopologyEdge[];
  length: number;
}

export interface SecurityReachabilityResult {
  sourceNodeId: string;
  targetNodeId: string;
  status: 'REACHABLE' | 'NOT_REACHABLE' | 'INSUFFICIENT_EVIDENCE';
  path?: SecurityPath;
}

export interface LateralMovementResult {
  sourceNodeId: string;
  targetNodeId: string;
  status: string;
  propagationEvidence: Record<string, unknown>;
}

export interface BlastRadiusResult {
  sourceNodeId: string;
  maxDepth: number;
  reachableNodes: Array<{ nodeId: string; resourceType: string; resourceId: string }>;
  traversedNodeCount: number;
  traversedEdgeCount: number;
}

export interface ForensicExportResult {
  metadata: {
    bundleId: string;
    accountId: string;
    region: string;
    generatedAt: string;
    format: string;
    sha256Digest: string;
    totalItemCount: number;
    sectionCounts: Record<string, number>;
  };
}

export type HealthStatus = 'UP' | 'DEGRADED' | 'UNAVAILABLE' | 'UNKNOWN';

export interface DetailedHealthResponse {
  status: string;
  service: string;
  version: string;
  release: string;
  components: Record<string, HealthStatus>;
  timestamp: string;
}

export type DashboardSnapshotStatus = 'LIVE' | 'STALE' | 'INITIALIZING' | 'REFRESH_FAILED' | 'ERROR';
export type SubsystemStatus = 'LIVE' | 'STALE' | 'EMPTY' | 'DENIED' | 'ERROR' | 'UNAVAILABLE';

export interface SubsystemSnapshot<T> {
  status: SubsystemStatus;
  source: string;
  data: T | null;
  fetchedAt: string;
  errorCode?: string | null;
  errorMessage?: string | null;
}

export interface DashboardSnapshot {
  accountId: string;
  region: string;
  snapshotStatus: DashboardSnapshotStatus;
  generatedAt: string;
  lastSuccessfulRefreshAt: string;
  resources: SubsystemSnapshot<InventorySummary>;
  topology: SubsystemSnapshot<TopologyGraph>;
  compliance: SubsystemSnapshot<ComplianceReport>;
  costs: SubsystemSnapshot<CostAggregationResult>;
}

export type AwsConnectivityStatus =
  | 'CONNECTED'
  | 'AWS_ACCESS_DENIED'
  | 'AWS_THROTTLED'
  | 'AWS_TIMEOUT'
  | 'AWS_UNAVAILABLE'
  | 'PARTIAL_EVIDENCE'
  | 'UNKNOWN';

export interface AwsOperationalStatus {
  status: AwsConnectivityStatus;
  accountId: string;
  region: string;
  lastSuccessfulSync?: string | null;
  lastAttemptedSync?: string;
  evidenceAgeSeconds?: number | null;
  message: string;
  metadata?: Record<string, unknown>;
}

export interface OperationalEvent {
  eventId: string;
  timestamp: string;
  eventType: string;
  severity: 'INFO' | 'WARN' | 'ERROR';
  message: string;
  sourceSubsystem: string;
  sanitizedDetails: Record<string, string>;
}

export type FederationStatus =
  | 'FEDERATED'
  | 'INVALID_ROLE'
  | 'ACCESS_DENIED'
  | 'ACCOUNT_MISMATCH'
  | 'REGION_UNAVAILABLE'
  | 'AWS_TIMEOUT'
  | 'AWS_THROTTLED'
  | 'AWS_UNAVAILABLE'
  | 'UNKNOWN';

export interface AwsAccountContext {
  accountId: string;
  accountName: string;
  defaultRegion: string;
  roleArn?: string | null;
  isCurrent: boolean;
  status: FederationStatus;
}

export interface FederationRequest {
  targetAccountId: string;
  roleArn: string;
  roleSessionName?: string;
  region?: string;
  externalId?: string;
}

export interface FederationResult {
  status: FederationStatus;
  targetAccountId: string;
  assumedRoleArn?: string | null;
  assumedRoleSessionName?: string | null;
  region: string;
  message: string;
  federatedAt: string;
}

export type PreflightStatus =
  | 'PASS'
  | 'BLOCKED'
  | 'ACCESS_DENIED'
  | 'UNAVAILABLE'
  | 'TIMEOUT'
  | 'INSUFFICIENT_EVIDENCE';

export interface AwsCapabilityCheck {
  capabilityName: string;
  requiredAction: string;
  status: PreflightStatus;
  message: string;
}

export interface DeploymentPreflightResult {
  overallStatus: PreflightStatus;
  accountId: string;
  region: string;
  callerArn: string;
  capabilityChecks: AwsCapabilityCheck[];
  evaluatedAt: string;
  summary: string;
}

export type ReleaseGateStatus =
  | 'PASS'
  | 'WARN'
  | 'BLOCKED'
  | 'INSUFFICIENT_EVIDENCE'
  | 'FAILED';

export type ReleaseGateSeverity = 'INFO' | 'WARN' | 'BLOCKING';

export interface ReleaseGateCheck {
  category: string;
  name: string;
  status: ReleaseGateStatus;
  severity: ReleaseGateSeverity;
  message: string;
  evidenceDetails?: string;
}

export interface ReleaseGateResult {
  overallStatus: ReleaseGateStatus;
  analyticsReady: boolean;
  operationallyReady: boolean;
  securityReady: boolean;
  e2eReady: boolean;
  determinismReady: boolean;
  resilienceReady: boolean;
  deploymentReady: boolean;
  runtimeReady: boolean;
  releaseReady: boolean;
  version: string;
  releaseTag: string;
  accountId: string;
  region: string;
  checks: ReleaseGateCheck[];
  sha256Digest: string;
  evaluatedAt: string;
  summary: string;
}

export type IncidentStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RECOVERING' | 'RESOLVED' | 'DEGRADED';
export type IncidentSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type IncidentType =
  | 'AWS_ACCESS_DENIED'
  | 'AWS_THROTTLED'
  | 'AWS_TIMEOUT'
  | 'AWS_UNAVAILABLE'
  | 'PARTIAL_EVIDENCE'
  | 'DISCOVERY_DEGRADED'
  | 'TOPOLOGY_DEGRADED'
  | 'SECURITY_ANALYSIS_DEGRADED'
  | 'COMPLIANCE_DEGRADED'
  | 'FORENSICS_DEGRADED'
  | 'DEPLOYMENT_BLOCKED'
  | 'SYSTEM_DEGRADED';

export interface IncidentRecord {
  incidentId: string;
  type: IncidentType;
  severity: IncidentSeverity;
  status: IncidentStatus;
  accountId: string;
  region: string;
  firstDetectedAt: string;
  lastObservedAt: string;
  occurrenceCount: number;
  message: string;
  source: string;
  evidenceState: string;
  sanitizedMetadata: Record<string, string>;
}

export type EvidenceFreshnessState = 'FRESH' | 'AGING' | 'STALE' | 'EXPIRED' | 'UNAVAILABLE' | 'PARTIAL';

export interface EvidenceLifecycleRecord {
  evidenceType: string;
  accountId: string;
  region: string;
  capturedAt: string;
  lastSuccessfulSync: string;
  lastAttemptedSync: string;
  ageSeconds: number;
  freshnessState: EvidenceFreshnessState;
  evidenceDigest: string;
}

export interface OperationalResilienceEvaluation {
  overallScore: string;
  isResilient: boolean;
  dimensionStates: Record<string, string>;
  activeIncidents: IncidentRecord[];
  evidenceStates: EvidenceLifecycleRecord[];
  accountId: string;
  region: string;
  canonicalDigest: string;
  evaluatedAt: string;
  summary: string;
}

export interface VerificationScenarioResult {
  scenarioId: string;
  scenarioName: string;
  status: string;
  simulatedState: string;
  observedHandling: string;
  executedAt: string;
  isSimulated: boolean;
}

export type QuotaStatus = 'NORMAL' | 'WARNING' | 'CRITICAL' | 'UNKNOWN';

export interface ServiceQuotaItem {
  serviceCode: string;
  serviceName: string;
  quotaCode: string;
  quotaName: string;
  appliedLimit: number | null;
  currentUsage: number | null;
  utilizationPercentage: number | null;
  status: QuotaStatus;
  region: string;
  usageSource: string;
  unit: string;
  adjustable: boolean;
  evaluatedAt: string;
}

export interface QuotaUtilizationReport {
  accountId: string;
  region: string;
  totalQuotasTracked: number;
  normalCount: number;
  warningCount: number;
  criticalCount: number;
  unknownCount: number;
  highestUtilizationPercentage: number;
  quotas: ServiceQuotaItem[];
  statusSummary: Record<string, number>;
  generatedAt: string;
}

export type RiskSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type RiskCategory = 'CAPACITY' | 'SECURITY' | 'RELIABILITY' | 'COMPLIANCE' | 'OPERATIONAL' | 'COST';
export type RiskSource = 'QUOTA' | 'SECURITY' | 'COMPLIANCE' | 'DRIFT' | 'RESILIENCE';
export type ActionSafety = 'READ_ONLY' | 'REQUIRES_APPROVAL' | 'HIGH_RISK';

export interface RecommendedAction {
  actionId: string;
  title: string;
  description: string;
  safetyLevel: ActionSafety;
  stepByStepGuide: string[];
  verificationCheck: string;
}

export interface OperationalRisk {
  riskId: string;
  category: RiskCategory;
  severity: RiskSeverity;
  title: string;
  description: string;
  impact: string;
  affectedResources: string[];
  evidence: Record<string, unknown>;
  detectedAt: string;
  action: RecommendedAction;
  sourceModule: RiskSource;
}

export interface RiskAssessmentReport {
  accountId: string;
  region: string;
  totalRisksTracked: number;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  risks: OperationalRisk[];
  generatedAt: string;
}

export type ImpactAnalysisStatus =
  | 'SUCCESS'
  | 'NOT_FOUND'
  | 'AMBIGUOUS_RESOURCE'
  | 'INVALID_REQUEST'
  | 'ANALYSIS_LIMIT_REACHED'
  | 'PARTIAL'
  | 'UNSUPPORTED_RESOURCE_TYPE';

export interface ImpactResourceSummary {
  nodeId: string;
  resourceType: string;
  resourceId: string;
  accountId: string;
  region: string;
  minimumDepth: number;
  isDirect: boolean;
  attributes: Record<string, unknown>;
}

export interface ImpactPath {
  sourceNodeId: string;
  targetNodeId: string;
  relationshipType: string;
  depth: number;
}

export interface ImpactAnalysisResult {
  targetResource: ImpactResourceSummary | null;
  accountId: string;
  region: string;
  maxDepth: number;
  totalAffectedResources: number;
  directAffectedCount: number;
  indirectAffectedCount: number;
  affectedTypeSummary: Record<string, number>;
  upstreamDependencies: ImpactResourceSummary[];
  downstreamDependents: ImpactResourceSummary[];
  impactPaths: ImpactPath[];
  status: ImpactAnalysisStatus;
  warnings: string[];
  evaluatedAt: string;
}