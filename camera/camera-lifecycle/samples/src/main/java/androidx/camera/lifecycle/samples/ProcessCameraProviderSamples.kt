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

package androidx.camera.lifecycle.samples

import android.content.Context
import android.os.Handler
import androidx.annotation.Sampled
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraXConfig
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.ProcessCameraProvider.Companion.configureInstance
import androidx.concurrent.futures.await
import java.util.concurrent.Executor

@Sampled
fun getCameraXConfigSample(executor: Executor, handler: Handler) {
    @Override
    fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setCameraExecutor(executor)
            .setSchedulerHandler(handler)
            .build()
    }
}

@androidx.annotation.OptIn(ExperimentalCameraProviderConfiguration::class)
@Sampled
fun configureAndGetInstanceSample(executor: Executor, scheduleHandler: Handler) {
    var configured = false // Whether the camera provider has been configured or not.

    @androidx.annotation.OptIn(ExperimentalCameraProviderConfiguration::class)
    suspend fun getInstance(context: Context): ProcessCameraProvider {
        synchronized(CameraProvider::class.java) {
            if (!configured) {
                configured = true
                configureInstance(
                    CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                        .setCameraExecutor(executor)
                        .setSchedulerHandler(scheduleHandler)
                        .build()
                )
            }
        }
        return ProcessCameraProvider.getInstance(context).await()
    }
}
