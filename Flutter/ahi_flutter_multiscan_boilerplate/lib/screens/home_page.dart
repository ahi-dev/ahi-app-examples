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
    "AHI_MULTI_SCAN_TOKEN":
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJSS2Q2QUxGclZYclNKWktWdnp3SitaVVhSdW5uY29leVp4UGNVWWRrNXNZV2RHRmU1eHo4ejVtM3lIUHZvcHBXajlJeXMva1lNdVg0MUdhTUtmWktvUWVpcWdNN3NSS0dOK3RqZC9oZDBWR3FTM2ZocE1DdU9XaExZYTZFYm1MMGhQQlJyOS9HSXhVdkdkQWVpM1RHM25udDU0K1dUSkY2SFl6dHk0VWJnRFl5bVQxeFA5MEJYaE11eGhTUExJMzk3VGloNm5POW5hZXhGQmZKZjFNZHl4dWFPZzRpVFBSTEtpeG5PRXpZRHpORzQ2L0RPM2NHNlV3Q0JvRDVhYm1JVXNLRXlHUnpHSy9mMUVxcHE1ZGlQM2EvOTFLRXU1NUFXeGtzajBJTk1Sa2lCMThWOWxjRHljeFpuME9TYWtJNEptYktHYlJEMXlpcnJYYU9WQlpBZHpEcDdkN2V6aThEdE5RSWtJWExPR1pDZUlZL0xkQ0hmSy9YYmJteE9uaHlwUWFiVUxLUUFPVHVCak1FdDFybVBLdmxMUTUvSEhwZTZZVFk3dGJWNjZab2FLazZRQ0JIeEpjN2pzTHY2NkNzekttZTJHbVRteHRKOFRBMVFETDBCQUlhYktOWjFtR2NPN0VsNmtnVXRPeVl1YkVhT2d3eEFLMFJqSE9KcVhxYUNkZklaT2dEYXFRU1BodlVzWGpQNWZ5R1BUTTRSUnB5V2FGYzFEWkJzeExTUU94cG9MT1R0SzdBZnViaUsvZFJORGd4TnJxVmpHeXJ2Q29PQmwvWEhhZkxkNzI2RlVBc3FTcFJsREE4bnZJNE9XVEVJRnVrUnZkR1gvcmNNVlUvc2h6VGJ0WmJscnFCU0RDWUhxQVBKd3NPbyt0N1Y1QWc4K1phMTFuTGkwST0iLCJleHAiOjE3ODI2MzI3NDEsInZlciI6M30.02efe4ww7XKcPh3vt1009lLmfvLoAfSQTvgmlkPWMNFv9yIPWCvXejcBWhheY1XLejWGad7MXZM_WlBHhM3tCK9FN1K8_47-MTRRUpXcOR2naVArswofbPE8yGs1rOc3HwzNThg2TrnbVICRflnKoFLs3PLg067DUMOAqlm-o20rQovpwr0Bf_V6IDW-bc6u0snFIPX-VLbTr3MjuOWlJ7WTNbmKttaQsNeVo4JLyU-BnoeMv8w5j6VPPsX8LAaqE_JcdIFFlyJxHCfrs5aTP6EiBk7tb4WrsW-x1fWC34UIQA22yVfZusPxzDTDqbJuZ_MsHkoMApuuEOuJblCoUyS7JVJsCFUcw_08cu94zgI_CMTaNpsxBgdQyP_cAGrFXjmbRvERDNmss0IbFFmcMNtJtAQ0PSjl28KqzbUGG7c4QM5ctGyPJ1zkPFmGWUl2kUuEK4OhKnbcvkFaj0x_HDNm6G3ksPNsZs7_oy_RhecolX2uTeMG1CshlG3_ppJyFpr_c1VPGmJd7zrvUqOimdgraVLL5JqC-uajhCNcCVLn37VUjCRtpCoN_qkxXciYfLh0tf9-oiXAE4uTPbTY1ZrvfvO8cyT9mxn84GdDigVEJ16eMflwYkK_j_5a04TZYpsqENXlLOpRf7y4a89wCVEAG-B_DfB8VViT59b37rU",
    "AHI_TEST_USER_ID": "AHI_TEST_USER",
    "AHI_TEST_USER_SALT": "user",
    "AHI_TEST_USER_CLAIMS": ["test"]
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