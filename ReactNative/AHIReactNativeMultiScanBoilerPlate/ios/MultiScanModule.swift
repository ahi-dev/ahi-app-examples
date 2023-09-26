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

import Foundation
import React
// The MultiScan SDK
import AHIMultiScan
// The Body Scan SDK
import AHIBodyScan
// The FaceScan SDK
import AHIFaceScan
// The FingerScan SDK
import AHIFingerScan

@objc(MultiScanModule)
class MultiScanModule: NSObject {
    // Start SDK init
    /// Instance of AHI MultiScan
    let ahi = MultiScan.shared()
    /// Instance of AHI FaceScan
    let faceScan = FaceScan()
    /// Instance of AHI FingerScan
    let fingerScan = FingerScan()
    /// Instance of AHI BodyScan
    let bodyScan = BodyScan()
    /// Body Scan Results
    var bodyScanResults = [[String: Any]]()

    @objc static func requiresMainQueueSetup() -> Bool {
        return false
    }
}

extension MultiScanModule {

    @objc
    func setupMultiScanSDK(_ token: String,
                           resolver resolve: @escaping RCTPromiseResolveBlock,
                           rejecter reject: @escaping RCTPromiseRejectBlock) {
        var hasReturned = false
        ahi.setup(withConfig: ["TOKEN": token], scans: [faceScan, fingerScan, bodyScan]) { error in
            if hasReturned {
                return
            }
            hasReturned = true
            if let err = error as NSError? {
                reject("\(err.code)", err.localizedDescription, err)
                return
            }
            resolve("")
        }
    }

    @objc
    func authorizeUser(_ userID: String,
                       salt aSalt: String,
                       claims aClaims: [String],
                       resolver resolve: @escaping RCTPromiseResolveBlock,
                       rejecter reject: @escaping RCTPromiseRejectBlock) {
        var hasReturned = false
        ahi.userAuthorize(forId: userID, withSalt: aSalt, withClaims: aClaims) { error in
            if hasReturned {
                return
            }
            hasReturned = true
            if let err = error as NSError? {
                reject("\(err.code)", err.localizedDescription, err)
                return
            }
            resolve("")
        }
    }

    @objc
    func areAHIResourcesAvailable(_ resolve: @escaping RCTPromiseResolveBlock,
                                  rejecter reject: @escaping RCTPromiseRejectBlock){
        // This is a temporary solution to prevent multiple callbacks being invoked on null pointer promise and resolve blocks. 
        var hasReturned = false
        ahi.areResourcesDownloaded{ success, error in
            if hasReturned {
                return
            }
            hasReturned = true
            resolve(success)
        }
    }

    @objc
    func downloadAHIResources() {
        ahi.downloadResourcesInBackground()
    }

    @objc
    func checkAHIResourcesDownloadSize(_ resolve: @escaping RCTPromiseResolveBlock,
                                       rejecter reject: @escaping RCTPromiseRejectBlock){
        ahi.totalEstimatedDownloadSizeInBytes(){ bytes, totalBytes, error in
            resolve(Int64(bytes))
        }
    }

    @objc
    func startFaceScan(_ userInputs: [String: Any],
                       resolver resolve: @escaping RCTPromiseResolveBlock,
                       rejecter reject: @escaping RCTPromiseRejectBlock) {
        DispatchQueue.main.async {
            // This is a temporary solution to prevent multiple callbacks being invoked on null pointer promise and resolve blocks. 
            var hasReturned = false
            guard let vc = self.topMostVC() else {return }
            self.ahi.initiateScan("face", withOptions: userInputs, from:vc) { scanTask, error in
                if hasReturned {
                    return
                }
                guard let task = scanTask,
                      error == nil else {
                          if let err = error as NSError? {
                              if (err.code == AHIFaceScanErrorCode.ScanCanceled.rawValue) {
                                  // Scan cancelled by user
                                  print("User scan cancelled")
                              }
                              reject("\(err.code)", err.localizedDescription, err)
                          } else {
                              reject("-10", "Error performing face scan.", "" as? Error)
                          }
                          return
                      }
                task.continueWith(block: { resultsTask in
                    if let results = resultsTask.result as? [String : Any] {
                        resolve(results)
                    } else {
                        resolve("")
                    }
                    // Handle failure.
                    return nil
                })
            }
        }
    }
    
    @objc
    func startFingerScan(_ userInputs: [String: Any],
                       resolver resolve: @escaping RCTPromiseResolveBlock,
                       rejecter reject: @escaping RCTPromiseRejectBlock) {
        DispatchQueue.main.async {
            // This is a temporary solution to prevent multiple callbacks being invoked on null pointer promise and resolve blocks.
            var hasReturned = false
            guard let vc = self.topMostVC() else {return }
            self.ahi.initiateScan("finger", withOptions: userInputs, from:vc) { scanTask, error in
                if hasReturned {
                    return
                }
                guard let task = scanTask, error == nil else {
                    if let err = error as NSError? {
                        if (err.code == AHIFingerScanErrorCode.codeScanCanceled.rawValue) {
                            // Scan cancelled by user
                            print("User scan cancelled")
                        }
                        reject("\(err.code)", err.localizedDescription, err)
                    } else {
                        reject("-10", "Error performing face scan.", "" as? Error)
                    }
                    return
                }
                task.continueWith(block: { resultsTask in
                    if let results = resultsTask.result as? [String : Any] {
                        resolve(results)
                    } else {
                        resolve("")
                    }
                    // Handle failure.
                    return nil
                })
            }
        }
    }

