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

internal const val INVALID_HANDLE: Long = 0L

/** Kotlin wrapper class for the OpenXR SceneCore native lifecycle entry points. */
internal class SceneCoreOpenXrNative : AutoCloseable {

    internal var nativeScenecore: Long = INVALID_HANDLE
        private set

    init {
        try {
            System.loadLibrary(LIBRARY_NAME)
        } catch (_: UnsatisfiedLinkError) {
            throw LibraryNotLinkedException(LIBRARY_NAME)
        }
        nativeScenecore = nativeCreate()
        check(nativeScenecore != INVALID_HANDLE) {
            "Failed to create native SceneCore runtime instance."
        }
    }

    /** Instantiates the native OpenXR SceneCore runtime and returns its handle. */
    private external fun nativeCreate(): Long

    /** Initializes the native OpenXR ScenecoreManager with instance, session, and GIPA handles. */
    private external fun nativeInit(
        handle: Long,
        xrInstanceHandle: Long,
        xrSessionHandle: Long,
        gipaHandle: Long,
    ): Boolean

    /** Creates the spatial container and root reference space in the native runtime. */
    private external fun nativeCreateSpatialContainer(handle: Long): Boolean

    /** Returns the native XrSpatialContainerEXT handle. */
    private external fun nativeGetSpatialContainerHandle(handle: Long): Long

    /** Returns the native root XrSpace handle. */
    private external fun nativeGetRootSpaceHandle(handle: Long): Long

    /** Shuts down owned spatial container and space handles in the native runtime. */
    private external fun nativeShutdown(handle: Long)

    /** Deletes the native OpenXR SceneCore runtime handle. */
    private external fun nativeDestroy(handle: Long)

    /** Initializes the native OpenXR ScenecoreManager with instance, session, and GIPA handles. */
    internal fun init(xrInstanceHandle: Long, xrSessionHandle: Long, gipaHandle: Long): Boolean {
        check(nativeScenecore != INVALID_HANDLE) { "SceneCoreOpenXrNative has been destroyed." }
        return nativeInit(nativeScenecore, xrInstanceHandle, xrSessionHandle, gipaHandle)
    }

    /** Creates the spatial container and root reference space. */
    internal fun createSpatialContainer(): Boolean {
        check(nativeScenecore != INVALID_HANDLE) { "SceneCoreOpenXrNative has been destroyed." }
        return nativeCreateSpatialContainer(nativeScenecore)
    }

    /** Returns the native XrSpatialContainerEXT handle. */
    internal fun getSpatialContainerHandle(): Long {
        check(nativeScenecore != INVALID_HANDLE) { "SceneCoreOpenXrNative has been destroyed." }
        return nativeGetSpatialContainerHandle(nativeScenecore)
    }

    /** Returns the native root XrSpace handle. */
    internal fun getRootSpaceHandle(): Long {
        check(nativeScenecore != INVALID_HANDLE) { "SceneCoreOpenXrNative has been destroyed." }
        return nativeGetRootSpaceHandle(nativeScenecore)
    }

    /** Cleans up spatial container and space handles. */
    internal fun shutdown() {
        if (nativeScenecore != INVALID_HANDLE) {
            nativeShutdown(nativeScenecore)
        }
    }

    /** Destroys the internal native runtime handle and sets it to INVALID_HANDLE. */
    internal fun destroy() {
        if (nativeScenecore != INVALID_HANDLE) {
            shutdown()
            nativeDestroy(nativeScenecore)
            nativeScenecore = INVALID_HANDLE
        }
    }

    override fun close() {
        destroy()
    }
}
