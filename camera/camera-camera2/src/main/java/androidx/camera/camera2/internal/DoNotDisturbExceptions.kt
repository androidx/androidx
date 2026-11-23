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

package androidx.camera.camera2.internal

import androidx.camera.camera2.pipe.DoNotDisturbException
import androidx.camera.core.CameraUnavailableException
import androidx.camera.core.CameraUnavailableException.CAMERA_UNAVAILABLE_DO_NOT_DISTURB
import androidx.camera.core.InitializationException
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Returns a [CameraUnavailableException] with [CAMERA_UNAVAILABLE_DO_NOT_DISTURB] if this throwable
 * (or any cause in its chain) carries a [DoNotDisturbException].
 */
internal fun Throwable.toDoNotDisturbCameraUnavailableExceptionOrNull():
    CameraUnavailableException? {
    if (this is CameraUnavailableException && reason == CAMERA_UNAVAILABLE_DO_NOT_DISTURB) {
        return this
    }
    val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    visited.add(this)
    val dndCause =
        generateSequence(this) { current -> current.cause?.takeIf(visited::add) }
            .firstOrNull { it is DoNotDisturbException } ?: return null
    return CameraUnavailableException(CAMERA_UNAVAILABLE_DO_NOT_DISTURB, dndCause)
}

/**
 * Returns an [InitializationException] carrying a [CAMERA_UNAVAILABLE_DO_NOT_DISTURB]
 * [CameraUnavailableException] if this throwable (or any cause) is a [DoNotDisturbException].
 */
internal fun Throwable.toDoNotDisturbInitializationExceptionOrNull(): InitializationException? {
    if (
        this is InitializationException &&
            (cause as? CameraUnavailableException)?.reason == CAMERA_UNAVAILABLE_DO_NOT_DISTURB
    ) {
        return this
    }
    return toDoNotDisturbCameraUnavailableExceptionOrNull()?.let(::InitializationException)
}
