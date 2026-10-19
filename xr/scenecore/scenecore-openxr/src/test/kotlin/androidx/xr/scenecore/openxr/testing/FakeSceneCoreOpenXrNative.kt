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

package androidx.xr.scenecore.openxr.testing

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.openxr.ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN
import androidx.xr.scenecore.openxr.AndroidViewPanelDisplayResult
import androidx.xr.scenecore.openxr.INVALID_HANDLE
import androidx.xr.scenecore.openxr.SceneCoreOpenXrNative
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * XR_SCENE_COMPONENT_ATTACHMENT_LOSS_BEHAVIOUR_DETACH_ANDROIDX1, which the native side accepts but
 * production code doesn't use.
 */
internal const val ATTACHMENT_LOSS_BEHAVIOUR_DETACH: Int = 2

/** Test fake implementation of [SceneCoreOpenXrNative] that records all calls in-memory. */
internal class FakeSceneCoreOpenXrNative : SceneCoreOpenXrNative(loadLibrary = false) {

    override var nativeScenecore: Long = 1L // Non-zero handle to indicate active native instance

    var fakeRootEntityHandle: Long = 1000L
    var fakeSpatialContainerHandle: Long = 2000L
    var fakeRootSpaceHandle: Long = 3000L
    // An XrSpace, such as an anchor's, for tests to attach entities to.
    var fakeSpaceHandle: Long = 4000L
    // Volatile because runtime tests can read it from the executor thread.
    @Volatile var fakeRootSpacePoseInPlatformReferenceSpace: Pose? = null
    val rootSpacePoseQueryCount = AtomicInteger(0)
    // When set, getRootSpacePoseInPlatformReferenceSpace() throws it, to simulate a failing sample.
    @Volatile var rootSpacePoseQueryError: Throwable? = null
    var fakeDisplayId: Int = 42
    var fakeInputToken: Any = Any()
    var displayIdFutureToResult: MutableMap<Long, AndroidViewPanelDisplayResult> = mutableMapOf()
    var attachedSurfacePackages: MutableMap<Long, Any> = mutableMapOf()
    private var nextFutureHandle: Long = 9001L

    var simulateTransactionUnavailable: Boolean = false
    var simulateInitFailure: Boolean = false
    var simulateCreateSpatialContainerFailure: Boolean = false
    var allowInvalidSessionHandle: Boolean = false
    // When set, staging or removing an attachment fails, as if the runtime rejected it.
    var simulateAttachmentFailure: Boolean = false

    private var nextEntityHandle: Long = 1001L
    private var nextTransactionHandle: Long = 5001L

    val createdEntities: MutableList<Long> = mutableListOf()
    val destroyedEntities: MutableList<Long> = mutableListOf()
    val entityParents: MutableMap<Long, Long> = mutableMapOf()
    val entityTransforms: MutableMap<Long, Pair<Pose, Vector3>> = mutableMapOf()
    val entityAttachments: MutableMap<Long, Attachment> = mutableMapOf()

    /** The `XrSpace` an entity is attached to, and what happens when that space loses tracking. */
    data class Attachment(val spaceHandle: Long, val trackingLossBehaviour: Int)

    class PendingTransaction(val handle: Long) {
        val transforms: MutableMap<Long, Pair<Pose, Vector3>> = mutableMapOf()
        val parents: MutableMap<Long, Long> = mutableMapOf()
        // A null value removes the entity's attachment on submit.
        val attachments: MutableMap<Long, Attachment?> = mutableMapOf()
    }

    val openTransactions: MutableMap<Long, PendingTransaction> = mutableMapOf()
    val committedTransactions: MutableList<Long> = mutableListOf()
    val cancelledTransactions: MutableList<Long> = mutableListOf()
    var requestedSpatialContainerVisible: Boolean? = null

    val isInitialized = AtomicBoolean(false)
    val isSpatialContainerCreated = AtomicBoolean(false)
    val isShutdown = AtomicBoolean(false)
    val isDestroyed = AtomicBoolean(false)

