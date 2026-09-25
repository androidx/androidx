/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalRemoteCreationComposeApi::class,
)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.VisibleForTesting
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteComposeApplier
import androidx.compose.remote.creation.compose.layout.RemoteRootNode
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.trace
import androidx.core.graphics.createBitmap
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tracing.traceAsync
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * Capture a single RemoteCompose document from the specified [content] Composable by rendering it
 * once inside a virtual display.
 *
 * This is a suspending function that performs the composition and rendering, returning a
 * [CapturedDocument] which contains the serialized bytes and metadata.
 *
 * @param context The Android [Context] to use.
 * @param creationDisplayInfo Details about the virtual display to capture for (size, density,
 *   etc.). Defaults to display metrics derived from [context].
 * @param remoteDensity The logical screen density and font scale to use for unit conversions.
 *   Defaults to density derived from [creationDisplayInfo]. Note: If passing custom values, they
 *   should typically match the density and font scale specified in [creationDisplayInfo] to avoid
 *   layout scaling discrepancies.
 * @param layoutDirection The layout direction (LTR or RTL) to use. Defaults to the system layout
 *   direction.
 * @param clock The clock used for the composition timeline. Defaults to [RemoteClock.SYSTEM].
 * @param profile The writing profile that determines supported operations. Defaults to
 *   [RcPlatformProfiles.ANDROIDX].
 * @param writerEvents Callback to handle non-serializable events (e.g. pending intents).
 * @param content The Composable content to render and capture.
 * @return A [CapturedDocument] containing the serialized document bytes.
 */
public suspend fun captureSingleRemoteDocument(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo = createCreationDisplayInfo(context),
    remoteDensity: RemoteDensity =
        RemoteDensity(
            creationDisplayInfo.density.density.rf,
            creationDisplayInfo.density.fontScale.rf,
        ),
    layoutDirection: LayoutDirection =
        toLayoutDirection(context.resources.configuration.layoutDirection),
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    writerEvents: WriterEvents = WriterEvents(),
    content: @Composable @RemoteComposable () -> Unit,
): CapturedDocument {
    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)

    val recomposerDispatcher =
        currentCoroutineContext()
            .toSingleThreadedRecomposerContext(name = "captureSingleRemoteDocument")
    val recomposer = Recomposer(currentCoroutineContext() + recomposerDispatcher)
    val composition = Composition(applier, recomposer)
    val lifecycleOwner = HeadlessLifecycleOwner()

    try {
        val creationState =
            RemoteComposeCreationState(
                creationDisplayInfo = creationDisplayInfo,
                profile = profile,
                writerEvents = writerEvents,
                layoutDirection = layoutDirection,
                remoteDensity = remoteDensity,
            )

        val initialSize = creationState.document.buffer.buffer.size()

        withContext(recomposerDispatcher) {
            composition.setContent {
                CompositionLocalProvider(
                    LocalRemoteComposeCreationState provides creationState,
                    LocalInspectionMode provides creationDisplayInfo.isInspectionMode,
                    LocalDensity provides
                        Density(
                            creationDisplayInfo.density.density,
                            creationDisplayInfo.density.fontScale,
                        ),
                    LocalRemoteDensity provides remoteDensity,
                    LocalContext provides context,
                    LocalConfiguration provides context.resources.configuration,
                    LocalLayoutDirection provides layoutDirection,
                    LocalFontWeightAdjustment provides
                        platformFontWeightAdjustment(context.resources.configuration),
                    LocalLifecycleOwner provides lifecycleOwner,
                    LocalIs24HourFormat provides DateFormat.is24HourFormat(context),
                    content = content,
                )
            }
        }

        coroutineScope {
            lateinit var frameClock: BroadcastFrameClock
            frameClock = BroadcastFrameClock {
                launch(recomposerDispatcher) { frameClock.sendFrame(clock.nanoTime()) }
            }
            try {
                traceAsync(
                    "CaptureRemoteDocument:captureSingleRemoteDocument:compositionInitialization",
                    ThreadLocalRandom.current().nextInt(),
                ) {
                    launch(recomposerDispatcher + frameClock) {
                        recomposer.runRecomposeAndApplyChanges()
                    }

                    check(
                        awaitRecomposerQuiescence(
                            recomposer = recomposer,
                            frameClock = frameClock,
                            recomposerDispatcher = recomposerDispatcher,
                            clock = clock,
                        )
                    ) {
                        "captureSingleRemoteDocument did not reach a quiescent Idle state " +
                            "after $MAX_IDLE_ITERATIONS iterations. This is likely caused by " +
                            "unstable recomposition (e.g. a Composable or effect continuously " +
                            "mutating state on every frame or snapshot apply notification)."
                    }
                }
            } finally {
                frameClock.cancel()
                recomposer.cancel()
                currentCoroutineContext().cancelChildren()
            }
        }

        val document =
            withContext(recomposerDispatcher) {
                Snapshot.withMutableSnapshot {
                    val recordingCanvas = RecordingCanvas(createBitmap(1, 1), creationState)

                    val remoteCanvas = RemoteCanvas(recordingCanvas)

                    if (RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled) {
                        check(creationState.document.buffer.buffer.size() == initialSize) {
                            "Document was written to during composition. Expected size $initialSize, got ${creationState.document.buffer.buffer.size()}"
                        }
                    }

                    trace("CaptureRemoteDocument:captureSingleRemoteDocument:rootNodeRender") {
                        rootNode.render(creationState, remoteCanvas)
                        remoteCanvas.flush()
                    }

                    creationState.document.encodeToByteArray()
                }
            }

        return CapturedDocument(document, writerEvents.pendingIntents, writerEvents.lambdas)
    } finally {
        withContext(recomposerDispatcher + NonCancellable) {
            recomposer.cancel()
            lifecycleOwner.destroy()
            composition.dispose()
        }
    }
}

