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

import SwiftUI
import UIKit
// The MultiScan SDK
import AHIMultiScan
// The Body Scan SDK
import AHIBodyScan
// The FaceScan SDK
import AHIFaceScan
// The FingerScan SDK
import AHIFingerScan

/// The required tokens for the MultiScan Setup and Authorization.
public struct AHIConfigTokens {
    /// Your AHI MultiScan token
    static let AHI_MULTI_SCAN_TOKEN = ""
    /// Your user ID. NOTE: User ID is hard-coded here for example, BUT should NOT be hard-coded in real integration (user ID from idP is expected).
    static let AHI_TEST_USER_ID = "EXAMPLE_USER_ID"
    /// Security salt value. This should be hard-coded into your app, and SHOULD NOT be changed (i.e. be the same in both iOS and Android). It can be any string value.
    static let AHI_TEST_USER_SALT = "EXAMPLE_APP_SALT"
    /// Claims are optional values to increase the security for the user. The order and values should be unique for a given user and be the same on both iOS and Android (e.g. user join date in the format "yyyy", "mm", "dd", "zzzz").
    static let AHI_TEST_USER_CLAIMS = ["EXAMPLE_CLAIM"]
}

struct ContentView: View {
    @ObservedObject var multiScan = AHISDKManager()
    @State private var buttonHeight = 55.0
    @State private var buttonInset = 16.0
    
