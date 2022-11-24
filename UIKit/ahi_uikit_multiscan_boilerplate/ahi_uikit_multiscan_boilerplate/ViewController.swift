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

class ViewController: UIViewController {

    // MARK: Variables

    /// Default state of the app when launched is that the AHI MultiScan SDK is not setup.
    ///
    ///  When the Setup has been completed options for scans will appear.
    var isSetup = false {
        didSet {
            weak var weakSelf = self
            DispatchQueue.main.async {
                guard let self = weakSelf else {
                    return
                }
                self.setupButton.isHidden = self.isSetup
                self.startFaceScanButton.isHidden = !self.isSetup
                self.startFingerScanButton.isHidden = !self.isSetup
                self.downloadResourcesButton.isHidden = !self.isSetup
            }
        }
    }
    /// Default state of the app when launched is that the AHI MultiScan SDK resources are not available.
    ///
    ///  When the resources have been confirmed as downloaded and ready, BodyScan option will appear.
    var isFinishedDownloadingResources = false {
        didSet {
            weak var weakSelf = self
            DispatchQueue.main.async {
                guard let self = weakSelf else {
                    return
                }
                self.downloadResourcesButton.isHidden = self.isFinishedDownloadingResources
                self.startBodyScanButton.isHidden = !self.isFinishedDownloadingResources
            }
        }
    }
    /// Only need to set the constraints once.
    ///
    /// When the app launches, the subviews will be added to the view.
    /// Constraints will then be applied to these subviews and this flag will become true.
    var hasSetConstraints = false

    // MARK: Scan Instances

    /// Instance of AHI MultiScan
    let ahi = MultiScan.shared()
    /// Instance of AHI FaceScan
    let faceScan = FaceScan()
    /// Instance of AHI FingerScacn
    let fingerScan = FingerScan()
    /// Instance of AHI BodyScan
    let bodyScan = BodyScan()

    // MARK: View Components

    private func createButton(withTitle title: String, action: Selector) -> UIButton {
        let ub = UIButton()
        ub.translatesAutoresizingMaskIntoConstraints = false
        ub.backgroundColor = .black
        ub.setTitleColor(.white, for: .normal)
        ub.setTitle(title, for: .normal)
        ub.addTarget(self, action: action, for: .touchUpInside)
        return ub
    }

