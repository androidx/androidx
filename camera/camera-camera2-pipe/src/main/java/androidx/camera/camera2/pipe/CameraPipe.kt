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

package androidx.camera.camera2.pipe

import android.content.Context
import android.os.HandlerThread
import androidx.camera.camera2.pipe.impl.CameraGraphConfigModule
import androidx.camera.camera2.pipe.impl.CameraPipeComponent
import androidx.camera.camera2.pipe.impl.CameraPipeConfigModule
import androidx.camera.camera2.pipe.impl.DaggerCameraPipeComponent
import kotlinx.atomicfu.atomic

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
class CameraPipe(config: Config) {
    private val debugId = cameraPipeIds.incrementAndGet()
    private val component: CameraPipeComponent = DaggerCameraPipeComponent.builder()
        .cameraPipeConfigModule(CameraPipeConfigModule(config))
        .build()

    /**
     * This creates a new [CameraGraph] that can be used to interact with a single Camera on the
     * device. Multiple [CameraGraph]s can be created, but only one should be active at a time.
     */
    fun create(config: CameraGraph.Config): CameraGraph {
        return component.cameraGraphComponentBuilder()
            .cameraGraphConfigModule(CameraGraphConfigModule(config))
            .build()
            .cameraGraph()
    }

    /**
     * This provides access to information about the available cameras on the device.
     */
    fun cameras(): Cameras {
        return component.cameras()
    }

    /**
     * This is the application level configuration for [CameraPipe]. Nullable values are optional
     * and reasonable defaults will be provided if the values are not specified.
     */
    data class Config(
        val appContext: Context,
        val cameraThread: HandlerThread? = null
    )

    override fun toString(): String = "CameraPipe-$debugId"
}
