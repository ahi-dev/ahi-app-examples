package com.example.ahi_flutter_multiscan_boilerplate

import android.os.Build
import androidx.annotation.RequiresApi
import com.myfiziq.sdk.MultiScan
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {

    private val CHANNEL = "flutter_boilerplate_wrapper"

    @RequiresApi(Build.VERSION_CODES.N)
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler{call, result ->
           if (call.method == "setupMultiScanSDK") {
               result.success(setupMultiScanSDK(result, call.arguments))
           } else {
               result.error("Unavailable", "this is important secret you can't have it", null)
           }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupMultiScanSDK(result: MethodChannel.Result, arguments: Any) {
        val token: String = (arguments as? String) ?: ""
        /* The token must be passed into the setup method in the form of a map with the single key "TOKEN"
         which should have the value of the token itself. */
        val config = mapOf("TOKEN" to token)
        try {
            MultiScan.waitForResult(MultiScan.shared().setup(config)) { response ->
                if (response.resultCode.isOk) {
                    result.success(response.resultCode.name)
                    return@waitForResult
                }
                result.error(response.resultCode.name, response.message, null)
            }
        } catch (e: Exception) {
            print(e)
        }
    }
}
