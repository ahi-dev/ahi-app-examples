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

interface MultiScanInterface {
  // SDK
  checkAHIResourcesDownloadSize(): Promise<string>;
  areAHIResourcesAvailable(): Promise<boolean>;
  downloadAHIResources(): void;
  setupMultiScanSDK(token: string): Promise<string>;
  authorizeUser(
    userID: string,
    aSalt: string,
    aClaims: string[],
  ): Promise<string>;
  startFaceScan(msPaymentType: string, avatarValues: Object): Promise<string>;
  startBodyScan(msPaymentType: string, avatarValues: Object): Promise<string>;
  getBodyScaExtra(id: string): Promise<string>;
  setPersistenceDelegate(result: Array<any>): void;
}
export default MultiScanModule as MultiScanInterface;