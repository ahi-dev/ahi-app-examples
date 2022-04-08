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

package com.example.ahi_kotlin_multiscan_boilerplate

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.ahi_kotlin_multiscan_boilerplate.databinding.ActivityMainBinding
import com.example.ahi_kotlin_multiscan_boilerplate.viewmodel.MultiScanViewModel
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

const val TAG = "MainActivityAHI"

/** The required tokens for the MultiScan Setup and Authorization. */
object AHIConfigTokens {
    /**Your AHI MultiScan DEV token */
    const val AHI_MULTI_SCAN_TOKEN = ""

    /** Your user id. Hardcode a valid user id for testing purposes. */
    const val AHI_TEST_USER_ID = "AHI_TEST_USER"

    /** Your salt token. */
    const val AHI_TEST_USER_SALT = "user"

    /** Any claims you require passed to the SDK. */
    val AHI_TEST_USER_CLAIMS = arrayOf("test")
}

class MainActivity : AppCompatActivity(), View.OnClickListener {
    /** Instance of AHI MultiScan */
    val ahi = MultiScan.shared()
    lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MultiScanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MultiScanViewModel::class.java)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ahi.registerDelegate(AHIPersistenceDelegate)
        viewModel.isSetup.observe(this, Observer {
            if (it) {
                binding.setupButton.visibility = View.GONE
                binding.startFaceScanButton.visibility = View.VISIBLE
                binding.downloadResourcesButton.visibility = View.VISIBLE
            }
        })
        viewModel.isFinishedDownloadingResources.observe(this, Observer {
            if (it) {
                binding.downloadResourcesButton.visibility = View.GONE
                binding.startBodyScanButton.visibility = View.VISIBLE
            }
        })
        binding.downloadResourcesButton.setOnClickListener(this)
        binding.startFaceScanButton.setOnClickListener(this)
        binding.startBodyScanButton.setOnClickListener(this)
        binding.setupButton.setOnClickListener(this)
    }

    /** Handle each button action and visibility. */
    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.setupButton -> didTapSetup()
            R.id.startFaceScanButton -> didTapStartFaceScan()
            R.id.startBodyScanButton -> didTapStartBodyScan()
            R.id.downloadResourcesButton -> {
                didTapDownloadResources()
                // Set button inactive
                binding.downloadResourcesButton.isEnabled = false
                binding.downloadResourcesButton.alpha = 0.5f
            }
        }
    }

    private fun didTapSetup() {
        setupMultiScanSDK()
    }

    private fun didTapStartFaceScan() {
        startFaceScan()
    }

    private fun didTapStartBodyScan() {
        startBodyScan()
        ahi.registerDelegate(AHIPersistenceDelegate)
    }

    private fun didTapDownloadResources() {
        downloadAHIResources()
        areAHIResourcesAvailable()
        checkAHIResourcesDownloadSize()
    }

    /** Check the size of the AHI resources that require downloading. */
    private fun checkAHIResourcesDownloadSize() {
        MultiScan.waitForResult(ahi.totalEstimatedDownloadSizeInBytes()) {
            Log.d(TAG, "AHI INFO: Size of download is ${it / 1024 / 1024}\n")
        }
    }

    /** Check if the AHI resources are downloaded. */
    private fun areAHIResourcesAvailable() {
        MultiScan.waitForResult(ahi.areResourcesDownloaded()) { it ->
            if (!it) {
                Log.d(TAG, "AHI INFO: Resources are not downloaded\n")
                GlobalScope.launch {
                    delay(30000)
                    checkAHIResourcesDownloadSize()
                    areAHIResourcesAvailable()
                }
            } else {
                viewModel.setIsFinishedDownloadingResources(true)
                Log.d(TAG, "AHI: Resources ready\n")
            }
        }
    }

    /**
     *  Download scan resources.
     *  We recommend only calling this function once per session to prevent duplicate background resource calls.
     */
    private fun downloadAHIResources() {
        ahi.downloadResourcesInBackground()
    }

    /**
     *  Setup the MultiScan SDK
     *  This must happen before requesting a scan.
     *  We recommend doing this on successful load of your application.
     */
    private fun setupMultiScanSDK() {
        val config: MutableMap<String, String> = HashMap()
        config["TOKEN"] = AHIConfigTokens.AHI_MULTI_SCAN_TOKEN
        MultiScan.waitForResult(ahi.setup(config)) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> authorizeUser()
                SdkResultCode.ERROR -> {
                    Log.d(TAG, "AHI: Error setting up: $}\n")
                    Log.d(TAG, "AHI: Confirm you habe a valid token.\n")
                    return@waitForResult
                }
            }
        }
    }

    /**
     *  Once successfully setup, you should authorize your user with our service.
     *  With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
     * */
    private fun authorizeUser() {
        MultiScan.waitForResult(
            ahi.userAuthorize(
                AHIConfigTokens.AHI_TEST_USER_ID,
                AHIConfigTokens.AHI_TEST_USER_SALT,
                AHIConfigTokens.AHI_TEST_USER_CLAIMS
            )
        ) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    Log.d(TAG, "AHI: Setup user successfully\n")
                    viewModel.setIsSetup(true)
                }
                SdkResultCode.ERROR -> {
                    Log.d(TAG, "AHI: Auth Error: ${it.message}\n")
                    Log.d(TAG, "AHI: Confirm you are using a valid user id, salt and claims\n")
                }
            }
        }
    }

    private fun startFaceScan() {
        // All required face scan options.
        val avatarValues: HashMap<String, Any> = HashMap()
        avatarValues["TAG_ARG_GENDER"] = "M"
        avatarValues["TAG_ARG_SMOKER"] = "F"
        avatarValues["TAG_ARG_DIABETIC"] = "none"
        avatarValues["TAG_ARG_HYPERTENSION"] = "F"
        avatarValues["TAG_ARG_BPMEDS"] = "F"
        avatarValues["TAG_ARG_HEIGHT_IN_CM"] = 180
        avatarValues["TAG_ARG_WEIGHT_IN_KG"] = 85
        avatarValues["TAG_ARG_AGE"] = 35
        avatarValues["TAG_ARG_PREFERRED_HEIGHT_UNITS"] = "CENTIMETRES"
        avatarValues["TAG_ARG_PREFERRED_WEIGHT_UNITS"] = "KILOGRAMS"
        if (!areFaceScanConfigOptionsValid(avatarValues)) {
            Log.d(TAG, "AHI ERROR: Face Scan inputs invalid.")
        }
        MultiScan.waitForResult(
            MultiScan.shared().initiateScan(MSScanType.FACE, MSPaymentType.PAYG, avatarValues)
        ) {
            /** Result check */
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> Log.d(TAG, "AHI: SCAN RESULT: ${it.result}\n")
                SdkResultCode.ERROR -> Log.d(TAG, "AHI: ERROR WITH FACE SCAN: ${it.message}\n")
            }
        }
    }

    private fun startBodyScan() {
        val avatarValues: HashMap<String, Any> = HashMap()
        avatarValues["TAG_ARG_GENDER"] = "M"
        avatarValues["TAG_ARG_HEIGHT_IN_CM"] = 180
        avatarValues["TAG_ARG_WEIGHT_IN_KG"] = 85
        if (!areBodyScanConfigOptionsValid(avatarValues)) {
            Log.d(TAG, "AHI ERROR: Body Scan inputs invalid.")
            return
        }
        MultiScan.waitForResult(
            MultiScan.shared().initiateScan(MSScanType.BODY, MSPaymentType.PAYG, avatarValues)
        ) {
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> {
                    Log.d(TAG, "AHI: SCAN RESULT: ${it.result}\n")
                    if (areBodyScanSmoothingResultsValid(it.resultMap)) {
                        val res = JSONObject(it.result)
                        val id = res["id"].toString()
                        getBodyScanExtras(id)
                    }
                }
                SdkResultCode.ERROR -> Log.d(TAG, "AHI: ERROR WITH BODY SCAN: ${it.message}\n")
            }
        }
    }

    /**
     *  Use this function to fetch the 3D avatar mesh.
     *  The 3D mesh can be created and returned at any time.
     *  We recommend doing this on successful completion of a body scan with the results.
     * */
    private fun getBodyScanExtras(id: String) {
        val parameters: MutableMap<String, Any> = HashMap()
        parameters["operation"] = MultiScanOperation.BodyGetMeshObj.name
        parameters["id"] = id
        MultiScan.waitForResult(
            MultiScan.shared().getScanExtra(MSScanType.BODY, parameters)
        ) {
            /** Write the mesh to a directory */
            val objFile = File(applicationContext.filesDir, "$id.obj")
            Log.d(TAG, "AHI: Mesh URL: ${applicationContext.filesDir.path}/$id.obj\n")
            /** Print the 3D mesh path */
            saveAvatarToFile(it, objFile)
            /** Return the URL */
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
            "ml_raw_fitness",
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