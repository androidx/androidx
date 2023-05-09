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

import android.os.IBinder
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat

/**
 * Wrapper for client provided implementation of [SdkSandboxControllerCompat].
 * Checks client version to determine if method supported.
 */
internal class LocalImpl(
    private val implFromClient: SdkSandboxControllerCompat.SandboxControllerImpl,
    private val clientVersion: Int
) : SdkSandboxControllerCompat.SandboxControllerImpl {
    override fun getSandboxedSdks(): List<SandboxedSdkCompat> {
        return implFromClient.getSandboxedSdks()
    }

    override fun registerSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ): IBinder {
        if (clientVersion < 3) {
            throw UnsupportedOperationException(
                "Client library version doesn't support SdkActivities"
            )
        }
        return implFromClient.registerSdkSandboxActivityHandler(handlerCompat)
    }

    override fun unregisterSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ) {
        if (clientVersion < 3) {
            throw UnsupportedOperationException(
                "Client library version doesn't support SdkActivities"
            )
        }
        implFromClient.unregisterSdkSandboxActivityHandler(handlerCompat)
    }
}