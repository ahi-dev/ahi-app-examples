package com.example.ahi_kotlin_multiscan_boilerplate.utils

import com.myfiziq.sdk.MultiScanDelegate
import com.myfiziq.sdk.enums.MSScanType
import com.myfiziq.sdk.enums.SdkResultCode
import com.myfiziq.sdk.vo.SdkResultParcelable
import java.util.concurrent.CompletableFuture

object AHIPersistenceDelegate : MultiScanDelegate {
    override fun request(scanType: MSScanType?, options: MutableMap<String, String>?): CompletableFuture<SdkResultParcelable> {
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