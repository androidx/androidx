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

package androidx.camera.camera2.pipe.integration.adapter

import android.annotation.SuppressLint
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.EvCompControl
import androidx.camera.camera2.pipe.integration.impl.TorchControl
import androidx.camera.camera2.pipe.integration.impl.ZoomControl
import androidx.camera.core.ExposureState
import androidx.camera.core.ZoomState
import androidx.lifecycle.LiveData
import javax.inject.Inject

/**
 * [CameraControlStateAdapter] caches and updates based on callbacks from the active CameraGraph.
 */
@SuppressLint("UnsafeOptInUsageError")
@CameraScope
public class CameraControlStateAdapter
@Inject
constructor(
    private val zoomControl: ZoomControl,
    private val evCompControl: EvCompControl,
    private val torchControl: TorchControl,
) {
    public val torchStateLiveData: LiveData<Int>
        get() = torchControl.torchStateLiveData

    public val zoomStateLiveData: LiveData<ZoomState>
        get() = zoomControl.zoomStateLiveData

    public val exposureState: ExposureState
        get() = evCompControl.exposureState
}
