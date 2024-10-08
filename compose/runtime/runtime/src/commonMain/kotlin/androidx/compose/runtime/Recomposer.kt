/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.emptyScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.collection.fastForEach
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.collection.wrapIntoSet
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentSetOf
import androidx.compose.runtime.internal.AtomicReference
import androidx.compose.runtime.internal.SnapshotThreadLocal
import androidx.compose.runtime.internal.logError
import androidx.compose.runtime.internal.trace
import androidx.compose.runtime.snapshots.MutableSnapshot
import androidx.compose.runtime.snapshots.ReaderKind
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotApplyResult
import androidx.compose.runtime.snapshots.StateObjectImpl
import androidx.compose.runtime.snapshots.fastAll
import androidx.compose.runtime.snapshots.fastAny
import androidx.compose.runtime.snapshots.fastFilterIndexed
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.fastGroupBy
import androidx.compose.runtime.snapshots.fastMap
import androidx.compose.runtime.snapshots.fastMapNotNull
import androidx.compose.runtime.tooling.CompositionData
import kotlin.collections.removeFirst as removeFirstKt
import kotlin.collections.removeLast as removeLastKt
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

// TODO: Can we use rootKey for this since all compositions will have an eventual Recomposer parent?
private const val RecomposerCompoundHashKey = 1000

/**
 * Runs [block] with a new, active [Recomposer] applying changes in the calling [CoroutineContext].
 * The [Recomposer] will be [closed][Recomposer.close] after [block] returns.
 * [withRunningRecomposer] will return once the [Recomposer] is [Recomposer.State.ShutDown] and all
 * child jobs launched by [block] have [joined][Job.join].
 */
suspend fun <R> withRunningRecomposer(
    block: suspend CoroutineScope.(recomposer: Recomposer) -> R
): R = coroutineScope {
    val recomposer = Recomposer(coroutineContext)
    // Will be cancelled when recomposerJob cancels
    launch { recomposer.runRecomposeAndApplyChanges() }
    block(recomposer).also {
        recomposer.close()
        recomposer.join()
    }
}

/**
 * Read-only information about a [Recomposer]. Used when code should only monitor the activity of a
 * [Recomposer], and not attempt to alter its state or create new compositions from it.
 */
interface RecomposerInfo {
    /** The current [State] of the [Recomposer]. See each [State] value for its meaning. */
    // TODO: Mirror the currentState/StateFlow API change here once we can safely add
    // default interface methods. https://youtrack.jetbrains.com/issue/KT-47000
    val state: Flow<Recomposer.State>

    /**
     * `true` if the [Recomposer] has been assigned work to do and it is currently performing that
     * work or awaiting an opportunity to do so.
     */
    val hasPendingWork: Boolean

    /**
     * The running count of the number of times the [Recomposer] awoke and applied changes to one or
     * more [Composer]s. This count is unaffected if the composer awakes and recomposed but
     * composition did not produce changes to apply.
     */
    val changeCount: Long
}

/** Read only information about [Recomposer] error state. */
@InternalComposeApi
internal interface RecomposerErrorInfo {
    /** Exception which forced recomposition to halt. */
    val cause: Exception

    /**
     * Whether composition can recover from the error by itself. If the error is not recoverable,
     * recomposer will not react to invalidate calls until state is reloaded.
     */
    val recoverable: Boolean
}

/**
 * The scheduler for performing recomposition and applying updates to one or more [Composition]s.
 */
// RedundantVisibilityModifier suppressed because metalava picks up internal function overrides
// if 'internal' is not explicitly specified - b/171342041
// NotCloseable suppressed because this is Kotlin-only common code; [Auto]Closeable not available.
@Suppress("RedundantVisibilityModifier", "NotCloseable")
@OptIn(InternalComposeApi::class)
class Recomposer(effectCoroutineContext: CoroutineContext) : CompositionContext() {
    /**
     * This is a running count of the number of times the recomposer awoke and applied changes to
     * one or more composers. This count is unaffected if the composer awakes and recomposed but
     * composition did not produce changes to apply.
     */
    var changeCount = 0L
        private set

    private val broadcastFrameClock = BroadcastFrameClock {
        synchronized(stateLock) {
                deriveStateLocked().also {
                    if (_state.value <= State.ShuttingDown)
                        throw CancellationException(
                            "Recomposer shutdown; frame clock awaiter will never resume",
                            closeCause
                        )
                }
            }
            ?.resume(Unit)
    }

    /** Valid operational states of a [Recomposer]. */
    enum class State {
        /**
         * [cancel] was called on the [Recomposer] and all cleanup work has completed. The
         * [Recomposer] is no longer available for use.
         */
        ShutDown,

        /**
         * [cancel] was called on the [Recomposer] and it is no longer available for use. Cleanup
         * work has not yet been fully completed and composition effect coroutines may still be
         * running.
         */
        ShuttingDown,

        /**
         * The [Recomposer] is not tracking invalidations for known composers and it will not
         * recompose them in response to changes. Call [runRecomposeAndApplyChanges] to await and
         * perform work. This is the initial state of a newly constructed [Recomposer].
         */
        Inactive,

        /**
         * The [Recomposer] is [Inactive] but at least one effect associated with a managed
         * composition is awaiting a frame. This frame will not be produced until the [Recomposer]
         * is [running][runRecomposeAndApplyChanges].
         */
        InactivePendingWork,

        /**
         * The [Recomposer] is tracking composition and snapshot invalidations but there is
         * currently no work to do.
         */
        Idle,

        /**
         * The [Recomposer] has been notified of pending work it must perform and is either actively
         * performing it or awaiting the appropriate opportunity to perform it. This work may
         * include invalidated composers that must be recomposed, snapshot state changes that must
         * be presented to known composers to check for invalidated compositions, or coroutines
         * awaiting a frame using the Recomposer's [MonotonicFrameClock].
         */
        PendingWork
    }

    private val stateLock = makeSynchronizedObject()

    // Begin properties guarded by stateLock
    private var runnerJob: Job? = null
    private var closeCause: Throwable? = null
    private val _knownCompositions = mutableListOf<ControlledComposition>()
    private var _knownCompositionsCache: List<ControlledComposition>? = null
    private val knownCompositions
        get() =
            _knownCompositionsCache
                ?: run {
                    val compositions = _knownCompositions
                    val newCache =
                        if (compositions.isEmpty()) emptyList() else ArrayList(compositions)
                    _knownCompositionsCache = newCache
                    newCache
                }

