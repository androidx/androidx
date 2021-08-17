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

package androidx.camera.camera2.pipe.integration.compat

import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE
import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP
import android.hardware.camera2.CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
import android.util.Range
import android.util.Rational
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import dagger.Binds
import dagger.Module
import javax.inject.Inject

interface EvCompCompat {
    val supported: Boolean
    val range: Range<Int>
    val step: Rational

    fun apply(
        evCompIndex: Int,
        camera: UseCaseCamera
    )

    @Module
    abstract class Bindings {
        @Binds
        abstract fun bindEvCompImpl(impl: EvCompImpl): EvCompCompat
    }
}

internal val EMPTY_RANGE = Range(0, 0)

@CameraScope
class EvCompImpl @Inject constructor(
    private val cameraProperties: CameraProperties
) : EvCompCompat {
    override val supported: Boolean
        get() = range.upper != 0 && range.lower != 0
    override val range: Range<Int> by lazy {
        cameraProperties.metadata.getOrDefault(CONTROL_AE_COMPENSATION_RANGE, EMPTY_RANGE)
    }
    override val step: Rational
        get() = if (!supported) {
            Rational.ZERO
        } else {
            cameraProperties.metadata[CONTROL_AE_COMPENSATION_STEP]!!
        }

    override fun apply(evCompIndex: Int, camera: UseCaseCamera) {
        camera.setParameterAsync(CONTROL_AE_EXPOSURE_COMPENSATION, evCompIndex)
    }
}