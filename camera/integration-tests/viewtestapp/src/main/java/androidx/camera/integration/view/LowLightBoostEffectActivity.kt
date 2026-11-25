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
import android.view.Surface
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.camera2Interop
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.effects.playservices.lowlight.LowLightBoostEffect
import androidx.camera.effects.playservices.lowlight.LowLightBoostException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.util.EdgeToEdgeUtil
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Activity for testing LowLightBoostEffect when configChanges is declared, using Jetpack Compose
 * [CameraXViewfinder].
 *
 * In activities with
 * android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden",
 * the Activity is not recreated on rotation. DisplayListener updates Preview#targetRotation when
 * display rotation changes to keep preview images upright without re-binding.
 */
@RequiresApi(Build.VERSION_CODES.R)
class LowLightBoostEffectActivity : AppCompatActivity() {

    private lateinit var composeViewfinder: ComposeView
    private lateinit var statusTextView: TextView
    private lateinit var toggleLlbButton: Button
    private lateinit var flipCameraButton: Button
    private lateinit var captureController: LowLightBoostCaptureController

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

    private val surfaceRequestState = mutableStateOf<SurfaceRequest?>(null)
    private val composeImplementationModeState = mutableStateOf(ImplementationMode.EXTERNAL)
    // The effect observed by the composition, i.e. lowLightBoostEffect.
    private val llbEffectState = mutableStateOf<LowLightBoostEffect?>(null)

    private var targetSetting: Int = CameraEffect.PREVIEW
    private var isRebindOnResume: Boolean = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var lowLightBoostEffect: LowLightBoostEffect? = null
    // Mirrors LowLightBoostEffect.isLowLightBoostEnabled across bindings. Enabled by default.
    private var isLlbEnabled = true
    // The status and error of lowLightBoostEffect, or null before they are observed.
    private var currentLlbStatus: Int? = null
    private var currentLlbError: LowLightBoostException? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var boundSessionConfig: SessionConfig? = null
    private var camera: Camera? = null
    private var implementationMode: ImplementationMode = ImplementationMode.EXTERNAL

    @get:VisibleForTesting internal val coordinateTransformer = MutableCoordinateTransformer()

    @get:VisibleForTesting
    internal val activeCamera: Camera?
        get() = camera

    @get:VisibleForTesting
    internal val activeSurfaceRequest: SurfaceRequest?
        get() = surfaceRequestState.value

    private var isSupported: Boolean? = null
    private var supportCheckJob: Job? = null
    private var bindErrorMessage: String? = null
    // Driven by CameraXViewfinder.onStreamStateChanged (and reset on unbind/cancellation).
    @Volatile private var currentStreamState: String = "IDLE"
    private val bindGeneration = AtomicLong(0)

    private fun updateTargetRotation(rotation: Int) {
        val activePreview = preview ?: return
        if (activePreview.targetRotation != rotation) {
            Log.d(
                TAG,
                "Display rotation changed to $rotation (was ${activePreview.targetRotation}); " +
                    "updating targetRotation dynamically",
            )
            activePreview.targetRotation = rotation
            videoCapture?.targetRotation = rotation
            imageCapture?.targetRotation = rotation
            updateStatus()
        }
    }