/**
 * Capture a stream of RemoteCompose documents by rendering the specified [content] Composable in a
 * virtual display and emitting the resulting byte arrays whenever recomposition occurs and the
 * layout visually changes.
 *
 * This API allows capturing dynamic Compose content (e.g., containing animations, transitions, or
 * state updates) as a Flow of serialized document byte arrays.
 *
 * Crucially, recomposition is handled cleanly, and duplicate documents (where nothing visually
 * changed in the layout tree) are automatically filtered out, so new byte arrays are only emitted
 * when the document actually changes.
 *
 * @param creationDisplayInfo Details about the virtual display to capture for (size, density,
 *   etc.).
 * @param remoteDensity The logical screen density and font scale to use for unit conversions.
 *   Defaults to density derived from [creationDisplayInfo]. Note: If passing custom values, they
 *   should typically match the density and font scale specified in [creationDisplayInfo] to avoid
 *   layout scaling discrepancies.
 * @param layoutDirection The layout direction (LTR or RTL) to use. Defaults to LTR.
 * @param writerEvents Callback to handle non-serializable events (e.g. pending intents).
 * @param context The Android [Context] to use.
 * @param clock The clock used for the recomposer timeline. Defaults to [RemoteClock.SYSTEM].
 * @param profile The writing profile that determines supported operations. Defaults to
 *   [RcPlatformProfiles.ANDROIDX].
 * @param coroutineContext The CoroutineContext to run recomposition and rendering on. Defaults to
 *   [Dispatchers.Default].
 * @param content The Composable content to render and capture.
 * @return A [Flow] of [ByteArray]s containing the serialized RemoteCompose documents.
 */
