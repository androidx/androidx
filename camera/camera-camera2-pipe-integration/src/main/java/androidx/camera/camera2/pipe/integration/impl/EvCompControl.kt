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

import androidx.camera.camera2.pipe.integration.adapter.EvCompValue
import androidx.camera.camera2.pipe.integration.compat.EvCompCompat
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

private const val DEFAULT_EXPOSURE_COMPENSATION = 0

/**
 * Implementation of Exposure compensation control, it implements the functionality of
 * [CameraControl.setExposureCompensationIndex].
 *
 * The [CameraControl.setExposureCompensationIndex] can only allow to run one task at the same time,
 * it will cancel the incomplete task if a new task is requested. The task will fail with
 * [CameraControl.OperationCanceledException] if the camera is closed.
 */
@CameraScope
public class EvCompControl
@Inject
constructor(
    private val compat: EvCompCompat,
) : UseCaseCameraControl {
    private var evCompIndex = DEFAULT_EXPOSURE_COMPENSATION
        set(value) {
            field = value
            exposureState = exposureState.updateIndex(value)
        }

    public var exposureState: EvCompValue =
        EvCompValue(
            compat.supported,
            evCompIndex,
            compat.range,
            compat.step,
        )

    private var _requestControl: UseCaseCameraRequestControl? = null
    override var requestControl: UseCaseCameraRequestControl?
        get() = _requestControl
        set(value) {
            _requestControl = value
            updateAsync(evCompIndex, cancelPreviousTask = false)
        }

    override fun reset() {
        evCompIndex = DEFAULT_EXPOSURE_COMPENSATION
        updateAsync(DEFAULT_EXPOSURE_COMPENSATION)
    }

    public fun updateAsync(exposureIndex: Int, cancelPreviousTask: Boolean = true): Deferred<Int> {
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

        return requestControl?.let { requestControl ->
            evCompIndex = exposureIndex
            compat.applyAsync(exposureIndex, requestControl, cancelPreviousTask)
        }
            ?: run {
                CameraControl.OperationCanceledException("Camera is not active.").let { cancelResult
                    ->
                    compat.stopRunningTask(cancelResult)
                    createFailureResult(cancelResult)
                }
            }
    }

    private fun createFailureResult(exception: Exception) =
        CompletableDeferred<Int>().apply { completeExceptionally(exception) }

    @Module
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(evCompControl: EvCompControl): UseCaseCameraControl
    }
}
