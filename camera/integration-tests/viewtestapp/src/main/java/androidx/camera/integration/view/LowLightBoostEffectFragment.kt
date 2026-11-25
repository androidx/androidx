/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.integration.view

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.interop.camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.effects.playservices.lowlight.LowLightBoostEffect
import androidx.camera.effects.playservices.lowlight.LowLightBoostException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Fragment for testing [LowLightBoostEffect] integration across [Preview], [VideoCapture], and
 * [ImageCapture] via [SessionConfig]. CameraX applies the effect on `StreamSharing` when more than
 * one bound use case matches its targets.
 */
@RequiresApi(Build.VERSION_CODES.R)
class LowLightBoostEffectFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var statusTextView: TextView
    private lateinit var toggleLlbButton: Button
    private lateinit var flipCameraButton: Button
    private lateinit var launchActivityButton: Button
    private lateinit var captureController: LowLightBoostCaptureController

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var boundSessionConfig: SessionConfig? = null
    private var camera: Camera? = null

    @get:VisibleForTesting
    internal val activeCamera: Camera?
        get() = camera

    @get:VisibleForTesting
    internal val activePreviewView: PreviewView?
        get() = if (::previewView.isInitialized && view != null) previewView else null

    private lateinit var lifecycleStrategyGroup: RadioGroup
    private lateinit var radioBindOnce: RadioButton
    private lateinit var radioRebindOnResume: RadioButton

    private lateinit var implementationModeGroup: RadioGroup
    private lateinit var radioModePerformance: RadioButton
    private lateinit var radioModeCompatible: RadioButton

    private lateinit var targetStrategyGroup: RadioGroup
    private lateinit var radioTargetPreview: RadioButton
    private lateinit var radioTargetPreviewVideo: RadioButton
    private lateinit var radioTargetAll: RadioButton

    private var targetSetting: Int = CameraEffect.PREVIEW
    private var isRebindOnResume: Boolean = false
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var lowLightBoostEffect: LowLightBoostEffect? = null
    // Mirrors LowLightBoostEffect.isLowLightBoostEnabled across bindings. Enabled by default.
    private var isLlbEnabled = true
    // The status and error of lowLightBoostEffect, or null before it is created.
    private var currentLlbStatus: Int? = null
    private var currentLlbError: LowLightBoostException? = null
    private var implementationMode: PreviewView.ImplementationMode =
        PreviewView.ImplementationMode.PERFORMANCE

    private var isSupported: Boolean? = null
    private var supportCheckJob: Job? = null
    private var bindErrorMessage: String? = null
    @Volatile private var currentStreamState: PreviewView.StreamState = PreviewView.StreamState.IDLE
    private var lastBoundRotation: Int = Surface.ROTATION_0
    private val bindGeneration = AtomicLong(0)

    // Called on the main thread when the enabled state, status, or error of lowLightBoostEffect
    // changes.
    private val llbListener =
        object : LowLightBoostEffect.Listener {
            override fun onLowLightBoostEnabledChanged(enabled: Boolean) {
                onLlbStateChanged()
            }

            override fun onStatusChanged(status: Int) {
                onLlbStateChanged()
            }

            override fun onErrorChanged(error: LowLightBoostException?) {
                onLlbStateChanged()
            }
        }

    private fun syncDisplayRotation() {
        if (!isAdded || view == null || cameraProvider == null) return
        val newRotation = getDisplayRotation()
        if (newRotation != lastBoundRotation) {
            Log.d(
                TAG,
                "Display rotation changed from $lastBoundRotation to $newRotation, updating " +
                    "targetRotation",
            )
            updateTargetRotation(newRotation)
        }
    }

    private val displayListener =
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}

            override fun onDisplayRemoved(displayId: Int) {}

            override fun onDisplayChanged(displayId: Int) {
                val act = activity ?: return
                if (!isAdded || view == null) return
                val currentDisplayId = previewView.display?.displayId ?: act.display?.displayId
                if (currentDisplayId == null || displayId == currentDisplayId) {
                    syncDisplayRotation()
                }
            }
        }

    /**
     * Releases the active [lowLightBoostEffect]. Passes `resetEnabled = true` when switching
     * camera/mode/target so the new effect starts enabled, and `resetEnabled = false` on view
     * destruction.
     */
    private fun releaseLowLightBoostEffect(reason: String, resetEnabled: Boolean = true) {
        val effect = lowLightBoostEffect
        if (effect != null) {
            Log.d(TAG, "LowLightBoostEffect released ($reason): effect=$effect")
            // Stop listening to the released effect so its late state updates don't overwrite the
            // status of the next effect.
            effect.removeListener(llbListener)
            lowLightBoostEffect = null
        }
        currentLlbStatus = null
        currentLlbError = null
        if (resetEnabled) {
            isLlbEnabled = true
        }
    }

    /** Reads the state of [lowLightBoostEffect], which is consistent on the main thread. */
    private fun onLlbStateChanged() {
        val effect = lowLightBoostEffect ?: return
        isLlbEnabled = effect.isLowLightBoostEnabled
        currentLlbStatus = effect.status
        currentLlbError = effect.error
        Log.d(
            TAG,
            "LowLightBoostEffect state updated: enabled=$isLlbEnabled, status=${effect.status}, " +
                "error=${effect.error}",
        )
        updateStatus()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.low_light_boost_effect_view, container, false)
        previewView = view.findViewById(R.id.preview_view)
        previewView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            previewView.post { syncDisplayRotation() }
        }

        statusTextView = view.findViewById(R.id.status_text)
        implementationModeGroup = view.findViewById(R.id.implementation_mode_group)
        radioModePerformance = view.findViewById(R.id.radio_mode_performance)
        radioModeCompatible = view.findViewById(R.id.radio_mode_compatible)
        lifecycleStrategyGroup = view.findViewById(R.id.lifecycle_strategy_group)
        radioBindOnce = view.findViewById(R.id.radio_bind_once)
        radioRebindOnResume = view.findViewById(R.id.radio_rebind_on_resume)
        targetStrategyGroup = view.findViewById(R.id.target_strategy_group)
        radioTargetPreview = view.findViewById(R.id.radio_target_preview)
        radioTargetPreviewVideo = view.findViewById(R.id.radio_target_preview_video)
        radioTargetAll = view.findViewById(R.id.radio_target_all)
        toggleLlbButton = view.findViewById(R.id.toggle_llb_button)
        flipCameraButton = view.findViewById(R.id.flip_camera_button)
        launchActivityButton = view.findViewById(R.id.launch_activity_button)
        captureController =
            LowLightBoostCaptureController(
                context = requireContext(),
                captureButton = view.findViewById(R.id.capture_button),
                recordButton = view.findViewById(R.id.record_button),
                onRecordingStateChanged = { isRecording -> setRebindControlsEnabled(!isRecording) },
                showMessage = ::toast,
            )

        if (savedInstanceState != null) {
            val isFront = savedInstanceState.getBoolean(KEY_FACING_FRONT, false)
            cameraSelector =
                if (isFront) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
            val implName = savedInstanceState.getString(KEY_IMPL_MODE)
            implementationMode =
                if (implName != null && implName.equals("COMPATIBLE", ignoreCase = true)) {
                    PreviewView.ImplementationMode.COMPATIBLE
                } else {
                    PreviewView.ImplementationMode.PERFORMANCE
                }
            isRebindOnResume = savedInstanceState.getBoolean(KEY_REBIND_ON_RESUME, false)
            targetSetting = savedInstanceState.getInt(KEY_TARGET_SETTING, CameraEffect.PREVIEW)
            isLlbEnabled = savedInstanceState.getBoolean(KEY_LLB_ENABLED, true)
        } else {
            val intentExtras = arguments ?: activity?.intent?.extras
            val bindingExtra = intentExtras?.getString(EXTRA_BINDING_BEHAVIOR)
            if (bindingExtra != null) {
                isRebindOnResume = bindingExtra.equals("rebind", ignoreCase = true)
            }
            val modeArg = intentExtras?.getString(LowLightBoostEffectActivity.EXTRA_MODE)
            if (modeArg != null && modeArg.equals("COMPATIBLE", ignoreCase = true)) {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            } else if (modeArg != null && modeArg.equals("PERFORMANCE", ignoreCase = true)) {
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }

            val lensExtra =
                intentExtras?.getString(LowLightBoostEffectActivity.EXTRA_LENS)
                    ?: intentExtras?.getString(MainActivity.INTENT_EXTRA_CAMERA_DIRECTION)
            if (lensExtra != null) {
                cameraSelector =
                    if (lensExtra.equals("FRONT", ignoreCase = true)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
            }

            val targetArg = intentExtras?.getString(LowLightBoostEffectActivity.EXTRA_TARGET)
            if (targetArg != null) {
                targetSetting = LowLightBoostAppHelper.parseTargetExtra(targetArg)
            }
            isLlbEnabled =
                intentExtras?.getBoolean(LowLightBoostEffectActivity.EXTRA_LLB_ENABLED, true)
                    ?: true
        }

        previewView.implementationMode = implementationMode

        // Initialize UI checked states before registering listeners to avoid spurious callbacks
        if (implementationMode == PreviewView.ImplementationMode.COMPATIBLE) {
            radioModeCompatible.isChecked = true
        } else {
            radioModePerformance.isChecked = true
        }
        if (isRebindOnResume) {
            radioRebindOnResume.isChecked = true
        } else {
            radioBindOnce.isChecked = true
        }
        LowLightBoostAppHelper.checkTargetRadioButton(
            targetSetting,
            radioTargetPreview,
            radioTargetPreviewVideo,
            radioTargetAll,
        )

        implementationModeGroup.setOnCheckedChangeListener { _, checkedId ->
            val newMode =
                if (checkedId == R.id.radio_mode_compatible) {
                    PreviewView.ImplementationMode.COMPATIBLE
                } else {
                    PreviewView.ImplementationMode.PERFORMANCE
                }
            if (implementationMode != newMode) {
                implementationMode = newMode
                Log.d(TAG, "Implementation mode changed: $implementationMode")
                releaseLowLightBoostEffect("implementation mode change")
                previewView.implementationMode = implementationMode
                bindCamera()
            }
        }

        lifecycleStrategyGroup.setOnCheckedChangeListener { _, checkedId ->
            isRebindOnResume = (checkedId == R.id.radio_rebind_on_resume)
            Log.d(TAG, "Lifecycle strategy changed: isRebindOnResume=$isRebindOnResume")
        }

        targetStrategyGroup.setOnCheckedChangeListener { _, checkedId ->
            val newTarget = LowLightBoostAppHelper.targetIdToMask(checkedId)
            if (targetSetting != newTarget) {
                targetSetting = newTarget
                Log.d(TAG, "Target setting changed: targetSetting=$targetSetting")
                releaseLowLightBoostEffect("target setting changed")
                bindCamera()
            }
        }

        toggleLlbButton.setOnClickListener { toggleLlb() }
        flipCameraButton.setOnClickListener { flipCamera() }
        launchActivityButton.setOnClickListener {
            // Release the camera and session manager before the Activity creates its own effect.
            unbindOwnUseCases()
            val intent =
                Intent(requireContext(), LowLightBoostEffectActivity::class.java).apply {
                    putExtra(
                        LowLightBoostEffectActivity.EXTRA_BINDING_BEHAVIOR,
                        if (isRebindOnResume) "rebind" else "bind_once",
                    )
                    putExtra(
                        LowLightBoostEffectActivity.EXTRA_MODE,
                        if (implementationMode == PreviewView.ImplementationMode.COMPATIBLE) {
                            "COMPATIBLE"
                        } else {
                            "PERFORMANCE"
                        },
                    )
                    putExtra(
                        LowLightBoostEffectActivity.EXTRA_TARGET,
                        when (targetSetting) {
                            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE -> "PREV_VIDEO"
                            CameraEffect.PREVIEW or
                                CameraEffect.VIDEO_CAPTURE or
                                CameraEffect.IMAGE_CAPTURE -> "ALL"
                            else -> "PREVIEW"
                        },
                    )
                    putExtra(
                        LowLightBoostEffectActivity.EXTRA_LENS,
                        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) "FRONT"
                        else "BACK",
                    )
                    putExtra(LowLightBoostEffectActivity.EXTRA_LLB_ENABLED, isLlbEnabled)
                }
            startActivity(intent)
        }

        // Observe preview stream state
        previewView.previewStreamState.observe(viewLifecycleOwner) { streamState ->
            Log.d(TAG, "Preview StreamState changed: $streamState")
            currentStreamState = streamState
            updateStatus()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                try {
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider
                    val isBound = boundSessionConfig?.let { provider.isBound(it) } == true
                    // In rebind-on-resume mode, onResume() binds the camera if not resumed yet.
                    val requiredState =
                        if (isRebindOnResume) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
                    if (
                        isAdded &&
                            view != null &&
                            !isBound &&
                            viewLifecycleOwner.lifecycle.currentState.isAtLeast(requiredState)
                    ) {
                        bindCamera()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get ProcessCameraProvider", e)
                }
            },
            ContextCompat.getMainExecutor(requireContext()),
        )

        updateStatus()
        return view
    }

    private fun unbindOwnUseCases() {
        bindGeneration.incrementAndGet()
        bindErrorMessage = null
        supportCheckJob?.cancel()
        supportCheckJob = null
        if (::captureController.isInitialized) {
            captureController.stopRecording()
            captureController.setUseCases(null, null)
        }
        preview?.setSurfaceProvider(null)
        currentStreamState = PreviewView.StreamState.IDLE
        boundSessionConfig?.let {
            cameraProvider?.unbind(it)
            boundSessionConfig = null
        }
        preview = null
        videoCapture = null
        imageCapture = null
        camera = null
    }

    /**
     * Called on the camera capture callback thread for each capture result. The stream state shown
     * in the status comes from [PreviewView.getPreviewStreamState] instead.
     */
    private fun onCaptureResult() {
        captureResultCounter.incrementAndGet()
    }

    private fun bindCamera() {
        val ctx = context ?: return
        if (!isAdded || view == null) return
        val provider = cameraProvider ?: return
        unbindOwnUseCases()
        val generation = bindGeneration.incrementAndGet()
        previewView.implementationMode = implementationMode

        val isVideoTarget = (targetSetting and CameraEffect.VIDEO_CAPTURE) != 0
        val isImageCaptureTarget = (targetSetting and CameraEffect.IMAGE_CAPTURE) != 0

        val effect =
            lowLightBoostEffect
                ?: LowLightBoostEffect(ctx, targetSetting).also {
                    it.isLowLightBoostEnabled = isLlbEnabled
                    lowLightBoostEffect = it
                    // The listener only reports future changes, so read the current state too.
                    it.addListener(llbListener)
                    onLlbStateChanged()
                    Log.d(TAG, "LowLightBoostEffect created: effect=$it, enabled=$isLlbEnabled")
                }

        val captureCallback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    if (bindGeneration.get() == generation) {
                        onCaptureResult()
                    }
                }
            }

        val displayRotation = getDisplayRotation()
        lastBoundRotation = displayRotation
        val previewUseCase = Preview.Builder().setTargetRotation(displayRotation).build()

        previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
        this.preview = previewUseCase

        val useCases = mutableListOf<UseCase>(previewUseCase)

        if (isVideoTarget) {
            val recorder = Recorder.Builder().build()
            val newVideoCapture =
                VideoCapture.Builder(recorder).setTargetRotation(displayRotation).build()
            videoCapture = newVideoCapture
            useCases.add(newVideoCapture)
        } else {
            videoCapture = null
        }

        if (isImageCaptureTarget) {
            val newImageCapture = ImageCapture.Builder().setTargetRotation(displayRotation).build()
            imageCapture = newImageCapture
            useCases.add(newImageCapture)
        } else {
            imageCapture = null
        }

        val sessionConfig =
            SessionConfig.Builder(useCases)
                .addEffect(effect)
                .camera2Interop { repeatingCaptureCallback = captureCallback }
                .build()

        try {
            camera = provider.bindToLifecycle(viewLifecycleOwner, cameraSelector, sessionConfig)
            boundSessionConfig = sessionConfig
            captureController.setUseCases(imageCapture, videoCapture)
            updateStatus()
            checkSupportAsync()
        } catch (e: Exception) {
            supportCheckJob?.cancel()
            supportCheckJob = null
            boundSessionConfig = null
            previewUseCase.setSurfaceProvider(null)
            currentStreamState = PreviewView.StreamState.IDLE
            preview = null
            videoCapture = null
            imageCapture = null
            camera = null
            Log.e(TAG, "Failed to bind camera", e)
            captureController.setUseCases(null, null)
            bindErrorMessage = "Error binding camera: ${e.message}"
            updateStatus()
            toast("Failed to bind camera: ${e.message}")
        }
    }

    private fun checkSupportAsync() {
        val cam = camera ?: return
        val appContext = context?.applicationContext ?: return
        // Drop the result of a check for a previous binding, e.g. of the other camera.
        supportCheckJob?.cancel()
        supportCheckJob =
            viewLifecycleOwner.lifecycleScope.launch {
                val supp =
                    try {
                        LowLightBoostEffect.isSupported(appContext, cam.cameraInfo)
                    } catch (e: CancellationException) {
                        // A newer check replaced this one, so don't report its result.
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to check Low Light Boost support", e)
                        false
                    }
                if (!isAdded || isRemoving) return@launch
                isSupported = supp
                Log.d(TAG, "Support checked: isSupported=$supp")
                updateStatus()
            }
    }

    private fun toggleLlb() {
        val newState = !(lowLightBoostEffect?.isLowLightBoostEnabled ?: isLlbEnabled)
        isLlbEnabled = newState
        lowLightBoostEffect?.isLowLightBoostEnabled = newState
        Log.d(TAG, "Toggled LowLightBoost to $newState")
        updateStatus()
    }

    private fun flipCamera() {
        cameraSelector =
            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        isSupported = null
        releaseLowLightBoostEffect("flip camera")
        bindCamera()
    }

    /**
     * Enables or disables the controls that rebind the camera, which would interrupt an active
     * recording.
     */
    private fun setRebindControlsEnabled(enabled: Boolean) {
        LowLightBoostAppHelper.setControlsEnabled(
            enabled,
            flipCameraButton,
            launchActivityButton,
            implementationModeGroup,
            lifecycleStrategyGroup,
            targetStrategyGroup,
        )
    }

    private fun updateStatus() {
        val ctx = context ?: return
        if (!isAdded || view == null) return
        captureController.setStreaming(currentStreamState == PreviewView.StreamState.STREAMING)
        val isEnabled = lowLightBoostEffect?.isLowLightBoostEnabled ?: isLlbEnabled
        val errorText = bindErrorMessage
        if (errorText != null) {
            statusTextView.text = errorText
            toggleLlbButton.text =
                if (isEnabled) getString(R.string.llb_disable) else getString(R.string.llb_enable)
            return
        }
        val rotation = preview?.targetRotation ?: getDisplayRotation()
        LowLightBoostAppHelper.updateStatusUi(
            context = ctx,
            statusTextView = statusTextView,
            toggleLlbButton = toggleLlbButton,
            isEnabled = isEnabled,
            cameraSelector = cameraSelector,
            isSupported = isSupported,
            isCompatibleMode = implementationMode == PreviewView.ImplementationMode.COMPATIBLE,
            targetSetting = targetSetting,
            rotation = rotation,
            currentLlbStatus = currentLlbStatus,
            currentLlbError = currentLlbError,
            streamStateText = currentStreamState.toString(),
        )
    }

    private fun updateTargetRotation(rotation: Int) {
        lastBoundRotation = rotation
        preview?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
        imageCapture?.targetRotation = rotation
        updateStatus()
    }

    private fun toast(message: String) {
        activity?.runOnUiThread {
            if (isAdded) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getDisplayRotation(): Int {
        val act = activity ?: return Surface.ROTATION_0
        return previewView.display?.rotation ?: act.display?.rotation ?: Surface.ROTATION_0
    }

    override fun onStart() {
        super.onStart()
        val displayManager = context?.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        displayManager?.registerDisplayListener(displayListener, null)
        syncDisplayRotation()
        // In rebind-on-resume mode, onResume() binds the camera.
        if (isRebindOnResume) {
            return
        }
        cameraProvider?.let { provider ->
            if (boundSessionConfig?.let { provider.isBound(it) } != true) {
                bindCamera()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cameraProvider?.let { provider ->
            if (isRebindOnResume || boundSessionConfig?.let { provider.isBound(it) } != true) {
                bindCamera()
            }
        }
        syncDisplayRotation()
        view?.post { syncDisplayRotation() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        syncDisplayRotation()
        view?.post { syncDisplayRotation() }
    }

    override fun onPause() {
        super.onPause()
        if (isRebindOnResume) {
            unbindOwnUseCases()
            updateStatus()
        }
    }

    override fun onStop() {
        super.onStop()
        val displayManager = context?.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        displayManager?.unregisterDisplayListener(displayListener)
        if (::captureController.isInitialized) {
            captureController.stopRecording()
        }
        if (!isRebindOnResume) {
            currentStreamState = PreviewView.StreamState.IDLE
            updateStatus()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FACING_FRONT, cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
        outState.putString(KEY_IMPL_MODE, implementationMode.name)
        outState.putBoolean(KEY_REBIND_ON_RESUME, isRebindOnResume)
        outState.putInt(KEY_TARGET_SETTING, targetSetting)
        outState.putBoolean(KEY_LLB_ENABLED, isLlbEnabled)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbindOwnUseCases()
        // Keep isLlbEnabled intact so state saved in onSaveInstanceState persists if recreated.
        releaseLowLightBoostEffect("onDestroyView", resetEnabled = false)
    }

    internal companion object {
        internal const val EXTRA_BINDING_BEHAVIOR = "binding_behavior"

        // Intentionally monotonic count of camera capture results, used by instrumentation tests
        // (waitForFrames delta checks) to check that the camera stream keeps running. It doesn't
        // count rendered frames. Not displayed in UI status text and must not be reset on camera
        // rebind.
        private val captureResultCounter = AtomicLong(0)

        @get:VisibleForTesting
        internal val captureResultCount: Long
            get() = captureResultCounter.get()

        private const val TAG = "LowLightBoostEffectFragment"
        private const val KEY_FACING_FRONT = "key_facing_front"
        private const val KEY_IMPL_MODE = "key_impl_mode"
        private const val KEY_REBIND_ON_RESUME = "key_rebind_on_resume"
        private const val KEY_TARGET_SETTING = "key_target_setting"
        private const val KEY_LLB_ENABLED = "key_llb_enabled"
    }
}
