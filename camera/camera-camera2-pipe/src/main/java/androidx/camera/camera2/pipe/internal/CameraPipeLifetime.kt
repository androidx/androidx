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

package androidx.camera.camera2.pipe.internal

import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.config.CameraPipeJob
import androidx.camera.camera2.pipe.core.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * CameraPipeLifetime is an internal class designed to facilitate CameraPipe shutdown. It does so in
 * an ordered manner:
 * 1. First, we shut down the camera backends (which closes all cameras).
 * 2. Then we cancel coroutine scopes.
 * 3. Finally, with scopes cancelled, we shut down the threads CameraPipe created.
 *
 * Internal classes that require a CameraPipe-level shutdown routine should invoke
 * [addShutdownAction] to register their respective shutdown action and type.
 */
@Singleton
internal class CameraPipeLifetime
@Inject
constructor(@CameraPipeJob private val cameraPipeJob: Job) {
    private val cameraLock = Any()
    @GuardedBy("cameraLock") private var isCameraShutdown = false
    @GuardedBy("cameraLock") private val cameraShutdownActions = mutableListOf<Runnable>()

    private val scopeLock = Any()
    @GuardedBy("scopeLock") private var isScopeShutdown = false
    @GuardedBy("scopeLock") private val scopeShutdownActions = mutableListOf<Runnable>()

    private val threadLock = Any()
    @GuardedBy("threadLock") private var isThreadShutdown = false
    @GuardedBy("threadLock") private val threadShutdownActions = mutableListOf<Runnable>()

    fun addShutdownAction(shutdownType: ShutdownType, shutdownAction: Runnable) {
        val success =
            when (shutdownType) {
                ShutdownType.CAMERA -> addCameraShutdownAction(shutdownAction)
                ShutdownType.SCOPE -> addScopeShutdownAction(shutdownAction)
                ShutdownType.THREAD -> addThreadShutdownAction(shutdownAction)
            }
        if (!success) {
            Log.error {
                "CameraPipeLifetime already shut down. This is unexpected. " +
                    "Executing $shutdownType shutdown action immediately..."
            }
            shutdownAction.run()
        }
    }

    private fun addCameraShutdownAction(shutdownAction: Runnable) =
        synchronized(cameraLock) {
            if (isCameraShutdown) {
                false
            } else {
                cameraShutdownActions.add(shutdownAction)
            }
        }

    private fun addScopeShutdownAction(shutdownAction: Runnable) =
        synchronized(scopeLock) {
            if (isScopeShutdown) {
                false
            } else {
                scopeShutdownActions.add(shutdownAction)
            }
        }

    private fun addThreadShutdownAction(shutdownAction: Runnable) =
        synchronized(threadLock) {
            if (isThreadShutdown) {
                false
            } else {
                threadShutdownActions.add(shutdownAction)
            }
        }

    fun shutdown() {
        shutdownCamera()
        shutdownScope()
        shutdownThread()
    }

    private fun shutdownCamera() =
        synchronized(cameraLock) {
            Log.debug { "Shutting down cameras..." }
            for (shutdownAction in cameraShutdownActions) {
                shutdownAction.run()
            }
        }

    private fun shutdownScope() =
        synchronized(scopeLock) {
            Log.debug { "Shutting down scopes..." }
            for (shutdownAction in scopeShutdownActions) {
                shutdownAction.run()
            }
            runBlocking {
                withTimeoutOrNull(CAMERA_PIPE_JOB_CANCEL_TIMEOUT_MS) {
                    Log.debug { "Cancelling CameraPipe root Job..." }
                    cameraPipeJob.cancelAndJoin()
                }
            }
        }

    private fun shutdownThread() =
        synchronized(threadLock) {
            Log.debug { "Shutting down threads..." }
            for (shutdownAction in threadShutdownActions) {
                shutdownAction.run()
            }
        }

    internal enum class ShutdownType {
        CAMERA,
        SCOPE,
        THREAD,
    }

    companion object {
        private const val CAMERA_PIPE_JOB_CANCEL_TIMEOUT_MS = 3_000L
    }
}
