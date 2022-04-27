import { NativeModules } from 'react-native';
const { MultiScanModule } = NativeModules;

interface MultiScanInterface {
  passValueFromReact(message: string): Promise<string>;
  increment(): Promise<string>;
  decrement(): Promise<string>;
  // SDK
  checkAHIResourcesDownloadSize(): Promise<string>;
  areAHIResourcesAvailable(): Promise<string>;
  downloadAHIResources(): Promise<string>;
  setupMultiScanSDK(token: string): Promise<string>;
  authorizeUser(
    userID: string,
    aSalt: string,
    aClaims: string[],
  ): Promise<string>;
  startFaceScan(): Promise<string>;
  startBodyScan(): Promise<string>;
}
export default MultiScanModule as MultiScanInterface;