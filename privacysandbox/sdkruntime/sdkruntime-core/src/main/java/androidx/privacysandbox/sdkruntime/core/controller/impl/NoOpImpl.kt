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
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import java.util.concurrent.Executor

/**
 * NoOp implementation for cases when [SdkSandboxControllerCompat] not supported.
 */
internal class NoOpImpl : SdkSandboxControllerCompat.SandboxControllerImpl {

    override fun loadSdk(
        sdkName: String,
        params: Bundle,
        executor: Executor,
        callback: SdkSandboxControllerCompat.LoadSdkCallback
    ) {
        executor.execute {
            callback.onError(
                LoadSdkCompatException(
                    LoadSdkCompatException.LOAD_SDK_NOT_FOUND,
                    "Loading SDK not supported on this device"
                )
            )
        }
    }

    override fun getSandboxedSdks(): List<SandboxedSdkCompat> = emptyList()

    override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
        emptyList()

    override fun registerSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ):
        IBinder {
        throw UnsupportedOperationException("Not supported")
    }

    override fun unregisterSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ) {
        throw UnsupportedOperationException("Not supported")
    }
}
