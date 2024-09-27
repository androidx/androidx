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

import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.warn
import androidx.camera.camera2.pipe.integration.adapter.awaitUntil
import androidx.camera.camera2.pipe.integration.adapter.propagateTo
import androidx.camera.camera2.pipe.integration.compat.workaround.UseFlashModeTorchFor3aUpdate
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
import kotlinx.coroutines.withContext

internal const val DEFAULT_FLASH_MODE = ImageCapture.FLASH_MODE_OFF

/** Implementation of Flash control exposed by [CameraControlInternal]. */
@CameraScope
public class FlashControl
@Inject
constructor(
    private val cameraProperties: CameraProperties,
    private val state3AControl: State3AControl,
    private val threads: UseCaseThreads,
    private val torchControl: TorchControl,
    private val useFlashModeTorchFor3aUpdate: UseFlashModeTorchFor3aUpdate,
) : UseCaseCameraControl {
    private var _requestControl: UseCaseCameraRequestControl? = null
    override var requestControl: UseCaseCameraRequestControl?
        get() = _requestControl
        set(value) {
            _requestControl = value
            setFlashAsync(_flashMode, false)
        }

    override fun reset() {
        _flashMode = DEFAULT_FLASH_MODE
        _screenFlash = null
        stopRunningTask()
        setFlashAsync(DEFAULT_FLASH_MODE)
    }

    @Volatile @ImageCapture.FlashMode private var _flashMode: Int = DEFAULT_FLASH_MODE

    @ImageCapture.FlashMode
    public var flashMode: Int = _flashMode
        get() = _flashMode
        private set

    @Volatile private var _screenFlash: ScreenFlash? = null

    public var screenFlash: ScreenFlash? = _screenFlash
        get() = _screenFlash
        private set

    private var _updateSignal: CompletableDeferred<Unit>? = null

    public var updateSignal: Deferred<Unit> = CompletableDeferred(Unit)
        get() =
            if (_updateSignal != null) {
                _updateSignal!!
            } else {
                CompletableDeferred(Unit)
            }
        private set

    public fun setFlashAsync(
        @ImageCapture.FlashMode flashMode: Int,
        cancelPreviousTask: Boolean = true
    ): Deferred<Unit> {
        val signal = CompletableDeferred<Unit>()

        requestControl?.let {

            // Update _flashMode immediately so that CameraControlInternal#getFlashMode()
            // returns correct value.
            _flashMode = flashMode

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
            ?: run {
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

    public fun setScreenFlash(screenFlash: ScreenFlash?) {
        _screenFlash = screenFlash
    }

    public suspend fun startScreenFlashCaptureTasks() {
        val pendingTasks = mutableListOf<Deferred<Unit>>()

        // Invoke ScreenFlash#apply and wait later for its listener to be completed
        pendingTasks.add(
            applyScreenFlash(
                TimeUnit.SECONDS.toMillis(ImageCapture.SCREEN_FLASH_UI_APPLY_TIMEOUT_SECONDS)
            )
        )

        // Try to set external flash AE mode if possible
        setExternalFlashAeModeAsync()?.let { pendingTasks.add(it) }

        // Set FLASH_MODE_TORCH for quirks
        setTorchForScreenFlash()?.let { pendingTasks.add(it) }

        pendingTasks.awaitAll()
    }

    /**
     * Invokes [ScreenFlash.apply] immediately and returns a [Deferred] waiting for the
     * [ScreenFlashListener] to be completed.
     */
    private suspend fun applyScreenFlash(timeoutMillis: Long): Deferred<Unit> {
        val onApplyCompletedSignal = CompletableDeferred<Unit>()
        val screenFlashListener = ScreenFlashListener { onApplyCompletedSignal.complete(Unit) }

        withContext(Dispatchers.Main) {
            val expirationTimeMillis = System.currentTimeMillis() + timeoutMillis
            screenFlash?.apply(expirationTimeMillis, screenFlashListener)
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
                warn {
                    "applyScreenFlash: ScreenFlashListener completion timed out" +
                        " after $timeoutMillis ms"
                }
            }
        }
    }

    /**
     * Tries to set external flash AE mode if possible.
     *
     * @return A [Deferred] that reports the completion of the operation, `null` if not supported.
     */
    private fun setExternalFlashAeModeAsync(): Deferred<Unit>? {
        val isExternalFlashAeModeSupported =
            cameraProperties.metadata.isExternalFlashAeModeSupported()
        debug {
            "setExternalFlashAeModeAsync: isExternalFlashAeModeSupported = " +
                "$isExternalFlashAeModeSupported"
        }

        if (!isExternalFlashAeModeSupported) {
            return null
        }

        state3AControl.tryExternalFlashAeMode = true
        return state3AControl.updateSignal?.also {
            debug { "setExternalFlashAeModeAsync: need to wait for state3AControl.updateSignal" }
            it.invokeOnCompletion {
                debug { "setExternalFlashAeModeAsync: state3AControl.updateSignal completed" }
            }
        }
    }

    /**
     * Enables the torch mode for screen flash capture when required.
     *
     * Since this is required due to a device quirk despite lacking physical flash unit, the
     * `ignoreFlashUnitAvailability` parameter is set to `true` while invoking
     * [TorchControl.setTorchAsync].
     *
     * @return A [Deferred] that reports the completion of the operation, `null` if not required.
     */
    private fun setTorchForScreenFlash(): Deferred<Unit>? {
        val shouldUseFlashModeTorch = useFlashModeTorchFor3aUpdate.shouldUseFlashModeTorch()
        debug { "setTorchIfRequired: shouldUseFlashModeTorch = $shouldUseFlashModeTorch" }

        if (!shouldUseFlashModeTorch) {
            return null
        }

        return torchControl.setTorchAsync(torch = true, ignoreFlashUnitAvailability = true).also {
            debug { "setTorchIfRequired: need to wait for torch control to be completed" }
            it.invokeOnCompletion { debug { "setTorchIfRequired: torch control completed" } }
        }
    }

    public suspend fun stopScreenFlashCaptureTasks() {
        withContext(Dispatchers.Main) {
            screenFlash?.clear()
            debug { "screenFlashPostCapture: ScreenFlash.clear() invoked" }
        }

        if (cameraProperties.metadata.isExternalFlashAeModeSupported()) {
            // Disable external flash AE mode, ok to complete whenever
            state3AControl.tryExternalFlashAeMode = false
        }

        if (useFlashModeTorchFor3aUpdate.shouldUseFlashModeTorch()) {
            torchControl.setTorchAsync(torch = false, ignoreFlashUnitAvailability = true)
        }
    }

    /**
     * Awaits for flash mode to be updated (if required) and returns the initial flash mode value
     * i.e. the value for which the waiting was started.
     */
    public suspend fun awaitFlashModeUpdate(): Int {
        debug { "FlashControl: Waiting for any ongoing update to be completed" }
        // The flash mode may change while waiting for it to be updated, snapshotting it to ensure
        // the initial flash mode value (for which waiting started) is returned afterwards.
        val initialFlashMode = flashMode
        updateSignal.join()
        return initialFlashMode
    }

    @Module
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(flashControl: FlashControl): UseCaseCameraControl
    }
}
