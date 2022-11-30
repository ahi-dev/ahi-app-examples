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

package com.ahireactnativemultiscanboilerplate

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.advancedhumanimaging.sdk.bodyscan.BodyScan
import com.advancedhumanimaging.sdk.bodyscan.common.BodyScanError
import com.advancedhumanimaging.sdk.common.IAHIScan
import com.advancedhumanimaging.sdk.common.models.AHIResult
import com.advancedhumanimaging.sdk.facescan.AHIFaceScanError
import com.advancedhumanimaging.sdk.facescan.FaceScan
import com.advancedhumanimaging.sdk.fingerscan.AHIFingerScanError
import com.advancedhumanimaging.sdk.fingerscan.FingerScan
import com.advancedhumanimaging.sdk.multiscan.AHIMultiScan
import com.facebook.react.bridge.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private const val TAG = "MultiScanModule"

class MultiScanModule(private val context: ReactApplicationContext) :
    ReactContextBaseJavaModule(context) {
    override fun getName(): String {
        return "MultiScanModule"
    }

    /**
     * Setup the MultiScan SDK This must happen before requesting a scan. We recommend doing this on
     * successful load of your application.
     */
    @ReactMethod
    private fun setupMultiScanSDK(token: String, promise: Promise) {
        val config: MutableMap<String, String> = HashMap()
        config["TOKEN"] = token
        val scans: Array<IAHIScan> = arrayOf(FaceScan(), FingerScan(), BodyScan())
        AHIMultiScan.setup(reactApplicationContext.currentActivity!!.application, config, scans, completionBlock = { ahiResult ->
            ahiResult.fold({
                promise.resolve("")
            }, {
                promise.reject(it.error.code().toString(), it.message)
            })
        })
    }

    /**
     * Once successfully setup, you should authorize your user with our service. With your signed in
     * user, you can authorize them to use the AHI service, provided that they have agreed to a
     * payment method.
     */
    @ReactMethod
    private fun authorizeUser(userID: String, salt: String, claims: ReadableArray, promise: Promise) {
        val inputClaims = claims.toArrayList().map { it.toString() }.toTypedArray()
        AHIMultiScan.userAuthorize(userID, salt, inputClaims, completionBlock = { ahiResult ->
            ahiResult.fold({
                promise.resolve("")
            }, {
                promise.reject(it.error.code().toString(), it.message)
            })
        })
    }

    /** Check if the AHI resources are downloaded. */
    @ReactMethod
    fun areAHIResourcesAvailable(promise: Promise) {
        AHIMultiScan.areResourcesDownloaded { ahiResult ->
            ahiResult.fold({
                Log.d(TAG, "bbb: $it")
                promise.resolve(it)
            }, {
                promise.reject(it.error.code().toString(), it.message)
            })
        }
    }

    /**
     * Download scan resources. We recommend only calling this function once per session to prevent
     * duplicate background resource calls.
     */
    @ReactMethod
    fun downloadAHIResources() {
        Log.d(TAG, "downloadAHIResources: start")
        AHIMultiScan.downloadResourcesInBackground()
        Log.d(TAG, "downloadAHIResources: done")
    }

    /** Check the size of the AHI resources that require downloading. */
    @ReactMethod
    fun checkAHIResourcesDownloadSize(promise: Promise) {
        Log.d(TAG, "checkAHIResourcesDownloadSize: start")
        AHIMultiScan.totalEstimatedDownloadSizeInBytes { ahiResult ->
            ahiResult.fold({
                Log.d(TAG, "checkAHIResourcesDownloadSize: done")
                promise.resolve(it.totalBytes.toString())
            }, {
                Log.d(TAG, "checkAHIResourcesDownloadSize: reject")
                promise.reject(it.error.code().toString(), it.message)
            })
        }
    }

    @ReactMethod
    fun startFaceScan(userInput: ReadableMap, promise: Promise) {
        val userInputSet = userInput.toHashMap()
        val registry = currentActivity as AppCompatActivity
        AHIMultiScan.initiateScan("face", userInputSet, registry.activityResultRegistry, completionBlock = {
            GlobalScope.launch(Dispatchers.Main) {
                val result = withContext(Dispatchers.IO) { it.get() }
                when (result) {
                    is AHIResult.Success -> {
                        val scanResult = scanResultsToMap(result.value)
                        promise.resolve(scanResult)
                    }
                    else -> {
                        if (result.error() == AHIFaceScanError.FACE_SCAN_CANCELED) {
                            promise.reject(AHIFaceScanError.FACE_SCAN_CANCELED.code().toString(), AHIFaceScanError.FACE_SCAN_CANCELED.name)
                        } else {
                            promise.reject(result.error()?.code().toString(), result.error().toString())
                        }
                    }
                }
            }
        })
    }

    @ReactMethod
    fun startFingerScan(userInput: ReadableMap, promise: Promise) {
        val userInputSet = userInput.toHashMap()
        val registry = currentActivity as AppCompatActivity
        AHIMultiScan.initiateScan("finger", userInputSet, registry.activityResultRegistry, completionBlock = {
            GlobalScope.launch(Dispatchers.Main) {
                val result = withContext(Dispatchers.IO) { it.get() }
                when (result) {
                    is AHIResult.Success -> {
                        val scanResult = scanResultsToMap(result.value)
                        promise.resolve(scanResult)
                    }
                    else -> {
                        if (result.error() == AHIFingerScanError.FINGER_SCAN_CANCELLED) {
                            promise.reject(AHIFingerScanError.FINGER_SCAN_CANCELLED.code().toString(), AHIFingerScanError.FINGER_SCAN_CANCELLED.name)
                        } else {
                            promise.reject(result.error()?.code().toString(), result.error().toString())
                        }
                    }
                }
            }
        })
    }

    @ReactMethod
    fun startBodyScan(userInput: ReadableMap, promise: Promise) {
        val userInputSet = userInput.toHashMap()
        val registry = currentActivity as AppCompatActivity
        AHIMultiScan.initiateScan("body", userInputSet, registry.activityResultRegistry, completionBlock = {
            GlobalScope.launch(Dispatchers.IO) {
                val result = withContext(Dispatchers.IO) { it.get() }
                when (result) {
                    is AHIResult.Success -> {
                        val scanResult = scanResultsToMap(result.value)
                        promise.resolve(scanResult)
                    }
                    else -> {
                        if (result.error() == BodyScanError.BODY_SCAN_CANCELED) {
                            promise.reject(BodyScanError.BODY_SCAN_CANCELED.code().toString(), BodyScanError.BODY_SCAN_CANCELED.name)
                        } else {
                            promise.reject(result.error()?.code().toString(), result.error().toString())
                        }
                    }
                }
            }
        })
    }

    /**
     * Use this function to fetch the 3D avatar mesh. The 3D mesh can be created and returned at any
     * time. We recommend doing this on successful completion of a body scan with the results.
     */
    @ReactMethod
    fun getBodyScanExtras(bodyScanResult: ReadableMap, promise: Promise) {
        val result = bodyScanResult.toHashMap()
        val options = mapOf("extrapolate" to listOf("mesh"))
        AHIMultiScan.getScanExtra(result, options, completionBlock = { ahiResult ->
            ahiResult.fold({
                val extraWriteableMap = scanResultsToMap(it)
                promise.resolve(extraWriteableMap)
            }, {
                promise.reject(it.error.code().toString(), it.message)
            })
        })
    }

    /** Check if MultiScan is on or offline. */

    @ReactMethod
    fun getMultiScanStatus(promise: Promise) {
        AHIMultiScan.getStatus {
            promise.resolve(it.name)
        }
    }


    /** Check your AHI MultiScan organisation details. */
    @ReactMethod
    fun getMultiScanDetails(promise: Promise) {
        AHIMultiScan.getDetails { ahiResult ->
            ahiResult.fold({
                val details = scanResultsToMap(it)
                promise.resolve(details)
            }, {
                promise.reject(it.error.code().toString(), it.message)
            })
        }
        promise.resolve(WritableNativeMap())
    }

    /** Check if the user is authorized to use the MultiScan service.
     *
     * The expected result for <= v21.1.3 is an error called "NO_OP".
     * */
    @ReactMethod
    fun getUserAuthorizedState(promise: Promise) {
        AHIMultiScan.userIsAuthorized { ahiResult ->
            ahiResult.fold({
                promise.resolve("AHI INFO: User is authorized")
            }, {
                promise.reject(it.error.code().toString(), it.message)
            })
        }
    }


    /** Deauthorize the user. */
    @ReactMethod
    fun deauthorizeUser(promise: Promise) {
        AHIMultiScan.userDeauthorize { ahiResult ->
            ahiResult.fold({
                promise.resolve("")
            }, {
                promise.reject(it.error.code().toString(), it.message)
            })
        }
    }

    /**
     * Release the MultiScan SDK session.
     *
     * If you use this, you will need to call setupSDK again.
     * The expected result for <= v21.1.3 is an error called "NO_OP".
     */
    @ReactMethod
    fun releaseMultiScanSDK(promise: Promise) {
        AHIMultiScan.releaseSdk { ahiResult ->
            ahiResult.fold({
                promise.resolve("")
            }, {
                promise.reject(it.error.code().toString(), it.message)
            })
        }
    }


    /**
     * The MultiScan SDK can provide personalised results.
     *
     * Optionally call this function on load of the SDK.
     *//*
    @ReactMethod
    fun setMultiScanPersistenceDelegate(results: ReadableArray) {
        AHIPersistenceDelegate.let { it ->
            it.bodyScanResults = results.toArrayList().map { it.toString() }.toMutableList()
            MultiScan.shared().registerDelegate(it)
        }
    }
    */

    /** For the newest AHIMultiScan version 21.1.3 need to implement PersistenceDelegate *//*
    object AHIPersistenceDelegate : MultiScanDelegate {
        */
    /**
     * You should have your body scan results stored somewhere in your app that this function
     * can access.
     *//*
        var bodyScanResults = mutableListOf<String>()

        override fun request(
            scanType: MSScanType?,
            options: MutableMap<String, String>?,
        ): CompletableFuture<SdkResultParcelable> {
            val future = CompletableFuture<SdkResultParcelable>()
            if (scanType == MSScanType.BODY) {
                options?.forEach { bodyScanResults.add(it.toString()) }
                val jsonArrayString = "[" + bodyScanResults.joinToString(separator = ",") + "]"
                future.complete(SdkResultParcelable(SdkResultCode.SUCCESS, jsonArrayString))
            } else {
                future.complete(SdkResultParcelable(SdkResultCode.ERROR, ""))
            }
            return future
        }
    }

    */
    /** Save 3D avatar mesh result on local device. *//*
    private fun saveAvatarToFile(res: SdkResultParcelable, objFile: File): Boolean {
        return try {
            val meshResObj = JSONObject(res.result)
            val objString = meshResObj["mesh"].toString()
            val words: List<String> = objString.split(",")
            val stream = FileOutputStream(objFile)
            val writer = BufferedWriter(OutputStreamWriter(stream))
            for (word in words) {
                writer.write(word)
                writer.newLine()
            }
            writer.close()
            true
        } catch (e: Exception) {
            print("AHI ERROR: KOTLIN: Exception when attempting to write file: $e")
            false
        }
    }
    */

    private fun scanResultsToMap(scanResult: Map<String, Any>?): WritableNativeMap {
        if (scanResult == null || scanResult.isEmpty()) {
            return WritableNativeMap()
        }
        val resultsMap = WritableNativeMap()
        for (key in scanResult.keys) {
            resultsMap.putString(key, scanResult[key].toString())
        }
        return resultsMap
    }
}
