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

/**
 * Test fake implementation of [SceneCoreOpenXrNative] that records all calls in-memory.
 *
 * Like the native side, it fails to stage a change to an entity that it didn't create, or has
 * destroyed. Like the OpenXR runtime, it has [transactionPoolSize] transactions: an open
 * transaction holds one until it's submitted, and a submitted transaction holds it until
 * [completeSubmittedTransactions]. A cancelled transaction never gives it back, since OpenXR has no
 * way to cancel a transaction. Destroying an entity uses a transaction of its own, to detach it, so
 * it fails while none is free.
 */
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
    // When set, staging an attachment throws, as with a prebuilt that predates the natives.
    var simulateMissingAttachmentNatives: Boolean = false
    // When set, submitting a transaction fails, and it keeps its slot.
    var simulateSubmitFailure: Boolean = false
    // When set, destroying an entity fails, even with a free transaction.
    var simulateDestroyFailure: Boolean = false
    // Runs at the start of submitSceneTransaction(), for example to change the scene mid-commit.
    var onSubmitSceneTransaction: (() -> Unit)? = null

    /** How many transactions can be open, or submitted and not completed, at a time. */
    var transactionPoolSize: Int = Int.MAX_VALUE

    /**
     * How many submitted transactions, including those that detached destroyed entities, hold a
     * slot until [completeSubmittedTransactions].
     */
    var submittedTransactionsInFlight: Int = 0
        private set

    /** How many slots cancelled or failed transactions hold, which never come back. */
    var leakedTransactionSlots: Int = 0
        private set

    /** How many times [createSceneTransaction] was called, including when it failed. */
    var transactionCreateAttempts: Int = 0
        private set

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

    /** The changes of each submitted transaction, in the order they were submitted. */
    val submittedTransactions: MutableList<PendingTransaction> = mutableListOf()
    var requestedSpatialContainerVisible: Boolean? = null

    val isInitialized = AtomicBoolean(false)
    val isSpatialContainerCreated = AtomicBoolean(false)
    val isShutdown = AtomicBoolean(false)

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
        // Like the native side, refuse the root, and entities that it didn't create or destroyed.
        if (simulateDestroyFailure || !isLiveEntity(entityHandle)) return false
        // Like the native side, detach the entity in a transaction of its own.
        if (!isTransactionFree()) return false
        submittedTransactionsInFlight++
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
        transactionCreateAttempts++
        if (!isTransactionFree()) return INVALID_HANDLE
        val handle = nextTransactionHandle++
        openTransactions[handle] = PendingTransaction(handle)
        return handle
    }

    private fun isTransactionFree(): Boolean {
        val usedSlots =
            openTransactions.size + submittedTransactionsInFlight + leakedTransactionSlots
        return !simulateTransactionUnavailable && usedSlots < transactionPoolSize
    }

    override fun setTransactionTransform(
        transactionHandle: Long,
        entityHandle: Long,
        pose: Pose,
        scale: Vector3,
    ): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        val tx = openTransactions[transactionHandle] ?: return false
        if (entityHandle != fakeRootEntityHandle && !isLiveEntity(entityHandle)) return false
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
        // Like the native side, refuse to reparent the root, or to parent to an unknown entity.
        val isParentValid =
            parentHandle == INVALID_HANDLE ||
                parentHandle == fakeRootEntityHandle ||
                isLiveEntity(parentHandle)
        if (!isLiveEntity(childHandle) || !isParentValid || childHandle == parentHandle) {
            return false
        }
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
        if (simulateMissingAttachmentNatives) {
            throw UnsatisfiedLinkError(
                "No implementation found for nativeSetTransactionSpaceAttachment"
            )
        }
        if (simulateAttachmentFailure) return false
        val tx = openTransactions[transactionHandle] ?: return false
        if (!isLiveEntity(entityHandle)) return false
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
        if (!isLiveEntity(entityHandle)) return false
        tx.attachments[entityHandle] = null
        return true
    }

    override fun submitSceneTransaction(transactionHandle: Long): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        onSubmitSceneTransaction?.invoke()
        val tx = openTransactions.remove(transactionHandle) ?: return false
        if (simulateSubmitFailure) {
            leakedTransactionSlots++
            return false
        }
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
        submittedTransactions.add(tx)
        submittedTransactionsInFlight++
        return true
    }

    override fun cancelSceneTransaction(transactionHandle: Long): Boolean {
        check(!isDestroyed.get()) { "SceneCoreOpenXrNative has been destroyed." }
        if (openTransactions.remove(transactionHandle) != null) leakedTransactionSlots++
        cancelledTransactions.add(transactionHandle)
        return true
    }

    /** Frees the slots of the submitted transactions, as the runtime does once it applied them. */
    fun completeSubmittedTransactions() {
        submittedTransactionsInFlight = 0
    }

    private fun isLiveEntity(entityHandle: Long): Boolean = entityHandle in createdEntities

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
        super.destroy()
        shutdown()
        for (txHandle in openTransactions.keys) {
            cancelledTransactions.add(txHandle)
        }
        openTransactions.clear()
    }
}
