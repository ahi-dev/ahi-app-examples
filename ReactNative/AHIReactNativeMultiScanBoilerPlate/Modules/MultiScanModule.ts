//
//  AHI - Example Code
//
//  Copyright (c) Advanced Human Imaging. All rights reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

import { NativeModules } from 'react-native';
const { MultiScanModule } = NativeModules;

/** Multi Scan SDK */
interface MultiScanInterface {
  /** Requires a token String to be provided as an argument. */
  setupMultiScanSDK(token: string): Promise<string>;
  /** Requires a Map object to be passed in containing 3 arguments. */
  authorizeUser(
    userID: string,
    salt: string,
    claims: string[],
  ): Promise<string>;
  /** Will return a boolean. */
  areAHIResourcesAvailable(): Promise<boolean>;
  /** A void function that will invoke the download of remote resources. */
  downloadAHIResources(): void;
  /** Will return an integer for the bytes size. */
  checkAHIResourcesDownloadSize(): Promise<string>;
  /** Will return an map for current download progress report. */
  getResourcesDownloadProgressReport(): void;
  startFaceScan(
    userInput: Object,
  ): Promise<Map<String, any>>;
  startFingerScan(
    userInput: Object,
  ): Promise<Map<String, any>>;
  startBodyScan(
    userInput: Object,
  ): Promise<Map<String, any>>;
  /** Requires a map object of the body scan results and returns a Map object. */
  getBodyScanExtra(scanResults: Map<String, any>): Promise<Map<String, any>>;
  /** Returns the SDK status. */
  getMultiScanStatus(): Promise<string>;
  /** Returns a Map containing the SDK details. */
  getMultiScanDetails(): Promise<Map<string, any>>;
  /** Returns the user authorization status of the SDK. */
  getUserAuthorizedState(): Promise<string>;
  /** Will deuathorize the user from the SDK. */
  deauthorizeUser(): Promise<string>;
  /** Released the actively registered SDK session. */
  releaseMultiScanSDK(): Promise<string>;
  /** Use the AHIMultiScan persistence delegate and set historical body scan results */
  setMultiScanPersistenceDelegate(scanResult: Map<String, any>): void;
}
export default MultiScanModule as MultiScanInterface;