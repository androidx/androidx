/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.testing.impl

import androidx.annotation.VisibleForTesting
import androidx.camera.core.CameraExecutor
import androidx.camera.core.CameraXConfig
import java.util.concurrent.Executor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll

/**
 * An executor which bypasses all tasks to a [CameraExecutor] while keeping track of the submitted
 * tasks which allows waiting for them to be completed when required.
 *
 * This enables tests to wait for camera thread to be complete all ongoing works up to some point
 * for some precise testing requirements. Note that `CameraExecutor` is the default executor that
 * CameraX uses when user doesn't set any executor via [CameraXConfig.Builder.setCameraExecutor].
 */
@VisibleForTesting
public class CameraTaskTrackingExecutor : Executor {
    private val lock = Object()
    private val cameraTasks = mutableListOf<CompletableDeferred<Unit>>()
    private val cameraExecutor = CameraExecutor()

    override fun execute(command: Runnable?) {
        if (command == null) {
            return
        }

        // Pass all tasks to background camera thread after adding a corresponding deferred
        val completableDeferred = CompletableDeferred<Unit>()
        synchronized(lock) { cameraTasks.add(completableDeferred) }
        cameraExecutor.execute {
            try {
                command.run()
            } finally {
                completableDeferred.complete(Unit)
            }
        }
    }

    /** Waits for all submitted tasks to be completed. */
    public suspend fun awaitIdle() {
        synchronized(lock) { cameraTasks }.awaitAll()
    }
}
