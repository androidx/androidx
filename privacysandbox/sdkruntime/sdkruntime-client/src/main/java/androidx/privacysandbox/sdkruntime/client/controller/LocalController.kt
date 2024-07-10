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

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import androidx.privacysandbox.sdkruntime.client.activity.LocalSdkActivityHandlerRegistry
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import java.util.concurrent.Executor

/** Local implementation that will be injected to locally loaded SDKs. */
internal class LocalController(
    private val sdkPackageName: String,
    private val applicationContext: Context,
    private val localSdkRegistry: SdkRegistry,
    private val appOwnedSdkRegistry: AppOwnedSdkRegistry
) : SdkSandboxControllerCompat.SandboxControllerImpl {

    override fun loadSdk(
        sdkName: String,
        params: Bundle,
        executor: Executor,
        callback: LoadSdkCallback
    ) {
        try {
            val result = localSdkRegistry.loadSdk(sdkName, params)
            executor.execute { callback.onResult(result) }
        } catch (ex: LoadSdkCompatException) {
            executor.execute { callback.onError(ex) }
        }
    }

    override fun getSandboxedSdks(): List<SandboxedSdkCompat> {
        return localSdkRegistry.getLoadedSdks()
    }

    override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
        appOwnedSdkRegistry.getAppOwnedSdkSandboxInterfaces()

    override fun registerSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ): IBinder {
        return LocalSdkActivityHandlerRegistry.register(sdkPackageName, handlerCompat)
    }

    override fun unregisterSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ) {
        LocalSdkActivityHandlerRegistry.unregister(handlerCompat)
    }

    override fun getClientPackageName(): String = applicationContext.getPackageName()
}
