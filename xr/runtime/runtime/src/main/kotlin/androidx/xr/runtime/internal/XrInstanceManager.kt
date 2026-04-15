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
package androidx.xr.runtime.internal

import android.content.Context
import androidx.xr.runtime.NativeInstanceData
import androidx.xr.runtime.getDeviceContextFeatures
import androidx.xr.runtime.interfaces.XrNativeInstanceProvider
import androidx.xr.runtime.loadProviders
import androidx.xr.runtime.selectProvider

/** Manages the loading and provision of native instance data at the runtime layer. */
internal object XrInstanceManager {
    @Volatile private var initContext: Context? = null

    private val provider: XrNativeInstanceProvider? by lazy {
        val context = checkNotNull(initContext) { "Context must be set before initialization" }
        val providers =
            loadProviders(
                XrNativeInstanceProvider::class.java,
                listOf(
                    "androidx.xr.runtime.openxr.OpenXrInstanceManager",
                    "androidx.xr.runtime.testing.internal.FakeXrNativeInstanceProvider",
                ),
            )
        // TODO(b/501089518): Throw an IllegalStateException if provider is not loaded once a
        // provider is returned for all runtimes.
        val newProvider = selectProvider(providers, getDeviceContextFeatures(context))
        newProvider?.apply { initialize(context) }
    }

    /** Returns the XrNativeInstanceProvider after initializing it. */
    internal fun getProvider(context: Context): XrNativeInstanceProvider? {
        if (initContext == null) {
            initContext = context
        }
        return provider
    }

    /** Helper for NativeDataExt to get pointers without direct provider access. */
    internal fun getNativeInstanceData(context: Context): NativeInstanceData? {
        val loadedProvider = getProvider(context) ?: return null
        return NativeInstanceData(
            loadedProvider.xrInstanceHandle,
            loadedProvider.xrInstanceProcAddr,
        )
    }
}
