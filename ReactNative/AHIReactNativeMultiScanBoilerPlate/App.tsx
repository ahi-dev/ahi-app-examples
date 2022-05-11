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

import React, {useState} from 'react';
import type {ReactNode} from 'react';
import MultiScanModule from './Modules/MultiScanModule';
import {
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

/// The required tokens for the MultiScan Setup and Authorization.
const AHI_MULTI_SCAN_TOKEN = '';
/// Your user id. Hardcode a valid user id for testing purposes.
const AHI_TEST_USER_ID = 'AHI_TEST_USER';
/// Your salt token.
const AHI_TEST_USER_SALT = 'user';
/// Any claims you require passed to the SDK.
const AHI_TEST_USER_CLAIMS = ['test'];
/// Payment type
enum MSPaymentType {
  PAYG = 'PAYG',
  SUBS = 'SUBSCRIBER',
}

const App: () => ReactNode = () => {
  const [isSetup, setIsSetup] = useState(false);
  const [downloadingButtonVisibility, setdownloadingButtonVisibility] =
    useState(true);
  const [resourcesDownloaded, setResourcesDownloaded] = useState(false);
  const [resourcesDownloading, setResourcesDownloading] = useState(false);

  /// Setup the MultiScan SDK
  ///
  /// This must happen before requesting a scan.
  /// We recommend doing this on successfuil load of your application.
  const setupSDK = async () => {
      await MultiScanModule.setupMultiScanSDK(AHI_MULTI_SCAN_TOKEN).then(
        result => {
          if (result !== null) {
            return;
          }
          authorizeUser();
        },
      ).catch((error) {
      console.log('AHI: Error setting up: ' + error);
      console.log('AHI: Confirm you have a valid token.');
    });
  };

  // authorize user
  const authorizeUser = async () => {
    await MultiScanModule.authorizeUser(
      AHI_TEST_USER_ID,
      AHI_TEST_USER_SALT,
      AHI_TEST_USER_CLAIMS,
    ).then((auth) => {
      if (auth !== null) {
        console.log('AHI: Auth Error: ' + auth);
        console.log(
          'AHI: Confirm you are using a valid user id, salt and claims.',
        );
        return;
      }
      setIsSetup(true);
      console.log('AHI: Setup user successfully');
    }).catch((error) => {
      console.log('AHI: Auth Error: ' + error);
      console.log(
      'AHI: Confirm you are using a valid user id, salt and claims.',
      );
    });
  };

   // download resources
  const didTapDownloadResources = async () => {
    MultiScanModule.areAHIResourcesAvailable().then((areAvailable: boolean) => {
      if (!areAvailable) {
        console.log('AHI INFO: Resources are not downloaded');
        // start download.
        MultiScanModule.downloadAHIResources();
        MultiScanModule.checkAHIResourcesDownloadSize().then((size: any) => {
          console.log(
            'AHI INFO: Size of download is ' + Number(size) / 1024 / 1024,
          );
        });
        setTimeout(() => didTapDownloadResources(), 30000);
      } else {
        console.log('AHI: Resources ready');
        // control view state
        setResourcesDownloaded(true);
      }
    });
  };

  // start facescan
  const didTapStartFaceScan = async () => {
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
    MultiScanModule.startFaceScan(userFaceScanInput, MSPaymentType.PAYG)
      .then((faceScanResults: Map<String, any>) => {
        console.log('AHI: SCAN RESULTS: ' + JSON.stringify(faceScanResults));
      })
      .catch(error => {
        console.log('AHI ERROR: Face Scan error: ' + error);
      });
  };

  // start bodyscan
  const didTapStartBodyScan = () => {
    let userBodyScanInput = {
      enum_ent_sex: 'male',
      cm_ent_height: 180,
      kg_ent_weight: 85,
      yr_ent_age: 35,
    };
    var id;
    if (!areBodyScanConfigOptionsValid(objectToMap(userBodyScanInput))) {
      console.log('AHI ERROR: Body Scan inputs invalid.');
    }
    MultiScanModule.startBodyScan(userBodyScanInput, MSPaymentType.PAYG).then(
      (bodyScanResults: Map<String, any>) => {
        console.log('AHI: SCAN RESULTS: ' + JSON.stringify(bodyScanResults));
        var result = JSON.parse(JSON.stringify(bodyScanResults));
        if (areBodyScanSmoothingResultsValid(result)) {
          MultiScanModule.getBodyScanExtras(bodyScanResults).then(
            (path: any) => {
              console.log('AHI 3D Mesh : ' + path['meshURL']);
              console.log('AHI 3D Mesh : ' + JSON.stringify(path));
            },
          );
        }
      },
    ).catch(error => {
      console.log('AHI ERROR: Body Scan error: ' + error);
    });
  };

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
    return (sex != null && height != null && numbers.isValidNumber(height) && weight != null && numbers.isValidNumber(weight) && ['male', 'female'].includes(sex));
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
    return (sex != null &&
      height != null &&
      weight != null &&
      height >= 50 &&
      height <= 255 &&
      weight >= 16 &&
      weight <= 300);
  }

  /** Confirm results have correct set of keys. */
  function areBodyScanSmoothingResultsValid(result: Map<string, any>): boolean {
    // Your token may only provide you access to a smaller subset of results.
    // You should modify this list based on your available config options.
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
    // Iterate over results
    for (let i = 0; i < sdkResultSchema.length; i++) {
      // Check if keys in result contains the required keys.
      if (!(sdkResultSchema[i] in result)) {
        isValid = false;
      }
    }
    return isValid;
  }

  const map = new Map();
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
          {isSetup ? null : (
            <TouchableOpacity onPress={setupSDK} style={styles.button}>
              <Text style={styles.text}>Setup SDK</Text>
            </TouchableOpacity>
          )}
          {isSetup ? (
            <TouchableOpacity
              onPress={didTapStartFaceScan}
              style={styles.button}>
              <Text style={styles.text}>Start Facescan</Text>
            </TouchableOpacity>
          ) : null}
          {isSetup && !resourcesDownloaded ? (
            <Pressable disabled={false}>
              <TouchableOpacity
                onPress={didTapDownloadResources}
                style={styles.button}>
                <Text style={styles.text}>Download Resources</Text>
              </TouchableOpacity>
            </Pressable>
          ) : null}
          {resourcesDownloaded ? (
            <TouchableOpacity
              onPress={didTapStartBodyScan}
              style={styles.button}>
              <Text style={styles.text}>Start BodyScan</Text>
            </TouchableOpacity>
          ) : null}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
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
const SDKButton = ({callbackFunction, buttonText}: any) => {
  return (
    <TouchableOpacity onPress={callbackFunction} style={styles.button}>
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
        typeof value === "number" &&
        minimumValue > 0
      );
    }
    return value !== null && value !== undefined && typeof value === "number";
  };
}