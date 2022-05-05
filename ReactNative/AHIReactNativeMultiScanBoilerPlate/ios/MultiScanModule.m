//
//  MultiScanModule.m
//  AHIReactNativeMultiScanBoilerPlate
//
//  Created by Jordy Yeoman on 5/4/2022.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(MultiScanModule, NSObject)

RCT_EXTERN_METHOD(
                  setupMultiScanSDK:(NSString *)token
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )

RCT_EXTERN_METHOD(
                  authorizeUser:(NSString *)userID
                  salt:(NSString *)salt
                  claims:(NSArray *)claims
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
RCT_EXTERN_METHOD(
                  startFaceScan:(NSDictionary *)userInputValues
                  paymentType:(NSString *)msPaymentType
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
RCT_EXTERN_METHOD(
                  startBodyScan:(NSDictionary *)userInputValues
                  paymentType:(NSString *)msPaymentType
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
RCT_EXTERN_METHOD(
                  areAHIResourcesAvailable:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
RCT_EXTERN_METHOD(
                  downloadAHIResources
                  )
RCT_EXTERN_METHOD(
                  checkAHIResourcesDownloadSize:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )

RCT_EXTERN_METHOD(
                  getBodyScanExtras:(NSDictionary *)bodyScanResult
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
RCT_EXTERN_METHOD(
                  getMultiScanStatus:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
RCT_EXTERN_METHOD(
                  getMultiScanDetails:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
RCT_EXTERN_METHOD(
                  getUserAuthorizedState:(NSString *)userId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
RCT_EXTERN_METHOD(
                  deauthorizeUser:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
RCT_EXTERN_METHOD(
                  releaseMultiScanSDK:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject
                  )
RCT_EXTERN_METHOD(
                  setMultiScanPersistenceDelegate
                  )


@end
