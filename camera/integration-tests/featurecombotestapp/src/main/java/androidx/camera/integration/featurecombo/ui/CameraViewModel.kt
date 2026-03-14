/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.camera.integration.featurecombo.ui

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.takePicture
import androidx.camera.integration.featurecombo.AppFeatures
import androidx.camera.integration.featurecombo.AppUseCase
import androidx.camera.integration.featurecombo.AppUseCase.Companion.getSupportedGroupableFeatures
import androidx.camera.integration.featurecombo.AppUseCase.IMAGE_ANALYSIS
import androidx.camera.integration.featurecombo.AppUseCase.IMAGE_CAPTURE
import androidx.camera.integration.featurecombo.AppUseCase.PREVIEW
import androidx.camera.integration.featurecombo.AppUseCase.VIDEO_CAPTURE
import androidx.camera.integration.featurecombo.DynamicRange
import androidx.camera.integration.featurecombo.Effect
import androidx.camera.integration.featurecombo.Fps
import androidx.camera.integration.featurecombo.ImageFormat
import androidx.camera.integration.featurecombo.PrimitiveCollections.distinctIntList
import androidx.camera.integration.featurecombo.RecordingQuality
import androidx.camera.integration.featurecombo.StabilizationMode
import androidx.camera.integration.featurecombo.effects.BouncyLogoOverlayEffect
import androidx.camera.integration.featurecombo.effects.BouncyLogoOverlayEffect.Companion.supportsEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.GroupableFeatures
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCamera2Interop::class)
class CameraViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private lateinit var appContext: Context
    private var bouncyLogoOverlayEffect: BouncyLogoOverlayEffect? = null

    private val cameraProviderDeferred = CompletableDeferred<ProcessCameraProvider>()

    private suspend fun cameraProvider() = cameraProviderDeferred.await()

    private lateinit var cameraSelector: CameraSelector

    private val captureFrameCount = AtomicInteger(0)
    private val lastFrameNumber = AtomicLong(-1)

    private val _cameraCaptureFps = MutableStateFlow(0)
    val cameraCaptureFps: StateFlow<Int>
        get() = _cameraCaptureFps

    private val captureCallback =
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult,
            ) {
                val currentFrame = result.frameNumber
                if (lastFrameNumber.getAndSet(currentFrame) != currentFrame) {
                    captureFrameCount.incrementAndGet()
                }
            }
        }

    private fun <T : androidx.camera.core.ExtendableBuilder<*>> T.withCaptureCallback(): T {
        Camera2Interop.Extender(this).setSessionCaptureCallback(captureCallback)
        return this
    }

    private var preview = Preview.Builder().withCaptureCallback().build()
    private val imageCapture = ImageCapture.Builder().withCaptureCallback().build()
    private val imageAnalysis = ImageAnalysis.Builder().withCaptureCallback().build()
    private val videoCapture: VideoCapture<Recorder> =
        VideoCapture.Builder(Recorder.Builder().build()).withCaptureCallback().build()

    private val _toastMessages = MutableSharedFlow<String>()
    private var activeRecording: Recording? = null
    val toastMessages = _toastMessages.asSharedFlow()

    private val _isRearCamera = MutableStateFlow(true)
    val isRearCamera: StateFlow<Boolean>
        get() = _isRearCamera

    private val _isUseCaseEnabled = MutableStateFlow(DEFAULT_USE_CASES)
    val isUseCaseEnabled: StateFlow<Map<AppUseCase, Boolean>>
        get() = _isUseCaseEnabled

    private val appUseCases: Set<AppUseCase>
        get() = isUseCaseEnabled.value.filterValues { it }.keys

    private val useCases: List<UseCase>
        get() = appUseCases.toUseCases()

    private val useCaseSupportedFeatures: List<GroupableFeature>
        get() = appUseCases.getSupportedGroupableFeatures()

    private val _featureUiList = MutableStateFlow(listOf<FeatureUi>())
    val featureUiList: StateFlow<List<FeatureUi>>
        get() = _featureUiList

    private val _useCaseResolutions =
        MutableStateFlow(
            buildMap {
                put(PREVIEW, "")
                put(IMAGE_CAPTURE, "")
                put(VIDEO_CAPTURE, "")
            }
        )

    val useCaseResolutions: StateFlow<Map<AppUseCase, String>>
        get() = _useCaseResolutions

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean>
        get() = _isRecording

    private val _imageAnalysisFrameCount = MutableStateFlow(0)
    val imageAnalysisFrameCount: StateFlow<Int>
        get() = _imageAnalysisFrameCount

    private var appFeatures: AppFeatures =
        AppFeatures(
            dynamicRange = DynamicRange.SDR,
            fps = Fps.FPS_30,
            stabilizationMode = StabilizationMode.OFF,
            imageFormat = ImageFormat.JPEG,
            recordingQuality = RecordingQuality.SD,
        )

    data class FeatureCombo(
        val requiredFeatures: Set<GroupableFeature> = emptySet(),
        val preferredFeatures: List<GroupableFeature> = emptyList(),
        val effects: List<Effect> = emptyList(),
    )

    private var featureCombo: FeatureCombo? = null

    private var bindStartTime: Long = Long.MIN_VALUE

    private var imageAnalysisJob: Job? = null

    init {
        viewModelScope.launch {
            var lastFrameCount = captureFrameCount.get()
            var lastFpsTime = SystemClock.elapsedRealtime()
            while (true) {
                delay(1_000)
                val currentTime = SystemClock.elapsedRealtime()
                val currentFrameCount = captureFrameCount.get()

                val timeDiff = currentTime - lastFpsTime
                if (timeDiff > 0) {
                    val frameDiff = currentFrameCount - lastFrameCount
                    _cameraCaptureFps.value = ((frameDiff * 1000.0) / timeDiff).toInt()
                }

                lastFpsTime = currentTime
                lastFrameCount = currentFrameCount
            }
        }
    }

    fun setupCamera(applicationContext: Context, lifecycleOwner: LifecycleOwner) {
        appContext = applicationContext

        viewModelScope.launch {
            with(ProcessCameraProvider.awaitInstance(applicationContext)) {
                cameraProviderDeferred.complete(this)
                initCameraSelector()

                if (!::cameraSelector.isInitialized) {
                    Log.e(TAG, "No camera found!")
                    return@launch
                }

                if (featureCombo == null) {
                    reconfigureUseCasesAndFeatureCombo(lifecycleOwner)
                } else {
                    bindCamera(lifecycleOwner)
                }
            }
        }
    }

    private suspend fun CameraSelector.getCameraInfo(): CameraInfo {
        return cameraProvider().getCameraInfo(this)
    }

    private suspend fun initCameraSelector() {
        if (::cameraSelector.isInitialized) return

        if (cameraProvider().hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            _isRearCamera.value = true
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        } else if (cameraProvider().hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            _isRearCamera.value = false
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            showToast("No camera found!")
        }
    }

    private suspend fun bindCamera(lifecycleOwner: LifecycleOwner) {
        Log.d(
            TAG,
            "bindCamera: useCases = $useCases" +
                ", featureCombo = $featureCombo, appFeatures = $appFeatures",
        )

        val featureCombo = featureCombo // snapshot for nullability

        if (featureCombo == null) {
            showToast("No feature combination found!")
            return
        }

        val selectedFeatures = CompletableDeferred<Set<GroupableFeature>>()

        val sessionConfig =
            SessionConfig(
                    useCases = useCases,
                    requiredFeatureGroup = featureCombo.requiredFeatures,
                    preferredFeatureGroup = featureCombo.preferredFeatures,
                    effects = featureCombo.effects.toCameraXEffects(),
                )
                .apply {
                    setFeatureSelectionListener { features ->
                        val duration = System.currentTimeMillis() - bindStartTime
                        if (features.isNotEmpty()) {
                            showToast(
                                "Features selected" +
                                    (if (bindStartTime != Long.MIN_VALUE) " in $duration ms"
                                    else "")
                            )
                        } else {
                            showToast(
                                "No feature combination supported!" +
                                    " $duration ms elapsed after bind start."
                            )
                        }

                        Logger.d(TAG, "Selected features: $features")
                        selectedFeatures.complete(features)
                    }
                }

        cameraProvider().unbindAll() // TODO: Check why this is needed while switching camera

        bindStartTime = System.currentTimeMillis()

        cameraProvider().bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)

        viewModelScope.launch {
            updateAppFeatures(
                selectedFeatures
                    .await()
                    .toAppFeatures()
                    .copy(effect = featureCombo.effects.firstOrNull() ?: Effect.NONE)
            )
            updateUnsupportedFeatures()
        }

        updateUseCaseResolutions()
    }

    private fun updateUseCaseResolutions() {
        AppUseCase.entries.forEach {
            if (isUseCaseEnabled.value[it] == true) {
                val useCaseResolution =
                    when (it) {
                        PREVIEW -> preview.resolutionInfo?.resolution
                        IMAGE_CAPTURE -> imageCapture.resolutionInfo?.resolution
                        IMAGE_ANALYSIS -> imageAnalysis.resolutionInfo?.resolution
                        VIDEO_CAPTURE -> videoCapture.resolutionInfo?.resolution
                    }

                _useCaseResolutions.update { oldMap ->
                    val newMap = oldMap.toMutableMap()
                    newMap[it] = "(${useCaseResolution?.width} x ${useCaseResolution?.height})"
                    newMap
                }
            }
        }
    }

    fun setSurfaceProvider(surfaceProvider: Preview.SurfaceProvider?) {
        preview.surfaceProvider = surfaceProvider
    }

    fun setBouncyLogoOverlayEffect(
        bouncyLogoOverlayEffect: BouncyLogoOverlayEffect?,
        lifecycleOwner: LifecycleOwner,
    ) {
        this.bouncyLogoOverlayEffect = bouncyLogoOverlayEffect
        viewModelScope.launch { bindCamera(lifecycleOwner) }
    }

    fun toggleCamera(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            val newCamera =
                if (!isRearCamera.value) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

            if (cameraProvider().hasCamera(newCamera)) {
                _isRearCamera.value = !_isRearCamera.value
                cameraSelector = newCamera

                // If the use case has already been bound to the previous camera, it may throw an
                // exception if not unbound first
                cameraProvider().unbindAll() // TODO
                reconfigureUseCasesAndFeatureCombo(lifecycleOwner)
            } else {
                showToast("newCamera($newCamera) is not supported!")
            }
        }
    }

    fun toggleUseCase(lifecycleOwner: LifecycleOwner, useCase: AppUseCase) {
        viewModelScope.launch {
            _isUseCaseEnabled.update { oldMap ->
                val newMap = oldMap.toMutableMap()
                val newEnabledState = !(newMap[useCase] ?: false)
                newMap[useCase] = newEnabledState

                newMap
            }

            if (useCase == IMAGE_ANALYSIS) {
                updateAnalyzer()
            }

            Log.d(TAG, "toggleUseCase: isUseCaseEnabled = ${isUseCaseEnabled.value}")

            reconfigureFeatureCombo(lifecycleOwner)
        }
    }

    private fun updateAnalyzer() {
        if (isUseCaseEnabled.value[IMAGE_ANALYSIS] == true) {
            val frameCount = AtomicInteger(0)

            imageAnalysis.setAnalyzer(Dispatchers.Default.asExecutor()) {
                frameCount.incrementAndGet()
                it.close()
            }

            imageAnalysisJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1.seconds)
                        _imageAnalysisFrameCount.value = (frameCount.get() + 5) / 10 * 10
                    }
                }
        } else {
            imageAnalysisJob?.cancel()
            imageAnalysis.clearAnalyzer()
        }
    }

    fun resetUseCasesAndFeatureCombo(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch { reconfigureUseCasesAndFeatureCombo(lifecycleOwner) }
    }

    fun updateFeature(featureUi: FeatureUi, newValueIndex: Int, lifecycleOwner: LifecycleOwner) {
        Log.d(TAG, "updateFeature: featureUi = $featureUi, newValueIndex = $newValueIndex")

        val candidateAppFeatures =
            when (featureUi.title) {
                AppFeatureTitle.HDR -> {
                    appFeatures.copy(dynamicRange = DynamicRange.entries[newValueIndex])
                }
                AppFeatureTitle.FPS -> {
                    appFeatures.copy(fps = Fps.entries[newValueIndex])
                }
                AppFeatureTitle.STABILIZATION -> {
                    appFeatures.copy(stabilizationMode = StabilizationMode.entries[newValueIndex])
                }
                AppFeatureTitle.IMAGE_FORMAT -> {
                    appFeatures.copy(imageFormat = ImageFormat.entries[newValueIndex])
                }
                AppFeatureTitle.RECORDING_QUALITY -> {
                    appFeatures.copy(recordingQuality = RecordingQuality.entries[newValueIndex])
                }
                AppFeatureTitle.EFFECT -> {
                    appFeatures.copy(effect = Effect.entries[newValueIndex])
                }
            }

        viewModelScope.launch {
            Log.d(TAG, "updateFeature: candidateAppFeatures = $candidateAppFeatures")

            if (candidateAppFeatures.isSupported()) {
                updateAppFeatures(candidateAppFeatures)
                featureCombo = appFeatures.toFeatureCombo()
                bindCamera(lifecycleOwner)
                updateUnsupportedFeatures()
            } else {
                showToast("New feature combination not supported!")
            }
        }
    }

    private fun updateAppFeatures(appFeatures: AppFeatures) {
        Log.d(TAG, "updateAppFeatures: appFeatures = $appFeatures")
        this.appFeatures = appFeatures
        _featureUiList.value = appFeatures.toFeatureUiList(appUseCases)
    }

    fun capture(context: Context) {
        require(isUseCaseEnabled.value[IMAGE_CAPTURE] == true) {
            "Capture is not supported without ImageCapture!"
        }
        capturePhoto(context)
    }

    fun record(context: Context) {
        require(isUseCaseEnabled.value[VIDEO_CAPTURE] == true) {
            "Recording is not supported without VideoCapture!"
        }
        recordVideo(context)
    }

    private fun capturePhoto(context: Context) {
        viewModelScope.launch {
            val name =
                "CameraXFcq_" +
                    SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraXFcq")
                }

            val outputFileOptions =
                ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues,
                    )
                    .build()

            val outputResults = imageCapture.takePicture(outputFileOptions)
            showToast("Image saved to: ${outputResults.savedUri}")
        }
    }

    private fun recordVideo(context: Context) {
        viewModelScope.launch {
            val name =
                "CameraXFcq_" +
                    SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraXFcq")
                }
            val mediaStoreOutput =
                MediaStoreOutputOptions.Builder(
                        context.contentResolver,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    )
                    .setContentValues(contentValues)
                    .setDurationLimitMillis(2_050)
                    .build()

            activeRecording =
                videoCapture.output.prepareRecording(context, mediaStoreOutput).start { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            _isRecording.value = true
                            showToast("Recording started")
                        }
                        is VideoRecordEvent.Finalize -> {
                            _isRecording.value = false
                            if (event.hasError() && event.error != ERROR_DURATION_LIMIT_REACHED) {
                                showToast("Recording error: ${event.error}")
                            } else {
                                showToast("Recording saved to: ${event.outputResults.outputUri}")
                            }
                            activeRecording = null
                        }
                    }
                }
        }
    }

    private suspend fun reconfigureUseCasesAndFeatureCombo(lifecycleOwner: LifecycleOwner) {
        _isUseCaseEnabled.value = DEFAULT_USE_CASES

        reconfigureFeatureCombo(lifecycleOwner)
    }

    private suspend fun reconfigureFeatureCombo(lifecycleOwner: LifecycleOwner) {
        _imageAnalysisFrameCount.value = 0

        featureCombo = FeatureCombo(preferredFeatures = useCaseSupportedFeatures)

        bindCamera(lifecycleOwner)
    }

    private fun showToast(text: String) {
        Log.d(TAG, "showToast: text = $text")
        viewModelScope.launch { _toastMessages.emit(text) }
    }

    private fun updateUnsupportedFeatures() {
        viewModelScope.launch {
            val (appFeatures, duration) = measureTimedValue { getUnsupportedFeatures() }

            Logger.d(TAG, "updateUnsupportedFeatures: duration = $duration")

            updateAppFeatures(appFeatures)
        }
    }

    private suspend fun getUnsupportedFeatures(): AppFeatures {
        var newAppFeatures = appFeatures.copy()
        val featureTypes = useCaseSupportedFeatures.map { it.featureType }.distinctIntList()

        Log.d(TAG, "getUnsupportedFeatures: featureTypes = $featureTypes")

        if (featureTypes.contains(GroupableFeature.FEATURE_TYPE_IMAGE_FORMAT)) {
            newAppFeatures =
                newAppFeatures.copy(
                    unsupportedImageFormats =
                        ImageFormat.entries.toTypedArray().getUnsupportedValues(
                            appFeatures.imageFormat
                        ) {
                            appFeatures.copy(imageFormat = it)
                        }
                )
        }

        if (featureTypes.contains(GroupableFeature.FEATURE_TYPE_RECORDING_QUALITY)) {
            newAppFeatures =
                newAppFeatures.copy(
                    unsupportedRecordingQualities =
                        RecordingQuality.entries.toTypedArray().getUnsupportedValues(
                            appFeatures.recordingQuality
                        ) {
                            appFeatures.copy(recordingQuality = it)
                        }
                )
        }

        if (featureTypes.contains(GroupableFeature.FEATURE_TYPE_DYNAMIC_RANGE)) {
            newAppFeatures =
                newAppFeatures.copy(
                    unsupportedDynamicRanges =
                        DynamicRange.entries.toTypedArray().getUnsupportedValues(
                            appFeatures.dynamicRange
                        ) {
                            appFeatures.copy(dynamicRange = it)
                        }
                )
        }

        if (featureTypes.contains(GroupableFeature.FEATURE_TYPE_FPS_RANGE)) {
            newAppFeatures =
                newAppFeatures.copy(
                    unsupportedFps =
                        Fps.entries.toTypedArray().getUnsupportedValues(appFeatures.fps) {
                            appFeatures.copy(fps = it)
                        }
                )
        }

        if (featureTypes.contains(GroupableFeature.FEATURE_TYPE_VIDEO_STABILIZATION)) {
            newAppFeatures =
                newAppFeatures.copy(
                    unsupportedStabilizationModes =
                        StabilizationMode.entries.toTypedArray().getUnsupportedValues(
                            appFeatures.stabilizationMode
                        ) {
                            appFeatures.copy(stabilizationMode = it)
                        }
                )
        }

        if (appUseCases.supportsEffect()) {
            newAppFeatures =
                newAppFeatures.copy(
                    unsupportedEffects =
                        Effect.entries.toTypedArray().getUnsupportedValues(appFeatures.effect) {
                            appFeatures.copy(effect = it)
                        }
                )
        }

        return newAppFeatures
    }

    /**
     * Returns the list of unsupported values from the receiver array of values.
     *
     * @param currentValue Currently selected value.
     * @param newAppFeatures The function to create a new [AppFeatures] using a specific value.
     */
    private suspend fun <T> Array<T>.getUnsupportedValues(
        currentValue: T,
        newAppFeatures: (T) -> AppFeatures,
    ): List<T> {
        val unsupportedValues = mutableListOf<T>()
        forEach {
            if (it == currentValue) return@forEach
            if (!newAppFeatures(it).isSupported()) {
                unsupportedValues.add(it)
            }
        }
        return unsupportedValues
    }

    private suspend fun AppFeatures.isSupported(): Boolean {
        return this.toFeatureCombo().isSupported()
    }

    private suspend fun FeatureCombo.isSupported(): Boolean {
        Log.d(TAG, "isSupported: cameraSelector lensFacing = ${cameraSelector.lensFacing}")
        Log.d(TAG, "isSupported: useCases = $useCases")
        Log.d(TAG, "isSupported: featureCombo = $this")

        val isSupported =
            cameraSelector
                .getCameraInfo()
                .isSessionConfigSupported(
                    SessionConfig(
                        useCases,
                        requiredFeatureGroup = requiredFeatures,
                        effects = effects.toCameraXEffects(),
                    )
                )

        Log.d(TAG, "isSupported: isSupported = $isSupported")

        return isSupported
    }

    private fun AppFeatures.toFeatureCombo(): FeatureCombo {
        return FeatureCombo(requiredFeatures = toCameraXFeatures(), effects = listOf(effect))
    }

    private fun AppFeatures.toCameraXFeatures(): Set<GroupableFeature> {
        val features = mutableSetOf<GroupableFeature>()

        val featureTypes = useCaseSupportedFeatures.map { it.featureType }.distinctIntList()

        if (featureTypes.contains(GroupableFeature.FEATURE_TYPE_RECORDING_QUALITY)) {
            when (recordingQuality) {
                RecordingQuality.UHD -> features.add(GroupableFeatures.UHD_RECORDING)
                RecordingQuality.FHD -> features.add(GroupableFeatures.FHD_RECORDING)
                RecordingQuality.HD -> features.add(GroupableFeatures.HD_RECORDING)
                RecordingQuality.SD -> features.add(GroupableFeatures.SD_RECORDING)
            }
        }

        if (
            featureTypes.contains(GroupableFeature.FEATURE_TYPE_IMAGE_FORMAT) &&
                imageFormat == ImageFormat.JPEG_R
        ) {
            features.add(GroupableFeature.IMAGE_ULTRA_HDR)
        }

        if (
            featureTypes.contains(GroupableFeature.FEATURE_TYPE_DYNAMIC_RANGE) &&
                dynamicRange == DynamicRange.HLG_10
        ) {
            features.add(GroupableFeature.HDR_HLG10)
        }

        if (featureTypes.contains(GroupableFeature.FEATURE_TYPE_FPS_RANGE) && fps == Fps.FPS_60) {
            features.add(GroupableFeature.FPS_60)
        }

        if (featureTypes.contains(GroupableFeature.FEATURE_TYPE_VIDEO_STABILIZATION)) {
            when (stabilizationMode) {
                StabilizationMode.PREVIEW -> features.add(GroupableFeature.PREVIEW_STABILIZATION)
                StabilizationMode.VIDEO -> features.add(GroupableFeatures.VIDEO_STABILIZATION)
                else -> {}
            }
        }

        return features
    }

    private fun Collection<Effect>.toCameraXEffects(): List<CameraEffect> {
        return mapNotNull {
            when (it) {
                Effect.BOUNCY_LOGO_EFFECT ->
                    bouncyLogoOverlayEffect
                        ?: null.also {
                            Log.e(TAG, "toCameraEffects: bouncyLogoOverlayEffect is null!")
                        }

                Effect.NONE -> null
            }
        }
    }

    private fun Set<GroupableFeature>.toAppFeatures(): AppFeatures {
        var newAppFeatures = AppFeatures()

        forEach { feature ->
            when (feature) {
                GroupableFeatures.UHD_RECORDING -> {
                    newAppFeatures = newAppFeatures.copy(recordingQuality = RecordingQuality.UHD)
                }
                GroupableFeatures.FHD_RECORDING -> {
                    newAppFeatures = newAppFeatures.copy(recordingQuality = RecordingQuality.FHD)
                }
                GroupableFeatures.HD_RECORDING -> {
                    newAppFeatures = newAppFeatures.copy(recordingQuality = RecordingQuality.HD)
                }
                GroupableFeatures.SD_RECORDING -> {
                    newAppFeatures = newAppFeatures.copy(recordingQuality = RecordingQuality.SD)
                }
                GroupableFeature.HDR_HLG10 -> {
                    newAppFeatures = newAppFeatures.copy(dynamicRange = DynamicRange.HLG_10)
                }
                GroupableFeature.FPS_60 -> {
                    newAppFeatures = newAppFeatures.copy(fps = Fps.FPS_60)
                }
                GroupableFeature.PREVIEW_STABILIZATION -> {
                    newAppFeatures =
                        newAppFeatures.copy(stabilizationMode = StabilizationMode.PREVIEW)
                }
                GroupableFeatures.VIDEO_STABILIZATION -> {
                    newAppFeatures =
                        newAppFeatures.copy(stabilizationMode = StabilizationMode.VIDEO)
                }
                GroupableFeature.IMAGE_ULTRA_HDR -> {
                    newAppFeatures = newAppFeatures.copy(imageFormat = ImageFormat.JPEG_R)
                }
            }
        }

        Log.d(TAG, "toAppFeatures: newAppFeatures = $newAppFeatures")

        return newAppFeatures
    }

    private fun Collection<AppUseCase>.toUseCases(): List<UseCase> {
        return map {
            when (it) {
                PREVIEW -> preview
                IMAGE_CAPTURE -> imageCapture
                IMAGE_ANALYSIS -> imageAnalysis
                VIDEO_CAPTURE -> videoCapture
            }
        }
    }

    companion object {
        private const val TAG = "CamXFcqViewModel"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        private val DEFAULT_USE_CASES = buildMap {
            put(PREVIEW, true)
            put(IMAGE_CAPTURE, true)
            put(VIDEO_CAPTURE, true)
            put(IMAGE_ANALYSIS, false)
        }
    }
}
