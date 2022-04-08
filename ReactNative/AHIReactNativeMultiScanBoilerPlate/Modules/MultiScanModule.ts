import {NativeModules} from 'react-native';
const {MultiScanModule} = NativeModules;

interface MultiScanInterface {
  passValueFromReact(message: string): Promise<string>;
  increment(): Promise<string>;
  decrement(): Promise<string>;
  // SDK
  setupMultiScanSDK(token: string): Promise<string>;
  authorizeUser(
    userID: string,
    aSalt: string,
    aClaims: string[],
  ): Promise<string>;
  isUserAuthorized(): Promise<string>;
  downloadAHIResources(): Promise<string>;
  checkResources(): Promise<string>;
  checkAHIResourcesDownloadSize(): Promise<string>;
  areAHIResourcesAvailable(): Promise<string>;
}

export default MultiScanModule as MultiScanInterface;
