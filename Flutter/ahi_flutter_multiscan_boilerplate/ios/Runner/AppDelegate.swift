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

import UIKit
import Flutter
// The MultiScan SDK
import AHIMultiScan
// The Body Scan SDK
import MyFiziqSDKCoreLite
// The FaceScan SDK
import MFZFaceScan

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate, FlutterStreamHandler {
    private let CHANNEL = "flutter_boilerplate_wrapper"
    private let EVENTS_CHANNEL = "flutter_boilerplate_wrapper"
    /// Event sink used to send scan status events back to the Flutter code
    private var eventSink: FlutterEventSink?
    let multiScan = AHIMultiScanModule()
    
    override func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        let controller: FlutterViewController = window?.rootViewController as! FlutterViewController
        let boilerPlateChannel = FlutterMethodChannel(name: CHANNEL, binaryMessenger: controller.binaryMessenger)
        let eventBoilerPlateChannel = FlutterEventChannel(name: EVENTS_CHANNEL, binaryMessenger: controller.binaryMessenger)
        eventBoilerPlateChannel.setStreamHandler(self)
        
        boilerPlateChannel.setMethodCallHandler({
            [weak self] (call: FlutterMethodCall, result: @escaping FlutterResult) -> Void in
            guard let weakSelf = self else {
                result("Error: self was nil when trying to call iOS method")
                return
            }
            if call.method == "setupMultiScanSDK" {
                guard let args = call.arguments else {
                    return
                }
                let myArgs = args as? [String: Any]
                let ahiMultiScanToken = myArgs?["AHI_MULTI_SCAN_TOKEN"] as! String
                weakSelf.multiScan.setupMultiScanSDK(ahiMultiScanToken, result: result)
            }
            else if call.method == "authorizeUser" {
                guard let args = call.arguments else {
                    return
                }
                let myArgs = args as? [String: Any]
                guard let userID = myArgs?["AHI_TEST_USER_ID"] as? String, let salt = myArgs?["AHI_TEST_USER_SALT"] as? String, let claims = myArgs?["AHI_TEST_USER_CLAIMS"] as? [String] else {return}
                weakSelf.multiScan.authorizeUser(userID, salt: salt, claims: claims)
            }
            else if call.method == "startFaceScan" {
                weakSelf.multiScan.startFaceScan()
            }
            else if call.method == "resourcesRelated" {
                weakSelf.multiScan.downloadAHIResources()
                weakSelf.multiScan.areAHIResourcesAvailable()
            }
            else if call.method == "startBodyScan" {
                weakSelf.multiScan.startBodyScan()
            }
        })
        
        GeneratedPluginRegistrant.register(with: self)
        return super.application(application, didFinishLaunchingWithOptions: launchOptions)
    }

    func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = events
        return nil
    }
    
    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        self.eventSink = nil
        return nil
    }
}

class AHIMultiScanModule: NSObject {
    
    // MARK: Scan Instances
    
    /// Instance of AHI MultiScan
    let ahi = AHIMultiScan.shared()!
    /// Instance of AHI FaceScan
    let faceScan = AHIFaceScan.shared()
    /// Instance of AHI BodyScan
    let bodyScan = AHIBodyScan.shared()
    
    public override init() {
        super.init()
        ahi.setPersistenceDelegate(self)
    }
}

// MARK: - MultiScan SDK Setup Functions

extension AHIMultiScanModule {
    /// Setup the MultiScan SDK
    ///
    /// This must happen before requesting a scan.
    /// We recommend doing this on successfuil load of your application.
    fileprivate func setupMultiScanSDK(_ token: String, result: @escaping FlutterResult) {
        ahi.setup(withConfig: ["TOKEN": token], scans: [faceScan, bodyScan]) {
            [weak self] error in
            if let err = error {
                print("AHI: Error setting up: \(err)")
                print("AHI: Confirm you have a valid token.")
                result("setup_falied");
                return
            }
            //            self?.authorizeUser()
            // Do flutter success event
            result("setup_successful")
        }
    }
    
    /// Once successfully setup, you should authorize your user with our service.
    ///
    /// With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
    fileprivate func authorizeUser(_ userID: String, salt: String, claims: [String]) {
        ahi.userAuthorize(forId: userID, withSalt: salt, withClaims: claims) {
            [weak self] authError in
            if let err = authError {
                print("AHI: Auth Error: \(err)")
                print("AHI: Confirm you are using a valid user id, salt and claims")
                // Do flutter fail
                return
            }
            print("AHI: Setup user successfully")
//            self?.isSetup = true
            // Do flutter success
        }
    }
}

