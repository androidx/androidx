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
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.glance.Applier
import androidx.glance.EmittableWithChildren
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [SessionWorker] handles composition for a particular Glanceable.
 *
 * This worker runs the [Session] it acquires from [SessionManager] for the key given in the worker
 * params. The worker then sets up and runs a composition, then provides the resulting UI tree
 * (and those of successive recompositions) to [Session.processEmittableTree]. After the initial
 * composition, the worker blocks on [Session.receiveEvents] until [Session.close] is called.
 */
internal class SessionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    @VisibleForTesting
    internal var sessionManager: SessionManager = GlanceSessionManager

    companion object {
        private const val TAG = "GlanceSessionWorker"
        private const val DEBUG = false
        @VisibleForTesting
        internal val defaultTimeout = 45.seconds
    }

    private val key = inputData.getString(sessionManager.keyParam)
            ?: error("SessionWorker must be started with a key")

    @Deprecated("Deprecated by super class, replacement in progress, see b/245353737")
    override val coroutineContext = Dispatchers.Main

    override suspend fun doWork(): Result = withTimeoutOrNull(defaultTimeout) {
        val session = sessionManager.getSession(key) ?: error("No session available for key $key")
        if (DEBUG) Log.d(TAG, "Setting up composition for ${session.key}")
        val frameClock = InteractiveFrameClock(this)
        val snapshotMonitor = launch { globalSnapshotMonitor() }
        val root = session.createRootEmittable()
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(Applier(root), recomposer).apply {
            setContent(session.provideGlance(applicationContext))
        }
        val uiReady = MutableStateFlow(false)

        launch(frameClock) {
            recomposer.runRecomposeAndApplyChanges()
        }
        launch {
            var lastRecomposeCount = recomposer.changeCount
            recomposer.currentState.collect { state ->
                if (DEBUG) Log.d(TAG, "Recomposer(${session.key}): currentState=$state")
                when (state) {
                    Recomposer.State.Idle -> {
                        // Only update the session when a change has actually occurred. The
                        // Recomposer may sometimes wake up due to changes in other compositions.
                        // Also update the session if we have not sent an initial tree yet.
                        if (recomposer.changeCount > lastRecomposeCount || !uiReady.value) {
                            if (DEBUG) Log.d(TAG, "UI tree updated (${session.key})")
                            val processed = session.processEmittableTree(
                                applicationContext,
                                root.copy() as EmittableWithChildren
                            )
                            if (!uiReady.value && processed) uiReady.emit(true)
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
        session.receiveEvents(applicationContext) {
            if (DEBUG) Log.d(TAG, "processing event for ${session.key}")
            launch { frameClock.startInteractive() }
        }

        composition.dispose()
        frameClock.stopInteractive()
        snapshotMonitor.cancel()
        recomposer.close()
        recomposer.join()
        return@withTimeoutOrNull Result.success()
    } ?: Result.success()
}
