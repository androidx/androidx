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
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.xr.runtime.XrExtensionsHolder

/**
 * Provides the XrExtensions available to Jetpack XR at runtime.
 *
 * This is a service provider interface that can be implemented by different XR runtimes.
 */
@RestrictTo(LIBRARY_GROUP)
public interface XrExtensionsHolderProvider {
    /**
     * A type-erased holder for the [android.extensions.xr.XrExtensions] instance.
     *
     * This property will be `null` if the XR runtime does not support XR Extensions version 2 or
     * higher. To access the underlying instance, use the helper functions in
     * [androidx.xr.runtime.TypeHolder].
     *
     * This holder uses the new `android.extensions.xr` namespace and is the preferred way to access
     * XR functionality.
     */
    public val holder: XrExtensionsHolder<*>?

    /**
     * A type-erased holder for the legacy [com.android.extensions.xr.XrExtensions] instance.
     *
     * This property provides backward compatibility with older XR runtimes. It is expected to be
     * deprecated and removed in the future in favor of [holder]. To access the underlying instance,
     * use the helper functions in [androidx.xr.runtime.TypeHolder].
     */
    public val holderLegacy: XrExtensionsHolder<*>?
}
