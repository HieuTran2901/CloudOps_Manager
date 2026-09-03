import React, { useEffect, useState } from 'react';
import {
  ShieldAlert,
  AlertOctagon,
  AlertTriangle,
  Info,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  SlidersHorizontal,
  CheckCircle,
  Lock,
  Cpu,
  Layers,
  FileCheck,
  Network
} from 'lucide-react';
import { cloudOpsApi } from '../../api';
import {
  RiskAssessmentReport,
  OperationalRisk,
  RiskSeverity,
  RiskCategory,
  ActionSafety,
  ImpactAnalysisResult
} from '../../types/api';

interface OperationalRiskCenterProps {
  region?: string;
}

export const OperationalRiskCenter: React.FC<OperationalRiskCenterProps> = ({ region }) => {
  const [report, setReport] = useState<RiskAssessmentReport | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Client-side filtering state
  const [selectedSeverity, setSelectedSeverity] = useState<string>('ALL');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [expandedRiskId, setExpandedRiskId] = useState<string | null>(null);
  const [impactMap, setImpactMap] = useState<Record<string, ImpactAnalysisResult>>({});
  const [loadingImpactId, setLoadingImpactId] = useState<string | null>(null);

  const fetchRisks = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await cloudOpsApi.getRisks(region);
      if (data) {
        setReport(data);
      } else {
        setError('Failed to retrieve operational risk assessment.');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error fetching operational risks.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRisks();
  }, [region]);

  const toggleExpand = (riskId: string) => {
    setExpandedRiskId(prev => (prev === riskId ? null : riskId));
  };

  const handleInspectImpact = async (risk: OperationalRisk) => {
    if (impactMap[risk.riskId]) return;
    const resId = risk.affectedResources?.[0];
    if (!resId) return;

    let resType = 'EC2_INSTANCE';
    if (resId.startsWith('sg-')) resType = 'SECURITY_GROUP';
    else if (resId.startsWith('subnet-')) resType = 'SUBNET';
    else if (resId.startsWith('vpc-')) resType = 'VPC';
    else if (resId.startsWith('i-')) resType = 'EC2_INSTANCE';
    else if (resId.includes('rds') || risk.category === 'RELIABILITY') resType = 'RDS_INSTANCE';
    else if (risk.title.toLowerCase().includes('iam')) resType = 'IAM_ROLE';

    setLoadingImpactId(risk.riskId);
    try {
      const result = await cloudOpsApi.getImpactBlastRadius(resType, resId, region, undefined, 3);
      if (result) {
        setImpactMap(prev => ({ ...prev, [risk.riskId]: result }));
      }
    } catch {
      // Non-fatal inspection error
    } finally {
      setLoadingImpactId(null);
    }
  };

  const getSeverityBadge = (severity: RiskSeverity) => {
    switch (severity) {
      case 'CRITICAL':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold bg-rose-950/80 border border-rose-500/50 text-rose-300">
            <AlertOctagon className="w-3 h-3 text-rose-400" />
            CRITICAL
          </span>
        );
      case 'HIGH':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold bg-amber-950/80 border border-amber-500/50 text-amber-300">
            <AlertTriangle className="w-3 h-3 text-amber-400" />
            HIGH
          </span>
        );
      case 'MEDIUM':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-semibold bg-yellow-950/60 border border-yellow-500/40 text-yellow-300">
            <Info className="w-3 h-3 text-yellow-400" />
            MEDIUM
          </span>
        );
      case 'LOW':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-medium bg-slate-900 border border-slate-700 text-slate-400">
            <Info className="w-3 h-3 text-slate-400" />
            LOW
          </span>
        );
    }
  };

  const getCategoryIcon = (category: RiskCategory) => {
    switch (category) {
      case 'CAPACITY':
        return <Cpu className="w-3.5 h-3.5 text-cyan-400" />;
      case 'SECURITY':
        return <Lock className="w-3.5 h-3.5 text-rose-400" />;
      case 'RELIABILITY':
        return <Layers className="w-3.5 h-3.5 text-purple-400" />;
      case 'COMPLIANCE':
        return <FileCheck className="w-3.5 h-3.5 text-blue-400" />;
      case 'OPERATIONAL':
        return <SlidersHorizontal className="w-3.5 h-3.5 text-amber-400" />;
      default:
        return <SlidersHorizontal className="w-3.5 h-3.5 text-slate-400" />;
    }
  };

  const getSafetyBadge = (safety: ActionSafety) => {
    switch (safety) {
      case 'HIGH_RISK':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[9px] font-bold bg-rose-950/90 border border-rose-600/60 text-rose-300">
            HIGH RISK
          </span>
        );
      case 'REQUIRES_APPROVAL':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[9px] font-bold bg-amber-950/90 border border-amber-600/60 text-amber-300">
            REQUIRES APPROVAL
          </span>
        );
      case 'READ_ONLY':
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[9px] font-medium bg-blue-950/90 border border-blue-600/50 text-blue-300">
            READ ONLY
          </span>
        );
    }
  };

  const filteredRisks = (report?.risks || []).filter(r => {
    if (selectedSeverity !== 'ALL' && r.severity !== selectedSeverity) return false;
    if (selectedCategory !== 'ALL' && r.category !== selectedCategory) return false;
    return true;
  });

  return (
    <div className="rounded-2xl border border-slate-800/90 bg-gradient-to-b from-[#0c1324] to-[#070b16] p-5 shadow-xl flex flex-col justify-between">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-800/80">
        <div className="flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-lg bg-rose-950/60 border border-rose-500/40 flex items-center justify-center">
            <ShieldAlert className="w-4 h-4 text-rose-400" />
          </div>
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-100 flex items-center gap-2">
              OPERATIONAL RISK & ACTION INTELLIGENCE
              <span className="text-[9px] font-mono px-1.5 py-0.5 rounded bg-blue-950/80 border border-blue-500/30 text-blue-300">
                MVP
              </span>
            </h3>
            <p className="text-[10px] text-slate-400">
              Region: <span className="font-mono text-slate-300">{report?.region || region || 'ap-southeast-2'}</span>
            </p>
          </div>
        </div>

        {/* Counter Badges & Refresh */}
        <div className="flex items-center gap-2">
          {report && (
            <div className="flex items-center gap-1.5 text-[10px] font-mono">
              {report.criticalCount > 0 && (
                <span className="px-2 py-0.5 rounded-md bg-rose-500/20 border border-rose-500/40 text-rose-300 font-bold">
                  {report.criticalCount} Critical
                </span>
              )}
              {report.highCount > 0 && (
                <span className="px-2 py-0.5 rounded-md bg-amber-500/20 border border-amber-500/40 text-amber-300 font-bold">
                  {report.highCount} High
                </span>
              )}
              {report.mediumCount > 0 && (
                <span className="px-2 py-0.5 rounded-md bg-yellow-500/20 border border-yellow-500/30 text-yellow-300 font-semibold">
                  {report.mediumCount} Med
                </span>
              )}
              {report.lowCount > 0 && (
                <span className="px-2 py-0.5 rounded-md bg-slate-800 border border-slate-700 text-slate-400">
                  {report.lowCount} Low
                </span>
              )}
            </div>
          )}
          <button
            onClick={fetchRisks}
            disabled={loading}
            className="p-1 rounded-md text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 transition-colors"
            title="Refresh operational risks"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-wrap items-center justify-between gap-2 my-3 text-[10px]">
        {/* Severity Filters */}
        <div className="flex items-center gap-1">
          <span className="text-slate-500 font-medium mr-1">Severity:</span>
          {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM'].map(s => (
            <button
              key={s}
              onClick={() => setSelectedSeverity(s)}
              className={`px-2 py-0.5 rounded-md font-medium transition-colors ${
                selectedSeverity === s
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-slate-900/80 text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              {s}
            </button>
          ))}
        </div>

        {/* Category Filters */}
        <div className="flex items-center gap-1">
          <span className="text-slate-500 font-medium mr-1">Category:</span>
          {['ALL', 'CAPACITY', 'SECURITY', 'RELIABILITY', 'COMPLIANCE', 'OPERATIONAL'].map(c => (
            <button
              key={c}
              onClick={() => setSelectedCategory(c)}
              className={`px-2 py-0.5 rounded-md font-medium transition-colors ${
                selectedCategory === c
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'bg-slate-900/80 text-slate-400 hover:text-slate-200 border border-slate-800'
              }`}
            >
              {c}
            </button>
          ))}
        </div>
      </div>

      {/* Content Area */}
      <div className="my-1 flex-1 flex flex-col justify-center">
        {loading ? (
          <div className="space-y-3 py-4">
            {[1, 2].map(i => (
              <div key={i} className="animate-pulse space-y-2 p-3 bg-slate-900/40 rounded-xl border border-slate-800/60">
                <div className="h-3.5 bg-slate-800 rounded w-1/3"></div>
                <div className="h-2.5 bg-slate-800/60 rounded w-4/5"></div>
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="p-3.5 rounded-xl bg-rose-950/40 border border-rose-800/50 text-xs text-rose-300 flex items-center gap-2.5">
            <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
        ) : filteredRisks.length === 0 ? (
          <div className="py-8 text-center text-slate-500 text-xs">
            <CheckCircle className="w-6 h-6 text-emerald-500/70 mx-auto mb-1.5" />
            No operational risks identified matching active filters.
          </div>
        ) : (
          <div className="space-y-3 max-h-80 overflow-y-auto pr-1">
            {filteredRisks.map((risk: OperationalRisk) => {
              const isExpanded = expandedRiskId === risk.riskId;

              return (
                <div
                  key={risk.riskId}
                  className="bg-slate-900/60 rounded-xl border border-slate-800/70 hover:border-slate-700/80 transition-all p-3 space-y-2.5"
                >
                  {/* Card Title Line */}
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2 min-w-0">
                      <div className="p-1 rounded bg-slate-800/80 border border-slate-700/50 shrink-0">
                        {getCategoryIcon(risk.category)}
                      </div>
                      <div className="min-w-0">
                        <h4 className="text-xs font-semibold text-slate-100 truncate" title={risk.title}>
                          {risk.title}
                        </h4>
                        <div className="flex items-center gap-2 text-[10px] text-slate-400 mt-0.5">
                          <span className="font-mono text-slate-500 uppercase">{risk.sourceModule}</span>
                          <span>•</span>
                          <span className="truncate">{risk.description}</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      {getSeverityBadge(risk.severity)}
                      <button
                        onClick={() => toggleExpand(risk.riskId)}
                        className="p-1 rounded-md text-slate-400 hover:text-slate-100 hover:bg-slate-800 transition-colors"
                        title={isExpanded ? 'Collapse action details' : 'Expand action details'}
                      >
                        {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                      </button>
                    </div>
                  </div>

                  {/* Impact Summary Line */}
                  <div className="text-[11px] text-slate-300 bg-slate-950/50 p-2 rounded-lg border border-slate-800/60 flex items-start gap-2">
                    <span className="font-semibold text-rose-300/90 shrink-0">Impact:</span>
                    <span className="text-slate-400">{risk.impact}</span>
                  </div>

                  {/* Action Summary & Safety */}
                  <div className="flex items-center justify-between text-[11px] pt-1">
                    <div className="flex items-center gap-2 truncate">
                      <span className="text-slate-500 font-medium">Action:</span>
                      <span className="text-blue-300 font-medium truncate">{risk.action?.title}</span>
                    </div>
                    <div>{getSafetyBadge(risk.action?.safetyLevel)}</div>
                  </div>

                  {/* Expanded Action & Evidence Details */}
                  {isExpanded && (
                    <div className="pt-2 border-t border-slate-800/70 space-y-2 text-[10px]">
                      {/* Step-by-step Guide */}
                      {risk.action?.stepByStepGuide && risk.action.stepByStepGuide.length > 0 && (
                        <div className="space-y-1 bg-slate-950/70 p-2.5 rounded-lg border border-slate-800">
                          <span className="font-semibold text-slate-200">Recommended Steps:</span>
                          <ol className="list-decimal list-inside space-y-0.5 text-slate-400 font-mono">
                            {risk.action.stepByStepGuide.map((step, idx) => (
                              <li key={idx}>{step}</li>
                            ))}
                          </ol>
                        </div>
                      )}

                      {/* Verification Check */}
                      {risk.action?.verificationCheck && (
                        <div className="flex items-center gap-1.5 text-emerald-400/90 font-mono bg-emerald-950/30 p-2 rounded-lg border border-emerald-500/20">
                          <CheckCircle className="w-3 h-3 shrink-0 text-emerald-400" />
                          <span>Verification: {risk.action.verificationCheck}</span>
                        </div>
                      )}

                      {/* Blast Radius Section */}
                      {risk.affectedResources && risk.affectedResources.length > 0 && (
                        <div className="bg-slate-950/80 p-2.5 rounded-lg border border-indigo-900/40 space-y-1.5">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-1.5 text-indigo-300 font-semibold">
                              <Network className="w-3.5 h-3.5 text-indigo-400" />
                              <span>Topology Blast Radius</span>
                            </div>
                            {!impactMap[risk.riskId] && (
                              <button
                                onClick={() => handleInspectImpact(risk)}
                                disabled={loadingImpactId === risk.riskId}
                                className="px-2 py-0.5 rounded bg-indigo-600/80 hover:bg-indigo-600 text-white text-[9px] font-medium transition-colors flex items-center gap-1"
                              >
                                {loadingImpactId === risk.riskId ? (
                                  <RefreshCw className="w-2.5 h-2.5 animate-spin" />
                                ) : (
                                  'Analyze Impact'
                                )}
                              </button>
                            )}
                          </div>

                          {impactMap[risk.riskId] && (
                            <div className="space-y-1 pt-1 text-[9px] font-mono text-slate-300">
                              <div className="flex items-center gap-2">
                                <span className="text-slate-500">Total Affected:</span>
                                <span className="text-amber-300 font-bold">
                                  {impactMap[risk.riskId].totalAffectedResources} resources
                                </span>
                                <span className="text-slate-600">|</span>
                                <span className="text-slate-400">
                                  Direct: {impactMap[risk.riskId].directAffectedCount}, Indirect: {impactMap[risk.riskId].indirectAffectedCount}
                                </span>
                              </div>

                              {impactMap[risk.riskId].downstreamDependents.length > 0 && (
                                <div className="text-slate-400">
                                  <span className="text-slate-500">Downstream Dependents: </span>
                                  {impactMap[risk.riskId].downstreamDependents.map(d => `${d.resourceId} (depth ${d.minimumDepth})`).join(', ')}
                                </div>
                              )}

                              {impactMap[risk.riskId].upstreamDependencies.length > 0 && (
                                <div className="text-slate-400">
                                  <span className="text-slate-500">Upstream Dependencies: </span>
                                  {impactMap[risk.riskId].upstreamDependencies.map(u => `${u.resourceId} (depth ${u.minimumDepth})`).join(', ')}
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      )}

                      {/* Evidence JSON */}
                      {risk.evidence && Object.keys(risk.evidence).length > 0 && (
                        <div className="bg-slate-950/90 p-2 rounded-lg border border-slate-800/90 font-mono text-slate-400 overflow-x-auto text-[9px]">
                          <span className="text-slate-500">Evidence: </span>
                          {JSON.stringify(risk.evidence)}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
