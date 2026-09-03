import React from 'react';
import { Header } from './Header';
import { Sidebar, NavTab } from './Sidebar';
import { useRegion } from '../../context/RegionContext';

interface AppLayoutProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({ activeTab, onTabChange, children }) => {
  const { currentRegion, setRegion } = useRegion();

  return (
    <div className="min-h-screen flex flex-col bg-slate-950 text-slate-100">
      <Header currentRegion={currentRegion} onRegionChange={setRegion} />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar activeTab={activeTab} onTabSelect={onTabChange} />
        <main className="flex-1 overflow-y-auto p-6 bg-slate-900/30">{children}</main>
      </div>
    </div>
  );
};