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

package androidx.xr.scenecore.runtime

import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RestrictTo

/**
 * Checks that the runtime API level is at least [requiredApiLevel] and, if so, executes the
 * [action] lambda, returning its return value. If the current API level is not at least
 * [requiredApiLevel], an [UnsupportedOperationException] is thrown.
 *
 * Although Jetpack XR's minSDK is 24, creating a SceneCore runtime is not currently supported on
 * any device that runs with an API lower than 34. This helper method is used to perform a check at
 * call sites for APIs that are not supported by minSDK=24 but are known to be safe to call based on
 * the existence of a Session. Without using this helper, all such call sites would need either 1) a
 * `@RequiresApi()` annotation that would need to be addressed by redundant client code, or 2) a
 * `Suppress("NewApi")` annotation, which is not allowed by Jetpack.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
public inline fun <T> requiresApiLevel(requiredApiLevel: Int, action: () -> T): T {
    return if (android.os.Build.VERSION.SDK_INT >= requiredApiLevel) {
        action()
    } else {
        throw UnsupportedOperationException(
            "Attempted to call an API for level $requiredApiLevel when running on ${android.os.Build.VERSION.SDK_INT}. A Jetpack XR Session should not have been created at the current API level."
        )
    }
}
