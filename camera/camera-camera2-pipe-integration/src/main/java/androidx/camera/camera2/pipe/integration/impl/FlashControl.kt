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
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.integration.adapter.awaitUntil
import androidx.camera.camera2.pipe.integration.adapter.propagateTo
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.ScreenFlash
import androidx.camera.core.ImageCapture.ScreenFlashListener
import androidx.camera.core.impl.CameraControlInternal
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val DEFAULT_FLASH_MODE = ImageCapture.FLASH_MODE_OFF

/**
 * Implementation of Flash control exposed by [CameraControlInternal].
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraScope
class FlashControl @Inject constructor(
    private val cameraProperties: CameraProperties,
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
        _screenFlash = null
        threads.sequentialScope.launch {
            stopRunningTask()
        }
        setFlashAsync(DEFAULT_FLASH_MODE)
    }

    @Volatile
    @ImageCapture.FlashMode
    private var _flashMode: Int = DEFAULT_FLASH_MODE

    @ImageCapture.FlashMode
    var flashMode: Int = _flashMode
        get() = _flashMode
        private set

    @Volatile
    private var _screenFlash: ScreenFlash? = null

    var screenFlash: ScreenFlash? = _screenFlash
        get() = _screenFlash
        private set

    private var _updateSignal: CompletableDeferred<Unit>? = null

    var updateSignal: Deferred<Unit> = CompletableDeferred(Unit)
        get() = if (_updateSignal != null) {
            _updateSignal!!
        } else {
            CompletableDeferred(Unit)
        }
        private set

    fun setFlashAsync(
        @ImageCapture.FlashMode flashMode: Int,
        cancelPreviousTask: Boolean = true
    ): Deferred<Unit> {
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

    fun setScreenFlash(screenFlash: ScreenFlash?) {
        _screenFlash = screenFlash
    }

    suspend fun startScreenFlashCaptureTasks() {
        val pendingTasks = mutableListOf<Deferred<Unit>>()

        // Invoke ScreenFlash#apply and wait later for its listener to be completed
        applyScreenFlash(
            TimeUnit.SECONDS.toMillis(ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS)
        ).let {
            pendingTasks.add(it)
        }

        // Enable external flash AE mode if possible
        val isExternalFlashAeModeSupported =
            cameraProperties.metadata.isExternalFlashAeModeSupported()
        debug {
            "startScreenFlashCaptureTasks: isExternalFlashAeModeSupported = " +
                "$isExternalFlashAeModeSupported"
        }
        if (isExternalFlashAeModeSupported) {
            state3AControl.tryExternalFlashAeMode = true
            state3AControl.updateSignal?.let {
                debug {
                    "startScreenFlashCaptureTasks: need to wait for state3AControl.updateSignal"
                }
                pendingTasks.add(it)
                it.invokeOnCompletion {
                    debug { "startScreenFlashCaptureTasks: state3AControl.updateSignal completed" }
                }
            }
        }

        // TODO: b/326170400 - Enable torch mode if TorchFlashRequiredFor3aUpdateQuirk added

        pendingTasks.awaitAll()
    }

    /**
     * Invokes [ScreenFlash.apply] immediately and returns a [Deferred] waiting for the
     * [ScreenFlashListener] to be completed.
     */
    private suspend fun applyScreenFlash(timeoutMillis: Long): Deferred<Unit> {
        val onApplyCompletedSignal = CompletableDeferred<Unit>()
        val screenFlashListener = ScreenFlashListener {
            onApplyCompletedSignal.complete(Unit)
        }

        withContext(Dispatchers.Main) {
            val expirationTimeMillis = System.currentTimeMillis() + timeoutMillis
            screenFlash?.apply(
                expirationTimeMillis,
                screenFlashListener
            )
            debug {
                "applyScreenFlash: ScreenFlash.apply() invoked" +
                    ", expirationTimeMillis = $expirationTimeMillis"
            }
        }

        return threads.scope.async {
            debug { "applyScreenFlash: Waiting for ScreenFlashListener to be completed" }

            // Wait for ScreenFlashListener#onCompleted to be invoked,
            // it's ok to give a little more time than expirationTimeMillis in ScreenFlash#apply

            if (onApplyCompletedSignal.awaitUntil(timeoutMillis)) {
                debug { "applyScreenFlash: ScreenFlashListener completed" }
            } else {
                warn { "applyScreenFlash: ScreenFlashListener completion timed out" +
                    " after $timeoutMillis ms" }
            }
        }
    }

    suspend fun stopScreenFlashCaptureTasks() {
        withContext(Dispatchers.Main) {
            screenFlash?.clear()
            debug { "screenFlashPostCapture: ScreenFlash.clear() invoked" }
        }

        if (cameraProperties.metadata.isExternalFlashAeModeSupported()) {
            // Disable external flash AE mode, ok to complete whenever
            state3AControl.tryExternalFlashAeMode = false
        }

        // TODO: b/326170400 - Disable torch mode if TorchFlashRequiredFor3aUpdateQuirk added
    }

    @Module
    abstract class Bindings {
        @Binds
        @IntoSet
        abstract fun provideControls(flashControl: FlashControl): UseCaseCameraControl
    }
}
