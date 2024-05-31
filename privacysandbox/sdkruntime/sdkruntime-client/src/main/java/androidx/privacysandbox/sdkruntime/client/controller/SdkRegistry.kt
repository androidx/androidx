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

package androidx.privacysandbox.sdkruntime.client.controller

import android.os.Bundle
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat

/** Responsible for lifecycle of particular SDKs (local, sandbox, test, etc). */
internal interface SdkRegistry {

    /**
     * Checks if SDK could be loaded / unloaded by this SdkRegistry.
     *
     * @param sdkName name of the SDK to be loaded / unloaded.
     * @return true if SDK could be loaded / unloaded by this SdkRegistry or false otherwise
     */
    fun isResponsibleFor(sdkName: String): Boolean

    /**
     * Loads SDK.
     *
     * @param sdkName name of the SDK to be loaded.
     * @param params additional parameters to be passed to the SDK in the form of a [Bundle] as
     *   agreed between the client and the SDK.
     * @return [SandboxedSdkCompat] from SDK on a successful run.
     * @throws [LoadSdkCompatException] on fail or when SdkRegistry not responsible for this SDK.
     */
    fun loadSdk(sdkName: String, params: Bundle): SandboxedSdkCompat

    /**
     * Unloads an SDK that has been previously loaded by SdkRegistry.
     *
     * @param sdkName name of the SDK to be unloaded.
     */
    fun unloadSdk(sdkName: String)

    /**
     * Fetches information about Sdks that are loaded by this SdkRegistry.
     *
     * @return List of [SandboxedSdkCompat] containing all currently loaded sdks
     */
    fun getLoadedSdks(): List<SandboxedSdkCompat>
}
