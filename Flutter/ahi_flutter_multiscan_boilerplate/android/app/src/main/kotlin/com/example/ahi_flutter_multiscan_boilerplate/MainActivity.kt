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

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.advancedhumanimaging.sdk.bodyscan.BodyScan
import com.advancedhumanimaging.sdk.common.IAHIPersistence
import com.advancedhumanimaging.sdk.common.IAHIScan
import com.advancedhumanimaging.sdk.common.models.AHIResult
import com.advancedhumanimaging.sdk.facescan.FaceScan
import com.advancedhumanimaging.sdk.fingerscan.FingerScan
import com.advancedhumanimaging.sdk.multiscan.AHIMultiScan
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*

enum class AHIMultiScanMethod(val methodKeys: String) {
    /** Default. */
    unknown(""),

    /** Requires a token String to be provided as an argument. */
    setupMultiScanSDK("setupMultiScanSDK"),

    /** Requires a Map object to be passed in containing 3 arguments. */
    authorizeUser("authorizeUser"),

    /** Will return a boolean */
    areAHIResourcesAvailable("areAHIResourcesAvailable"),

    /** A void function that will invoke the download of remote resources. */
    downloadAHIResources("downloadAHIResources"),

    /** Will return an integer for the bytes size. */
    checkAHIResourcesDownloadSize("checkAHIResourcesDownloadSize"),

    /** Requires a map object for the required user inputs and the payment type ("SUBSCRIBER" or "PAYG") */
    startFaceScan("startFaceScan"),

    /** Requires a map object for the required user inputs and the payment type ("SUBSCRIBER" or "PAYG") */
    startBodyScan("startBodyScan"),

    /** Requires a map object of the body scan results and returns a Map object. */
    getBodyScanExtras("getBodyScanExtras"),

    /** Returns the SDK status */
    getMultiScanStatus("getMultiScanStatus"),

    /** Returns a Map containing the SDK details. */
    getMultiScanDetails("getMultiScanDetails"),

    /** Returns the user authorization status of the SDK. */
    getUserAuthorizedState("getUserAuthorizedState"),

    /** Will deuathorize the user from the SDK. */
    deauthorizeUser("deauthorizeUser"),

    /** Released the actively registered SDK session. */
    releaseMultiScanSDK("releaseMultiScanSDK"),

    /** Use the AHIMultiScan persistence delegate and set historical body scan results */
    setMultiScanPersistenceDelegate("setMultiScanPersistenceDelegate"),
}

const val TAG = "MainActivityAHI"
const val PERMISSION_REQUEST_CODE = 111

class MainActivity : FlutterActivity() {
    private val CHANNEL = "ahi_multiscan_flutter_wrapper"

