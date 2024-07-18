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
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

@JvmDefaultWithCompatibility
/**
 * [SessionManager] is the entrypoint for Glance surfaces to start a session worker that will handle
 * their composition.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SessionManager {
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

    val keyParam: String
        get() = "KEY"
}

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val GlanceSessionManager: SessionManager = SessionManagerImpl(SessionWorker::class.java)

internal class SessionManagerImpl(
    private val workerClass: Class<out ListenableWorker>
) : SessionManager {
    private val sessions = mutableMapOf<String, Session>()
    companion object {
        private const val TAG = "GlanceSessionManager"
        private const val DEBUG = false
    }

    override suspend fun startSession(context: Context, session: Session) {
        if (DEBUG) Log.d(TAG, "startSession(${session.key})")
        synchronized(sessions) {
            sessions.put(session.key, session)
        }?.close()
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

    override fun getSession(key: String): Session? = synchronized(sessions) {
        sessions[key]
    }

    override suspend fun isSessionRunning(context: Context, key: String) =
        (WorkManager.getInstance(context).getWorkInfosForUniqueWork(key).await()
            .any { it.state == WorkInfo.State.RUNNING } && synchronized(sessions) {
            sessions.containsKey(key)
        }).also {
            if (DEBUG) Log.d(TAG, "isSessionRunning($key) == $it")
        }

    override suspend fun closeSession(key: String) {
        if (DEBUG) Log.d(TAG, "closeSession($key)")
        synchronized(sessions) {
            sessions.remove(key)
        }?.close()
    }

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
