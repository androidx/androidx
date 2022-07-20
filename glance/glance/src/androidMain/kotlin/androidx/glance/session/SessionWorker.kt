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
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

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
    }

    override suspend fun doWork(): Result = coroutineScope {
        val frameClock = InteractiveFrameClock(this)
        val key =
            inputData.getString(sessionManager.keyParam) ?: return@coroutineScope Result.failure()
        val session = requireNotNull(sessionManager.getSession(key)) {
            "No session available to key $key"
        }

        if (DEBUG) Log.d(TAG, "Setting up composition for ${session.key}")
        GlobalSnapshotManager.ensureStarted()
        val root = session.createRootEmittable()
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(Applier(root), recomposer)
        val contentReady = MutableStateFlow(false)
        val uiReady = MutableStateFlow(false)
        var contentCoroutine: CancellableContinuation<Unit>? = null
        launch {
            session.provideGlance(applicationContext) { content ->
                contentCoroutine?.cancel()
                suspendCancellableCoroutine { co ->
                    contentCoroutine = co
                    composition.setContent(content)
                    contentReady.tryEmit(true)
                }
            }
        }
        launch {
            contentReady.first { it }
            withContext(frameClock) { recomposer.runRecomposeAndApplyChanges() }
        }
        launch {
            contentReady.first { it }
            recomposer.currentState.collect { state ->
                if (DEBUG) Log.d(TAG, "Recomposer(${session.key}): currentState=$state")
                when (state) {
                    Recomposer.State.Idle -> {
                        if (DEBUG) Log.d(TAG, "UI tree ready (${session.key})")
                        session.processEmittableTree(
                            applicationContext,
                            root.copy() as EmittableWithChildren
                        )
                        uiReady.emit(true)
                    }
                    Recomposer.State.ShutDown -> cancel()
                    else -> {}
                }
            }
        }

        uiReady.first { it }
        session.receiveEvents(applicationContext) {
            if (DEBUG) Log.d(TAG, "processing event for ${session.key}")
            launch { frameClock.startInteractive() }
        }

        composition.dispose()
        contentCoroutine?.resume(Unit)
        frameClock.stopInteractive()
        recomposer.close()
        recomposer.join()
        return@coroutineScope Result.success()
    }
}