    var body: some View {
        VStack() {
            Button (action:{
                if multiScan.isSetup {
                    didTapStartFaceScan()
                } else {
                    didTapSetup()
                }
                }, label: {
                    Text(multiScan.isSetup ? "Start FaceScan" : "Setup SDK")
                        .foregroundColor(Color.white)
                        .frame(maxWidth: .infinity)
                        })
            .frame(height: buttonHeight)
            .background(Color.black)
            
            Button (action:{
                didTapStartFingerScan()
            }, label: {
                Text("Start Finger")
                    .foregroundColor(Color.white)
                    .frame(maxWidth: .infinity)
            })
            .frame(height: buttonHeight)
            .background(Color.black)
            .hidden(!multiScan.isSetup)
            
            Button (action:{
                if multiScan.isFinishedDownloadingResources {
                    didTapStartBodyScan()
                } else {
                    didTapDownloadResources()
                }
            }, label: {
                Text(multiScan.isFinishedDownloadingResources ? "Start BodyScan" : "Download Resources")
                    .foregroundColor(Color.white)
                    .frame(maxWidth: .infinity)
            })
            .frame(height: buttonHeight)
            .background(Color.black)
            .hidden(!multiScan.isSetup)
            .disabled(multiScan.isDownloadInProgress)
            .opacity(multiScan.isDownloadInProgress ? 0.5 : 1)
            Spacer()
        }
        .padding(EdgeInsets(top: buttonHeight + buttonInset, leading: buttonInset, bottom: buttonInset, trailing: buttonInset))
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

// MARK: - Actions

extension ContentView {
    func didTapSetup() {
        multiScan.setupMultiScanSDK()
    }
    
    func didTapStartFaceScan() {
        multiScan.startFaceScan()
    }
    
    func didTapStartFingerScan() {
        multiScan.startFingerScan()
    }

    func didTapStartBodyScan() {
        multiScan.startBodyScan()
    }
    
    func didTapDownloadResources() {
        multiScan.downloadAHIResources()
        multiScan.areAHIResourcesAvailable()
        multiScan.checkAHIResourcesDownloadSize()
    }
}

// MARK: - AHI MultiScan SDK Manager

class AHISDKManager: NSObject, ObservableObject {
    /// Default state of the app when launched is that the AHI MultiScan SDK is not setup.
    ///
    ///  When the Setup has been completed options for scans will appear.
    @Published var isSetup = false
    @Published var isFinishedDownloadingResources = false
    @Published var isDownloadInProgress = false
    
    // MARK: Scan Instances

    /// Instance of AHI MultiScan
    let ahi = MultiScan.shared()
    /// Instance of AHI FaceScan
    let faceScan = FaceScan()
    /// Instance of AHI FaceScan
    let fingerScan = FingerScan()
    /// Instance of AHI BodyScan
    let bodyScan = BodyScan()
    
    public override init() {
        super.init()
        // Set persistence delegate
        ahi.delegatePersistence = self
    }
}

// MARK: - AHI Multi Scan SDK Setup

extension AHISDKManager {
    /// Setup the MultiScan SDK
    ///
    /// This must happen before requesting a scan.
    /// We recommend doing this on successfuil load of your application.
    fileprivate func setupMultiScanSDK() {
        bodyScan.setEventListener(self)
        ahi.setup(withConfig: ["TOKEN": AHIConfigTokens.AHI_MULTI_SCAN_TOKEN], scans: [faceScan, fingerScan, bodyScan]) { error in
            if let err = error {
                print("AHI: Error setting up: \(err)")
                print("AHI: Confirm you have a valid token.")
                return
            }
            self.authorizeUser()
        }
    }

    /// Once successfully setup, you should authorize your user with our service.
    ///
    /// With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
    fileprivate func authorizeUser() {
        ahi.userAuthorize(forId: AHIConfigTokens.AHI_TEST_USER_ID, withSalt: AHIConfigTokens.AHI_TEST_USER_SALT, withClaims: AHIConfigTokens.AHI_TEST_USER_CLAIMS) { authError in
            if let err = authError {
                print("AHI: Auth Error: \(err)")
                print("AHI: Confirm you are using a valid user id, salt and claims")
                return
            } else {
                print("AHI: Setup user successfully")
                DispatchQueue.main.async {
                    self.isSetup = true
                }
                return
            }
        }
    }
}

// MARK: - AHI Multi Scan Remote Resources

extension AHISDKManager {
    /// Check if the AHI resources are downloaded.
    ///
    /// We have remote resources that exceed 100MB that enable our scans to work.
    /// You are required to download them inorder to obtain a body scan.
    fileprivate func areAHIResourcesAvailable() {
        ahi.areResourcesDownloaded { [weak self] success, error in
            if !success {
                print("AHI INFO: Resources are not downloaded, Error: \(error)")
                self?.isDownloadInProgress = true
                weak var weakSelf = self
                // We recommend polling to check resource state.
                // This is a simple example of how.
                DispatchQueue.main.asyncAfter(deadline: .now() + 30.0) {
                    weakSelf?.checkAHIResourcesDownloadSize()
                    weakSelf?.areAHIResourcesAvailable()
                }
                return
            }
            DispatchQueue.main.async {
                self?.isFinishedDownloadingResources = success
                self?.isDownloadInProgress = false
                print("AHI: Resources ready")
            }
            return
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
        ahi.totalEstimatedDownloadSizeInBytes { [weak self] bytes, totalBytes, error in
            print("AHI INFO: Size of download is \(self?.convertBytesToMBString(Int(bytes)) ?? "0") / \(self?.convertBytesToMBString(Int(totalBytes)) ?? "0")")
        }
    }
}

// MARK: - AHI Face Scan Initialiser

extension AHISDKManager {
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
        ahi.initiateScan("face", withOptions: options, from: vc) { scanTask, error in
            guard let task = scanTask, error == nil else {
                if let nsError = error as? NSError, nsError.code == AHIFaceScanErrorCode.ScanCanceled.rawValue {
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
                // Handle failure.
                return nil
            })
        }
    }
}


// MARK: - AHI Finger Scan Initialiser

extension AHISDKManager {
    fileprivate func startFingerScan() {
        // All required finger scan options.
        let options: [String : Any] = [
            "sec_ent_scanLength": 60
        ]
        if !areFingerScanConfigOptionsValid(fingerScanInput: options) {
            print("AHI ERROR: Finger Scan inputs invalid.")
            return
        }
        // Ensure the view controller being used is the top one.
        // If you are not attempting to get a scan simultaneous with dismissing your calling view controller, or attempting to present from a view controller lower in the stack
        // you may have issues.
        guard let vc = topMostVC() else { return }
        ahi.initiateScan("finger", withOptions: options, from: vc) { scanTask, error in
            guard let task = scanTask, error == nil else {
                if let nsError = error as? NSError, nsError.code == AHIFingerScanErrorCode.codeScanCanceled.rawValue {
                    print("AHI: INFO: User cancelled the session.")
                } else {
                    // Handle error through either lack of results or error.
                    print("AHI: ERROR WITH FINGER SCAN: \(error ?? NSError())")
                }
                return
            }
            task.continueWith(block: { resultsTask in
                if let results = resultsTask.result as? [String : Any] {
                    // Handle results
                    print("AHI: SCAN RESULTS: \(results)")
                }
                // Handle failure.
                return nil
            })
        }
    }
}

// MARK: - AHI Body Scan Initialiser

extension AHISDKManager: AHIBSEventListenerDelegate {
    fileprivate func startBodyScan() {
        // All required body scan options
        let options: [String : Any] = [
            "enum_ent_sex": "male",
            "cm_ent_height": 180,
            "kg_ent_weight": 85
        ]
        if !areBodyScanConfigOptionsValid(bodyScanInput: options) {
            print("AHI ERROR: Body Scan inputs invalid.")
            return
        }
        // Ensure the view controller being used is the top one.
        // If you are not attempting to get a scan simultaneous with dismissing your calling view controller, or attempting to present from a view controller lower in the stack
        // you may have issues.
        guard let vc = topMostVC() else { return }
        ahi.initiateScan("body", withOptions: options, from: vc) { [weak self] scanTask, error in
            guard let task = scanTask, error == nil else {
                // Error code 2011 is the code for the SDK interaction that cancels the scan.
                if let nsError = error as? NSError, nsError.code == 2011 {
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
                // Handle failure.
                return nil
            })
        }
    }
    
    func event(_ name: String, meta: [String : Any]?) {
        print("event: \(name)")
    }
}

// MARK: - Body Scan Extras

extension AHISDKManager {
    /// Use this function to fetch the 3D avatar mesh.
    ///
    /// The 3D mesh can be created and returned at any time.
    /// We recommend doing this on successful completion of a body scan with the results.
    fileprivate func getBodyScanExtras(withBodyScanResult result: [String: Any]) {
        ahi.getExtra(["body": [result]], query: ["extrapolate": ["mesh"]]) { extras, error in
            guard let extras = extras, error == nil else {
                print("AHI: ERROR GETTING BODY SCAN EXTRAS. \(error ?? NSError())")
                return
            }
            print("AHI EXTRAS: \(extras)")
            // The mesh is returned as a URL that stored the file in the app cache.
            if let meshResult = extras["extrapolate"]?.first as? Dictionary<String, Any>, let meshURL = meshResult["mesh"] as? URL {
                print("AHI: Mesh URL: \(meshURL)")
            }
        }
    }
}

// MARK: - AHI MultiScan Optional Functions

extension AHISDKManager {
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
        ahi.userIsAuthorized { isAuthorized, userId, error in
            print("AHI INFO: User is \(isAuthorized ? "authorized" : "not authorized")")
        }
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
                self?.isSetup = false
            }
        }
    }
}

