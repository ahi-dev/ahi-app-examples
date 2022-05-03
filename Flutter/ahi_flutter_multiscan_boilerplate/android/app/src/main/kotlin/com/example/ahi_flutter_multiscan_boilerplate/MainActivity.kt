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
    startFaceScan("startFaceScan" ),
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
//        val  ahiMultiScanMethod: AHIMultiScanMethod = AHIMultiScanMethod.setupMultiScanSDK
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            var method = AHIMultiScanMethod.valueOf(call.method)
            Log.d(TAG, "configureFlutterEngine: $method")
//            AHIMultiScanMethod(call.method)
//            Log.d(TAG, "call -> ${call.method}, call.arguments -> ${call.arguments} ")
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
//                "startFaceScan" -> {
//                    startFaceScan(paymentType = call.argument<String>("Payment_Type")!!,
//                    avatarValues = call.arguments as HashMap<String, Any>, result = result)
//                }
//                "areAHIResourcesAvailable" -> {
//                    areAHIResourcesAvailable(result = result)
//                }
//                "downloadAHIResources" ->{
//                    downloadAHIResources();
//                }
//                "checkAHIResourcesDownloadSize" -> {
//                    checkAHIResourcesDownloadSize(result = result)
//                }
//                "startBodyScan" -> {
//                    startBodyScan(paymentType = call.argument<String>("Payment_Type")!!,
//                        avatarValues = call.arguments as HashMap<String, Any>, result = result)
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
    // todo this looks good, leave
    private fun  downloadAHIResources() {
        MultiScan.shared().downloadResourcesInBackground()
    }

    /**
     *  Setup the MultiScan SDK
     *  This must happen before requesting a scan.
     *  We recommend doing this on successful load of your application.
     */
    // todo this looks good I think, not sure will have to ask to confirm
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
    // todo this looks good I think, not sure will have to ask to confirm
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

    private fun startFaceScan(paymentType: String, avatarValues: HashMap<String, Any>, result: MethodChannel.Result) {
        val pType = when (paymentType) {
            "PAYG" -> MSPaymentType.PAYG
            "SUBS" -> MSPaymentType.SUBS
            else -> null
        }
        if (pType == null) {
            result.error("-99", "invalid payment type.", null)
            return
        }
        // todo the below is handle by flutter
        MultiScan.waitForResult(
            MultiScan.shared().initiateScan(MSScanType.FACE, pType, avatarValues)
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

    fun startBodyScan(paymentType: String, avatarValues: HashMap<String, Any>, result: MethodChannel.Result) {
        val pType = when (paymentType) {
            "PAYG" -> MSPaymentType.PAYG
            "SUBS" -> MSPaymentType.SUBS
            else -> null
        }
        if (pType == null) {
            result.error("-99", "invalid payment type.", null)
            return
        }
        MultiScan.shared().registerDelegate(AHIPersistenceDelegate)
        MultiScan.waitForResult(
            MultiScan.shared()
                .initiateScan(MSScanType.BODY, pType, avatarValues)
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
    // todo stays
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
/*******************************************************************************************************************************/
/*******************************************************************************************************************************/
/*******************************************************************************************************************************/

    /**
     *  All MultiScan scan configs require this information.
     *
     *  BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/
     *  FaceScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
     * */
    // todo this should be in flutter
    private fun areSharedScanConfigOptionsValid(avatarValues: java.util.HashMap<String, Any>): Boolean {
        val sex = avatarValues["TAG_ARG_GENDER"].takeIf { it is String }
        val height = avatarValues["TAG_ARG_HEIGHT_IN_CM"].takeIf { it is Int }
        val weight = avatarValues["TAG_ARG_WEIGHT_IN_KG"].takeIf { it is Int }
        return if (sex != null && height != null && weight != null) {
            arrayListOf("M", "F").contains(sex)
        } else {
            false
        }
    }

    /**
     *  FaceScan config requirements validation.
     *  Please see the Schemas for more information:
     *  FaceScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
     * */
    // todo this should be in flutter
    private fun areFaceScanConfigOptionsValid(avatarValues: HashMap<String, Any>): Boolean {
        if (!areSharedScanConfigOptionsValid(avatarValues)) {
            return false
        }
        val sex = avatarValues["TAG_ARG_GENDER"].takeIf { it is String }
        val smoke = avatarValues["TAG_ARG_SMOKER"].takeIf { it is String }
        val isDiabetic = avatarValues["TAG_ARG_DIABETIC"].takeIf { it is String }
        val hypertension = avatarValues["TAG_ARG_HYPERTENSION"].takeIf { it is String }
        val blood = avatarValues["TAG_ARG_BPMEDS"].takeIf { it is String }
        val height = avatarValues["TAG_ARG_HEIGHT_IN_CM"].takeIf { it is Int }
        val weight = avatarValues["TAG_ARG_WEIGHT_IN_KG"].takeIf { it is Int }
        val age = avatarValues["TAG_ARG_AGE"].takeIf { it is Int }
        val heightUnits = avatarValues["TAG_ARG_PREFERRED_HEIGHT_UNITS"].takeIf { it is String }
        val weightUnits = avatarValues["TAG_ARG_PREFERRED_WEIGHT_UNITS"].takeIf { it is String }
        if (sex != null &&
            smoke != null &&
            isDiabetic != null &&
            hypertension != null &&
            blood != null &&
            height != null &&
            weight != null &&
            age != null &&
            heightUnits != null &&
            weightUnits != null &&
            height in 25..300 &&
            weight in 25..300 &&
            age in 13..120
        ) {
            return arrayListOf("none", "type1", "type2").contains(isDiabetic)
        } else {
            return false
        }
    }

    /**
     *  BodyScan config requirements validation.
     *  Please see the Schemas for more information:
     *  BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/
     * */
    // todo this should be in flutter
    private fun areBodyScanConfigOptionsValid(avatarValues: java.util.HashMap<String, Any>): Boolean {
        if (!areSharedScanConfigOptionsValid(avatarValues)) {
            return false
        }
        val sex = avatarValues["TAG_ARG_GENDER"].takeIf { it is String }
        val height = avatarValues["TAG_ARG_HEIGHT_IN_CM"].takeIf { it is Int }
        val weight = avatarValues["TAG_ARG_WEIGHT_IN_KG"].takeIf { it is Int }
        if (sex != null &&
            height != null &&
            weight != null &&
            height in 50..255 &&
            weight in 16..300
        ) {
            return true
        }
        return false
    }

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