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
  PAYG = "PAYG",
  SUBS = "SUBS"
}

let avatarValues = {
  'TAG_ARG_GENDER': 'M',
  'TAG_ARG_SMOKER': 'F',
  'TAG_ARG_DIABETIC': 'none',
  'TAG_ARG_HYPERTENSION': 'F',
  'TAG_ARG_BPMEDS': 'F',
  'TAG_ARG_HEIGHT_IN_CM': 180,
  'TAG_ARG_WEIGHT_IN_KG': 85,
  'TAG_ARG_AGE': 35,
  'TAG_ARG_PREFERRED_HEIGHT_UNITS': 'CENTIMETRES',
  'TAG_ARG_PREFERRED_WEIGHT_UNITS': 'KILOGRAMS'
}

const App: () => ReactNode = () => {
  const [isSetup, setIsSetup] = useState(false);
  const [downloadingButtonVisibility, setdownloadingButtonVisibility] = useState(true);
  const [resourcesDownloaded, setResourcesDownloaded] = useState(false);
  const [resourcesDownloading, setResourcesDownloading] = useState(false);
  const map = new Map();
  const objectToMap = (avatarValues: any) => {
    const keys = Object.keys(avatarValues);
    const map = new Map();
    for (let i = 0; i < keys.length; i++) {
      map.set(keys[i], avatarValues[keys[i]]);
    };
    return map
  };

  // setup sdk
  const didTapSetup = async () => {
    try {
      let res = await MultiScanModule.setupMultiScanSDK(AHI_MULTI_SCAN_TOKEN);
      if (res !== 'SUCCESS') {
        return;
      }
      let auth = await MultiScanModule.authorizeUser(
        AHI_TEST_USER_ID,
        AHI_TEST_USER_SALT,
        AHI_TEST_USER_CLAIMS
      );
      if (auth !== 'SUCCESS') {
        console.log("AHI: Auth Error: " + auth);
        console.log("AHI: Confirm you are using a valid user id, salt and claims.");
        return;
      }
      setIsSetup(true);
      console.log("AHI: Setup user successfully");
    } catch (e) {
      console.log(e);
    }
  };

  // start facescan
  const didTapStartFaceScan = async () => {
    if (!areFaceScanConfigOptionsValid(objectToMap(avatarValues))) {
      console.log("AHI ERROR: Face Scan inputs")
      return;
    }
    MultiScanModule.startFaceScan(MSPaymentType.PAYG, avatarValues).then((value: any) => {
      console.log("AHI: SCAN RESULTS: " + value);
    });
  }

  // start bodyscan
  const didTapStartBodyScan = () => {
    var id;
    if (!areBodyScanConfigOptionsValid(objectToMap(avatarValues))) {
      console.log("AHI ERROR: Body Scan inputs invalid.");
    }
    MultiScanModule.startBodyScan(MSPaymentType.PAYG, avatarValues).then((value: any) => {
      console.log("AHI: SCAN RESULTS: " + value);
      var result = JSON.parse(value);
      if (areBodyScanSmoothingResultsValid(result)) {
        id = result['id'];
        MultiScanModule.getBodyScanExtras(id).then((path: any) => {
          console.log("AHI 3D Mesh path: " + path);
        });
      }
    });

  }

  // download resources
  const didTapDownloadResources = async () => {
    MultiScanModule.areAHIResourcesAvailable().then((value: any) => {
      if (!value) {
        console.log("AHI INFO: Resources are not downloaded");
        // start download.
        MultiScanModule.downloadAHIResources();
        MultiScanModule.checkAHIResourcesDownloadSize()
          .then((size: any) => {
            console.log("AHI INFO: Size of download is " + Number(size) / 1024 / 1024);
          });
        setTimeout(() => didTapDownloadResources(), 30000);
      } else {
        console.log("AHI: Resources ready");
        // control view state
        setResourcesDownloaded(true);
      }
    });
  };

  /**
   * FaceScan config requirements validation. Please see the Schemas for more information:
   * FaceScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
   */
  function areFaceScanConfigOptionsValid(avatarValues: Map<string, any>): boolean {
    if (!areSharedScanConfigOptionsValid(avatarValues)) {
      return false;
    }
    var sex = avatarValues.get('TAG_ARG_GENDER');
    var smoke = avatarValues.get('TAG_ARG_SMOKER');
    var isDiabetic = avatarValues.get('TAG_ARG_DIABETIC');
    var hypertension = avatarValues.get('TAG_ARG_HYPERTENSION');
    var blood = avatarValues.get('TAG_ARG_BPMEDS');
    var height = avatarValues.get('TAG_ARG_HEIGHT_IN_CM');
    var weight = avatarValues.get('TAG_ARG_WEIGHT_IN_KG');
    var age = avatarValues.get('TAG_ARG_AGE');
    var heightUnits = avatarValues.get('TAG_ARG_PREFERRED_HEIGHT_UNITS');
    var weightUnits = avatarValues.get('TAG_ARG_PREFERRED_WEIGHT_UNITS');
    if (sex != null &&
      smoke != null &&
      isDiabetic != null &&
      hypertension != null &&
      blood != null &&
      height != null &&
      weight != null &&
      age != null &&
      heightUnits != null &&
      weightUnits != null
    ) {
      return ['none', 'type1', 'type2'].includes(isDiabetic);
    } else {
      return false;
    }
  }

  /**
  * BodyScan config requirements validation. Please see the Schemas for more information:
  * BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/
  */
  function areBodyScanConfigOptionsValid(avatarValues: Map<string, any>): boolean {
    if (!areSharedScanConfigOptionsValid(avatarValues)) {
      return false
    }
    var sex = avatarValues.get('TAG_ARG_GENDER');
    var height = avatarValues.get('TAG_ARG_HEIGHT_IN_CM');
    var weight = avatarValues.get('TAG_ARG_WEIGHT_IN_KG');
    if (sex != null &&
      height != null &&
      weight != null &&
      height >= 50 &&
      height <= 255 &&
      weight >= 16 &&
      weight <= 300
    ) {
      return true
    }
    return false
  }

  /** Confirm results have correct set of keys. */
  function areBodyScanSmoothingResultsValid(result: Map<string, any>): boolean {
    // Your token may only provide you access to a smaller subset of results.
    // You should modify this list based on your available config options.
    var sdkResultSchema = [
      "enum_ent_sex",
      "cm_ent_height",
      "kg_ent_weight",
      "cm_raw_chest",
      "cm_raw_hips",
      "cm_raw_inseam",
      "cm_raw_thigh",
      "cm_raw_waist",
      "kg_raw_weightPredict",
      "percent_raw_bodyFat",
      "id",
      "date"];
    var isValid = false;
    // Iterate over results
    for (let i = 0; i < sdkResultSchema.length; i++) {
      // Check if keys in result contains the required keys.
      if (!(sdkResultSchema[i] in result)) {
        isValid = true;
      }
    }
    return !isValid;
  }

  /**
  * All MultiScan scan configs require this information.
  *
  * BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/ FaceScan:
  * https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
  */
  function areSharedScanConfigOptionsValid(avatarValues: Map<string, any>): boolean {
    var sex = avatarValues.get('TAG_ARG_GENDER');
    var height = avatarValues.get('TAG_ARG_HEIGHT_IN_CM');
    var weight = avatarValues.get('TAG_ARG_WEIGHT_IN_KG');
    if (sex != null && height != null && weight != null) {
      return ['M', 'F'].includes(sex);
    }
    return false;
  }

  return (
    <SafeAreaView>
      <ScrollView>
        <View>
          {
            isSetup ? null : <TouchableOpacity onPress={didTapSetup} style={styles.button} >
              <Text style={styles.text}>Setup SDK</Text>
            </TouchableOpacity>
          }
          {
            isSetup ? <TouchableOpacity onPress={didTapStartFaceScan} style={styles.button}>
              <Text style={styles.text}>Start Facescan</Text>
            </TouchableOpacity> : null
          }
          {
            isSetup && !resourcesDownloaded ? <TouchableOpacity onPress={didTapDownloadResources} style={styles.button}>
              <Text style={styles.text}>Download Resources</Text>
            </TouchableOpacity> : null
          }
          {
            resourcesDownloaded ? <TouchableOpacity onPress={didTapStartBodyScan} style={styles.button}>
              <Text style={styles.text}>Start BodyScan</Text>
            </TouchableOpacity>
              : null
          }
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
const SDKButton = ({ callbackFunction, buttonText }: any) => {
  return (
    <TouchableOpacity onPress={callbackFunction} style={styles.button}>
      <Text style={styles.text}>{buttonText}</Text>
    </TouchableOpacity>
  );
};
