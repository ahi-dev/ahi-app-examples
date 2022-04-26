import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key}) : super(key: key);

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel('flutter_boilerplate_wrapper');
  bool setupSuccessful = false;
  bool resourcesDownload = false;
  bool _isButtonDisabled = false;

  // todo do not commit the tokens
  var ahiConfigTokens = <String, dynamic>{
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
          child: setupSuccessful ? afterSetupSDKButtonClicked() : setupSDKButton()),
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
                  backgroundColor: MaterialStateProperty.all(Colors.black),),
                onPressed: _isButtonDisabled == false ? _didTapDownloadResources: null
            ),
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

  Future<void> _didTapSetup() async {
    try {
      var setupSDKResult = await platform.invokeMethod("setupMultiScanSDK", ahiConfigTokens);
      if (!(setupSDKResult == "setup_successful" || setupSDKResult == "SUCCESS")) {
       return;
      }
      var authorizeUserResult = await platform.invokeMethod("authorizeUser", ahiConfigTokens);
      if (!(authorizeUserResult == "AHI: Setup user successfully" || authorizeUserResult == "SUCCESS")) {
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
      var faceScanResult = await platform.invokeMethod("startFaceScan");
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
      var downloadResourcesResult = await platform.invokeMethod("downloadAHIResources");
      if (!(downloadResourcesResult == "setup_successful" || downloadResourcesResult == "SUCCESS")) {
        return;
      }
      var checkAHIResourcesDownloadSizeResult = platform.invokeMethod("checkAHIResourcesDownloadSize");
      checkAHIResourcesDownloadSizeResult.then((value) => print(value));
      var areResourcesDownloadedResult = await platform.invokeMethod("didTapDownloadResources");
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