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

package com.example.ahi_jetpack_compose_multiscan_boilerplate

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.core.content.ContextCompat
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
import com.example.ahi_jetpack_compose_multiscan_boilerplate.viewmodel.MultiScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MainActivityAHI"
const val PERMISSION_REQUEST_CODE = 111

/** The required tokens for the MultiScan Setup and Authorization. */
/** Your AHI MultiScan token */
const val AHI_MULTI_SCAN_TOKEN = ""

/** Your user id. Hardcode a valid user id for testing purposes. */
const val AHI_TEST_USER_ID = "EXAMPLE_USER_ID"

/** Your salt token. */
const val AHI_TEST_USER_SALT = "EXAMPLE_APP_SALT"

/** Any claims you require passed to the SDK. */
val AHI_TEST_USER_CLAIMS = arrayOf("EXAMPLE_CLAIM")

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MultiScanViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inset = 16.dp
        val buttonHeight = 55.dp
        checkPermission()
        @Composable
        fun defaultButton(buttonText: String, isEnabled: Boolean?, action: () -> Unit) {
            Button(
                colors = buttonColors(backgroundColor = Color.Black),
                modifier = Modifier.height(buttonHeight),
                enabled = isEnabled ?: true,
                onClick = { action() }) { Text(text = buttonText.uppercase(), color = Color.White) }
        }
        setContent {
            viewModel = MultiScanViewModel()
            /** Set up view's layout constrains */
            fun updateConstraints(): ConstraintSet {
                return ConstraintSet {
                    val setupButton = createRefFor("setupButton")
                    val startFaceScanButton = createRefFor("startFaceScanButton")
                    val startFingerScanButton = createRefFor("startFingerScanButton")
                    val downloadResourcesButton = createRefFor("downloadResourcesButton")
                    val startBodyScanButton = createRefFor("startBodyScanButton")
                    constrain(setupButton) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    }
                    constrain(startFaceScanButton) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    }
                    constrain(startFingerScanButton) {
                        start.linkTo(parent.start)
                        top.linkTo(startFaceScanButton.bottom, margin = inset)
                        end.linkTo(parent.end)
                    }
                    constrain(downloadResourcesButton) {
                        start.linkTo(parent.start)
                        top.linkTo(startFingerScanButton.bottom, margin = inset)
                        end.linkTo(parent.end)
                    }
                    constrain(startBodyScanButton) {
                        start.linkTo(parent.start)
                        top.linkTo(startFingerScanButton.bottom, margin = inset)
                        end.linkTo(parent.end)
                    }
                }
            }
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inset),
                    constraintSet = updateConstraints()
                ) {
                    AnimatedVisibility(
                        modifier = Modifier
                            .layoutId("setupButton")
                            .fillMaxWidth()
                            .height(buttonHeight), visible = !viewModel.isSetupState.value
                    ) {
                        defaultButton("Setup SDK", true, { didTapSetup() })
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .layoutId("startFaceScanButton")
                            .fillMaxWidth()
                            .height(buttonHeight), visible = viewModel.isSetupState.value
                    ) {
                        defaultButton("Start Facescan", true) { didTapStartFaceScan() }
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .layoutId("startFingerScanButton")
                            .fillMaxWidth()
                            .height(buttonHeight), visible = viewModel.isSetupState.value
                    ) {
                        defaultButton("Start FingerScan", true) { didTapStartFingerScan() }
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .layoutId("downloadResourcesButton")
                            .fillMaxWidth()
                            .height(buttonHeight),
                        visible = viewModel.isSetupState.value && !viewModel.isFinishedDownloadingResourcesState.value
                    ) {
                        defaultButton("Download Resources", viewModel.buttonEnabled.value) {
                            viewModel.buttonEnabled.value = false
                            didTapDownloadResources()
                        }
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .layoutId("startBodyScanButton")
                            .fillMaxWidth()
                            .height(buttonHeight),
                        visible = viewModel.isFinishedDownloadingResourcesState.value
                    ) {
                        defaultButton(buttonText = "Start BodyScan", isEnabled = true, action = { didTapStartBodyScan() })
                    }
                }
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

    private fun didTapCheckDownloadSize() {
        checkAHIResourcesDownloadSize()
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
                authorizeUser()
                // Set results persistence delegate
                AHIMultiScan.delegatePersistence = AHIPersistenceDelegate
            }, {
                Log.d(TAG, "AHI: Error setting up: $}\n")
                Log.d(TAG, "AHI: Confirm you have a valid token.\n")
            })
        })
    }

    /**
     *  Once successfully setup, you should authorize your user with our service.
     *  With your signed in user, you can authorize them to use the AHI service,  provided that they have agreed to a payment method.
     */
    private fun authorizeUser() {
        AHIMultiScan.userAuthorize(AHI_TEST_USER_ID, AHI_TEST_USER_SALT, AHI_TEST_USER_CLAIMS, completionBlock = {
            it.fold({
                Log.d(TAG, "AHI: Setup user successfully\n")
                viewModel.isSetupState.value = true
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
                    viewModel.isFinishedDownloadingResourcesState.value = true
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
                        viewModel.isFinishedDownloadingResourcesState.value = true
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
                    viewModel.isFinishedDownloadingResourcesState.value = true
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
        avatarValues["cm_ent_height"] = 165.0
        avatarValues["kg_ent_weight"] = 67.0
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
     */
    private fun getBodyScanExtras(result: Map<String, Any>) {
        val options = mapOf("extrapolate" to listOf("mesh"))
        AHIMultiScan.getScanExtra(result, options, completionBlock = {
            it.fold({
                Log.d(TAG, "AHI: GetBodyScanExtras: $it")
                val uri = (it["extrapolate"] as? List<Map<*, *>>)?.firstOrNull()?.get("mesh") as? Uri
                Log.i(TAG, "AHI: 3D Mesh: $uri")
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
                Log.d(TAG, "AHI INFO: SDK released Successfuly.")
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
