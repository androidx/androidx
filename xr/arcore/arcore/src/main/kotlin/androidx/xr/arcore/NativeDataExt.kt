/*
 * Copyright 2025 The Android Open Source Project
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

@file:JvmName("NativeDataExt")

package androidx.xr.arcore

import androidx.lifecycle.Lifecycle
import androidx.xr.arcore.openxr.OpenXrManager
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.Session

@RequiresOptIn(
    "Access to native pointers is discouraged and the data returned by this API may change in the future."
)
@Retention(AnnotationRetention.BINARY)
public annotation class UnstableNativeResourceApi

/**
 * Returns a [NativeData] class containing pointers to native resources if available. This is a
 * dangerous API and can put the JXR Session in a bad state if used incorrectly.
 *
 * The pointers are owned by the ARCore runtime and should only be used to access APIs only
 * available in the native C++ spec for the relevant runtime. Any lifecycle events should be handled
 * only by the ARCore runtime.
 *
 * @throws [IllegalStateException] if the session is not using a runtime backed by a native session
 *   or the session has been destroyed.
 */
@UnstableNativeResourceApi
public fun Session.getNativeData(): NativeData {
    check(lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
        "Session has been destroyed."
    }

    // TODO(b/467096822) - Add support for getting the ARCore 1.x session once it is a dependency.
    val manager =
        this.runtimes.filterIsInstance<PerceptionRuntime>().singleOrNull()?.lifecycleManager
            ?: throw IllegalStateException(
                "The provided session is not backed by a PerceptionRuntime."
            )
    return when (manager) {
        is OpenXrManager -> NativeData(manager.sessionPointer, manager.instancePointer)
        else ->
            throw IllegalStateException(
                "The provided session is not using an OpenXR-enabled runtime." +
                    " Native handle access is only supported for OpenXR" +
                    " sessions."
            )
    }
}

/** Class containing pointers to the native perception resources backing the ARCore runtime. */
public class NativeData
internal constructor(
    /**
     * For OpenXR runtimes, this is the native
     * [XrSession](https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XrSession)
     * pointer.
     *
     * For Play Services runtimes, this is the native ARCore C API session pointer. See
     * [ARCore C API Reference](https://developers.google.com/ar/reference/c).
     */
    @get:Suppress("AutoBoxing") public val nativeSessionPointer: Long,
    /**
     * For OpenXR runtimes, this is the native
     * [XrInstance](https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XrInstance)
     * pointer.
     *
     * For Play Services runtimes this is null.
     */
    @get:Suppress("AutoBoxing") public val nativeInstancePointer: Long?,
)
