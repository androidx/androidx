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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe

import android.content.Context
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.config.CameraGraphConfigModule
import androidx.camera.camera2.pipe.config.CameraPipeComponent
import androidx.camera.camera2.pipe.config.CameraPipeConfigModule
import androidx.camera.camera2.pipe.config.DaggerCameraPipeComponent
import androidx.camera.camera2.pipe.config.DaggerExternalCameraPipeComponent
import androidx.camera.camera2.pipe.config.ExternalCameraGraphComponent
import androidx.camera.camera2.pipe.config.ExternalCameraGraphConfigModule
import androidx.camera.camera2.pipe.config.ExternalCameraPipeComponent
import androidx.camera.camera2.pipe.config.ThreadConfigModule
import kotlinx.atomicfu.atomic
import java.util.concurrent.Executor

internal val cameraPipeIds = atomic(0)

/**
 * [CameraPipe] is the top level scope for all interactions with a Camera2 camera.
 *
 * Under most circumstances an application should only ever have a single instance of a [CameraPipe]
 * object as each instance will cache expensive calls and operations with the Android Camera
 * framework. In addition to the caching behaviors it will optimize the access and configuration of
 * [android.hardware.camera2.CameraDevice] and [android.hardware.camera2.CameraCaptureSession] via
 * the [CameraGraph] interface.
 */
public class CameraPipe(config: Config, threadConfig: ThreadConfig = ThreadConfig()) {
    private val debugId = cameraPipeIds.incrementAndGet()
    private val component: CameraPipeComponent = DaggerCameraPipeComponent.builder()
        .cameraPipeConfigModule(CameraPipeConfigModule(config))
        .threadConfigModule(ThreadConfigModule(threadConfig))
        .build()

    /**
     * This creates a new [CameraGraph] that can be used to interact with a single Camera on the
     * device. Multiple [CameraGraph]s can be created, but only one should be active at a time.
     */
    public fun create(config: CameraGraph.Config): CameraGraph {
        return component.cameraGraphComponentBuilder()
            .cameraGraphConfigModule(CameraGraphConfigModule(config))
            .build()
            .cameraGraph()
    }

    /**
     * This provides access to information about the available cameras on the device.
     */
    public fun cameras(): CameraDevices {
        return component.cameras()
    }

    /**
     * Application level configuration for [CameraPipe]. Nullable values are optional and
     * reasonable defaults will be provided if values are not specified.
     */
    public data class Config(
        val appContext: Context,
        val threadConfig: ThreadConfig = ThreadConfig()
    )

    /**
     * Application level configuration for default thread and executors. If set, these executors
     * will be used to run asynchronous background work across [CameraPipe].
     *
     * - [defaultLightweightExecutor] is used to run fast, non-blocking, lightweight tasks.
     * - [defaultBackgroundExecutor] is used to run blocking and/or io bound tasks.
     * - [defaultCameraExecutor] is used on newer API versions to interact with CameraAPIs. This is
     *   split into a separate field since many camera operations are extremely latency sensitive.
     * - [defaultCameraHandler] is used on older API versions to interact with CameraAPIs. This is
     *   split into a separate field since many camera operations are extremely latency sensitive.
     */
    public data class ThreadConfig(
        val defaultLightweightExecutor: Executor? = null,
        val defaultBackgroundExecutor: Executor? = null,
        val defaultCameraExecutor: Executor? = null,
        val defaultCameraHandler: HandlerThread? = null
    )

    override fun toString(): String = "CameraPipe-$debugId"

    /**
     * External may be used if the underlying implementation needs to delegate to another library
     * or system.
     */
    class External(threadConfig: ThreadConfig = ThreadConfig()) {
        private val component: ExternalCameraPipeComponent = DaggerExternalCameraPipeComponent
            .builder()
            .threadConfigModule(ThreadConfigModule(threadConfig))
            .build()

        /**
         * This creates a new [CameraGraph] instance that is configured to use an externally
         * defined [RequestProcessor].
         *
         * TODO: Consider changing cameraDevices to be a single device + physical metadata.
         */
        public fun create(
            config: CameraGraph.Config,
            cameraMetadata: CameraMetadata,
            requestProcessor: RequestProcessor
        ): CameraGraph {
            check(config.camera == cameraMetadata.camera) {
                "Invalid camera config: ${config.camera} does not match ${cameraMetadata.camera}"
            }
            val componentBuilder = component.cameraGraphBuilder()
            val component: ExternalCameraGraphComponent = componentBuilder
                .externalCameraGraphConfigModule(
                    ExternalCameraGraphConfigModule(
                        config,
                        cameraMetadata,
                        requestProcessor
                    )
                ).build()
            return component.cameraGraph()
        }
    }
}
