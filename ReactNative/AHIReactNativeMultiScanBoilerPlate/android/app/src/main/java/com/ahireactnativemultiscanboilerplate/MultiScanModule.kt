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

import com.facebook.react.bridge.*
import com.myfiziq.sdk.MultiScan
import com.myfiziq.sdk.MultiScanDelegate
import com.myfiziq.sdk.MultiScanOperation
import com.myfiziq.sdk.enums.MSPaymentType
import com.myfiziq.sdk.enums.MSScanType
import com.myfiziq.sdk.enums.SdkResultCode
import com.myfiziq.sdk.vo.SdkResultParcelable
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.CompletableFuture

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
    fun setupMultiScanSDK(token: String, promise: Promise) {
        val config: MutableMap<String, String> = HashMap()
        config["TOKEN"] = token
        MultiScan.waitForResult(MultiScan.shared().setup(config)) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    MultiScan.shared().registerDelegate(AHIPersistenceDelegate)
                    promise.resolve("")
                }
                else -> {
                    handleFailedResult(it, promise)
                }
            }
        }
    }

    /**
     * Once successfully setup, you should authorize your user with our service. With your signed in
     * user, you can authorize them to use the AHI service, provided that they have agreed to a
     * payment method.
     */
    @ReactMethod
    fun authorizeUser(userID: String, salt: String, claims: ReadableArray, promise: Promise) {
        /* Convert claims to an Array<String> */
        val claimsArray: Array<String> = claims.toArrayList().map { it.toString() }.toTypedArray()
        MultiScan.waitForResult(MultiScan.shared().userAuthorize(userID, salt, claimsArray)) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    promise.resolve("")
                }
                else -> {
                    handleFailedResult(it, promise)
                }
            }
        }
    }

    /** Check if the AHI resources are downloaded. */
    @ReactMethod
    fun areAHIResourcesAvailable(promise: Promise) {
        MultiScan.waitForResult(MultiScan.shared().areResourcesDownloaded()) {
            promise.resolve(it)
        }
    }

    /**
     * Download scan resources. We recommend only calling this function once per session to prevent
     * duplicate background resource calls.
     */
    @ReactMethod
    fun downloadAHIResources() {
        MultiScan.shared().downloadResourcesInBackground()
    }

    /** Check the size of the AHI resources that require downloading. */
    @ReactMethod
    fun checkAHIResourcesDownloadSize(promise: Promise) {
        MultiScan.waitForResult(MultiScan.shared().totalEstimatedDownloadSizeInBytes()) {
            promise.resolve(it.toDouble())
        }
    }

    @ReactMethod
    fun startFaceScan(userInput: ReadableMap, paymentType: String, promise: Promise) {
        val pType = if (paymentType == "PAYG") {
            MSPaymentType.PAYG
        } else if (paymentType == "SUBSCRIBER") {
            MSPaymentType.SUBS
        } else {
            promise.reject("-4", "Missing user face scan payment type.")
            return
        }
        MultiScan.waitForResult(
                MultiScan.shared().initiateScan(MSScanType.FACE, pType, userInput)
        ) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    val result = scanResultsToMap(it.result)
                    promise.resolve(result)
                }
                else -> {
                    handleFailedResult(it, promise)
                }
            }
        }
    }

    @ReactMethod
    fun startBodyScan(userInput: ReadableMap, paymentType: String, promise: Promise) {
        val pType = if (paymentType == "PAYG") {
            MSPaymentType.PAYG
        } else if (paymentType == "SUBSCRIBER") {
            MSPaymentType.SUBS
        } else {
            promise.reject("-6", "Missing user body scan payment type.")
            return
        }
        MultiScan.waitForResult(
                MultiScan.shared().initiateScan(MSScanType.BODY, pType, userInput)
        ) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    val resultsMap = scanResultsToMap(it.result)
                    promise.resolve(resultsMap)
                }
                else -> {
                    handleFailedResult(it, promise)
                }
            }
        }
    }

    /**
     * Use this function to fetch the 3D avatar mesh. The 3D mesh can be created and returned at any
     * time. We recommend doing this on successful completion of a body scan with the results.
     */
    @ReactMethod
    fun getBodyScanExtras(bodyScanResult: ReadableMap, promise: Promise) {
        if (bodyScanResult == null) {
            promise.reject("-8", "Missing valid body scan result.")
            return
        }
        val resultID = bodyScanResult.toHashMap()["id"] as? String ?: run {
            promise.reject("-8", "Missing valid body scan result.")
            return
        }
        val parameters: MutableMap<String, Any> = HashMap()
        parameters["operation"] = MultiScanOperation.BodyGetMeshObj.name
        parameters["id"] = resultID
        /* Write the mesh to a directory */
        val objFilePath = File(context.filesDir, "$resultID.obj")
        MultiScan.waitForResult(MultiScan.shared().getScanExtra(MSScanType.BODY, parameters)) {
            var bsExtras = WritableNativeMap()
            when (saveAvatarToFile(it, objFilePath)) {
                true -> bsExtras.putString("meshURL", objFilePath.path)
                false -> bsExtras.putString("meshURL", "")
            }
            promise.resolve(bsExtras)
        }
    }

    /** Check if MultiScan is on or offline. */
    @ReactMethod
    fun getMultiScanStatus(promise: Promise) {
        MultiScan.waitForResult(MultiScan.shared().state) { promise.resolve(it.result.toString()) }
    }

    /** Check your AHI MultiScan organisation details. */
    @ReactMethod
    fun getMultiScanDetails(promise: Promise) {
        promise.resolve(WritableNativeMap())
    }

    /** Check if the user is authorized to use the MultiScan service.
     *
     * The expected result for <= v21.1.3 is an error called "NO_OP".
     * */
    @ReactMethod
    fun getUserAuthorizedState(userID: String?, promise: Promise) {
        if (userID.isNullOrEmpty()) {
            promise.reject("-9", "Missing user ID")
            return
        }
        MultiScan.waitForResult(MultiScan.shared().userIsAuthorized(userID)) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    promise.resolve(it.result)
                }
                else -> {
                    if (it.resultCode == SdkResultCode.NO_OP) {
                        promise.reject("-15", "AHI MultiScan SDK functionality not implemented.")
                    } else {
                        handleFailedResult(it, promise)
                    }
                }
            }
        }
    }

    /** Deauthorize the user. */
    @ReactMethod
    fun deauthorizeUser(promise: Promise) {
        MultiScan.waitForResult(MultiScan.shared().userDeauthorize()) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    promise.resolve("")
                }
                else -> {
                    if (it.resultCode == SdkResultCode.NO_OP) {
                        promise.reject("-15", "AHI MultiScan SDK functionality not implemented.")
                    } else {
                        handleFailedResult(it, promise)
                    }
                }
            }
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
        promise.resolve("AHI MultiScan SDK functionality not implemented.")
    }

    /**
     * The MultiScan SDK can provide personalised results.
     *
     * Optionally call this function on load of the SDK.
     */
    @ReactMethod
    fun setMultiScanPersistenceDelegate(results: ReadableArray) {
        AHIPersistenceDelegate.let { it ->
            it.bodyScanResults = results.toArrayList().map { it.toString() }.toMutableList()
            MultiScan.shared().registerDelegate(it)
        }
    }

    /** For the newest AHIMultiScan version 21.1.3 need to implement PersistenceDelegate */
    object AHIPersistenceDelegate : MultiScanDelegate {
        /**
         * You should have your body scan results stored somewhere in your app that this function
         * can access.
         */
        var bodyScanResults = mutableListOf<String>()

        override fun request(
            scanType: MSScanType?,
            options: MutableMap<String, String>?
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

    /** Save 3D avatar mesh result on local device. */
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

    private fun scanResultsToMap(result: String?): WritableNativeMap {
        if (result == null || result!!.isEmpty()) {
            return WritableNativeMap()
        }
        val jsonMap = JSONObject("${result}")
        var resultsMap = WritableNativeMap()
        for (key in jsonMap.keys()) {
            resultsMap.putString(key, jsonMap[key].toString())
        }
        return resultsMap
    }

    /** There is no guarantee that the SDK will return a valid result code or message.
     * This function handles that scenario and rejects the promise.
     */
    private fun handleFailedResult(sdkResult: SdkResultParcelable, promise: Promise) {
        val message = sdkResult.message ?: "Unknown response."
        val errorCode = sdkResult.resultCode.toString()
        promise.reject(errorCode, message)
    }
}
