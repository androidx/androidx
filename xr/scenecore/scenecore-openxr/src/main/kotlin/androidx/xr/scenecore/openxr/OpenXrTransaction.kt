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
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/**
 * Manages an OpenXR scene transaction for atomic mutations of entity transforms, parent
 * hierarchies, and `XrSpace` attachments.
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
        check(!isClosed) { "Cannot mutate a closed transaction." }
        require(entityHandle != INVALID_HANDLE) {
            "Cannot set transform on entity with INVALID_HANDLE."
        }
        val isStaged =
            transactionHandle != INVALID_HANDLE &&
                nativeWrapper.setTransactionTransform(transactionHandle, entityHandle, pose, scale)
        if (!isStaged) {
            // TODO: b/537445115 - Record a warning event that the transform can't be staged.
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
        check(!isClosed) { "Cannot mutate a closed transaction." }
        require(child != INVALID_HANDLE) { "Cannot set parent on entity with INVALID_HANDLE." }
        val parentHandle =
            if (parent == null || parent == INVALID_HANDLE) INVALID_HANDLE else parent
        val isStaged =
            transactionHandle != INVALID_HANDLE &&
                nativeWrapper.setTransactionParent(transactionHandle, child, parentHandle)
        if (!isStaged) {
            // TODO: b/537445115 - Record a warning event that the parent can't be staged.
            hasStagingError = true
        }
        return this
    }

    /**
     * Attaches an entity to an `XrSpace` within this transaction.
     *
     * Once committed, the runtime places the entity at the space's pose every frame. Where the
     * runtime supports it, the entity stays at the space's last known pose while the space isn't
     * tracked.
     *
     * The caller keeps ownership of the space, and must commit [removeSpaceAttachment] before
     * destroying it. Staging fails, so [commit] returns false, if the runtime doesn't have
     * `XR_ANDROIDX1_scene_component_attachment` enabled, or if the space isn't backed by an anchor.
     * For example, the runtime rejects a `LOCAL` reference space with
     * `XR_ERROR_FEATURE_UNSUPPORTED`.
     */
    public fun setSpaceAttachment(entityHandle: Long, spaceHandle: Long): OpenXrTransaction {
        check(!isClosed) { "Cannot mutate a closed transaction." }
        require(entityHandle != INVALID_HANDLE) {
            "Cannot set attachment on entity with INVALID_HANDLE."
        }
        require(spaceHandle != INVALID_HANDLE) { "Cannot attach entity to space INVALID_HANDLE." }
        val isStaged =
            transactionHandle != INVALID_HANDLE &&
                nativeWrapper.setTransactionSpaceAttachment(
                    transactionHandle,
                    entityHandle,
                    spaceHandle,
                    ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN,
                )
        if (!isStaged) {
            // TODO: b/537445115 - Record a warning event that the attachment can't be staged.
            hasStagingError = true
        }
        return this
    }

    /**
     * Removes an entity's `XrSpace` attachment within this transaction.
     *
     * Once committed, the runtime stops moving the entity with the space.
     */
    public fun removeSpaceAttachment(entityHandle: Long): OpenXrTransaction {
        check(!isClosed) { "Cannot mutate a closed transaction." }
        require(entityHandle != INVALID_HANDLE) {
            "Cannot remove attachment from entity with INVALID_HANDLE."
        }
        val isStaged =
            transactionHandle != INVALID_HANDLE &&
                nativeWrapper.removeTransactionSpaceAttachment(transactionHandle, entityHandle)
        if (!isStaged) {
            // TODO: b/537445115 - Record a warning event that the removal can't be staged.
            hasStagingError = true
        }
        return this
    }

    /**
     * Submits the transaction to the OpenXR scene graph.
     *
     * @return true if the runtime accepted the transaction. Returns false, and applies none of its
     *   changes, if this transaction isn't [isAvailable], if a change couldn't be staged, or if the
     *   runtime rejected the transaction.
     */
    public fun commit(): Boolean {
        check(!isClosed) { "Cannot commit a closed transaction." }
        isClosed = true
        if (hasStagingError) {
            if (transactionHandle != INVALID_HANDLE) {
                nativeWrapper.cancelSceneTransaction(transactionHandle)
            }
            return false
        }
        if (transactionHandle != INVALID_HANDLE) {
            val success = nativeWrapper.submitSceneTransaction(transactionHandle)
            if (!success) {
                // TODO: b/537445115 - Record a warning event that the submit failed.
                nativeWrapper.cancelSceneTransaction(transactionHandle)
            }
            return success
        }
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