public fun captureRemoteDocument(
    context: Context,
    creationDisplayInfo: RemoteCreationDisplayInfo,
    remoteDensity: RemoteDensity =
        RemoteDensity(
            creationDisplayInfo.density.density.rf,
            creationDisplayInfo.density.fontScale.rf,
        ),
    layoutDirection: LayoutDirection? = null,
    writerEvents: WriterEvents = WriterEvents(),
    clock: RemoteClock = RemoteClock.SYSTEM,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    content: @Composable @RemoteComposable () -> Unit,
): Flow<ByteArray> = flow {
    require(RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled) {
        "captureRemoteDocument requires isEnforceCleanRecompositionEnabled to be true"
    }

    val rootNode = RemoteRootNode()
    val applier = RemoteComposeApplier(rootNode)

    val recomposerDispatcher =
        (currentCoroutineContext() + coroutineContext).toSingleThreadedRecomposerContext(
            name = "captureRemoteDocument"
        )
    val recomposer = Recomposer(currentCoroutineContext() + recomposerDispatcher)
    val composition = Composition(applier, recomposer)
    val lifecycleOwner = HeadlessLifecycleOwner()

    try {
        val layoutDirection =
            (layoutDirection ?: toLayoutDirection(context.resources.configuration.layoutDirection))
        val creationState =
            RemoteComposeCreationState(
                creationDisplayInfo = creationDisplayInfo,
                profile = profile,
                writerEvents = writerEvents,
                layoutDirection = layoutDirection,
                remoteDensity = remoteDensity,
            )

        val initialSize = creationState.document.buffer.buffer.size()

        withContext(recomposerDispatcher) {
            composition.setContent {
                CompositionLocalProvider(
                    LocalRemoteComposeCreationState provides creationState,
                    LocalInspectionMode provides creationDisplayInfo.isInspectionMode,
                    LocalDensity provides
                        Density(
                            creationDisplayInfo.density.density,
                            creationDisplayInfo.density.fontScale,
                        ),
                    LocalRemoteDensity provides remoteDensity,
                    LocalContext provides context,
                    LocalConfiguration provides context.resources.configuration,
                    LocalLayoutDirection provides layoutDirection,
                    LocalFontWeightAdjustment provides
                        platformFontWeightAdjustment(context.resources.configuration),
                    LocalLifecycleOwner provides lifecycleOwner,
                    LocalIs24HourFormat provides DateFormat.is24HourFormat(context),
                    content = content,
                )
            }
        }

        coroutineScope {
            SnapshotWriteMonitor.acquire()
            lateinit var frameClock: BroadcastFrameClock
            frameClock = BroadcastFrameClock {
                launch(recomposerDispatcher) { frameClock.sendFrame(clock.nanoTime()) }
            }
            try {
                launch(recomposerDispatcher + frameClock) {
                    recomposer.runRecomposeAndApplyChanges()
                }

                // Regeneration must not be driven by observing Recomposer.currentState
                // transitions. currentState is a conflated StateFlow: a collector that is
                // descheduled (e.g. under CPU contention) while the recomposer cycles
                // Idle -> Recomposing -> Idle observes no value change at all, so
                // `filter { it == Idle }` never re-emits and the updated document is silently
                // dropped. Drive regeneration from snapshot apply notifications instead. A
                // CONFLATED channel collapses bursts of writes into a single regeneration but,
                // unlike a StateFlow value slot, can never lose the fact that state changed.
                val invalidations = Channel<Unit>(Channel.CONFLATED)
                // Seed so the initial document is always produced.
                invalidations.trySend(Unit)
                // Apply notifications raised by states written during this session's own document
                // rendering must not re-trigger it, otherwise regeneration would spin. Tracking the
                // exact state objects mutated inside renderSnapshot avoids swallowing global
                // snapshot changes from other threads that happen to be coalesced during apply().
                val renderingModifiedStates =
                    Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
                val applyObserverHandle = Snapshot.registerApplyObserver { changed, _ ->
                    val hasExternalModification =
                        synchronized(renderingModifiedStates) {
                            renderingModifiedStates.isEmpty() ||
                                changed.any { it !in renderingModifiedStates }
                        }
                    if (hasExternalModification) {
                        invalidations.trySend(Unit)
                    }
                }

                try {
                    var previousBytes: ByteArray? = null
                    for (invalidation in invalidations) {
                        // Let the recomposer observe and apply the invalidation before
                        // rendering. An unsettled composition (e.g. continuously animating
                        // content) still renders after the iteration bound so that streaming
                        // captures keep producing frames.
                        awaitRecomposerQuiescence(
                            recomposer = recomposer,
                            frameClock = frameClock,
                            recomposerDispatcher = recomposerDispatcher,
                            clock = clock,
                        )

                        val bytes =
                            withContext(recomposerDispatcher) {
                                val renderSnapshot =
                                    Snapshot.takeMutableSnapshot(
                                        writeObserver = { written ->
                                            synchronized(renderingModifiedStates) {
                                                renderingModifiedStates.add(written)
                                            }
                                        }
                                    )
                                try {
                                    val encoded = renderSnapshot.enter {
                                        creationState.document =
                                            profile.create(
                                                creationDisplayInfo.toCreationDisplayInfo(),
                                                writerEvents,
                                            )
                                        creationState.expressionCache.clear()
                                        creationState.intExpressionCache.clear()
                                        creationState.remoteVariableToId.clear()
                                        creationState.floatArrayCache.clear()
                                        creationState.longArrayCache.clear()
                                        val recordingCanvas =
                                            RecordingCanvas(createBitmap(1, 1), creationState)

                                        val remoteCanvas = RemoteCanvas(recordingCanvas)

                                        check(
                                            creationState.document.buffer.buffer.size() ==
                                                initialSize
                                        ) {
                                            "Document was written to during composition. Expected size $initialSize, got ${creationState.document.buffer.buffer.size()}"
                                        }

                                        rootNode.render(creationState, remoteCanvas)
                                        remoteCanvas.flush()

                                        creationState.document.encodeToByteArray()
                                    }
                                    renderSnapshot.apply().check()
                                    encoded
                                } finally {
                                    renderSnapshot.dispose()
                                    synchronized(renderingModifiedStates) {
                                        renderingModifiedStates.clear()
                                    }
                                }
                            }

                        if (previousBytes?.contentEquals(bytes) != true) {
                            previousBytes = bytes
                            emit(bytes)
                        }
                    }
                } finally {
                    applyObserverHandle.dispose()
                    invalidations.close()
                }
            } finally {
                frameClock.cancel()
                recomposer.cancel()
                currentCoroutineContext().cancelChildren()
                SnapshotWriteMonitor.release()
            }
        }
    } finally {
        withContext(recomposerDispatcher + NonCancellable) {
            recomposer.cancel()
            lifecycleOwner.destroy()
            composition.dispose()
        }
    }
}

