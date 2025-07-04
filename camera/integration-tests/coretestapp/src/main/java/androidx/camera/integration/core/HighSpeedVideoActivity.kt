/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.camera.integration.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Log.DEBUG
import android.util.Log.ERROR
import android.util.Log.INFO
import android.util.Log.VERBOSE
import android.util.Log.WARN
import android.util.Range
import android.util.Rational
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import android.widget.ToggleButton
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.AspectRatio.RATIO_DEFAULT
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.Preview
import androidx.camera.integration.core.button.FrameRateButton
import androidx.camera.integration.core.button.VideoQualityButton
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.FileUtil.canDeviceWriteToMediaStore
import androidx.camera.testing.impl.FileUtil.generateVideoFileOutputOptions
import androidx.camera.testing.impl.FileUtil.generateVideoMediaStoreOptions
import androidx.camera.testing.impl.FileUtil.getAbsolutePathFromUri
import androidx.camera.video.ExperimentalHighSpeedVideo
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.HighSpeedVideoSessionConfig
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/** Activity for verifying behavior of high speed video recording. */
@kotlin.OptIn(ExperimentalSessionConfig::class, ExperimentalHighSpeedVideo::class)
@SuppressLint("RestrictedApiAndroidX", "NullAnnotationGroup")
class HighSpeedVideoActivity : AppCompatActivity() {

    // Views
    private val previewView: PreviewView by lazy { findViewById(R.id.camera_preview) }
    private val switchCameraButton: ImageButton by lazy { findViewById(R.id.switch_camera) }
    private val recordButton: Button by lazy { findViewById(R.id.record) }
    private val pauseButton: Button by lazy { findViewById(R.id.pause) }
    private val qualityButton: VideoQualityButton by lazy { findViewById(R.id.quality_button) }
    private val frameRateButton: FrameRateButton by lazy { findViewById(R.id.frame_rate_button) }
    private val audioEnableButton: ToggleButton by lazy { findViewById(R.id.audio_enable) }
    private val recordStatus: TextView by lazy { findViewById(R.id.record_status) }

