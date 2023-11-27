package com.ahi_react_native_multiscan_boilerplate

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader

class MainApplication : Application(), ReactApplication {
    private val ahiReactNativeHost = object : DefaultReactNativeHost(this) {
        override fun getPackages(): MutableList<ReactPackage> {
            val packages = PackageList(this).packages
            packages.add(AHIMultiScanPackage())
            return packages
        }

        override fun getUseDeveloperSupport() = BuildConfig.DEBUG

        override fun getJSMainModuleName() = "index"
    }

    override fun getReactNativeHost() = ahiReactNativeHost

    override fun onCreate() {
        super.onCreate()
        SoLoader.init(this, false)
        if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED)
            DefaultNewArchitectureEntryPoint.load()
        ReactNativeFlipper.initializeFlipper(this, reactNativeHost.reactInstanceManager)
    }
}