private const val MAX_IDLE_ITERATIONS = 100

/**
 * Suspends until [recomposer] has settled, i.e. it is [Recomposer.State.Idle] with no pending work,
 * [frameClock] has no awaiters, and no snapshot apply observer notifications are in flight.
 *
 * A single Idle await is insufficient because:
 * 1. Effects launched during composition suspend on the external [frameClock] (whose awaiters are
 *    tracked by [BroadcastFrameClock.hasAwaiters], not by [Recomposer.hasPendingWork]),
 * 2. Continuations resuming from `withFrameNanos {}` are dispatched onto [recomposerDispatcher]
 *    after [BroadcastFrameClock.sendFrame] completes, and
 * 3. Another thread concurrently inside `advanceGlobalSnapshot()` may have claimed
 *    `globalSnapshot.modified` while its applyObserver notifications are still in flight (tracked
 *    by [Snapshot.isApplyObserverNotificationPending]).
 *
 * Draining [recomposerDispatcher] and sending apply notifications until all of those are clear
 * guarantees that every pending write has been applied and composed.
 *
 * Note that [Recomposer.currentState] is only ever sampled by value here, never used to detect a
 * transition, so conflation of the underlying [StateFlow] cannot cause a missed wake-up.
 *
 * @param maxIterations bound on the number of non-quiescent iterations before giving up.
 * @return `true` if a quiescent state was reached, `false` if [maxIterations] was exhausted, which
 *   indicates unstable recomposition (e.g. content that mutates state on every frame).
 */
private suspend fun awaitRecomposerQuiescence(
    recomposer: Recomposer,
    frameClock: BroadcastFrameClock,
    recomposerDispatcher: CoroutineContext,
    clock: RemoteClock,
    maxIterations: Int = MAX_IDLE_ITERATIONS,
): Boolean {
    // Only call yield() when recomposerDispatcher actually queues tasks (isDispatchNeeded == true).
    // For immediate dispatchers like Dispatchers.Main.immediate on the main thread (where
    // continuations run inline), yield() forces a Handler.post() via YieldContext, which deadlocks
    // if the main thread is blocked inside runBlocking/runTest.
    val shouldYield =
        recomposerDispatcher is CoroutineDispatcher &&
            recomposerDispatcher.isDispatchNeeded(EmptyCoroutineContext)
    var idleIterations = 0
    while (true) {
        recomposer.currentState.filter { it == Recomposer.State.Idle }.first()
        val isQuiescent =
            withContext(recomposerDispatcher) {
                if (shouldYield) yield()
                while (frameClock.hasAwaiters) {
                    frameClock.sendFrame(clock.nanoTime())
                    if (shouldYield) yield()
                }
                if (shouldYield) yield()
                Snapshot.sendApplyNotifications()
                !Snapshot.isApplyObserverNotificationPending &&
                    recomposer.currentState.value == Recomposer.State.Idle &&
                    !recomposer.hasPendingWork &&
                    !frameClock.hasAwaiters
            }
        if (isQuiescent) {
            return true
        }
        if (++idleIterations >= maxIterations) {
            return false
        }
    }
}

