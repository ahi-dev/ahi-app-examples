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

    // Default.
    @ReactMethod
    fun unknow(promise: Promise) {
        promise.resolve(null)
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
                    promise.resolve(it.resultCode.toString())
                }
                SdkResultCode.ERROR -> {
                    promise.reject(it.resultCode.toString(), it.message)
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
    fun authorizeUser(userId: String, salt: String, claims: ReadableArray, promise: Promise) {
        // Convert claims to an Array<String>
        val claimsArray: Array<String> = claims.toArrayList().map { it.toString() }.toTypedArray()
        MultiScan.waitForResult(MultiScan.shared().userAuthorize(userId, salt, claimsArray)) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    promise.resolve(it.resultCode.toString())
                }
                SdkResultCode.ERROR -> {
                    promise.reject(it.resultCode.toString(), it.message)
                }
            }
        }
    }

    /** Check if the AHI resources are downloaded. */
    @ReactMethod
    fun areAHIResourcesAvailable(promise: Promise) {
        MultiScan.waitForResult(MultiScan.shared().areResourcesDownloaded()) { promise.resolve(it) }
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
            promise.resolve("$it")
        }
    }

    @ReactMethod
    fun startFaceScan(userInputAvatarMap: ReadableMap, paymentType: String, promise: Promise) {
        val pType =
            when (paymentType) {
                "PAYG" -> MSPaymentType.PAYG
                "SUBS" -> MSPaymentType.SUBS
                else -> null
            }
        if (pType == null) {
            promise.reject("-99", "invalid payment type.")
            return
        }
        val faceScanValues = userInputConverter(userInputAvatarMap)
        MultiScan.waitForResult(
            MultiScan.shared().initiateScan(MSScanType.FACE, pType, faceScanValues)
        ) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    val result = convertJSONStringToMap(it.result)
                    promise.resolve(result)
                }
                SdkResultCode.ERROR -> {
                    promise.reject(it.resultCode.toString(), it.message)
                }
            }
        }
    }

    @ReactMethod
    fun startBodyScan(userInputAvatarMap: ReadableMap, paymentType: String, promise: Promise) {
        val pType =
            when (paymentType) {
                "PAYG" -> MSPaymentType.PAYG
                "SUBS" -> MSPaymentType.SUBS
                else -> null
            }
        if (pType == null) {
            promise.reject("-99", "invalid payment type.")
            return
        }
        // Before we feed to SDK we need to mapping the keys and values to reach the sdk needs.
        MultiScan.shared().registerDelegate(AHIPersistenceDelegate)
        val bodyScanValues = userInputConverter(userInputAvatarMap)
        MultiScan.waitForResult(
            MultiScan.shared().initiateScan(MSScanType.BODY, pType, bodyScanValues)
        ) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    val resultsMap = convertJSONStringToMap(it.result)
                    promise.resolve(resultsMap)
                }
                SdkResultCode.ERROR -> {
                    promise.reject(it.resultCode.toString(), it.message)
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
        val result = bodyScanResult.toHashMap()
        val id = result["id"].toString()
        val parameters: MutableMap<String, Any> = HashMap()
        parameters["operation"] = MultiScanOperation.BodyGetMeshObj.name
        parameters["id"] = id
        /** Write the mesh to a directory */
        val objFile = File(context.filesDir, "$id.obj")
        MultiScan.waitForResult(MultiScan.shared().getScanExtra(MSScanType.BODY, parameters)) {
            /** Print the 3D mesh path */
            saveAvatarToFile(it, objFile)
            val map = WritableNativeMap()
            map.putString("meshURL",objFile.path.toString())
            promise.resolve(map)
        }
    }

    /** Check if MultiScan is on or offline. */
    @ReactMethod
    fun getMultiScanStatus(promise: Promise) {
        MultiScan.waitForResult(MultiScan.shared().state) { promise.resolve(it.result) }
    }

    /** Check your AHI MultiScan organisation details. */
    @ReactMethod
    fun getMultiScanDetails(promise: Promise) {
        promise.resolve(null)
    }

    /** Check if the userr is authorized to use the MuiltScan service. */
    @ReactMethod
    fun getUserAuthorizedState(userId: String?, promise: Promise) {
        if (userId.isNullOrEmpty()) {
            promise.reject("ERROR", "Missing user ID")
            return
        }
        MultiScan.waitForResult(MultiScan.shared().userIsAuthorized(userId)) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    promise.resolve(it.result)
                }
                SdkResultCode.ERROR -> {
                    promise.reject(it.resultCode.toString(), it.message)
                }
            }
        }
    }

    /** Deuathorize the user. */
    @ReactMethod
    fun deauthorizeUser(promise: Promise) {
        MultiScan.waitForResult(MultiScan.shared().userDeauthorize()) {
            promise.reject(it.resultCode.toString(), it.message)
        }
    }

    /**
     * Release the MultiScan SDK session.
     *
     * If you use this, you will need to call setupSDK again.
     */
    @ReactMethod
    fun releaseMultiScanSDK(promise: Promise) {
        promise.resolve(null)
    }

    /**
     * The MultiScan SDK can provide personalised results.
     *
     * Optionally call this function on load of the SDK.
     */
    @ReactMethod
    fun setMultiScanPersistenceDelegate(results: ReadableArray) {
        AHIPersistenceDelegate.let {
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
    private fun saveAvatarToFile(res: SdkResultParcelable, objFile: File) {
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
    }

    private fun userInputConverter(userInputAvatarMap: ReadableMap): Map<String, Any?> {
        // Convert the schema and feed to SDK
        val inputAvatarValues = userInputAvatarMap.toHashMap()
        val sex = when (inputAvatarValues["enum_ent_sex"]) {
            "male" -> "M"
            else -> "F"
        }
        val smoker = when (inputAvatarValues["bool_ent_smoker"]) {
            true -> "T"
            else -> "F"
        }
        val hypertension = when (inputAvatarValues["bool_ent_hypertension"]) {
            true -> "T"
            else -> "F"
        }
        val bpmds = when (inputAvatarValues["bool_ent_bloodPressureMedication"]) {
            true -> "T"
            else -> "F"
        }

        val convertedSchema = mapOf(
            "TAG_ARG_GENDER" to sex,
            "TAG_ARG_SMOKER" to smoker,
            "TAG_ARG_DIABETIC" to inputAvatarValues["enum_ent_diabetic"],
            "TAG_ARG_HYPERTENSION" to hypertension,
            "TAG_ARG_BPMEDS" to bpmds,
            "TAG_ARG_HEIGHT_IN_CM" to inputAvatarValues["cm_ent_height"],
            "TAG_ARG_WEIGHT_IN_KG" to inputAvatarValues["kg_ent_weight"],
            "TAG_ARG_AGE" to inputAvatarValues["yr_ent_age"],
        )
        return convertedSchema
    }

    private fun convertJSONStringToMap(result: String?): WritableNativeMap {
        if (result == null || result!!.isEmpty()) {
            return WritableNativeMap()
        }
        val jsonMap =  JSONObject("${result}")
        var resultsMap = WritableNativeMap()
        for (key in jsonMap.keys()) {
            resultsMap.putString(key,jsonMap[key].toString())
        }
        return resultsMap
    }
}
