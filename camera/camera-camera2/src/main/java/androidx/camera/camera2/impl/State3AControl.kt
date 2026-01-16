/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.impl

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.annotation.GuardedBy
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.compat.workaround.AutoFlashAEModeDisabler
import androidx.camera.camera2.config.CameraScope
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CaptureConfig
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

@CameraScope
public class State3AControl
@Inject
constructor(
    public val cameraProperties: CameraProperties,
    private val aeModeDisabler: AutoFlashAEModeDisabler,
    private val threads: UseCaseThreads,
) : UseCaseCameraControl, UseCaseManager.RunningUseCasesChangeListener {
    private val lock = Any()

    private var _requestControl: UseCaseCameraRequestControl? = null
    override var requestControl: UseCaseCameraRequestControl?
        get() = _requestControl
        set(value) {
            _requestControl = value
            update()
        }

    @GuardedBy("lock") private val pendingSignals = mutableListOf<CompletableDeferred<Unit>>()

    @GuardedBy("lock") private var currentRevision = 0L

    // Internal state variables, guarded by lock
    @GuardedBy("lock") private var _flashMode = DEFAULT_FLASH_MODE
    @GuardedBy("lock") private var _template = DEFAULT_REQUEST_TEMPLATE
    @GuardedBy("lock") private var _tryExternalFlashAeMode = false
    @GuardedBy("lock") private var _preferredAeMode: Int? = null
    @GuardedBy("lock") private var _preferredFocusMode: Int? = null

    public val flashMode: Int
        get() = synchronized(lock) { _flashMode }

    public val template: Int
        get() = synchronized(lock) { _template }

    public val tryExternalFlashAeMode: Boolean
        get() = synchronized(lock) { _tryExternalFlashAeMode }

    /**
     * The [CaptureRequest.CONTROL_AE_MODE] that is set to camera if supported.
     *
     * If null, a value based on other settings is calculated and available via
     * [getFinalPreferredAeMode]. If not supported, [getSupportedAeMode] is used to find the next
     * best option.
     */
    public val preferredAeMode: Int?
        get() = synchronized(lock) { _preferredAeMode }

    public val preferredFocusMode: Int?
        get() = synchronized(lock) { _preferredFocusMode }

    public fun setFlashModeAsync(value: Int): Deferred<Unit> {
        synchronized(lock) { _flashMode = value }
        return update()
    }

    public fun setTemplateAsync(value: Int): Deferred<Unit> {
        synchronized(lock) { _template = value }
        return update()
    }

    public fun setTryExternalFlashAeModeAsync(value: Boolean): Deferred<Unit> {
        synchronized(lock) { _tryExternalFlashAeMode = value }
        return update()
    }

    public fun setPreferredAeModeAsync(value: Int?): Deferred<Unit> {
        synchronized(lock) { _preferredAeMode = value }
        return update()
    }

    public fun setPreferredFocusModeAsync(value: Int?): Deferred<Unit> {
        synchronized(lock) { _preferredFocusMode = value }
        return update()
    }

    override fun reset() {
        synchronized(lock) {
            _tryExternalFlashAeMode = false
            _preferredAeMode = null
            _preferredFocusMode = null
            _flashMode = DEFAULT_FLASH_MODE
            _template = DEFAULT_REQUEST_TEMPLATE
        }
        update()
    }

    override fun onRunningUseCasesChanged(runningUseCases: Set<UseCase>) {
        val useCasesSnapshot = runningUseCases.toSet()

        threads.confineLaunch {
            if (useCasesSnapshot.isEmpty()) return@confineLaunch

            val newTemplate = calculateTemplateFromUseCases(useCasesSnapshot)

            val changed =
                synchronized(lock) {
                    if (_template != newTemplate) {
                        _template = newTemplate
                        true
                    } else {
                        false
                    }
                }

            if (changed) {
                update()
            }
        }
    }

    /**
     * Extracts the template type from a set of use cases. This method creates a
     * SessionConfigAdapter which is expensive (~2-10ms).
     */
    private fun calculateTemplateFromUseCases(useCases: Set<UseCase>): Int {
        return SessionConfigAdapter(useCases)
            .getValidSessionConfigOrNull()
            ?.repeatingCaptureConfig
            ?.templateType
            ?.takeIf { it != CaptureConfig.TEMPLATE_TYPE_NONE } ?: DEFAULT_REQUEST_TEMPLATE
    }

    /**
     * Enqueues an update task to the sequential scope. Returns a Deferred that completes when the
     * camera parameters are successfully proceeded.
     */
    private fun update(): Deferred<Unit> {
        val signal = CompletableDeferred<Unit>()
        val revision: Long

        synchronized(lock) {
            pendingSignals.add(signal)
            revision = ++currentRevision
        }

        threads.confineLaunch { applyUpdate(revision) }

        return signal
    }

    /**
     * Calculates the 3A parameters based on the *current* state snapshot and submits them. Must be
     * called from the sequentialScope.
     */
    private fun applyUpdate(myRevision: Long) {
        val control = requestControl

        if (control == null) {
            failAllPendingSignals(CameraControl.OperationCanceledException("Camera is not active."))
            return
        }

        // If a newer update has been requested since we started, we skip the camera work.
        // We do NOT fail the signals; we leave them in the list for the newer job to complete.
        val isLatest = synchronized(lock) { myRevision == currentRevision }

        if (!isLatest) {
            return
        }

        val snapshot =
            synchronized(lock) {
                StateSnapshot(
                    flashMode = _flashMode,
                    template = _template,
                    tryExternalFlashAeMode = _tryExternalFlashAeMode,
                    preferredAeMode = _preferredAeMode,
                    preferredFocusMode = _preferredFocusMode,
                )
            }

        // TODO(b/276779600): Refactor and move the setting of these parameter to
        //  CameraGraph.Config(requiredParameters = mapOf(....)).
        val finalAeMode =
            getFinalPreferredAeMode(
                snapshot.flashMode,
                snapshot.tryExternalFlashAeMode,
                snapshot.preferredAeMode,
            )
        val finalAfMode = snapshot.preferredFocusMode ?: getDefaultAfMode(snapshot.template)

        val parameters: Map<CaptureRequest.Key<*>, Any> =
            mapOf(
                CaptureRequest.CONTROL_AE_MODE to
                    cameraProperties.metadata.getSupportedAeMode(finalAeMode),
                CaptureRequest.CONTROL_AF_MODE to
                    cameraProperties.metadata.getSupportedAfMode(finalAfMode),
                CaptureRequest.CONTROL_AWB_MODE to
                    cameraProperties.metadata.getSupportedAwbMode(
                        CaptureRequest.CONTROL_AWB_MODE_AUTO
                    ),
            )

        try {
            val signal = control.submitParameters(values = parameters)

            val signalsToComplete = synchronized(lock) { pendingSignals.toList() }

            signal.invokeOnCompletion { cause ->
                if (cause != null) {
                    signalsToComplete.forEach { it.completeExceptionally(cause) }
                } else {
                    signalsToComplete.forEach { it.complete(Unit) }
                }
                synchronized(lock) { pendingSignals.removeAll(signalsToComplete) }
            }
        } catch (e: Exception) {
            failAllPendingSignals(e)
        }
    }

    private fun failAllPendingSignals(e: Exception) {
        val signalsToFail =
            synchronized(lock) {
                val copy = pendingSignals.toList()
                pendingSignals.clear()
                copy
            }
        signalsToFail.forEach { it.completeExceptionally(e) }
    }

    /**
     * Returns the AE mode that is finally set to camera based on all other settings and camera
     * capabilities.
     */
    public fun getFinalSupportedAeMode(): Int =
        synchronized(lock) {
            cameraProperties.metadata.getSupportedAeMode(
                getFinalPreferredAeMode(_flashMode, _tryExternalFlashAeMode, _preferredAeMode)
            )
        }

    /**
     * Returns the AE mode that is finally set to camera based on all other settings.
     *
     * Note that this may not be supported via the camera and should be sanitized with
     * [getSupportedAeMode].
     */
    private fun getFinalPreferredAeMode(
        flashMode: Int,
        tryExternalFlashAeMode: Boolean,
        preferredAeMode: Int?,
    ): Int {
        var preferAeMode =
            preferredAeMode
                ?: when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> CaptureRequest.CONTROL_AE_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
                    ImageCapture.FLASH_MODE_AUTO ->
                        aeModeDisabler.getCorrectedAeMode(
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                        )
                    else -> CaptureRequest.CONTROL_AE_MODE_ON
                }

        // Overwrite AE mode to ON_EXTERNAL_FLASH only if required and explicitly supported
        if (tryExternalFlashAeMode && cameraProperties.metadata.isExternalFlashAeModeSupported()) {
            Camera2Logger.debug { "State3AControl.invalidate: trying external flash AE mode." }
            preferAeMode = CaptureRequest.CONTROL_AE_MODE_ON_EXTERNAL_FLASH
        }

        Camera2Logger.debug {
            "State3AControl.getFinalPreferredAeMode: preferAeMode = $preferAeMode"
        }

        return preferAeMode
    }

    private fun getDefaultAfMode(template: Int): Int =
        when (template) {
            CameraDevice.TEMPLATE_RECORD -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            CameraDevice.TEMPLATE_PREVIEW -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            else -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        }

    /** Snapshot for consistent processing in background */
    private data class StateSnapshot(
        val flashMode: Int,
        val template: Int,
        val tryExternalFlashAeMode: Boolean,
        val preferredAeMode: Int?,
        val preferredFocusMode: Int?,
    )

    @Module
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(state3AControl: State3AControl): UseCaseCameraControl
    }
}
