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

package androidx.privacysandbox.sdkruntime.core.controller.impl

import android.annotation.SuppressLint
import android.app.sdksandbox.sdkprovider.SdkSandboxController
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.AppOwnedInterfaceConverter
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat

/**
 * Fetches all registered [AppOwnedSdkSandboxInterfaceCompat] from [SdkSandboxController].
 */
internal class AppOwnedSdkProvider private constructor(
    private val providerImpl: ProviderImpl
) {

    fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
        providerImpl.getAppOwnedSdkSandboxInterfaces()

    private interface ProviderImpl {
        fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat>
    }

    /**
     * Implementation for cases when API not supported by [SdkSandboxController]
     */
    private class NoOpImpl : ProviderImpl {
        override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
            return emptyList()
        }
    }

    /**
     * Implementation for Developer Preview builds.
     * Using reflection to call public methods / constructors.
     * TODO(b/281397807) Replace reflection for method calls when new prebuilt will be available.
     */
    private class DeveloperPreviewImpl(
        private val controller: SdkSandboxController
    ) : ProviderImpl {

        private val getAppOwnedSdkSandboxInterfacesMethod = controller.javaClass.getMethod(
            "getAppOwnedSdkSandboxInterfaces",
        )

        private val converter: AppOwnedInterfaceConverter = AppOwnedInterfaceConverter()

        @SuppressLint("BanUncheckedReflection") // calling public non restricted methods
        override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
            val apiResult = getAppOwnedSdkSandboxInterfacesMethod.invoke(controller) as List<*>
            return apiResult.map { converter.toCompat(it!!) }
        }
    }

    companion object {
        fun create(controller: SdkSandboxController): AppOwnedSdkProvider {
            return if (AdServicesInfo.isDeveloperPreview()) {
                AppOwnedSdkProvider(DeveloperPreviewImpl(controller))
            } else {
                AppOwnedSdkProvider(NoOpImpl())
            }
        }
    }
}
