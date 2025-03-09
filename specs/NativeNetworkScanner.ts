import { TurboModule, TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  scanNetwork(): Promise<string[]>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NetworkScanner');
