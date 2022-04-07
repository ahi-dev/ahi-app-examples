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

package com.example.ahi_kotlin_multiscan_boilerplate.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// MultiScanViewModel is responsible for the realtime view change when data change have been notified on time.
// Android MVVM architecture standard LiveData design to prevent the memory leak.
// LiveData only have getter method, it will update value for subscribed views by MutableLiveData provider newest data.
// MutableLiveData have getter and setter methods, it is response for inside the view model to update the LiveData value.
class MultiScanViewModel : ViewModel() {
    // Observe for SDK setup state.
    private val _isSetup = MutableLiveData(false)
    val isSetup: LiveData<Boolean>
        get() = _isSetup

    // Observe for SDK download resources state.
    private val _isFinishedDownloadingResources = MutableLiveData(false)
    val isFinishedDownloadingResources: LiveData<Boolean>
        get() = _isFinishedDownloadingResources

    // Set download resources state from view side.
    fun setIsFinishedDownloadingResources(isFinishedDownloadingResourcesState: Boolean) {
        _isFinishedDownloadingResources.value = isFinishedDownloadingResourcesState
    }

    // Set setup SDK state from view side
    fun setIsSetup(isSetupState: Boolean) {
        _isSetup.value = isSetupState
    }
}