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
 * * **Threading:** This class is thread-safe. Commits run one at a time on [executor].
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
         * Destroys native entity [entityHandle] after the changes made before, and drops its
         * uncommitted changes. Ignores any later change to it.
         */
        fun destroyEntity(entityHandle: Long)
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
     * different transactions. Does nothing after [shutdown].
     *
     * [changes] runs while holding the committer's lock, so it must only call [SceneEditor]
     * methods, and must not keep the [SceneEditor]. If it throws, the changes it made before stay
     * recorded.
     *
     * @throws IllegalArgumentException if [changes] passes [INVALID_HANDLE] as an entity.
     */
    fun edit(changes: SceneEditor.() -> Unit) {
        synchronized(lock) {
            if (state == State.SHUT_DOWN) return
            try {
                editor.changes()
            } finally {
                // If a commit is scheduled or running, it will pick these changes up.
                if (state == State.IDLE && commitQueue.isNotEmpty()) {
                    scheduleCommitLocked(coalescingDelayMs)
                }
            }
        }
    }

    /**
     * Drops every uncommitted change, and stops committing.
     *
     * Destroying the native instance afterwards releases the native entities.
     */
    fun shutdown() {
        synchronized(lock) { shutDownLocked() }
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
                synchronized(lock) { shutDownLocked() }
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
                // Rethrow so callers that run tasks synchronously, such as tests, see the failure.
                throw t
            }
        synchronized(lock) {
            if (batchCommit == null) {
                // The native instance was destroyed, with every native entity.
                shutDownLocked()
            } else {
                finishBatchLocked(batch, batchCommit)
            }
        }
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
        // Destroying a native entity detaches it in a transaction of its own, so if one fails, no
        // transaction is likely free for the next ones either.
        fun destroy(entities: List<EntityState>) {
            for (entity in entities) {
                if (!nativeWrapper.destroySceneEntity(entity.entityHandle)) return
                destroyed += entity
            }
        }
        fun notSubmitted() = BatchCommit(isSubmitted = false, destroyed = destroyed)

        // Commits the changes even if a destruction failed, so that one that keeps failing doesn't
        // hold them back. If no transaction is free, they can't be committed either.
        destroy(batch.destructionsBefore)

        if (batch.changes.isNotEmpty()) {
            val transactionHandle = nativeWrapper.createSceneTransaction()
            if (transactionHandle == INVALID_HANDLE) return notSubmitted()
            batch.changes.forEach { stage(transactionHandle, it) }
            // A transaction only ends once it's submitted, even with nothing staged in it.
            if (!nativeWrapper.submitSceneTransaction(transactionHandle)) return notSubmitted()
        }

        destroy(batch.destructionsAfter)
        return BatchCommit(isSubmitted = true, destroyed = destroyed)
    }

    /**
     * Stages [changes] in transaction [transactionHandle]. Stops at the first change that the
     * runtime rejects, so that it doesn't apply the changes that depend on it: a transform is
     * relative to the parent.
     */
    private fun stage(transactionHandle: Long, changes: EntityChanges) {
        val entityHandle = changes.entity.entityHandle
        // TODO: b/537445115 - Record a warning event when the runtime rejects a change.
        val isParentStaged =
            changes.parent == null ||
                nativeWrapper.setTransactionParent(transactionHandle, entityHandle, changes.parent)
        if (isParentStaged && changes.transform != null) {
            nativeWrapper.setTransactionTransform(
                transactionHandle,
                entityHandle,
                changes.transform.pose,
                changes.transform.scale,
            )
        }
    }

    @GuardedBy("lock")
    private fun finishBatchLocked(batch: Batch, batchCommit: BatchCommit) {
        // shutdown() ran during the commit, and dropped every change.
        if (state == State.SHUT_DOWN) return
        // The entities whose changes weren't submitted, which have waited the longest.
        val unsubmitted = ArrayList<EntityState>()
        for (changes in batch.changes) {
            val entity = changes.entity
            entity.parent.finish(batchCommit.isSubmitted)
            entity.transform.finish(batchCommit.isSubmitted)
            if (entity.isDestroyRequested) {
                // destroyEntity() ran during the commit, so drop the changes that weren't
                // committed.
                entity.parent.dropPending()
                entity.transform.dropPending()
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
        scheduleCommitLocked(if (shouldBackOff) retryDelayMs else minCommitIntervalMs)
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

    @GuardedBy("lock")
    private fun scheduleCommitLocked(delayMs: Long) {
        try {
            scheduledCommit = executor.schedule(commitTask, delayMs, TimeUnit.MILLISECONDS)
            state = State.SCHEDULED
        } catch (_: RejectedExecutionException) {
            // The executor shut down with the runtime.
            shutDownLocked()
        }
    }

    @GuardedBy("lock")
    private fun shutDownLocked() {
        state = State.SHUT_DOWN
        scheduledCommit?.cancel(/* mayInterruptIfRunning= */ false)
        scheduledCommit = null
        entities.clear()
        commitQueue.clear()
    }

    /** Applies the changes recorded by [edit]. Only called while holding [lock]. */
    private inner class Editor : SceneEditor {
        override fun setTransform(entityHandle: Long, pose: Pose, scale: Vector3) {
            change(entityHandle) { it.transform.set(Transform(pose, scale)) }
        }

        override fun setParent(entityHandle: Long, parentHandle: Long) {
            change(entityHandle) { it.parent.set(parentHandle) }
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
    )

    /** The changes to commit and the native entities to destroy in one commit. */
    private class Batch(
        // The entities to destroy that waited longer than [changes].
        val destructionsBefore: List<EntityState>,
        val changes: List<EntityChanges>,
        // The other entities to destroy.
        val destructionsAfter: List<EntityState>,
    )

    /** The changes to [entity] in a [Batch]. A null change means that component didn't change. */
    private class EntityChanges(
        val entity: EntityState,
        val parent: Long?,
        val transform: Transform?,
    )

    /** What the committer knows about a native entity. */
    private class EntityState(val entityHandle: Long) {
        val parent = ComponentState<Long>()
        val transform = ComponentState<Transform>()
        var isDestroyRequested = false

        val hasPendingChanges: Boolean
            get() = parent.pending != null || transform.pending != null

        val isDirty: Boolean
            get() = hasPendingChanges || isDestroyRequested

        fun requestDestruction() {
            isDestroyRequested = true
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
         * Finishes committing the change in flight, if any. If the transaction wasn't submitted,
         * the change commits next time, unless a newer change replaced it.
         */
        fun finish(isSubmitted: Boolean) {
            val value = inFlight ?: return
            inFlight = null
            if (!isSubmitted && pending == null) {
                pending = value
            }
        }
    }

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
