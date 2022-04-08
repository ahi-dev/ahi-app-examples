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
  
  fileprivate let success = "success"
  fileprivate var isSetup = false
  fileprivate var isFinishedDownloadingResources = false
  fileprivate let errorCode = NSError(domain: "", code: 200, userInfo: nil)
  fileprivate var flag = false
  // Start SDK init
  /// Instance of AHI MultiScan
  let ahi = AHIMultiScan.shared()!
  /// Instance of AHI FaceScan
  let faceScan = AHIFaceScan.shared()
  /// Instance of AHI BodyScan
  let bodyScan = AHIBodyScan.shared()
  //  ahi.setPersistenceDelegate(self)
  
  // DEBUG
  /// Testing shit
  private var count = 0
  // DEBUG
  
  // This method is required since the MultiScanModule overrides init
  @objc static func requiresMainQueueSetup() -> Bool {
    return false
  }
  
}

extension MultiScanModule {
  /// Setup the MultiScan SDK
  ///
  /// This must happen before requesting a scan.
  /// We recommend doing this on successfuil load of your application.
  @objc
  fileprivate func setupMultiScanSDK(_ token: String,
                                     resolver resolve: @escaping RCTPromiseResolveBlock,
                                     rejecter reject: @escaping RCTPromiseRejectBlock) -> Void {
    var flag = false
    ahi.setup(withConfig: ["TOKEN": token], scans: [faceScan, bodyScan]) { error in
      if let err = error {
        reject("SETUP_ERROR", "Failed to setup SDK: \(err). OR SDK already setup bro show error code=1??", self.errorCode)
        return
      }
      print("We are called here")
      flag = true
    }
    // resolve must be outside of the completion block due to the success state being called twice
    if flag == true {
      print("Are we resolving??")
      resolve(self.success)
    }
  }
  
  /// Once successfully setup, you should authorize your user with our service.
  ///
  /// With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
  @objc
  fileprivate func authorizeUser(_ userID: String, salt aSalt:String, claims aClaims:[String],
                                 resolver resolve: @escaping RCTPromiseResolveBlock,
                                 rejecter reject: @escaping RCTPromiseRejectBlock) {
    ahi.userAuthorize(forId: userID, withSalt: aSalt, withClaims: aClaims) { authError in
      if let err = authError {
        reject("USER_AUTHENTICATION_ERROR","Failed to authorize user: \(err)", self.errorCode)
        return
      }
      // Redundant? -> Just return the success back to react.
      self.isSetup = true
      resolve(self.success)
      print("AHI: Setup user successfully")
    }
  }
  
  // Redundant method? - We can just call userAuthorize() method again to get the userAuth status.
  @objc
  fileprivate func isUserAuthorized(_ resolve: RCTPromiseResolveBlock, rejecter reject: RCTPromiseRejectBlock) {
    if !self.isSetup {
      reject("USER_AUTHENTICATION_ERROR", "User not authenticated", self.errorCode)
      return
    }
    resolve(self.success)
  }
  
}



extension MultiScanModule {
  /// Check if the AHI resources are downloaded.
  ///
  /// We have remote resources that exceed 100MB that enable our scans to work.
  /// You are required to download them inorder to obtain a body scan.
  @objc
  fileprivate func areAHIResourcesAvailable(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
    ahi.areResourcesDownloaded { [weak self] success in
      if !success {
//        print("AHI INFO: Resources are not downloaded.")
//        reject("AHI_ASSET_ERROR","AHI INFO: Resources are not finished downloading.", self?.errorCode)
        resolve("Resources not finished downloading")
        return
      }
      self?.isFinishedDownloadingResources = success
      print("AHI: Resources ready")
      resolve(self?.success)
      return
    }
  }
  
  /// Download scan resources.
  ///
  /// We recomment only calling this function once per session to prevent duplicate background resource calls.
  @objc
  fileprivate func downloadAHIResources(_ resolve: RCTPromiseResolveBlock, rejecter reject: RCTPromiseRejectBlock) {
    ahi.downloadResourcesInBackground()
    resolve(self.success)
  }
  
  /// Check the size of the AHI resources that require downloading.
  @objc
  fileprivate func checkAHIResourcesDownloadSize(_ resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping  RCTPromiseRejectBlock) {
    ahi.totalEstimatedDownloadSizeInBytes() { [weak self] bytes in
      print("AHI INFO: Size of download is \(self?.convertBytesToMBString(Int(bytes)) ?? "0")")
      resolve(self?.convertBytesToMBString(Int(bytes)))
    }
  }
  
}

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
    ahi.initiateScan("face", paymentType: .PAYG, withOptions: options, from: self) { scanTask, error in
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




extension MultiScanModule {
  // DEBUG
  @objc func passValueFromReact(_ value : String) -> String {
    debugPrint(" Print Here \(value)")
    return "Thanks braaaaaaa, ALL GOOD!!"
  }
  @objc
  func increment( _ resolve: RCTPromiseResolveBlock,
                  rejecter reject: RCTPromiseRejectBlock
  ) -> Void {
    count += 1
    resolve("count was incremented, count: \(count)")
  }
  
  @objc
  func decrement(
    _ resolve: RCTPromiseResolveBlock,
    rejecter reject: RCTPromiseRejectBlock
  ) -> Void {
    if (count == 0) {
      let error = NSError(domain: "", code: 200, userInfo: nil)
      reject("E_COUNT", "count cannot be negative", error)
    } else {
      count -= 1
      resolve("count was decremented, count: \(count)")
    }
  }
  // DEBUG
}

