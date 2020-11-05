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
package androidx.camera.camera2.pipe.integration.impl

import android.content.Context
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.impl.Debug
import androidx.camera.camera2.pipe.impl.Log.debug
import androidx.camera.camera2.pipe.impl.Timestamps
import androidx.camera.camera2.pipe.impl.Timestamps.measureNow
import androidx.camera.camera2.pipe.impl.Timestamps.formatMs
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraThreadConfig

/**
 * The CameraPipeCameraFactory is responsible for creating and configuring CameraPipe for CameraX.
 */
class CameraPipeFactory(
    context: Context,
    threadConfig: CameraThreadConfig
) : CameraFactory {
    // Lazily create and configure a CameraPipe instance.
    private val cameraPipe: CameraPipe by lazy {
        Debug.traceStart { "CameraPipeCameraFactory#cameraPipe" }
        val result: CameraPipe?
        val start = Timestamps.now()

        // TODO: CameraPipe should find a way to make sure callbacks are executed on the configured
        //   executors that are provided in `threadConfig`
        debug { "TODO: Use $threadConfig if defined" }

        result = CameraPipe(CameraPipe.Config(appContext = context.applicationContext))
        debug { "Created CameraPipe in ${start.measureNow().formatMs()}" }
        Debug.traceStop()
        result
    }

    init {
        debug { "Created CameraPipeCameraFactory" }
        // TODO: Consider preloading the list of camera ids and metadata.
    }

    override fun getCamera(cameraId: String): CameraInternal {
        // TODO: The CameraInternal object is an facade that covers most of the high level camera
        //   state and interactions. CameraInternal objects are persistent across camera switches.

        return CameraAdaptor(cameraPipe, CameraId(cameraId))
    }

    override fun getAvailableCameraIds(): Set<String> {
        // TODO: This may need some amount of work to limit the returned values well behaved "Front"
        //   and "Back" camera devices.
        return cameraPipe.cameras().findAll().map { it.value }.toSet()
    }

    override fun getCameraManager(): Any? {
        // Note: This object is passed around as an untyped parameter when constructing a few
        // objects (Such as `DeviceSurfaceManagerProvider`). It's better to rely on the parameter
        // passing than to try to turn this object into a singleton.
        return cameraPipe
    }
}