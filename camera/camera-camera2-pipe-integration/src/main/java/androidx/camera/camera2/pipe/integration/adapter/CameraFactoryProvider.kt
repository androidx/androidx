/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.content.Context
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.DurationNs
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.core.Timestamps.measureNow
import androidx.camera.camera2.pipe.integration.impl.CameraInteropStateCallbackRepository
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.internal.StreamSpecsCalculator

/**
 * The [CameraFactoryProvider] is responsible for creating the root dagger component that is used to
 * share resources across Camera instances. There should generally be one [CameraFactoryProvider]
 * instance per CameraX instance.
 */
public class CameraFactoryProvider(
    private val sharedAppContext: Context? = null,
    private val sharedThreadConfig: CameraThreadConfig? = null,
) : CameraFactory.Provider {
    private val sharedInteropCallbacks = CameraInteropStateCallbackRepository()

    override fun newInstance(
        context: Context,
        threadConfig: CameraThreadConfig,
        availableCamerasLimiter: CameraSelector?,
        cameraOpenRetryMaxTimeoutInMs: Long,
        streamSpecsCalculator: StreamSpecsCalculator,
    ): CameraFactory {

        val openRetryMaxTimeout =
            if (cameraOpenRetryMaxTimeoutInMs != -1L) null
            else DurationNs(cameraOpenRetryMaxTimeoutInMs)

        val lazyCameraPipe = lazy { createCameraPipe(context, threadConfig, openRetryMaxTimeout) }

        return CameraFactoryAdapter(
            lazyCameraPipe,
            sharedAppContext ?: context,
            sharedThreadConfig ?: threadConfig,
            sharedInteropCallbacks,
            availableCamerasLimiter,
            streamSpecsCalculator,
        )
    }

    private fun createCameraPipe(
        context: Context,
        threadConfig: CameraThreadConfig,
        openRetryMaxTimeout: DurationNs?,
    ): CameraPipe {
        Debug.traceStart { "Create CameraPipe" }
        val timeSource = SystemTimeSource()
        val start = Timestamps.now(timeSource)

        val cameraPipe =
            CameraPipe(
                CameraPipe.Config(
                    appContext = context.applicationContext,
                    threadConfig =
                        CameraPipe.ThreadConfig(
                            defaultCameraExecutor = threadConfig.cameraExecutor
                        ),
                    cameraInteropConfig =
                        CameraPipe.CameraInteropConfig(
                            sharedInteropCallbacks.deviceStateCallback,
                            sharedInteropCallbacks.sessionStateCallback,
                            openRetryMaxTimeout,
                        ),
                )
            )
        Log.debug { "Created CameraPipe in ${start.measureNow(timeSource).formatMs()}" }
        Debug.traceStop()
        return cameraPipe
    }
}
