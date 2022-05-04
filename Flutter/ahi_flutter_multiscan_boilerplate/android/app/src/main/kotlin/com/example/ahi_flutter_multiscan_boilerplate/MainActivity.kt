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

package com.example.ahi_flutter_multiscan_boilerplate

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.myfiziq.sdk.MultiScan
import com.myfiziq.sdk.MultiScanDelegate
import com.myfiziq.sdk.MultiScanOperation
import com.myfiziq.sdk.enums.MSPaymentType
import com.myfiziq.sdk.enums.MSScanType
import com.myfiziq.sdk.enums.SdkResultCode
import com.myfiziq.sdk.vo.SdkResultParcelable
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.DelicateCoroutinesApi
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.CompletableFuture

private const val TAG = "MainActivity"

enum class AHIMultiScanMethod(val methodKeys: String) {
    unknown(""),
    setupMultiScanSDK("setupMultiScanSDK"),
    authorizeUser("authorizeUser"),
    areAHIResourcesAvailable("areAHIResourcesAvailable"),
    downloadAHIResources("downloadAHIResources"),
    checkAHIResourcesDownloadSize("checkAHIResourcesDownloadSize"),
    startFaceScan("startFaceScan"),
    startBodyScan("startBodyScan"),
    getBodyScanExtras("getBodyScanExtras"),
    getMultiScanStatus("getMultiScanStatus"),
    getMultiScanDetails("getMultiScanDetails"),
    getUserAuthorizedState("getUserAuthorizedState"),
    deauthorizeUser("deauthorizeUser"),
    releaseMultiScanSDK("releaseMultiScanSDK"), // todo notes: dont have this
    setMultiScanPersistenceDelegate("setMultiScanPersistenceDelegate"),
}

class MainActivity : FlutterActivity() {
    private val CHANNEL = "ahi_multiscan_flutter_wrapper"