// MARK: - Persistence Delegate example

/// If you choose to use this, you will obtain two sets of results - one containing the "raw" output and another set containing "adj" output.
/// "adj" means adjusted and is used to help provide historical results as a reference for the newest result to provide tailored to the user results.
/// We recommend using this for individual users results; avoid using this if the app is a single user ID with multiple users results.
/// More info found here: https://docs.advancedhumanimaging.io/MultiScan%20SDK/Data/
extension AHISDKManager: AHIDelegatePersistence {
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
    /// FingerScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FingerScan/Schemas/
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
    
    /// FingerScan config requirements validation.
    ///
    /// Please see the Schemas for more information:
    /// FingerScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FingerScan/Schemas/
    public func areFingerScanConfigOptionsValid(fingerScanInput configs: [String: Any]) -> Bool {
        
        guard let scanLength = configs["sec_ent_scanLength"] as? Int,
              (scanLength >= 20) else {
            return false
        }
        return true
    }
    

    /// BodyScan config requirements validation.
    ///
    /// Please see the Schemas for more information:
    /// BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/
    public func areBodyScanConfigOptionsValid(bodyScanInput configs: [String: Any]) -> Bool {
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

// MARK: - View Helper

extension View {
    @ViewBuilder func hidden(_ shouldHide: Bool) -> some View {
        switch shouldHide {
        case true: self.hidden()
        case false: self
        }
    }
}
