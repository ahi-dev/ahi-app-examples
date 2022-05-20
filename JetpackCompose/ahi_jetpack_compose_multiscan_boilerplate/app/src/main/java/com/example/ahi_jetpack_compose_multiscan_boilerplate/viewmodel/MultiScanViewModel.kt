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

package com.example.ahi_jetpack_compose_multiscan_boilerplate.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MultiScanViewModel : ViewModel() {
    /** Control view visibility state */
    val isSetupState = mutableStateOf(false)

    /** Check download finished state */
    val isFinishedDownloadingResourcesState = mutableStateOf(false)

    /** Control button enable state */
    val buttonEnabled = mutableStateOf(true)
}