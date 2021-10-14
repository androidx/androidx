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

package androidx.camera.camera2.pipe.integration.impl

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.adapter.EvCompValue
import androidx.camera.camera2.pipe.integration.compat.EvCompCompat
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import javax.inject.Inject

private const val DEFAULT_EXPOSURE_COMPENSATION = 0

/**
 * Implementation of Exposure compensation control, it implements the functionality of
 * [CameraControl.setExposureCompensationIndex].
 *
 * The [CameraControl.setExposureCompensationIndex] can only allow to run one task at the same
 * time, it will cancel the incomplete task if a new task is requested.
 * The task will fail with [CameraControl.OperationCanceledException] if the camera is
 * closed.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
class EvCompControl @Inject constructor(
    private val compat: EvCompCompat,
) : UseCaseCameraControl {
    private var evCompIndex = DEFAULT_EXPOSURE_COMPENSATION
        set(value) {
            field = value
            exposureState = exposureState.updateIndex(value)
        }
    var exposureState = EvCompValue(
        compat.supported,
        evCompIndex,
        compat.range,
        compat.step,
    )

    private var _useCaseCamera: UseCaseCamera? = null
    override var useCaseCamera: UseCaseCamera?
        get() = _useCaseCamera
        set(value) {
            _useCaseCamera = value
            updateAsync(evCompIndex)
        }

    override fun reset() {
        evCompIndex = 0
        compat.stopRunningTask()
    }

    fun updateAsync(exposureIndex: Int): Deferred<Int> {
        if (!compat.supported) {
            return createFailureResult(
                IllegalArgumentException("ExposureCompensation is not supported")
            )
        }

        if (!compat.range.contains(exposureIndex)) {
            return createFailureResult(
                IllegalArgumentException(
                    "Requested ExposureCompensation $exposureIndex is not within valid range " +
                        "[${compat.range.upper} .. ${compat.range.lower}]"
                )
            )
        }

        useCaseCamera?.let {
            evCompIndex = exposureIndex
            return compat.applyAsync(exposureIndex, it)
        } ?: return createFailureResult(
            CameraControl.OperationCanceledException("Camera is not active.")
        )
    }

    private fun createFailureResult(exception: Exception) = CompletableDeferred<Int>().apply {
        completeExceptionally(exception)
    }

    @Module
    abstract class Bindings {
        @Binds
        @IntoSet
        abstract fun provideControls(evCompControl: EvCompControl): UseCaseCameraControl
    }
}
