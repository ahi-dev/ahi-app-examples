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
import com.facebook.react.bridge.*
import com.myfiziq.sdk.MultiScan
import com.myfiziq.sdk.MultiScanDelegate
import com.myfiziq.sdk.MultiScanOperation
import com.myfiziq.sdk.enums.MSPaymentType
import com.myfiziq.sdk.enums.MSScanType
import com.myfiziq.sdk.enums.SdkResultCode
import com.myfiziq.sdk.vo.SdkResultParcelable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.CompletableFuture

private const val TAG = "MultiScanModule"

class MultiScanModule(private val context: ReactApplicationContext) :
    ReactContextBaseJavaModule(context) {
    override fun getName(): String {
        return "MultiScanModule"
    }

    /** Check the size of the AHI resources that require downloading. */
    @ReactMethod
    fun checkAHIResourcesDownloadSize(promise: Promise) {
        MultiScan.waitForResult(MultiScan.shared().totalEstimatedDownloadSizeInBytes()) {
            promise.resolve("$it")
        }
    }

    /** Check if the AHI resources are downloaded. */
    @ReactMethod
    fun areAHIResourcesAvailable(promise: Promise) {
        MultiScan.waitForResult(MultiScan.shared().areResourcesDownloaded()) {
            if (!it) {
                promise.resolve(it)
                GlobalScope.launch {
                    delay(30000)
                    checkAHIResourcesDownloadSize(promise)
                    areAHIResourcesAvailable(promise)
                }
            } else {
                promise.resolve(it)
            }
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
        val claimsArray = arrayOf(claims.getString(0))
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

    @ReactMethod
    fun startFaceScan(msPaymentType: String, avatarValues: ReadableMap, promise: Promise) {
        var paymentType = when (msPaymentType) {
            "PAYG" -> MSPaymentType.PAYG
            "SUBS" -> MSPaymentType.SUBS
            else -> null
        }
        MultiScan.waitForResult(

            MultiScan.shared()
                .initiateScan(MSScanType.FACE, paymentType, avatarValues.toHashMap())
        ) {
            /** Result check */
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

    @ReactMethod
    fun startBodyScan(msPaymentType: String, avatarValues: ReadableMap, promise: Promise) {
        var payment = when (msPaymentType) {
            "PAYG" -> MSPaymentType.PAYG
            "SUBS" -> MSPaymentType.SUBS
            else -> null
        }
        MultiScan.shared().registerDelegate(AHIPersistenceDelegate)
        MultiScan.waitForResult(
            MultiScan.shared()
                .initiateScan(MSScanType.BODY, payment, avatarValues.toHashMap())
        ) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    val res = JSONObject(it.result)
                    val id = res["id"].toString()
                    if (areBodyScanSmoothingResultsValid(it.resultMap)) {
                        getBodyScanExtras(id, promise)
                    }
                    promise.resolve(
                        "AHI: SCAN RESULT: ${it.result}\nAHI: Mesh URL: ${context.filesDir.path}/$id.obj"
                    )
                }
                SdkResultCode.ERROR -> {
                    promise.reject(
                        it.resultCode.toString(), "AHI: ERROR WITH BODY SCAN: ${it.message}"
                    )
                }
            }
        }
    }

    /**
     * Use this function to fetch the 3D avatar mesh. The 3D mesh can be created and returned at any
     * time. We recommend doing this on successful completion of a body scan with the results.
     */
    private fun getBodyScanExtras(id: String, promise: Promise) {
        val parameters: MutableMap<String, Any> = HashMap()
        parameters["operation"] = MultiScanOperation.BodyGetMeshObj.name
        parameters["id"] = id
        MultiScan.waitForResult(MultiScan.shared().getScanExtra(MSScanType.BODY, parameters)) {
            /** Write the mesh to a directory */
            val objFile = File(context.filesDir, "$id.obj")
            /** Print the 3D mesh path */
            saveAvatarToFile(it, objFile)
            /** Return the URL */
            Log.d(TAG, "AHI: Mesh URL: ${context.filesDir.path}/$id.obj\n")
        }
    }

    /** For the newest AHIMultiScan version 21.1.3 need to implement PersistenceDelegate */
    object AHIPersistenceDelegate : MultiScanDelegate {
        override fun request(
            scanType: MSScanType?,
            options: MutableMap<String, String>?
        ): CompletableFuture<SdkResultParcelable> {
            val future = CompletableFuture<SdkResultParcelable>()
            if (scanType == MSScanType.BODY) {
                val rawResultList = mutableListOf<String>()
                options?.forEach { rawResultList.add(it.toString()) }
                val jsonArrayString = "[" + rawResultList.joinToString(separator = ",") + "]"
                future.complete(SdkResultParcelable(SdkResultCode.SUCCESS, jsonArrayString))
            } else {
                future.complete(SdkResultParcelable(SdkResultCode.ERROR, ""))
            }
            return future
        }
    }

    /** Confirm results have correct set of keys. */
    private fun areBodyScanSmoothingResultsValid(it: MutableMap<String, String>): Boolean {
        // Your token may only provide you access to a smaller subset of results.
        // You should modify this list based on your available config options.
        val sdkResultSchema =
            listOf(
                "enum_ent_sex",
                "cm_ent_height",
                "kg_ent_weight",
                "cm_raw_chest",
                "cm_raw_hips",
                "cm_raw_inseam",
                "cm_raw_thigh",
                "cm_raw_waist",
                "kg_raw_weightPredict",
                "percent_raw_bodyFat",
                "id",
                "date"
            )
        var isValid = false
        /** Iterate over results */
        sdkResultSchema.forEach { str ->
            /** Check if keys in results contains the required keys. */
            if (!it.keys.contains(str)) {
                isValid = true
            }
        }
        return !isValid
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
}
