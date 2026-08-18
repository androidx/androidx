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

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.LibraryNotLinkedException
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import java.util.concurrent.atomic.AtomicBoolean

internal const val INVALID_HANDLE: Long = 0L

private const val LIBRARY_NAME = "androidx.xr.scenecore.openxr"

/** Kotlin wrapper class for the OpenXR SceneCore native lifecycle entry points. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class SceneCoreOpenXrNative internal constructor(loadLibrary: Boolean = true) :
    AutoCloseable {

    internal open var nativeScenecore: Long = INVALID_HANDLE

    private val isLibraryLoaded = AtomicBoolean(false)
    private val isDestroyed = AtomicBoolean(false)

    init {
        if (loadLibrary) {
            try {
                System.loadLibrary(LIBRARY_NAME)
                isLibraryLoaded.set(true)
            } catch (_: UnsatisfiedLinkError) {
                throw LibraryNotLinkedException(LIBRARY_NAME)
            }
            nativeScenecore = nativeCreate()
            check(nativeScenecore != INVALID_HANDLE) {
                "Failed to create native SceneCore runtime instance."
            }
        }
    }

    /** Native JNI entry points matching exported symbols in libandroidx.xr.scenecore.openxr.so */
    private external fun nativeCreate(): Long

    private external fun nativeInit(
        handle: Long,
        instance: Long,
        session: Long,
        gipa: Long,
    ): Boolean

    private external fun nativeCreateSpatialContainer(handle: Long): Boolean

    private external fun nativeGetSpatialContainerHandle(handle: Long): Long

    private external fun nativeGetRootSpaceHandle(handle: Long): Long

    private external fun nativeGetRootEntityHandle(handle: Long): Long

    private external fun nativeCreateSceneEntity(handle: Long): Long

    private external fun nativeDestroySceneEntity(handle: Long, entityHandle: Long): Boolean

    private external fun nativeCreateSceneTransaction(handle: Long): Long

    private external fun nativeSetTransactionTransform(
        handle: Long,
        transactionHandle: Long,
        entityHandle: Long,
        px: Float,
        py: Float,
        pz: Float,
        qx: Float,
        qy: Float,
        qz: Float,
        qw: Float,
        sx: Float,
        sy: Float,
        sz: Float,
    ): Boolean

    private external fun nativeSetTransactionParent(
        handle: Long,
        transactionHandle: Long,
        childHandle: Long,
        parentHandle: Long,
    ): Boolean

    private external fun nativeSubmitSceneTransaction(
        handle: Long,
        transactionHandle: Long,
    ): Boolean

    private external fun nativeCancelSceneTransaction(
        handle: Long,
        transactionHandle: Long,
    ): Boolean

    private external fun nativeShutdown(handle: Long)

    private external fun nativeDestroy(handle: Long)

    /** Initializes the native OpenXR ScenecoreManager with instance, session, and GIPA handles. */
    open fun init(xrInstanceHandle: Long, xrSessionHandle: Long, gipaHandle: Long): Boolean {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeInit(nativeScenecore, xrInstanceHandle, xrSessionHandle, gipaHandle)
    }

    /** Creates the spatial container and root reference space. */
    open fun createSpatialContainer(): Boolean {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeCreateSpatialContainer(nativeScenecore)
    }

    /** Returns the native XrSpatialContainerEXT handle. */
    open fun getSpatialContainerHandle(): Long {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeGetSpatialContainerHandle(nativeScenecore)
    }

    /** Returns the native root XrSpace handle. */
    open fun getRootSpaceHandle(): Long {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeGetRootSpaceHandle(nativeScenecore)
    }

    /** Returns the native root XrSceneEntityKHRX1 handle. */
    open fun getRootEntityHandle(): Long {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeGetRootEntityHandle(nativeScenecore)
    }

    /** Creates a child scene entity under the scene context. */
    open fun createSceneEntity(): Long {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeCreateSceneEntity(nativeScenecore)
    }

    /** Destroys an existing child scene entity. */
    open fun destroySceneEntity(entityHandle: Long): Boolean {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeDestroySceneEntity(nativeScenecore, entityHandle)
    }

    /** Creates a new OpenXR scene transaction. */
    open fun createSceneTransaction(): Long {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeCreateSceneTransaction(nativeScenecore)
    }

    /** Stages a transform component mutation in the given transaction. */
    open fun setTransactionTransform(
        transactionHandle: Long,
        entityHandle: Long,
        pose: Pose,
        scale: Vector3,
    ): Boolean {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeSetTransactionTransform(
            nativeScenecore,
            transactionHandle,
            entityHandle,
            pose.translation.x,
            pose.translation.y,
            pose.translation.z,
            pose.rotation.x,
            pose.rotation.y,
            pose.rotation.z,
            pose.rotation.w,
            scale.x,
            scale.y,
            scale.z,
        )
    }

    /** Stages a parent component mutation (or unparent if parentHandle is INVALID_HANDLE). */
    open fun setTransactionParent(
        transactionHandle: Long,
        childHandle: Long,
        parentHandle: Long,
    ): Boolean {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeSetTransactionParent(
            nativeScenecore,
            transactionHandle,
            childHandle,
            parentHandle,
        )
    }

    /** Submits and commits the batched mutations in the transaction. */
    open fun submitSceneTransaction(transactionHandle: Long): Boolean {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeSubmitSceneTransaction(nativeScenecore, transactionHandle)
    }

    /** Cancels the transaction and drops uncommitted mutations. */
    open fun cancelSceneTransaction(transactionHandle: Long): Boolean {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        return nativeCancelSceneTransaction(nativeScenecore, transactionHandle)
    }

    /**
     * Creates a new [OpenXrTransaction] instance.
     *
     * @throws IllegalStateException if [SceneCoreOpenXrNative] has been destroyed.
     */
    fun openTransaction(): OpenXrTransaction {
        check(nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            "SceneCoreOpenXrNative has been destroyed."
        }
        val txHandle = createSceneTransaction()
        return OpenXrTransaction(this, txHandle)
    }

    /** Cleans up spatial container and space handles. */
    open fun shutdown() {
        if (nativeScenecore != INVALID_HANDLE && !isDestroyed.get()) {
            nativeShutdown(nativeScenecore)
        }
    }

    /** Destroys the internal native runtime handle and sets it to INVALID_HANDLE. */
    open fun destroy() {
        if (!isDestroyed.getAndSet(true) && nativeScenecore != INVALID_HANDLE) {
            nativeShutdown(nativeScenecore)
            nativeDestroy(nativeScenecore)
            nativeScenecore = INVALID_HANDLE
        }
    }

    override fun close() {
        destroy()
    }
}
