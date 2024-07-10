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
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat

/**
 * List of features using Client-Core internal API. Each feature available since particular
 * ([ClientApiVersion]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class ClientFeature {
    /**
     * Support for starting SDK Activity:
     * [SdkSandboxControllerCompat.registerSdkSandboxActivityHandler]
     * [SdkSandboxControllerCompat.unregisterSdkSandboxActivityHandler]
     */
    SDK_ACTIVITY_HANDLER,

    /**
     * Support for retrieving App-owned interfaces:
     * [SdkSandboxControllerCompat.getAppOwnedSdkSandboxInterfaces]
     */
    APP_OWNED_INTERFACES,

    /** Support for loading SDKs by other SDKs: [SdkSandboxControllerCompat.loadSdk] */
    LOAD_SDK,

    /**
     * Support for retrieving client app package name:
     * [SdkSandboxControllerCompat.getClientPackageName]
     */
    GET_CLIENT_PACKAGE_NAME;

    val availableFrom: ClientApiVersion
        get() = ClientApiVersion.minAvailableVersionFor(this)

    fun isAvailable(apiLevel: Int): Boolean {
        return apiLevel >= availableFrom.apiLevel
    }
}
