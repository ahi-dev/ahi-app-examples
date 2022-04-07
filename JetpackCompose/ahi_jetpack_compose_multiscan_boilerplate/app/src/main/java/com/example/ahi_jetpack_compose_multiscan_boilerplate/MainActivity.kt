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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet

private const val TAG = "MainActivityAHI"

// The required tokens for the MultiScan Setup and Authorization.
object AHIConfigTokens {
    // Your AHI MultiScan DEV token
    const val AHI_MULTI_SCAN_TOKEN = ""

    // Your user id. Hardcode a valid user id for testing purposes.
    const val AHI_TEST_USER_ID = "AHI_TEST_USER"

    // Your salt token.
    const val AHI_TEST_USER_SALT = "user"

    // Any claims you require passed to the SDK.
    val AHI_TEST_USER_CLAIMS = arrayOf("test")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultiScanView()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MultiScanView() {

    fun updateConstrains(): ConstraintSet {
        return ConstraintSet {
            val setupButton = createRefFor("setupButton")
            val startFaceScanButton = createRefFor("startFaceScanButton")
            val downloadResourcesButton = createRefFor("downloadResourcesButton")
            val startBodyScanButton = createRefFor("startBodyScanButton")
            constrain(setupButton){
                //TODO: button constraints
            }
        }


    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
            ,
            constraintSet = updateConstrains()
        ) {
            Button(modifier = Modifier
                .layoutId("setupButton")
                .fillMaxWidth()
                .height(60.dp),
                onClick = { /*TODO*/ }
            ) {
                Text(text = "Setup SDK")
            }
            Button(
                modifier = Modifier
                    .layoutId("startFaceScanButton")
                    .fillMaxWidth()
                    .height(60.dp),
                onClick = { /*TODO*/ }) {
                Text(text = "Start Facescan")
            }
            Button(
                modifier = Modifier
                    .layoutId("downloadResourcesButton")
                    .fillMaxWidth()
                    .height(60.dp),
                onClick = { /*TODO*/ }) {
                Text(text = "Download Resources")
            }
            Button(
                modifier = Modifier
                    .layoutId("startBodyScanButton")
                    .fillMaxWidth()
                    .height(60.dp),
                onClick = { /*TODO*/ }) {
                Text(text = "Start bodyscan")
            }
        }
    }
}