    private var snapshotInvalidations = MutableScatterSet<Any>()
    private val compositionInvalidations = mutableVectorOf<ControlledComposition>()
    private val compositionsAwaitingApply = mutableListOf<ControlledComposition>()
    private val compositionValuesAwaitingInsert = mutableListOf<MovableContentStateReference>()
    private val compositionValuesRemoved =
        mutableMapOf<MovableContent<Any?>, MutableList<MovableContentStateReference>>()
    private val compositionValueStatesAvailable =
        mutableMapOf<MovableContentStateReference, MovableContentState>()
    private var failedCompositions: MutableList<ControlledComposition>? = null
    private var compositionsRemoved: MutableSet<ControlledComposition>? = null
    private var workContinuation: CancellableContinuation<Unit>? = null
    private var concurrentCompositionsOutstanding = 0
    private var isClosed: Boolean = false
    private var errorState: RecomposerErrorState? = null
    private var frameClockPaused: Boolean = false
    // End properties guarded by stateLock

    private val _state = MutableStateFlow(State.Inactive)
    private val pausedScopes = SnapshotThreadLocal<MutableScatterSet<RecomposeScopeImpl>?>()

    /**
     * A [Job] used as a parent of any effects created by this [Recomposer]'s compositions. Its
     * cleanup is used to advance to [State.ShuttingDown] or [State.ShutDown].
     *
     * Initialized after other state above, since it is possible for [Job.invokeOnCompletion] to run
     * synchronously during construction if the [Recomposer] is constructed with a completed or
     * cancelled [Job].
     */
    private val effectJob =
        Job(effectCoroutineContext[Job]).apply {
            invokeOnCompletion { throwable ->
                // Since the running recompose job is operating in a disjoint job if present,
                // kick it out and make sure no new ones start if we have one.
                val cancellation =
                    CancellationException("Recomposer effect job completed", throwable)

                var continuationToResume: CancellableContinuation<Unit>? = null
                synchronized(stateLock) {
                    val runnerJob = runnerJob
                    if (runnerJob != null) {
                        _state.value = State.ShuttingDown
                        // If the recomposer is closed we will let the runnerJob return from
                        // runRecomposeAndApplyChanges normally and consider ourselves shut down
                        // immediately.
                        if (!isClosed) {
                            // This is the job hosting frameContinuation; no need to resume it
                            // otherwise
                            runnerJob.cancel(cancellation)
                        } else if (workContinuation != null) {
                            continuationToResume = workContinuation
                        }
                        workContinuation = null
                        runnerJob.invokeOnCompletion { runnerJobCause ->
                            synchronized(stateLock) {
                                closeCause =
                                    throwable?.apply {
                                        runnerJobCause
                                            ?.takeIf { it !is CancellationException }
                                            ?.let { addSuppressed(it) }
                                    }
                                _state.value = State.ShutDown
                            }
                        }
                    } else {
                        closeCause = cancellation
                        _state.value = State.ShutDown
                    }
                }
                continuationToResume?.resume(Unit)
            }
        }

    /** The [effectCoroutineContext] is derived from the parameter of the same name. */
    override val effectCoroutineContext: CoroutineContext =
        effectCoroutineContext + broadcastFrameClock + effectJob

    internal override val recomposeCoroutineContext: CoroutineContext
        get() = EmptyCoroutineContext

    private val hasBroadcastFrameClockAwaitersLocked: Boolean
        get() = !frameClockPaused && broadcastFrameClock.hasAwaiters

    private val hasBroadcastFrameClockAwaiters: Boolean
        get() = synchronized(stateLock) { hasBroadcastFrameClockAwaitersLocked }

    /**
     * Determine the new value of [_state]. Call only while locked on [stateLock]. If it returns a
     * continuation, that continuation should be resumed after releasing the lock.
     */
    private fun deriveStateLocked(): CancellableContinuation<Unit>? {
        if (_state.value <= State.ShuttingDown) {
            clearKnownCompositionsLocked()
            snapshotInvalidations = MutableScatterSet()
            compositionInvalidations.clear()
            compositionsAwaitingApply.clear()
            compositionValuesAwaitingInsert.clear()
            failedCompositions = null
            workContinuation?.cancel()
            workContinuation = null
            errorState = null
            return null
        }

        val newState =
            when {
                errorState != null -> {
                    State.Inactive
                }
                runnerJob == null -> {
                    snapshotInvalidations = MutableScatterSet()
                    compositionInvalidations.clear()
                    if (hasBroadcastFrameClockAwaitersLocked) State.InactivePendingWork
                    else State.Inactive
                }
                compositionInvalidations.isNotEmpty() ||
                    snapshotInvalidations.isNotEmpty() ||
                    compositionsAwaitingApply.isNotEmpty() ||
                    compositionValuesAwaitingInsert.isNotEmpty() ||
                    concurrentCompositionsOutstanding > 0 ||
                    hasBroadcastFrameClockAwaitersLocked -> State.PendingWork
                else -> State.Idle
            }

        _state.value = newState
        return if (newState == State.PendingWork) {
            workContinuation.also { workContinuation = null }
        } else null
    }

    /** `true` if there is still work to do for an active caller of [runRecomposeAndApplyChanges] */
    private val shouldKeepRecomposing: Boolean
        get() = synchronized(stateLock) { !isClosed } || effectJob.children.any { it.isActive }

    /** The current [State] of this [Recomposer]. See each [State] value for its meaning. */
    @Deprecated("Replaced by currentState as a StateFlow", ReplaceWith("currentState"))
    public val state: Flow<State>
        get() = currentState

    /** The current [State] of this [Recomposer], available synchronously. */
    public val currentState: StateFlow<State>
        get() = _state

    // A separate private object to avoid the temptation of casting a RecomposerInfo
    // to a Recomposer if Recomposer itself were to implement RecomposerInfo.
    private inner class RecomposerInfoImpl : RecomposerInfo {
        override val state: Flow<State>
            get() = this@Recomposer.currentState

        override val hasPendingWork: Boolean
            get() = this@Recomposer.hasPendingWork

        override val changeCount: Long
            get() = this@Recomposer.changeCount

        val currentError: RecomposerErrorInfo?
            get() = synchronized(stateLock) { this@Recomposer.errorState }

        fun invalidateGroupsWithKey(key: Int) {
            val compositions: List<ControlledComposition> =
                synchronized(stateLock) { knownCompositions }
            compositions
                .fastMapNotNull { it as? CompositionImpl }
                .fastForEach { it.invalidateGroupsWithKey(key) }
        }

        fun saveStateAndDisposeForHotReload(): List<HotReloadable> {
            val compositions: List<ControlledComposition> =
                synchronized(stateLock) { knownCompositions }
            return compositions
                .fastMapNotNull { it as? CompositionImpl }
                .fastMap { HotReloadable(it).apply { clearContent() } }
        }

        fun resetErrorState(): RecomposerErrorState? = this@Recomposer.resetErrorState()

        fun retryFailedCompositions() = this@Recomposer.retryFailedCompositions()
    }

