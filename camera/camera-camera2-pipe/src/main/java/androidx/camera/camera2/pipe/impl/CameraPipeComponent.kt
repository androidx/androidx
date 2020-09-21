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

package androidx.camera.camera2.pipe.impl

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.Cameras
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Reusable
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class ForCameraPipe

@Singleton
@Component(
    modules = [
        CameraPipeModules::class,
        CameraPipeConfigModule::class
    ]
)
interface CameraPipeComponent {
    fun cameraGraphComponentBuilder(): CameraGraphComponent.Builder
    fun cameras(): Cameras
}

@Module(
    subcomponents = [CameraGraphComponent::class]
)
class CameraPipeConfigModule(private val config: CameraPipe.Config) {
    @Provides
    fun provideCameraPipeConfig(): CameraPipe.Config = config
}

@Module
abstract class CameraPipeModules {
    @Binds
    abstract fun bindCameras(impl: CamerasImpl): Cameras

    companion object {
        @Provides
        fun provideContext(config: CameraPipe.Config): Context = config.appContext

        @Reusable
        @Provides
        fun provideCameraManager(context: Context): CameraManager =
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        @Singleton
        @Provides
        fun provideCameraPipeThreads(config: CameraPipe.Config): Threads {

            val threadIds = atomic(0)
            val cameraExecutor = Executors.newFixedThreadPool(2) {
                object : Thread(it) {
                    init {
                        val number = threadIds.incrementAndGet().toString().padStart(2, '0')
                        name = "CXCP-$number"
                    }

                    override fun run() {
                        Process.setThreadPriority(
                            Process.THREAD_PRIORITY_DISPLAY + Process.THREAD_PRIORITY_LESS_FAVORABLE
                        )
                        super.run()
                    }
                }
            }
            val cameraDispatcher = cameraExecutor.asCoroutineDispatcher()
            val lazyCameraHandlerThread = lazy {
                HandlerThread("CXCP-Hndlr").also {
                    it.start()
                }
            }
            val cameraHandlerProvider =
                {
                    @Suppress("DEPRECATION")
                    config.cameraThread?.let { Handler(it.looper) } ?: Handler(
                        lazyCameraHandlerThread.value.looper
                    )
                }
            val ioExecutor = Executors.newFixedThreadPool(8) {
                object : Thread(it) {
                    init {
                        val number = threadIds.incrementAndGet().toString().padStart(2, '0')
                        name = "CXCP-IO-$number"
                    }
                }
            }
            val ioDispatcher = ioExecutor.asCoroutineDispatcher()

            val globalScope = CoroutineScope(
                cameraDispatcher.plus(
                    CoroutineName
                    ("CXCP-Pipe")
                )
            )

            return Threads(
                globalScope = globalScope,
                defaultExecutor = cameraExecutor,
                defaultDispatcher = cameraDispatcher,
                ioExecutor = ioExecutor,
                ioDispatcher = ioDispatcher,
                handlerBuilder = cameraHandlerProvider
            )
        }
    }
}