// MARK: - AHI Multi Scan Remote Resources

extension AHIMultiScanModule {
    /// Check if the AHI resources are downloaded.
    ///
    /// We have remote resources that exceed 100MB that enable our scans to work.
    /// You are required to download them inorder to obtain a body scan.
    fileprivate func areAHIResourcesAvailable() {
        ahi.areResourcesDownloaded {
            [weak self] success in
            if !success {
                print("AHI INFO: Resources are not downloaded.")
                               weak var weakSelf = self
                //                // We recommend polling to check resource state.
                //                // This is a simple example of how.
                                DispatchQueue.main.asyncAfter(deadline: .now() + 30.0) {
                                    weakSelf?.checkAHIResourcesDownloadSize()
                                    weakSelf?.areAHIResourcesAvailable()
                                }
                
                return
            }
//            self?.isFinishedDownloadingResources = success
            print("AHI: Resources ready")
        }
    }
    
    /// Download scan resources.
    ///
    /// We recomment only calling this function once per session to prevent duplicate background resource calls.
    fileprivate func downloadAHIResources() {
        ahi.downloadResourcesInBackground()
    }
    
    /// Check the size of the AHI resources that require downloading.
    fileprivate func checkAHIResourcesDownloadSize() {
        ahi.totalEstimatedDownloadSizeInBytes { [weak self] bytes in
            print("AHI INFO: Size of download is \(self?.convertBytesToMBString(Int(bytes)) ?? "0")")
        }
    }
}

// MARK: - AHI Face Scan Initialiser

extension AHIMultiScanModule {
    fileprivate func startFaceScan() {
        // All required face scan options.
        let options: [String : Any] = [
            "enum_ent_sex": "male",
            "cm_ent_height": 180,
            "kg_ent_weight": 85,
            "yr_ent_age": 35,
            "bool_ent_smoker": false,
            "bool_ent_hypertension": false,
            "bool_ent_bloodPressureMedication": false,
            "enum_ent_diabetic": "none"
        ]
        if !areFaceScanConfigOptionsValid(faceScanInput: options) {
            print("AHI ERROR: Face Scan inputs invalid.")
            return
        }
        // Ensure the view controller being used is the top one.
        // If you are not attempting to get a scan simultaneous with dismissing your calling view controller, or attempting to present from a view controller lower in the stack
        // you may have issues.
        guard let vc = topMostVC() else { return }
        ahi.initiateScan("face", paymentType: .PAYG, withOptions: options, from: vc) { scanTask, error in
            guard let task = scanTask, error == nil else {
                // Error code 7 is the code for the SDK interaction that cancels the scan.
                if let nsError = error as? NSError, nsError.code == 7 {
                    print("AHI: INFO: User cancelled the session.")
                } else {
                    // Handle error through either lack of results or error.
                    print("AHI: ERROR WITH FACE SCAN: \(error ?? NSError())")
                }
                return
            }
            task.continueWith(block: { resultsTask in
                if let results = resultsTask.result as? [String : Any] {
                    // Handle results
                    print("AHI: SCAN RESULTS: \(results)")
                }
                /// Handle failure.
                return nil
            })
        }
    }
}

// MARK: - AHI Body Scan Initialiser

extension AHIMultiScanModule {
    fileprivate func startBodyScan() {
        // All required body scan options
        let options: [String : Any] = [
            "enum_ent_sex": "male",
            "cm_ent_height": 180,
            "kg_ent_weight": 85
        ]
        if !areBodyScanConfigOptionsValid(faceScanInput: options) {
            print("AHI ERROR: Body Scan inputs invalid.")
            return
        }
        // Ensure the view controller being used is the top one.
        // If you are not attempting to get a scan simultaneous with dismissing your calling view controller, or attempting to present from a view controller lower in the stack
        // you may have issues.
        guard let vc = topMostVC() else { return }
        ahi.initiateScan("body", paymentType: .PAYG, withOptions: options, from: vc) {
            [weak self] scanTask, error in
            guard let task = scanTask, error == nil else {
                // Error code 4 is the code for the SDK interaction that cancels the scan.
                if let nsError = error as? NSError, nsError.code == 4 {
                    print("AHI: INFO: User cancelled the session.")
                } else {
                    // Handle error through either lack of results or error.
                    print("AHI: ERROR WITH BODY SCAN: \(error ?? NSError())")
                }
                return
            }
            task.continueWith(block: { resultsTask in
                if let results = resultsTask.result as? [String : Any] {
                    // Handle results
                    print("AHI: SCAN RESULTS: \(results)")
                    // Consider getting the 3D mesh here
                    // This is an optional feature.
                    self?.getBodyScanExtras(withBodyScanResult: results)
                }
                /// Handle failure.
                return nil
            })
        }
    }
}

