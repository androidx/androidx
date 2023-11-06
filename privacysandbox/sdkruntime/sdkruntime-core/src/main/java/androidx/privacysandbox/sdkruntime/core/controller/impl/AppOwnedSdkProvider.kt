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
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.core.os.BuildCompat
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
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
     * Implementation for AdServices V8.
     */
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    private class ApiAdServicesV8Impl(
        private val controller: SdkSandboxController
    ) : ProviderImpl {
        override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
            val apiResult = controller.getAppOwnedSdkSandboxInterfaces()
            return apiResult.map { AppOwnedSdkSandboxInterfaceCompat(it) }
        }
    }

    companion object {
        @SuppressLint("NewApi", "ClassVerificationFailure") // For supporting DP Builds
        fun create(controller: SdkSandboxController): AppOwnedSdkProvider {
            return if (BuildCompat.AD_SERVICES_EXTENSION_INT >= 8 ||
                AdServicesInfo.isDeveloperPreview()
            ) {
                AppOwnedSdkProvider(ApiAdServicesV8Impl(controller))
            } else {
                AppOwnedSdkProvider(NoOpImpl())
            }
        }
    }
}
