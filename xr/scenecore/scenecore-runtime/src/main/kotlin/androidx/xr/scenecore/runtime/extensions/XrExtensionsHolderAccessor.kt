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

package androidx.xr.scenecore.runtime.extensions

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import androidx.xr.runtime.SpatialApiVersionHelper.spatialApiVersion
import androidx.xr.runtime.loadProviders
import androidx.xr.scenecore.runtime.XrExtensionsHolder
import androidx.xr.scenecore.runtime.extensions.XrExtensionsHolderAccessor.PROVIDERS
import androidx.xr.scenecore.runtime.extensions.XrExtensionsHolderAccessor.holder

/**
 * Provides a centralized access point to the active OEM implementation of [XrExtensionsHolder].
 *
 * This helper object searches for a valid [XrExtensionsHolderProvider] on the classpath and exposes
 * its holders.
 */
// TODO (b/502178246): Change to LIBRARY_GROUP once the Compose stop accessing this API.
@RestrictTo(LIBRARY_GROUP_PREFIX)
public object XrExtensionsHolderAccessor {
    /** A list of well-known [XrExtensionsHolderProvider] implementations. */
    private val PROVIDERS =
        listOf(
            // The order in this list defines provider priority. The fake provider is listed first
            // so that it is prioritized in test environments where it is available.
            "androidx.xr.scenecore.testing.FakeXrExtensionsHolderProvider",
            "androidx.xr.scenecore.spatial.core.SpatialCoreXrExtensionsHolderProvider",
        )

    /** Lazily loads all available [XrExtensionsHolderProvider]s from the [PROVIDERS] list. */
    private val providers by lazy {
        loadProviders(XrExtensionsHolderProvider::class.java, PROVIDERS)
    }

    /**
     * Returns a [XrExtensionsHolder] for the [android.extensions.xr.XrExtensions] instance.
     *
     * This property will be `null` under any of the following conditions:
     * 1. No [XrExtensionsHolderProvider] implementation is found on the classpath
     * 2. The underlying XR platform is not supported on the device.
     * 3. The platform's [spatialApiVersion] is less than 2.
     *
     * This holder uses the new `android.extensions.xr` namespace and is the preferred way to access
     * XR functionality.
     */
    @JvmStatic
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val holder: XrExtensionsHolder<*>?
        get() = providers.firstOrNull()?.holder

    /**
     * Returns a [XrExtensionsHolder] for the legacy [com.android.extensions.xr.XrExtensions]
     * instance.
     *
     * This property will be `null` if no [XrExtensionsHolderProvider] is found or if the underlying
     * XR platform is not supported.
     *
     * It provides backward compatibility and is expected to be deprecated and removed in the
     * future. For platforms supporting [spatialApiVersion] 2 or higher, prefer using [holder].
     */
    @JvmStatic
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val holderLegacy: XrExtensionsHolder<*>?
        get() = providers.firstOrNull()?.holderLegacy
}
