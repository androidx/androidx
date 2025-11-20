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

@file:JvmName("NativeHandleGetter")

package androidx.xr.arcore.openxr

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime

/*
 * Returns the pointer to the native [XrSession](https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XrSession)
 * from the session's lifecycle manager.
 *
 * The XrSession is owned by the ARCore runtime and should only be used to create new OpenXR
 * objects. Any lifecycle events should be handled only by the ARCore runtime.
 *
 * @throws IllegalArgumentException if the session is not using an OpenXR-enabled runtime.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun getXrSessionPointer(runtime: PerceptionRuntime): Long {
    val manager = runtime.lifecycleManager
    require(manager is OpenXrManager) {
        "The provided session is not using an OpenXR-enabled runtime." +
            " Native handle access is only supported for OpenXR" +
            " sessions."
    }
    // TODO: b/439081333 - Add a check to verify if the Session/LifecycleManager have not been
    // stopped.
    return manager.sessionPointer
}

/**
 * Returns the pointer to the native
 * [XrInstance](https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#instance) from the
 * session's lifecycle manager.
 *
 * The XrInstance is owned by the ARCore runtime and should only be used to create new OpenXR
 * objects. Any lifecycle events should be handled only by the ARCore runtime.
 *
 * @throws IllegalArgumentException if the session is not using an OpenXR-enabled runtime.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun getXrInstancePointer(runtime: PerceptionRuntime): Long {
    val manager = runtime.lifecycleManager
    require(manager is OpenXrManager) {
        "The provided session is not using an OpenXR-enabled runtime." +
            " Native handle access is only supported for OpenXR" +
            " sessions."
    }
    // TODO: b/439081333 - Add a check to verify if the Session/LifecycleManager have not been
    // stopped.
    return manager.instancePointer
}