    @objc
    func startBodyScan(_ userInputs: [String: Any],
                       resolver resolve: @escaping RCTPromiseResolveBlock,
                       rejecter reject: @escaping RCTPromiseRejectBlock) {
        DispatchQueue.main.async {
            guard let vc = self.topMostVC() else { return }
            // This is a temporary solution to prevent multiple callbacks being invoked on null pointer promise and resolve blocks. 
            var hasReturned = false
            self.ahi.initiateScan("body", withOptions: userInputs, from:vc) { scanTask, error in
                if hasReturned {
                    return
                }
                hasReturned = true
                guard let task = scanTask,
                      error == nil else {
                          if let err = error as NSError? {
                              reject("\(err.code)", err.localizedDescription, err)
                          } else {
                              reject("-12", "Error performing body scan.", "" as? Error)
                          }
                          return
                      }
                task.continueWith(block: { resultsTask in
                    if let results = resultsTask.result as? [String : Any] {
                        resolve(results)
                    } else {
                        resolve("")
                    }
                    // Handle failure.
                    return nil
                })
            }
        }
    }

    @objc
    func getBodyScanExtras(_ bodyScanResult: [String: Any],
                           resolver resolve: @escaping RCTPromiseResolveBlock,
                           rejecter reject: @escaping RCTPromiseRejectBlock) {
        
        ahi.getExtra(["body": [bodyScanResult]], query: ["extrapolate" : ["mesh"]]) { bodyExtras, error in
            if let err = error as NSError? {
                reject("\(err.code)", err.localizedDescription, err)
                return
            }
            guard let extras = bodyExtras else {
                reject("-10", "No body scan extras available.", "" as? Error)
                return
            }
            
//        TODO: get mesh
            var bsExtras = [String: String]()
            
            if let meshResult = extras["extrapolate"]?.first as? Dictionary<String, Any>, let meshURL = meshResult["mesh"] as? URL {
                bsExtras["meshURL"] = meshURL.absoluteString
            } else {
                bsExtras["meshURL"] = ""
            }
            resolve(bsExtras)
        }
    }

    @objc
    func getMultiScanStatus(_ resolve: @escaping RCTPromiseResolveBlock,
                            rejecter reject: @escaping RCTPromiseRejectBlock){
        ahi.status {  multiScanStatus in
            var result = ""
            switch multiScanStatus {
                case .ready:
                    result = "ready"
                    break
                case .disconnected:
                    result = "disconnected"
                    break
                case .notSetup:
                    result = "not setup"
                    break
                @unknown default:
                    break
            }
            resolve(result)
        }
    }
    @objc
    func getMultiScanDetails(_ resolve: @escaping RCTPromiseResolveBlock,
                             rejecter reject: @escaping RCTPromiseRejectBlock){
        if let details = ahi.getDetails(){
            resolve(details)
        }else {
            resolve("")
        }
    }

    @objc
    func getUserAuthorizedState( resolver resolve: @escaping RCTPromiseResolveBlock,
                                 rejecter reject: @escaping RCTPromiseRejectBlock) {
        ahi.userIsAuthorized() { isAuthorized, userId, error in
            resolve(isAuthorized)
        }
    }

    @objc
    func deauthorizeUser(_ resolve: @escaping RCTPromiseResolveBlock,
                         rejecter reject: @escaping RCTPromiseRejectBlock){
        ahi.userDeauthorize{ error in
            if let err = error as NSError? {
                reject("\(err.code)", err.localizedDescription, err)
                return
            }
            resolve("")
        }
    }
        
    @objc
    func releaseMultiScanSDK(_ resolve: @escaping RCTPromiseResolveBlock,
                             rejecter reject: @escaping RCTPromiseRejectBlock){
        ahi.releaseSDK { error in
            resolve(error)
        }
    }

    @objc
    func setMultiScanPersistenceDelegate(_ results: Any?) {
        guard let bsResults = results as? [[String: Any]] else {
            print("AHI: Results must not be nil and must conform to an Array of Map results.")
            return
        }
        ahi.delegatePersistence = self
        bodyScanResults = bsResults
    }
    @objc
    func getResourcesDownloadProgressReport(){
        ahi.delegateDownloadProgress = self
    }
}

extension MultiScanModule: AHIDelegateDownloadProgress {
    func downloadProgressReport(_ error: Error?) {
        if((error) != nil){
            print("AHI: Download Failed.")
            return
        }
        DispatchQueue.main.sync {
            ahi.totalEstimatedDownloadSizeInBytes(){ bytes, totalBytes, error in
                if(bytes>=totalBytes){
                    print("AHI: INFO: Download Finished.")
                }else{
                    let progress = bytes/1024/1024
                    let total = totalBytes/1024/1024
                    print("AHI: Download Size: \(progress) / \(total)")
                }
            }
        }
    }
}

extension MultiScanModule: AHIDelegatePersistence {
    func requestScanType(_ scan: String, options: [String : Any] = [:], completion completionBlock: @escaping (Error?, [[String : Any]]?) -> Void) {
        // Call the completion block to return the results to the SDK.
        completionBlock(nil, bodyScanResults)
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
