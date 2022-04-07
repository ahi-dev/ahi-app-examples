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
package com.example.ahi_kotlin_multiscan_boilerplate.utils

import com.myfiziq.sdk.MultiScanDelegate
import com.myfiziq.sdk.enums.MSScanType
import com.myfiziq.sdk.enums.SdkResultCode
import com.myfiziq.sdk.vo.SdkResultParcelable
import java.util.concurrent.CompletableFuture

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