// MARK: - Body Scan Extras

extension AHIMultiScanModule {
    /// Use this function to fetch the 3D avatar mesh.
    ///
    /// The 3D mesh can be created and returned at any time.
    /// We recommend doing this on successful completion of a body scan with the results.
    fileprivate func getBodyScanExtras(withBodyScanResult result: [String: Any]) {
        ahi.getExtra(result, options: nil) { error, extras in
            guard let extras = extras, error == nil else {
                print("AHI: ERROR GETTING BODY SCAN EXTRAS. \(error ?? NSError())")
                return
            }
            print("AHI EXTRAS: \(extras)")
            // The mesh is returned as a URL that stored the file in the app cache.
            if let meshURL = extras["meshURL"] as? URL {
                print("AHI: Mesh URL: \(meshURL)")
            }
        }
    }
}

// MARK: - AHI MultiScan Optional Functions

extension AHIMultiScanModule {
    /// Check if MultiScan is on or offline.
    fileprivate func getMultiScanStatus() {
        ahi.status { multiScanStatus in
            print("AHI INFO: Status: \(multiScanStatus)")
        }
    }
    
    /// Check your AHI MultiScan organisation  details.
    fileprivate func getMultiScanDetails() {
        if let details = ahi.getDetails() {
            print("AHI INFO: MultiScan details: \(details)")
        }
    }
    
    /// Check if the userr is authorized to use the MuiltScan service.
    fileprivate func getUserAuthorizedState() {
//        ahi.userIsAuthorized(forId: AHIConfigTokens.AHI_TEST_USER_ID) {
//            isAuthorized in
//            print("AHI INFO: User is \(isAuthorized ? "authorized" : "not authorized")")
//        }
    }
    
    /// Deuathorize the user.
    fileprivate func deauthorizeUser() {
        ahi.userDeauthorize { error in
            if let err = error {
                print("AHI ERROR: Failed to deuathorize user with error: \(err)")
            } else {
                print("AHI INFO: User is deauthorized.")
            }
        }
    }
    
    /// Release the MultiScan SDK session.
    ///
    /// If you  use this, you will need to call setupSDK again.
    fileprivate func releaseMultiScanSDK() {
        ahi.releaseSDK { [weak self] error in
            if let err = error {
                print("AHI ERROR: Failed to release SDK with error: \(err)")
            } else {
                print("AHI INFO: SDK has been released successfully.")
//                self?.isSetup = false
            }
        }
    }
}

// MARK: - Persistence Delegate example

// If you choose to use this, you will obtain two sets of results - one containing the "raw" output and another set containing "adj" output.
// "adj" means adjusted and is used to help provide historical results as a reference for the newest result to provide tailored to the user results.
// We recommend using this for individual users results; avoid using this if the app is a single user ID with multiple users results.
// More info found here: https://docs.advancedhumanimaging.io/MultiScan%20SDK/Data/
extension AHIMultiScanModule: AHIDelegatePersistence {
    func requestScanType(_ scan: String, options: [String : Any] = [:], completion completionBlock: @escaping (Error?, [[String : Any]]?) -> Void) {
        print("AHI INFO: Persistence Delegate method called by MultiScan SDK.")
        // You should have your body scan results stored somewhere that this function can access.
        var exampleResults = [[String: Any]]()
        // Each result requires:
        // - _ent_ values
        // - _raw_ values
        // - id value
        // - date value
        // Your token may only provide you access to a smaller subset of results.
        // The persistence delegate will still work with your results provided you adhere to the validation check.
        exampleResults.append([
            "enum_ent_sex": "male",
            "cm_ent_height": 180,
            "kg_ent_weight": 85,
            "cm_raw_chest": 104.5213096618652,
            "cm_raw_hips": 100.4377449035645,
            "cm_raw_inseam": 82.3893051147461,
            "cm_raw_thigh": 60.23823547363281,
            "cm_raw_waist": 84.81353988647462,
            "kg_raw_weightPredict": 82.55660247802734,
            "ml_raw_fitness": 0.8,
            "percent_raw_bodyFat": 17.3342390826027,
            "id": "ee2367211649040093",
            "date": 1649040093,
        ])
        if !areBodyScanSmoothingResultsValid(bodyScanResults: exampleResults) {
            print("AHI ERROR: Example body scan results does not contain correct key values in array.")
            completionBlock(nil, nil)
            return
        }
        // Call the completion block to return the results to the SDK.
        completionBlock(nil, exampleResults)
    }
}

