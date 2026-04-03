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

package androidx.xr.runtime

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.openxr.OpenXrInstanceManager

@RequiresOptIn(
    "Access to native pointers is discouraged and the data returned by this API may change in the future."
)
@Retention(AnnotationRetention.BINARY)
public annotation class UnstableNativeResourceApi

/**
 * Returns a [NativeInstanceData] class containing pointers to native resources if available. This
 * is a dangerous API and can put the XR runtime in a bad state if used incorrectly.
 *
 * The pointers are owned by the underlying runtime and should only be used to access APIs available
 * in the native C spec for the relevant runtime. Applications should not trigger any lifecycle
 * events on their own.
 *
 * @throws [IllegalStateException] if the device context is not using a runtime backed by an OpenXR
 *   native instance.
 */
@UnstableNativeResourceApi
public fun XrDevice.getNativeInstanceData(context: Context): NativeInstanceData {
    check(getDeviceContextFeatures(context).contains(Feature.OPEN_XR)) {
        "The device context is not using an OpenXR-enabled runtime." +
            " Native handle access is only supported for OpenXR" +
            " runtimes."
    }

    val instancePointer = OpenXrInstanceManager.getXrInstanceHandle(context)
    // TODO(b/467096822) - Add support for getting the ARCore 1.x function table once it is a
    // dependency.
    val functionTablePointer = OpenXrInstanceManager.getXrInstanceProcAddr()

    return NativeInstanceData(instancePointer, functionTablePointer)
}

/**
 * Returns a [NativeSessionData] class containing pointers to native resources if available. This is
 * a dangerous API and can put the XR Session in a bad state if used incorrectly.
 *
 * The pointers are owned by the underlying runtime and should only be used to access APIs available
 * in the native C spec for the relevant runtime. Applications should not trigger any lifecycle
 * events on their own.
 *
 * @throws [IllegalStateException] if the session is not using a runtime backed by a native session
 *   or the session has been destroyed.
 */
@UnstableNativeResourceApi
public fun Session.getNativeSessionData(): NativeSessionData {
    check(lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
        "Session has been destroyed."
    }

    // TODO(b/467096822) - Add support for getting the ARCore 1.x session once it is a dependency.
    val sessionPointer =
        this.runtimes.firstNotNullOfOrNull { it.sessionPointer }
            ?: throw IllegalStateException(
                "The provided session is not using an OpenXR-enabled runtime." +
                    " Native handle access is only supported for OpenXR" +
                    " sessions."
            )

    return NativeSessionData(sessionPointer)
}

/** Class containing pointers to the native resources backing the XR runtime. */
public class NativeInstanceData
internal constructor(
    /**
     * For OpenXR runtimes, this is the native
     * [XrInstance](https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XrInstance)
     * pointer.
     *
     * For Play Services runtimes this is null.
     */
    @get:Suppress("AutoBoxing") public val instancePointer: Long,
    /**
     * For OpenXR runtimes, this is the function pointer for
     * [xrGetInstanceProcAddr](https://registry.khronos.org/OpenXR/specs/1.0/man/html/xrGetInstanceProcAddr.html).
     *
     * For Play Services runtimes this is null.
     */
    @get:Suppress("AutoBoxing") public val functionTablePointer: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NativeInstanceData) return false

        if (instancePointer != other.instancePointer) return false
        if (functionTablePointer != other.functionTablePointer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = instancePointer.hashCode()
        result = 31 * result + functionTablePointer.hashCode()
        return result
    }
}

/** Class containing pointers to the native resources backing the XR runtime. */
public class NativeSessionData
internal constructor(
    /**
     * For OpenXR runtimes, this is the native
     * [XrSession](https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XrSession)
     * pointer.
     *
     * For Play Services runtimes this is null.
     */
    @get:Suppress("AutoBoxing") public val sessionPointer: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NativeSessionData) return false

        if (sessionPointer != other.sessionPointer) return false

        return true
    }

    override fun hashCode(): Int {
        return sessionPointer.hashCode()
    }
}
