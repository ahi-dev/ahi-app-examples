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

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.example.ahi_kotlin_multiscan_boilerplate.databinding.ActivityMainBinding
import com.example.ahi_kotlin_multiscan_boilerplate.viewmodel.MultiScanViewModel
import kotlinx.coroutines.*

const val TAG = "MainActivityAHI"
const val PERMISSION_REQUEST_CODE = 111

/** The required tokens for the MultiScan Setup and Authorization. */
/** Your AHI MultiScan token */
const val AHI_MULTI_SCAN_TOKEN = ""

/** Your user ID. NOTE: User ID is hard-coded here for example, BUT should NOT be hard-coded in real integration (user ID from idP is expected). */
const val AHI_TEST_USER_ID = "EXAMPLE_USER_ID"

/** Security salt value. This should be hard-coded into your app, and SHOULD NOT be changed (i.e. be the same in both iOS and Android). It can be any string value. */
const val AHI_TEST_USER_SALT = "EXAMPLE_APP_SALT"

/** Claims are optional values to increase the security for the user. The order and values should be unique for a given user and be the same on both iOS and Android (e.g. user join date in the format "yyyy", "mm", "dd", "zzzz"). */
val AHI_TEST_USER_CLAIMS = arrayOf("EXAMPLE_CLAIM")

