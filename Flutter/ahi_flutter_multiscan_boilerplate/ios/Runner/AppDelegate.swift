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
import AHIBodyScan
// The FaceScan SDK
import AHIFaceScan
// The FingerScan SDK
import AHIFingerScan

private let CHANNEL = "ahi_multiscan_flutter_wrapper"
private let EVENT_CHANNEL = "ahi_multiscan_flutter_event_channel"

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate, FlutterStreamHandler {
    /// All the possible methods that can be called.
    private enum AHIMultiScanMethod: String {
        /// Default.
        case unknown = ""
        /// Requires a token String to be provided as an argument.
        case setupMultiScanSDK = "setupMultiScanSDK"
        /// Requires a Map object to be passed in containing 3 arguments.
        case authorizeUser = "authorizeUser"
        /// Will return a boolean
        case areAHIResourcesAvailable = "areAHIResourcesAvailable"
        /// A void function that will invoke the download of remote resources.
        case downloadAHIResources = "downloadAHIResources"
        /// Will return an integer for the bytes size.
        case checkAHIResourcesDownloadSize = "checkAHIResourcesDownloadSize"
        /// Requires a map object for the required user inputs
        case startFaceScan = "startFaceScan"
        /// Requires a map object for the required user inputs
        case startFingerScan = "startFingerScan"
        /// Requires a map object for the required user inputs 
        case startBodyScan = "startBodyScan"
        /// Requires a map object of the body scan results and returns a Map object.
        case getBodyScanExtras = "getBodyScanExtras"
        /// Returns the SDK status
        case getMultiScanStatus = "getMultiScanStatus"
        /// Returns a Map containing the SDK details.
        case getMultiScanDetails = "getMultiScanDetails"
        /// Returns the user authorization status of the SDK.
        case getUserAuthorizedState = "getUserAuthorizedState"
        /// Will deuathorize the user from the SDK.
        case deauthorizeUser = "deauthorizeUser"
        /// Released the actively registered SDK session.
        case releaseMultiScanSDK = "releaseMultiScanSDK"
        /// Use the AHIMultiScan persistence delegate and set historical body scan results
        case setMultiScanPersistenceDelegate = "setMultiScanPersistenceDelegate"
    }

    /// Event sink used to send scan status events back to the Flutter code
    fileprivate var eventSink: FlutterEventSink?
    fileprivate let multiScan = AHIMultiScanModule()
    override func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        setupFlutter()
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

extension AppDelegate {
    fileprivate func setupFlutter() {
        let controller: FlutterViewController = window?.rootViewController as! FlutterViewController
        let boilerPlateChannel = FlutterMethodChannel(name: CHANNEL, binaryMessenger: controller.binaryMessenger)
        let eventBoilerPlateChannel = FlutterEventChannel(name: EVENT_CHANNEL, binaryMessenger: controller.binaryMessenger)
        eventBoilerPlateChannel.setStreamHandler(self)
        boilerPlateChannel.setMethodCallHandler({
            [weak self] (call: FlutterMethodCall, resultHandler: @escaping FlutterResult) -> Void in
            guard let weakSelf = self else {
                resultHandler("Error: self was nil when trying to call iOS method")
                return
            }
            let method = AHIMultiScanMethod(rawValue: call.method)
            switch method {
                case .setupMultiScanSDK:
                    weakSelf.multiScan.setupMultiScanSDK(token: call.arguments, resultHandler: resultHandler)
                    break
                case .authorizeUser:
                    weakSelf.authorizeUser(arguments: call.arguments, resultHandler: resultHandler)
                    break
                case .areAHIResourcesAvailable:
                    weakSelf.multiScan.areAHIResourcesAvailable(resultHandler: resultHandler)
                    break
                case .downloadAHIResources:
                    weakSelf.multiScan.downloadAHIResources()
                    break
                case .checkAHIResourcesDownloadSize:
                    weakSelf.multiScan.checkAHIResourcesDownloadSize(resultHandler: resultHandler)
                    break
                case .startFaceScan:
                    weakSelf.startFaceScan(arguments: call.arguments, resultHandler: resultHandler)
                    break
                case .startFingerScan:
                    weakSelf.startFingerScan(arguments: call.arguments, resultHandler: resultHandler)
                    break
                case .startBodyScan:
                    weakSelf.startBodyScan(arguments: call.arguments, resultHandler: resultHandler)
                    break
                case .getBodyScanExtras:
                    weakSelf.multiScan.getBodyScanExtras(withBodyScanResult: call.arguments, resultHandler: resultHandler)
                    break
                case .getMultiScanStatus:
                    weakSelf.multiScan.getMultiScanStatus(resultHandler: resultHandler)
                    break
                case .getMultiScanDetails:
                    weakSelf.multiScan.getMultiScanDetails(resultHandler: resultHandler)
                    break
                case .getUserAuthorizedState:
                    weakSelf.multiScan.getUserAuthorizedState(resultHandler: resultHandler)
                    break
                case .deauthorizeUser:
                    weakSelf.multiScan.deauthorizeUser(resultHandler: resultHandler)
                    break
                case .releaseMultiScanSDK:
                    weakSelf.multiScan.releaseMultiScanSDK(resultHandler: resultHandler)
                    break
                case .setMultiScanPersistenceDelegate:
                    weakSelf.multiScan.setBodyScanResults(results: call.arguments)
                default:
                    print("AHI: Invalid method name.")
            }
        })
        GeneratedPluginRegistrant.register(with: self)
    }

    fileprivate func authorizeUser(arguments: Any?, resultHandler: @escaping FlutterResult) {
        guard let args = arguments as? [String: Any],
              let userID = args["USER_ID"] as? String,
              let salt = args["SALT"] as? String,
              let claims = args["CLAIMS"] as? [String]
        else {
            resultHandler(FlutterError(code: "-2", message: "Missing user authorization details.", details: nil))
            return
        }
        multiScan.authorizeUser(userID: userID, salt: salt, claims: claims, resultHandler: resultHandler)
    }

    fileprivate func startFaceScan(arguments: Any?, resultHandler: @escaping FlutterResult) {
        guard let args = arguments as? [String: Any],
              let enum_ent_sex = args["enum_ent_sex"] as? String,
                let cm_ent_height = args["cm_ent_height"] as? Int,
                let kg_ent_weight = args["kg_ent_weight"] as? Int,
                let yr_ent_age = args["yr_ent_age"] as? Int,
                let bool_ent_smoker = args["bool_ent_smoker"] as? Bool,
                let bool_ent_hypertension = args["bool_ent_hypertension"] as? Bool,
                let bool_ent_bloodPressureMedication = args["bool_ent_bloodPressureMedication"] as? Bool,
                let enum_ent_diabetic = args["enum_ent_diabetic"] as? String
        else {
            resultHandler(FlutterError(code: "-3", message: "Missing user face scan input details.", details: nil))
            return
        }
        let userInputs: [String : Any] = [
            "enum_ent_sex": enum_ent_sex,
            "cm_ent_height": cm_ent_height,
            "kg_ent_weight": kg_ent_weight,
            "yr_ent_age": yr_ent_age,
            "bool_ent_smoker": bool_ent_smoker,
            "bool_ent_hypertension": bool_ent_hypertension,
            "bool_ent_bloodPressureMedication": bool_ent_bloodPressureMedication,
            "enum_ent_diabetic": enum_ent_diabetic,
        ]
        multiScan.startFaceScan(userInputs: userInputs, resultHandler: resultHandler)
    }

    fileprivate func startFingerScan(arguments: Any?, resultHandler: @escaping FlutterResult) {
        guard let args = arguments as? [String: Any],
              let sec_ent_scanLength = args["sec_ent_scanLength"] as? Int,
              let str_ent_instruction1 = args["str_ent_instruction1"] as? String,
              let str_ent_instruction2 = args["str_ent_instruction2"] as? String
        else {
            resultHandler(FlutterError(code: "-3", message: "Missing user finger scan input details.", details: nil))
            return
        }
        let userInputs: [String : Any] = [
            "sec_ent_scanLength": sec_ent_scanLength,
            "str_ent_instruction1": str_ent_instruction1,
            "str_ent_instruction2": str_ent_instruction2,
        ]
        multiScan.startFingerScan(userInputs: userInputs, resultHandler: resultHandler)
    }

    fileprivate func startBodyScan(arguments: Any?, resultHandler: @escaping FlutterResult) {
        guard let args = arguments as? [String: Any],
              let enum_ent_sex = args["enum_ent_sex"] as? String,
              let cm_ent_height = args["cm_ent_height"] as? Int,
              let kg_ent_weight = args["kg_ent_weight"] as? Int
        else {
            resultHandler(FlutterError(code: "-5", message: "Missing user body scan input details.", details: nil))
            return
        }
        let userInputs: [String : Any] = [
            "enum_ent_sex": enum_ent_sex,
            "cm_ent_height": cm_ent_height,
            "kg_ent_weight": kg_ent_weight
        ]
        multiScan.startBodyScan(userInputs: userInputs, resultHandler: resultHandler)
    }
}

// MARK: - AHI MultiScan Module

private class AHIMultiScanModule: NSObject {
    
    // MARK: Scan Instances
    
    /// Instance of AHI MultiScan
    let ahi = MultiScan.shared()
    /// Instance of AHI FaceScan
    let faceScan = FaceScan()
    /// Instance of AHI FigerScan
    let fingerScan = FingerScan()
    /// Instance of AHI BodyScan
    let bodyScan = BodyScan()
    /// Body Scan Results
    var bodyScanResults = [[String: Any]]()
    
    public override init() {
        super.init()
    }
}

// MARK: - MultiScan SDK Setup Functions

extension AHIMultiScanModule {
    /// Setup the MultiScan SDK
    ///
    /// This must happen before requesting a scan.
    /// We recommend doing this on successfuil load of your application.
    fileprivate func setupMultiScanSDK(token: Any?, resultHandler: @escaping FlutterResult) {
        guard let token = token as? String else {
            resultHandler(FlutterError(code: "-1", message: "Missing multi scan token", details: nil))
            return
        }
        ahi.setup(withConfig: ["TOKEN": token], scans: [faceScan, fingerScan, bodyScan]) { [weak self] error in
            if let err = error {
                resultHandler(self?.createFlutterError(fromError: err))
                return
            }
            resultHandler(nil)
        }
    }
    
    /// Once successfully setup, you should authorize your user with our service.
    ///
    /// With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
    fileprivate func authorizeUser(userID: String, salt: String, claims: [String], resultHandler: @escaping FlutterResult) {
        ahi.userAuthorize(forId: userID, withSalt: salt, withClaims: claims) { [weak self] authError in
            if let err = authError {
                resultHandler(self?.createFlutterError(fromError: err))
                return
            }
            resultHandler(nil)
        }
    }
}

// MARK: - AHI Multi Scan Remote Resources

extension AHIMultiScanModule {
    /// Check if the AHI resources are downloaded.
    ///
    /// We have remote resources that exceed 100MB that enable our scans to work.
    /// You are required to download them inorder to obtain a body scan.
    ///
    /// This function checks if they are already downloaded and available for use.
    fileprivate func areAHIResourcesAvailable(resultHandler: @escaping FlutterResult) {
        ahi.areResourcesDownloaded { success, error in
            resultHandler(success)
        }
    }
    
    /// Download scan resources.
    ///
    /// We recomment only calling this function once per session to prevent duplicate background resource calls.
    fileprivate func downloadAHIResources() {
        ahi.delegateDownloadProgress = self
        ahi.downloadResourcesInBackground()
    }
    
    /// Check the size of the AHI resources that require downloading.
    fileprivate func checkAHIResourcesDownloadSize(resultHandler: @escaping FlutterResult) {
        ahi.totalEstimatedDownloadSizeInBytes { bytes, totalBytes, error in
            resultHandler(Int64(bytes))
        }
    }
}

// MARK: - AHI Face Scan Initialiser

extension AHIMultiScanModule {
    fileprivate func startFaceScan(userInputs: [String: Any], resultHandler: @escaping FlutterResult) {
        // Ensure the view controller being used is the top one.
        // If you are not attempting to get a scan simultaneous with dismissing your calling view controller, or attempting to present from a view controller lower in the stack
        // you may have issues.
        guard let vc = topMostVC() else { return }
        ahi.initiateScan("face", withOptions: userInputs, from: vc) { [weak self] scanTask, error in
            guard let task = scanTask, error == nil else {
                resultHandler(self?.createFlutterError(fromError: error))
                return
            }
            task.continueWith(block: { resultsTask in
                if let results = resultsTask.result as? [String : Any] {
                    resultHandler(results)
                } else {
                    resultHandler(nil)
                }
                return nil
            })
        }
    }
}


// MARK: - AHI Finger Scan Initialiser

extension AHIMultiScanModule {
    fileprivate func startFingerScan(userInputs: [String: Any], resultHandler: @escaping FlutterResult) {
        // Ensure the view controller being used is the top one.
        // If you are not attempting to get a scan simultaneous with dismissing your calling view controller, or attempting to present from a view controller lower in the stack
        // you may have issues.
        guard let vc = topMostVC() else { return }
        ahi.initiateScan("finger", withOptions: userInputs, from: vc) { [weak self] scanTask, error in
            guard let task = scanTask, error == nil else {
                resultHandler(self?.createFlutterError(fromError: error))
                return
            }
            task.continueWith(block: { resultsTask in
                if let results = resultsTask.result as? [String : Any] {
                    resultHandler(results)
                } else {
                    resultHandler(nil)
                }
                return nil
            })
        }
    }
}

// MARK: - AHI Body Scan Initialiser

extension AHIMultiScanModule {
    fileprivate func startBodyScan(userInputs: [String: Any], resultHandler: @escaping FlutterResult) {
        // Ensure the view controller being used is the top one.
        // If you are not attempting to get a scan simultaneous with dismissing your calling view controller, or attempting to present from a view controller lower in the stack
        // you may have issues.
        guard let vc = topMostVC() else { return }
        ahi.initiateScan("body", withOptions: userInputs, from: vc) {
            [weak self] scanTask, error in
            guard let task = scanTask, error == nil else {
                resultHandler(self?.createFlutterError(fromError: error))
                return
            }
            task.continueWith(block: { resultsTask in
                if let results = resultsTask.result as? [String : Any] {
                    resultHandler(results)
                } else {
                    resultHandler(nil)
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
    fileprivate func getBodyScanExtras(withBodyScanResult result: Any?, resultHandler: @escaping FlutterResult) {
        guard let bodyScanResult = result as? [String: Any] else {
            resultHandler(FlutterError(code: "-8", message: "Missing valid body scan result.", details: nil))
            return
        }
        ahi.getExtra(["body": [result ?? [:]]], query: ["extrapolate": ["mesh"]]) { [weak self] extras, error in
            guard let extras = extras, error == nil else {
                resultHandler(self?.createFlutterError(fromError: error))
                return
            }
            
            var bsExtras = [String: String]()
            if let meshResult = extras["extrapolate"]?.first as? Dictionary<String, Any>, let meshURL = meshResult["mesh"] as? URL {
                // This may require being relative path over absoluteString.
                // Would recommend in session moving the mesh to another file controlled by the app.
                bsExtras["meshURL"] = meshURL.absoluteString
            } else {
                bsExtras["meshURL"] = ""
            }
            resultHandler(bsExtras)
        }
    }
}

// MARK: - AHI MultiScan Optional Functions

extension AHIMultiScanModule {
    /// Check if MultiScan is on or offline.
    fileprivate func getMultiScanStatus(resultHandler: @escaping FlutterResult) {
        ahi.status { multiScanStatus in
            resultHandler("\(multiScanStatus)")
        }
    }
    
    /// Check your AHI MultiScan organisation  details.
    fileprivate func getMultiScanDetails(resultHandler: @escaping FlutterResult) {
        if let details = ahi.getDetails() {
            resultHandler("\(details)")
        } else {
            resultHandler(nil)
        }
    }
    
    /// Check if the user is authorized to use the MuiltScan service.
    fileprivate func getUserAuthorizedState(resultHandler: @escaping FlutterResult) {
        ahi.userIsAuthorized { isAuthorized, partnerUserId, error in
            resultHandler(isAuthorized)
        }
    }
    
    /// Deuauthorize the user.
    fileprivate func deauthorizeUser(resultHandler: @escaping FlutterResult) {
        ahi.userDeauthorize { [weak self] error in
            resultHandler(self?.createFlutterError(fromError: error))
        }
    }
    
    /// Release the MultiScan SDK session.
    ///
    /// If you  use this, you will need to call setupSDK again.
    fileprivate func releaseMultiScanSDK(resultHandler: @escaping FlutterResult) {
        ahi.releaseSDK { [weak self] error in
            resultHandler(self?.createFlutterError(fromError: error))
        }
    }
}

// MARK: - Persistence Delegate example

// If you choose to use this, you will obtain two sets of results - one containing the "raw" output and another set containing "adj" output.
// "adj" means adjusted and is used to help provide historical results as a reference for the newest result to provide tailored to the user results.
// We recommend using this for individual users results; avoid using this if the app is a single user ID with multiple users results.
// More info found here: https://docs.advancedhumanimaging.io/MultiScan%20SDK/Data/
extension AHIMultiScanModule: AHIDelegatePersistence {

    public func setBodyScanResults(results: Any?) {
        guard let bsResults = results as? [[String: Any]] else {
            print("AHI: Results must not be nil and must conform to an Array of Map results.")
            return
        }
        ahi.delegatePersistence = self
        bodyScanResults = bsResults
    }

    func requestScanType(_ scan: String, options: [String : Any] = [:], completion completionBlock: @escaping (Error?, [[String : Any]]?) -> Void) {
        // Call the completion block to return the results to the SDK.
        completionBlock(nil, bodyScanResults)
    }
}

// MARK: - Download Progress report example
extension AHIMultiScanModule:AHIDelegateDownloadProgress{
    func downloadProgressReport(_ error: Error?) {
        if(error != nil){
            ((UIApplication.shared.delegate as? AppDelegate)?.eventSink?("failed"))
            return
        }
        DispatchQueue.main.sync {
            ahi.totalEstimatedDownloadSizeInBytes(){ bytes, totalBytes, error in
                if(bytes>=totalBytes){
                    ((UIApplication.shared.delegate as? AppDelegate)?.eventSink?("done"))
                }else {
                    ((UIApplication.shared.delegate as? AppDelegate)?.eventSink?("\(bytes):\(totalBytes)"))
                }
            }
        }
    }
}


// MARK: - Error Safety

extension AHIMultiScanModule {
    private func createFlutterError(fromError error: Error?) -> FlutterError? {
        guard let err = error as? NSError else {
            return nil
        }
        let errCode = (err.code == AHIFingerScanErrorCode.codeScanCanceled.rawValue ||
                       err.code == AHIFaceScanErrorCode.ScanCanceled.rawValue) ? "USER_CANCELLED" : "\(err.code)"
        var errMessage = err.localizedDescription
        if errMessage.isEmpty {
            errMessage = "Unknown error occurred. Please contact developer support."
        }
        return FlutterError(code: "\(errCode)", message: errMessage, details: nil)
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
