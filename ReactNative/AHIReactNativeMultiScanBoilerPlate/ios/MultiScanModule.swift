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
import MyFiziqSDKCoreLite
// The FaceScan SDK
import MFZFaceScan

@objc(MultiScanModule)
class MultiScanModule: NSObject {
    // Start SDK init
    /// Instance of AHI MultiScan
    let ahi = AHIMultiScan.shared()!
    /// Instance of AHI FaceScan
    let faceScan = AHIFaceScan.shared()
    /// Instance of AHI BodyScan
    let bodyScan = AHIBodyScan.shared()
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
        ahi.setup(withConfig: ["TOKEN": token], scans: [faceScan, bodyScan]) { error in
            if hasReturned {
                return
            }
            hasReturned = true
            if let err = error as? NSError {
                reject("\(err.code)", err.localizedDescription, err)
                return
            }
            resolve(nil)
        }
    }

    @objc
    func authorizeUser(_ userID: String,
                       salt aSalt:String,
                       claims aClaims:[String],
                       resolver resolve: @escaping RCTPromiseResolveBlock,
                       rejecter reject: @escaping RCTPromiseRejectBlock) {
        ahi.userAuthorize(forId: userID, withSalt: aSalt, withClaims: aClaims) { error in
            if let err = error as? NSError {
                reject("\(err.code)", err.localizedDescription, err)
                return
            }
            resolve(nil)
        }
    }

    @objc
    func areAHIResourcesAvailable(_ resolve: @escaping RCTPromiseResolveBlock,
                                  rejecter reject: @escaping RCTPromiseRejectBlock){
        ahi.areResourcesDownloaded{ success in
            resolve(success)
        }
    }

    @objc
    func downloadAHIResources(){
        ahi.downloadResourcesInBackground()
    }

    @objc
    func checkAHIResourcesDownloadSize(_ resolve: @escaping RCTPromiseResolveBlock,
                                       rejecter reject: @escaping RCTPromiseRejectBlock){
        ahi.totalEstimatedDownloadSizeInBytes(){ bytes in
            resolve(Int64(bytes))
        }
    }

    @objc
    func startFaceScan(_ userInputs: [String: Any], paymentType msPaymentType: String,
                       resolver resolve: @escaping RCTPromiseResolveBlock,
                       rejecter reject: @escaping RCTPromiseRejectBlock) {
        var pType = AHIMultiScanPaymentType.PAYG
        if msPaymentType == "PAYG" {
            pType = AHIMultiScanPaymentType.PAYG
        } else if msPaymentType == "SUBSCRIBER" {
            pType = AHIMultiScanPaymentType.subscriber
        } else {
            reject("-4", "Missing user face scan payment type.", nil)
            return
        }
        guard let vc = topMostVC() else {return }
        ahi.initiateScan("face", paymentType: pType, withOptions: userInputs, from:vc) { scanTask, error in
            guard let task = scanTask,
                  error == nil else {
                // Error code 4 is the code for the SDK interaction that cancels the scan.
                if let err = error as? NSError {
                    reject("\(err.code)", err.localizedDescription, err)
                } else {
                    reject("-10", "Error performing face scan.", nil)
                }
                return
            }
            task.continueWith(block: { resultsTask in
                if let results = resultsTask.result as? [String : Any] {
                    resolve(results)
                } else {
                    resolve(nil)
                }
                // Handle failure.
                return nil
            })
        }
    }

    @objc
    func startBodyScan(_ userInputs: [String: Any],
                       paymentType msPaymentType: String,
                       resolver resolve: @escaping RCTPromiseResolveBlock,
                       rejecter reject: @escaping RCTPromiseRejectBlock) {
        var pType = AHIMultiScanPaymentType.PAYG
        if msPaymentType == "PAYG" {
            pType = AHIMultiScanPaymentType.PAYG
        } else if msPaymentType == "SUBSCRIBER" {
            pType = AHIMultiScanPaymentType.subscriber
        } else {
            reject("-6", "Missing user face scan payment type.", nil)
            return
        }
        guard let vc = topMostVC() else {return }
        ahi.initiateScan("body", paymentType: pType, withOptions: userInputs, from:vc) { scanTask, error in
            guard let task = scanTask,
                  error == nil else {
                if let err = error as? NSError {
                    reject("\(err.code)", err.localizedDescription, err)
                } else {
                    reject("-12", "Error performing body scan.", nil)
                }
                return
            }
            task.continueWith(block: { resultsTask in
                if let results = resultsTask.result as? [String : Any] {
                    resolve(results)
                } else {
                    resolve(nil)
                }
                // Handle failure.
                return nil
            })
        }
    }

    @objc
    func getBodyScanExtras(_ bodyScanResult: [String: Any],
                           resolver resolve: @escaping RCTPromiseResolveBlock,
                           rejecter reject: @escaping RCTPromiseRejectBlock){
        ahi.getExtra(bodyScanResult, options: nil) { error, bodyExtras in
            if let err = error as? NSError {
                reject("\(err.code)", err.localizedDescription, err)
                return
            }
            guard let extras = bodyExtras else {
                reject("-10", "No body scan extras available.", nil)
                return
            }
            var bsExtras = [String: String]()
            if let meshURL = extras["meshURL"] as? URL {
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
            resolve(multiScanStatus)
        }
    }
    @objc
    func getMultiScanDetails(_ resolve: @escaping RCTPromiseResolveBlock,
                             rejecter reject: @escaping RCTPromiseRejectBlock){
        if let details = ahi.getDetails(){
            resolve(details)
        }else {
            resolve(nil)
        }
    }

    @objc
    func getUserAuthorizedState(_ userId: Any?,
                                resolver resolve: @escaping RCTPromiseResolveBlock,
                                rejecter reject: @escaping RCTPromiseRejectBlock){
        guard let userId = userId as? String else {
            let err = NSError(domain: "com.ahi.ios.ahi_multiscan_react_native_wrapper", code: -11, userInfo: [NSLocalizedDescriptionKey: "User are not authorized"])
            reject("\(err.code)", err.localizedDescription, err)
            return
        }
        ahi.userIsAuthorized(forId: userId) { isAuthorized in
            resolve(isAuthorized)
        }
    }

    @objc
    func deauthorizeUser(_ resolve: @escaping RCTPromiseResolveBlock,
                         rejecter reject: @escaping RCTPromiseRejectBlock){
        ahi.userDeauthorize{error in
            resolve(error)
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
    func setMultiScanPersistenceDelegate(_ results: [[String: Any]]?) {
        guard let bsResults = results else {
            print("AHI: Results must not be nil and must conform to an Array of Map results.")
            return
        }
        ahi.setPersistenceDelegate(self)
        bodyScanResults = bsResults
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
