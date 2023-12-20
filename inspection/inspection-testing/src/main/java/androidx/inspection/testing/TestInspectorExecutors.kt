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

package androidx.inspection.testing

import android.os.Handler
import android.os.HandlerThread
import androidx.inspection.InspectorExecutors
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlinx.coroutines.Job

/**
 * Test Inspector Executors.
 *
 * HandlerThread created for inspector will quit once parent job completes.
 */
class TestInspectorExecutors(
    parentJob: Job,
    ioExecutor: Executor? = null
) : InspectorExecutors {
    private val handlerThread = HandlerThread("Test Inspector Handler Thread")
    private val handler: Handler
    private val ioExecutor: Executor

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        parentJob.invokeOnCompletion {
            handlerThread.looper.quitSafely()
        }
        this.ioExecutor = ioExecutor ?: Executors.newFixedThreadPool(4).also { executor ->
            parentJob.invokeOnCompletion {
                executor.shutdown()
            }
        }
    }

    override fun handler() = handler

    override fun primary(): Executor = Executor {
        if (!handler.post(it)) {
            throw RejectedExecutionException()
        }
    }

    override fun io() = ioExecutor
}