    private val dynamicRange = DynamicRange.SDR
    private var cameraSelector = DEFAULT_BACK_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder?>? = null
    private var sessionConfigBuilder: HighSpeedVideoSessionConfig.Builder? = null
    private var cameraInfo: CameraInfo? = null
    private var videoCapabilitiesSource = VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE
    private var recording: Recording? = null
    private var slowMotionVideoEnabled = false
    private var targetAspectRatio = RATIO_DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_speed_video)

        setupCameraConfig()
        setupViewsAndButtons()

        if (allPermissionsGranted()) {
            prepareCameraProvider()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    @SuppressLint("VisibleForTests")
    @OptIn(ExperimentalCameraProviderConfiguration::class)
    private fun setupCameraConfig() {
        val cameraConfig =
            pendingCameraConfig?.takeIf { it != currentCameraConfig } ?: currentCameraConfig
        ProcessCameraProvider.shutdown()
        ProcessCameraProvider.configureInstance(cameraConfig.cameraXConfig)
        currentCameraConfig = cameraConfig
        pendingCameraConfig = null
    }

    private fun setupViewsAndButtons() {
        setTitle(currentCameraConfig.header)
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE)
        switchCameraButton.setOnClickListener { _ -> switchCamera() }
        recordButton.setOnClickListener { _ ->
            if (recording == null) {
                startRecording()
            } else {
                stopRecording()
            }
        }
        pauseButton.setOnClickListener { _ ->
            if (pauseButton.text == TEXT_PAUSE) {
                pauseRecording()
            } else {
                resumeRecording()
            }
        }
        qualityButton.setOnItemChangedListener { _ -> prepareCamera() }
        frameRateButton.setOnItemChangedListener { _ -> prepareCamera() }
    }

    private fun prepareCameraProvider() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                try {
                    cameraProvider = cameraProviderFuture.get()
                    prepareCamera()
                } catch (e: ExecutionException) {
                    logWarnAndToast("Failed to get CameraProvider by $e", e)
                } catch (e: InterruptedException) {
                    logWarnAndToast("Failed to get CameraProvider by $e", e)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun prepareCamera() {
        disableRecordingControls()

        try {
            cameraInfo = cameraProvider!!.getCameraInfo(cameraSelector)
        } catch (e: IllegalArgumentException) {
            logWarnAndToast("No available camera can be found.$e", e)
            return
        }

        val videoCapabilities =
            Recorder.getHighSpeedVideoCapabilities(cameraInfo!!, videoCapabilitiesSource)

        if (videoCapabilities == null) {
            logWarnAndToast(
                "Camera with lens facing ${cameraInfo!!.lensFacing} doesn't support high-speed video recording."
            )
            return
        }

        val qualities = videoCapabilities.getSupportedQualities(dynamicRange)
        Log.d(TAG, "Supported qualities = $qualities for $dynamicRange")
        if (qualities.isEmpty()) {
            logWarnAndToast("No supported quality for $dynamicRange")
            return
        }

        qualityButton.apply {
            isEnabled = true
            setAllowedItems(qualities)
        }

        val preview =
            Preview.Builder().build().also { it.surfaceProvider = previewView.getSurfaceProvider() }

        val quality = qualityButton.getSelectedItem()
        val recorder =
            Recorder.Builder()
                .apply {
                    setAspectRatio(targetAspectRatio)
                    setVideoCapabilitiesSource(videoCapabilitiesSource)
                    quality?.let { setQualitySelector(QualitySelector.from(it)) }
                }
                .build()

        videoCapture = VideoCapture.Builder(recorder).setDynamicRange(dynamicRange).build()

        sessionConfigBuilder =
            HighSpeedVideoSessionConfig.Builder(videoCapture!!)
                .setPreview(preview)
                .setSlowMotionEnabled(slowMotionVideoEnabled)

        val supportedFpsRanges =
            cameraInfo!!.getSupportedFrameRateRanges(sessionConfigBuilder!!.build())
        Log.d(TAG, "Supported FPS = $supportedFpsRanges for Quality $quality")
        if (supportedFpsRanges.isEmpty()) {
            logWarnAndToast("No supported frame rate ranges for Quality $quality")
            return
        }

        frameRateButton.apply {
            isEnabled = true
            setAllowedItems(supportedFpsRanges)
            if (slowMotionVideoEnabled) {
                setIconNameProvider { it.toSlowMotionIconName() }
                setMenuItemNameProvider { it.toSlowMotionMenuItemName() }
            } else {
                resetNameProviders()
            }
        }

        bindSessionConfig()
    }

    private fun bindSessionConfig() {
        try {
            cameraProvider!!.unbindAll()

            sessionConfigBuilder!!.setFrameRateRange(frameRateButton.getSelectedItem()!!)

            cameraProvider!!.bindToLifecycle(this, cameraSelector, sessionConfigBuilder!!.build())
            enableRecordingControls()
        } catch (e: IllegalArgumentException) {
            logWarnAndToast("Fail to bind use cases by $e", e)
        }
    }

    private fun switchCamera() {
        cameraProvider!!.unbindAll()
        cameraSelector =
            when (cameraInfo!!.lensFacing) {
                LENS_FACING_BACK -> DEFAULT_FRONT_CAMERA
                LENS_FACING_FRONT -> DEFAULT_BACK_CAMERA
                else -> throw IllegalStateException("Invalid camera lens facing.")
            }
        prepareCamera()
    }

    private fun enableRecordingControls() {
        qualityButton.isEnabled = true
        frameRateButton.isEnabled = true
        recordButton.isEnabled = true
        pauseButton.isEnabled = true
        audioEnableButton.isEnabled = true
    }

    private fun disableRecordingControls() {
        qualityButton.isEnabled = false
        frameRateButton.isEnabled = false
        recordButton.isEnabled = false
        pauseButton.isEnabled = false
        audioEnableButton.isEnabled = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.high_speed_video_activity_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(ID_TO_CAMERA_CONFIG_MAP.getKey(currentCameraConfig)!!).isChecked = true
        menu.findItem(R.id.slow_motion_video_enabled).isChecked = slowMotionVideoEnabled
        menu.findItem(ID_TO_ASPECT_RATIO_MAP.getKey(targetAspectRatio)!!).isChecked = true
        menu
            .findItem(ID_TO_VIDEO_CAPABILITIES_SOURCE_MAP.getKey(videoCapabilitiesSource)!!)
            .isChecked = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(
            TAG,
            "onOptionsItemSelected: item = ${item.title}, groupId = ${item.groupId}, itemId = ${item.itemId}",
        )

        var shouldResetCamera = false

        when (item.groupId) {
            R.id.camera_config_group -> {
                val newCameraConfig = ID_TO_CAMERA_CONFIG_MAP[item.itemId]!!
                if (newCameraConfig != currentCameraConfig) {
                    pendingCameraConfig = newCameraConfig
                    recreate()
                    return true
                }
            }
            R.id.aspect_ratio_group -> {
                val ratio = ID_TO_ASPECT_RATIO_MAP[item.itemId]!!
                if (targetAspectRatio != ratio) {
                    targetAspectRatio = ratio
                    shouldResetCamera = true
                }
            }
            R.id.video_capabilities_source_group -> {
                val newVideoSource = ID_TO_VIDEO_CAPABILITIES_SOURCE_MAP[item.itemId]!!
                if (videoCapabilitiesSource != newVideoSource) {
                    videoCapabilitiesSource = newVideoSource
                    shouldResetCamera = true
                }
            }
        }

        when (item.itemId) {
            R.id.slow_motion_video_enabled -> {
                slowMotionVideoEnabled = !slowMotionVideoEnabled
                shouldResetCamera = true
            }
        }

        item.isChecked = !item.isChecked
        if (shouldResetCamera) {
            prepareCamera()
        }

        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        recordButton.text = TEXT_STOP
        pauseButton.visibility = View.VISIBLE
        pauseButton.text = TEXT_PAUSE
        recordStatus.visibility = View.VISIBLE
        recordStatus.text = ""
        frameRateButton.isEnabled = false
        qualityButton.isEnabled = false
        audioEnableButton.isEnabled = false
        val fileNamePrefix: String =
            if (slowMotionVideoEnabled) SLOW_MOTION_VIDEO_FILE_PREFIX
            else HIGH_SPEED_VIDEO_FILE_PREFIX
        val videoFileName = fileNamePrefix + "_" + System.currentTimeMillis()

        val pendingRecording =
            if (canDeviceWriteToMediaStore()) {
                    videoCapture!!
                        .getOutput()
                        .prepareRecording(
                            this,
                            generateVideoMediaStoreOptions(contentResolver, videoFileName),
                        )
                } else {
                    videoCapture!!
                        .getOutput()
                        .prepareRecording(
                            this,
                            generateVideoFileOutputOptions(videoFileName, "mp4"),
                        )
                }
                .apply {
                    if (audioEnableButton.isChecked) {
                        withAudioEnabled()
                    }
                }

        recording =
            pendingRecording.start(ContextCompat.getMainExecutor(this)) { videoRecordEvent ->
                updateRecordStatus(videoRecordEvent)
                if (videoRecordEvent is VideoRecordEvent.Finalize) {
                    handleFinalizeEvent(videoRecordEvent)
                    recording = null
                    recordButton.text = TEXT_START
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        recordButton.text = "Stopping"
        pauseButton.visibility = View.GONE
        recordStatus.visibility = View.GONE
        frameRateButton.isEnabled = true
        qualityButton.isEnabled = true
        audioEnableButton.isEnabled = true
    }

    private fun pauseRecording() {
        recording?.pause()
        pauseButton.text = TEXT_RESUME
    }

    private fun resumeRecording() {
        recording?.resume()
        pauseButton.text = TEXT_PAUSE
    }

    private fun updateRecordStatus(videoRecordEvent: VideoRecordEvent) {
        val timeSec =
            TimeUnit.NANOSECONDS.toSeconds(
                videoRecordEvent.recordingStats.getRecordedDurationNanos()
            )
        recordStatus.text =
            if (slowMotionVideoEnabled) {
                val speedMultiplier = frameRateButton.getSelectedItem()!!.toSpeed().toFloat()
                "${(timeSec * speedMultiplier).toInt()} -> $timeSec"
            } else {
                "$timeSec"
            }
    }

    private fun handleFinalizeEvent(finalize: VideoRecordEvent.Finalize) {
        when (finalize.error) {
            VideoRecordEvent.Finalize.ERROR_NONE,
            VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED,
            VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED,
            VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE,
            VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> {
                val uri = finalize.outputResults.getOutputUri()
                val outputOptions = finalize.outputOptions
                var msg =
                    when (outputOptions) {
                        is MediaStoreOutputOptions -> {
                            val videoFilePath = getAbsolutePathFromUri(contentResolver, uri)
                            "Saved uri $uri and path $videoFilePath"
                        }

                        is FileOutputOptions -> {
                            val videoFilePath = outputOptions.file.path
                            MediaScannerConnection.scanFile(
                                this,
                                arrayOf<String>(videoFilePath),
                                null,
                            ) { path, uri ->
                                Log.d(TAG, "Scanned $path -> uri= $uri")
                            }
                            "Saved path $videoFilePath"
                        }

                        else -> {
                            throw AssertionError(
                                "Unknown or unsupported OutputOptions type: " +
                                    outputOptions.javaClass.getSimpleName()
                            )
                        }
                    }

                if (finalize.error != VideoRecordEvent.Finalize.ERROR_NONE) {
                    msg += " with code (" + finalize.error + ")"
                }
                logDebugAndToast(msg)
            }
            else ->
                logWarnAndToast(
                    "Video capture failed by (" + finalize.error + "): " + finalize.cause,
                    finalize.cause,
                )
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                prepareCameraProvider()
            } else {
                toast(R.string.permission_warning)
                finish()
            }
        }
    }

    private data class CameraConfig(val header: String, val cameraXConfig: CameraXConfig)

    companion object {
        private const val TAG = "HighSpeedVideoActivity"

        private const val REQUEST_CODE_PERMISSIONS = 1001

        private const val TEXT_START = "Start"
        private const val TEXT_STOP = "Stop"
        private const val TEXT_RESUME = "Resume"
        private const val TEXT_PAUSE = "Pause"

        private val REQUIRED_PERMISSIONS: Array<String> =
            mutableListOf<String>()
                .apply {
                    add(Manifest.permission.CAMERA)
                    add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
                .toTypedArray()

        private val CAMERA_CONFIG_CAMERA2 =
            CameraConfig(
                header = "Camera2 High Speed Video",
                cameraXConfig = Camera2Config.defaultConfig(),
            )
        private val CAMERA_CONFIG_CAMERA_PIPE =
            CameraConfig(
                header = "Camera Pipe High Speed Video",
                cameraXConfig = CameraPipeConfig.defaultConfig(),
            )
        private val DEFAULT_CAMERA_CONFIG = CAMERA_CONFIG_CAMERA2

        private var currentCameraConfig: CameraConfig = DEFAULT_CAMERA_CONFIG
        private var pendingCameraConfig: CameraConfig? = null

        private val ID_TO_CAMERA_CONFIG_MAP: Map<Int, CameraConfig> =
            mapOf(
                R.id.camera_config_camera2 to CAMERA_CONFIG_CAMERA2,
                R.id.camera_config_camera_pipe to CAMERA_CONFIG_CAMERA_PIPE,
            )

        private val ID_TO_ASPECT_RATIO_MAP: Map<Int, Int> =
            mapOf(
                R.id.aspect_ratio_default to RATIO_DEFAULT,
                R.id.aspect_ratio_4_3 to RATIO_4_3,
                R.id.aspect_ratio_16_9 to RATIO_16_9,
            )

        private val ID_TO_VIDEO_CAPABILITIES_SOURCE_MAP: Map<Int, Int> =
            mapOf(
                R.id.video_capabilities_source_camcorder_profile to
                    VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                R.id.video_capabilities_source_codec_capabilities to
                    VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
            )

        private const val HIGH_SPEED_VIDEO_FILE_PREFIX = "high_speed_video"
        private const val SLOW_MOTION_VIDEO_FILE_PREFIX = "slow_motion_video"
        private const val SLOW_MOTION_VIDEO_ENCODING_FRAME_RATE = 30

        private fun <T, E> Map<T, E>.getKey(value: E?): T? = entries.find { it.value == value }?.key

        private fun Range<Int>.toSpeed() = Rational(SLOW_MOTION_VIDEO_ENCODING_FRAME_RATE, upper)

        private fun Range<Int>?.toSlowMotionIconName() = if (this != null) "${toSpeed()}x" else ""

        private fun Range<Int>?.toSlowMotionMenuItemName() =
            if (this != null) "$upper FPS ${toSpeed()}x" else ""

        private fun Context.logDebugAndToast(message: String) {
            logAndToast(level = DEBUG, tag = TAG, message = message, toastLength = LENGTH_SHORT)
        }

        private fun Context.logWarnAndToast(message: String, throwable: Throwable? = null) {
            logAndToast(
                level = WARN,
                tag = TAG,
                message = message,
                throwable = throwable,
                toastLength = LENGTH_LONG,
            )
        }

        private fun Context.logAndToast(
            level: Int,
            tag: String,
            message: String,
            throwable: Throwable? = null,
            toastLength: Int,
        ) {
            when (level) {
                INFO -> Log.i(tag, message, throwable)
                VERBOSE -> Log.v(tag, message, throwable)
                DEBUG -> Log.d(tag, message, throwable)
                WARN -> Log.w(tag, message, throwable)
                ERROR -> Log.e(tag, message, throwable)
            }
            toast(message, toastLength)
        }

        private fun Context.toast(message: String, toastLength: Int = LENGTH_SHORT) {
            Toast.makeText(this, message, toastLength).show()
        }

        private fun Context.toast(@StringRes resId: Int, toastLength: Int = LENGTH_SHORT) {
            Toast.makeText(this, resId, toastLength).show()
        }
    }
}
