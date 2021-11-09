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
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Threads
import dagger.Module
import dagger.Provides
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import javax.inject.Singleton

/**
 * Configure and provide a single [Threads] object to other parts of the library.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Module
internal class ThreadConfigModule(private val threadConfig: CameraPipe.ThreadConfig) {
    @Singleton
    @Provides
    fun provideThreads(): Threads {
        val threadIds = atomic(0)
        val cameraThreadPriority =
            Process.THREAD_PRIORITY_DISPLAY + Process.THREAD_PRIORITY_LESS_FAVORABLE
        val defaultExecutor =
            threadConfig.defaultLightweightExecutor ?: Executors.newFixedThreadPool(2) {
                object : Thread(it) {
                    init {
                        val number = threadIds.incrementAndGet().toString().padStart(2, '0')
                        name = "CXCP-$number"
                    }

                    override fun run() {
                        Process.setThreadPriority(cameraThreadPriority)
                        super.run()
                    }
                }
            }
        val defaultDispatcher = defaultExecutor.asCoroutineDispatcher()

        val ioExecutor =
            threadConfig.defaultBackgroundExecutor ?: Executors.newFixedThreadPool(8) {
                object : Thread(it) {
                    init {
                        val number = threadIds.incrementAndGet().toString().padStart(2, '0')
                        name = "CXCP-IO-$number"
                    }
                }
            }
        val ioDispatcher = ioExecutor.asCoroutineDispatcher()

        val cameraHandlerFn =
            {
                threadConfig.defaultCameraHandler?.let { Handler(it.looper) }
                    ?: Handler(
                        HandlerThread("CXCP-Camera2-H").also {
                            it.start()
                        }.looper
                    )
            }
        val cameraExecutorFn = {
            threadConfig.defaultCameraExecutor ?: Executors.newFixedThreadPool(1) {
                object : Thread(it) {
                    init {
                        name = "CXCP-Camera2-E"
                    }

                    override fun run() {
                        Process.setThreadPriority(cameraThreadPriority)
                        super.run()
                    }
                }
            }
        }

        val globalScope = CoroutineScope(
            defaultDispatcher.plus(CoroutineName("CXCP-Pipe"))
        )

        return Threads(
            globalScope = globalScope,
            defaultExecutor = defaultExecutor,
            defaultDispatcher = defaultDispatcher,
            ioExecutor = ioExecutor,
            ioDispatcher = ioDispatcher,
            camera2Handler = cameraHandlerFn,
            camera2Executor = cameraExecutorFn
        )
    }
}