    private class HotReloadable(private val composition: CompositionImpl) {
        private var composable: @Composable () -> Unit = composition.composable

        fun clearContent() {
            if (composition.isRoot) {
                composition.setContent {}
            }
        }

        fun resetContent() {
            composition.composable = composable
        }

        fun recompose() {
            if (composition.isRoot) {
                composition.setContent(composable)
            }
        }
    }

    private class RecomposerErrorState(
        override val recoverable: Boolean,
        override val cause: Exception
    ) : RecomposerErrorInfo

    private val recomposerInfo = RecomposerInfoImpl()

    /** Obtain a read-only [RecomposerInfo] for this [Recomposer]. */
    fun asRecomposerInfo(): RecomposerInfo = recomposerInfo

    /**
     * Propagate all invalidations from `snapshotInvalidations` to all the known compositions.
     *
     * @return `true` if the frame has work to do (e.g. [hasFrameWorkLocked])
     */
    private fun recordComposerModifications(): Boolean {
        val changes =
            synchronized(stateLock) {
                if (snapshotInvalidations.isEmpty()) return hasFrameWorkLocked
                snapshotInvalidations.wrapIntoSet().also {
                    snapshotInvalidations = MutableScatterSet()
                }
            }
        val compositions = synchronized(stateLock) { knownCompositions }
        var complete = false
        try {
            run {
                compositions.fastForEach { composition ->
                    composition.recordModificationsOf(changes)

                    // Stop dispatching if the recomposer if we detect the recomposer
                    // is shutdown.
                    if (_state.value <= State.ShuttingDown) return@run
                }
            }
            synchronized(stateLock) { snapshotInvalidations = MutableScatterSet() }
            complete = true
        } finally {
            if (!complete) {
                // If the previous loop was not complete, we have not sent all of theses
                // changes to all the composers so try again after the exception that caused
                // the early exit is handled and we can then retry sending the changes.
                synchronized(stateLock) { snapshotInvalidations.addAll(changes) }
            }
        }
        return synchronized(stateLock) {
            if (deriveStateLocked() != null) {
                error("called outside of runRecomposeAndApplyChanges")
            }
            hasFrameWorkLocked
        }
    }

    private inline fun recordComposerModifications(
        onEachInvalidComposition: (ControlledComposition) -> Unit
    ) {
        val changes =
            synchronized(stateLock) {
                    snapshotInvalidations.also {
                        if (it.isNotEmpty()) snapshotInvalidations = MutableScatterSet()
                    }
                }
                .wrapIntoSet()
        if (changes.isNotEmpty()) {
            knownCompositions.fastForEach { composition ->
                composition.recordModificationsOf(changes)
            }
        }
        compositionInvalidations.forEach(onEachInvalidComposition)
        compositionInvalidations.clear()
        synchronized(stateLock) {
            if (deriveStateLocked() != null) {
                error("called outside of runRecomposeAndApplyChanges")
            }
        }
    }

    private fun registerRunnerJob(callingJob: Job) {
        synchronized(stateLock) {
            closeCause?.let { throw it }
            if (_state.value <= State.ShuttingDown) error("Recomposer shut down")
            if (runnerJob != null) error("Recomposer already running")
            runnerJob = callingJob
            deriveStateLocked()
        }
    }

