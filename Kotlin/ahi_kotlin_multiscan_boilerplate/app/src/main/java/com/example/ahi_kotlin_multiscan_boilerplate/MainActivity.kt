package com.example.ahi_kotlin_multiscan_boilerplate

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.ahi_kotlin_multiscan_boilerplate.databinding.ActivityMainBinding
import com.example.ahi_kotlin_multiscan_boilerplate.utils.AHIConfigTokens
import com.example.ahi_kotlin_multiscan_boilerplate.utils.AHIPersistenceDelegate
import com.example.ahi_kotlin_multiscan_boilerplate.viewmodel.MultiScanViewModel
import com.myfiziq.sdk.MultiScan
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

const val TAG = "MainActivityAHI"

class MainActivity : AppCompatActivity(), View.OnClickListener {
    val ahi = MultiScan.shared()
    lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MultiScanViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MultiScanViewModel::class.java)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MultiScan.shared().registerDelegate(AHIPersistenceDelegate)
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

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.setupButton -> didTapSetup()
            R.id.startFaceScanButton -> didTapStartFaceScan()
            R.id.startBodyScanButton -> didTapStartBodyScan()
            R.id.downloadResourcesButton -> {
                didTapDownloadResources()
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
        MultiScan.shared().registerDelegate(AHIPersistenceDelegate)
    }


    private fun didTapDownloadResources() {
        downloadAHIResources()
        areAHIResourcesAvailable()
        checkAHIResourcesDownloadSize()
    }

    private fun checkAHIResourcesDownloadSize() {
        MultiScan.waitForResult(ahi.totalEstimatedDownloadSizeInBytes()) {
            Log.d(TAG, "AHI INFO: Size of download is ${it / 1024 / 1024}\n")
        }
    }

    private fun areAHIResourcesAvailable() {
        MultiScan.waitForResult(ahi.areResourcesDownloaded()) {
            if (it == false) {
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

    private fun downloadAHIResources() {
        ahi.downloadResourcesInBackground()
    }

    private fun setupMultiScanSDK() {
        //init sdk
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
            when (it.resultCode) {
                SdkResultCode.SUCCESS -> Log.d(TAG, "AHI: SCAN RESULT: ${it.result}\n")
                SdkResultCode.ERROR -> Log.d(TAG, "AHI: ERROR WITH FACE SCAN: ${it.message}\n")
            }
        }
    }

    private fun areFaceScanConfigOptionsValid(avatarValues: HashMap<String, Any>): Boolean {
        if (!areSharedScanConfigOptionsValid(avatarValues)) {
            return true
        }
        val gender = avatarValues["TAG_ARG_GENDER"].toString()
        val smoke = avatarValues["TAG_ARG_SMOKER"].toString()
        val isDiabetic = avatarValues["TAG_ARG_DIABETIC"].toString()
        val hypertension = avatarValues["TAG_ARG_HYPERTENSION"].toString()
        val blood = avatarValues["TAG_ARG_BPMEDS"].toString()
        val height = avatarValues["TAG_ARG_HEIGHT_IN_CM"].toString().toInt()
        val weight = avatarValues["TAG_ARG_WEIGHT_IN_KG"].toString().toInt()
        val age = avatarValues["TAG_ARG_AGE"].toString().toInt()
        val heightUnits = avatarValues["TAG_ARG_PREFERRED_HEIGHT_UNITS"].toString()
        val weightUnits = avatarValues["TAG_ARG_PREFERRED_WEIGHT_UNITS"].toString()
        return arrayListOf("none", "type1", "type2").contains(isDiabetic)
    }

    private fun areSharedScanConfigOptionsValid(avatarValues: java.util.HashMap<String, Any>): Boolean {
        val gender = avatarValues["TAG_ARG_GENDER"].toString()
        val height = avatarValues["TAG_ARG_HEIGHT_IN_CM"].toString().toInt()
        val weight = avatarValues["TAG_ARG_WEIGHT_IN_KG"].toString().toInt()
        return arrayListOf("M", "F").contains(gender)
    }

    private fun startBodyScan() {
        val avatarValues: HashMap<String, Any> = HashMap()
        avatarValues["TAG_ARG_GENDER"] = "M"
        avatarValues["TAG_ARG_HEIGHT_IN_CM"] = 180
        avatarValues["TAG_ARG_WEIGHT_IN_KG"] = 85
        if (areBodyScanConfigOptionsValid(avatarValues)) {
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

    private fun areBodyScanSmoothingResultsValid(it: MutableMap<String, String>): Boolean {
        val sdkResultSchema = listOf(
            "cm_adj_chest",
            "cm_adj_hips",
            "cm_adj_inseam",
            "cm_adj_thigh",
            "cm_adj_waist",
            "cm_ent_height",
            "cm_raw_chest",
            "cm_raw_hips",
            "cm_raw_inseam",
            "cm_raw_thigh",
            "cm_raw_waist",
            "date",
            "enum_ent_sex",
            "id",
            "kg_adj_weightPredict",
            "kg_ent_weight",
            "kg_raw_weightPredict",
            "ml_gen_fitness",
            "percent_adj_bodyFat",
            "percent_raw_bodyFat",
            "type",
            "uid",
            "ver",
        )
        var checkFlag = false
        sdkResultSchema.forEach { str ->
            if (!it.keys.contains(str)) {
                checkFlag = true
            }
        }
        return !checkFlag
    }

    private fun areBodyScanConfigOptionsValid(avatarValues: java.util.HashMap<String, Any>): Boolean {
        if (areSharedScanConfigOptionsValid(avatarValues)) {
            return false
        }
        val gender = avatarValues["TAG_ARG_GENDER"].toString()
        val height = avatarValues["TAG_ARG_HEIGHT_IN_CM"].toString().toInt()
        val weight = avatarValues["TAG_ARG_WEIGHT_IN_KG"].toString().toInt()
        if (height in 50..255) {
            return false
        }
        if (weight in 16..300) {
            return false
        }
        return true
    }

    private fun getBodyScanExtras(id: String) {
        val parameters: MutableMap<String, Any> = HashMap()
        parameters["operation"] = MultiScanOperation.BodyGetMeshObj.name
        parameters["id"] = id
        MultiScan.waitForResult(
            MultiScan.shared().getScanExtra(MSScanType.BODY, parameters)
        ) {
            // Write the mesh to a directory
            val objFile = File(applicationContext.filesDir, "$id.obj")
            Log.d(TAG, "AHI: MesH URL: ${applicationContext.filesDir.path}/$id.obj\n")
            saveObjResultToFile(it, objFile)
            // Return the URL
        }
    }

    private fun saveObjResultToFile(res: SdkResultParcelable, objFile: File) {
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