    @RequiresApi(Build.VERSION_CODES.N)
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            var method = AHIMultiScanMethod.valueOf(call.method)
            Log.d(TAG, "configureFlutterEngine: $method")
            when (method) {
                 AHIMultiScanMethod.setupMultiScanSDK -> {
                    setupMultiScanSDK(
                        token = call.argument("AHI_MULTI_SCAN_TOKEN")!!,
                        result = result
                    )
                }
                AHIMultiScanMethod.authorizeUser -> {
                    authorizeUser(
                        userId = call.argument("AHI_TEST_USER_ID")!!,
                        salt = call.argument("AHI_TEST_USER_SALT")!!,
                        claims = call.argument("AHI_TEST_USER_CLAIMS")!!, result = result
                    )
                }
                AHIMultiScanMethod.areAHIResourcesAvailable -> {
                    areAHIResourcesAvailable(result = result)
                }
                AHIMultiScanMethod.downloadAHIResources ->{
                    downloadAHIResources();
                }
                AHIMultiScanMethod.checkAHIResourcesDownloadSize -> {
                    checkAHIResourcesDownloadSize(result = result)
                }
                AHIMultiScanMethod.startFaceScan -> {
                    startFaceScan(paymentType = call.argument<String>("Payment_Type")!!,
                        userInputAvatarMap = call.arguments as HashMap<String, Any>, result = result)
                }
                AHIMultiScanMethod.startBodyScan -> {
                    startBodyScan(paymentType = call.argument<String>("Payment_Type")!!,
                        userInputAvatarMap = call.arguments as HashMap<String, Any>, result = result)
                }
                AHIMultiScanMethod.getBodyScanExtras -> {
                    getBodyScanExtras(id = call.argument("id")!!, result = result)
                }
                AHIMultiScanMethod.getMultiScanStatus -> {
                    getMultiScanStatus(result = result)
                }
                AHIMultiScanMethod.getMultiScanDetails -> {
                    getMultiScanDetails(result = result)
                }
                AHIMultiScanMethod.getUserAuthorizedState -> {
                    getUserAuthorizedState(userId = call.argument("AHI_TEST_USER_ID")!!, result = result)
                }
                AHIMultiScanMethod.deauthorizeUser -> {
                    deauthorizeUser(result = result)
                }
                AHIMultiScanMethod.releaseMultiScanSDK -> {
                    releaseMultiScanSDK(result = result)
                }
                // todo will need the method below
//                AHIMultiScanMethod.setMultiScanPersistenceDelegate -> {
//                    setPersistenceDelegate(results = call.argument("AHI_TEST_USER_CLAIMS")!!)
//                }
                else -> {
                    Log.d(TAG, "fail")
                }
            }
        }
    }


    /** Check the size of the AHI resources that require downloading. */
    private fun checkAHIResourcesDownloadSize(result: MethodChannel.Result) {
        MultiScan.waitForResult(MultiScan.shared().totalEstimatedDownloadSizeInBytes()) {
            result.success(it)
        }
    }

    /** Check if the AHI resources are downloaded. */
    @OptIn(DelicateCoroutinesApi::class)
    private fun areAHIResourcesAvailable(result: MethodChannel.Result) {
        MultiScan.waitForResult(MultiScan.shared().areResourcesDownloaded()) {
         result.success(it)
        }
    }

    /**
     *  Download scan resources.
     *  We recommend only calling this function once per session to prevent duplicate background resource calls.
     */
    private fun  downloadAHIResources() {
        MultiScan.shared().downloadResourcesInBackground()
    }

    /**
     *  Setup the MultiScan SDK
     *  This must happen before requesting a scan.
     *  We recommend doing this on successful load of your application.
     */
    private fun setupMultiScanSDK(token: String, result: MethodChannel.Result) {
        val config: MutableMap<String, String> = HashMap()
        config["TOKEN"] = token
        MultiScan.waitForResult(MultiScan.shared().setup(config)) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    MultiScan.shared().registerDelegate(AHIPersistenceDelegate)
                    result.success(it.resultCode.toString())
                }
                SdkResultCode.ERROR -> {
                    result.error(it.resultCode.toString(), it.message, null)
                }
            }
        }
    }

    /**
     *  Once successfully setup, you should authorize your user with our service.
     *  With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
     * */
    private fun authorizeUser(
        userId: String,
        salt: String,
        claims: ArrayList<String>,
        result: MethodChannel.Result
    ) {
        MultiScan.waitForResult(
            MultiScan.shared().userAuthorize(userId, salt, arrayOf(claims[0]))
        ) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    result.success(it.resultCode.toString())
                }
                SdkResultCode.ERROR -> {
                    result.error(it.resultCode.toString(), it.message, null)
                }
            }
        }
    }

    private fun startFaceScan(paymentType: String, userInputAvatarMap: HashMap<String, Any>, result: MethodChannel.Result) {
        val pType = when (paymentType) {
            "PAYG" -> MSPaymentType.PAYG
            "SUBS" -> MSPaymentType.SUBS
            else -> null
        }
        if (pType == null) {
            result.error("-99", "invalid payment type.", null)
            return
        }
        // Before we feed to SDK we need to mapping the keys and values to reach the sdk needs.
        val sdkStandradSchema = userInputConverter(userInputAvatarMap)
        MultiScan.waitForResult(
            MultiScan.shared().initiateScan(MSScanType.FACE, pType, sdkStandradSchema)
        ) {
            /** Result check */
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    result.success(it.result)
                }
                SdkResultCode.ERROR -> {
                    result.error(
                        it.resultCode.toString(), it.message, null
                    )
                }
            }
        }
    }

    private fun startBodyScan(paymentType: String, userInputAvatarMap: HashMap<String, Any>, result: MethodChannel.Result) {
        val pType = when (paymentType) {
            "PAYG" -> MSPaymentType.PAYG
            "SUBS" -> MSPaymentType.SUBS
            else -> null
        }
        if (pType == null) {
            result.error("-99", "invalid payment type.", null)
            return
        }
        val sdkStandradSchema = userInputConverter(userInputAvatarMap)
        MultiScan.shared().registerDelegate(AHIPersistenceDelegate)
        MultiScan.waitForResult(
            MultiScan.shared()
                .initiateScan(MSScanType.BODY, pType, sdkStandradSchema)
        ) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    result.success(
                        it.result
                    )
                }
                SdkResultCode.ERROR -> {
                    result.error(
                        it.resultCode.toString(), it.message, null
                    )
                }
            }
        }
    }

    /**
     *  Use this function to fetch the 3D avatar mesh.
     *  The 3D mesh can be created and returned at any time.
     *  We recommend doing this on successful completion of a body scan with the results.
     * */
    private fun getBodyScanExtras(id: String, result: MethodChannel.Result) {
        val parameters: MutableMap<String, Any> = HashMap()
        parameters["operation"] = MultiScanOperation.BodyGetMeshObj.name
        parameters["id"] = id
        /** Write the mesh to a directory */
        val objFile = File(context.filesDir, "$id.obj")
        MultiScan.waitForResult(MultiScan.shared().getScanExtra(MSScanType.BODY, parameters)) {
            /** Print the 3D mesh path */
            saveAvatarToFile(it, objFile)
        }
        result.success(objFile.path);
    }

    /**
     * Check if MultiScan is on or offline.
     * */
    fun getMultiScanStatus(result: MethodChannel.Result) {
        MultiScan.waitForResult(MultiScan.shared().state) {
            result.success(it.result)
        }
    }

    /**
     * Check your AHI MultiScan organisation  details.
     * */
    fun getMultiScanDetails(result: MethodChannel.Result) {
        result.success(null)
    }

    fun getUserAuthorizedState(userId: String?, result: MethodChannel.Result) {
        if (userId.isNullOrEmpty()) {
            result.error("ERROR", "Missing user ID", null)
            return
        }
        MultiScan.waitForResult(MultiScan.shared().userIsAuthorized(userId)) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    result.success(it.result)
                }
                SdkResultCode.ERROR -> {
                    result.error(it.resultCode.toString(),it.message, null)
                }
            }
        }
    }

    /**
     * Deuathorize the user.
     * */
    fun deauthorizeUser(result: MethodChannel.Result){
        MultiScan.waitForResult(MultiScan.shared().userDeauthorize()){
            result.error(it.resultCode.toString(),it.message, null)
        }
    }

    /**
     * Release the MultiScan SDK session.
     *
     * If you  use this, you will need to call setupSDK again.
     * */
    fun releaseMultiScanSDK(result: MethodChannel.Result){
        result.success(null)
    }

    /** The MultiScan SDK can provide personalised results.
     *
     * Optionally call this function on load of the SDK.
     * */
    fun setPersistenceDelegate(results: ArrayList<String>) {
        AHIPersistenceDelegate.let {
            it.bodyScanResults = results.map {
                it
            }.toMutableList()
            MultiScan.shared().registerDelegate(it)
        }
    }


    /** For the newest AHIMultiScan version 21.1.3 need to implement PersistenceDelegate */
    object AHIPersistenceDelegate : MultiScanDelegate {
        /** You should have your body scan results stored somewhere in your app that this function can access.*/
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

    private fun userInputConverter(userInputAvatarMap: HashMap<String, Any>): Map<String, Any?> {
        // Convert the schema and feed to SDK
        val inputAvatarValues = userInputAvatarMap
        val newSchema = mapOf(
            "TAG_ARG_GENDER" to inputAvatarValues["sex"],
            "TAG_ARG_SMOKER" to inputAvatarValues["smoker"],
            "TAG_ARG_DIABETIC" to inputAvatarValues["diabetic"],
            "TAG_ARG_HYPERTENSION" to inputAvatarValues["hypertension"],
            "TAG_ARG_BPMEDS" to inputAvatarValues["bpmeds"],
            "TAG_ARG_HEIGHT_IN_CM" to inputAvatarValues["height"],
            "TAG_ARG_WEIGHT_IN_KG" to inputAvatarValues["weight"],
            "TAG_ARG_AGE" to inputAvatarValues["age"]
        )
        return newSchema
    }
/*******************************************************************************************************************************/
/*******************************************************************************************************************************/
/*******************************************************************************************************************************/

    /** Confirm results have correct set of keys. */
    // todo this should be in flutter
    private fun areBodyScanSmoothingResultsValid(it: MutableMap<String, String>): Boolean {
        // Your token may only provide you access to a smaller subset of results.
        // You should modify this list based on your available config options.
        val sdkResultSchema = listOf(
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
}