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

package androidx.privacysandbox.sdkruntime.core.internal

import androidx.annotation.RestrictTo
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerBackendHolder

/**
 * List of features using Client-Core internal API. Each feature available since particular
 * ([ClientApiVersion]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class ClientFeature {
    /**
     * Support for retrieving client app package name:
     * [androidx.privacysandbox.sdkruntime.provider.controller.SdkSandboxControllerCompat.getClientPackageName]
     */
    GET_CLIENT_PACKAGE_NAME,

    /**
     * Support for listening of client app foreground state:
     *
     * [androidx.privacysandbox.sdkruntime.provider.controller.SdkSandboxControllerCompat.registerSdkSandboxClientImportanceListener]
     * [androidx.privacysandbox.sdkruntime.provider.controller.SdkSandboxControllerCompat.unregisterSdkSandboxClientImportanceListener]
     */
    CLIENT_IMPORTANCE_LISTENER,

    /** Dedicated [SdkSandboxControllerBackendHolder] for setting local implementation. */
    SDK_SANDBOX_CONTROLLER_BACKEND_HOLDER;

    public val availableFrom: ClientApiVersion
        get() = ClientApiVersion.minAvailableVersionFor(this)

    public fun isAvailable(apiLevel: Int): Boolean {
        return apiLevel >= availableFrom.apiLevel
    }
}