    /**
     * Await the invalidation of any associated [Composer]s, recompose them, and apply their changes
     * to their associated [Composition]s if recomposition is successful.
     *
     * While [runRecomposeAndApplyChanges] is running, [awaitIdle] will suspend until there are no
     * more invalid composers awaiting recomposition.
     *
     * This method will not return unless the [Recomposer] is [close]d and all effects in managed
     * compositions complete. Unhandled failure exceptions from child coroutines will be thrown by
     * this method.
     */
    suspend fun runRecomposeAndApplyChanges() = recompositionRunner { parentFrameClock ->
        val toRecompose = mutableListOf<ControlledComposition>()
        val toInsert = mutableListOf<MovableContentStateReference>()
        val toApply = mutableListOf<ControlledComposition>()
        val toLateApply = mutableScatterSetOf<ControlledComposition>()
        val toComplete = mutableScatterSetOf<ControlledComposition>()
        val modifiedValues = MutableScatterSet<Any>()
        val modifiedValuesSet = modifiedValues.wrapIntoSet()
        val alreadyComposed = mutableScatterSetOf<ControlledComposition>()

        fun clearRecompositionState() {
            synchronized(stateLock) {
                toRecompose.clear()
                toInsert.clear()

                toApply.fastForEach {
                    it.abandonChanges()
                    recordFailedCompositionLocked(it)
                }
                toApply.clear()

                toLateApply.forEach {
                    it.abandonChanges()
                    recordFailedCompositionLocked(it)
                }
                toLateApply.clear()

                toComplete.forEach { it.changesApplied() }
                toComplete.clear()

                modifiedValues.clear()

                alreadyComposed.forEach {
                    it.abandonChanges()
                    recordFailedCompositionLocked(it)
                }
                alreadyComposed.clear()
            }
        }

        fun fillToInsert() {
            toInsert.clear()
            synchronized(stateLock) {
                compositionValuesAwaitingInsert.fastForEach { toInsert += it }
                compositionValuesAwaitingInsert.clear()
            }
        }

        while (shouldKeepRecomposing) {
            awaitWorkAvailable()

            // Don't await a new frame if we don't have frame-scoped work
            if (!recordComposerModifications()) continue

            // Align work with the next frame to coalesce changes.
            // Note: it is possible to resume from the above with no recompositions pending,
            // instead someone might be awaiting our frame clock dispatch below.
            // We use the cached frame clock from above not just so that we don't locate it
            // each time, but because we've installed the broadcastFrameClock as the scope
            // clock above for user code to locate.
            parentFrameClock.withFrameNanos { frameTime ->
                // Dispatch MonotonicFrameClock frames first; this may produce new
                // composer invalidations that we must handle during the same frame.
                if (hasBroadcastFrameClockAwaiters) {
                    trace("Recomposer:animation") {
                        // Propagate the frame time to anyone who is awaiting from the
                        // recomposer clock.
                        broadcastFrameClock.sendFrame(frameTime)

                        // Ensure any global changes are observed
                        Snapshot.sendApplyNotifications()
                    }
                }

                trace("Recomposer:recompose") {
                    // Drain any composer invalidations from snapshot changes and record
                    // composers to work on
                    recordComposerModifications()
                    synchronized(stateLock) {
                        compositionInvalidations.forEach { toRecompose += it }
                        compositionInvalidations.clear()
                    }

                    // Perform recomposition for any invalidated composers
                    modifiedValues.clear()
                    alreadyComposed.clear()
                    while (toRecompose.isNotEmpty() || toInsert.isNotEmpty()) {
                        try {
                            toRecompose.fastForEach { composition ->
                                performRecompose(composition, modifiedValues)?.let { toApply += it }
                                alreadyComposed.add(composition)
                            }
                        } catch (e: Exception) {
                            processCompositionError(e, recoverable = true)
                            clearRecompositionState()
                            return@withFrameNanos
                        } finally {
                            toRecompose.clear()
                        }

                        // Find any trailing recompositions that need to be composed because
                        // of a value change by a composition. This can happen, for example, if
                        // a CompositionLocal changes in a parent and was read in a child
                        // composition that was otherwise valid.
                        if (modifiedValues.isNotEmpty() || compositionInvalidations.isNotEmpty()) {
                            synchronized(stateLock) {
                                knownCompositions.fastForEach { value ->
                                    if (
                                        value !in alreadyComposed &&
                                            value.observesAnyOf(modifiedValuesSet)
                                    ) {
                                        toRecompose += value
                                    }
                                }

                                // Composable lambda is a special kind of value that is not observed
                                // by the snapshot system, but invalidates composition scope
                                // directly instead.
                                compositionInvalidations.removeIf { value ->
                                    if (value !in alreadyComposed && value !in toRecompose) {
                                        toRecompose += value
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                        }

                        if (toRecompose.isEmpty()) {
                            try {
                                fillToInsert()
                                while (toInsert.isNotEmpty()) {
                                    toLateApply += performInsertValues(toInsert, modifiedValues)
                                    fillToInsert()
                                }
                            } catch (e: Exception) {
                                processCompositionError(e, recoverable = true)
                                clearRecompositionState()
                                return@withFrameNanos
                            }
                        }
                    }

                    if (toApply.isNotEmpty()) {
                        changeCount++

                        // Perform apply changes
                        try {
                            // We could do toComplete += toApply but doing it like below
                            // avoids unnecessary allocations since toApply is a mutable list
                            // toComplete += toApply
                            toApply.fastForEach { composition -> toComplete.add(composition) }
                            toApply.fastForEach { composition -> composition.applyChanges() }
                        } catch (e: Exception) {
                            processCompositionError(e)
                            clearRecompositionState()
                            return@withFrameNanos
                        } finally {
                            toApply.clear()
                        }
                    }

                    if (toLateApply.isNotEmpty()) {
                        try {
                            toComplete += toLateApply
                            toLateApply.forEach { composition -> composition.applyLateChanges() }
                        } catch (e: Exception) {
                            processCompositionError(e)
                            clearRecompositionState()
                            return@withFrameNanos
                        } finally {
                            toLateApply.clear()
                        }
                    }

                    if (toComplete.isNotEmpty()) {
                        try {
                            toComplete.forEach { composition -> composition.changesApplied() }
                        } catch (e: Exception) {
                            processCompositionError(e)
                            clearRecompositionState()
                            return@withFrameNanos
                        } finally {
                            toComplete.clear()
                        }
                    }

                    synchronized(stateLock) { deriveStateLocked() }

                    // Ensure any state objects that were written during apply changes, e.g. nodes
                    // with state-backed properties, get sent apply notifications to invalidate
                    // anything observing the nodes. Call this method instead of
                    // sendApplyNotifications to ensure that objects that were _created_ in this
                    // snapshot are also considered changed after this point.
                    Snapshot.notifyObjectsInitialized()
                    alreadyComposed.clear()
                    modifiedValues.clear()
                    compositionsRemoved = null
                }
            }

            discardUnusedValues()
        }
    }

    private fun processCompositionError(
        e: Exception,
        failedInitialComposition: ControlledComposition? = null,
        recoverable: Boolean = false,
    ) {
        if (_hotReloadEnabled.get() && e !is ComposeRuntimeError) {
            synchronized(stateLock) {
                logError("Error was captured in composition while live edit was enabled.", e)

                compositionsAwaitingApply.clear()
                compositionInvalidations.clear()
                snapshotInvalidations = MutableScatterSet()

                compositionValuesAwaitingInsert.clear()
                compositionValuesRemoved.clear()
                compositionValueStatesAvailable.clear()

                errorState = RecomposerErrorState(recoverable = recoverable, cause = e)

                if (failedInitialComposition != null) {
                    recordFailedCompositionLocked(failedInitialComposition)
                }

                deriveStateLocked()
            }
        } else {
            // withFrameNanos uses `runCatching` to ensure that crashes are not propagated to
            // AndroidUiDispatcher. This means that errors that happen during recomposition might
            // be delayed by a frame and swallowed if composed into inconsistent state caused by
            // the error.
            // Common case is subcomposition: if measure occurs after recomposition has thrown,
            // composeInitial will throw because of corrupted composition while original exception
            // won't be recorded.
            synchronized(stateLock) {
                val errorState = errorState
                if (errorState == null) {
                    // Record exception if current error state is empty.
                    this.errorState = RecomposerErrorState(recoverable = false, e)
                } else {
                    // Re-throw original cause if we recorded it previously.
                    throw errorState.cause
                }
            }

            throw e
        }
    }

    private fun clearKnownCompositionsLocked() {
        _knownCompositions.clear()
        _knownCompositionsCache = emptyList()
    }

    private fun removeKnownCompositionLocked(composition: ControlledComposition) {
        _knownCompositions -= composition
        _knownCompositionsCache = null
    }

    private fun addKnownCompositionLocked(composition: ControlledComposition) {
        _knownCompositions += composition
        _knownCompositionsCache = null
    }

    private fun resetErrorState(): RecomposerErrorState? {
        val errorState =
            synchronized(stateLock) {
                val error = errorState
                if (error != null) {
                    errorState = null
                    deriveStateLocked()
                }
                error
            }
        return errorState
    }

    private fun retryFailedCompositions() {
        val compositionsToRetry =
            synchronized(stateLock) { failedCompositions.also { failedCompositions = null } }
                ?: return
        try {
            while (compositionsToRetry.isNotEmpty()) {
                val composition = compositionsToRetry.removeLastKt()
                if (composition !is CompositionImpl) continue

                composition.invalidateAll()
                composition.setContent(composition.composable)

                if (errorState != null) break
            }
        } finally {
            if (compositionsToRetry.isNotEmpty()) {
                // If we did not complete the last list then add the remaining compositions back
                // into the failedCompositions list
                synchronized(stateLock) {
                    compositionsToRetry.fastForEach { recordFailedCompositionLocked(it) }
                }
            }
        }
    }

    private fun recordFailedCompositionLocked(composition: ControlledComposition) {
        val failedCompositions =
            failedCompositions
                ?: mutableListOf<ControlledComposition>().also { failedCompositions = it }

        if (composition !in failedCompositions) {
            failedCompositions += composition
        }
        removeKnownCompositionLocked(composition)
    }

    /**
     * Await the invalidation of any associated [Composer]s, recompose them, and apply their changes
     * to their associated [Composition]s if recomposition is successful.
     *
     * While [runRecomposeConcurrentlyAndApplyChanges] is running, [awaitIdle] will suspend until
     * there are no more invalid composers awaiting recomposition.
     *
     * Recomposition of invalidated composers will occur in [recomposeCoroutineContext].
     * [recomposeCoroutineContext] must not contain a [Job].
     *
     * This method will not return unless the [Recomposer] is [close]d and all effects in managed
     * compositions complete. Unhandled failure exceptions from child coroutines will be thrown by
     * this method.
     */
    @ExperimentalComposeApi
    suspend fun runRecomposeConcurrentlyAndApplyChanges(
        recomposeCoroutineContext: CoroutineContext
    ) = recompositionRunner { parentFrameClock ->
        requirePrecondition(recomposeCoroutineContext[Job] == null) {
            "recomposeCoroutineContext may not contain a Job; found " +
                recomposeCoroutineContext[Job]
        }
        val recomposeCoroutineScope =
            CoroutineScope(coroutineContext + recomposeCoroutineContext + Job(coroutineContext.job))
        val frameSignal = ProduceFrameSignal()
        val frameLoop = launch { runFrameLoop(parentFrameClock, frameSignal) }
        while (shouldKeepRecomposing) {
            awaitWorkAvailable()

            // Don't await a new frame if we don't have frame-scoped work
            recordComposerModifications { composition ->
                synchronized(stateLock) { concurrentCompositionsOutstanding++ }
                recomposeCoroutineScope.launch(composition.recomposeCoroutineContext) {
                    val changedComposition = performRecompose(composition, null)
                    synchronized(stateLock) {
                            changedComposition?.let { compositionsAwaitingApply += it }
                            concurrentCompositionsOutstanding--
                            deriveStateLocked()
                        }
                        ?.resume(Unit)
                }
            }
            synchronized(stateLock) {
                    if (hasConcurrentFrameWorkLocked) frameSignal.requestFrameLocked() else null
                }
                ?.resume(Unit)
        }
        recomposeCoroutineScope.coroutineContext.job.cancelAndJoin()
        frameLoop.cancelAndJoin()
    }

    private suspend fun runFrameLoop(
        parentFrameClock: MonotonicFrameClock,
        frameSignal: ProduceFrameSignal
    ) {
        val toRecompose = mutableListOf<ControlledComposition>()
        val toApply = mutableListOf<ControlledComposition>()
        while (true) {
            frameSignal.awaitFrameRequest(stateLock)
            // Align applying changes to the frame.
            // Note: it is possible to resume from the above with no recompositions pending,
            // instead someone might be awaiting our frame clock dispatch below.
            // We use the cached frame clock from above not just so that we don't locate it
            // each time, but because we've installed the broadcastFrameClock as the scope
            // clock above for user code to locate.
            parentFrameClock.withFrameNanos { frameTime ->
                // Dispatch MonotonicFrameClock frames first; this may produce new
                // composer invalidations that we must handle during the same frame.
                if (hasBroadcastFrameClockAwaiters) {
                    trace("Recomposer:animation") {
                        // Propagate the frame time to anyone who is awaiting from the
                        // recomposer clock.
                        broadcastFrameClock.sendFrame(frameTime)

                        // Ensure any global changes are observed
                        Snapshot.sendApplyNotifications()
                    }
                }

                trace("Recomposer:recompose") {
                    // Drain any composer invalidations from snapshot changes and record
                    // composers to work on.
                    // We'll do these synchronously to make the current frame.
                    recordComposerModifications()
                    synchronized(stateLock) {
                        compositionsAwaitingApply.fastForEach { toApply += it }
                        compositionsAwaitingApply.clear()
                        compositionInvalidations.forEach { toRecompose += it }
                        compositionInvalidations.clear()
                        frameSignal.takeFrameRequestLocked()
                    }

                    // Perform recomposition for any invalidated composers
                    val modifiedValues = MutableScatterSet<Any>()
                    try {
                        toRecompose.fastForEach { composer ->
                            performRecompose(composer, modifiedValues)?.let { toApply += it }
                        }
                    } finally {
                        toRecompose.clear()
                    }

                    // Perform any value inserts

                    if (toApply.isNotEmpty()) changeCount++

                    // Perform apply changes
                    try {
                        toApply.fastForEach { composition -> composition.applyChanges() }
                    } finally {
                        toApply.clear()
                    }

                    synchronized(stateLock) { deriveStateLocked() }
                }
            }
        }
    }

    private val hasSchedulingWork: Boolean
        get() =
            synchronized(stateLock) {
                snapshotInvalidations.isNotEmpty() ||
                    compositionInvalidations.isNotEmpty() ||
                    hasBroadcastFrameClockAwaitersLocked
            }

    private suspend fun awaitWorkAvailable() {
        if (!hasSchedulingWork) {
            // NOTE: Do not remove the `<Unit>` from the next line even if the IDE reports it as
            // redundant. Removing this causes the Kotlin compiler to crash without reporting
            // an error message
            suspendCancellableCoroutine<Unit> { co ->
                synchronized(stateLock) {
                        if (hasSchedulingWork) {
                            co
                        } else {
                            workContinuation = co
                            null
                        }
                    }
                    ?.resume(Unit)
            }
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    private suspend fun recompositionRunner(
        block: suspend CoroutineScope.(parentFrameClock: MonotonicFrameClock) -> Unit
    ) {
        val parentFrameClock = coroutineContext.monotonicFrameClock
        withContext(broadcastFrameClock) {
            // Enforce mutual exclusion of callers; register self as current runner
            val callingJob = coroutineContext.job
            registerRunnerJob(callingJob)

            // Observe snapshot changes and propagate them to known composers only from
            // this caller's dispatcher, never working with the same composer in parallel.
            // unregisterApplyObserver is called as part of the big finally below
            val unregisterApplyObserver =
                Snapshot.registerApplyObserver { changed, _ ->
                    synchronized(stateLock) {
                            if (_state.value >= State.Idle) {
                                val snapshotInvalidations = snapshotInvalidations
                                changed.fastForEach {
                                    if (
                                        it is StateObjectImpl &&
                                            !it.isReadIn(ReaderKind.Composition)
                                    ) {
                                        // continue if we know that state is never read in
                                        // composition
                                        return@fastForEach
                                    }
                                    snapshotInvalidations.add(it)
                                }
                                deriveStateLocked()
                            } else null
                        }
                        ?.resume(Unit)
                }

            addRunning(recomposerInfo)

            try {
                // Invalidate all registered composers when we start since we weren't observing
                // snapshot changes on their behalf. Assume anything could have changed.
                synchronized(stateLock) { knownCompositions }.fastForEach { it.invalidateAll() }

                coroutineScope { block(parentFrameClock) }
            } finally {
                unregisterApplyObserver.dispose()
                synchronized(stateLock) {
                    if (runnerJob === callingJob) {
                        runnerJob = null
                    }
                    deriveStateLocked()
                }
                removeRunning(recomposerInfo)
            }
        }
    }

    /**
     * Permanently shut down this [Recomposer] for future use. [currentState] will immediately
     * reflect [State.ShuttingDown] (or a lower state) before this call returns. All ongoing
     * recompositions will stop, new composer invalidations with this [Recomposer] at the root will
     * no longer occur, and any [LaunchedEffect]s currently running in compositions managed by this
     * [Recomposer] will be cancelled. Any [rememberCoroutineScope] scopes from compositions managed
     * by this [Recomposer] will also be cancelled. See [join] to await the completion of all of
     * these outstanding tasks.
     */
    fun cancel() {
        // Move to State.ShuttingDown immediately rather than waiting for effectJob to join
        // if we're cancelling to shut down the Recomposer. This permits other client code
        // to use `state.first { it < State.Idle }` or similar to reliably and immediately detect
        // that the recomposer can no longer be used.
        // It looks like a CAS loop would be more appropriate here, but other occurrences
        // of taking stateLock assume that the state cannot change without holding it.
        synchronized(stateLock) {
            if (_state.value >= State.Idle) {
                _state.value = State.ShuttingDown
            }
        }
        effectJob.cancel()
    }

    /**
     * Close this [Recomposer]. Once all effects launched by managed compositions complete, any
     * active call to [runRecomposeAndApplyChanges] will return normally and this [Recomposer] will
     * be [State.ShutDown]. See [join] to await the completion of all of these outstanding tasks.
     */
    fun close() {
        if (effectJob.complete()) {
            synchronized(stateLock) { isClosed = true }
        }
    }

    /** Await the completion of a [cancel] operation. */
    suspend fun join() {
        currentState.first { it == State.ShutDown }
    }

    internal override fun composeInitial(
        composition: ControlledComposition,
        content: @Composable () -> Unit
    ) {
        val composerWasComposing = composition.isComposing
        try {
            composing(composition, null) { composition.composeContent(content) }
        } catch (e: Exception) {
            processCompositionError(e, composition, recoverable = true)
            return
        }

        // TODO(b/143755743)
        if (!composerWasComposing) {
            Snapshot.notifyObjectsInitialized()
        }

        synchronized(stateLock) {
            if (_state.value > State.ShuttingDown) {
                if (composition !in knownCompositions) {
                    addKnownCompositionLocked(composition)
                }
            }
        }

        try {
            performInitialMovableContentInserts(composition)
        } catch (e: Exception) {
            processCompositionError(e, composition, recoverable = true)
            return
        }

        try {
            composition.applyChanges()
            composition.applyLateChanges()
        } catch (e: Exception) {
            processCompositionError(e)
            return
        }

        if (!composerWasComposing) {
            // Ensure that any state objects created during applyChanges are seen as changed
            // if modified after this call.
            Snapshot.notifyObjectsInitialized()
        }
    }

    internal override fun composeInitialPaused(
        composition: ControlledComposition,
        shouldPause: () -> Boolean,
        content: @Composable () -> Unit
    ): ScatterSet<RecomposeScopeImpl> {
        return try {
            composition.pausable(shouldPause) {
                composeInitial(composition, content)
                pausedScopes.get() ?: emptyScatterSet()
            }
        } finally {
            pausedScopes.set(null)
        }
    }

    internal override fun recomposePaused(
        composition: ControlledComposition,
        shouldPause: () -> Boolean,
        invalidScopes: ScatterSet<RecomposeScopeImpl>
    ): ScatterSet<RecomposeScopeImpl> {
        return try {
            recordComposerModifications()
            composition.recordModificationsOf(invalidScopes.wrapIntoSet())
            composition.pausable(shouldPause) {
                val needsApply = performRecompose(composition, null)
                if (needsApply != null) {
                    performInitialMovableContentInserts(composition)
                    needsApply.applyChanges()
                    needsApply.applyLateChanges()
                }
                pausedScopes.get() ?: emptyScatterSet()
            }
        } finally {
            pausedScopes.set(null)
        }
    }

    override fun reportPausedScope(scope: RecomposeScopeImpl) {
        val scopes =
            pausedScopes.get()
                ?: run {
                    val newScopes = mutableScatterSetOf<RecomposeScopeImpl>()
                    pausedScopes.set(newScopes)
                    newScopes
                }
        scopes.add(scope)
    }

    private fun performInitialMovableContentInserts(composition: ControlledComposition) {
        synchronized(stateLock) {
            if (!compositionValuesAwaitingInsert.fastAny { it.composition == composition }) return
        }
        val toInsert = mutableListOf<MovableContentStateReference>()
        fun fillToInsert() {
            toInsert.clear()
            synchronized(stateLock) {
                val iterator = compositionValuesAwaitingInsert.iterator()
                while (iterator.hasNext()) {
                    val value = iterator.next()
                    if (value.composition == composition) {
                        toInsert.add(value)
                        iterator.remove()
                    }
                }
            }
        }
        fillToInsert()
        while (toInsert.isNotEmpty()) {
            performInsertValues(toInsert, null)
            fillToInsert()
        }
    }

    private fun performRecompose(
        composition: ControlledComposition,
        modifiedValues: MutableScatterSet<Any>?
    ): ControlledComposition? {
        if (
            composition.isComposing ||
                composition.isDisposed ||
                compositionsRemoved?.contains(composition) == true
        )
            return null

        return if (
            composing(composition, modifiedValues) {
                if (modifiedValues?.isNotEmpty() == true) {
                    // Record write performed by a previous composition as if they happened during
                    // composition.
                    composition.prepareCompose {
                        modifiedValues.forEach { composition.recordWriteOf(it) }
                    }
                }
                composition.recompose()
            }
        )
            composition
        else null
    }

    private fun performInsertValues(
        references: List<MovableContentStateReference>,
        modifiedValues: MutableScatterSet<Any>?
    ): List<ControlledComposition> {
        val tasks = references.fastGroupBy { it.composition }
        for ((composition, refs) in tasks) {
            runtimeCheck(!composition.isComposing)
            composing(composition, modifiedValues) {
                // Map insert movable content to movable content states that have been released
                // during `performRecompose`.
                val pairs =
                    synchronized(stateLock) {
                        refs.fastMap { reference ->
                            reference to
                                compositionValuesRemoved.removeLastMultiValue(reference.content)
                        }
                    }

                // Avoid mixing creating new content with moving content as the moved content
                // may release content when it is moved as it is recomposed when move.
                val toInsert =
                    if (
                        pairs.fastAll { it.second == null } || pairs.fastAll { it.second != null }
                    ) {
                        pairs
                    } else {
                        // Return the content not moving to the awaiting list. These will come back
                        // here in the next iteration of the caller's loop and either have content
                        // to move or by still needing to create the content.
                        val toReturn =
                            pairs.fastMapNotNull { item ->
                                if (item.second == null) item.first else null
                            }
                        synchronized(stateLock) { compositionValuesAwaitingInsert += toReturn }

                        // Only insert the moving content this time
                        pairs.fastFilterIndexed { _, item -> item.second != null }
                    }

                // toInsert is guaranteed to be not empty as,
                // 1) refs is guaranteed to be not empty as a condition of groupBy
                // 2) pairs is guaranteed to be not empty as it is a map of refs
                // 3) toInsert is guaranteed to not be empty because the toReturn and toInsert
                //    lists have at least one item by the condition of the guard in the if
                //    expression. If one would be empty the condition is true and the filter is not
                //    performed. As both have at least one item toInsert has at least one item. If
                //    the filter is not performed the list is pairs which has at least one item.
                composition.insertMovableContent(toInsert)
            }
        }
        return tasks.keys.toList()
    }

    private fun discardUnusedValues() {
        val unusedValues =
            synchronized(stateLock) {
                if (compositionValuesRemoved.isNotEmpty()) {
                    val references = compositionValuesRemoved.values.flatten()
                    compositionValuesRemoved.clear()
                    val unusedValues =
                        references.fastMap { it to compositionValueStatesAvailable[it] }
                    compositionValueStatesAvailable.clear()
                    unusedValues
                } else emptyList()
            }
        unusedValues.fastForEach { (reference, state) ->
            if (state != null) {
                reference.composition.disposeUnusedMovableContent(state)
            }
        }
    }

    private fun readObserverOf(composition: ControlledComposition): (Any) -> Unit {
        return { value -> composition.recordReadOf(value) }
    }

    private fun writeObserverOf(
        composition: ControlledComposition,
        modifiedValues: MutableScatterSet<Any>?
    ): (Any) -> Unit {
        return { value ->
            composition.recordWriteOf(value)
            modifiedValues?.add(value)
        }
    }

    private inline fun <T> composing(
        composition: ControlledComposition,
        modifiedValues: MutableScatterSet<Any>?,
        block: () -> T
    ): T {
        val snapshot =
            Snapshot.takeMutableSnapshot(
                readObserverOf(composition),
                writeObserverOf(composition, modifiedValues)
            )
        try {
            return snapshot.enter(block)
        } finally {
            applyAndCheck(snapshot)
        }
    }

    private fun applyAndCheck(snapshot: MutableSnapshot) {
        try {
            val applyResult = snapshot.apply()
            if (applyResult is SnapshotApplyResult.Failure) {
                error(
                    "Unsupported concurrent change during composition. A state object was " +
                        "modified by composition as well as being modified outside composition."
                )
            }
        } finally {
            snapshot.dispose()
        }
    }

    /**
     * `true` if this [Recomposer] has any pending work scheduled, regardless of whether or not it
     * is currently [running][runRecomposeAndApplyChanges].
     */
    val hasPendingWork: Boolean
        get() =
            synchronized(stateLock) {
                snapshotInvalidations.isNotEmpty() ||
                    compositionInvalidations.isNotEmpty() ||
                    concurrentCompositionsOutstanding > 0 ||
                    compositionsAwaitingApply.isNotEmpty() ||
                    hasBroadcastFrameClockAwaitersLocked
            }

    private val hasFrameWorkLocked: Boolean
        get() = compositionInvalidations.isNotEmpty() || hasBroadcastFrameClockAwaitersLocked

    private val hasConcurrentFrameWorkLocked: Boolean
        get() = compositionsAwaitingApply.isNotEmpty() || hasBroadcastFrameClockAwaitersLocked

    /**
     * Suspends until the currently pending recomposition frame is complete. Any recomposition for
     * this recomposer triggered by actions before this call begins will be complete and applied (if
     * recomposition was successful) when this call returns.
     *
     * If [runRecomposeAndApplyChanges] is not currently running the [Recomposer] is considered idle
     * and this method will not suspend.
     */
    suspend fun awaitIdle() {
        currentState.takeWhile { it > State.Idle }.collect()
    }

    /**
     * Pause broadcasting the frame clock while recomposing. This effectively pauses animations, or
     * any other use of the [withFrameNanos], while the frame clock is paused.
     *
     * [pauseCompositionFrameClock] should be called when the recomposer is not being displayed for
     * some reason such as not being the current activity in Android, for example.
     *
     * Calls to [pauseCompositionFrameClock] are thread-safe and idempotent (calling it when the
     * frame clock is already paused is a no-op).
     */
    fun pauseCompositionFrameClock() {
        synchronized(stateLock) { frameClockPaused = true }
    }

    /**
     * Resume broadcasting the frame clock after is has been paused. Pending calls to
     * [withFrameNanos] will start receiving frame clock broadcasts at the beginning of the frame
     * and a frame will be requested if there are pending calls to [withFrameNanos] if a frame has
     * not already been scheduled.
     *
     * Calls to [resumeCompositionFrameClock] are thread-safe and idempotent (calling it when the
     * frame clock is running is a no-op).
     */
    fun resumeCompositionFrameClock() {
        synchronized(stateLock) {
                if (frameClockPaused) {
                    frameClockPaused = false
                    deriveStateLocked()
                } else null
            }
            ?.resume(Unit)
    }

    // Recomposer always starts with a constant compound hash
    internal override val compoundHashKey: Int
        get() = RecomposerCompoundHashKey

    internal override val collectingCallByInformation: Boolean
        get() = _hotReloadEnabled.get()

    // Collecting parameter happens at the level of a composer; starts as false
    internal override val collectingParameterInformation: Boolean
        get() = false

    internal override val collectingSourceInformation: Boolean
        get() = false

    internal override fun recordInspectionTable(table: MutableSet<CompositionData>) {
        // TODO: The root recomposer might be a better place to set up inspection
        // than the current configuration with an CompositionLocal
    }

    internal override fun registerComposition(composition: ControlledComposition) {
        // Do nothing.
    }

    internal override fun unregisterComposition(composition: ControlledComposition) {
        synchronized(stateLock) {
            removeKnownCompositionLocked(composition)
            compositionInvalidations -= composition
            compositionsAwaitingApply -= composition
        }
    }

    internal override fun invalidate(composition: ControlledComposition) {
        synchronized(stateLock) {
                if (composition !in compositionInvalidations) {
                    compositionInvalidations += composition
                    deriveStateLocked()
                } else null
            }
            ?.resume(Unit)
    }

    internal override fun invalidateScope(scope: RecomposeScopeImpl) {
        synchronized(stateLock) {
                snapshotInvalidations.add(scope)
                deriveStateLocked()
            }
            ?.resume(Unit)
    }

    internal override fun insertMovableContent(reference: MovableContentStateReference) {
        synchronized(stateLock) {
                compositionValuesAwaitingInsert += reference
                deriveStateLocked()
            }
            ?.resume(Unit)
    }

    internal override fun deletedMovableContent(reference: MovableContentStateReference) {
        synchronized(stateLock) {
            compositionValuesRemoved.addMultiValue(reference.content, reference)
        }
    }

    internal override fun movableContentStateReleased(
        reference: MovableContentStateReference,
        data: MovableContentState
    ) {
        synchronized(stateLock) { compositionValueStatesAvailable[reference] = data }
    }

    internal override fun reportRemovedComposition(composition: ControlledComposition) {
        synchronized(stateLock) {
            val compositionsRemoved =
                compositionsRemoved
                    ?: mutableSetOf<ControlledComposition>().also { compositionsRemoved = it }
            compositionsRemoved.add(composition)
        }
    }

    override fun movableContentStateResolve(
        reference: MovableContentStateReference
    ): MovableContentState? =
        synchronized(stateLock) { compositionValueStatesAvailable.remove(reference) }

    /**
     * hack: the companion object is thread local in Kotlin/Native to avoid freezing
     * [_runningRecomposers] with the current memory model. As a side effect, recomposers are now
     * forced to be single threaded in Kotlin/Native targets.
     *
     * This annotation WILL BE REMOVED with the new memory model of Kotlin/Native.
     */
    @Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
    @kotlin.native.concurrent.ThreadLocal
    companion object {

        private val _runningRecomposers = MutableStateFlow(persistentSetOf<RecomposerInfoImpl>())

        private val _hotReloadEnabled = AtomicReference(false)

        /**
         * An observable [Set] of [RecomposerInfo]s for currently
         * [running][runRecomposeAndApplyChanges] [Recomposer]s. Emitted sets are immutable.
         */
        val runningRecomposers: StateFlow<Set<RecomposerInfo>>
            get() = _runningRecomposers

        internal fun setHotReloadEnabled(value: Boolean) {
            _hotReloadEnabled.set(value)
        }

        private fun addRunning(info: RecomposerInfoImpl) {
            while (true) {
                val old = _runningRecomposers.value
                val new = old.add(info)
                if (old === new || _runningRecomposers.compareAndSet(old, new)) break
            }
        }

        private fun removeRunning(info: RecomposerInfoImpl) {
            while (true) {
                val old = _runningRecomposers.value
                val new = old.remove(info)
                if (old === new || _runningRecomposers.compareAndSet(old, new)) break
            }
        }

        internal fun saveStateAndDisposeForHotReload(): Any {
            // NOTE: when we move composition/recomposition onto multiple threads, we will want
            // to ensure that we pause recompositions before this call.
            _hotReloadEnabled.set(true)
            return _runningRecomposers.value.flatMap { it.saveStateAndDisposeForHotReload() }
        }

        internal fun loadStateAndComposeForHotReload(token: Any) {
            // NOTE: when we move composition/recomposition onto multiple threads, we will want
            // to ensure that we pause recompositions before this call.
            _hotReloadEnabled.set(true)

            _runningRecomposers.value.forEach { it.resetErrorState() }

            @Suppress("UNCHECKED_CAST") val holders = token as List<HotReloadable>
            holders.fastForEach { it.resetContent() }
            holders.fastForEach { it.recompose() }

            _runningRecomposers.value.forEach { it.retryFailedCompositions() }
        }

        internal fun invalidateGroupsWithKey(key: Int) {
            _hotReloadEnabled.set(true)
            _runningRecomposers.value.forEach {
                if (it.currentError?.recoverable == false) {
                    return@forEach
                }

                it.resetErrorState()

                it.invalidateGroupsWithKey(key)

                it.retryFailedCompositions()
            }
        }

        internal fun getCurrentErrors(): List<RecomposerErrorInfo> =
            _runningRecomposers.value.mapNotNull { it.currentError }

        internal fun clearErrors() {
            _runningRecomposers.value.mapNotNull { it.resetErrorState() }
        }
    }
}

/** Sentinel used by [ProduceFrameSignal] */
private val ProduceAnotherFrame = Any()
private val FramePending = Any()

/**
 * Multiple producer, single consumer conflated signal that tells concurrent composition when it
 * should try to produce another frame. This class is intended to be used along with a lock shared
 * between producers and consumer.
 */
private class ProduceFrameSignal {
    private var pendingFrameContinuation: Any? = null

    /**
     * Suspend until a frame is requested. After this method returns the signal is in a
     * [FramePending] state which must be acknowledged by a call to [takeFrameRequestLocked] once
     * all data that will be used to produce the frame has been claimed.
     */
    suspend fun awaitFrameRequest(lock: SynchronizedObject) {
        synchronized(lock) {
            if (pendingFrameContinuation === ProduceAnotherFrame) {
                pendingFrameContinuation = FramePending
                return
            }
        }
        suspendCancellableCoroutine<Unit> { co ->
            synchronized(lock) {
                    if (pendingFrameContinuation === ProduceAnotherFrame) {
                        pendingFrameContinuation = FramePending
                        co
                    } else {
                        pendingFrameContinuation = co
                        null
                    }
                }
                ?.resume(Unit)
        }
    }

    /**
     * Signal from the frame request consumer that the frame is beginning with data that was
     * available up until this point. (Synchronizing access to that data is up to the caller.)
     */
    fun takeFrameRequestLocked() {
        checkPrecondition(pendingFrameContinuation === FramePending) { "frame not pending" }
        pendingFrameContinuation = null
    }

    fun requestFrameLocked(): Continuation<Unit>? =
        when (val co = pendingFrameContinuation) {
            is Continuation<*> -> {
                pendingFrameContinuation = FramePending
                @Suppress("UNCHECKED_CAST")
                co as Continuation<Unit>
            }
            ProduceAnotherFrame,
            FramePending -> null
            null -> {
                pendingFrameContinuation = ProduceAnotherFrame
                null
            }
            else -> error("invalid pendingFrameContinuation $co")
        }
}

// Allow treating a mutable map of shape MutableMap<K, MutableMap<V>> as a multi-value map
internal fun <K, V> MutableMap<K, MutableList<V>>.addMultiValue(key: K, value: V) =
    getOrPut(key) { mutableListOf() }.add(value)

internal fun <K, V> MutableMap<K, MutableList<V>>.removeLastMultiValue(key: K): V? =
    get(key)?.let { list -> list.removeFirstKt().also { if (list.isEmpty()) remove(key) } }
