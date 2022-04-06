package com.example.ahi_kotlin_multiscan_boilerplate.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MultiScanViewModel : ViewModel() {

    private val _isSetup = MutableLiveData(false)
    val isSetup: LiveData<Boolean>
        get() = _isSetup

    private val _isFinishedDownloadingResources = MutableLiveData(false)
    val isFinishedDownloadingResources: LiveData<Boolean>
        get() = _isFinishedDownloadingResources

    fun setIsFinishedDownloadingResources(isFinishedDownloadingResourcesState: Boolean) {
        _isFinishedDownloadingResources.value = isFinishedDownloadingResourcesState
    }

    fun setIsSetup(isSetupState: Boolean) {
        _isSetup.value = isSetupState
    }

}



















