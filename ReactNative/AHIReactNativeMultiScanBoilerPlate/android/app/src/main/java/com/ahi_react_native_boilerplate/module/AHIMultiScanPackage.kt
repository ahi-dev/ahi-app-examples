//
//  AHI
//
//  Copyright (c) AHI. All rights reserved.
//

package com.ahi_react_native_boilerplate.module

import android.view.View
import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ReactShadowNode
import com.facebook.react.uimanager.ViewManager
import java.util.Collections

class AHIMultiScanPackage : ReactPackage {
    override fun createNativeModules(context: ReactApplicationContext): MutableList<NativeModule> {
        val modules = arrayListOf<NativeModule>()
        modules.add(AHIMultiScanModule(context))
        return modules
    }

    override fun createViewManagers(
        context: ReactApplicationContext,
    ): MutableList<ViewManager<View, ReactShadowNode<*>>> {
        return Collections.emptyList()
    }

}
