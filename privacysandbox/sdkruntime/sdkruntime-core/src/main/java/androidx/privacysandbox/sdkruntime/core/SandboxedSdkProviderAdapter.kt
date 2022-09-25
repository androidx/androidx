/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.privacysandbox.sdkruntime.core

import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SandboxedSdkProvider
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting

/**
 * Implementation of platform [SandboxedSdkProvider] that delegate to [SandboxedSdkProviderCompat]
 * Gets compat class name from property "android.sdksandbox.PROPERTY_COMPAT_SDK_PROVIDER_CLASS_NAME"
 *
 */
// TODO(b/249981547) Update check when prebuilt with SdkSandbox APIs dropped to T
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class SandboxedSdkProviderAdapter : SandboxedSdkProvider() {

    @get:VisibleForTesting
    @Volatile
    var delegate: SandboxedSdkProviderCompat? = null
        private set

    @Throws(LoadSdkException::class)
    override fun onLoadSdk(params: Bundle): SandboxedSdk {
        return try {
            ensureDelegateLoaded().onLoadSdk(params).toSandboxedSdk()
        } catch (e: LoadSdkCompatException) {
            throw e.toLoadSdkException()
        }
    }

    override fun beforeUnloadSdk() {
        ensureDelegateLoaded().beforeUnloadSdk()
    }

    override fun getView(
        windowContext: Context,
        params: Bundle,
        width: Int,
        height: Int
    ): View {
        return ensureDelegateLoaded().getView(windowContext, params, width, height)
    }

    private fun ensureDelegateLoaded(): SandboxedSdkProviderCompat {
        val existing1 = delegate
        if (existing1 != null) {
            return existing1
        }
        synchronized(this) {
            val existing2 = delegate
            if (existing2 != null) {
                return existing2
            }
            val currentContext = context!!
            return try {
                val jetPackSdkProviderClassName = getCompatProviderClassName(currentContext)
                val clz = Class.forName(jetPackSdkProviderClassName)
                val newDelegate = clz.getConstructor().newInstance() as SandboxedSdkProviderCompat
                newDelegate.attachContext(currentContext)
                delegate = newDelegate
                newDelegate
            } catch (ex: Exception) {
                throw RuntimeException("Failed to instantiate SandboxedSdkProviderCompat", ex)
            }
        }
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun getCompatProviderClassName(context: Context): String {
        return context.packageManager.getProperty(
            COMPAT_SDK_PROVIDER_CLASS_NAME_ATTRIBUTE_NAME,
            context.packageName
        ).string!!
    }

    companion object {
        private const val COMPAT_SDK_PROVIDER_CLASS_NAME_ATTRIBUTE_NAME =
            "android.sdksandbox.PROPERTY_COMPAT_SDK_PROVIDER_CLASS_NAME"
    }
}