    /// Button to invoke  setup of the SDK
    lazy var setupButton: UIButton = {
        return createButton(withTitle: "Setup SDK", action: #selector(didTapSetup))
    }()
    /// Button to invoke face scan.
    /// Is hiddden until successful setup.
    lazy var startFaceScanButton: UIButton = {
        let ub = createButton(withTitle: "Start FaceScan", action: #selector(didTapStartFaceScan))
        ub.isHidden = true
        return ub
    }()
    /// Button to invoke finger scan.
    /// Is hiddden until successful setup.
    lazy var startFingerScanButton: UIButton = {
        let ub = createButton(withTitle: "Start FingerScan", action: #selector(didTapStartFingerScan))
        ub.isHidden = true
        return ub
    }()
    
    /// Button to invoke face scan.
    /// Is hiddden until successful download of resources.
    lazy var startBodyScanButton: UIButton = {
        let ub = createButton(withTitle: "Start BodyScan", action: #selector(didTapStartBodyScan))
        ub.isHidden = true
        return ub
    }()
    /// Button to invoke face scan.
    /// Is hiddden until successful setup.
    lazy var downloadResourcesButton: UIButton = {
        let ub = createButton(withTitle: "Download Resources", action: #selector(didTapDownloadResources))
        ub.isHidden = true
        return ub
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .white
        view.addSubview(setupButton)
        view.addSubview(startFaceScanButton)
        view.addSubview(startFingerScanButton)
        view.addSubview(startBodyScanButton)
        view.addSubview(downloadResourcesButton)
        updateViewConstraints()
        ahi.delegatePersistence = self
        bodyScan.setEventListener(self)
    }

    // MARK: Constraints

    override func updateViewConstraints() {
        super.updateViewConstraints()
        if hasSetConstraints {
            return
        }
        hasSetConstraints = true
        let inset: CGFloat = 16.0
        let buttonHeight: CGFloat = 55.0
        setupButton.heightAnchor.constraint(equalToConstant: buttonHeight).isActive = true
        setupButton.leftAnchor.constraint(equalTo: view.leftAnchor, constant: inset).isActive = true
        setupButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -inset).isActive = true
        setupButton.safeAreaLayoutGuide.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: (buttonHeight + inset)).isActive = true
        startFaceScanButton.heightAnchor.constraint(equalToConstant: buttonHeight).isActive = true
        startFaceScanButton.leftAnchor.constraint(equalTo: view.leftAnchor, constant: inset).isActive = true
        startFaceScanButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -inset).isActive = true
        startFaceScanButton.safeAreaLayoutGuide.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: (buttonHeight + inset)).isActive = true
        startFingerScanButton.heightAnchor.constraint(equalToConstant: buttonHeight).isActive = true
        startFingerScanButton.leftAnchor.constraint(equalTo: view.leftAnchor, constant: inset).isActive = true
        startFingerScanButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -inset).isActive = true
        startFingerScanButton.topAnchor.constraint(equalTo: startFaceScanButton.bottomAnchor, constant: inset).isActive = true
        startBodyScanButton.heightAnchor.constraint(equalToConstant: buttonHeight).isActive = true
        startBodyScanButton.leftAnchor.constraint(equalTo: view.leftAnchor, constant: inset).isActive = true
        startBodyScanButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -inset).isActive = true
        startBodyScanButton.topAnchor.constraint(equalTo: startFingerScanButton.bottomAnchor, constant: inset).isActive = true
        downloadResourcesButton.heightAnchor.constraint(equalToConstant: buttonHeight).isActive = true
        downloadResourcesButton.leftAnchor.constraint(equalTo: view.leftAnchor, constant: inset).isActive = true
        downloadResourcesButton.rightAnchor.constraint(equalTo: view.rightAnchor, constant: -inset).isActive = true
        downloadResourcesButton.topAnchor.constraint(equalTo: startFingerScanButton.bottomAnchor, constant: inset).isActive = true
    }
}

// MARK: - Actions

extension ViewController {
    @IBAction func didTapSetup() {
        setupMultiScanSDK()
    }

    @IBAction func didTapStartFaceScan() {
        startFaceScan()
    }
    
    @IBAction func didTapStartFingerScan() {
        startFingerScan()
    }

    @IBAction func didTapStartBodyScan() {
        startBodyScan()
    }

    @IBAction func didTapCheckDownloadSize() {
        checkAHIResourcesDownloadSize()
    }

    @IBAction func didTapDownloadResources() {
        downloadAHIResources()
        areAHIResourcesAvailable()
        checkAHIResourcesDownloadSize()
        // Set button inactive
        downloadResourcesButton.isEnabled = false
        downloadResourcesButton.alpha = 0.5
    }
}

// MARK: - MultiScan SDK Setup Functions

extension ViewController {
    /// Setup the MultiScan SDK
    ///
    /// This must happen before requesting a scan.
    /// We recommend doing this on successful load of your application.
    fileprivate func setupMultiScanSDK() {
        ahi.setup(withConfig: ["TOKEN": AHIConfigTokens.AHI_MULTI_SCAN_TOKEN], scans: [faceScan, bodyScan, fingerScan]) { [weak self] error in
            if let err = error {
                print("AHI: Error setting up: \(err)")
                print("AHI: Confirm you have a valid token.")
                return
            }
            self?.authorizeUser()
        }
    }

