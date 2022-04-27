/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

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
// !!!! DEBUG - DO NOT COMMIT!!!!!!!
const AHI_MULTI_SCAN_TOKEN = '';
// !!!! DEBUG - DO NOT COMMIT!!!!!!!
/// Your user id. Hardcode a valid user id for testing purposes.
const AHI_TEST_USER_ID = 'AHI_TEST_USER';
/// Your salt token.
const AHI_TEST_USER_SALT = 'user';
/// Any claims you require passed to the SDK.
const AHI_TEST_USER_CLAIMS = ['test'];

const App: () => ReactNode = () => {
  const [isSetup, setIsSetup] = useState(false);
  const [resourcesDownloaded, setResourcesDownloaded] = useState(false);
  const [resourcesDownloading, setResourcesDownloading] = useState(false);

  // setup sdk
  const didTapSetup = async () => {
    console.log('didTapSetup: ');
    try {
      let res = await MultiScanModule.setupMultiScanSDK(AHI_MULTI_SCAN_TOKEN);
      if (res === 'SUCCESS') {
        let auth = await MultiScanModule.authorizeUser(
          AHI_TEST_USER_ID,
          AHI_TEST_USER_SALT,
          AHI_TEST_USER_CLAIMS,
        );
        if (auth === 'SUCCESS') {
          setIsSetup(true);
          console.log("AHI: Setup user successfully");
        } else {
          console.log("AHI: Auth Error: " + auth);
          console.log("AHI: Confirm you are using a valid user id, salt and claims.");
        }
      } else {
        console.log(res);
      }
    } catch (e) {
      console.log(e);
    }
  };

  // start facescan
  const didTapStartFaceScan = () => {
    console.log('didTapCheckDownloadSize: ');
    MultiScanModule.startFaceScan()
      .then(value => {
        console.log("AHI: SCAN RESULT: " + value);
      });
  }

  // download resources
  const didTapDownloadResources = () => {
    console.log('didTapCheckDownloadSize: ');
    MultiScanModule.downloadAHIResources();
    MultiScanModule.areAHIResourcesAvailable().then(value => {
      console.log(value);
    });
    MultiScanModule.checkAHIResourcesDownloadSize()
      .then(value => {
        console.log(value);
        setResourcesDownloaded(true);
      });
  };

  // start bodyscan
  const didTapStartBodyScan = () => {
    console.log('didTapStartBodyScan: ');
    MultiScanModule.startBodyScan().then(value => {
      console.log(value);
    });
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
    padding: 10,
    marginHorizontal: 15,
    marginTop: 10,
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
