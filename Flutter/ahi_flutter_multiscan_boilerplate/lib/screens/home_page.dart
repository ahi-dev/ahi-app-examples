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

  var ahiConfigTokens = <String, dynamic>{

  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
          child: setupSuccessful ? afterSetupButtons() : setupSDKButton()),
    );
  }

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

  Widget afterSetupButtons() {
    return Column(
      children: [
        resourcesDownload ? resourcesDownloadButtons() : setupCompleted()
      ],
    );
  }

  Widget setupCompleted() {
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
                  backgroundColor: MaterialStateProperty.all(Colors.black)),
              onPressed: () {
                _didTapDownloadResources();
                // setState(() {
                //   resourcesDownload = true;
                // });
              },
            ),
          ),
        ),
      ],
    );
  }

  Widget resourcesDownloadButtons() {
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
      var result = await platform.invokeMethod("setupMultiScanSDK", ahiConfigTokens);
      if (result == "setup_successful" || result == "SUCCESS") {
        setState(() {
          setupSuccessful = true;
        });
      }
    } on PlatformException catch (e) {
      print(e.message);
    }
  }

  Future<void> _didTapStartFaceScan() async {
    try {
      platform.invokeMethod("startFaceScan");
    } on PlatformException catch (e) {
      print(e.message);
    }
  }

  Future<void> _didTapDownloadResources() async {
    try {
      var result = await platform.invokeMethod("downloadResources");
      if (result == "AHI: Resources ready" || result == "SUCCESS") {
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
      platform.invokeMethod("startBodyScan");
    } on PlatformException catch (e) {
      print(e.message);
    }
  }
}