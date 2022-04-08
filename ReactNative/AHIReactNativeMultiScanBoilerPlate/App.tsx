/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, {useState} from 'react';
import type {ReactNode} from 'react';
import MultiScanModule from './Modules/MultiScanModule';
import {
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

const App: () => ReactNode = () => {
  const [isSetup, setIsSetup] = useState(false);
  const [resourcesDownloaded, setResourcesDownloaded] = useState(false);
  const [resourcesDownloading, setResourcesDownloading] = useState(false);

  const didTapSetup = async () => {
    console.log('didTapSetup: ');
    try {
      let res = await MultiScanModule.setupMultiScanSDK(AHI_MULTI_SCAN_TOKEN);
      console.log('This is the result: ', res);
    } catch (e) {
      console.log(e);
    }
  };

  const didTapAuthorizeUser = async () => {
    console.log('didTapAuthorizeUser: ');
    console.log(AHI_TEST_USER_ID, AHI_TEST_USER_SALT, AHI_TEST_USER_CLAIMS);
    try {
      let res = await MultiScanModule.authorizeUser(
        AHI_TEST_USER_ID,
        AHI_TEST_USER_SALT,
        AHI_TEST_USER_CLAIMS,
      );
      if (res === 'success') {
        setIsSetup(true);
      }
      console.log(res);
    } catch (e) {
      console.log(e);
    }
  };

  const didTapCheckDownloadSize = () => {
    console.log('didTapCheckDownloadSize: ');
    MultiScanModule.checkAHIResourcesDownloadSize()
      .then(value => {
        console.log('Download resources progress: ', value);
      })
      .catch((e: {message: any; code: any}) => console.log(e.message, e.code));
  };

  // Potentially redundant method? - can just call didTapAuthorizeUser again for userAuth status
  const checkIfUserAuthorized = () => {
    console.log('checkIfUserAuthorized: ');
    MultiScanModule.isUserAuthorized()
      .then((value: string) => {
        console.log('User is authenticated ', value);
        if (value === 'success') {
          setIsSetup(true);
        }
      })
      .catch((e: {message: any; code: any}) => console.log(e.message, e.code));
  };

  const didTapDownloadResources = async () => {
    console.log('didTapDownloadResources: ');
    try {
      let value = await MultiScanModule.downloadAHIResources();
      console.log('Download resources started: ', value);
      if (value === 'success') {
        // Check if the resources are available
        await MultiScanModule.areAHIResourcesAvailable();
        let downloadSize =
          await MultiScanModule.checkAHIResourcesDownloadSize();
        console.log('Resources donwload size: ', downloadSize);
        // Poll for resources download complete and assign a boolean based on result
        let downloadComplete = await monitorDownloadProgress();
        if (downloadComplete) {
          console.log('Download resources complete! Lets rock n roll');
          setResourcesDownloaded(true);
        }
      }
    } catch (e) {
      console.log('Error occured: ', e);
    }
  };

  const monitorDownloadProgress = async () => {
    console.log('monitorDownloadProgress function call!!');
    // Check download progress every 5 seconds
    var flag = false;
    if (flag == false) {
      console.log('Checking resources');
      let result = await MultiScanModule.areAHIResourcesAvailable();
      if (result != 'success') {
        setTimeout(() => {
          monitorDownloadProgress();
        }, 3000);
      }
      console.log('Result success!!');
      flag = true;
      return;
    }
    return flag;
  };

  const didTapCheckResourcesAvailable = async () => {
    console.log('didTapCheckResourcesAvailable: ');
    MultiScanModule.areAHIResourcesAvailable()
      .then(value => {
        console.log('Download resources progress: ', value);
      })
      .catch((e: {message: any; code: any}) => console.log(e.message, e.code));
  };

  return (
    <SafeAreaView>
      <ScrollView>
        <View>
          <TouchableOpacity onPress={didTapSetup} style={styles.button}>
            <Text style={styles.text}>Setup SDK</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={didTapAuthorizeUser} style={styles.button}>
            <Text style={styles.text}>Authorize User</Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={checkIfUserAuthorized}
            style={styles.button}>
            <Text style={styles.text}>Check User Authenticated</Text>
          </TouchableOpacity>
          {isSetup ? (
            <>
              <SDKButton
                callbackFunction={checkIfUserAuthorized}
                buttonText={'Start FaceScan'}
              />
              <SDKButton
                callbackFunction={didTapCheckDownloadSize}
                buttonText={'Check Download Size'}
              />
              <TouchableOpacity
                onPress={didTapDownloadResources}
                style={styles.button}
                disabled={resourcesDownloading}>
                <Text style={styles.text}>Download Resources</Text>
              </TouchableOpacity>
              <SDKButton
                callbackFunction={didTapCheckResourcesAvailable}
                buttonText={'Check progress'}
              />
            </>
          ) : null}
          {resourcesDownloading ? <Text>Resources Loading</Text> : null}
          {resourcesDownloaded ? (
            <SDKButton
              callbackFunction={didTapCheckResourcesAvailable}
              buttonText={'Start BodyScan'}
            />
          ) : null}
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

const SDKButton = ({callbackFunction, buttonText}: any) => {
  return (
    <TouchableOpacity onPress={callbackFunction} style={styles.button}>
      <Text style={styles.text}>{buttonText}</Text>
    </TouchableOpacity>
  );
};
