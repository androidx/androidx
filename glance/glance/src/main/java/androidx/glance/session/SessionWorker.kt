/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.session

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.glance.Applier
import androidx.glance.EmittableWithChildren
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Options to configure [SessionWorker] timeouts.
 *
 * @property initialTimeout How long to wait after the first successful composition before timing
 *   out.
 * @property additionalTime If an external event is received and there is less than [additionalTime]
 *   remaining, add [additionalTime] so that there is enough time to respond to the event.
 * @property idleTimeout Timeout within [idleTimeout] if the system is in idle/light idle/low power
 *   standby mode.
 * @property timeSource The time source for measuring progress towards timeouts.
 */
@RestrictTo(LIBRARY_GROUP)
data class TimeoutOptions(
    val initialTimeout: Duration = 45.seconds,
    val additionalTime: Duration = 5.seconds,
    val idleTimeout: Duration = 5.seconds,
    val timeSource: TimeSource = TimeSource.Monotonic,
)

/**
 * [SessionWorker] handles composition for a particular Glanceable.
 *
 * This worker runs the [Session] it acquires from [SessionManager] for the key given in the worker
 * params. The worker then sets up and runs a composition, then provides the resulting UI tree (and
 * those of successive recompositions) to [Session.processEmittableTree]. After the initial
 * composition, the worker blocks on [Session.receiveEvents] until [Session.close] is called.
 */
@RestrictTo(LIBRARY_GROUP)
class SessionWorker(
    appContext: Context,
    private val params: WorkerParameters,
    private val sessionManager: SessionManager = GlanceSessionManager,
    private val timeouts: TimeoutOptions = TimeoutOptions(),
    @Deprecated("Deprecated by super class, replacement in progress, see b/245353737")
    override val coroutineContext: CoroutineDispatcher = Dispatchers.Main
) : CoroutineWorker(appContext, params) {
    // This constructor is required by WorkManager's default WorkerFactory.
    constructor(
        appContext: Context,
        params: WorkerParameters
    ) : this(
        appContext,
        params,
        GlanceSessionManager,
    )

    companion object {
        internal const val TAG = "GlanceSessionWorker"
        internal const val DEBUG = false
        internal const val TimeoutExitReason = "TIMEOUT_EXIT_REASON"
    }

    private val key =
        inputData.getString(sessionManager.keyParam)
            ?: error("SessionWorker must be started with a key")

    @VisibleForTesting internal var effectJob: Job? = null

    override suspend fun doWork() =
        withTimerOrNull(timeouts.timeSource) {
            observeIdleEvents(
                applicationContext,
                onIdle = {
                    startTimer(timeouts.idleTimeout)
                    if (DEBUG) Log.d(TAG, "Received idle event, session timeout $timeLeft")
                }
            ) {
                val session =
                    sessionManager.runWithLock { getSession(key) }
                        ?: if (params.runAttemptCount == 0) {
                            error("No session available for key $key")
                        } else {
                            // If this is a retry because the process was restarted (e.g. on app
                            // upgrade
                            // or reinstall), the Session object won't be available because it's not
                            // persistable.
                            Log.w(
                                TAG,
                                "SessionWorker attempted restart but Session is not available for $key"
                            )
                            return@observeIdleEvents Result.success()
                        }

                try {
                    runSession(
                        applicationContext,
                        session,
                        timeouts,
                        effectJobFactory = { Job().also { effectJob = it } }
                    )
                } finally {
                    // Get session manager lock to close session to prevent a race where an observer
                    // who is checking session state (e.g. GlanceAppWidget.update) sees this session
                    // as running before trying to send an event. Without the lock, it may happen
                    // that we close right after they see us as running, which will cause an error
                    // when they try to send an event.
                    // With the lock, if they see us as running and send an event right before this
                    // block, then the event will be unhandled.
                    // After this block, observers will see this session as closed, so they will
                    // start a new one instead of trying to send events to this one.
                    // We must use NonCancellable here because it provides a Job that has not been
                    // cancelled. The withTimerOrNull Job that the session runs in has already been
                    // cancelled, so suspending with that Job would throw a CancellationException.
                    withContext(NonCancellable) {
                        sessionManager.runWithLock { closeSession(session.key) }
                    }
                }
                Result.success()
            }
        } ?: Result.success(Data.Builder().putBoolean(TimeoutExitReason, true).build())
}

