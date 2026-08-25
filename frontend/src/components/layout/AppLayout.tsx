import React, { useState } from 'react';
import { Header } from './Header';
import { Sidebar, NavTab } from './Sidebar';
import { APP_CONFIG } from '../../config/env';

interface AppLayoutProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({ activeTab, onTabChange, children }) => {
  const [region, setRegion] = useState(APP_CONFIG.defaultRegion);

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100">
      <Header currentRegion={region} onRegionChange={setRegion} />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar activeTab={activeTab} onTabSelect={onTabChange} />
        <main className="flex-1 overflow-y-auto p-6 bg-slate-900/30">{children}</main>
      </div>
    </div>
  );
};