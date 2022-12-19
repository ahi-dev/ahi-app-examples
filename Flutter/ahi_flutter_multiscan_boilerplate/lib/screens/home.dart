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

import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class Home extends StatefulWidget {
  const Home({Key? key}) : super(key: key);

  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> {
  // Display UI buttons based on SDK state
  bool _isSDKSetup = false;
  bool _isUserAuthorized = false;
  bool _resourcesAreAvailable = false;
  bool _downloadResourcesButtonEnabled = true;
  // Communicate with native layer
  final platform = const MethodChannel('ahi_multiscan_flutter_wrapper');
  final event = const EventChannel('ahi_multiscan_flutter_event_channel');

  /// The required tokens for the MultiScan Setup and Authorization.

  /// Your user ID. NOTE: User ID is hard-coded here for example, BUT should NOT be hard-coded in real integration (user ID from idP is expected).
  final String AHI_TEST_USER_ID = "EXAMPLE_USER_ID";

  /// Security salt value. This should be hard-coded into your app, and SHOULD NOT be changed (i.e. be the same in both iOS and Android). It can be any string value.
  final String AHI_TEST_USER_SALT = "EXAMPLE_APP_SALT";

  /// Claims are optional values to increase the security for the user. The order and values should be unique for a given user and be the same on both iOS and Android (e.g. user join date in the format "yyyy", "mm", "dd", "zzzz").
  final List<String> AHI_TEST_USER_CLAIMS = ["EXAMPLE_CLAIM"];

  /// Your AHI MultiScan token
  final String AHI_MULTI_SCAN_TOKEN = "";

  didTapSetup() {
    setupMultiScanSDK();
  }

  didTapStartFaceScan() {
    startFaceScan();
  }

  didTapStartFingerScan() {
    startFingerScan();
  }

  didTapStartBodyScan() {
    startBodyScan();
  }

  didTapCheckDownloadSize() {
    checkAHIResourcesDownloadSize();
  }

  didTapDownloadResources() {
    areAHIResourcesAvailable();

    /// register download progress report's event observer
    event.receiveBroadcastStream().listen(getResourceDownloadProgressReport);
    downloadAHIResources();
  }

  void getResourceDownloadProgressReport(dynamic report) {
    if (report.toString().contains('done')) {
      setState(() {
        _downloadResourcesButtonEnabled = false;
        _resourcesAreAvailable = true;
      });
      print("AHI INFO: Download Finished");
    } else if (report.toString().contains('failed')) {
      print("AHI INFO: Download Failed");
    } else {
      final progressReport = report.toString().split(':');
      final progress = double.parse(progressReport[0]) / 1024 / 1024;
      final total = double.parse(progressReport[1]) / 1024 / 1024;
      print(
          "AHI INFO: Size of Download is ${progress.toStringAsFixed(1)} / ${total.toStringAsFixed(1)}");
    }
  }

  /// Setup the MultiScan SDK
  ///
  /// This must happen before requesting a scan.
  /// We recommend doing this on successful load of your application.
  void setupMultiScanSDK() async {
    try {
      await platform.invokeMethod("setupMultiScanSDK", AHI_MULTI_SCAN_TOKEN);
      setState(() {
        _isSDKSetup = true;
      });
      authorizeUser();
    } on PlatformException catch (error) {
      print("AHI: Error setting up: $error");
      print("AHI: Confirm you have a valid token.");
    }
  }

  /// Once successfully setup, you should authorize your user with our service.
  ///
  /// With your signed in user, you can authorize them to use the AHI service.
  void authorizeUser() async {
    Map<String, dynamic> ahiConfigTokens = {
      "USER_ID": AHI_TEST_USER_ID,
      "SALT": AHI_TEST_USER_SALT,
      "CLAIMS": AHI_TEST_USER_CLAIMS
    };
    try {
      await platform
          .invokeMethod("authorizeUser", ahiConfigTokens)
          .then((response) => {handleAuthorizeUser(response)});
    } on PlatformException catch (error) {
      print("AHI ERROR: authorizeUser $error}");
    }
  }

  void handleAuthorizeUser(dynamic response) {
    if (response != null) {
      print("AHI: Auth Error: $response");
      print("AHI: Confirm you are using a valid user id, salt and claims");
      return;
    }
    setState(() {
      _isUserAuthorized = true;
    });
    print("AHI: Setup user successfully");
  }

  /// Check if the AHI resources are downloaded.
  ///
  /// We have remote resources that exceed 100MB that enable our scans to work.
  /// You are required to download them inorder to obtain a body scan.
  void areAHIResourcesAvailable() async {
    platform.invokeMethod("areAHIResourcesAvailable").then(
        (resourcesAvailable) => {handleResourcesAvailable(resourcesAvailable)});
  }

  void handleResourcesAvailable(bool resourcesAvailable) {
    if (!resourcesAvailable) {
      print("AHI INFO: Resources are not downloaded.");
      return;
    }
    setState(() {
      _resourcesAreAvailable = true;
    });
    print("AHI: Resources ready");
  }

  /// Download scan resources.
  ///
  /// We recommend only calling this function once per session to prevent duplicate background resource calls.
  void downloadAHIResources() {
    platform.invokeMethod("downloadAHIResources");
  }

  /// Check the size of the AHI resources that require downloading.
  ///
  /// Resource size is returned from the MultiScan SDK in bytes.
  /// We are demonstrating the conversion into MB.
  void checkAHIResourcesDownloadSize() {
    platform.invokeMethod("checkAHIResourcesDownloadSize").then((size) => {
          print("AHI INFO: Size of download is ${(size as num) / 1024 / 1024}"),
        });
  }

  void startFaceScan() async {
    Map<String, dynamic> options = {
      "enum_ent_sex": "male",
      "cm_ent_height": 180,
      "kg_ent_weight": 85,
      "yr_ent_age": 35,
      "bool_ent_smoker": false,
      "bool_ent_hypertension": false,
      "bool_ent_bloodPressureMedication": false,
      "enum_ent_diabetic": "none"
    };
    if (!_areFaceScanConfigOptionsValid(options)) {
      print("AHI ERROR: Face Scan inputs invalid.");
      return;
    }
    try {
      await platform
          .invokeMethod("startFaceScan", options)
          .then((value) => {handleFaceScanResults(value)});
    } on PlatformException catch (error) {
      if (error.code == "USER_CANCELLED") {
        print("AHI: INFO: User cancelled the session.");
        return;
      }
      print("AHI: ERROR WITH FACE SCAN: $error");
    }
  }

  void handleFaceScanResults(dynamic data) {
    Map<String, dynamic> result = Map.from(data);
    if (result is Map<String, dynamic>) {
      // Handle face scan results
      print("AHI: SCAN RESULTS: $result");
      return;
    }
    print("AHI: UNKNOWN ERROR WITH FACE SCAN RESULT: $result");
  }

  void startFingerScan() async {
    Map<String, dynamic> options = {
      "sec_ent_scanLength": 60,
      "str_ent_instruction1": "Instruction 1",
      "str_ent_instruction2": "Instruction 2"
    };

    if (!_areFingerScanConfigOptionsValid(options)) {
      print("AHI ERROR: Finger Scan inputs invalid.");
      return;
    }

    try {
      await platform
          .invokeMethod("startFingerScan", options)
          .then((value) => {handleFingerScanResults(value)});
    } on PlatformException catch (error) {
      if (error.code == "USER_CANCELLED") {
        print("AHI: INFO: User cancelled the session.");
        return;
      }
      print("AHI: ERROR WITH FINGER SCAN: $error");
    }
  }

  void handleFingerScanResults(dynamic data) {
    Map<String, dynamic> result = Map.from(data);
    if (result is Map<String, dynamic>) {
      // Handle finger scan results
      print("AHI: SCAN RESULTS: $result");
      return;
    }
    print("AHI: UNKNOWN ERROR WITH FACE SCAN RESULT: $result");
  }

  /// Initiate native module BodyScan
  void startBodyScan() async {
    Map<String, dynamic> options = {
      "enum_ent_sex": "male",
      "yr_ent_age": 30,
      "cm_ent_height": 180,
      "kg_ent_weight": 85
    };
    if (!areBodyScanConfigOptionsValid(options)) {
      print("AHI ERROR: Body Scan inputs invalid.");
      return;
    }
    try {
      await platform
          .invokeMethod("startBodyScan", options)
          .then((bodyScanResult) => {
                handleBodyScanResults(bodyScanResult),
              });
    } on PlatformException catch (error) {
      if (error.code == "USER_CANCELLED") {
        print("AHI: INFO: User cancelled the session.");
        return;
      }
      print("AHI: ERROR WITH BODY SCAN: $error");
    }
  }

  void handleBodyScanResults(dynamic data) {
    Map<String, dynamic> result = Map<String, dynamic>.from(data);
    try {
      // Handle body scan results
      print("AHI: SCAN RESULTS: $result");
      // Consider getting the 3D mesh here
      // This is an optional feature.
      getBodyScanExtras(Map.from(result));
    } on PlatformException catch (error) {
      print(
          "AHI: UNKNOWN ERROR WITH BODY SCAN RESULT: ERROR: $error RESULT: $result");
    }
  }

  /// Use this function to fetch the 3D avatar mesh.
  ///
  /// The 3D mesh can be created and returned at any time.
  /// We recommend doing this on successful completion of a body scan with the results.
  /// getBodyScanExtras(Map<String, dynamic> result) async {
  void getBodyScanExtras(Map<String, dynamic> result) async {
    try {
      await platform
          .invokeMethod("getBodyScanExtras", result)
          .then((value) => {handleBodyScanExtras(value)});
    } on PlatformException catch (error) {
      print("AHI: ERROR GETTING BODY SCAN EXTRAS. $error");
    }
  }

  void handleBodyScanExtras(dynamic extras) {
    print("AHI EXTRAS: ${extras}");
    var path = extras["meshURL"];
    print("AHI 3D Mesh path: $path");
  }

  // Check if MultiScan is on or offline.
  void getMultiScanStatus() async {
    platform
        .invokeMethod("getMultiScanStatus")
        .then((status) => print("AHI INFO: Status: $status"));
  }

  /// Check your AHI MultiScan organisation  details.
  void getMultiScanDetails() async {
    try {
      await platform
          .invokeMethod("getMultiScanDetails")
          .then((details) => print("AHI INFO: MultiScan details: $details"));
    } on PlatformException catch (error) {
      print("AHI ERROR: getMultiScanDetails $error}");
    }
  }

  /// Check if the user is authorized to use the MuiltScan service.
  void getUserAuthorizedState() async {
    try {
      await platform.invokeMethod("getUserAuthorizedState").then(
          (isAuthorized) => {
                print(
                    "AHI INFO: User is ${isAuthorized ? "authorized" : "not authorized"}")
              });
    } on PlatformException catch (error) {
      print("AHI ERROR: getUserAuthorizedState $error}");
    }
  }

  /// Deauthorize the user.
  void deAuthorizeUser() async {
    try {
      await platform
          .invokeMethod("deauthorizeUser")
          .then((deAuthorizeResult) => {
                if (deAuthorizeResult != null)
                  {
                    print(
                        "AHI ERROR: Failed to deuathorize user with error: $deAuthorizeResult)"),
                  }
                else
                  {
                    print("AHI INFO: User is deauthorized."),
                  }
              });
    } on PlatformException catch (error) {
      print("AHI ERROR: deAuthorizeUser $error");
    }
  }

  /// Release the MultiScan SDK session.
  ///
  /// If you  use this, you will need to call setupSDK again.
  void releaseMultiScanSDK() async {
    var releaseSDKResult = await platform.invokeMethod("releaseMultiScanSDK");
    if (releaseSDKResult != null) {
      print("AHI ERROR: Failed to release SDK with error: $releaseSDKResult");
      return;
    }
    print("AHI INFO: SDK has been released successfully.");
  }

  /// All MultiScan scan configs require this information.
  ///
  /// Please see the Schemas for more information:
  /// BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/
  /// FaceScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
  bool areSharedScanConfigOptionsValid(Map<String, dynamic> inputValues) {
    var height = inputValues["cm_ent_height"];
    var weight = inputValues["cm_ent_weight"];
    var sex = inputValues["enum_ent_sex"];
    if (height is! int && weight is! int && sex is! String) {
      return false;
    }
    return ['male', 'female'].contains(sex);
  }

  /// FaceScan config requirements validation.
  ///
  /// Please see the Schemas for more information:
  /// FaceScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
  bool _areFaceScanConfigOptionsValid(Map<String, dynamic> inputValues) {
    if (!areSharedScanConfigOptionsValid(inputValues)) {
      return false;
    }
    var sex = inputValues["enum_ent_sex"];
    var smoke = inputValues["bool_ent_smoker"];
    var isDiabetic = inputValues["enum_ent_diabetic"];
    var hypertension = inputValues["bool_ent_hypertension"];
    var blood = inputValues["bool_ent_bloodPressureMedication"];
    var height = inputValues["cm_ent_height"];
    var weight = inputValues["kg_ent_weight"];
    var age = inputValues["yr_ent_age"];
    if (sex is! String &&
        smoke is! String &&
        isDiabetic is! String &&
        hypertension is! String &&
        blood is! String &&
        height is! int &&
        weight is! int &&
        age is! int &&
        height < 25 &&
        height > 300 &&
        weight < 25 &&
        weight > 300) {
      return false;
    } else {
      return ["none", "type1", "type2"].contains(isDiabetic);
    }
  }

  /// FingerScan config requirements validation.
  ///
  /// Please see the Schemas for more information:
  /// FingerScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FingerScan/Schemas/
  bool _areFingerScanConfigOptionsValid(Map<String, dynamic> inputValues) {
    var scanLength = inputValues["sec_ent_scanLength"];
    var instruction1 = inputValues["str_ent_instruction1"];
    var instruction2 = inputValues["str_ent_instruction2"];

    if (scanLength is! int &&
        instruction1 is! String &&
        instruction2 is! String &&
        scanLength < 20) {
      return false;
    } else {
      return true;
    }
  }

  /// BodyScan config requirements validation.
  ///
  /// Please see the Schemas for more information:
  /// BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/
  bool areBodyScanConfigOptionsValid(Map<String, dynamic> inputValues) {
    if (!areSharedScanConfigOptionsValid(inputValues)) {
      return false;
    }
    var height = inputValues["cm_ent_height"];
    var weight = inputValues["kg_ent_weight"];
    if (height is! int &&
        weight is! int &&
        height < 50 &&
        height > 255 &&
        weight < 16 &&
        weight > 300) {
      return false;
    }
    return true;
  }

  bool areBodyScanSmoothingResultsValid(
      List<Map<String, dynamic>> resultsList) {
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
      "date"
    ];
    bool isValid = true;
    for (Map<String, dynamic> result in resultsList) {
      var resultsKeys = result.keys;
      for (String key in sdkResultSchema) {
        if (!resultsKeys.contains(key)) {
          isValid = false;
        }
      }
    }
    return isValid;
  }

  // Component
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        top: true,
        bottom: true,
        left: true,
        right: true,
        child: ListView(
          padding: const EdgeInsets.only(left: 12, right: 12),
          children: [
            if (!_isSDKSetup) defaultButton("Setup SDK", () => {didTapSetup()}),
            if (_isUserAuthorized)
              defaultButton("Start FaceScan", () => {didTapStartFaceScan()}),
            if (_isUserAuthorized)
              defaultButton(
                  "Start FingerScan", () => {didTapStartFingerScan()}),
            if (_isUserAuthorized && !_resourcesAreAvailable)
              defaultButton(
                  "Download Resources", () => {didTapDownloadResources()}),
            if (_isUserAuthorized && _resourcesAreAvailable)
              defaultButton("Start BodyScan", () => {didTapStartBodyScan()}),
          ],
        ),
      ),
    );
  }
}

Widget defaultButton(String title, Function action) {
  return Container(
    width: double.infinity,
    padding: const EdgeInsets.only(top: 5),
    height: 55.0,
    child: TextButton(
      child: Text(
        title,
        style: const TextStyle(color: Colors.white),
      ),
      style:
          ButtonStyle(backgroundColor: MaterialStateProperty.all(Colors.black)),
      onPressed: () {
        action();
      },
    ),
  );
}
