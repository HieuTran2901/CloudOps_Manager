import React, { createContext, useContext, useState } from 'react';
import { APP_CONFIG } from '../config/env';

interface RegionContextType {
  currentRegion: string;
  setRegion: (region: string) => void;
}

const RegionContext = createContext<RegionContextType>({
  currentRegion: APP_CONFIG.defaultRegion,
  setRegion: () => {},
});

export const RegionProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentRegion, setCurrentRegion] = useState<string>(APP_CONFIG.defaultRegion);

  const setRegion = (newRegion: string) => {
    if (newRegion && newRegion.trim()) {
      setCurrentRegion(newRegion.trim());
    }
  };

  return (
    <RegionContext.Provider value={{ currentRegion, setRegion }}>
      {children}
    </RegionContext.Provider>
  );
};

export const useRegion = () => useContext(RegionContext);
