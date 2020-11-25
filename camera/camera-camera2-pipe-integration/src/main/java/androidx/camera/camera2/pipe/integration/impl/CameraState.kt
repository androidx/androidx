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

import android.annotation.SuppressLint
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.adapter.ExposureStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.ZoomStateAdapter
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.ExposureState
import androidx.camera.core.ZoomState
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Provider

/**
 * [CameraState] caches and updates based on callbacks from the active CameraGraph.
 */
@SuppressLint("UnsafeExperimentalUsageError")
@CameraScope
class CameraState @Inject constructor(
    private val cameraMetadata: Provider<CameraMetadata>,
) {
    val torchState = MutableLiveData<Int>()
    val zoomState by lazy {
        MutableLiveData<ZoomState>(
            ZoomStateAdapter(
                cameraMetadata.get(),
                1.0f
            )
        )
    }
    val exposureState by lazy {
        MutableLiveData<ExposureState>(
            ExposureStateAdapter(
                cameraMetadata.get(),
                0
            )
        )
    }
}