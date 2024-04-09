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

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.await
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@JvmDefaultWithCompatibility
/**
 * [SessionManager] is the entrypoint for Glance surfaces to start a session worker that will handle
 * their composition.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SessionManager {
    /**
     * [runWithLock] provides a scope in which to run operations on SessionManager.
     *
     * The implementation must ensure that concurrent calls to [runWithLock] are mutually exclusive.
     * Because this function holds a lock while running [block], clients should not run any
     * long-running operations in [block]. The client should not maintain a reference to the
     * [SessionManagerScope] after [block] returns.
     */
    suspend fun <T> runWithLock(block: suspend SessionManagerScope.() -> T): T

    /**
     * The name of the session key parameter, which is used to set the session key in the Worker's
     * input data.
     * TODO: consider using a typealias instead
     */
    val keyParam: String
        get() = "KEY"
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SessionManagerScope {
    /**
     * Start a session for the Glance in [session].
     */
    suspend fun startSession(context: Context, session: Session)

    /**
     * Closes the channel for the session corresponding to [key].
     */
    suspend fun closeSession(key: String)

    /**
     * Returns true if a session is active with the given [key].
     */
    suspend fun isSessionRunning(context: Context, key: String): Boolean

    /**
     * Gets the session corresponding to [key] if it exists
     */
    fun getSession(key: String): Session?
}

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val GlanceSessionManager: SessionManager = SessionManagerImpl(SessionWorker::class.java)

internal class SessionManagerImpl(
    private val workerClass: Class<out ListenableWorker>
) : SessionManager {
    private companion object {
        const val TAG = "GlanceSessionManager"
        const val DEBUG = false
    }

    // This mutex guards access to the SessionManagerScope, to prevent multiple clients from
    // performing SessionManagerScope operations at the same time.
    private val mutex = Mutex()

    // All external access to this object is protected with a mutex, so there is no need for any
    // internal synchronization.
    private val scope = object : SessionManagerScope {
        private val sessions = mutableMapOf<String, Session>()

        override suspend fun startSession(context: Context, session: Session) {
            if (DEBUG) Log.d(TAG, "startSession(${session.key})")
            sessions.put(session.key, session)?.let { previousSession ->
                previousSession.close()
            }
            val workRequest = OneTimeWorkRequest.Builder(workerClass)
                .setInputData(
                    workDataOf(
                        keyParam to session.key
                    )
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(session.key, ExistingWorkPolicy.REPLACE, workRequest)
                .result.await()
            enqueueDelayedWorker(context)
        }

        override fun getSession(key: String): Session? = sessions[key]

        @SuppressLint("ListIterator")
        override suspend fun isSessionRunning(
            context: Context,
            key: String
        ): Boolean {
            val workerIsRunningOrEnqueued = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(key)
                .await()
                .any { it.state in listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED) }
            val hasOpenSession = sessions[key]?.isOpen ?: false
            val isRunning = hasOpenSession && workerIsRunningOrEnqueued
            if (DEBUG) Log.d(TAG, "isSessionRunning($key) == $isRunning")
            return isRunning
        }

        override suspend fun closeSession(key: String) {
            if (DEBUG) Log.d(TAG, "closeSession($key)")
            sessions.remove(key)?.close()
        }
    }

    override suspend fun <T> runWithLock(
        block: suspend SessionManagerScope.() -> T
    ): T = mutex.withLock { scope.block() }

    /**
     * Workaround worker to fix b/119920965
     */
    private fun enqueueDelayedWorker(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sessionWorkerKeepEnabled",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequest.Builder(workerClass)
                .setInitialDelay(10 * 365, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresCharging(true)
                        .build()
                )
                .build()
        )
    }
}
