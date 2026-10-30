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

import androidx.annotation.GuardedBy
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Commits the scene graph changes of SceneCore entities to the OpenXR runtime, about once a frame.
 *
 * The runtime has only a few scene transactions, and refuses to create one while they are all in
 * use. So instead of committing a transaction per change, entities [edit] their changes here. A few
 * milliseconds later, the committer commits the latest value of every changed component in one
 * transaction, then waits about a frame before the next commit. When no transaction is free, it
 * keeps the changes and tries again with exponential backoff, so no change is lost. It polls, since
 * SceneCore can't receive XR_TYPE_EVENT_DATA_SCENE_TRANSACTION_AVAILABLE_EXTX1 yet.
 *
 * * **Ordering:** The changes to an entity commit together, and the entities that have waited the
 *   longest commit first.
 * * **Destruction:** [SceneEditor.destroyEntity] drops the uncommitted changes to that entity, and
 *   ignores later changes to it. The native entity is only destroyed after the changes made before,
 *   such as moving its children elsewhere, since the runtime can remove its children with it. The
 *   committer keeps trying to destroy it until the runtime does, since the entity stays in the
 *   scene until then, and commits the other changes meanwhile. Destroying it takes a transaction of
 *   its own, so a commit can use more than one transaction.
 * * **Submission:** OpenXR has no way to cancel a transaction, so the committer submits every
 *   transaction that it creates, even when the runtime rejected every change in it.
 * * **Threading:** This class is thread-safe. Commits run one at a time on [executor], which also
 *   runs the [CommitCallback]s, without holding the committer's lock or a [SpaceLease]'s. So a
 *   caller can hold its own lock while it calls [edit] or [SpaceLease.revoke], and take that lock
 *   in its callbacks.
 *
 * @param nativeWrapper the native runtime to create and submit transactions with.
 * @param executor the executor that runs the commits. Commits are serialized by the committer.
 * @param coalescingDelayMs how long to wait after the first change before committing, so that the
 *   changes made together commit together. It's not a debounce: later changes don't extend it.
 * @param minCommitIntervalMs the shortest time between the end of a commit and the start of the
 *   next one.
 * @param initialRetryDelayMs how long to wait before trying again when no transaction is free. The
 *   delay doubles after each try, up to [maxRetryDelayMs].
 * @param maxEntitiesPerTransaction how many entities a transaction changes at most, so that it fits
 *   in the runtime's transaction buffer. The other entities change in the next transactions.
 */