    /// Once successfully setup, you should authorize your user with our service.
    ///
    /// With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
    fileprivate func authorizeUser() {
        ahi.userAuthorize(forId: AHIConfigTokens.AHI_TEST_USER_ID, withSalt: AHIConfigTokens.AHI_TEST_USER_SALT, withClaims: AHIConfigTokens.AHI_TEST_USER_CLAIMS) { [weak self] authError in
            if let err = authError {
                print("AHI: Auth Error: \(err)")
                print("AHI: Confirm you are using a valid user id, salt and claims")
                return
            }
            print("AHI: Setup user successfully")
            self?.isSetup = true
        }
    }
}

// MARK: - AHI Multi Scan Remote Resources

extension ViewController {
    /// Check if the AHI resources are downloaded.
    ///
    /// We have remote resources that exceed 100MB that enable our scans to work.
    /// You are required to download them inorder to obtain a body scan.
    fileprivate func areAHIResourcesAvailable() {
        ahi.areResourcesDownloaded { [weak self] success, error in
            if !success {
                print("AHI INFO: Resources are not downloaded, error: \(error?.localizedDescription)")
                weak var weakSelf = self
                // We recommend polling to check resource state.
                // This is a simple example of how.
                DispatchQueue.main.asyncAfter(deadline: .now() + 30.0) {
                    weakSelf?.checkAHIResourcesDownloadSize()
                    weakSelf?.areAHIResourcesAvailable()
                }
                return
            }
            self?.isFinishedDownloadingResources = success
            print("AHI: Resources ready")
        }
    }

    /// Download scan resources.
    ///
    /// We recommend only calling this function once per session to prevent duplicate background resource calls.
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

extension ViewController {
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
        ahi.initiateScan("face", withOptions: options, from: self) { scanTask, error in
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
                /// Handle failure.
                return nil
            })
        }
    }
}

// MARK: - AHI Finger Scan Initialiser

extension ViewController {
    fileprivate func startFingerScan() {
        // All required finger scan options.
        let options: [String : Any] = [
            "sec_ent_scanLength" : 60
        ]
        if !areFingerScanConfigOptionsValid(fingerScanInput: options) {
            print("AHI ERROR: Finger Scan inputs invalid.")
            return
        }
        // Ensure the view controller being used is the top one.
        // If you are not attempting to get a scan simultaneous with dismissing your calling view controller, or attempting to present from a view controller lower in the stack
        // you may have issues.
        ahi.initiateScan("finger", withOptions: options, from: self) { scanTask, error in
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
                /// Handle failure.
                return nil
            })
        }
    }
}

// MARK: - AHI Body Scan Initialiser

extension ViewController {
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
        ahi.initiateScan("body", withOptions: options, from: self) { [weak self] scanTask, error in
            guard let task = scanTask, error == nil else {
                // TODO: use enum
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
                /// Handle failure.
                return nil
            })
        }
    }
}

// MARK: - Body Scan Extras

extension ViewController {
    /// Use this function to fetch the 3D avatar mesh.
    ///
    /// The 3D mesh can be created and returned at any time.
    /// We recommend doing this on successful completion of a body scan with the results.
    fileprivate func getBodyScanExtras(withBodyScanResult result: [String: Any]) {
        ahi.getExtra(["body": [result]], query: ["extrapolate" : ["mesh"]]) { extras, error in
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

// MARK: - AHI Body Scan optional event lintener

extension ViewController: AHIBSEventListenerDelegate {
    func event(_ name: String, meta: [String : Any]?) {
        print("AHI Body Scan event: \(name)")
    }
}

// MARK: - AHI MultiScan Optional Functions

extension ViewController {
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
        ahi.userIsAuthorized { isAuthorized, partnerUserId, error in
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

// If you choose to use this, you will obtain two sets of results - one containing the "raw" output and another set containing "adj" output.
// "adj" means adjusted and is used to help provide historical results as a reference for the newest result to provide tailored to the user results.
// We recommend using this for individual users results; avoid using this if the app is a single user ID with multiple users results.
// More info found here: https://docs.advancedhumanimaging.io/MultiScan%20SDK/Data/
extension ViewController: AHIDelegatePersistence {
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
