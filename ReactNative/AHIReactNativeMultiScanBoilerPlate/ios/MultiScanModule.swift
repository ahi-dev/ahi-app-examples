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


// DEBUG - TODO
// 1. ALWAYS GO WITH ERROR CASE FIRST
// 2. Extract config tokens into react side and pass in as arguments

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
    fileprivate func setupMultiScanSDK(_ token: String,
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
        if hasReturned {
          resolve("SUCCESS")
        }
      }
    }

  @objc
  fileprivate func authorizeUser(_ userID: String, salt aSalt:String, claims aClaims:[String],
                                 resolver resolve: @escaping RCTPromiseResolveBlock,
                                 rejecter reject: @escaping RCTPromiseRejectBlock) {
    ahi.userAuthorize(forId: userID, withSalt: aSalt, withClaims: aClaims) { error in
      if let err = error as? NSError {
        reject("\(err.code)", err.localizedDescription, err)
        return
      }
      resolve("SUCCESS")
    }
  }
  
  @objc
  fileprivate func areAHIResourcesAvailable(_ resolve: @escaping RCTPromiseResolveBlock,
                                            rejecter reject: @escaping RCTPromiseRejectBlock){
    ahi.areResourcesDownloaded{ success in
      resolve(success)
    }
  }
  
  @objc
  fileprivate func downloadAHIResources(){
    ahi.downloadResourcesInBackground()
  }
  
  @objc
  fileprivate func checkAHIResourcesDownloadSize(_ resolve: @escaping RCTPromiseResolveBlock,
                                                 rejecter reject: @escaping RCTPromiseRejectBlock){
    ahi.totalEstimatedDownloadSizeInBytes(){ bytes in
      resolve(Int(bytes))
    }
  }
  
  @objc
  fileprivate func startFaceScan(_ userInputs:[String: Any], paymentType msPaymentType:String,
                                 resolver resolve: @escaping RCTPromiseResolveBlock,
                                 rejecter reject: @escaping RCTPromiseRejectBlock) {
    var pType = AHIMultiScanPaymentType.PAYG
    if msPaymentType == "PAYG" {
      pType = AHIMultiScanPaymentType.PAYG
    }else if msPaymentType == "SUBS" {
      pType = AHIMultiScanPaymentType.subscriber
    }
    guard let vc = topMostVC() else {return }
    ahi.initiateScan("face", paymentType: pType, withOptions: userInputs, from:vc) { scanTask, error in
      guard let task = scanTask,
              error == nil else {
              // Error code 4 is the code for the SDK interaction that cancels the scan.
              if let err = error as? NSError {
                reject("\(err.code)", err.localizedDescription, err)
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
  fileprivate func startBodyScan(_ userInputs:[String: Any], paymentType msPaymentType:String,
                                 resolver resolve: @escaping RCTPromiseResolveBlock,
                                 rejecter reject: @escaping RCTPromiseRejectBlock) {
    var pType = AHIMultiScanPaymentType.PAYG
    if msPaymentType == "PAYG" {
      pType = AHIMultiScanPaymentType.PAYG
    }else if msPaymentType == "SUBS" {
      pType = AHIMultiScanPaymentType.subscriber
    }
    guard let vc = topMostVC() else {return }
    ahi.initiateScan("body", paymentType: pType, withOptions: userInputs, from:vc) { scanTask, error in
      guard let task = scanTask,
              error == nil else {
              // Error code 4 is the code for the SDK interaction that cancels the scan.
              if let err = error as? NSError {
                reject("\(err.code)", err.localizedDescription, err)
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
  func getBodyScanExtras(_ bodyScanResult:[String: Any],
                                    resolver resolve: @escaping RCTPromiseResolveBlock,
                                    rejecter reject: @escaping RCTPromiseRejectBlock){
    print("TESTING")
    ahi.getExtra(bodyScanResult, options: nil){ error, bodyExtras in
      print("GOT IT")
      if let err = error as? NSError {
        reject("\(err.code)", err.localizedDescription, err)
        return
      }
      guard let extras = bodyExtras else {
        let err = NSError(domain: "com.ahi.ios.ahi_multiscan_react_native_wrapper", code: -10, userInfo: [NSLocalizedDescriptionKey: "No body scan extras available."])
        reject("\(err.code)", err.localizedDescription, err)
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
  func getUserAuthorizedState(_ userId:Any?,
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
  func setMultiScanPersistenceDelegate(){
    
  }
}

extension MultiScanModule:AHIDelegatePersistence{
  public func setBodyScanResults(results: Any?) {
          guard let bsResults = results as? [[String: Any]] else {
              print("AHI: Results must not be nil and must conform to an Array of Map results.")
              return
          }
          ahi.setPersistenceDelegate(self)
          bodyScanResults = bsResults
      }

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
