/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.integration.macrobenchmark.target

import android.content.Context
import android.content.Intent
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ExperimentalLensFacing
import androidx.camera.core.SessionConfig
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner

object CameraXSetup {
    fun Intent.toCameraXConfig() =
        when (extras?.getString("camerax_config")) {
            Camera2Config::class.simpleName -> Camera2Config.defaultConfig()
            else -> Camera2Config.defaultConfig()
        }

    @androidx.annotation.OptIn(ExperimentalLensFacing::class)
    fun Intent.toCameraSelector() =
        when (extras?.getInt("lens")) {
            CameraSelector.LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            CameraSelector.LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.LENS_FACING_EXTERNAL ->
                CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL)
                    .build()
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }

    @androidx.annotation.OptIn(ExperimentalCameraProviderConfiguration::class)
    suspend fun initCameraX(
        cameraXConfig: CameraXConfig,
        cameraSelector: CameraSelector,
        context: Context,
        lifecycleOwner: LifecycleOwner,
        sessionConfig: SessionConfig,
    ): Camera {
        ProcessCameraProvider.configureInstance(cameraXConfig)
        return ProcessCameraProvider.awaitInstance(context)
            .bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
    }
}
