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

package androidx.camera.camera2.adapter

import android.content.Context
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.DurationNs
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.core.Timestamps.measureNow
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.utils.ContextUtil
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.StreamSpecsCalculator

/**
 * The [CameraFactoryProvider] is responsible for creating the root dagger component that is used to
 * share resources across Camera instances. There should generally be one [CameraFactoryProvider]
 * instance per CameraX instance.
 */
public class CameraFactoryProvider(
    private val sharedCameraPipe: CameraPipe? = null,
    private val sharedAppContext: Context? = null,
    private val sharedThreadConfig: CameraThreadConfig? = null,
) : CameraFactory.Provider {
    private val sharedInteropCallbacks = CameraInteropStateCallbackRepository()

    override fun newInstance(
        context: Context,
        threadConfig: CameraThreadConfig,
        availableCamerasLimiter: CameraSelector?,
        cameraOpenRetryMaxTimeoutInMs: Long,
        cameraXConfig: CameraXConfig?,
        streamSpecsCalculator: StreamSpecsCalculator,
    ): CameraFactory {

        val openRetryMaxTimeout =
            if (cameraOpenRetryMaxTimeoutInMs == -1L) null
            else DurationNs(cameraOpenRetryMaxTimeoutInMs)

        val lazyCameraPipe = lazy {
            if (sharedCameraPipe != null) {
                Camera2Logger.debug { "Using shared a $sharedCameraPipe instance." }
                sharedCameraPipe
            } else {
                createCameraPipe(context, threadConfig, openRetryMaxTimeout)
            }
        }

        return CameraFactoryAdapter(
            lazyCameraPipe,
            sharedAppContext ?: context,
            sharedThreadConfig ?: threadConfig,
            sharedInteropCallbacks,
            availableCamerasLimiter,
            streamSpecsCalculator,
            cameraXConfig ?: CameraXConfig.Builder().build(),
        )
    }

    private fun createCameraPipe(
        context: Context,
        threadConfig: CameraThreadConfig,
        openRetryMaxTimeout: DurationNs?,
    ): CameraPipe =
        Debug.trace("Create CameraPipe") {
            val timeSource = SystemTimeSource()
            val start = Timestamps.now(timeSource)

            val cameraPipe =
                CameraPipe(
                    CameraPipe.Config(
                        appContext = ContextUtil.getPersistentApplicationContext(context),
                        threadConfig =
                            CameraPipe.ThreadConfig(
                                // This executor should be single-threaded or a sequential executor
                                // to avoid bugs on various API levels (29 ~ 34). See b/446771606
                                // fore more details.
                                defaultCameraExecutor =
                                    CameraXExecutors.newSequentialExecutor(
                                        threadConfig.cameraExecutor
                                    )
                            ),
                        cameraInteropConfig =
                            CameraPipe.CameraInteropConfig(
                                sharedInteropCallbacks.deviceStateCallback,
                                sharedInteropCallbacks.sessionStateCallback,
                                openRetryMaxTimeout,
                            ),
                    )
                )
            Camera2Logger.debug {
                "Created CameraPipe in ${start.measureNow(timeSource).formatMs()}"
            }
            cameraPipe
        }
}
