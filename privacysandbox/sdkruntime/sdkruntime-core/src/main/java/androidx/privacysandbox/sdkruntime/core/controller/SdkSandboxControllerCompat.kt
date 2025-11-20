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
@file:Suppress("DEPRECATION")

package androidx.privacysandbox.sdkruntime.core.controller

import android.os.Bundle
import android.os.IBinder
import androidx.annotation.Keep
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import java.util.concurrent.Executor

/**
 * Legacy version of SdkSandboxControllerCompat. Shouldn't be used by Apps / SDKs.
 *
 * Allows previous version of sdkruntime-client to load SDK locally using [injectLocalImpl].
 */
@RestrictTo(LIBRARY_GROUP)
public class SdkSandboxControllerCompat {

    public interface SandboxControllerImpl {

        public fun loadSdk(
            sdkName: String,
            params: Bundle,
            executor: Executor,
            callback: LoadSdkCallback,
        )

        public fun getSandboxedSdks(): List<SandboxedSdkCompat>

        public fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat>

        public fun registerSdkSandboxActivityHandler(
            handlerCompat: SdkSandboxActivityHandlerCompat
        ): IBinder

        public fun unregisterSdkSandboxActivityHandler(
            handlerCompat: SdkSandboxActivityHandlerCompat
        )

        public fun getClientPackageName(): String

        public fun registerSdkSandboxClientImportanceListener(
            executor: Executor,
            listenerCompat: SdkSandboxClientImportanceListenerCompat,
        )

        public fun unregisterSdkSandboxClientImportanceListener(
            listenerCompat: SdkSandboxClientImportanceListenerCompat
        )
    }

    public companion object {
        /**
         * Inject implementation from client library. Implementation will be used only if loaded
         * locally. This method will be called from client side via reflection during loading SDK.
         * New library versions should use [SdkSandboxControllerBackendHolder.injectLocalBackend].
         */
        @JvmStatic
        @Keep
        @RestrictTo(LIBRARY_GROUP)
        public fun injectLocalImpl(impl: SandboxControllerImpl) {
            SdkSandboxControllerBackendHolder.injectLocalBackend(LegacyBackend(impl))
        }

        /**
         * When SDK loaded by old version of client library, converts legacy SandboxControllerImpl
         * to [SdkSandboxControllerBackend].
         */
        private class LegacyBackend(private val legacyImpl: SandboxControllerImpl) :
            SdkSandboxControllerBackend {

            override fun loadSdk(
                sdkName: String,
                params: Bundle,
                executor: Executor,
                callback: LoadSdkCallback,
            ): Unit = legacyImpl.loadSdk(sdkName, params, executor, callback)

            override fun getSandboxedSdks(): List<SandboxedSdkCompat> =
                legacyImpl.getSandboxedSdks()

            override fun getAppOwnedSdkSandboxInterfaces():
                List<AppOwnedSdkSandboxInterfaceCompat> =
                legacyImpl.getAppOwnedSdkSandboxInterfaces()

            override fun registerSdkSandboxActivityHandler(
                handlerCompat: SdkSandboxActivityHandlerCompat
            ): IBinder = legacyImpl.registerSdkSandboxActivityHandler(handlerCompat)

            override fun unregisterSdkSandboxActivityHandler(
                handlerCompat: SdkSandboxActivityHandlerCompat
            ): Unit = legacyImpl.unregisterSdkSandboxActivityHandler(handlerCompat)

            override fun getClientPackageName(): String = legacyImpl.getClientPackageName()

            override fun registerSdkSandboxClientImportanceListener(
                executor: Executor,
                listenerCompat: SdkSandboxClientImportanceListenerCompat,
            ): Unit =
                legacyImpl.registerSdkSandboxClientImportanceListener(executor, listenerCompat)

            override fun unregisterSdkSandboxClientImportanceListener(
                listenerCompat: SdkSandboxClientImportanceListenerCompat
            ): Unit = legacyImpl.unregisterSdkSandboxClientImportanceListener(listenerCompat)
        }
    }
}
