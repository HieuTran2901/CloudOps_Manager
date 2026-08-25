import React from 'react';
import { LiveConnectionBanner } from '../components/dashboard/LiveConnectionBanner';
import { TopMetricCards } from '../components/dashboard/TopMetricCards';
import { ComplianceOverviewCard } from '../components/dashboard/ComplianceOverviewCard';
import { ResourceDistributionCard } from '../components/dashboard/ResourceDistributionCard';
import { RecentAlertsCard } from '../components/dashboard/RecentAlertsCard';
import { TopologyMapCard } from '../components/dashboard/TopologyMapCard';
import { QuickActionsCard } from '../components/dashboard/QuickActionsCard';
import { BottomTrendCards } from '../components/dashboard/BottomTrendCards';

export const DashboardPage: React.FC = () => {
  return (
    <div className="space-y-5">
      {/* 1. Live Connection Status Banner */}
      <LiveConnectionBanner />

      {/* 2. Top 5 Metric Cards */}
      <TopMetricCards />

      {/* 3. Middle 3-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <ComplianceOverviewCard />
        <ResourceDistributionCard />
        <RecentAlertsCard />
      </div>

      {/* 4. Topology Map & Quick Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2">
          <TopologyMapCard />
        </div>
        <div>
          <QuickActionsCard />
        </div>
      </div>

      {/* 5. Bottom Trend & Risk Cards */}
      <BottomTrendCards />
    </div>
  );
};