    private val displayListener =
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}

            override fun onDisplayRemoved(displayId: Int) {}

            override fun onDisplayChanged(displayId: Int) {
                val currentDisplayId = composeViewfinder.display?.displayId ?: display?.displayId
                if (currentDisplayId == null || displayId == currentDisplayId) {
                    updateTargetRotation(getDisplayRotation())
                }
            }
        }

    /**
     * Releases the active [lowLightBoostEffect]. Passes `resetEnabled = true` when switching
     * camera/mode/target so the new effect starts enabled, and `resetEnabled = false` on destroy.
     */
    private fun releaseLowLightBoostEffect(reason: String, resetEnabled: Boolean = true) {
        val effect = lowLightBoostEffect
        if (effect != null) {
            Log.d(TAG, "LowLightBoostEffect released ($reason): effect=$effect")
            // Stop observing the released effect so its late state updates don't overwrite the
            // status of the next effect. This cancels the LaunchedEffect that observes it.
            llbEffectState.value = null
            lowLightBoostEffect = null
        }
        currentLlbStatus = null
        currentLlbError = null
        if (resetEnabled) {
            isLlbEnabled = true
        }
    }

    /** Reads the state of [effect] in [LowLightBoostEffect.observe] on the main thread. */
    private fun onLlbStateChanged(effect: LowLightBoostEffect) {
        if (effect !== lowLightBoostEffect) return
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: savedInstanceState=$savedInstanceState")
        if (savedInstanceState != null) {
            val isFront = savedInstanceState.getBoolean(KEY_FACING_FRONT, false)
            cameraSelector =
                if (isFront) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
            val modeName = savedInstanceState.getString(KEY_IMPL_MODE)
            implementationMode =
                if (modeName != null && modeName.equals("COMPATIBLE", ignoreCase = true)) {
                    ImplementationMode.EMBEDDED
                } else {
                    ImplementationMode.EXTERNAL
                }
            isRebindOnResume = savedInstanceState.getBoolean(KEY_REBIND_ON_RESUME, false)
            targetSetting = savedInstanceState.getInt(KEY_TARGET_SETTING, CameraEffect.PREVIEW)
            isLlbEnabled = savedInstanceState.getBoolean(KEY_LLB_ENABLED, true)
        } else {
            val bindingExtra = intent.getStringExtra(EXTRA_BINDING_BEHAVIOR)
            if (bindingExtra != null) {
                isRebindOnResume = bindingExtra.equals("rebind", ignoreCase = true)
            }
            val modeExtra = intent.getStringExtra(EXTRA_MODE)
            implementationMode =
                if (modeExtra != null && modeExtra.equals("COMPATIBLE", ignoreCase = true)) {
                    ImplementationMode.EMBEDDED
                } else {
                    ImplementationMode.EXTERNAL
                }
            val lensExtra =
                intent.getStringExtra(EXTRA_LENS)
                    ?: intent.getStringExtra(MainActivity.INTENT_EXTRA_CAMERA_DIRECTION)
            if (lensExtra != null) {
                cameraSelector =
                    if (lensExtra.equals("FRONT", ignoreCase = true)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
            }
            val targetExtra = intent.getStringExtra(EXTRA_TARGET)
            if (targetExtra != null) {
                targetSetting = LowLightBoostAppHelper.parseTargetExtra(targetExtra)
            }
            isLlbEnabled = intent.getBooleanExtra(EXTRA_LLB_ENABLED, true)
        }

        composeImplementationModeState.value = implementationMode

        setContentView(R.layout.activity_low_light_boost_effect)
        EdgeToEdgeUtil.enableEdgeToEdge(
            this,
            viewIdsTopPaddingRequired = listOf(R.id.root_layout),
        )
        composeViewfinder = findViewById(R.id.compose_viewfinder)
        composeViewfinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            composeViewfinder.post {
                if (!isFinishing && !isDestroyed) {
                    updateTargetRotation(getDisplayRotation())
                }
            }
        }
        composeViewfinder.setContent {
            // Observes the current effect, emitting its current state first, until the effect is
            // released or the composition is disposed.
            val currentEffect = llbEffectState.value
            LaunchedEffect(currentEffect) { currentEffect?.observe { onLlbStateChanged(this) } }
            val currentRequest = surfaceRequestState.value
            val currentMode = composeImplementationModeState.value
            if (currentRequest != null) {
                CameraXViewfinder(
                    surfaceRequest = currentRequest,
                    modifier = Modifier.fillMaxSize(),
                    implementationMode = currentMode,
                    coordinateTransformer = coordinateTransformer,
                    onStreamStateChanged = { state ->
                        currentStreamState =
                            if (state == Preview.STREAM_STATE_STREAMING) "STREAMING" else "IDLE"
                        updateStatus()
                    },
                )
            }
        }

        statusTextView = findViewById(R.id.status_text)
        implementationModeGroup = findViewById(R.id.implementation_mode_group)
        radioModePerformance = findViewById(R.id.radio_mode_performance)
        radioModeCompatible = findViewById(R.id.radio_mode_compatible)
        lifecycleStrategyGroup = findViewById(R.id.lifecycle_strategy_group)
        radioBindOnce = findViewById(R.id.radio_bind_once)
        radioRebindOnResume = findViewById(R.id.radio_rebind_on_resume)
        targetStrategyGroup = findViewById(R.id.target_strategy_group)
        radioTargetPreview = findViewById(R.id.radio_target_preview)
        radioTargetPreviewVideo = findViewById(R.id.radio_target_preview_video)
        radioTargetAll = findViewById(R.id.radio_target_all)
        toggleLlbButton = findViewById(R.id.toggle_llb_button)
        flipCameraButton = findViewById(R.id.flip_camera_button)
        captureController =
            LowLightBoostCaptureController(
                context = this,
                captureButton = findViewById(R.id.capture_button),
                recordButton = findViewById(R.id.record_button),
                onRecordingStateChanged = { isRecording -> setRebindControlsEnabled(!isRecording) },
                showMessage = ::toast,
            )

        // Initialize UI checked states before registering listeners to avoid spurious callbacks
        if (implementationMode == ImplementationMode.EMBEDDED) {
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
                    ImplementationMode.EMBEDDED
                } else {
                    ImplementationMode.EXTERNAL
                }
            if (implementationMode != newMode) {
                implementationMode = newMode
                composeImplementationModeState.value = newMode
                Log.d(TAG, "Implementation mode changed: $implementationMode")
                releaseLowLightBoostEffect("implementation mode change")
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

        toggleLlbButton.setOnClickListener { toggleLowLightBoost() }
        flipCameraButton.setOnClickListener { toggleCamera() }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
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
                        !isFinishing &&
                            !isDestroyed &&
                            !isBound &&
                            lifecycle.currentState.isAtLeast(requiredState)
                    ) {
                        bindCamera()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get ProcessCameraProvider", e)
                    toast("Failed to initialize camera: ${e.message}")
                }
            },
            ContextCompat.getMainExecutor(this),
        )
        updateStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        displayManager?.registerDisplayListener(displayListener, null)
        updateTargetRotation(getDisplayRotation())
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
        updateTargetRotation(getDisplayRotation())
        composeViewfinder.post {
            if (!isFinishing && !isDestroyed) {
                updateTargetRotation(getDisplayRotation())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing || isRebindOnResume) {
            unbindOwnUseCases()
            if (!isFinishing) {
                updateStatus()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        displayManager?.unregisterDisplayListener(displayListener)
        if (::captureController.isInitialized) {
            captureController.stopRecording()
        }
        if (!isFinishing && !isRebindOnResume) {
            currentStreamState = "IDLE"
            updateStatus()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged: orientation=${newConfig.orientation}")
        updateTargetRotation(getDisplayRotation())
        composeViewfinder.post {
            if (!isFinishing && !isDestroyed) {
                updateTargetRotation(getDisplayRotation())
            }
        }
    }

    private fun unbindOwnUseCases(clearSurfaceRequest: Boolean = true) {
        bindGeneration.incrementAndGet()
        bindErrorMessage = null
        supportCheckJob?.cancel()
        supportCheckJob = null
        if (::captureController.isInitialized) {
            captureController.stopRecording()
            captureController.setUseCases(null, null)
        }
        preview?.setSurfaceProvider(null)
        if (clearSurfaceRequest) {
            surfaceRequestState.value = null
        }
        currentStreamState = "IDLE"
        boundSessionConfig?.let {
            cameraProvider?.unbind(it)
            boundSessionConfig = null
        }
        preview = null
        videoCapture = null
        imageCapture = null
        camera = null
    }

    /** Called on the camera capture callback thread for each capture result. */
    private fun onCaptureResult() {
        captureResultCounter.incrementAndGet()
    }

    private fun bindCamera() {
        val provider = cameraProvider ?: return
        unbindOwnUseCases(clearSurfaceRequest = false)
        val generation = bindGeneration.incrementAndGet()
        composeImplementationModeState.value = implementationMode

        val isVideoTarget = (targetSetting and CameraEffect.VIDEO_CAPTURE) != 0
        val isImageCaptureTarget = (targetSetting and CameraEffect.IMAGE_CAPTURE) != 0

        val effect =
            lowLightBoostEffect
                ?: LowLightBoostEffect(this, targetSetting).also {
                    it.isLowLightBoostEnabled = isLlbEnabled
                    lowLightBoostEffect = it
                    llbEffectState.value = it
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

        val targetRotation = getDisplayRotation()
        val newPreview =
            Preview.Builder().setTargetRotation(targetRotation).build().also {
                it.setSurfaceProvider { request ->
                    surfaceRequestState.value = request
                    request.addRequestCancellationListener(ContextCompat.getMainExecutor(this)) {
                        if (surfaceRequestState.value === request) {
                            currentStreamState = "IDLE"
                            updateStatus()
                        }
                    }
                }
                preview = it
            }

        val useCases = mutableListOf<UseCase>(newPreview)

        if (isVideoTarget) {
            val recorder = Recorder.Builder().build()
            val newVideoCapture =
                VideoCapture.Builder(recorder).setTargetRotation(targetRotation).build()
            videoCapture = newVideoCapture
            useCases.add(newVideoCapture)
        } else {
            videoCapture = null
        }

        if (isImageCaptureTarget) {
            val newImageCapture = ImageCapture.Builder().setTargetRotation(targetRotation).build()
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
            val boundCamera = provider.bindToLifecycle(this, cameraSelector, sessionConfig)
            camera = boundCamera
            boundSessionConfig = sessionConfig
            Log.d(TAG, "Camera bound successfully with LowLightBoostEffect in Activity")
            captureController.setUseCases(imageCapture, videoCapture)
            updateStatus()
            checkSupportAsync(boundCamera.cameraInfo)
        } catch (e: Exception) {
            supportCheckJob?.cancel()
            supportCheckJob = null
            boundSessionConfig = null
            newPreview.setSurfaceProvider(null)
            surfaceRequestState.value = null
            currentStreamState = "IDLE"
            preview = null
            videoCapture = null
            imageCapture = null
            camera = null
            Log.e(TAG, "Failed to bind camera use cases with LowLightBoostEffect", e)
            captureController.setUseCases(null, null)
            bindErrorMessage = "Error binding camera: ${e.message}"
            updateStatus()
            toast("Bind failed: ${e.message}")
        }
    }

    private fun checkSupportAsync(cameraInfo: CameraInfo) {
        // Drop the result of a check for a previous binding, e.g. of the other camera.
        supportCheckJob?.cancel()
        supportCheckJob = lifecycleScope.launch {
            val supp =
                try {
                    LowLightBoostEffect.isSupported(applicationContext, cameraInfo)
                } catch (e: CancellationException) {
                    // A newer check replaced this one, so don't report its result.
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to check Low Light Boost support", e)
                    false
                }
            isSupported = supp
            Log.d(TAG, "Support checked: isSupported=$supp")
            updateStatus()
        }
    }

    private fun toggleLowLightBoost() {
        val newState = !(lowLightBoostEffect?.isLowLightBoostEnabled ?: isLlbEnabled)
        isLlbEnabled = newState
        lowLightBoostEffect?.isLowLightBoostEnabled = newState
        Log.d(TAG, "Toggled LowLightBoost to $newState")
        updateStatus()
    }

    private fun toggleCamera() {
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

    private fun getDisplayRotation(): Int {
        return composeViewfinder.display?.rotation ?: display?.rotation ?: Surface.ROTATION_0
    }

    /**
     * Enables or disables the controls that rebind the camera, which would interrupt an active
     * recording.
     */
    private fun setRebindControlsEnabled(enabled: Boolean) {
        LowLightBoostAppHelper.setControlsEnabled(
            enabled,
            flipCameraButton,
            implementationModeGroup,
            lifecycleStrategyGroup,
            targetStrategyGroup,
        )
    }

    private fun updateStatus() {
        captureController.setStreaming(currentStreamState == "STREAMING")
        val isEnabled = lowLightBoostEffect?.isLowLightBoostEnabled ?: isLlbEnabled
        val errorText = bindErrorMessage
        if (errorText != null) {
            statusTextView.text = errorText
            toggleLlbButton.text =
                if (isEnabled) getString(R.string.llb_disable) else getString(R.string.llb_enable)
            return
        }
        val currentRotation = preview?.targetRotation ?: getDisplayRotation()
        LowLightBoostAppHelper.updateStatusUi(
            context = this,
            statusTextView = statusTextView,
            toggleLlbButton = toggleLlbButton,
            isEnabled = isEnabled,
            cameraSelector = cameraSelector,
            isSupported = isSupported,
            isCompatibleMode = implementationMode == ImplementationMode.EMBEDDED,
            targetSetting = targetSetting,
            rotation = currentRotation,
            currentLlbStatus = currentLlbStatus,
            currentLlbError = currentLlbError,
            streamStateText = currentStreamState,
        )
    }

    private fun toast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FACING_FRONT, cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
        outState.putString(
            KEY_IMPL_MODE,
            if (implementationMode == ImplementationMode.EMBEDDED) "COMPATIBLE" else "PERFORMANCE",
        )
        outState.putBoolean(KEY_REBIND_ON_RESUME, isRebindOnResume)
        outState.putInt(KEY_TARGET_SETTING, targetSetting)
        outState.putBoolean(KEY_LLB_ENABLED, isLlbEnabled)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindOwnUseCases()
        // Keep isLlbEnabled intact so state saved in onSaveInstanceState persists if recreated.
        releaseLowLightBoostEffect("onDestroy", resetEnabled = false)
        Log.d(TAG, "onDestroy")
    }

    internal companion object {
        internal const val EXTRA_BINDING_BEHAVIOR = "binding_behavior"
        internal const val EXTRA_MODE = "extra_mode"
        internal const val EXTRA_LENS = "extra_lens"
        internal const val EXTRA_TARGET = "extra_target"
        internal const val EXTRA_LLB_ENABLED = "extra_llb_enabled"

        // Intentionally monotonic count of camera capture results, used by instrumentation tests
        // (waitForFrames delta checks) to check that the camera stream keeps running. It doesn't
        // count rendered frames. Not displayed in UI status text and must not be reset on camera
        // rebind.
        private val captureResultCounter = AtomicLong(0)

        @get:VisibleForTesting
        internal val captureResultCount: Long
            get() = captureResultCounter.get()

        private const val TAG = "LowLightBoostEffectActivity"
        private const val KEY_FACING_FRONT = "key_facing_front"
        private const val KEY_IMPL_MODE = "key_impl_mode"
        private const val KEY_REBIND_ON_RESUME = "key_rebind_on_resume"
        private const val KEY_TARGET_SETTING = "key_target_setting"
        private const val KEY_LLB_ENABLED = "key_llb_enabled"
    }
}
