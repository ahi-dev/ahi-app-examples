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

import React, { useState } from 'react';
import type { ReactNode } from 'react';
import MultiScanModule from './Modules/MultiScanModule';
import {
  Button,
  PermissionsAndroid,
  StatusBar,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  DeviceEventEmitter,
} from 'react-native';

/** The required tokens for the MultiScan Setup and Authorization. */
/** Your AHI MultiScan token */
const AHI_MULTI_SCAN_TOKEN = '';
/** Your user ID. NOTE: User ID is hard-coded here for example, BUT should NOT be hard-coded in real integration (user ID from idP is expected). */
const AHI_TEST_USER_ID = 'EXAMPLE_USER_ID';
/** Security salt value. This should be hard-coded into your app, and SHOULD NOT be changed (i.e. be the same in both iOS and Android). It can be any string value. */
const AHI_TEST_USER_SALT = 'EXAMPLE_APP_SALT';
/** Claims are optional values to increase the security for the user. The order and values should be unique for a given user and be the same on both iOS and Android (e.g. user join date in the format "yyyy", "mm", "dd", "zzzz"). */

const AHI_TEST_USER_CLAIMS = ['EXAMPLE_CLAIM'];

const App: () => ReactNode = () => {
  const [resourcesDownloaded, setResourcesDownloaded] = useState(false);
  const [resourcesDownloading, setResourcesDownloading] = useState(false);
  const [isSDKSetup, setIsSDKSetup] = useState(false);

  function didTapSetup() {
    setupMultiScanSDK();
  }

  function didTapStartFaceScan() {
    startFaceScan();
  }

  function didTapStartFingerScan() {
    startFingerScan();
  }

  function didTapStartBodyScan() {
    startBodyScan();
  }

  function didTapCheckDownloadSize() {
    checkAHIResourcesDownloadSize();
  }

  function didTapDownloadResources() {
    areAHIResourcesAvailable();
    checkAHIResourcesDownloadSize();
  }

  requestCameraPermission();

  /**
   * Setup the MultiScan SDK
   *
   * This must happen before requesting a scan.
   * We recommend doing this on successfuil load of your application.
   */
  const setupMultiScanSDK = async () => {
    await MultiScanModule.setupMultiScanSDK(AHI_MULTI_SCAN_TOKEN)
      .then((result: any) => {
        console.log(result);
        if (result !== '') {
          return;
        }
        authorizeUser();
      })
      .catch(error => {
        console.log('AHI: Error setting up: ' + error);
        console.log('AHI: Confirm you have a valid token.');
      });
  };

  /**
   * Once successfully setup, you should authorize your user with our service.
   *
   * With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
   */
  const authorizeUser = async () => {
    await MultiScanModule.authorizeUser(
      AHI_TEST_USER_ID,
      AHI_TEST_USER_SALT,
      AHI_TEST_USER_CLAIMS,
    )
      .then(auth => {
        if (auth !== '') {
          console.log('AHI: Auth Error: ' + auth);
          console.log(
            'AHI: Confirm you are using a valid user id, salt and claims.',
          );
          return;
        }
        setIsSDKSetup(true);
        console.log('AHI: Setup user successfully');
      })
      .catch(error => {
        console.log('AHI: Auth Error: ' + error);
        console.log(
          'AHI: Confirm you are using a valid user id, salt and claims.',
        );
      });
  };

  /**
   * Check if the AHI resources are downloaded.
   *
   * We have remote resources that exceed 100MB that enable our scans to work.
   * You are required to download them inorder to obtain a body scan.
   */
  const areAHIResourcesAvailable = async () => {
    MultiScanModule.areAHIResourcesAvailable().then((areAvailable: boolean) => {
      if (!areAvailable) {
        console.log('AHI INFO: Resources are not downloaded');
        // start download.
        if (resourcesDownloading != true) {
          getResourcesDownloadProgressReport();
          downloadAHIResources();
        }
        setResourcesDownloading(true);
      } else {
        console.log('AHI: Resources ready');
        setResourcesDownloaded(true);
      }
    });
  };

  const getResourcesDownloadProgressReport = () => {
    DeviceEventEmitter.addListener('progress_report', (value) => {
      if (value == "done") {
        setResourcesDownloaded(true);
        setResourcesDownloading(false);
        console.log('AHI INFO: Download Finished')
      } else if (value == "failed") {
        console.log('AHI INFO: Download Failed.')
      } else {
        console.log('AHI INFO: Size of Download is ' +
          (Number(value["progress"]) / 1024 / 1024).toFixed(1) + ' / ' +
          (Number(value["total"]) / 1024 / 1024).toFixed(1)
        );
      }
    }
    );
    MultiScanModule.getResourcesDownloadProgressReport();
  };


  /**
   * Download scan resources.
   *
   * We recomment only calling this function once per session to prevent duplicate background resource calls.
   */
  function downloadAHIResources() {
    MultiScanModule.downloadAHIResources();
  }

  /**
   * Check the size of the AHI resources that require downloading.
   */
  function checkAHIResourcesDownloadSize() {
    MultiScanModule.checkAHIResourcesDownloadSize().then((size: any) => {
      console.log(
        'AHI INFO: Size of download is ' + Number(size) / 1024 / 1024,
        // 'AHI INFO: Size of download is ' + size,
      );
    });
  }

  const startFaceScan = async () => {
    let userFaceScanInput = {
      enum_ent_sex: 'male',
      cm_ent_height: 180,
      kg_ent_weight: 85,
      yr_ent_age: 35,
      bool_ent_smoker: false,
      bool_ent_hypertension: false,
      bool_ent_bloodPressureMedication: false,
      enum_ent_diabetic: 'none',
    };
    if (!areFaceScanConfigOptionsValid(objectToMap(userFaceScanInput))) {
      console.log('AHI ERROR: Face Scan inputs');
      return;
    }
    MultiScanModule.startFaceScan(userFaceScanInput)
      .then((faceScanResults: Map<String, any>) => {
        console.log('AHI: SCAN RESULTS: ' + JSON.stringify(faceScanResults));
      })
      .catch(error => {
        console.log('AHI ERROR: Face Scan error: ' + error);
      });
  };

  const startFingerScan = async () => {
    let userFingerScanInput = {
      sec_ent_scanLength: 60,
      str_ent_instruction1: "Instruction 1",
      str_ent_instruction2: "Instruction 2",
    };
    if (!areFingerScanConfigOptionsValid(objectToMap(userFingerScanInput))) {
      console.log('AHI ERROR: Finger Scan inputs');
      return;
    }

    MultiScanModule.startFingerScan(userFingerScanInput)
      .then((fingerScanResults: Map<String, any>) => {
        console.log('AHI: SCAN RESULTS: ' + JSON.stringify(fingerScanResults));
      })
      .catch(error => {
        console.log('AHI ERROR: Face Scan error: ' + error);
      });

  };

  const startBodyScan = () => {
    let userBodyScanInput = {
      enum_ent_sex: 'male',
      cm_ent_height: 180,
      kg_ent_weight: 85,
      yr_ent_age: 35,
    };
    if (!areBodyScanConfigOptionsValid(objectToMap(userBodyScanInput))) {
      console.log('AHI ERROR: Body Scan inputs invalid.');
      return;
    }
    MultiScanModule.startBodyScan(userBodyScanInput)
      .then((bodyScanResult: Map<String, any>) => {
        console.log('AHI: SCAN RESULTS: ' + JSON.stringify(bodyScanResult));
        getBodyScanExtra(bodyScanResult);
      })
      .catch(error => {
        console.log('AHI ERROR: Body Scan error: ' + error);
      });
  };

  /**
   * Use this function to fetch the 3D avatar mesh.
   *
   * The 3D mesh can be created and returned at any time.
   * We recommend doing this on successful completion of a body scan with the results.
   */
  function getBodyScanExtra(bodyScanResult: Map<String, any>) {
    if (bodyScanResult == null) {
      console.log('AHI ERROR: Body scan results must not be null.');
      return;
    }
    // Check bodyScanResult has contains output schema.
    if (!areBodyScanSmoothingResultsValid(bodyScanResult)) {
      console.log('AHI ERROR: Body scan results not valid for extras.');
      return;
    }
    // Set bodyScanResult to MultiScanPersistenceDelagate.
    setMultiScanPersistenceDelegate(bodyScanResult)
    // Get body scan extra.
    var result = JSON.parse(JSON.stringify(bodyScanResult));
    MultiScanModule.getBodyScanExtra(result).then((path: any) => {
      console.log('AHI 3D Mesh : ' + path['meshURL']);
      console.log('AHI 3D Mesh : ' + JSON.stringify(path));
    });
  }

  /**
   * Check if MultiScan is on or offline.
   */
  function getMultiScanStatus() {
    MultiScanModule.getMultiScanStatus().then(status =>
      console.log('AHI INFO: Status: ', status),
    );
  }

  /**
   * Check your AHI MultiScan organisation  details.
   */
  function getMultiScanDetails() {
    MultiScanModule.getMultiScanDetails()
      .then(details => {
        console.log('AHI INFO: MultiScan details: ', details);
      })
      .catch(error => {
        console.log('AHI ERROR: getMultiScanDetails ', error);
      });
  }

  /**
   * Check if the user is authorized to use the MuiltScan service.
   */
  function getUserAuthorizedState() {
    MultiScanModule.getUserAuthorizedState()
      .then(isAuthorized => {
        console.log(
          'AHI INFO: User is ',
          isAuthorized ? 'authorized' : 'not authorized',
        );
      })
      .catch(error => {
        console.log('AHI ERROR: getUserAuthorizedState ', error);
      });
  }

  /**
   * Deauthorize the user.
   */
  function deauthorizeUser() {
    MultiScanModule.deauthorizeUser()
      .then(deAuthorizeResult => {
        if (deAuthorizeResult !== '') {
          console.log(
            'AHI ERROR: Failed to deuathorize user with error: ',
            deAuthorizeResult,
          );
        } else {
          console.log('AHI INFO: User is deauthorized.');
        }
      })
      .catch(error => {
        console.log('AHI ERROR: deAuthorizeUser ', error);
      });
  }

  /**
   * Release the MultiScan SDK session.
   *
   * If you  use this, you will need to call setupSDK again.
   * The Android MultiScan SDK does not yet have this functionality implemented. This will be available in future versions of the AHIMultiScan module.
   */
  function releaseMultiScanSDK() {
    MultiScanModule.releaseMultiScanSDK().then(releaseSDKResult => {
      if (releaseSDKResult !== '') {
        console.log(
          'AHI ERROR: Failed to release SDK with error: ',
          releaseSDKResult,
        );
        return;
      }
      console.log('AHI INFO: SDK has been released successfully.');
    });
  }

  /**
   * If you choose to use this, you will obtain two sets of results - one containing the "raw" output and another set containing "adj" output.
   * "adj" means adjusted and is used to help provide historical results as a reference for the newest result to provide results tailored to the user.
   * We recommend using this for individual users results; avoid using this if the app is a single user ID with multiple users results.
   * More info found here: https://docs.advancedhumanimaging.io/MultiScan%20SDK/Data/
   */
  function setMultiScanPersistenceDelegate(scanResult: Map<String, any>) {
    /* Each result requires: 
     * - _ent_ values 
     * - _raw_ values 
     * - id value 
     * - date value 
     * The persistence delegate will still work with your result provided you add here to the validation check. 
     */
    if (!areBodyScanSmoothingResultsValid(scanResult)) {
      console.log(
        'AHI WARN: Results are not valid for the persistence delegate. Please compare your results against the schema for more information.',
      );
      return;
    }
    MultiScanModule.setMultiScanPersistenceDelegate(scanResult);
  }

  /**
   * All MultiScan scan configs require this information.
   *
   * BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/ FaceScan:
   * https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
   */
  function areSharedScanConfigOptionsValid(
    inputValues: Map<string, any>,
  ): boolean {
    var sex = inputValues.get('enum_ent_sex');
    var height = inputValues.get('cm_ent_height');
    var weight = inputValues.get('kg_ent_weight');
    var numbers = new AHINumbers();
    return (
      sex != null &&
      height != null &&
      numbers.isValidNumber(height) &&
      weight != null &&
      numbers.isValidNumber(weight) &&
      ['male', 'female'].includes(sex)
    );
  }

  /**
   * FaceScan config requirements validation. Please see the Schemas for more information:
   * FaceScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
   */
  function areFaceScanConfigOptionsValid(
    inputValues: Map<string, any>,
  ): boolean {
    if (!areSharedScanConfigOptionsValid(inputValues)) {
      return false;
    }
    var sex = inputValues.get('enum_ent_sex');
    var smoke = inputValues.get('bool_ent_smoker');
    var isDiabetic = inputValues.get('enum_ent_diabetic');
    var hypertension = inputValues.get('bool_ent_hypertension');
    var blood = inputValues.get('bool_ent_bloodPressureMedication');
    var height = inputValues.get('cm_ent_height');
    var weight = inputValues.get('kg_ent_weight');
    var age = inputValues.get('yr_ent_age');
    return (
      sex != null &&
      smoke != null &&
      isDiabetic != null &&
      hypertension != null &&
      blood != null &&
      height != null &&
      weight != null &&
      age != null &&
      height >= 25 &&
      height <= 300 &&
      weight >= 25 &&
      weight <= 300 &&
      ['none', 'type1', 'type2'].includes(isDiabetic)
    );
  }

  /**
   * FingerScan config requirements validation. Please see the Schemas for more information:
   * FingerScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FingerScan/Schemas/
   */
  function areFingerScanConfigOptionsValid(
    inputValues: Map<string, any>,
  ): boolean {
    var scanLength = inputValues.get('sec_ent_scanLength');
    var instruction1 = inputValues.get('str_ent_instruction1');
    var instruction2 = inputValues.get('str_ent_instruction2');
    return (
      scanLength != null &&
      instruction1 != null &&
      instruction2 != null &&
      scanLength >= 20
    );
  }

  /**
   * BodyScan config requirements validation. Please see the Schemas for more information:
   * BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/
   */
  function areBodyScanConfigOptionsValid(
    inputValues: Map<string, any>,
  ): boolean {
    if (!areSharedScanConfigOptionsValid(inputValues)) {
      return false;
    }
    var sex = inputValues.get('enum_ent_sex');
    var height = inputValues.get('cm_ent_height');
    var weight = inputValues.get('kg_ent_weight');
    return (
      sex != null &&
      height != null &&
      weight != null &&
      height >= 50 &&
      height <= 255 &&
      weight >= 16 &&
      weight <= 300
    );
  }

  /** Confirm results have correct set of keys. */
  function areBodyScanSmoothingResultsValid(
    result: Map<String, any>,
  ): boolean {
    /* Your token may only provide you access to a smaller subset of results. */
    /* You should modify this list based on your available config options. */
    var sdkResultSchema = [
      'enum_ent_sex',
      'cm_ent_height',
      'kg_ent_weight',
      'cm_raw_chest',
      'cm_raw_hips',
      'cm_raw_inseam',
      'cm_raw_thigh',
      'cm_raw_waist',
      'kg_raw_weightPredict',
      'percent_raw_bodyFat',
      'id',
      'date',
    ];
    var isValid = true;
    for (var key of sdkResultSchema) {
      /* Check if keys in result contains the required keys. */
      if (!result.hasOwnProperty(key)) {
        isValid = false;
      }
    }
    return isValid;
  }

  const objectToMap = (scanResult: any) => {
    const keys = Object.keys(scanResult);
    const map = new Map();
    for (let i = 0; i < keys.length; i++) {
      map.set(keys[i], scanResult[keys[i]]);
    }
    return map;
  };

  return (
    <SafeAreaView>
      <ScrollView>
        <View>
          {!isSDKSetup ? (
            <DefaultButton action={didTapSetup} buttonText={'Setup SDK'} />
          ) : (
            <>
              <DefaultButton
                action={didTapStartFaceScan}
                buttonText={'Start FaceScan'}
              />
              <DefaultButton
                action={didTapStartFingerScan}
                buttonText={'Start FingerScan'}
              />
              {resourcesDownloaded ? (
                <>
                  <DefaultButton
                    action={didTapStartBodyScan}
                    buttonText={'Start BodyScan'}
                  />
                </>
              ) : (
                <Pressable disabled={resourcesDownloading}>
                  <DefaultButton
                    action={didTapDownloadResources}
                    buttonText={'Download Resources'}
                  />
                </Pressable>
              )}
            </>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const requestCameraPermission = async () => {
  try {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.CAMERA,
      {
        title: "Camera Permission",
        message:
          "All scans need to granted permission",
        buttonNeutral: "Ask Me Later",
        buttonNegative: "Cancel",
        buttonPositive: "OK"
      }
    );
    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      console.log("You can use the camera");
    } else {
      console.log("Camera permission denied");
    }
  } catch (err) {
    console.warn(err);
  }
};

const styles = StyleSheet.create({
  button: {
    flex: 1,
    padding: 8,
    marginHorizontal: 16,
    marginTop: 8,
    backgroundColor: 'black',
    alignSelf: 'stretch',
  },
  text: {
    textAlign: 'center',
    color: 'white',
  },
});

export default App;

const DefaultButton = ({ action, buttonText }: any) => {
  return (
    <TouchableOpacity onPress={action} style={styles.button}>
      <Text style={styles.text}>{buttonText}</Text>
    </TouchableOpacity>
  );
};

export class AHINumbers {
  public isValidNumber = (value: number, minimumValue?: number) => {
    if (minimumValue !== undefined && minimumValue !== null) {
      return (
        value !== null &&
        value !== undefined &&
        typeof value === 'number' &&
        minimumValue > 0
      );
    }
    return value !== null && value !== undefined && typeof value === 'number';
  };
}