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

// TODO: remove Suppress after XrLog is updated or removed pending b/537445115
@file:Suppress("DEPRECATION")
@file:SuppressLint("RestrictedApiAndroidX")

package androidx.xr.scenecore.openxr

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.xr.runtime.XrLog
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/**
 * Manages an OpenXR scene transaction for atomic mutations of entity transforms and parent
 * hierarchies.
 *
 * Instances are strictly thread-confined and non-reentrant within their `.use { ... }` lifecycle.
 * They are intended to be used on a single thread and must not be shared across threads or invoked
 * recursively.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OpenXrTransaction
internal constructor(
    private val nativeWrapper: SceneCoreOpenXrNative,
    @JvmField internal val transactionHandle: Long,
) : AutoCloseable {

    private var isClosed = false
    private var hasStagingError = false

    public val isAvailable: Boolean
        get() = transactionHandle != INVALID_HANDLE

    /** Sets the local pose and scale of an entity within this transaction. */
    public fun setTransform(entityHandle: Long, pose: Pose, scale: Vector3): OpenXrTransaction {
        if (isClosed) {
            XrLog.warn("Cannot mutate a closed transaction.")
            throw IllegalStateException("Cannot mutate a closed transaction.")
        }
        require(entityHandle != INVALID_HANDLE) {
            "Cannot set transform on entity with INVALID_HANDLE."
        }
        if (transactionHandle != INVALID_HANDLE) {
            val success =
                nativeWrapper.setTransactionTransform(transactionHandle, entityHandle, pose, scale)
            if (!success) {
                XrLog.warn {
                    "Failed to stage transform for entity $entityHandle in transaction $transactionHandle"
                }
                hasStagingError = true
            }
        } else {
            XrLog.warn("Cannot stage transform on an invalid transaction handle.")
            hasStagingError = true
        }
        return this
    }

    /**
     * Sets the parent entity within this transaction.
     *
     * Passing `null` or [INVALID_HANDLE] unparents the child entity in the scene graph.
     */
    public fun setParent(child: Long, parent: Long?): OpenXrTransaction {
        if (isClosed) {
            XrLog.warn("Cannot mutate a closed transaction.")
            throw IllegalStateException("Cannot mutate a closed transaction.")
        }
        require(child != INVALID_HANDLE) { "Cannot set parent on entity with INVALID_HANDLE." }
        if (transactionHandle != INVALID_HANDLE) {
            val parentHandle =
                if (parent == null || parent == INVALID_HANDLE) INVALID_HANDLE else parent
            val success = nativeWrapper.setTransactionParent(transactionHandle, child, parentHandle)
            if (!success) {
                XrLog.warn {
                    "Failed to stage parent for entity $child in transaction $transactionHandle"
                }
                hasStagingError = true
            }
        } else {
            XrLog.warn("Cannot stage parent on an invalid transaction handle.")
            hasStagingError = true
        }
        return this
    }

    /** Submits the transaction to the OpenXR scene graph. */
    public fun commit(): Boolean {
        if (isClosed) {
            XrLog.warn("Cannot commit a closed transaction.")
            throw IllegalStateException("Cannot commit a closed transaction.")
        }
        isClosed = true
        if (hasStagingError) {
            XrLog.warn {
                "Transaction $transactionHandle encountered errors during staging; aborting commit."
            }
            if (transactionHandle != INVALID_HANDLE) {
                nativeWrapper.cancelSceneTransaction(transactionHandle)
            }
            return false
        }
        if (transactionHandle != INVALID_HANDLE) {
            val success = nativeWrapper.submitSceneTransaction(transactionHandle)
            if (!success) {
                XrLog.warn {
                    "Failed to submit transaction $transactionHandle; cancelling transaction."
                }
                nativeWrapper.cancelSceneTransaction(transactionHandle)
            }
            return success
        }
        XrLog.warn("Cannot commit an invalid transaction handle.")
        return false
    }

    /** Cancels the transaction if it has not been committed. */
    override fun close() {
        if (!isClosed) {
            isClosed = true
            if (transactionHandle != INVALID_HANDLE) {
                nativeWrapper.cancelSceneTransaction(transactionHandle)
            }
        }
    }
}