    @RequiresApi(Build.VERSION_CODES.N)
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            val method = AHIMultiScanMethod.valueOf(call.method)
            when (method) {
                AHIMultiScanMethod.setupMultiScanSDK -> {
                    setupMultiScanSDK(
                        token = call.arguments,
                        result = result
                    )
                }
                AHIMultiScanMethod.authorizeUser -> {
                    authorizeUser(
                        arguments = call.arguments,
                        result = result
                    )
                }
                AHIMultiScanMethod.areAHIResourcesAvailable -> {
                    areAHIResourcesAvailable(
                        result = result
                    )
                }
                AHIMultiScanMethod.downloadAHIResources -> {
                    downloadAHIResources()
                }
                AHIMultiScanMethod.checkAHIResourcesDownloadSize -> {
                    checkAHIResourcesDownloadSize(
                        result = result
                    )
                }
                AHIMultiScanMethod.startFaceScan -> {
                    startFaceScan(
                        arguments = call.arguments, result = result
                    )
                }
                AHIMultiScanMethod.startBodyScan -> {
                    startBodyScan(
                        arguments = call.arguments, result = result
                    )
                }
                AHIMultiScanMethod.getBodyScanExtras -> {
                    getBodyScanExtras(bodyScanResult = call.arguments, result = result)
                }
                AHIMultiScanMethod.getMultiScanStatus -> {
                    getMultiScanStatus(result = result)
                }
                AHIMultiScanMethod.getMultiScanDetails -> {
                    getMultiScanDetails(result = result)
                }
                AHIMultiScanMethod.getUserAuthorizedState -> {
                    getUserAuthorizedState(
                        userId = call.arguments,
                        result = result
                    )
                }
                AHIMultiScanMethod.deauthorizeUser -> {
                    deauthorizeUser(result = result)
                }
                AHIMultiScanMethod.releaseMultiScanSDK -> {
                    releaseMultiScanSDK(result = result)
                }
                AHIMultiScanMethod.setMultiScanPersistenceDelegate -> {
                    setPersistenceDelegate(results = call.arguments)
                }
                else -> {
                    Log.d("AHI ERROR", "AHI: Invalid method name.")
                }
            }
        }
    }

    /**
     *  Setup the MultiScan SDK
     *  This must happen before requesting a scan.
     *  We recommend doing this on successful load of your application.
     */
    private fun setupMultiScanSDK(token: Any?, result: MethodChannel.Result) {
        if (token == null || token !is String) {
            result.error("-1", "Missing multi scan token", null)
            return
        }
        val config: MutableMap<String, String> = HashMap()
        config["TOKEN"] = token as String
        val scans: Array<IAHIScan> = arrayOf(FaceScan(), FingerScan(), BodyScan())
        AHIMultiScan.setup(application, config, scans, completionBlock = {
            it.fold({
                    result.success(null)
            }, {
                Log.d(TAG, "AHI: Error setting up: $}\n")
                Log.d(TAG, "AHI: Confirm you have a valid token.\n")
                result.error(it.error.code().toString(), it.message, null)
            })
        })

    }

    /**
     *  Once successfully setup, you should authorize your user with our service.
     *  With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
     * */
    private fun authorizeUser(
        arguments: Any?,
        result: MethodChannel.Result
    ) {
        if (arguments == null || arguments !is Map<*, *>) {
            result.error("-2", "Missing user authorization details.", null)
            return
        }
        if (!arguments.contains("USER_ID") || !arguments.contains("SALT") || !arguments.contains("CLAIMS")) {
            result.error("-2", "Missing user authorization details.", null)
            return
        }
        val userID = arguments["USER_ID"] as? String ?: run {
            result.error("-2", "Missing user authorization details.", null)
            return
        }
        val salt = arguments["SALT"] as? String ?: run {
            result.error("-2", "Missing user authorization details.", null)
            return
        }
        val claims = arguments["CLAIMS"] as? ArrayList<String> ?: run {
            val claimsArgs = arguments["CLAIMS"]!!
            val typeOfClaimsArgs = claimsArgs.javaClass
            result.error(
                "-2",
                "Missing user authorization details.",
                "Claims is not correct format: $claimsArgs and $typeOfClaimsArgs"
            )
            return
        }
        val claimsArray: Array<String> = claims.toTypedArray()

        AHIMultiScan.userAuthorize(userID, salt, claimsArray, completionBlock = {
            it.fold({
                    result.success(null)
            }, {
                result.error(it.error.code().toString(), it.message, null)
            })
        })

    }

    /** Check if the AHI resources are downloaded.
     *
     * We have remote resources that exceed 100MB that enable our scans to work.
     * You are required to download them in order to obtain a body scan.
     *
     * This function checks if they are already downloaded and available for use.
     * */
    private fun areAHIResourcesAvailable(result: MethodChannel.Result) {
        AHIMultiScan.areResourcesDownloaded {
            it.fold({
                result.success(it)
            }, {
                Log.d(TAG, "AHI: Error in resource downloading \n")
                result.error(it.error.code().toString(), it.message, null)
            })
        }
    }

    /**
     *  Download scan resources.
     *  We recommend only calling this function once per session to prevent duplicate background resource calls.
     */
    private fun downloadAHIResources() {
        AHIMultiScan.downloadResourcesInForeground()
    }

    /** Check the size of the AHI resources that require downloading. */
    private fun checkAHIResourcesDownloadSize(result: MethodChannel.Result) {
        AHIMultiScan.totalEstimatedDownloadSizeInBytes {
            it.fold({
                Log.d(TAG, "AHI INFO: Size of download is ${it.progressBytes / 1024 / 1024} / ${it.totalBytes / 1024 / 1024}\n")
                result.success(it.progressBytes)
            }, {
                Log.e(TAG, it.message.toString())
                result.error(it.error.toString(), it.message, null)
            })
        }
    }

    private fun getFaceScanUserInput(arguments: Any?): HashMap<String, Any>? {
        if (arguments == null || arguments !is Map<*, *>) {
            return null
        }
        val enumEntSex = arguments["enum_ent_sex"] as? String ?: return null
        val cmEntHeight = arguments["cm_ent_height"] as? Int ?: return null
        val kgEntWeight = arguments["kg_ent_weight"] as? Int ?: return null
        val yrEntAge = arguments["yr_ent_age"] as? Int ?: return null
        val boolEntSmoker = arguments["bool_ent_smoker"] as? Boolean ?: return null
        val boolEntHypertension = arguments["bool_ent_hypertension"] as? Boolean ?: return null
        val boolEntBloodpressuremedication =
            arguments["bool_ent_bloodPressureMedication"] as? Boolean ?: return null
        val enumEntDiabetic = arguments["enum_ent_diabetic"] as? String ?: return null
        return hashMapOf(
            "enum_ent_sex" to enumEntSex,
            "cm_ent_height" to cmEntHeight,
            "kg_ent_weight" to kgEntWeight,
            "yr_ent_age" to yrEntAge,
            "bool_ent_smoker" to boolEntSmoker,
            "bool_ent_hypertension" to boolEntHypertension,
            "bool_ent_bloodPressureMedication" to boolEntBloodpressuremedication,
            "enum_ent_diabetic" to enumEntDiabetic,
        )
    }

    private fun startFaceScan(
        arguments: Any?,
        result: MethodChannel.Result
    ) {
        val userInput = getFaceScanUserInput(arguments)
        if (userInput == null) {
            result.error("-3", "Missing user face scan input details", null)
            return
        }

        /*
        AHIMultiScan.initiateScan("face", userInput, activityResultRegistry, completionBlock = {
            lifecycleScope.launch(Dispatchers.Main) {
                if (!it.isDone) {
                    Log.i(TAG, "Waiting of results, can show waiting screen here")
                }
                val result = withContext(Dispatchers.IO) { it.get() }
                when (result) {
                    is AHIResult.Success -> {
                        Log.d(TAG, "initiateScan: ${result.value}")
                    }
                    else -> {
//                        if (result.error() == AHIFaceScanError.FACE_SCAN_CANCELED) {
//                            Log.i(TAG, "User cancelled scan")
//                        } else {
//                            Log.d(TAG, "initiateScan: ${result.error()}")
//                        }
                    }
                }
            }
        })*/
    }

    private fun getBodyScanUserInput(arguments: Any?): HashMap<String, Any>? {
        if (arguments == null || arguments !is Map<*, *>) {
            return null
        }
        val enumEntSex = arguments["enum_ent_sex"] as? String ?: return null
        val cmEntHeight = arguments["cm_ent_height"] as? Int ?: return null
        val kgEntWeight = arguments["kg_ent_weight"] as? Int ?: return null
        return hashMapOf(
            "enum_ent_sex" to enumEntSex,
            "cm_ent_height" to cmEntHeight,
            "kg_ent_weight" to kgEntWeight
        )
    }

    private fun startBodyScan(
        arguments: Any?,
        result: MethodChannel.Result
    ) {
        val userInput = getBodyScanUserInput(arguments)
        if (userInput == null) {
            result.error("-5", "Missing user body scan input details", null)
            return
        }

//        AHIMultiScan.delegatePersistence = AHIPersistenceDelegate

        /*
        AHIMultiScan.initiateScan("body", userInput, activityResultRegistry, completionBlock = {
            lifecycleScope.launch(Dispatchers.Main) {
                if (!it.isDone) {
                    Log.i(TAG, "Waiting of results, can show waiting screen here")
                }

                val result = withContext(Dispatchers.IO) { it.get() }
                when (result) {
                    is AHIResult.Success -> {
                        Log.d(TAG, "initiateScan: ${result.value}")
                        // get scan extra
                        getBodyScanExtras(result.value)
                    }
                    else -> {
                        if (result.error() == BodyScanError.BODY_SCAN_CANCELED) {
                            Log.i(TAG, "User cancelled scan")
                        } else {
                            Log.d(TAG, "initiateScan: ${result.error()}")
                        }
                    }
                }
            }
        })
        */
    }

    /**
     *  Use this function to fetch the 3D avatar mesh.
     *  The 3D mesh can be created and returned at any time.
     *  We recommend doing this on successful completion of a body scan with the results.
     * */
    private fun getBodyScanExtras(bodyScanResult: Any?, result: MethodChannel.Result) {
        if (bodyScanResult == null || bodyScanResult !is HashMap<*, *>) {
            result.error("-8", "Missing valid body scan result.", null)
            return
        }
        val resultID = bodyScanResult["id"] as? String ?: run {
            result.error("-8", "Missing valid body scan result.", null)
            return
        }

        val options = mapOf("extrapolate" to listOf("mesh"))
        AHIMultiScan.getScanExtra(bodyScanResult as Map<String, Any>, options, completionBlock = {
            it.fold({
                val uri = (it["extrapolate"] as? List<Map<*, *>>)?.firstOrNull()?.get("mesh") as? Uri
                Log.i(TAG, "$uri")
                result.success(uri)
            }, {
                Log.e(TAG, it.toString())
                result.error(it.error.code().toString(), it.message, null)
            })
        })
    }

    /**
     * Check if MultiScan is on or offline.
     * */
    private fun getMultiScanStatus(result: MethodChannel.Result) {

        AHIMultiScan.getStatus {
            Log.d(TAG, "AHI INFO: Status: ${it.toString()}")
            result.success(it.toString())
        }
    }

    /**
     * Check your AHI MultiScan organisation details.
     * */
    private fun getMultiScanDetails(result: MethodChannel.Result) {
        AHIMultiScan.getDetails {
            it.fold({
                Log.d(TAG, "AHI INFO: MultiScan details: ${it}")
                result.success(it)
            }, {
                Log.d(TAG, "AHI INFO: Failed to get details")
                result.error(it.error.code().toString(), it.error.toString(), it)
            })
        }
    }

    /** Check if the user is authorized to use the MuiltScan service. */
    private fun getUserAuthorizedState(userId: Any?, result: MethodChannel.Result) {
        val userID = userId as? String
        if (userID.isNullOrEmpty()) {
            result.error("-9", "Missing user ID", null)
            return
        }

        AHIMultiScan.userIsAuthorized {
            it.fold({
                Log.d(TAG, "AHI INFO: User is authorized")
                result.success(null)
            }, {
                Log.d(TAG, "AHI INFO: User is not authorized")
                result.error(it.error.code().toString(), it.error.toString(), it)
            })
        }
    }

    /**
     * Deuauthorize the user.
     * */
    private fun deauthorizeUser(result: MethodChannel.Result) {

        AHIMultiScan.userDeauthorize {
            it.fold({
                Log.d(TAG, "AHI INFO: User is deauthorized.")
                result.success(null)
            }, {
                Log.e(TAG, it.toString())
                result.error(it.error.code().toString(), it.error.toString(), it)
            })
        }
    }

    /**
     * Release the MultiScan SDK session.
     *
     * If you  use this, you will need to call setupSDK again.
     * */
    private fun releaseMultiScanSDK(result: MethodChannel.Result) {
        AHIMultiScan.releaseSdk {
            it.fold({
                Log.d(TAG, "AHI INFO: SDK is released.")
                result.success(null)
            }, {
                Log.e(TAG, it.toString())
            })
        }
    }

    /** The MultiScan SDK can provide personalised results.
     *
     * Optionally call this function on load of the SDK.
     * */
    private fun setPersistenceDelegate(results: Any?) {
        val bodyScanResults = convertBodyScanResultsToSDKFormat(results)
        AHIPersistenceDelegate.let { it ->
            it.bodyScanResults = bodyScanResults
            AHIMultiScan.delegatePersistence = it
        }
    }

    /** For the newest AHIMultiScan version 21.1.3 need to implement PersistenceDelegate */
    object AHIPersistenceDelegate : IAHIPersistence {
        /** You should have your body scan results stored somewhere in your app that this function can access.*/
        var bodyScanResults = mutableListOf<String>()

        override fun request(
            scanType: String,
            options: Map<String, Any>,
            completionBlock: (result: AHIResult<Array<Map<String, Any>>>) -> Unit
        ) {
//            TODO("Not yet implemented")
        }
    }

    /** The Android MultiScan SDK is currently returning results and a JSONString which needs to be converted to a Map to ensure consistency with Flutter and iOS. */ 
    private fun scanResultsToMap(results: String?): Map<String, Any> {
        if (results == null) {
            return emptyMap<String, Any>()
        }
        val jsonObject = JSONObject("""${results}""")
        val map = mutableMapOf<String, Any>()
        for (key in jsonObject.keys()) {
            map[key] = jsonObject[key]
        }
        return map
    }

    private fun convertBodyScanResultsToSDKFormat(result: Any?): MutableList<String> {
        val resultsList = result as? List<Map<String, Any>> ?: emptyList<Map<String, Any>>()
        if (resultsList == null) {
            Log.d("AHI ERROR:", "AHI ERROR: KOTLIN: Body Scan results format was not [ {String: Any} ].")
            return arrayListOf<String>()
        }
        val bodyScanResultsStringList = resultsList.map {
            it.toString()
        }.toMutableList()
        return bodyScanResultsStringList
    }
}
