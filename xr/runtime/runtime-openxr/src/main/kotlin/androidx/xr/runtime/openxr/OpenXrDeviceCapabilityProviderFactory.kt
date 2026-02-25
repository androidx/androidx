/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.runtime.openxr

import android.content.Context
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.interfaces.XrDeviceCapabilityProvider
import androidx.xr.runtime.interfaces.XrDeviceCapabilityProviderFactory
import kotlin.coroutines.CoroutineContext

internal class OpenXrDeviceCapabilityProviderFactory() : XrDeviceCapabilityProviderFactory {
    companion object {
        private const val LIBRARY_NAME: String = "androidx.xr.runtime.openxr"

        init {
            // TODO(b/461561664): Add proper logging to this library.
            System.loadLibrary(LIBRARY_NAME)
        }
    }

    override val requirements: Set<Feature> = setOf(Feature.FULLSTACK, Feature.OPEN_XR)

    override fun create(
        context: Context,
        coroutineContext: CoroutineContext,
    ): XrDeviceCapabilityProvider {
        return OpenXrDeviceCapabilityProvider(context)
    }
}