internal class OpenXrSceneCommitter(
    private val nativeWrapper: SceneCoreOpenXrNative,
    private val executor: ScheduledExecutorService,
    private val coalescingDelayMs: Long = COALESCING_DELAY_MS,
    private val minCommitIntervalMs: Long = MIN_COMMIT_INTERVAL_MS,
    private val initialRetryDelayMs: Long = INITIAL_RETRY_DELAY_MS,
    private val maxRetryDelayMs: Long = MAX_RETRY_DELAY_MS,
    private val maxEntitiesPerTransaction: Int = MAX_ENTITIES_PER_TRANSACTION,
) {

    init {
        require(coalescingDelayMs >= 0 && minCommitIntervalMs >= 0) {
            "Delays must not be negative."
        }
        require(initialRetryDelayMs in 1..maxRetryDelayMs) {
            "initialRetryDelayMs must be positive and at most maxRetryDelayMs."
        }
        require(maxEntitiesPerTransaction > 0) { "maxEntitiesPerTransaction must be positive." }
    }

    /** Records changes to the scene graph, see [edit]. */
    internal interface SceneEditor {
        /** Sets the [pose] and [scale] of entity [entityHandle], relative to its parent. */
        fun setTransform(entityHandle: Long, pose: Pose, scale: Vector3)

        /**
         * Parents entity [entityHandle] to entity [parentHandle], or unparents it if [parentHandle]
         * is [INVALID_HANDLE].
         */
        fun setParent(entityHandle: Long, parentHandle: Long)

        /**
         * Attaches entity [entityHandle] to the `XrSpace` of [lease], so that the runtime draws it
         * at the space's pose, and reports the result to [onResult]. Where the runtime supports it,
         * the entity stays at the space's last known pose while the space isn't tracked.
         *
         * The runtime reads the space when the committer stages the attachment, which the committer
         * only does while [lease] isn't revoked.
         */
        fun setSpaceAttachment(entityHandle: Long, lease: SpaceLease, onResult: CommitCallback)

        /**
         * Removes the attachment of entity [entityHandle], if any, and reports the result to
         * [onResult], if any.
         */
        fun removeSpaceAttachment(entityHandle: Long, onResult: CommitCallback? = null)

        /**
         * Destroys native entity [entityHandle] after the changes made before, and drops its
         * uncommitted changes. Ignores any later change to it.
         */
        fun destroyEntity(entityHandle: Long)
    }

    /**
     * Lets the committer pass the `XrSpace` [spaceHandle] to the runtime, to attach an entity to
     * it, until [revoke] is called.
     *
     * The runtime reads the space when the committer stages the attachment. So once [revoke]
     * returns, the runtime doesn't read the space for this attachment anymore. If [revoke] returns
     * true, the runtime has the attachment, or will once the transaction commits: remove the
     * attachment, and wait for the result of the removal, before destroying the space.
     */
    internal class SpaceLease(private val spaceHandle: Long) {
        init {
            require(spaceHandle != INVALID_HANDLE) { "Cannot attach to space INVALID_HANDLE." }
        }

        private val lock = Any()

        @GuardedBy("lock") private var isRevoked = false

        // Whether the runtime accepted the attachment in a transaction.
        @GuardedBy("lock") private var isStaged = false

        /**
         * Stops the committer from staging the attachment. If the committer is staging it, waits
         * for it to finish first.
         *
         * @return whether the runtime accepted the attachment.
         */
        fun revoke(): Boolean =
            synchronized(lock) {
                isRevoked = true
                isStaged
            }

        /**
         * Unless this lease is revoked, runs [stage] with the space while holding the lease's lock,
         * so that [revoke] waits for it. Only the committer calls this.
         *
         * @return what [stage] returns, which is whether the runtime accepted the attachment, or
         *   null if this lease is revoked.
         */
        internal fun stageUnlessRevoked(stage: (spaceHandle: Long) -> Boolean): Boolean? {
            synchronized(lock) {
                if (isRevoked) return null
                val isAccepted = stage(spaceHandle)
                if (isAccepted) isStaged = true
                return isAccepted
            }
        }
    }

    /** The outcome of an attachment change, see [CommitCallback]. */
    internal enum class CommitResult {
        /** The change was committed to the runtime. */
        COMMITTED,
        /** The runtime refused the change. */
        REJECTED,
        /**
         * The change wasn't committed, and won't be: its [SpaceLease] was revoked before the
         * committer staged it, or the committer stopped first.
         */
        DROPPED,
    }

    /**
     * Receives the [CommitResult] of an attachment change on the committer's executor, once the
     * runtime commits or rejects the change, or once the committer drops it.
     *
     * Runs once for each change, unless a newer attachment change to the entity replaces it before
     * it's staged, or destroying the entity drops it. When the committer stops, the changes that
     * are waiting, or in a transaction that wasn't submitted, are DROPPED. The committer stops on
     * [shutdown], and when the native instance is destroyed, a native call throws, or the executor
     * shuts down. Never runs for a change made in an [edit] that returns false.
     */
    internal fun interface CommitCallback {
        fun onResult(result: CommitResult)
    }

    private val lock = Any()

    // Every entity that this committer changed and hasn't destroyed.
    @GuardedBy("lock") private val entities = HashMap<Long, EntityState>()

    // The entities with changes to commit, or waiting to be destroyed, from the one that has waited
    // the longest. Commits take them in this order, so that every entity gets its turn, and an
    // entity is only destroyed after the changes made before.
    @GuardedBy("lock") private val commitQueue = LinkedHashSet<EntityState>()

    @GuardedBy("lock") private var state = State.IDLE

    @GuardedBy("lock") private var scheduledCommit: ScheduledFuture<*>? = null

    // Current backoff delay; 0 when the last commit succeeded.
    @GuardedBy("lock") private var retryDelayMs = 0L

    private val editor = Editor()

    private val commitTask = Runnable { commit() }

    /**
     * Records the scene graph changes made by [changes], and schedules their commit.
     *
     * The changes to an entity commit together, but the changes to different entities can commit in
     * different transactions.
     *
     * [changes] runs while holding the committer's lock, so it must only call [SceneEditor]
     * methods, and must not keep the [SceneEditor]. If it throws, the changes it made before stay
     * recorded.
     *
     * @return false if the committer stopped, before or during this edit. The changes then won't
     *   commit, and their callbacks won't run.
     * @throws IllegalArgumentException if [changes] passes [INVALID_HANDLE] as an entity.
     */
    fun edit(changes: SceneEditor.() -> Unit): Boolean {
        synchronized(lock) {
            if (state == State.SHUT_DOWN) return false
            try {
                editor.changes()
            } finally {
                // If a commit is scheduled or running, it will pick these changes up.
                if (state == State.IDLE && commitQueue.isNotEmpty()) {
                    // Nothing else was waiting, so if the executor rejects the commit, only the
                    // callbacks of these changes don't run.
                    scheduleCommitLocked(coalescingDelayMs)
                }
            }
            return state != State.SHUT_DOWN
        }
    }

    /**
     * Drops every uncommitted change, and stops committing. The callbacks of the attachment changes
     * that were waiting get DROPPED, on the executor, unless it has shut down.
     *
     * Destroying the native instance afterwards releases the native entities.
     */
    fun shutdown() {
        val reports = synchronized(lock) { shutDownLocked() }
        if (reports.isEmpty()) return
        try {
            executor.execute { deliver(reports) }
        } catch (_: RejectedExecutionException) {
            // The executor shut down with the runtime, so nothing can run the callbacks.
        }
    }

    private fun commit() {
        val batch =
            synchronized(lock) {
                // shutdown() ran since the commit was scheduled.
                if (state != State.SCHEDULED) return
                scheduledCommit = null
                if (commitQueue.isEmpty()) {
                    // Nothing changed while waiting after the last commit.
                    retryDelayMs = 0L
                    state = State.IDLE
                    return
                }
                state = State.RUNNING
                takeBatchLocked()
            }
        val batchCommit =
            try {
                // Holds the native lifecycle read lock so destroy() on another thread waits for
                // these calls.
                nativeWrapper.runIfAlive { commitBatch(batch) }
            } catch (t: Throwable) {
                val reports = synchronized(lock) { shutDownLocked() + batch.droppedReports() }
                // ScheduledExecutorService.schedule() captures exceptions in the returned
                // ScheduledFuture instead of delivering them to the thread's
                // UncaughtExceptionHandler, so report the failure to the handler explicitly.
                val thread = Thread.currentThread()
                thread.uncaughtExceptionHandler?.uncaughtException(
                    thread,
                    IllegalStateException(
                        "Committing scene changes failed; later changes won't be committed.",
                        t,
                    ),
                )
                deliver(reports)
                // Rethrow so callers that run tasks synchronously, such as tests, see the failure.
                throw t
            }
        val reports =
            synchronized(lock) {
                if (batchCommit == null) {
                    // The native instance was destroyed, with every native entity, before the
                    // batch was committed.
                    shutDownLocked() + batch.droppedReports()
                } else {
                    finishBatchLocked(batch, batchCommit)
                }
            }
        deliver(reports)
    }

    /**
     * Takes the changes of the entities that have waited the longest, and the native entities to
     * destroy among them.
     */
    @GuardedBy("lock")
    private fun takeBatchLocked(): Batch {
        val destructionsBefore = ArrayList<EntityState>()
        val changes = ArrayList<EntityChanges>()
        val destructionsAfter = ArrayList<EntityState>()
        for (entity in commitQueue) {
            if (entity.hasPendingChanges) {
                // The next entities wait for the next commit, including those to destroy, so that
                // they're destroyed after the changes made before.
                if (changes.size == maxEntitiesPerTransaction) break
                changes +=
                    EntityChanges(
                        entity,
                        entity.attachment.takePending(),
                        entity.parent.takePending(),
                        entity.transform.takePending(),
                    )
            } else if (entity.isDestroyRequested) {
                if (changes.isEmpty()) destructionsBefore += entity else destructionsAfter += entity
            }
        }
        // Batched entities leave the queue; a change made during the commit re-queues them at the
        // back (prevents starvation). Entities to destroy stay in [entities] until they're
        // destroyed, so changes to their handles during the commit are ignored.
        changes.forEach { commitQueue.remove(it.entity) }
        destructionsBefore.forEach { commitQueue.remove(it) }
        destructionsAfter.forEach { commitQueue.remove(it) }
        return Batch(destructionsBefore, changes, destructionsAfter)
    }

    /**
     * Commits [batch] to the runtime, and returns what happened. Runs without holding [lock], so it
     * only reads immutable values from [batch].
     */
    private fun commitBatch(batch: Batch): BatchCommit {
        val destroyed = HashSet<EntityState>()
        val rejectedAttachments = HashSet<EntityState>()
        val revokedAttachments = HashSet<EntityState>()
        // Destroying a native entity detaches it in a transaction of its own, so if one fails, no
        // transaction is likely free for the next ones either.
        fun destroy(entities: List<EntityState>) {
            for (entity in entities) {
                if (!nativeWrapper.destroySceneEntity(entity.entityHandle)) return
                destroyed += entity
            }
        }
        fun notSubmitted() =
            BatchCommit(
                isSubmitted = false,
                destroyed = destroyed,
                rejectedAttachments = emptySet(),
                revokedAttachments = revokedAttachments,
            )

        // Commits the changes even if a destruction failed, so that one that keeps failing doesn't
        // hold them back. If no transaction is free, they can't be committed either.
        destroy(batch.destructionsBefore)

        if (batch.changes.isNotEmpty()) {
            val transactionHandle = nativeWrapper.createSceneTransaction()
            if (transactionHandle == INVALID_HANDLE) return notSubmitted()
            for (changes in batch.changes) {
                when (stage(transactionHandle, changes)) {
                    CommitResult.REJECTED -> rejectedAttachments += changes.entity
                    CommitResult.DROPPED -> revokedAttachments += changes.entity
                    CommitResult.COMMITTED,
                    null -> {}
                }
            }
            // A transaction only ends once it's submitted, even with nothing staged in it.
            if (!nativeWrapper.submitSceneTransaction(transactionHandle)) return notSubmitted()
        }

        destroy(batch.destructionsAfter)
        return BatchCommit(
            isSubmitted = true,
            destroyed = destroyed,
            rejectedAttachments = rejectedAttachments,
            revokedAttachments = revokedAttachments,
        )
    }

    /**
     * Stages [changes] in transaction [transactionHandle], and returns how the attachment change
     * went, if there's one: COMMITTED if the runtime accepted it, so that it commits with the
     * transaction, REJECTED, or DROPPED if its lease is revoked.
     *
     * Stages the attachment change first, since the runtime is the most likely to reject it, and
     * stops at the first change that is rejected or dropped, so that it doesn't apply the changes
     * that go with it: an attached entity is parented to the root, and a transform is relative to
     * the parent. So if the runtime rejects the removal of an attachment, the entity stays parented
     * to the root, where an attached entity belongs.
     */
    private fun stage(transactionHandle: Long, changes: EntityChanges): CommitResult? {
        val entityHandle = changes.entity.entityHandle
        // TODO: b/537445115 - Record a warning event when the runtime rejects a change.
        val attachmentResult =
            changes.attachment?.let { stageAttachment(transactionHandle, entityHandle, it) }
        val isParentStaged =
            (attachmentResult == null || attachmentResult == CommitResult.COMMITTED) &&
                (changes.parent == null ||
                    nativeWrapper.setTransactionParent(
                        transactionHandle,
                        entityHandle,
                        changes.parent,
                    ))
        if (isParentStaged && changes.transform != null) {
            nativeWrapper.setTransactionTransform(
                transactionHandle,
                entityHandle,
                changes.transform.pose,
                changes.transform.scale,
            )
        }
        return attachmentResult
    }

    /**
     * Stages [change] to the attachment of entity [entityHandle]: the attachment, unless its lease
     * is revoked, or the removal of the attachment.
     */
    private fun stageAttachment(
        transactionHandle: Long,
        entityHandle: Long,
        change: AttachmentChange,
    ): CommitResult {
        val isStaged =
            try {
                if (change.lease == null) {
                    nativeWrapper.removeTransactionSpaceAttachment(transactionHandle, entityHandle)
                } else {
                    change.lease.stageUnlessRevoked { spaceHandle ->
                        nativeWrapper.setTransactionSpaceAttachment(
                            transactionHandle,
                            entityHandle,
                            spaceHandle,
                            ATTACHMENT_LOSS_BEHAVIOUR_LAST_KNOWN,
                        )
                    } ?: return CommitResult.DROPPED
                }
            } catch (_: UnsatisfiedLinkError) {
                // TODO: b/575789269 - Remove once the prebuilt has these natives.
                false
            }
        return if (isStaged) CommitResult.COMMITTED else CommitResult.REJECTED
    }

    /** Records the results of committing [batch], and returns the reports to deliver. */
    @GuardedBy("lock")
    private fun finishBatchLocked(batch: Batch, batchCommit: BatchCommit): List<Report> {
        val reports = ArrayList<Report>()
        for (changes in batch.changes) {
            val onResult = changes.attachment?.onResult ?: continue
            val result = batchCommit.attachmentResult(changes.entity)
            if (result != null) {
                reports += Report(onResult, result)
            } else if (state == State.SHUT_DOWN) {
                // The change would be committed next time, but shutdown() ran during the commit.
                reports += Report(onResult, CommitResult.DROPPED)
            }
        }
        // shutdown() ran during the commit, and dropped the other changes.
        if (state == State.SHUT_DOWN) return reports
        // The entities whose changes weren't submitted, which have waited the longest.
        val unsubmitted = ArrayList<EntityState>()
        for (changes in batch.changes) {
            val entity = changes.entity
            // A revoked attachment is never committed, and neither are the changes that go with it.
            val isDone = batchCommit.isSubmitted || entity in batchCommit.revokedAttachments
            entity.attachment.finish(isDone)
            entity.parent.finish(isDone)
            entity.transform.finish(isDone)
            if (entity.isDestroyRequested) {
                // destroyEntity() ran during the commit, so drop the changes that weren't
                // committed.
                entity.dropPendingChanges()
            }
            if (!batchCommit.isSubmitted && entity.hasPendingChanges) {
                unsubmitted += entity
            }
        }
        batchCommit.destroyed.forEach { entities.remove(it.entityHandle) }
        // TODO: b/537445115 - Record a warning event when the runtime refuses to destroy an entity.
        val remainingBefore = batch.destructionsBefore - batchCommit.destroyed
        val remainingAfter = batch.destructionsAfter - batchCommit.destroyed
        moveToFrontLocked(remainingBefore + unsubmitted + remainingAfter)
        // TODO: b/538937703 - Try again on XR_TYPE_EVENT_DATA_SCENE_TRANSACTION_AVAILABLE_EXTX1
        // instead of polling, once SceneCore receives OpenXR events.
        val shouldBackOff =
            (!batchCommit.isSubmitted ||
                remainingBefore.isNotEmpty() ||
                remainingAfter.isNotEmpty()) && commitQueue.isNotEmpty()
        retryDelayMs =
            when {
                !shouldBackOff -> 0L
                retryDelayMs == 0L -> initialRetryDelayMs
                else -> minOf(retryDelayMs * 2, maxRetryDelayMs)
            }
        // Waits before the next commit even with nothing left to commit, so that the changes made
        // in the meantime commit together.
        reports += scheduleCommitLocked(if (shouldBackOff) retryDelayMs else minCommitIntervalMs)
        return reports
    }

    /** Moves [movedEntities] to the front of [commitQueue], in order, ahead of the others. */
    @GuardedBy("lock")
    private fun moveToFrontLocked(movedEntities: List<EntityState>) {
        if (movedEntities.isEmpty()) return
        val others = ArrayList(commitQueue)
        commitQueue.clear()
        commitQueue.addAll(movedEntities)
        // LinkedHashSet keeps first insertion order, so re-adding others doesn't duplicate.
        commitQueue.addAll(others)
    }

    /**
     * Schedules the next commit. If the executor rejects it, stops, and returns the reports of the
     * attachment changes that were waiting.
     */
    @GuardedBy("lock")
    private fun scheduleCommitLocked(delayMs: Long): List<Report> =
        try {
            scheduledCommit = executor.schedule(commitTask, delayMs, TimeUnit.MILLISECONDS)
            state = State.SCHEDULED
            emptyList()
        } catch (_: RejectedExecutionException) {
            // The executor shut down with the runtime.
            shutDownLocked()
        }

    /**
     * Drops every uncommitted change, and stops committing. Returns the DROPPED reports of the
     * attachment changes that were waiting. A running commit reports the changes in flight.
     */
    @GuardedBy("lock")
    private fun shutDownLocked(): List<Report> {
        state = State.SHUT_DOWN
        scheduledCommit?.cancel(/* mayInterruptIfRunning= */ false)
        scheduledCommit = null
        val reports = entities.values.mapNotNull { it.attachment.pending?.droppedReport() }
        entities.clear()
        commitQueue.clear()
        return reports
    }

    /** Runs the callbacks of [reports], in order. Called on [executor], without holding [lock]. */
    private fun deliver(reports: List<Report>) {
        for (report in reports) {
            try {
                report.callback.onResult(report.result)
            } catch (t: Throwable) {
                // Reports the failure like an uncaught exception. On Android, the default handler
                // ends the process. If the handler returns, as in tests, the next callbacks run.
                val thread = Thread.currentThread()
                thread.uncaughtExceptionHandler?.uncaughtException(thread, t)
            }
        }
    }

    /** Applies the changes recorded by [edit]. Only called while holding [lock]. */
    private inner class Editor : SceneEditor {
        override fun setTransform(entityHandle: Long, pose: Pose, scale: Vector3) {
            change(entityHandle) { it.transform.set(Transform(pose, scale)) }
        }

        override fun setParent(entityHandle: Long, parentHandle: Long) {
            change(entityHandle) { it.parent.set(parentHandle) }
        }

        override fun setSpaceAttachment(
            entityHandle: Long,
            lease: SpaceLease,
            onResult: CommitCallback,
        ) {
            change(entityHandle) { it.attachment.set(AttachmentChange(lease, onResult)) }
        }

        override fun removeSpaceAttachment(entityHandle: Long, onResult: CommitCallback?) {
            change(entityHandle) { it.attachment.set(AttachmentChange(lease = null, onResult)) }
        }

        override fun destroyEntity(entityHandle: Long) {
            change(entityHandle) {
                // Waits behind the changes made so far, so that it's destroyed after them.
                commitQueue.remove(it)
                it.requestDestruction()
            }
        }

        @GuardedBy("lock")
        private inline fun change(entityHandle: Long, block: (EntityState) -> Unit) {
            require(entityHandle != INVALID_HANDLE) { "Cannot change entity INVALID_HANDLE." }
            val entity = entities.getOrPut(entityHandle) { EntityState(entityHandle) }
            if (entity.isDestroyRequested) return
            block(entity)
            // An entity that was already waiting keeps its place.
            if (entity.isDirty) commitQueue.add(entity) else commitQueue.remove(entity)
        }
    }

    /**
     * Where the committer is in its commit cycle. At most one commit is scheduled at a time: edit()
     * only schedules one from IDLE, and a finished commit schedules the next.
     *
     * ```
     * IDLE      --edit()-------------> SCHEDULED
     * SCHEDULED --commit(), changes--> RUNNING
     * SCHEDULED --commit(), nothing--> IDLE
     * RUNNING   --batch finished-----> SCHEDULED
     * Any state --shutdown()---------> SHUT_DOWN
     * ```
     *
     * The committer also shuts down when the executor rejects a commit, the native instance is
     * destroyed, or a commit throws.
     */
    private enum class State {
        // Nothing to commit, and no commit scheduled.
        IDLE,
        SCHEDULED,
        RUNNING,
        // Changes are dropped, see shutdown().
        SHUT_DOWN,
    }

    /**
     * What committing a [Batch] did. Immutable, so that [commitBatch] can return it without holding
     * [lock].
     */
    private class BatchCommit(
        // Whether the transaction with [Batch.changes] was submitted, or there were no changes.
        val isSubmitted: Boolean,
        // The entities in [Batch.destructionsBefore] and [Batch.destructionsAfter] that were
        // destroyed.
        val destroyed: Set<EntityState>,
        // The entities in [Batch.changes] whose attachment change the runtime rejected.
        val rejectedAttachments: Set<EntityState>,
        // The entities in [Batch.changes] whose attachment wasn't staged because its lease was
        // revoked, so the changes that go with it weren't staged either.
        val revokedAttachments: Set<EntityState>,
    ) {
        /**
         * The result of the attachment change to [entity], or null if the change is to be committed
         * again, since the transaction wasn't submitted.
         */
        fun attachmentResult(entity: EntityState): CommitResult? =
            when {
                entity in revokedAttachments -> CommitResult.DROPPED
                !isSubmitted -> null
                entity in rejectedAttachments -> CommitResult.REJECTED
                else -> CommitResult.COMMITTED
            }
    }

    /** The changes to commit and the native entities to destroy in one commit. */
    private class Batch(
        // The entities to destroy that waited longer than [changes].
        val destructionsBefore: List<EntityState>,
        val changes: List<EntityChanges>,
        // The other entities to destroy.
        val destructionsAfter: List<EntityState>,
    ) {
        /** The DROPPED reports of the attachment changes in [changes]. */
        fun droppedReports(): List<Report> = changes.mapNotNull { it.attachment?.droppedReport() }
    }

    /** The changes to [entity] in a [Batch]. A null change means that component didn't change. */
    private class EntityChanges(
        val entity: EntityState,
        val attachment: AttachmentChange?,
        val parent: Long?,
        val transform: Transform?,
    )

    /** What the committer knows about a native entity. */
    private class EntityState(val entityHandle: Long) {
        val attachment = ComponentState<AttachmentChange>()
        val parent = ComponentState<Long>()
        val transform = ComponentState<Transform>()
        var isDestroyRequested = false

        val hasPendingChanges: Boolean
            get() =
                attachment.pending != null || parent.pending != null || transform.pending != null

        val isDirty: Boolean
            get() = hasPendingChanges || isDestroyRequested

        // Doesn't remove the attachment first: releasing the native entity removes it from the
        // runtime's scene, attachment included.
        fun requestDestruction() {
            isDestroyRequested = true
            dropPendingChanges()
        }

        fun dropPendingChanges() {
            attachment.dropPending()
            parent.dropPending()
            transform.dropPending()
        }
    }

    /**
     * What the committer knows about one component of a native entity: the change being committed,
     * and the change waiting for the next commit.
     */
    private class ComponentState<T : Any> {
        private var inFlight: T? = null

        var pending: T? = null
            private set

        fun set(value: T) {
            pending = value
        }

        fun dropPending() {
            pending = null
        }

        /** Moves the pending change, if any, in flight, and returns its value. */
        fun takePending(): T? {
            val value = pending ?: return null
            inFlight = value
            pending = null
            return value
        }

        /**
         * Finishes committing the change in flight, if any. Unless [isDone], because the
         * transaction wasn't submitted, the change is committed next time, unless a newer change
         * replaced it.
         */
        fun finish(isDone: Boolean) {
            val value = inFlight ?: return
            inFlight = null
            if (!isDone && pending == null) {
                pending = value
            }
        }
    }

    /**
     * A change to the attachment of a native entity: the lease on the `XrSpace` to attach it to, or
     * null to remove its attachment, and the callback to report the result to, if any.
     */
    private class AttachmentChange(val lease: SpaceLease?, val onResult: CommitCallback?) {
        /** The report that this change was dropped, if it has a callback. */
        fun droppedReport(): Report? = onResult?.let { Report(it, CommitResult.DROPPED) }
    }

    /** A [CommitResult] to deliver to a [CommitCallback]. */
    private class Report(val callback: CommitCallback, val result: CommitResult)

    private data class Transform(val pose: Pose, val scale: Vector3)

    internal companion object {
        /**
         * A heuristic, not measured: roughly the time it takes to make the changes of a frame, so
         * that they commit together without adding much latency.
         */
        internal const val COALESCING_DELAY_MS: Long = 2L

        /** About one frame at 90 Hz, like [ROOT_SPACE_POSE_SAMPLE_PERIOD_NANOS]. */
        internal const val MIN_COMMIT_INTERVAL_MS: Long = 11L

        /**
         * How long to wait before trying again when no transaction is free. The wait doubles after
         * each try, up to [MAX_RETRY_DELAY_MS].
         */
        internal const val INITIAL_RETRY_DELAY_MS: Long = 4L

        /**
         * The longest wait between tries, about 6 frames at 90 Hz, so that changes still commit
         * soon after a transaction is free.
         */
        internal const val MAX_RETRY_DELAY_MS: Long = 64L

        /**
         * The OpenXR runtime splits a 4 KiB transaction buffer between its 3 transactions, and the
         * changes to an entity take about 100 bytes of it, so about 13 entities fit. 8 leaves room
         * for changes that take more.
         */
        internal const val MAX_ENTITIES_PER_TRANSACTION: Int = 8
    }
}
