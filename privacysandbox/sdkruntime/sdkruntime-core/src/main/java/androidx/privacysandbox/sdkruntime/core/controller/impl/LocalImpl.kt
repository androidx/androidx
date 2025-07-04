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

import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerBackend
import androidx.privacysandbox.sdkruntime.core.internal.ClientFeature
import java.util.concurrent.Executor

/**
 * Wrapper for client provided implementation of [SdkSandboxControllerBackend]. Checks client
 * version to determine if method supported.
 */
// TODO(b/426122358) Make it internal after finishing migration
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LocalImpl(
    private val implFromClient: SdkSandboxControllerBackend,
    private val clientVersion: Int,
) : SdkSandboxControllerBackend {

    override fun loadSdk(
        sdkName: String,
        params: Bundle,
        executor: Executor,
        callback: LoadSdkCallback,
    ) {
        implFromClient.loadSdk(sdkName, params, executor, callback)
    }

    override fun getSandboxedSdks(): List<SandboxedSdkCompat> {
        return implFromClient.getSandboxedSdks()
    }

    override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
        return implFromClient.getAppOwnedSdkSandboxInterfaces()
    }

    override fun registerSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ): IBinder {
        return implFromClient.registerSdkSandboxActivityHandler(handlerCompat)
    }

    override fun unregisterSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ) {
        implFromClient.unregisterSdkSandboxActivityHandler(handlerCompat)
    }

    override fun getClientPackageName(): String {
        return implFromClient.getClientPackageName()
    }

    override fun registerSdkSandboxClientImportanceListener(
        executor: Executor,
        listenerCompat: SdkSandboxClientImportanceListenerCompat,
    ) {
        if (ClientFeature.CLIENT_IMPORTANCE_LISTENER.isAvailable(clientVersion)) {
            implFromClient.registerSdkSandboxClientImportanceListener(executor, listenerCompat)
        }
    }

    override fun unregisterSdkSandboxClientImportanceListener(
        listenerCompat: SdkSandboxClientImportanceListenerCompat
    ) {
        if (ClientFeature.CLIENT_IMPORTANCE_LISTENER.isAvailable(clientVersion)) {
            implFromClient.unregisterSdkSandboxClientImportanceListener(listenerCompat)
        }
    }
}
