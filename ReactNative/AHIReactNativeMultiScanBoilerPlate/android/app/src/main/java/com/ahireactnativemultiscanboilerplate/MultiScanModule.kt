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

import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.advancedhumanimaging.sdk.bodyscan.BodyScan
import com.advancedhumanimaging.sdk.bodyscan.common.BodyScanError
import com.advancedhumanimaging.sdk.common.IAHIDownloadProgress
import com.advancedhumanimaging.sdk.common.IAHIPersistence
import com.advancedhumanimaging.sdk.common.IAHIScan
import com.advancedhumanimaging.sdk.common.models.AHIResult
import com.advancedhumanimaging.sdk.facescan.AHIFaceScanError
import com.advancedhumanimaging.sdk.facescan.FaceScan
import com.advancedhumanimaging.sdk.fingerscan.AHIFingerScanError
import com.advancedhumanimaging.sdk.fingerscan.FingerScan
import com.advancedhumanimaging.sdk.multiscan.AHIMultiScan
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import kotlinx.coroutines.*

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
        AHIMultiScan.setup(
            reactApplicationContext.currentActivity!!.application,
            config,
            scans,
            completionBlock = { ahiResult ->
                ahiResult.fold(
                    { promise.resolve("") },
                    {
                        Log.d(TAG, "AHI: Error setting up: $}\n")
                        Log.d(TAG, "AHI: Confirm you have a valid token.\n")
                        promise.reject(it.error.code().toString(), it.message)
                    }
                )
            }
        )
    }

    /**
     * Once successfully setup, you should authorize your user with our service. With your signed in
     * user, you can authorize them to use the AHI service.
     */
    @ReactMethod
    private fun authorizeUser(
        userID: String,
        salt: String,
        claims: ReadableArray,
        promise: Promise,
    ) {
        val inputClaims = claims.toArrayList().map { it.toString() }.toTypedArray()
        AHIMultiScan.userAuthorize(
            userID,
            salt,
            inputClaims,
            completionBlock = { ahiResult ->
                ahiResult.fold(
                    { promise.resolve("") },
                    { promise.reject(it.error.code().toString(), it.message) }
                )
            }
        )
    }

    /**
     * Check if the AHI resources are downloaded.
     *
     * We have remote resources that exceed 100MB that enable our scans to work. You are required to
     * download them in order to obtain a body scan.
     *
     * This function checks if they are already downloaded and available for use.
     */
    @ReactMethod
    fun areAHIResourcesAvailable(promise: Promise) {
        AHIMultiScan.areResourcesDownloaded { ahiResult ->
            ahiResult.fold(
                { promise.resolve(it) },
                { promise.reject(it.error.code().toString(), it.message) }
            )
        }
    }

    /**
     * Download scan resources. We recommend only calling this function once per session to prevent
     * duplicate background resource calls.
     */
    @ReactMethod
    fun downloadAHIResources() {
        GlobalScope.launch(Dispatchers.Main) {
            AHIMultiScan.downloadResourcesInForeground(3)
        }
    }

    /** Use AHIMultiscan delegateDownloadProgress register to listen download progress report. */
    @ReactMethod
    fun getResourcesDownloadProgressReport() {
        AHIMultiScan.delegateDownloadProgress = object : IAHIDownloadProgress {
            override fun downloadProgressReport(status: AHIResult<Unit>) {
                GlobalScope.launch(Dispatchers.IO) {
                    AHIMultiScan.totalEstimatedDownloadSizeInBytes { ahiResult ->
                        ahiResult.fold(
                            { downloadState ->
                                val progressReport = mutableMapOf<String, Any>(
                                    "progress" to downloadState.progressBytes,
                                    "total" to downloadState.totalBytes,
                                )
                                if (downloadState.progressBytes != downloadState.totalBytes) {
                                    reactApplicationContext.getJSModule(RCTDeviceEventEmitter::class.java)
                                        .emit("progress_report", scanResultsToMap(progressReport))
                                }else {
                                    reactApplicationContext.getJSModule(RCTDeviceEventEmitter::class.java)
                                        .emit("progress_report", "done")
                                }
                            }, {
                                reactApplicationContext.getJSModule(RCTDeviceEventEmitter::class.java)
                                    .emit("progress_report", "failed")
                            }
                        )
                    }
                }
            }
        }
    }

    /** Check the size of the AHI resources that require downloading in single time. */
    @ReactMethod
    fun checkAHIResourcesDownloadSize(promise: Promise) {
        AHIMultiScan.totalEstimatedDownloadSizeInBytes { ahiResult ->
            ahiResult.fold(
                {
                    Log.d(TAG, "AHI INFO: Size of download is ${it.progressBytes / 1024 / 1024} / ${it.totalBytes / 1024 / 1024}\n")
                    promise.resolve(it.progressBytes.toString())
                },
                {
                    Log.e(TAG, it.message.toString())
                    promise.reject(it.error.code().toString(), it.message)
                }
            )
        }
    }

    @ReactMethod
    fun startFaceScan(userInput: ReadableMap, promise: Promise) {
        val userInputSet = userInput.toHashMap()
        val registry = currentActivity as AppCompatActivity
        AHIMultiScan.initiateScan(
            "face",
            userInputSet,
            registry.activityResultRegistry,
            completionBlock = {
                GlobalScope.launch(Dispatchers.Main) {
                    if (!it.isDone) {
                        Log.d(TAG, "Waiting of results, can show waiting screen here")
                    }
                    val result = withContext(Dispatchers.IO) { it.get() }
                    when (result) {
                        is AHIResult.Success -> {
                            Log.d(TAG, "initiateScan: ${result.value}")
                            val scanResult = scanResultsToMap(result.value)
                            promise.resolve(scanResult)
                        }
                        else -> {
                            if (result.error() == AHIFaceScanError.FACE_SCAN_CANCELED) {
                                Log.d(TAG, "User cancelled scan")
                                promise.reject(
                                    AHIFaceScanError.FACE_SCAN_CANCELED.code().toString(),
                                    AHIFaceScanError.FACE_SCAN_CANCELED.name
                                )
                            } else {
                                Log.d(TAG, "initiateScan: ${result.error()}")
                                promise.reject(
                                    result.error()?.code().toString(),
                                    result.error().toString()
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    @ReactMethod
    fun startFingerScan(userInput: ReadableMap, promise: Promise) {
        val userInputSet = userInput.toHashMap()
        val registry = currentActivity as AppCompatActivity
        AHIMultiScan.initiateScan(
            "finger",
            userInputSet,
            registry.activityResultRegistry,
            completionBlock = {
                if (!it.isDone) {
                    Log.d(TAG, "Waiting of results, can show waiting screen here")
                }
                GlobalScope.launch(Dispatchers.Main) {
                    val result = withContext(Dispatchers.IO) { it.get() }
                    when (result) {
                        is AHIResult.Success -> {
                            Log.d(TAG, "initiateScan: ${result.value}")
                            val scanResult = scanResultsToMap(result.value)
                            promise.resolve(scanResult)
                        }
                        else -> {
                            if (result.error() == AHIFingerScanError.FINGER_SCAN_CANCELLED) {
                                Log.d(TAG, "User cancelled scan")
                                promise.reject(
                                    AHIFingerScanError.FINGER_SCAN_CANCELLED
                                        .code()
                                        .toString(),
                                    AHIFingerScanError.FINGER_SCAN_CANCELLED.name
                                )
                            } else {
                                Log.d(TAG, "initiateScan: ${result.error()}")
                                promise.reject(
                                    result.error()?.code().toString(),
                                    result.error().toString()
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    @ReactMethod
    fun startBodyScan(userInput: ReadableMap, promise: Promise) {
        AHIMultiScan.delegatePersistence = AHIPersistenceDelegate
        val userInputSet = userInput.toHashMap()
        val registry = currentActivity as AppCompatActivity
        //        AHIMultiScan.delegatePersistence = AHIPersistenceDelegate
        AHIMultiScan.initiateScan(
            "body",
            userInputSet,
            registry.activityResultRegistry,
            completionBlock = {
                GlobalScope.launch(Dispatchers.IO) {
                    if (!it.isDone) {
                        Log.d(TAG, "Waiting of results, can show waiting screen here")
                    }
                    val result = withContext(Dispatchers.IO) { it.get() }
                    when (result) {
                        is AHIResult.Success -> {
                            Log.d(TAG, "initiateScan: ${result.value}")
                            AHIPersistenceDelegate.bodyScanResult.add(result.value)
                            val scanResult = scanResultsToMap(result.value)
                            promise.resolve(scanResult)
                        }
                        else -> {
                            if (result.error() == BodyScanError.BODY_SCAN_CANCELED) {
                                Log.d(TAG, "User cancelled scan")
                                promise.reject(
                                    BodyScanError.BODY_SCAN_CANCELED.code().toString(),
                                    BodyScanError.BODY_SCAN_CANCELED.name
                                )
                            } else {
                                Log.d(TAG, "initiateScan: ${result.error()}")
                                promise.reject(
                                    result.error()?.code().toString(),
                                    result.error().toString()
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Use this function to fetch the 3D avatar mesh. The 3D mesh can be created and returned at any
     * time. We recommend doing this on successful completion of a body scan with the results.
     */
    @ReactMethod
    fun getBodyScanExtra(bodyScanResult: ReadableMap, promise: Promise) {
        val result = bodyScanResult.toHashMap()
        val options = mapOf("extrapolate" to listOf("mesh"))
        AHIMultiScan.getScanExtra(
            result,
            options,
            completionBlock = { ahiResult ->
                ahiResult.fold(
                    {
                        val uri =
                            (it["extrapolate"] as? List<Map<*, *>>)
                                ?.firstOrNull()
                                ?.get("mesh") as?
                                    Uri
                        Log.d(TAG, "getBodyScanExtras: URI: $uri")
                        promise.resolve(scanResultsToMap(it))
                    },
                    {
                        Log.d(TAG, "getBodyScanExtras: ${it.error}")
                        promise.reject(it.error.code().toString(), it.message)
                    }
                )
            }
        )
    }

    /** Check if MultiScan is on or offline. */
    @ReactMethod
    fun getMultiScanStatus(promise: Promise) {
        AHIMultiScan.getStatus {
            Log.d(TAG, "AHI INFO: Status: ${it.name}")
            promise.resolve(it.name)
        }
    }

    /** Check your AHI MultiScan organisation details. */
    @ReactMethod
    fun getMultiScanDetails(promise: Promise) {
        AHIMultiScan.getDetails { ahiResult ->
            ahiResult.fold(
                {
                    Log.d(TAG, "AHI INFO: MultiScan details: ${it}")
                    val details = scanResultsToMap(it)
                    promise.resolve(details)
                },
                {
                    Log.d(TAG, "AHI INFO: Failed to get details")
                    promise.reject(it.error.code().toString(), it.message)
                }
            )
        }
        promise.resolve(WritableNativeMap())
    }

    /** Check if the user is authorized to use the MultiScan service. */
    @ReactMethod
    fun getUserAuthorizedState(promise: Promise) {
        AHIMultiScan.userIsAuthorized { ahiResult ->
            ahiResult.fold(
                { promise.resolve("AHI INFO: User is authorized") },
                { promise.reject(it.error.code().toString(), it.message) }
            )
        }
    }

    /** Deauthorize the user. */
    @ReactMethod
    fun deauthorizeUser(promise: Promise) {
        AHIMultiScan.userDeauthorize { ahiResult ->
            ahiResult.fold(
                {
                    Log.d(TAG, "AHI INFO: User is deauthorized.")
                    promise.resolve("")
                },
                {
                    Log.d(TAG, "AHI INFO: User is not deauthorized")
                    promise.reject(it.error.code().toString(), it.message)
                }
            )
        }
    }

    /**
     * Release the MultiScan SDK session.
     *
     * If you use this, you will need to call setupSDK again.
     */
    @ReactMethod
    fun releaseMultiScanSDK(promise: Promise) {
        AHIMultiScan.releaseSdk { ahiResult ->
            ahiResult.fold(
                {
                    Log.d(TAG, "AHI INFO: SDK is released.")
                    promise.resolve("")
                },
                {
                    Log.d(TAG, "AHI INFO: User is not released")
                    promise.reject(it.error.code().toString(), it.message)
                }
            )
        }
    }

    /**
     * The MultiScan SDK can provide personalised results.
     *
     * Optionally call this function on load of the SDK.
     */
    @ReactMethod
    fun setMultiScanPersistenceDelegate(results: ReadableMap) {
        AHIPersistenceDelegate.bodyScanResult.add(results.toHashMap())
        AHIMultiScan.delegatePersistence = AHIPersistenceDelegate
    }

    object AHIPersistenceDelegate : IAHIPersistence {
        /**
         * You should have your body scan results stored somewhere in your app that this function can access.
         * */
        var bodyScanResult = mutableListOf<Map<String, Any>>()
        override fun request(
            scanType: String,
            options: Map<String, Any>,
            completionBlock: (result: AHIResult<Array<Map<String, Any>>>) -> Unit,
        ) {
            val data: MutableList<Map<String, Any>> = when (scanType) {
                "body" -> {
                    bodyScanResult
                }
                else -> mutableListOf()
            }

            val sort = options["SORT"] as? String
            val order = options["ORDER"] as? String
            if (sort != null) {
                if (order == "descending") {
                    data.sortByDescending { it[sort].toString().toDoubleOrNull() }
                } else {
                    data.sortBy { it[sort].toString().toDoubleOrNull() }
                }
            }
            val since = options["SINCE"] as? Long
            if (since != null) {
                data.removeIf { (it["date"] as? Long)?.let { date -> date >= since } == true }
            }
            val count = options["COUNT"] as? Int
            if (count != null) {
                data.dropLast(data.size - count)
            }
            completionBlock(AHIResult.success(data.toTypedArray()))
        }
    }

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

private fun Double.format(digits: Int) = "%.${digits}f".format(this)