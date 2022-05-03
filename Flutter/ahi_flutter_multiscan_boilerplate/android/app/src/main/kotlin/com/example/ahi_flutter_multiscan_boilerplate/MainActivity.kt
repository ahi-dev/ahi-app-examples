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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.CompletableFuture

private const val TAG = "MainActivity"

class MainActivity : FlutterActivity() {
    private val CHANNEL = "ahi_multiscan_flutter_wrapper"

    @RequiresApi(Build.VERSION_CODES.N)
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
//            Log.d(TAG, "call -> ${call.method}, call.arguments -> ${call.arguments} ")
            when (call.method) {
                "setupMultiScanSDK" -> {
                    setupMultiScanSDK(
                        token = call.argument("AHI_MULTI_SCAN_TOKEN")!!,
                        result = result
                    )
                }
                "authorizeUser" -> {
                    authorizeUser(
                        userId = call.argument("AHI_TEST_USER_ID")!!,
                        salt = call.argument("AHI_TEST_USER_SALT")!!,
                        claims = call.argument("AHI_TEST_USER_CLAIMS")!!, result = result
                    )
                }
                "startFaceScan" -> {
                    startFaceScan(dataMap = call.arguments, result = result)
                }
                "didTapDownloadResources" -> {
                    areAHIResourcesAvailable(result = result)
                }
                "downloadAHIResources" ->{
                    downloadAHIResources(result = result);
                }
                "checkAHIResourcesDownloadSize" -> {
                    Log.d(TAG, "configureFlutterEngine: 213213123")
                    checkAHIResourcesDownloadSize(result = result)
                }
                "startBodyScan" -> {
                    startBodyScan(result = result)
                }
                else -> {
                    Log.d(TAG, "fail")
                }
            }
        }
    }


    /** Check the size of the AHI resources that require downloading. */

    private fun checkAHIResourcesDownloadSize(result: MethodChannel.Result) {
        MultiScan.waitForResult(MultiScan.shared().totalEstimatedDownloadSizeInBytes()) {
            result.success("AHI INFO: Size of download is ${it / 1024 / 1024}")
        }
    }

    /** Check if the AHI resources are downloaded. */

    @OptIn(DelicateCoroutinesApi::class)
    private fun areAHIResourcesAvailable(result: MethodChannel.Result) {
        MultiScan.waitForResult(MultiScan.shared().areResourcesDownloaded()) {
            if (!it) {
                result.success("AHI INFO: Resources are not downloaded")
                GlobalScope.launch {
                    delay(30000)
                    checkAHIResourcesDownloadSize(result)
                    areAHIResourcesAvailable(result)
                }
            } else {
                result.success("AHI: Resources ready")
            }
        }
    }

    /**
     *  Download scan resources.
     *  We recommend only calling this function once per session to prevent duplicate background resource calls.
     */
    // todo this looks good, leave
    private fun downloadAHIResources(result: MethodChannel.Result) {
        MultiScan.waitForResult(MultiScan.shared().downloadResourcesInBackground()) {
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
                    result.success(it.resultCode.toString())
//                    Log.d(TAG, "setupMultiScanSDK: ")
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

    private fun startFaceScan(avatarValues: Any, result: MethodChannel.Result) {
        // All required face scan options.
        // todo the below is handle by flutter
        avatarValues as HashMap<String, Any>
//        avatarValues["TAG_ARG_GENDER"] = "M"
//        avatarValues["TAG_ARG_SMOKER"] = "F"
//        avatarValues["TAG_ARG_DIABETIC"] = "none"
//        avatarValues["TAG_ARG_HYPERTENSION"] = "F"
//        avatarValues["TAG_ARG_BPMEDS"] = "F"
//        avatarValues["TAG_ARG_HEIGHT_IN_CM"] = 180
//        avatarValues["TAG_ARG_WEIGHT_IN_KG"] = 85
//        avatarValues["TAG_ARG_AGE"] = 35
//        avatarValues["TAG_ARG_PREFERRED_HEIGHT_UNITS"] = "CENTIMETRES"
//        avatarValues["TAG_ARG_PREFERRED_WEIGHT_UNITS"] = "KILOGRAMS"
//        if (!areFaceScanConfigOptionsValid(avatarValues)) {
//            result.error("AHI ERROR: Face Scan inputs invalid.", "there was an error", null)
//        }
        MultiScan.waitForResult(
            MultiScan.shared().initiateScan(MSScanType.FACE, MSPaymentType.PAYG, avatarValues)
        ) {
            /** Result check */
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    result.success("AHI: SCAN RESULT: ${it.result}")
                }
                SdkResultCode.ERROR -> {
                    result.error(
                        it.resultCode.toString(), "AHI: ERROR WITH FACE SCAN: ${it.message}", null
                    )
                }
            }
        }
    }

    private fun startBodyScan(result: MethodChannel.Result) {
        MultiScan.shared().registerDelegate(AHIPersistenceDelegate)
        val avatarValues: HashMap<String, Any> = HashMap()
        avatarValues["TAG_ARG_GENDER"] = "M"
        avatarValues["TAG_ARG_HEIGHT_IN_CM"] = 180
        avatarValues["TAG_ARG_WEIGHT_IN_KG"] = 85
        if (!areBodyScanConfigOptionsValid(avatarValues)) {
            result.success("AHI ERROR: Body Scan inputs invalid.")
            return
        }
        MultiScan.waitForResult(
            MultiScan.shared().initiateScan(MSScanType.BODY, MSPaymentType.PAYG, avatarValues)
        ) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    val res = JSONObject(it.result)
                    val id = res["id"].toString()
                    if (areBodyScanSmoothingResultsValid(it.resultMap)) {
                        getBodyScanExtras(id, result)
                    }
                    result.success("AHI: SCAN RESULT: ${it.result}\nAHI: Mesh URL: ${context.filesDir.path}/$id.obj")
                }
                SdkResultCode.ERROR -> {
                    result.error(
                        it.resultCode.toString(),
                        "AHI: ERROR WITH BODY SCAN: ${it.message}", null
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
        MultiScan.waitForResult(
            MultiScan.shared().getScanExtra(MSScanType.BODY, parameters)
        ) {
            /** Write the mesh to a directory */
            val objFile = File(context.filesDir, "$id.obj")
            /** Print the 3D mesh path */
            saveAvatarToFile(it, objFile)
        }
    }

    /** For the newest AHIMultiScan version 21.1.3 need to implement PersistenceDelegate */
    // todo stay here
    object AHIPersistenceDelegate : MultiScanDelegate {
        override fun request(
            scanType: MSScanType?,
            options: MutableMap<String, String>?
        ): CompletableFuture<SdkResultParcelable> {
            val future = CompletableFuture<SdkResultParcelable>()
            if (scanType == MSScanType.BODY) {
                val rawResultList = mutableListOf<String>()
                options?.forEach {
                    rawResultList.add(it.toString())
                }
                val jsonArrayString = "[" + rawResultList.joinToString(separator = ",") + "]"
                future.complete(SdkResultParcelable(SdkResultCode.SUCCESS, jsonArrayString))
            } else {
                future.complete(SdkResultParcelable(SdkResultCode.ERROR, ""))
            }
            return future
        }
    }

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

    /** Save 3D avatar mesh result on local device. */
    // todo stay
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