    override fun init(xrInstanceHandle: Long, xrSessionHandle: Long, gipaHandle: Long): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        if (simulateInitFailure) {
            return false
        }
        if (
            xrInstanceHandle == INVALID_HANDLE ||
                (!allowInvalidSessionHandle && xrSessionHandle == INVALID_HANDLE) ||
                gipaHandle == INVALID_HANDLE
        ) {
            return false
        }
        isInitialized.set(true)
        return true
    }

    override fun createSpatialContainer(): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        check(isInitialized.get()) { "SceneCoreOpenXrNative has not been initialized." }
        if (simulateCreateSpatialContainerFailure) {
            return false
        }
        isSpatialContainerCreated.set(true)
        return true
    }

    override fun getSpatialContainerHandle(): Long {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        check(isInitialized.get()) { "SceneCoreOpenXrNative has not been initialized." }
        return if (isSpatialContainerCreated.get()) fakeSpatialContainerHandle else INVALID_HANDLE
    }

    override fun getRootSpaceHandle(): Long {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        check(isInitialized.get()) { "SceneCoreOpenXrNative has not been initialized." }
        return if (isSpatialContainerCreated.get()) fakeRootSpaceHandle else INVALID_HANDLE
    }

    override fun getRootSpacePoseInPlatformReferenceSpace(): Pose? {
        rootSpacePoseQueryCount.incrementAndGet()
        rootSpacePoseQueryError?.let { throw it }
        if (
            isDestroyed.get() ||
                isShutdown.get() ||
                !isInitialized.get() ||
                !isSpatialContainerCreated.get()
        ) {
            return null
        }
        return fakeRootSpacePoseInPlatformReferenceSpace
    }

    override fun createSceneEntity(): Long {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        check(isInitialized.get()) { "SceneCoreOpenXrNative has not been initialized." }
        val handle = nextEntityHandle++
        createdEntities.add(handle)
        return handle
    }

    override fun destroySceneEntity(entityHandle: Long): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        destroyedEntities.add(entityHandle)
        createdEntities.remove(entityHandle)
        entityParents.remove(entityHandle)
        entityParents.entries.removeIf { it.value == entityHandle }
        entityTransforms.remove(entityHandle)
        entityAttachments.remove(entityHandle)
        return true
    }

    override fun getRootEntityHandle(): Long {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        check(isInitialized.get()) { "SceneCoreOpenXrNative has not been initialized." }
        return if (isSpatialContainerCreated.get()) fakeRootEntityHandle else INVALID_HANDLE
    }

    override fun createSceneTransaction(): Long {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        check(isInitialized.get()) { "SceneCoreOpenXrNative has not been initialized." }
        if (simulateTransactionUnavailable) {
            return INVALID_HANDLE
        }
        val handle = nextTransactionHandle++
        openTransactions[handle] = PendingTransaction(handle)
        return handle
    }

    override fun setTransactionTransform(
        transactionHandle: Long,
        entityHandle: Long,
        pose: Pose,
        scale: Vector3,
    ): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        val tx = openTransactions[transactionHandle] ?: return false
        tx.transforms[entityHandle] = Pair(pose, scale)
        return true
    }

    override fun setTransactionParent(
        transactionHandle: Long,
        childHandle: Long,
        parentHandle: Long,
    ): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        val tx = openTransactions[transactionHandle] ?: return false
        tx.parents[childHandle] = parentHandle
        return true
    }

    override fun setTransactionSpaceAttachment(
        transactionHandle: Long,
        entityHandle: Long,
        spaceHandle: Long,
        trackingLossBehaviour: Int,
    ): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        if (simulateAttachmentFailure) return false
        val tx = openTransactions[transactionHandle] ?: return false
        // Like the native side, reject values that aren't LAST_KNOWN or DETACH.
        if (
            trackingLossBehaviour != ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN &&
                trackingLossBehaviour != ATTACHMENT_LOSS_BEHAVIOUR_DETACH
        ) {
            return false
        }
        tx.attachments[entityHandle] = Attachment(spaceHandle, trackingLossBehaviour)
        return true
    }

    override fun removeTransactionSpaceAttachment(
        transactionHandle: Long,
        entityHandle: Long,
    ): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        if (simulateAttachmentFailure) return false
        val tx = openTransactions[transactionHandle] ?: return false
        tx.attachments[entityHandle] = null
        return true
    }

    override fun submitSceneTransaction(transactionHandle: Long): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        val tx = openTransactions.remove(transactionHandle) ?: return false
        for ((entity, pair) in tx.transforms) {
            entityTransforms[entity] = pair
        }
        for ((child, parent) in tx.parents) {
            if (parent != INVALID_HANDLE) {
                entityParents[child] = parent
            } else {
                entityParents.remove(child)
            }
        }
        for ((entity, attachment) in tx.attachments) {
            if (attachment != null) {
                entityAttachments[entity] = attachment
            } else {
                entityAttachments.remove(entity)
            }
        }
        committedTransactions.add(transactionHandle)
        return true
    }

    override fun cancelSceneTransaction(transactionHandle: Long): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        openTransactions.remove(transactionHandle)
        cancelledTransactions.add(transactionHandle)
        return true
    }

    override fun requestSpatialContainerVisible(visible: Boolean): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        check(isInitialized.get()) { "SceneCoreOpenXrNative has not been initialized." }
        if (!isSpatialContainerCreated.get()) {
            return false
        }
        requestedSpatialContainerVisible = visible
        return true
    }

    override fun requestAndroidViewPanelDisplayId(
        entityHandle: Long,
        width: Int,
        height: Int,
        dpi: Int,
    ): Long {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        val future = nextFutureHandle++
        displayIdFutureToResult[future] =
            AndroidViewPanelDisplayResult(fakeDisplayId, fakeInputToken)
        return future
    }

    override fun pollAndroidViewPanelDisplayId(
        entityHandle: Long,
        futureHandle: Long,
    ): AndroidViewPanelDisplayResult? {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        return displayIdFutureToResult[futureHandle]
    }

    override fun setAndroidViewPanelSurfacePackage(
        entityHandle: Long,
        surfacePackage: Any,
    ): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        attachedSurfacePackages[entityHandle] = surfacePackage
        return true
    }

    override fun shutdown() {
        isShutdown.set(true)
    }

    override fun destroy() {
        if (!isDestroyed.getAndSet(true)) {
            shutdown()
            nativeScenecore = INVALID_HANDLE
            for (txHandle in openTransactions.keys) {
                cancelledTransactions.add(txHandle)
            }
            openTransactions.clear()
        }
    }
}
