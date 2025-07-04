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

package androidx.camera.lifecycle.samples

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn as JavaOptIn
import androidx.annotation.Sampled
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FPS_60
import androidx.camera.core.featuregroup.GroupableFeature.Companion.HDR_HLG10
import androidx.camera.core.featuregroup.GroupableFeature.Companion.PREVIEW_STABILIZATION
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.LifecycleCameraProvider
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

@Sampled
@JavaOptIn(ExperimentalCameraProviderConfiguration::class)
suspend fun configureAndCreateInstances(
    context1: Context,
    context2: Context,
    lifecycleOwner1: LifecycleOwner,
    lifecycleOwner2: LifecycleOwner,
    executor1: Executor,
    executor2: Executor,
    useCase1: UseCase,
    useCase2: UseCase,
) {
    val cameraProvider1 =
        LifecycleCameraProvider.createInstance(
            context1,
            CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                .setCameraExecutor(executor1)
                .build(),
        )
    cameraProvider1.bindToLifecycle(lifecycleOwner1, CameraSelector.DEFAULT_FRONT_CAMERA, useCase1)

    // Switch to different lifecycle owner.

    val cameraProvider2 =
        LifecycleCameraProvider.createInstance(
            context2,
            CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
                .setCameraExecutor(executor2)
                .build(),
        )
    cameraProvider2.bindToLifecycle(lifecycleOwner2, CameraSelector.DEFAULT_BACK_CAMERA, useCase2)
}

@Sampled
@OptIn(ExperimentalSessionConfig::class)
fun bindSessionConfigToLifecycle(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    effect1: CameraEffect,
    effect2: CameraEffect,
) {
    val preview =
        Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
    val imageCapture = ImageCapture.Builder().build()
    val sessionConfig =
        SessionConfig(
            useCases = listOf(preview, imageCapture),
            viewPort = previewView.getViewPort(preview.getTargetRotation()),
            effects = listOf(effect1),
        )
    // Starts the camera with the given effect and viewPort when the lifecycleOwner is started.
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        sessionConfig,
    )

    // To apply a different effect, unbind the previous SessionConfig and bind the new SessionConfig
    // with the new effect.
    val sessionConfigNewEffect =
        SessionConfig(
            useCases = listOf(preview, imageCapture),
            viewPort = previewView.getViewPort(preview.getTargetRotation()),
            effects = listOf(effect2),
        )
    // Make sures to unbind the previous sessionConfig before binding the new one
    cameraProvider.unbind(sessionConfig)
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        sessionConfigNewEffect,
    )
}

@Sampled
@OptIn(ExperimentalSessionConfig::class)
fun bindSessionConfigWithFeatureGroupsToLifecycle(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    useCases: List<UseCase>,
) {
    // Starts the camera with feature groups configured.
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        // HDR is mandatory in this camera configuration and an exception will be thrown if it's not
        // supported. 60 FPS and preview stabilization are optional and used if they are also
        // supported, with the 60 FPS having higher priority over preview stabilization.
        SessionConfig(
                useCases = useCases,
                requiredFeatureGroup = setOf(HDR_HLG10),
                preferredFeatureGroup = listOf(FPS_60, PREVIEW_STABILIZATION),
            )
            .apply {
                setFeatureSelectionListener { features ->
                    Log.d(
                        "TAG",
                        "Features selected as per priority and device capabilities: $features",
                    )

                    // Update app UI based on the selected features if required
                }
            },
    )
}