class MainActivity : AppCompatActivity(), View.OnClickListener {
    /** Instance of AHI MultiScan */
    lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MultiScanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MultiScanViewModel::class.java)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.isSetup.observe(this, Observer {
            if (it) {
                binding.setupButton.visibility = View.GONE
                binding.startFaceScanButton.visibility = View.VISIBLE
                binding.startFingerScanButton.visibility = View.VISIBLE
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
        binding.startFingerScanButton.setOnClickListener(this)
        binding.startBodyScanButton.setOnClickListener(this)
        binding.setupButton.setOnClickListener(this)

        checkPermission()
    }

    /** Handle each button action and visibility. */
    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.setupButton -> didTapSetup()
            R.id.startFaceScanButton -> didTapStartFaceScan()
            R.id.startFingerScanButton -> didTapStartFingerScan()
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

    private fun didTapStartFingerScan() {
        startFingerScan()
    }

    private fun didTapStartBodyScan() {
        startBodyScan()
    }

    private fun didTapDownloadResources() {
        getResourcesDownloadProgressReport()
    }

    /**
     *  Setup the MultiScan SDK
     *  This must happen before requesting a scan.
     *  We recommend doing this on successful load of your application.
     */
    private fun setupMultiScanSDK() {
        val config: MutableMap<String, String> = HashMap()
        config["TOKEN"] = AHI_MULTI_SCAN_TOKEN
        val scans: Array<IAHIScan> = arrayOf(FaceScan(), FingerScan(), BodyScan())
        AHIMultiScan.setup(application, config, scans, completionBlock = {
            it.fold({
                // Set results persistence delegate
                AHIMultiScan.delegatePersistence = AHIPersistenceDelegate

                authorizeUser()
            }, {
                Log.d(TAG, "AHI: Error setting up: $}\n")
                Log.d(TAG, "AHI: Confirm you have a valid token.\n")
            })
        })
    }

    /**
     *  Once successfully setup, you should authorize your user with our service.
     *  With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
     * */
    private fun authorizeUser() {
        AHIMultiScan.userAuthorize(AHI_TEST_USER_ID, AHI_TEST_USER_SALT, AHI_TEST_USER_CLAIMS, completionBlock = {
            it.fold({
                Log.d(TAG, "AHI: Setup user successfully\n")
                viewModel.setIsSetup(true)
            }, {
                Log.d(TAG, "AHI: Auth Error: ${it.message}\n")
                Log.d(TAG, "AHI: Confirm you are using a valid user id, salt and claims\n")
            })
        })
    }

    /** Check if the AHI resources are downloaded. */
    private fun areAHIResourcesAvailable() {
        AHIMultiScan.areResourcesDownloaded {
            it.fold({
                if (it) {
                    viewModel.setIsFinishedDownloadingResources(true)
                    Log.d(TAG, "AHI: Resources ready\n")
                } else {
                    Log.d(TAG, "AHI INFO: Resources are not downloaded\n")
                }
            }, {
                Log.d(TAG, "AHI: Error in resource downloading \n")
            })
        }
    }

    /**
     *  Download scan resources.
     *  We recommend only calling this function once per session to prevent duplicate background resource calls.
     */
    private fun downloadAHIResources() {
        AHIMultiScan.downloadResourcesInForeground(3)
    }

    /**
     *  Get resources download progress report.
     **/
    private fun getResourcesDownloadProgressReport() {
        GlobalScope.launch(Dispatchers.IO) {
            AHIMultiScan.areResourcesDownloaded { ahiResult ->
                ahiResult.fold({
                    if (it) {
                        viewModel.setIsFinishedDownloadingResources(true)
                        Log.d(TAG, "AHI: Resources ready\n")
                    } else {
                        Log.d(TAG, "AHI INFO: Resources are not downloaded\n")
                        AHIMultiScan.delegateDownloadProgress = object : IAHIDownloadProgress {
                            override fun downloadProgressReport(status: AHIResult<Unit>) {
                                if (status.isFailure) {
                                    Log.d(TAG, "AHI: Failed to download resources: ${status.error().toString()}")
                                } else {
                                    checkAHIResourcesDownloadSize()
                                }
                            }
                        }
                        downloadAHIResources()
                    }
                }, {
                    Log.d(TAG, "AHI INFO: Resources download failed\n")
                })
            }
        }
    }

    /** Check the size of the AHI resources that require downloading. */
    private fun checkAHIResourcesDownloadSize() {
        AHIMultiScan.totalEstimatedDownloadSizeInBytes {
            it.fold({ downloadState ->
                val mB = 1024.0 * 1024.0
                val progress = downloadState.progressBytes / mB
                val total = downloadState.totalBytes / mB
                if (progress == total) {
                    viewModel.setIsFinishedDownloadingResources(true)
                }
                Log.d(TAG, "AHI INFO: Size of download is ${progress.format(2)}MB / ${total.format(2)}MB")
            }, {
                Log.e(TAG, it.message.toString())
            })
        }
    }

    private fun startFaceScan() {
        // All required face scan options.
        val avatarValues: HashMap<String, Any> = HashMap()
        avatarValues["enum_ent_sex"] = "male"
        avatarValues["bool_ent_smoker"] = false
        avatarValues["enum_ent_diabetic"] = "none"
        avatarValues["bool_ent_hypertension"] = false
        avatarValues["bool_ent_bloodPressureMedication"] = false
        avatarValues["cm_ent_height"] = 165
        avatarValues["kg_ent_weight"] = 67
        avatarValues["yr_ent_age"] = 35
        if (!areFaceScanConfigOptionsValid(avatarValues)) {
            Log.d(TAG, "AHI ERROR: Face Scan inputs invalid.")
            return
        }

        AHIMultiScan.initiateScan("face", avatarValues, activityResultRegistry, completionBlock = {
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
                        if (result.error() == AHIFaceScanError.FACE_SCAN_CANCELED) {
                            Log.i(TAG, "User cancelled scan")
                        } else {
                            Log.d(TAG, "initiateScan: ${result.error()}")
                        }
                    }
                }
            }
        })
    }

    private fun startFingerScan() {
        // All required face scan options.
        val avatarValues: HashMap<String, Any> = HashMap()
        avatarValues["sec_ent_scanLength"] = 60
        avatarValues["str_ent_instruction1"] = "Instruction 1"
        avatarValues["str_ent_instruction2"] = "Instruction 2"

        if (!areFingerScanConfigOptionsValid(avatarValues)) {
            Log.d(TAG, "AHI ERROR: Finger Scan inputs invalid.")
            return
        }

        AHIMultiScan.initiateScan("finger", avatarValues, activityResultRegistry, completionBlock = {
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
                        if (result.error() == AHIFingerScanError.FINGER_SCAN_CANCELLED) {
                            Log.i(TAG, "User cancelled scan")
                        } else {
                            Log.d(TAG, "initiateScan: ${result.error()}")
                        }
                    }
                }
            }
        })
    }

    private fun startBodyScan() {
        val avatarValues: HashMap<String, Any> = HashMap()
        avatarValues["enum_ent_sex"] = "male"
        avatarValues["cm_ent_height"] = 180
        avatarValues["kg_ent_weight"] = 85
        if (!areBodyScanConfigOptionsValid(avatarValues)) {
            Log.d(TAG, "AHI ERROR: Body Scan inputs invalid.")
            return
        }

        AHIMultiScan.initiateScan("body", avatarValues, activityResultRegistry, completionBlock = {
            lifecycleScope.launch(Dispatchers.Main) {
                if (!it.isDone) {
                    Log.i(TAG, "Waiting of results, can show waiting screen here")
                }

                val result = withContext(Dispatchers.IO) { it.get() }
                when (result) {
                    is AHIResult.Success -> {
                        Log.d(TAG, "initiateScan: ${result.value}")
                        // persist results
                        AHIPersistenceDelegate.bodyScanResult.add(result.value)
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
    }

    /**
     *  Use this function to fetch the 3D avatar mesh.
     *  The 3D mesh can be created and returned at any time.
     *  We recommend doing this on successful completion of a body scan with the results.
     * */
    private fun getBodyScanExtras(result: Map<String, Any>) {
        val options = mapOf("extrapolate" to listOf("mesh"))
        AHIMultiScan.getScanExtra(result, options, completionBlock = {
            it.fold({
                val uri = (it["extrapolate"] as? List<Map<*, *>>)?.firstOrNull()?.get("mesh") as? Uri
                Log.i(TAG, "$uri")
            }, {
                Log.e(TAG, it.toString())
            })
        })
    }

    /**
     * Check if MultiScan is on or offline.
     */
    private fun getMultiScanStatus() {
        AHIMultiScan.getStatus {
            Log.d(TAG, "AHI INFO: Status: ${it.toString()}")
        }
    }

    /**
     * Check your AHI MultiScan organisation details.
     */
    private fun getMultiScanDetails() {
        AHIMultiScan.getDetails {
            it.fold({
                Log.d(TAG, "AHI INFO: MultiScan details: ${it}")
            }, {
                Log.d(TAG, "AHI INFO: Failed to get details")
            })
        }
    }

    /**
     * Check if the user is authorized to use the MultiScan service.
     *
     * The expected result for <= v21.1.3 is an error called "NO_OP".
     */
    private fun getUserAuthorizedState() {
        AHIMultiScan.userIsAuthorized {
            it.fold({
                Log.d(TAG, "AHI INFO: User is authorized")
            }, {
                Log.d(TAG, "AHI INFO: User is not authorized")
            })
        }
    }

    /**
     * Deauthorize the user.
     */
    private fun deauthorizeUser() {
        AHIMultiScan.userDeauthorize {
            it.fold({
                Log.d(TAG, "AHI INFO: User is deauthorized.")
            }, {
                Log.e(TAG, it.toString())
            })
        }
    }

    /**
     * Release the MultiScan SDK session.
     *
     * If you use this, you will need to call setupSDK again.
     * The expected result for <= v21.1.3 is an error called "NO_OP".
     */
    private fun releaseMultiScanSDK() {
        AHIMultiScan.releaseSdk {
            it.fold({
                Log.d(TAG, "AHI INFO: SDK is released.")
            }, {
                Log.e(TAG, it.toString())
            })
        }
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


    /**
     *  All MultiScan scan configs require this information.
     *
     *  BodyScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/BodyScan/Schemas/
     *  FaceScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FaceScan/Schemas/
     * */
    private fun areSharedScanConfigOptionsValid(avatarValues: java.util.HashMap<String, Any>): Boolean {
        val sex = avatarValues["enum_ent_sex"].takeIf { it is String }
        val height = avatarValues["cm_ent_height"].takeIf { it is Double || it is Int }
        val weight = avatarValues["kg_ent_weight"].takeIf { it is Double || it is Int }
        return if (sex != null && height != null && weight != null) {
            arrayListOf("male", "female").contains(sex)
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
        val sex = avatarValues["enum_ent_sex"].takeIf { it is String }
        val smoke = avatarValues["bool_ent_smoker"].takeIf { it is Boolean }
        val diabeticType = avatarValues["enum_ent_diabetic"].takeIf { it is String }
        val hypertension = avatarValues["bool_ent_hypertension"].takeIf { it is Boolean }
        val blood = avatarValues["bool_ent_bloodPressureMedication"].takeIf { it is Boolean }
        val height = avatarValues["cm_ent_height"].toString().toDoubleOrNull()
        val weight = avatarValues["kg_ent_weight"].toString().toDoubleOrNull()
        val age = avatarValues["yr_ent_age"].takeIf { it is Int }
        if (sex != null &&
            smoke != null &&
            diabeticType != null &&
            hypertension != null &&
            blood != null &&
            height != null &&
            weight != null &&
            age != null &&
            height in 50.0..300.0 &&
            weight in 25.0..300.0 &&
            age in 13..120
        ) {
            return arrayListOf("none", "type1", "type2").contains(diabeticType)
        } else {
            return false
        }
    }

    /**
     *  FingerScan config requirements validation.
     *  Please see the Schemas for more information:
     *  FingerScan: https://docs.advancedhumanimaging.io/MultiScan%20SDK/FingerScan/Schemas/
     * */
    private fun areFingerScanConfigOptionsValid(avatarValues: HashMap<String, Any>): Boolean {
        val scanLength = avatarValues["sec_ent_scanLength"].toString().toIntOrNull()
        val instruction1 = avatarValues["str_ent_instruction1"].takeIf { it is String }
        val instruction2 = avatarValues["str_ent_instruction2"].takeIf { it is String }
        return (scanLength != null &&
                instruction1 != null &&
                instruction2 != null &&
                scanLength >= 20)
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
        val sex = avatarValues["enum_ent_sex"].takeIf { it is String }
        val height = avatarValues["cm_ent_height"].toString().toDoubleOrNull()
        val weight = avatarValues["kg_ent_weight"].toString().toDoubleOrNull()
        return (sex != null &&
                height != null &&
                weight != null &&
                height in 50.0..255.0 &&
                weight in 16.0..300.0
                )
    }

    /**
     * Check camera permissions
     */
    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
