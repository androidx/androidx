/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.config

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.AndroidThreads
import androidx.camera.camera2.pipe.core.AndroidThreads.asCachedThreadPool
import androidx.camera.camera2.pipe.core.AndroidThreads.asFixedSizeThreadPool
import androidx.camera.camera2.pipe.core.AndroidThreads.asScheduledThreadPool
import androidx.camera.camera2.pipe.core.AndroidThreads.withAndroidPriority
import androidx.camera.camera2.pipe.core.AndroidThreads.withPrefix
import androidx.camera.camera2.pipe.core.Threads
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor

/** Configure and provide a single [Threads] object to other parts of the library. */
@Module
internal class ThreadConfigModule(private val threadConfig: CameraPipe.ThreadConfig) {
    // Lightweight executors are for CPU bound work that should take less than ~10ms to operate and
    // do not block the calling thread.
    private val lightweightThreadCount: Int =
        maxOf(4, Runtime.getRuntime().availableProcessors() - 2)

    // Background thread count is for operations that are not latency sensitive and may take more
    // than a few milliseconds to run.
    private val backgroundThreadCount: Int = 4

    // High priority threads for interrupt and rendering sensitive operations. This is set to have
    // slightly (1) lower priority than the display rendering thread should have.
    private val cameraThreadPriority: Int =
        Process.THREAD_PRIORITY_DISPLAY + Process.THREAD_PRIORITY_LESS_FAVORABLE

    // Default thread priorities are slightly higher than the default priorities since most camera
    // operations are latency sensitive and should take precedence over other background work.
    private val defaultThreadPriority: Int =
        Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_MORE_FAVORABLE

    @Singleton
    @Provides
    fun provideThreads(): Threads {
        val testOnlyDispatcher = threadConfig.testOnlyDispatcher
        val testOnlyScope = threadConfig.testOnlyScope
        if (testOnlyDispatcher != null && testOnlyScope != null) {
            return provideTestOnlyThreads(testOnlyDispatcher, testOnlyScope)
        }
        check(testOnlyDispatcher == null || testOnlyScope == null) {
            "testOnlyDispatcher and testOnlyScope must be specified together!"
        }

        val blockingExecutor =
            threadConfig.defaultBlockingExecutor
                ?: AndroidThreads.factory
                    .withPrefix("CXCP-IO-")
                    .withAndroidPriority(defaultThreadPriority)
                    .asCachedThreadPool()
        val blockingDispatcher = blockingExecutor.asCoroutineDispatcher()

        val backgroundExecutor =
            threadConfig.defaultBackgroundExecutor
                ?: AndroidThreads.factory
                    .withPrefix("CXCP-BG-")
                    .withAndroidPriority(defaultThreadPriority)
                    .asScheduledThreadPool(backgroundThreadCount)
        val backgroundDispatcher = backgroundExecutor.asCoroutineDispatcher()

        val lightweightExecutor =
            threadConfig.defaultLightweightExecutor
                ?: AndroidThreads.factory
                    .withPrefix("CXCP-")
                    .withAndroidPriority(cameraThreadPriority)
                    .asScheduledThreadPool(lightweightThreadCount)
        val lightweightDispatcher = lightweightExecutor.asCoroutineDispatcher()

        val cameraHandlerFn = {
            val handlerThread =
                threadConfig.defaultCameraHandler
                    ?: HandlerThread("CXCP-Camera-H", cameraThreadPriority).also { it.start() }
            Handler(handlerThread.looper)
        }
        val cameraExecutorFn = {
            threadConfig.defaultCameraExecutor
                ?: AndroidThreads.factory
                    .withPrefix("CXCP-Camera-E")
                    .withAndroidPriority(cameraThreadPriority)
                    .asFixedSizeThreadPool(1)
        }

        val globalScope =
            CoroutineScope(SupervisorJob() + lightweightDispatcher + CoroutineName("CXCP"))

        return Threads(
            globalScope = globalScope,
            blockingExecutor = blockingExecutor,
            blockingDispatcher = blockingDispatcher,
            backgroundExecutor = backgroundExecutor,
            backgroundDispatcher = backgroundDispatcher,
            lightweightExecutor = lightweightExecutor,
            lightweightDispatcher = lightweightDispatcher,
            camera2Handler = cameraHandlerFn,
            camera2Executor = cameraExecutorFn
        )
    }

    private fun provideTestOnlyThreads(
        testDispatcher: CoroutineDispatcher,
        testScope: CoroutineScope
    ): Threads {
        val testExecutor = testDispatcher.asExecutor()

        // TODO: This should delegate to the testDispatcher instead of using a HandlerThread.
        val cameraHandlerFn = {
            val handlerThread =
                HandlerThread("CXCP-Camera-H", cameraThreadPriority).also { it.start() }
            Handler(handlerThread.looper)
        }

        return Threads(
            globalScope = testScope,
            blockingExecutor = testExecutor,
            blockingDispatcher = testDispatcher,
            backgroundExecutor = testExecutor,
            backgroundDispatcher = testDispatcher,
            lightweightExecutor = testExecutor,
            lightweightDispatcher = testDispatcher,
            camera2Handler = cameraHandlerFn,
            camera2Executor = { testExecutor }
        )
    }
}
