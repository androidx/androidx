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

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featurecombination.Feature
import androidx.camera.core.takePicture
import androidx.camera.integration.featurecombo.AppFeatures
import androidx.camera.integration.featurecombo.DynamicRange
import androidx.camera.integration.featurecombo.Fps
import androidx.camera.integration.featurecombo.ImageFormat
import androidx.camera.integration.featurecombo.MainActivity
import androidx.camera.integration.featurecombo.StabilizationMode
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalSessionConfig::class)
class CameraViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private lateinit var appContext: Context

    private val isCameraPipe: Boolean by lazy {
        savedStateHandle.get<String>(MainActivity.INTENT_EXTRA_CAMERA_IMPLEMENTATION) ==
            CAMERA_PIPE_IMPLEMENTATION_OPTION
    }

    private val cameraProviderDeferred = CompletableDeferred<ProcessCameraProvider>()

    private suspend fun cameraProvider() = cameraProviderDeferred.await()

    private lateinit var cameraSelector: CameraSelector

    private var preview = Preview.Builder().build()
    private val imageCapture = ImageCapture.Builder().build()
    private val videoCapture: VideoCapture<Recorder> =
        VideoCapture.withOutput(Recorder.Builder().build())

    private val useCases = mutableListOf<UseCase>()

    private val _toastMessages = MutableSharedFlow<String>()
    val toastMessages = _toastMessages.asSharedFlow()

    private val _isRearCamera = MutableStateFlow(true)
    val isRearCamera: StateFlow<Boolean>
        get() = _isRearCamera

    private val _isVideoMode = MutableStateFlow(true)
    val isVideoMode: StateFlow<Boolean>
        get() = _isVideoMode

    private val _featureUiList = MutableStateFlow(listOf<FeatureUi>())
    val featureUiList: StateFlow<List<FeatureUi>>
        get() = _featureUiList

    private val _useCaseDetails = MutableStateFlow("")
    val useCaseDetails: StateFlow<String>
        get() = _useCaseDetails

    private var appFeatures: AppFeatures =
        AppFeatures(
            dynamicRange = DynamicRange.SDR,
            fps = Fps.FPS_30,
            stabilizationMode = StabilizationMode.OFF,
            imageFormat = ImageFormat.JPEG,
        )

    data class FeatureCombination(
        val requiredFeatures: Set<Feature> = emptySet(),
        val preferredFeatures: List<Feature> = emptyList(),
    )

    private var featureCombination: FeatureCombination? = null

    private var bindStartTime: Long = Long.MIN_VALUE

    @androidx.annotation.OptIn(ExperimentalCameraProviderConfiguration::class)
    fun init(applicationContext: Context, lifecycleOwner: LifecycleOwner) {
        appContext = applicationContext

        ProcessCameraProvider.configureInstance(
            if (isCameraPipe) {
                CameraPipeConfig.defaultConfig()
            } else {
                Camera2Config.defaultConfig()
            }
        )

        viewModelScope.launch {
            with(ProcessCameraProvider.awaitInstance(applicationContext)) {
                cameraProviderDeferred.complete(this)
                initCameraSelector()

                if (!::cameraSelector.isInitialized) {
                    Log.e(TAG, "No camera found!")
                    return@launch
                }

                updateFeatureCombination(lifecycleOwner)
            }
        }
    }

    private suspend fun CameraSelector.getCameraInfo(): CameraInfo {
        return cameraProvider().getCameraInfo(this)
    }

    private suspend fun initCameraSelector() {
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

    @OptIn(ExperimentalSessionConfig::class)
    private suspend fun bindCamera(lifecycleOwner: LifecycleOwner) {
        Log.d(
            TAG,
            "bindCamera: isVideoMode.value = ${isVideoMode.value}" +
                ", featureCombination = $featureCombination, appFeatures = $appFeatures",
        )

        if (featureCombination == null) {
            showToast("No feature combination found!")
            return
        }

        val selectedFeatures = CompletableDeferred<Set<Feature>>()

        val sessionConfig =
            SessionConfig(
                    useCases = useCases,
                    requiredFeatures = featureCombination!!.requiredFeatures,
                    preferredFeatures = featureCombination!!.preferredFeatures,
                )
                .apply {
                    setFeatureSelectionListener { features ->
                        val duration = System.currentTimeMillis() - bindStartTime
                        if (features.isNotEmpty()) {
                            showToast(
                                "Default feature combination selected by CameraX" +
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
            updateAppFeatures(selectedFeatures.await().toAppFeatures())
            updateUnsupportedFeatures()
        }
    }

    fun setSurfaceProvider(surfaceProvider: Preview.SurfaceProvider) {
        preview.surfaceProvider = surfaceProvider
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
                updateFeatureCombination(lifecycleOwner)
            } else {
                showToast("newCamera($newCamera) is not supported!")
            }
        }
    }

    fun toggleVideoMode(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            _isVideoMode.value = !_isVideoMode.value
            updateFeatureCombination(lifecycleOwner)
        }
    }

    fun resetFeatureCombination(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch { updateFeatureCombination(lifecycleOwner) }
    }

    fun updateFeature(featureUi: FeatureUi, newValueIndex: Int, lifecycleOwner: LifecycleOwner) {
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
            }

        viewModelScope.launch {
            if (candidateAppFeatures.isSupported()) {
                updateAppFeatures(candidateAppFeatures)
                featureCombination = appFeatures.toFeatureCombination()
                bindCamera(lifecycleOwner)
                showToast("Camera configured with feature combination!")
                updateUnsupportedFeatures()
            } else {
                showToast("New feature combination not supported!")
            }
        }
    }

    private fun updateAppFeatures(appFeatures: AppFeatures) {
        Log.d(TAG, "updateAppFeatures: appFeatures = $appFeatures")
        this.appFeatures = appFeatures
        _featureUiList.value = appFeatures.toFeatureUiList(isVideoMode.value)
    }

    fun capture(context: Context) {
        if (isVideoMode.value) {
            recordVideo(context)
        } else {
            capturePhoto(context)
        }
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

            videoCapture.output.prepareRecording(context, mediaStoreOutput).start { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        showToast("Recording started")
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError() && event.error != ERROR_DURATION_LIMIT_REACHED) {
                            showToast("Recording error: ${event.error}")
                        } else {
                            showToast("Recording saved to: ${event.outputResults.outputUri}")
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateFeatureCombination(lifecycleOwner: LifecycleOwner) {
        useCases.clear()
        useCases.add(preview)
        if (isVideoMode.value) {
            useCases.add(videoCapture)
        } else {
            useCases.add(imageCapture)
        }

        if (isVideoMode.value) {
            featureCombination =
                FeatureCombination(
                    preferredFeatures =
                        listOf(Feature.HDR_HLG10, Feature.FPS_60, Feature.PREVIEW_STABILIZATION)
                )
        } else {
            featureCombination =
                FeatureCombination(
                    preferredFeatures =
                        listOf(
                            Feature.IMAGE_ULTRA_HDR,
                            Feature.HDR_HLG10,
                            Feature.PREVIEW_STABILIZATION,
                            Feature.FPS_60,
                        )
                )
        }

        bindCamera(lifecycleOwner)

        val previewSize = preview.resolutionInfo?.resolution
        if (isVideoMode.value) {
            val videoCaptureSize = videoCapture.resolutionInfo?.resolution
            _useCaseDetails.value =
                "Preview (${previewSize?.width} x ${previewSize?.height})" +
                    "\nVideoCapture (${videoCaptureSize?.width} x ${videoCaptureSize?.height})"
        } else {
            val imageCaptureSize = imageCapture.resolutionInfo?.resolution
            _useCaseDetails.value =
                "Preview (${previewSize?.width} x ${previewSize?.height})" +
                    "\nImageCapture (${imageCaptureSize?.width} x ${imageCaptureSize?.height})"
        }
    }

    private fun showToast(text: String) {
        Log.d(TAG, "showToast: text = $text")
        viewModelScope.launch { _toastMessages.emit(text) }
    }

    private fun updateUnsupportedFeatures() {
        viewModelScope.launch {
            val (appFeatures, duration) =
                measureTimedValue {
                    if (isVideoMode.value) {
                        getVideoModeUnsupportedFeatures()
                    } else {
                        getImageModeUnsupportedFeatures()
                    }
                }

            Logger.d(TAG, "updateUnsupportedFeatures: duration = $duration")

            updateAppFeatures(appFeatures)
        }
    }

    private suspend fun getVideoModeUnsupportedFeatures(): AppFeatures {
        return appFeatures.copy(
            unsupportedDynamicRanges =
                DynamicRange.entries.toTypedArray().getUnsupportedValues(appFeatures.dynamicRange) {
                    appFeatures.copy(dynamicRange = it)
                },
            unsupportedFps =
                Fps.entries.toTypedArray().getUnsupportedValues(appFeatures.fps) {
                    appFeatures.copy(fps = it)
                },
            unsupportedStabilizationModes =
                StabilizationMode.entries.toTypedArray().getUnsupportedValues(
                    appFeatures.stabilizationMode
                ) {
                    appFeatures.copy(stabilizationMode = it)
                },
        )
    }

    private suspend fun getImageModeUnsupportedFeatures(): AppFeatures {
        return appFeatures.copy(
            unsupportedImageFormats =
                ImageFormat.entries.toTypedArray().getUnsupportedValues(appFeatures.imageFormat) {
                    appFeatures.copy(imageFormat = it)
                },
            unsupportedDynamicRanges =
                DynamicRange.entries.toTypedArray().getUnsupportedValues(appFeatures.dynamicRange) {
                    appFeatures.copy(dynamicRange = it)
                },
            unsupportedFps =
                Fps.entries.toTypedArray().getUnsupportedValues(appFeatures.fps) {
                    appFeatures.copy(fps = it)
                },
            unsupportedStabilizationModes =
                StabilizationMode.entries.toTypedArray().getUnsupportedValues(
                    appFeatures.stabilizationMode
                ) {
                    appFeatures.copy(stabilizationMode = it)
                },
        )
    }

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
        return this.toFeatureCombination().isSupported()
    }

    @OptIn(ExperimentalSessionConfig::class)
    private suspend fun FeatureCombination.isSupported(): Boolean {
        Log.d(TAG, "isSupported: cameraSelector lensFacing = ${cameraSelector.lensFacing}")
        Log.d(TAG, "isSupported: useCases = $useCases")
        Log.d(TAG, "isSupported: featureCombination = $this")

        val isSupported =
            cameraSelector
                .getCameraInfo()
                .isFeatureCombinationSupported(
                    SessionConfig(useCases, requiredFeatures = requiredFeatures)
                )

        Log.d(TAG, "isSupported: isSupported = $isSupported")

        return isSupported
    }

    private fun AppFeatures.toFeatureCombination(): FeatureCombination {
        return FeatureCombination(requiredFeatures = toCameraXFeatures())
    }

    private fun AppFeatures.toCameraXFeatures(): Set<Feature> {
        val features = mutableSetOf<Feature>()

        if (!isVideoMode.value) {
            if (imageFormat == ImageFormat.JPEG_R) {
                features.add(Feature.IMAGE_ULTRA_HDR)
            }
        }

        if (dynamicRange == DynamicRange.HLG_10) {
            features.add(Feature.HDR_HLG10)
        }
        if (fps == Fps.FPS_60) {
            features.add(Feature.FPS_60)
        }
        if (stabilizationMode == StabilizationMode.PREVIEW) {
            features.add(Feature.PREVIEW_STABILIZATION)
        }

        return features
    }

    private fun Set<Feature>.toAppFeatures(): AppFeatures {
        var newAppFeatures = AppFeatures()

        forEach { feature ->
            when (feature) {
                Feature.HDR_HLG10 -> {
                    newAppFeatures = newAppFeatures.copy(dynamicRange = DynamicRange.HLG_10)
                }
                Feature.FPS_60 -> {
                    newAppFeatures = newAppFeatures.copy(fps = Fps.FPS_60)
                }
                Feature.PREVIEW_STABILIZATION -> {
                    newAppFeatures =
                        newAppFeatures.copy(stabilizationMode = StabilizationMode.PREVIEW)
                }
                Feature.IMAGE_ULTRA_HDR -> {
                    newAppFeatures = newAppFeatures.copy(imageFormat = ImageFormat.JPEG_R)
                }
            }
        }

        Log.d(TAG, "toAppFeatures: newAppFeatures = $newAppFeatures")

        return newAppFeatures
    }

    companion object {
        private const val TAG = "CamXFcqViewModel"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        const val CAMERA_PIPE_IMPLEMENTATION_OPTION: String = "camera_pipe"
    }
}