private suspend fun TimerScope.runSession(
    context: Context,
    session: Session,
    timeouts: TimeoutOptions,
    effectJobFactory: () -> Job = { Job() },
) {
    if (SessionWorker.DEBUG) Log.d(SessionWorker.TAG, "Setting up composition for ${session.key}")
    val frameClock = InteractiveFrameClock(this)
    val snapshotMonitor = launch { globalSnapshotMonitor() }
    val root = session.createRootEmittable()
    val uiReady = MutableStateFlow(false)
    // For effects, use an independent Job with a CoroutineExceptionHandler so that we can catch
    // errors from LaunchedEffects in the composition and they won't propagate up to TimerScope.
    // If we set Job.parent, then we cannot use our own CoroutineExceptionHandler. However, this
    // also means that cancellation of TimerScope does not propagate automatically to this Job,
    // so we must set that up manually here to avoid leaking the effect Job after this scope
    // ends.
    val effectExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        launch {
            session.onCompositionError(context, throwable)
            this@runSession.cancel("Error in composition effect coroutine", throwable)
        }
    }
    val effectCoroutineContext =
        effectJobFactory().let { job ->
            coroutineContext[Job]?.invokeOnCompletion { job.cancel() }
            coroutineContext + job + effectExceptionHandler
        }
    val recomposer = Recomposer(effectCoroutineContext)
    val composition = Composition(Applier(root), recomposer)

    try {
        launch(frameClock) {
            try {
                composition.setContent(session.provideGlance(context))
                recomposer.runRecomposeAndApplyChanges()
            } catch (e: CancellationException) {
                // do nothing if we are cancelled.
            } catch (throwable: Throwable) {
                session.onCompositionError(context, throwable)
                this@runSession.cancel("Error in recomposition coroutine", throwable)
            }
        }
        launch {
            var lastRecomposeCount = recomposer.changeCount
            recomposer.currentState.collectLatest { state ->
                if (SessionWorker.DEBUG)
                    Log.d(SessionWorker.TAG, "Recomposer(${session.key}): currentState=$state")
                when (state) {
                    Recomposer.State.Idle -> {
                        // Only update the session when a change has actually occurred. The
                        // Recomposer may sometimes wake up due to changes in other
                        // compositions. Also update the session if we have not sent an initial
                        // tree yet.
                        if (recomposer.changeCount > lastRecomposeCount || !uiReady.value) {
                            if (SessionWorker.DEBUG)
                                Log.d(SessionWorker.TAG, "UI tree updated (${session.key})")
                            val processed =
                                session.processEmittableTree(
                                    context,
                                    root.copy() as EmittableWithChildren
                                )
                            // If the UI has been processed for the first time, set uiReady to true
                            // and start the timeout.
                            if (!uiReady.value && processed) {
                                uiReady.emit(true)
                                startTimer(timeouts.initialTimeout)
                            }
                        }
                        lastRecomposeCount = recomposer.changeCount
                    }
                    Recomposer.State.ShutDown -> cancel()
                    else -> {}
                }
            }
        }

        // Wait until the Emittable tree has been processed at least once before receiving events.
        uiReady.first { it }
        // receiveEvents will suspend until the session is closed (usually due to widget deletion)
        // or it is cancelled (in case of composition errors or timeout).
        session.receiveEvents(context) {
            // If time is running low, add time to make sure that we have time to respond to this
            // event.
            if (timeLeft < timeouts.additionalTime) addTime(timeouts.additionalTime)
            if (SessionWorker.DEBUG)
                Log.d(SessionWorker.TAG, "processing event for ${session.key}; $timeLeft left")
            launch { frameClock.startInteractive() }
        }
    } finally {
        composition.dispose()
        frameClock.stopInteractive()
        snapshotMonitor.cancel()
        recomposer.cancel()
    }
}

/**
 * Run this [Session] with no timeout. The session will continue to run until [Session.close] is
 * called or this call is cancelled. This can be used to run a session without
 * [GlanceSessionManager], i.e. without starting a worker.
 */
@RestrictTo(LIBRARY_GROUP)
suspend fun Session.runSession(context: Context) {
    noopTimer { runSession(context, this@runSession, TimeoutOptions()) }
}