/**
 * Returns a [CoroutineContext] (without a [Job]) whose dispatcher executes at most one task at a
 * time so that recomposition, frame clock callbacks, effect continuations, and rendering are
 * serialized:
 * - When no [ContinuationInterceptor] is present (e.g. a raw background or Binder thread), falls
 *   back to a single-threaded slice of [Dispatchers.Default].
 * - When running with a [MainCoroutineDispatcher], preserves the context as-is since execution is
 *   already confined to the main thread.
 * - When running on a multi-threaded [CoroutineDispatcher] (where
 *   [CoroutineDispatcher.isDispatchNeeded] is true), constrains it via
 *   `limitedParallelism(parallelism = 1, name = name)`.
 * - For unconfined dispatchers (e.g. [Dispatchers.Unconfined], where `isDispatchNeeded` is false
 *   and `limitedParallelism` throws [UnsupportedOperationException]) or custom non-dispatcher
 *   [ContinuationInterceptor]s (such as Compose test interceptors), preserves the context as-is.
 */
private fun CoroutineContext.toSingleThreadedRecomposerContext(name: String): CoroutineContext {
    val baseContext = this.minusKey(Job)
    val interceptor = baseContext[ContinuationInterceptor]
    return when {
        interceptor == null ->
            baseContext + Dispatchers.Default.limitedParallelism(parallelism = 1, name = name)
        interceptor is MainCoroutineDispatcher -> baseContext
        interceptor is CoroutineDispatcher && interceptor.isDispatchNeeded(EmptyCoroutineContext) ->
            baseContext + interceptor.limitedParallelism(parallelism = 1, name = name)
        else -> baseContext
    }
}

/**
 * Reference-counted monitor that coalesces global snapshot writes into
 * [Snapshot.sendApplyNotifications] while at least one streaming [captureRemoteDocument] session is
 * active.
 *
 * Compose Runtime's [Snapshot.registerGlobalWriteObserver] is process-wide (written state objects
 * carry no per-session identity), and a single [Snapshot.sendApplyNotifications] call advances the
 * global snapshot and notifies every active [Recomposer], each of which filters the changed set
 * against its own recorded read set.
 *
 * Therefore, multiple concurrent sessions across threads share a single observer registration and a
 * single coalescing channel loop:
 * - When active session count transitions `0 -> 1`, one global write observer and consumer loop are
 *   started.
 * - While K >= 1 sessions are active across threads, all K sessions share that single observer and
 *   single coalesced notification dispatch.
 * - When active session count drops `1 -> 0` (in `finally`), the [ObserverHandle] is disposed and
 *   the consumer coroutine is cancelled so zero global observers or background coroutines remain
 *   when idle.
 *
 * See prior art in other Compose composition hosts across AndroidX:
 * - `androidx.glance.session.globalSnapshotMonitor` and
 *   `androidx.glance.session.GlobalSnapshotManager` (session-scoped suspend monitor and
 *   process-wide coalescing channel loop on `Dispatchers.Default`).
 * - `androidx.compose.ui.platform.GlobalSnapshotManager` (process-wide coalescing channel loop on
 *   `AndroidUiDispatcher.Main`).
 * - `androidx.glance.session.SessionWorker.runSession` (hosting `Recomposer`, frame clock, and
 *   snapshot monitor inside a coroutine scope).
 */
internal object SnapshotWriteMonitor {
    private val lock = Any()
    private var refCount = 0
    private var observerHandle: ObserverHandle? = null
    private var monitorJob: Job? = null

    @get:VisibleForTesting
    internal val activeSessionCount: Int
        get() = synchronized(lock) { refCount }

    @get:VisibleForTesting
    internal val isRunning: Boolean
        get() = synchronized(lock) { observerHandle != null }

    fun acquire() {
        synchronized(lock) {
            if (refCount++ == 0) {
                val channel = Channel<Unit>(1)
                val sent = AtomicBoolean(false)
                val scope =
                    CoroutineScope(
                        Dispatchers.Default.limitedParallelism(
                            parallelism = 1,
                            name = "RemoteComposeSnapshotWriteMonitor",
                        ) + SupervisorJob()
                    )
                monitorJob = scope.launch {
                    channel.consumeEach {
                        sent.set(false)
                        Snapshot.sendApplyNotifications()
                    }
                }
                observerHandle = Snapshot.registerGlobalWriteObserver {
                    if (sent.compareAndSet(false, true)) {
                        if (channel.trySend(Unit).isFailure) {
                            sent.set(false)
                        }
                    }
                }
            }
        }
    }

