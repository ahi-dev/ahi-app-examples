import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key}) : super(key: key);

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

enum MSPaymentType {
  PAYG,
  SUBS
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel('ahi_multiscan_flutter_wrapper');
  bool setupSuccessful = false;
  bool resourcesDownload = false;
  bool _isButtonDisabled = false;

  // todo do not commit the tokens
  var ahiConfigTokens = <String, dynamic>{
  };

  var ahiConfigFaceScan = <String, dynamic>{
    "TAG_ARG_GENDER": "M",
    "TAG_ARG_SMOKER": "F",
    "TAG_ARG_DIABETIC": "none",
    "TAG_ARG_HYPERTENSION": "F",
    "TAG_ARG_BPMEDS": "F",
    "TAG_ARG_HEIGHT_IN_CM": 171,
    "TAG_ARG_WEIGHT_IN_KG": 86,
    "TAG_ARG_AGE": 24,
    "TAG_ARG_PREFERRED_HEIGHT_UNITS": "CENTIMETRES",
    "TAG_ARG_PREFERRED_WEIGHT_UNITS": "KILOGRAMS"
  };

  var ahiConfigBodyScan = <String, dynamic>{
    "TAG_ARG_GENDER": "M",
    "TAG_ARG_HEIGHT_IN_CM": 180,
    "TAG_ARG_WEIGHT_IN_KG": 85
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
    Size size = MediaQuery
        .of(context)
        .size;
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
    Size size = MediaQuery
        .of(context)
        .size;
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
    Size size = MediaQuery
        .of(context)
        .size;
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

  bool _areFaceScanConfigOptionsValid(Map<String, dynamic> avatarValues) {

    var sex = avatarValues["TAG_ARG_GENDER"] is String;
    var smoke = avatarValues["TAG_ARG_SMOKER"] is String;
    var isDiabetic = avatarValues["TAG_ARG_DIABETIC"] is String;
    var hypertension = avatarValues["TAG_ARG_HYPERTENSION"] is String;
    var blood = avatarValues["TAG_ARG_BPMEDS"] is String;
    var height = avatarValues["TAG_ARG_HEIGHT_IN_CM"] is int;
    var weight = avatarValues["TAG_ARG_WEIGHT_IN_KG"] is int;
    var age = avatarValues["TAG_ARG_AGE"] is int;
    var heightUnits = avatarValues["TAG_ARG_PREFERRED_HEIGHT_UNITS"] is String;
    var weightUnits = avatarValues["TAG_ARG_PREFERRED_WEIGHT_UNITS"] is String;
    if (sex != null && smoke != null && isDiabetic != null &&
        hypertension != null && blood != null &&
        height != null && weight != null &&
        age != null && heightUnits != null &&
        weightUnits != null && height in 25..300 && weight in 25..300 && age in 13..120) {

    }
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
        return;
      }
      setState(() {
        setupSuccessful = true;
      });
    } on PlatformException catch (e) {
      print(e.message);
    }
  }

  Future<void> _didTapStartFaceScan() async {
    try {
      var faceScanResult = await platform.invokeMethod(
          "startFaceScan", ahiConfigFaceScan);
      print(faceScanResult);
    } on PlatformException catch (e) {
      print(e.message);
    }
  }

  Future<void> _didTapDownloadResources() async {
    try {
      setState(() {
        _isButtonDisabled = true;
      });
      var downloadResourcesResult =
      await platform.invokeMethod("downloadAHIResources");
      if (!(downloadResourcesResult == "setup_successful" ||
          downloadResourcesResult == "SUCCESS")) {
        return;
      }
      var checkAHIResourcesDownloadSizeResult =
      platform.invokeMethod("checkAHIResourcesDownloadSize");
      checkAHIResourcesDownloadSizeResult.then((value) => print(value));
      var areResourcesDownloadedResult =
      await platform.invokeMethod("didTapDownloadResources");
      if (areResourcesDownloadedResult == "AHI: Resources ready") {
        setState(() {
          resourcesDownload = true;
        });
      }
    } on PlatformException catch (e) {
      print(e.message);
    }
  }

  Future<void> _didTapStartBodyScan() async {
    try {
      var bodyScanResult = await platform.invokeMethod("startBodyScan");
      print(bodyScanResult);
    } on PlatformException catch (e) {
      print(e.message);
    }
  }
}
