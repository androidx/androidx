/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.core.controller

import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import java.util.concurrent.Executor

/** Internal implementation of [SdkSandboxControllerCompat] */
@RestrictTo(LIBRARY_GROUP)
public interface SdkSandboxControllerBackend {

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

    public fun unregisterSdkSandboxActivityHandler(handlerCompat: SdkSandboxActivityHandlerCompat)

    public fun getClientPackageName(): String

    public fun registerSdkSandboxClientImportanceListener(
        executor: Executor,
        listenerCompat: SdkSandboxClientImportanceListenerCompat,
    )

    public fun unregisterSdkSandboxClientImportanceListener(
        listenerCompat: SdkSandboxClientImportanceListenerCompat
    )
}
