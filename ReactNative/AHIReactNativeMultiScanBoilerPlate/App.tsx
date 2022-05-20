/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React from 'react';
import type {ReactNode} from 'react';
import MultiScanModule from './Modules/MultiScanModule';
import {
  SafeAreaView,
  ScrollView,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

const onPress = () => {
  console.log('Button pressed');
  console.log(MultiScanModule);
  console.log(MultiScanModule.doNothing());
  console.log(MultiScanModule.doSomething());
};

const App: () => ReactNode = () => {
  return (
    <SafeAreaView>
      <ScrollView>
        <View>
          <Text>Click button below....</Text>
          <TouchableOpacity onPress={onPress}>
            <Text>Cliccckkkkkk</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

export default App;
