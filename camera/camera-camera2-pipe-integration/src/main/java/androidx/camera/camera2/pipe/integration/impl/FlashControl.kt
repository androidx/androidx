/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.adapter.propagateTo
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.CameraControlInternal
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

internal const val DEFAULT_FLASH_MODE = ImageCapture.FLASH_MODE_OFF

/**
 * Implementation of Flash control exposed by [CameraControlInternal].
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
class FlashControl @Inject constructor(
    private val state3AControl: State3AControl,
    private val threads: UseCaseThreads,
) : UseCaseCameraControl {
    private var _useCaseCamera: UseCaseCamera? = null
    override var useCaseCamera: UseCaseCamera?
        get() = _useCaseCamera
        set(value) {
            _useCaseCamera = value
            setFlashAsync(_flashMode, false)
        }

    override fun reset() {
        _flashMode = DEFAULT_FLASH_MODE
        threads.sequentialScope.launch {
            stopRunningTask()
        }
        setFlashAsync(DEFAULT_FLASH_MODE)
    }
    private var _updateSignal: CompletableDeferred<Unit>? = null

    @Volatile
    private var _flashMode: Int = DEFAULT_FLASH_MODE
    var flashMode: Int = _flashMode
        get() = _flashMode
        private set

    var updateSignal: Deferred<Unit> = CompletableDeferred(Unit)
        get() = if (_updateSignal != null) {
            _updateSignal!!
        } else {
            CompletableDeferred(Unit)
        }
        private set

    fun setFlashAsync(flashMode: Int, cancelPreviousTask: Boolean = true): Deferred<Unit> {
        val signal = CompletableDeferred<Unit>()

        useCaseCamera?.let {

            // Update _flashMode immediately so that CameraControlInternal#getFlashMode()
            // returns correct value.
            _flashMode = flashMode

            threads.sequentialScope.launch {
                if (cancelPreviousTask) {
                    stopRunningTask()
                } else {
                    // Propagate the result to the previous updateSignal
                    _updateSignal?.let { previousUpdateSignal ->
                        signal.propagateTo(previousUpdateSignal)
                    }
                }

                _updateSignal = signal
                state3AControl.flashMode = flashMode
                state3AControl.updateSignal?.propagateTo(signal) ?: run { signal.complete(Unit) }
            }
        } ?: run {
            signal.completeExceptionally(
                CameraControl.OperationCanceledException("Camera is not active.")
            )
        }

        return signal
    }

    private fun stopRunningTask() {
        _updateSignal?.apply {
            completeExceptionally(
                CameraControl.OperationCanceledException(
                    "There is a new flash mode being set or camera was closed"
                )
            )
        }
        _updateSignal = null
    }

    @Module
    abstract class Bindings {
        @Binds
        @IntoSet
        abstract fun provideControls(flashControl: FlashControl): UseCaseCameraControl
    }
}
