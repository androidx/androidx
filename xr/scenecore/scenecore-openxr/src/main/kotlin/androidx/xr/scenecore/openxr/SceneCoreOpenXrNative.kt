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

package androidx.xr.scenecore.openxr

import androidx.xr.runtime.internal.LibraryNotLinkedException

private const val LIBRARY_NAME = "androidx.xr.scenecore.openxr"

/** Kotlin wrapper class for the OpenXR SceneCore native lifecycle entry points. */
internal class SceneCoreOpenXrNative : AutoCloseable {

    internal var nativeScenecore: Long = 0L
        private set

    init {
        try {
            System.loadLibrary(LIBRARY_NAME)
        } catch (_: UnsatisfiedLinkError) {
            throw LibraryNotLinkedException(LIBRARY_NAME)
        }
        nativeScenecore = nativeCreate()
    }

    /** Instantiates the native OpenXR SceneCore runtime and returns its handle. */
    private external fun nativeCreate(): Long

    /**
     * Deletes the native OpenXR SceneCore runtime handle.
     *
     * @param handle The native runtime handle to destroy.
     */
    private external fun nativeDestroy(handle: Long)

    /** Destroys the internal native runtime handle and sets it to 0L. */
    internal fun destroy() {
        if (nativeScenecore != 0L) {
            nativeDestroy(nativeScenecore)
            nativeScenecore = 0L
        }
    }

    override fun close() {
        destroy()
    }
}