    fun release() {
        synchronized(lock) {
            check(refCount > 0) {
                "SnapshotWriteMonitor.release() called without matching acquire()"
            }
            if (--refCount == 0) {
                observerHandle?.dispose()
                observerHandle = null
                monitorJob?.cancel()
                monitorJob = null
            }
        }
    }
}

/**
 * Lightweight, thread-safe [LifecycleOwner] for headless document capture.
 *
 * `androidx.lifecycle.LifecycleRegistry` enforces main-thread access (`enforceMainThreadIfNeeded`),
 * which throws [IllegalStateException] when a capture runs on a background thread (e.g. a Binder
 * thread or [Dispatchers.Default]). See also `androidx.lifecycle.LifecycleRegistry.createUnsafe` as
 * prior art for bypassing main-thread enforcement in off-main-thread / headless hosts. This
 * implementation avoids main-thread enforcement and uses per-registration tokens with
 * reference-identity matching so observers can safely remove themselves during event dispatch from
 * any thread.
 */
private class HeadlessLifecycleOwner : LifecycleOwner {
    private class Registration(val observer: LifecycleObserver) {
        @Volatile var active = true

        fun dispatch(owner: LifecycleOwner, event: Lifecycle.Event) {
            if (observer is DefaultLifecycleObserver) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> observer.onCreate(owner)
                    Lifecycle.Event.ON_START -> observer.onStart(owner)
                    Lifecycle.Event.ON_RESUME -> observer.onResume(owner)
                    Lifecycle.Event.ON_PAUSE -> observer.onPause(owner)
                    Lifecycle.Event.ON_STOP -> observer.onStop(owner)
                    Lifecycle.Event.ON_DESTROY -> observer.onDestroy(owner)
                    Lifecycle.Event.ON_ANY -> {}
                }
            }
            if (active && observer is LifecycleEventObserver) {
                observer.onStateChanged(owner, event)
            }
        }
    }

    private val lock = Any()
    @Volatile private var state = Lifecycle.State.RESUMED
    private val registrations = CopyOnWriteArrayList<Registration>()

    override val lifecycle: Lifecycle =
        object : Lifecycle() {
            override val currentState: State
                get() = state

            override fun addObserver(observer: LifecycleObserver) {
                synchronized(lock) {
                    if (state == State.DESTROYED) {
                        return
                    }
                    val registration = Registration(observer)
                    registrations.add(registration)
                    for (event in arrayOf(Event.ON_CREATE, Event.ON_START, Event.ON_RESUME)) {
                        if (!registration.active) break
                        registration.dispatch(this@HeadlessLifecycleOwner, event)
                    }
                }
            }

            override fun removeObserver(observer: LifecycleObserver) {
                synchronized(lock) {
                    for (registration in registrations.toTypedArray()) {
                        if (registration.observer === observer) {
                            registration.active = false
                            registrations.remove(registration)
                            break
                        }
                    }
                }
            }
        }

    fun destroy() {
        synchronized(lock) {
            state = Lifecycle.State.DESTROYED
            for (registration in registrations.toTypedArray()) {
                for (event in
                    arrayOf(
                        Lifecycle.Event.ON_PAUSE,
                        Lifecycle.Event.ON_STOP,
                        Lifecycle.Event.ON_DESTROY,
                    )) {
                    if (!registration.active) break
                    registration.dispatch(this, event)
                }
            }
            registrations.clear()
        }
    }
}

private fun CreationDisplayInfo.toRemote(
    fontScale: Float,
    isInspectionMode: Boolean = false,
): RemoteCreationDisplayInfo =
    RemoteCreationDisplayInfo(
        width = this.width,
        height = this.height,
        densityDpi = this.densityDpi,
        fontScale = fontScale,
        isInspectionMode = isInspectionMode,
    )

private fun platformFontWeightAdjustment(configuration: Configuration): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (configuration.fontWeightAdjustment != Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED) {
            configuration.fontWeightAdjustment
        } else {
            0
        }
    } else {
        0
    }
