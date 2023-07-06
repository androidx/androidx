/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.privacysandbox.sdkruntime.client.controller.impl

import android.annotation.SuppressLint
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import androidx.annotation.RequiresApi
import androidx.privacysandbox.sdkruntime.client.controller.AppOwnedSdkRegistry
import androidx.privacysandbox.sdkruntime.core.AppOwnedInterfaceConverter
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat

/**
 * Implementation for Developer Preview builds backed by [SdkSandboxManager].
 * Using reflection to call public methods / constructors.
 * TODO(b/281397807) Replace reflection for method calls when new prebuilt will be available.
 */
@RequiresApi(33) // will be available later via mainline update
internal class PlatformAppOwnedSdkRegistry(
    context: Context
) : AppOwnedSdkRegistry {

    private val sdkSandboxManager = context.getSystemService(
        SdkSandboxManager::class.java
    )

    private val appOwnedSdkInterfaceClass = Class.forName(
        "android.app.sdksandbox.AppOwnedSdkSandboxInterface"
    )

    private val registerAppOwnedSdkMethod = sdkSandboxManager.javaClass.getMethod(
        "registerAppOwnedSdkSandboxInterface",
        /* parameter1 */ appOwnedSdkInterfaceClass
    )

    private val unregisterAppOwnedSdkMethod = sdkSandboxManager.javaClass.getMethod(
        "unregisterAppOwnedSdkSandboxInterface",
        /* parameter1 */ String::class.java
    )

    private val getAppOwnedSdksMethod = sdkSandboxManager.javaClass.getMethod(
        "getAppOwnedSdkSandboxInterfaces"
    )

    private val converter: AppOwnedInterfaceConverter = AppOwnedInterfaceConverter()

    @SuppressLint("BanUncheckedReflection") // calling public non restricted methods
    override fun registerAppOwnedSdkSandboxInterface(
        appOwnedSdk: AppOwnedSdkSandboxInterfaceCompat
    ) {
        val platformObj = converter.toPlatform(appOwnedSdk)
        registerAppOwnedSdkMethod.invoke(
            sdkSandboxManager,
            /* parameter1 */ platformObj
        )
    }

    @SuppressLint("BanUncheckedReflection") // calling public non restricted methods
    override fun unregisterAppOwnedSdkSandboxInterface(sdkName: String) {
        unregisterAppOwnedSdkMethod.invoke(
            sdkSandboxManager,
            /* parameter1 */ sdkName
        )
    }

    @SuppressLint("BanUncheckedReflection") // calling public non restricted methods
    override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
        val apiResult = getAppOwnedSdksMethod.invoke(sdkSandboxManager) as List<*>
        return apiResult.map { converter.toCompat(it!!) }
    }
}
