/*
 * Copyright 2024 The Android Open Source Project
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

import android.app.sdksandbox.sdkprovider.SdkSandboxClientImportanceListener
import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.core.os.BuildCompat
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import java.util.concurrent.Executor

/** Register [SdkSandboxClientImportanceListenerCompat] in [SdkSandboxController]. */
internal class PlatformClientImportanceListenerRegistry
private constructor(private val registryImpl: RegistryImpl) {

    fun registerSdkSandboxClientImportanceListener(
        executor: Executor,
        listenerCompat: SdkSandboxClientImportanceListenerCompat,
    ) = registryImpl.registerSdkSandboxClientImportanceListener(executor, listenerCompat)

    fun unregisterSdkSandboxClientImportanceListener(
        listenerCompat: SdkSandboxClientImportanceListenerCompat
    ) = registryImpl.unregisterSdkSandboxClientImportanceListener(listenerCompat)

    private interface RegistryImpl {
        fun registerSdkSandboxClientImportanceListener(
            executor: Executor,
            listenerCompat: SdkSandboxClientImportanceListenerCompat,
        )

        fun unregisterSdkSandboxClientImportanceListener(
            listenerCompat: SdkSandboxClientImportanceListenerCompat
        )
    }

    /** Implementation for cases when API not supported by [SdkSandboxController] */
    private class NoOpImpl : RegistryImpl {
        override fun registerSdkSandboxClientImportanceListener(
            executor: Executor,
            listenerCompat: SdkSandboxClientImportanceListenerCompat,
        ) {
            // do nothing
        }

        override fun unregisterSdkSandboxClientImportanceListener(
            listenerCompat: SdkSandboxClientImportanceListenerCompat
        ) {
            // do nothing
        }
    }

    /** Implementation for AdServices V14. */
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
    private class ApiAdServicesV14Impl(private val controller: SdkSandboxController) :
        RegistryImpl {

        private val compatToPlatformMap =
            hashMapOf<
                SdkSandboxClientImportanceListenerCompat,
                SdkSandboxClientImportanceListener,
            >()

        @DoNotInline
        override fun registerSdkSandboxClientImportanceListener(
            executor: Executor,
            listenerCompat: SdkSandboxClientImportanceListenerCompat,
        ) {
            synchronized(compatToPlatformMap) {
                val platformListener: SdkSandboxClientImportanceListener =
                    compatToPlatformMap[listenerCompat]
                        ?: SdkSandboxClientImportanceListener { isForeground ->
                            listenerCompat.onForegroundImportanceChanged(isForeground)
                        }

                controller.registerSdkSandboxClientImportanceListener(executor, platformListener)
                compatToPlatformMap[listenerCompat] = platformListener
            }
        }

        @DoNotInline
        override fun unregisterSdkSandboxClientImportanceListener(
            listenerCompat: SdkSandboxClientImportanceListenerCompat
        ) {
            synchronized(compatToPlatformMap) {
                val platformListener: SdkSandboxClientImportanceListener =
                    compatToPlatformMap[listenerCompat] ?: return
                controller.unregisterSdkSandboxClientImportanceListener(platformListener)
                compatToPlatformMap.remove(listenerCompat)
            }
        }
    }

    companion object {
        fun create(controller: SdkSandboxController): PlatformClientImportanceListenerRegistry {
            return if (BuildCompat.AD_SERVICES_EXTENSION_INT >= 14) {
                PlatformClientImportanceListenerRegistry(ApiAdServicesV14Impl(controller))
            } else {
                PlatformClientImportanceListenerRegistry(NoOpImpl())
            }
        }
    }
}
