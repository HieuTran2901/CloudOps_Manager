import React, { useState } from 'react';
import { AppLayout } from '../components/layout/AppLayout';
import { NavTab } from '../components/layout/Sidebar';
import { ErrorBoundary } from '../components/feedback/ErrorBoundary';
import { RegionProvider } from '../context/RegionContext';

import { DashboardPage } from '../pages/DashboardPage';
import { ResourcesPage } from '../pages/ResourcesPage';
import { ObservabilityPage } from '../pages/ObservabilityPage';
import { CostsPage } from '../pages/CostsPage';
import { CloudTrailPage } from '../pages/CloudTrailPage';
import { CompliancePage } from '../pages/CompliancePage';
import { DriftPage } from '../pages/DriftPage';
import { TopologyPage } from '../pages/TopologyPage';
import { SecurityPage } from '../pages/SecurityPage';
import { ForensicsPage } from '../pages/ForensicsPage';
import { OperationsPage } from '../pages/OperationsPage';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<NavTab>('dashboard');

  const renderActivePage = () => {
    switch (activeTab) {
      case 'dashboard': return <DashboardPage />;
      case 'resources': return <ResourcesPage />;
      case 'observability': return <ObservabilityPage />;
      case 'costs': return <CostsPage />;
      case 'cloudtrail': return <CloudTrailPage />;
      case 'compliance': return <CompliancePage />;
      case 'drift': return <DriftPage />;
      case 'topology': return <TopologyPage />;
      case 'security': return <SecurityPage />;
      case 'forensics': return <ForensicsPage />;
      case 'operations': return <OperationsPage />;
      default: return <DashboardPage />;
    }
  };

  return (
    <ErrorBoundary>
      <RegionProvider>
        <AppLayout activeTab={activeTab} onTabChange={setActiveTab}>
          {renderActivePage()}
        </AppLayout>
      </RegionProvider>
    </ErrorBoundary>
  );
};