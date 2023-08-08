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

import androidx.privacysandbox.sdkruntime.client.loader.LocalSdkProvider
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import org.jetbrains.annotations.TestOnly

/**
 * Represents list of locally loaded SDKs.
 * Shared between:
 * 1) [androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat]
 * 2) [androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat]
 */
internal class LocallyLoadedSdks {

    private val sdks = HashMap<String, Entry>()

    fun isLoaded(sdkName: String): Boolean {
        return sdks.containsKey(sdkName)
    }

    fun put(sdkName: String, entry: Entry) {
        sdks[sdkName] = entry
    }

    @TestOnly
    fun get(sdkName: String): Entry? = sdks[sdkName]

    fun remove(sdkName: String): Entry? {
        return sdks.remove(sdkName)
    }

    fun getLoadedSdks(): List<SandboxedSdkCompat> {
        return sdks.values.map { it.sdk }
    }

    data class Entry(
        val sdkProvider: LocalSdkProvider,
        val sdk: SandboxedSdkCompat
    )
}
