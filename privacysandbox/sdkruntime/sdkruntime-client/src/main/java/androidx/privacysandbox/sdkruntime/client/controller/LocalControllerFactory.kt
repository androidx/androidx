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

import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.loader.SdkLoader
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat

/**
 * Create [LocalController] instance for specific sdk.
 */
internal class LocalControllerFactory(
    private val locallyLoadedSdks: LocallyLoadedSdks,
    private val appOwnedSdkRegistry: AppOwnedSdkRegistry
) : SdkLoader.ControllerFactory {
    override fun createControllerFor(
        sdkConfig: LocalSdkConfig
    ): SdkSandboxControllerCompat.SandboxControllerImpl {
        return LocalController(sdkConfig.packageName, locallyLoadedSdks, appOwnedSdkRegistry)
    }
}
