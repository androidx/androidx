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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.adapter

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.util.Range
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.core.ExposureState

internal val EMPTY_RANGE = Range(0, 0)

/** Adapt [ExposureState] to a [CameraMetadata] instance. */
@SuppressLint("UnsafeOptInUsageError")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class ExposureStateAdapter(
    private val cameraProperties: CameraProperties,
    private val exposureCompensation: Int
) : ExposureState {
    override fun isExposureCompensationSupported(): Boolean {
        val range = exposureCompensationRange
        return range.lower != 0 && range.upper != 0
    }

    override fun getExposureCompensationIndex(): Int = exposureCompensation
    override fun getExposureCompensationStep(): Rational {
        if (!isExposureCompensationSupported) {
            return Rational.ZERO
        }
        return cameraProperties.metadata[CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP]!!
    }

    override fun getExposureCompensationRange(): Range<Int> {
        return cameraProperties.metadata[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]
            ?: EMPTY_RANGE
    }
}