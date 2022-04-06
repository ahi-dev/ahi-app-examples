package com.example.ahi_kotlin_multiscan_boilerplate.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import com.example.ahi_kotlin_multiscan_boilerplate.TAG
import com.example.ahi_kotlin_multiscan_boilerplate.utils.AHIConfigTokens
import com.myfiziq.sdk.MultiScan
import com.myfiziq.sdk.enums.MSPaymentType
import com.myfiziq.sdk.enums.MSScanType

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



















