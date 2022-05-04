import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key}) : super(key: key);

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

enum MSPaymentType { PAYG, SUBS }

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel('ahi_multiscan_flutter_wrapper');
  bool setupSuccessful = false;
  bool resourcesDownload = false;
  bool _isButtonDisabled = false;

  // todo do not commit the tokens
  var ahiConfigTokens = <String, dynamic>{
  };

  var ahiConfigScan = <String, dynamic>{
    "sex": "M",
    "smoker": "F",
    "diabetic": "none",
    "hypertension": "F",
    "bloodPressureMedication": "F",
    "height": 171,
    "weight": 86,
    "age": 24
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
          child: setupSuccessful
              ? afterSetupSDKButtonClicked()
              : setupSDKButton()),
    );
  }

  /// This is the initial view when the app launches
  Widget setupSDKButton() {
    Size size = MediaQuery.of(context).size;
    return Column(
      children: [
        const SizedBox(height: 100),
        Padding(
          padding: const EdgeInsets.all(8.0),
          child: SizedBox(
            width: size.width,
            child: TextButton(
              child: const Text(
                'Setup SDK',
                style: TextStyle(color: Colors.white),
              ),
              style: ButtonStyle(
                  backgroundColor: MaterialStateProperty.all(Colors.black)),
              onPressed: () {
                _didTapSetup();
              },
            ),
          ),
        ),
      ],
    );
  }

  /// This view appears after the setup sdk button is clicked
  Widget afterSetupSDKButtonClicked() {
    return Column(
      children: [
        resourcesDownload ? resourcesDownloaded() : setupSDKCompleted()
      ],
    );
  }

  /// This is when the sdk setup is complete
  Widget setupSDKCompleted() {
    Size size = MediaQuery.of(context).size;
    return Column(
      children: [
        const SizedBox(height: 100),
        Padding(
          padding: const EdgeInsets.all(8.0),
          child: SizedBox(
            width: size.width,
            child: TextButton(
              child: const Text(
                'Start FaceScan',
                style: TextStyle(color: Colors.white),
              ),
              style: ButtonStyle(
                  backgroundColor: MaterialStateProperty.all(Colors.black)),
              onPressed: () {
                _didTapStartFaceScan();
              },
            ),
          ),
        ),
        const SizedBox(height: 10),
        Padding(
          padding: const EdgeInsets.all(8.0),
          child: SizedBox(
            width: size.width,
            child: TextButton(
                child: const Text(
                  'Download Resources',
                  style: TextStyle(color: Colors.white),
                ),
                style: ButtonStyle(
                  backgroundColor: MaterialStateProperty.all(Colors.black),
                ),
                onPressed: _isButtonDisabled == false
                    ? _didTapDownloadResources
                    : null),
          ),
        ),
      ],
    );
  }

  /// THis view is when the resources are downloaded.
  Widget resourcesDownloaded() {
    Size size = MediaQuery.of(context).size;
    return Column(
      children: [
        const SizedBox(height: 100),
        Padding(
          padding: const EdgeInsets.all(8.0),
          child: SizedBox(
            width: size.width,
            child: TextButton(
              child: const Text(
                'Start FaceScan',
                style: TextStyle(color: Colors.white),
              ),
              style: ButtonStyle(
                  backgroundColor: MaterialStateProperty.all(Colors.black)),
              onPressed: () {
                _didTapStartFaceScan();
              },
            ),
          ),
        ),
        const SizedBox(height: 10),
        Padding(
          padding: const EdgeInsets.all(8.0),
          child: SizedBox(
            width: size.width,
            child: TextButton(
              child: const Text(
                'Start BodyScan',
                style: TextStyle(color: Colors.white),
              ),
              style: ButtonStyle(
                  backgroundColor: MaterialStateProperty.all(Colors.black)),
              onPressed: () {
                _didTapStartBodyScan();
              },
            ),
          ),
        ),
      ],
    );
  }

  bool _areFaceScanConfigOptionsValid(Map<String, dynamic> inputValues) {
    if (!_areSharedScanConfigOptionsValid(inputValues)) {
      return false;
    }
    var sex = inputValues["sex"];
    var smoke = inputValues["smoker"];
    var isDiabetic = inputValues["diabetic"];
    var hypertension = inputValues["hypertension"];
    var blood = inputValues["bloodPressureMedication"];
    var height = inputValues["height"];
    var weight = inputValues["weight"];
    var age = inputValues["age"];
    if (sex != null &&
        sex is String &&
        smoke != null &&
        smoke is String &&
        isDiabetic != null &&
        isDiabetic is String &&
        hypertension != null &&
        hypertension is String &&
        blood != null &&
        blood is String &&
        height != null &&
        height is int &&
        weight != null &&
        weight is int &&
        age != null &&
        age is int) {
      return ["none", "type1", "type2"].contains(isDiabetic);
    } else {
      return false;
    }
  }

  // todo we will do range check later
  bool _areBodyScanConfigOptionsValid(Map<String, dynamic> inputValues) {
    if (!_areSharedScanConfigOptionsValid(inputValues)) {
      return false;
    }
    var sex = inputValues["sex"];
    var height = inputValues["height"];
    var weight = inputValues["weight"];
    if (sex != null &&
        sex is String &&
        height != null &&
        height is int &&
        weight != null &&
        weight is int) {
      return true;
    }
    return false;
  }

  bool _areBodyScanSmoothingResultsValid(Map<String, dynamic> result) {
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
    var isValid = false;
    for (var i = 0; i < sdkResultSchema.length; i++) {
      // Check if keys in result contains the required keys.
      if (!(sdkResultSchema[i].contains(result as Pattern))) {
        isValid = true;
      }
    }
    return !isValid;
  }

  bool _areSharedScanConfigOptionsValid(Map<String, dynamic> inputValues) {
    var sex = inputValues["sex"];
    var height = inputValues["height"];
    var weight = inputValues["weight"];

    if (sex != null &&
        sex is String &&
        height != null &&
        height is int &&
        weight != null &&
        weight is int) {
      return ['M', 'F'].contains(sex);
    }
    return false;
  }

  Future<void> _didTapSetup() async {
    try {
      var setupSDKResult =
          await platform.invokeMethod("setupMultiScanSDK", ahiConfigTokens);
      if (!(setupSDKResult == "setup_successful" ||
          setupSDKResult == "SUCCESS")) {
        return;
      }
      var authorizeUserResult =
          await platform.invokeMethod("authorizeUser", ahiConfigTokens);
      if (!(authorizeUserResult == "AHI: Setup user successfully" ||
          authorizeUserResult == "SUCCESS")) {
        print("AHI: Auth Error: $authorizeUserResult");
        print("AHI: Confirm you are using a valid user id, salt and claims.");
        return;
      }
      setState(() {
        setupSuccessful = true;
      });
      print("AHI: Setup user successfully");
    } on PlatformException catch (e) {
      print(e.message);
    }
  }

  Future<void> _didTapStartBodyScan() async {
    try {
      var paymentType = MSPaymentType.PAYG.name;
      ahiConfigScan["Payment_Type"] = paymentType;
      var inputValues = ahiConfigScan;
      if (!_areBodyScanConfigOptionsValid(inputValues)) {
        print("AHI ERROR: Body Scan inputs invalid.");
        return;
      }
      var bodyScanResult =
          await platform.invokeMethod("startBodyScan", ahiConfigScan);
      print("AHI: SCAN RESULTS: $bodyScanResult");
      var result = jsonDecode(bodyScanResult);
      if (_areBodyScanSmoothingResultsValid(result)) {
        var id = bodyScanResult['id'];
        var path = await platform.invokeMethod("getBodyScanExtras", id);
        print("AHI 3D Mesh path: $path");
      }
    } on PlatformException catch (e) {
      print(e.message);
    }
  }

  Future<void> _didTapStartFaceScan() async {
    try {
      var paymentType = MSPaymentType.PAYG.name;
      ahiConfigScan["Payment_Type"] = paymentType;
      var inputValues = ahiConfigScan;
      if (!_areFaceScanConfigOptionsValid(inputValues)) {
        print("AHI ERROR: Face Scan inputs invalid.");
        return;
      }
      var faceScanResult =
          await platform.invokeMethod("startFaceScan", ahiConfigScan);
      print("AHI: SCAN RESULTS: $faceScanResult");
    } on PlatformException catch (e) {
      print(e.message);
    }
  }

  Future<void> _didTapDownloadResources() async {
    try {
      bool areResourcesDownloadedResult =
          await platform.invokeMethod("areAHIResourcesAvailable");
      if (areResourcesDownloadedResult == false) {
        print("AHI INFO: Resources are not downloaded");
        platform.invokeMethod("downloadAHIResources");
        print("something here");
        platform.invokeMethod("checkAHIResourcesDownloadSize").then((size) =>
            print(
                "AHI INFO: Size of download is ${(size as num) / 1024 / 1024}"));
        Future.delayed(const Duration(seconds: 30), () {
          _didTapDownloadResources();
        });
      } else {
        print("AHI: Resources ready");
        setState(() {
          resourcesDownload = true;
        });
      }

      setState(() {
        _isButtonDisabled = true;
      });
      var downloadResourcesResult =
          await platform.invokeMethod("downloadAHIResources");
      if (!(downloadResourcesResult == "setup_successful" ||
          downloadResourcesResult == "SUCCESS")) {
        return;
      }
    } on PlatformException catch (e) {
      print(e.message);
    }
  }
}