// MARK: - Utilities

// MARK: Scan Input Validation

extension NSObject {
    /// All MultiScan scan configs require this information.
    ///
    /// Please see the Schemas for more information:
    /// BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/
    /// FaceScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
    public func areSharedScanConfigOptionsValid(scanInput configs: [String: Any]) -> Bool {
        guard
            let sex = configs["enum_ent_sex"] as? String,
            let _ = configs["cm_ent_height"] as? Int,
            let _ = configs["kg_ent_weight"] as? Int
        else {
            return false
        }
        return ["male", "female"].contains(sex)
    }
    
    /// FaceScan config requirements validation.
    ///
    /// Please see the Schemas for more information:
    /// FaceScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
    public func areFaceScanConfigOptionsValid(faceScanInput configs: [String: Any]) -> Bool {
        if !areSharedScanConfigOptionsValid(scanInput: configs) {
            return false
        }
        guard let age = configs["yr_ent_age"] as? Int,
              let _ = configs["bool_ent_smoker"] as? Bool,
              let _ = configs["bool_ent_hypertension"] as? Bool,
              let _ = configs["bool_ent_bloodPressureMedication"] as? Bool,
              let isDiabetic = configs["enum_ent_diabetic"] as? String,
              let height = configs["cm_ent_height"] as? Int,
              let weight = configs["kg_ent_weight"] as? Int,
              (height >= 25 && height <= 300),
              (weight >= 25 && weight <= 300),
              (age >= 13 && age <= 120)
        else {
            return false
        }
        return ["none", "type1", "type2"].contains(isDiabetic)
    }
    
    /// BodyScan config requirements validation.
    ///
    /// Please see the Schemas for more information:
    /// BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/
    public func areBodyScanConfigOptionsValid(faceScanInput configs: [String: Any]) -> Bool {
        if !areSharedScanConfigOptionsValid(scanInput: configs) {
            return false
        }
        guard let height = configs["cm_ent_height"] as? Int,
              let weight = configs["kg_ent_weight"] as? Int,
              (height >= 50 && height <= 255),
              (weight >= 16 && weight <= 300) else {
            return false
        }
        return true
    }
}

// MARK: Persistence Delegate Smoothing

extension NSObject {
    /// Confirm results have correct set of keys.
    public func areBodyScanSmoothingResultsValid(bodyScanResults results: [[String: Any]]) -> Bool {
        // Your token may only provide you access to a smaller subset of results.
        // You should modify this list based on your available config options.
        let required: Set = [
            "enum_ent_sex",
            "cm_ent_height",
            "kg_ent_weight",
            "cm_raw_chest",
            "cm_raw_hips",
            "cm_raw_inseam",
            "cm_raw_thigh",
            "cm_raw_waist",
            "kg_raw_weightPredict",
            "ml_raw_fitness",
            "percent_raw_bodyFat",
            "id",
            "date"
        ]
        // Iterate over results
        for result in results {
            // Check if keys in results contains the required keys.
            let keys = Set(Array(result.keys))
            if !required.isSubset(of: keys) {
                return false
            }
        }
        return true
    }
}

// MARK: Bytes Helper

extension NSObject {
    /// Recieves the download size in bytes and returns the conversion in MB.
    ///
    /// Use the iOS native ByteFormatter to convert the bytes value to a MB String.
    public func convertBytesToMBString(_ bytes: Int) -> String {
        let byteFormatter = ByteCountFormatter()
        byteFormatter.allowedUnits = .useMB
        byteFormatter.countStyle = .binary
        return byteFormatter.string(fromByteCount: Int64(bytes))
    }
}

// MARK: - ViewController Helper

extension NSObject {
    /// Return topmost viewController to initiate face and bodyscan
    public func topMostVC() -> UIViewController? {
        let keyWindow = UIApplication.shared.windows.filter {$0.isKeyWindow}.first
        if var topController = keyWindow?.rootViewController {
            while let presentedViewController = topController.presentedViewController {
                topController = presentedViewController
            }
            return topController
        }
        return nil
    }
}
