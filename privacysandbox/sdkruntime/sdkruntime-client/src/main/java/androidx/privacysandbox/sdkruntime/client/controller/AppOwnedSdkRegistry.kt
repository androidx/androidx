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

package androidx.privacysandbox.sdkruntime.client.controller

import android.annotation.SuppressLint
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.AppOwnedInterfaceConverter
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat

/**
 * Register/Unregister/Fetches [AppOwnedSdkSandboxInterfaceCompat] from [SdkSandboxManager].
 */
internal class AppOwnedSdkRegistry private constructor(
    private val registryImpl: RegistryImpl
) {

    fun registerAppOwnedSdkSandboxInterface(appOwnedSdk: AppOwnedSdkSandboxInterfaceCompat) {
        registryImpl.registerAppOwnedSdkSandboxInterface(appOwnedSdk)
    }

    fun unregisterAppOwnedSdkSandboxInterface(sdkName: String) {
        registryImpl.unregisterAppOwnedSdkSandboxInterface(sdkName)
    }

    fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
        registryImpl.getAppOwnedSdkSandboxInterfaces()

    private interface RegistryImpl {
        fun registerAppOwnedSdkSandboxInterface(appOwnedSdk: AppOwnedSdkSandboxInterfaceCompat)

        fun unregisterAppOwnedSdkSandboxInterface(sdkName: String)

        fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat>
    }

    /**
     * Implementation for cases when API not supported by [SdkSandboxManager]
     * Throwing [UnsupportedOperationException] for any method calls.
     */
    private class FailImpl : RegistryImpl {
        override fun registerAppOwnedSdkSandboxInterface(
            appOwnedSdk: AppOwnedSdkSandboxInterfaceCompat
        ) {
            throw UnsupportedOperationException("Requires Developer Preview 8 Build")
        }

        override fun unregisterAppOwnedSdkSandboxInterface(sdkName: String) {
            throw UnsupportedOperationException("Requires Developer Preview 8 Build")
        }

        override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
            throw UnsupportedOperationException("Requires Developer Preview 8 Build")
        }
    }

    /**
     * Implementation for Developer Preview builds.
     * Using reflection to call public methods / constructors.
     * TODO(b/281397807) Replace reflection for method calls when new prebuilt will be available.
     */
    private class DeveloperPreviewImpl(
        private val sdkSandboxManager: SdkSandboxManager
    ) : RegistryImpl {

        private val appOwnedInterfaceClass = Class.forName(
            "android.app.sdksandbox.AppOwnedSdkSandboxInterface"
        )

        private val registerAppOwnedSdkMethod = sdkSandboxManager.javaClass.getMethod(
            "registerAppOwnedSdkSandboxInterface",
            /* parameter1 */ appOwnedInterfaceClass
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

    companion object {
        @SuppressLint("NewApi", "ClassVerificationFailure") // UpsideDownCakePrivacySandbox is UDC
        fun create(context: Context): AppOwnedSdkRegistry {
            return if (AdServicesInfo.isDeveloperPreview()) {
                AppOwnedSdkRegistry(
                    DeveloperPreviewImpl(
                        context.getSystemService(
                            SdkSandboxManager::class.java
                        )
                    )
                )
            } else {
                AppOwnedSdkRegistry(FailImpl())
            }
        }
    }
}