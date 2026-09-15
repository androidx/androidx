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

package androidx.camera.integration.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoOutput
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutionException
import kotlin.math.max

/**
 * Activity for manually testing custom [VideoOutput] with [VideoCapture] and [ResolutionSelector].
 */
class CustomVideoOutputActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var btnRecord: Button
    private lateinit var spinnerResolution: Spinner

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var customVideoOutput: CustomVideoOutput

    private var currentPresetIndex = 2 // Default to "16:9 - 1920x1080 (FHD)"
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[Manifest.permission.CAMERA] == true) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_video_output)

        val rootLayout = findViewById<View>(R.id.root_layout)
        val topPanel = findViewById<View>(R.id.top_panel)
        val controlPanel = findViewById<View>(R.id.control_panel)
        val density = resources.displayMetrics.density
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, windowInsets ->
            val insets =
                windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
            topPanel.setPadding(
                topPanel.paddingLeft,
                insets.top + (12 * density).toInt(),
                topPanel.paddingRight,
                topPanel.paddingBottom,
            )
            controlPanel.setPadding(
                controlPanel.paddingLeft,
                controlPanel.paddingTop,
                controlPanel.paddingRight,
                insets.bottom + (16 * density).toInt(),
            )
            WindowInsetsCompat.CONSUMED
        }

        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.status_text)
        btnRecord = findViewById(R.id.btn_record)
        spinnerResolution = findViewById(R.id.spinner_resolution)
        val btnSwitchCamera = findViewById<Button>(R.id.btn_switch_camera)

        customVideoOutput =
            CustomVideoOutput(
                this,
                onStateChangedCallback = { updateStatusUi() },
                onVideoSavedCallback = { savedName ->
                    runOnUiThread {
                        Toast.makeText(
                                this@CustomVideoOutputActivity,
                                "Saved to Google Photos: $savedName",
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    }
                },
            )

        setupResolutionSpinner()

        btnRecord.setOnClickListener {
            if (customVideoOutput.isRecording) {
                customVideoOutput.stopRecording()
                btnRecord.text = "Start Rec"
            } else {
                val fileName = "custom_video_${System.currentTimeMillis()}.mp4"
                customVideoOutput.startRecording(fileName)
                btnRecord.text = "Stop Rec"
            }
            updateStatusUi()
        }

        btnSwitchCamera.setOnClickListener {
            if (customVideoOutput.isRecording) {
                customVideoOutput.stopRecording()
                btnRecord.text = "Start Rec"
            }
            lensFacing =
                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            bindUseCases()
        }

        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    private fun setupResolutionSpinner() {
        val labels = RESOLUTION_PRESETS.map { it.label }
        val adapter =
            object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    (view as? TextView)?.apply {
                        setTextColor(Color.WHITE)
                        textSize = 14f
                    }
                    return view
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup,
                ): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    (view as? TextView)?.apply {
                        setTextColor(Color.WHITE)
                        setBackgroundColor(Color.parseColor("#DD222222"))
                        val pad = (12 * resources.displayMetrics.density).toInt()
                        setPadding(pad, pad, pad, pad)
                    }
                    return view
                }
            }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerResolution.adapter = adapter
        spinnerResolution.setSelection(currentPresetIndex, false)

        spinnerResolution.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (position == currentPresetIndex && cameraProvider != null) {
                        return
                    }
                    if (customVideoOutput.isRecording) {
                        customVideoOutput.stopRecording()
                        btnRecord.text = "Start Rec"
                    }
                    currentPresetIndex = position
                    bindUseCases()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                try {
                    cameraProvider = cameraProviderFuture.get()
                    bindUseCases()
                } catch (e: ExecutionException) {
                    Log.e(TAG, "Error starting camera: ", e)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Error starting camera: ", e)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    @SuppressLint("RestrictedApiAndroidX")
    private fun bindUseCases() {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val preset = RESOLUTION_PRESETS[currentPresetIndex]

        val previewBuilder = Preview.Builder()
        preset.previewResolutionSelector?.let { previewBuilder.setResolutionSelector(it) }
        val preview =
            previewBuilder.build().also { it.surfaceProvider = previewView.surfaceProvider }

        val videoCaptureBuilder = VideoCapture.Builder(customVideoOutput)
        preset.videoResolutionSelector?.let { videoCaptureBuilder.setResolutionSelector(it) }
        val videoCapture = videoCaptureBuilder.build()

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            provider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            updateStatusUi()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind use cases", e)
            statusText.text = "Bind failed: ${e.message}"
        }
    }

    private fun updateStatusUi() {
        runOnUiThread {
            val preset = RESOLUTION_PRESETS[currentPresetIndex]
            val requestedRes = customVideoOutput.surfaceResolution
            val resStr =
                if (requestedRes != null) {
                    val gcd = gcd(requestedRes.width, requestedRes.height)
                    val ratioW = requestedRes.width / gcd
                    val ratioH = requestedRes.height / gcd
                    "${requestedRes.width}x${requestedRes.height} ($ratioW:$ratioH)"
                } else {
                    "Pending..."
                }
            val recState =
                if (customVideoOutput.isRecording) {
                    "RECORDING (${customVideoOutput.recordedFrames} frames)"
                } else if (customVideoOutput.lastSavedDisplayName.isEmpty()) {
                    "Ready"
                } else {
                    "Saved: Pictures/Camera/${customVideoOutput.lastSavedDisplayName}"
                }

            statusText.text =
                "Preset: ${preset.label}\n" +
                    "SurfaceRequest: $resStr | Rot: ${customVideoOutput.rotationDegrees}°\n" +
                    "State: $recState"
        }
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    override fun onDestroy() {
        super.onDestroy()
        if (::customVideoOutput.isInitialized) {
            customVideoOutput.release()
        }
    }

    private data class ResolutionPreset(
        val label: String,
        val videoResolutionSelector: ResolutionSelector?,
        val previewResolutionSelector: ResolutionSelector?,
    )

    /**
     * Custom [VideoOutput] implementation using [MediaCodec] and [MediaMuxer].
     *
     * Note: Intentionally only implements the public [onSurfaceRequested] method and relies on
     * default implementations for all other [VideoOutput] methods.
     */
    class CustomVideoOutput(
        context: Context,
        private val onStateChangedCallback: () -> Unit,
        private val onVideoSavedCallback: (String) -> Unit,
    ) : VideoOutput {

        private val appContext: Context = context.applicationContext
        private val encoderThread = HandlerThread("CustomEncoderThread").apply { start() }
        private val encoderHandler = Handler(encoderThread.looper)

        private var activeSession: EncoderSession? = null
        private var mediaMuxer: MediaMuxer? = null
        private var currentPfd: ParcelFileDescriptor? = null
        private var currentMediaStoreUri: Uri? = null
        private var currentLegacyFile: File? = null
        private var currentDisplayName: String = ""
        private var videoTrackIndex: Int = -1
        private var muxerStarted: Boolean = false
        private var waitingForKeyFrame: Boolean = false
        private var firstFrameTimestampUs: Long = -1L
        private var lastPresentationTimeUs: Long = -1L

        @Volatile
        var isRecording: Boolean = false
            private set

        @Volatile
        var recordedFrames: Int = 0
            private set

        @Volatile
        var lastSavedDisplayName: String = ""
            private set

        @Volatile
        var surfaceResolution: Size? = null
            private set

        @Volatile
        var rotationDegrees: Int = 0
            private set

        override fun onSurfaceRequested(request: SurfaceRequest) {
            val resolution = request.resolution
            surfaceResolution = resolution

            request.setTransformationInfoListener(ContextCompat.getMainExecutor(appContext)) { info
                ->
                rotationDegrees = info.rotationDegrees
                onStateChangedCallback()
            }

            encoderHandler.post { setupEncoder(request, resolution) }
            onStateChangedCallback()
        }

        private fun setupEncoder(request: SurfaceRequest, resolution: Size) {
            try {
                val session = EncoderSession(resolution)
                activeSession = session

                request.provideSurface(session.surface, encoderHandler::post) { result ->
                    Log.d(
                        TAG,
                        "SurfaceRequest result: ${result.resultCode} for resolution $resolution",
                    )
                    session.release()
                    if (activeSession === session) {
                        activeSession = null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup MediaCodec encoder", e)
                request.willNotProvideSurface()
            }
        }

        /**
         * Encapsulates a single MediaCodec instance and its input Surface for a specific
         * SurfaceRequest so that releasing an older SurfaceRequest's surface when switching cameras
         * or resolutions never destroys a newly bound SurfaceRequest's active encoder.
         */
        private inner class EncoderSession(resolution: Size) {
            val codec: MediaCodec
            val surface: Surface
            var outputFormat: MediaFormat? = null
            var released = false

            init {
                val format =
                    MediaFormat.createVideoFormat(MIME_TYPE, resolution.width, resolution.height)
                        .apply {
                            setInteger(
                                MediaFormat.KEY_COLOR_FORMAT,
                                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
                            )
                            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                            setInteger(MediaFormat.KEY_FRAME_RATE, DEFAULT_FRAME_RATE)
                            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                        }

                codec = MediaCodec.createEncoderByType(MIME_TYPE)
                codec.setCallback(
                    object : MediaCodec.Callback() {
                        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                            // Input comes from Surface
                        }

                        override fun onOutputBufferAvailable(
                            codec: MediaCodec,
                            index: Int,
                            info: MediaCodec.BufferInfo,
                        ) {
                            if (released) return
                            if (activeSession === this@EncoderSession) {
                                handleEncodedOutput(codec, index, info)
                            } else {
                                // Drain output buffer from retiring session so BufferQueue doesn't
                                // stall while Camera2 finishes detaching the old capture session.
                                try {
                                    codec.releaseOutputBuffer(index, false)
                                } catch (ignored: Exception) {}
                            }
                        }

                        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                            Log.e(TAG, "MediaCodec error", e)
                        }

                        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                            outputFormat = format
                            Log.d(TAG, "Encoder output format changed: $format")
                            if (activeSession === this@EncoderSession) {
                                startMuxerIfReady()
                            }
                        }
                    },
                    encoderHandler,
                )

                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                surface = codec.createInputSurface()
                codec.start()
            }

            fun requestSyncFrame() {
                if (!released) {
                    try {
                        val params =
                            Bundle().apply {
                                putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                            }
                        codec.setParameters(params)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to request sync frame", e)
                    }
                }
            }

            fun release() {
                if (released) return
                released = true
                try {
                    codec.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping MediaCodec", e)
                }
                try {
                    codec.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing MediaCodec", e)
                }
                try {
                    surface.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing Surface", e)
                }
            }
        }

        private fun handleEncodedOutput(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo,
        ) {
            try {
                if (
                    isRecording &&
                        muxerStarted &&
                        (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                        info.size > 0
                ) {
                    if (waitingForKeyFrame) {
                        if ((info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                            waitingForKeyFrame = false
                            firstFrameTimestampUs = info.presentationTimeUs
                            lastPresentationTimeUs = -1L
                        }
                    }
                    if (!waitingForKeyFrame) {
                        val encodedData = codec.getOutputBuffer(index)
                        val muxer = mediaMuxer
                        if (encodedData != null && muxer != null) {
                            encodedData.position(info.offset)
                            encodedData.limit(info.offset + info.size)

                            val muxerInfo = MediaCodec.BufferInfo()
                            var normalizedPtsUs =
                                max(0L, info.presentationTimeUs - firstFrameTimestampUs)
                            if (normalizedPtsUs <= lastPresentationTimeUs) {
                                normalizedPtsUs = lastPresentationTimeUs + 1000L
                            }
                            lastPresentationTimeUs = normalizedPtsUs
                            muxerInfo.set(info.offset, info.size, normalizedPtsUs, info.flags)

                            muxer.writeSampleData(videoTrackIndex, encodedData, muxerInfo)
                            recordedFrames++
                            if (recordedFrames % 15 == 0) {
                                onStateChangedCallback()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to MediaMuxer", e)
            } finally {
                try {
                    codec.releaseOutputBuffer(index, false)
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing output buffer", e)
                }
            }
        }

        private fun startMuxerIfReady() {
            val muxer = mediaMuxer ?: return
            val session = activeSession ?: return
            val format = session.outputFormat ?: return
            if (isRecording && !muxerStarted) {
                videoTrackIndex = muxer.addTrack(format)
                muxer.setOrientationHint(rotationDegrees)
                muxer.start()
                muxerStarted = true
                waitingForKeyFrame = true
                firstFrameTimestampUs = -1L
                lastPresentationTimeUs = -1L
                session.requestSyncFrame()
            }
        }

        fun startRecording(fileName: String) {
            encoderHandler.post {
                if (isRecording) return@post
                try {
                    currentDisplayName = fileName
                    val resolver = appContext.contentResolver
                    val values = createValues(fileName)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val uri =
                            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                                ?: throw IOException("Failed to create MediaStore entry")
                        currentMediaStoreUri = uri
                        val pfd =
                            resolver.openFileDescriptor(uri, "rw")
                                ?: throw IOException("Failed to open ParcelFileDescriptor")
                        currentPfd = pfd
                        mediaMuxer =
                            MediaMuxer(
                                pfd.fileDescriptor,
                                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                            )
                    } else {
                        val dir =
                            Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_MOVIES
                            )
                        if (!dir.exists() && !dir.mkdirs()) {
                            Log.w(TAG, "Fail to create folder: $dir")
                        }
                        val file = File(dir, fileName)
                        currentLegacyFile = file
                        mediaMuxer =
                            MediaMuxer(
                                file.absolutePath,
                                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                            )
                    }

                    muxerStarted = false
                    videoTrackIndex = -1
                    recordedFrames = 0
                    isRecording = true
                    startMuxerIfReady()
                    activeSession?.requestSyncFrame()
                    onStateChangedCallback()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to create MediaMuxer", e)
                }
            }
        }

        fun stopRecording() {
            encoderHandler.post {
                stopRecordingInternal()
                onStateChangedCallback()
            }
        }

        private fun stopRecordingInternal() {
            val wasRecording = isRecording
            isRecording = false
            mediaMuxer?.let { muxer ->
                try {
                    if (muxerStarted) {
                        muxer.stop()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping MediaMuxer", e)
                } finally {
                    try {
                        muxer.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error releasing MediaMuxer", e)
                    }
                    mediaMuxer = null
                    muxerStarted = false
                }
            }
            currentPfd?.let { pfd ->
                try {
                    pfd.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing ParcelFileDescriptor", e)
                }
                currentPfd = null
            }
            if (wasRecording) {
                lastSavedDisplayName = currentDisplayName
                currentMediaStoreUri?.let { uri ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values =
                            ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                        appContext.contentResolver.update(uri, values, null, null)
                    }
                    currentMediaStoreUri = null
                }
                    ?: currentLegacyFile?.let { file ->
                        MediaScannerConnection.scanFile(
                            appContext,
                            arrayOf(file.absolutePath),
                            arrayOf("video/mp4"),
                            null,
                        )
                        currentLegacyFile = null
                    }
                onVideoSavedCallback(lastSavedDisplayName)
            }
        }

        fun release() {
            encoderHandler.post {
                stopRecordingInternal()
                activeSession?.release()
                activeSession = null
                encoderThread.quitSafely()
            }
        }

        companion object {
            private fun createValues(fileName: String): ContentValues {
                return ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.Video.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_MOVIES,
                        )
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CustomVideoOutput"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val BIT_RATE = 8_000_000
        private const val DEFAULT_FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 1

        private fun buildPreset(
            label: String,
            aspectRatioStrategy: AspectRatioStrategy,
            boundSize: Size? = null,
        ): ResolutionPreset {
            val videoBuilder =
                ResolutionSelector.Builder().setAspectRatioStrategy(aspectRatioStrategy)
            if (boundSize != null) {
                videoBuilder.setResolutionStrategy(
                    ResolutionStrategy(
                        boundSize,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    )
                )
            } else {
                videoBuilder.setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            }
            val previewSelector =
                ResolutionSelector.Builder().setAspectRatioStrategy(aspectRatioStrategy).build()
            return ResolutionPreset(
                label = label,
                videoResolutionSelector = videoBuilder.build(),
                previewResolutionSelector = previewSelector,
            )
        }

        private val RESOLUTION_PRESETS =
            listOf(
                ResolutionPreset(
                    label = "Default (No ResolutionSelector)",
                    videoResolutionSelector = null,
                    previewResolutionSelector = null,
                ),
                buildPreset(
                    label = "16:9 - 3840x2160 (4K UHD)",
                    aspectRatioStrategy = AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
                    boundSize = Size(3840, 2160),
                ),
                buildPreset(
                    label = "16:9 - 1920x1080 (FHD)",
                    aspectRatioStrategy = AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
                    boundSize = Size(1920, 1080),
                ),
                buildPreset(
                    label = "16:9 - 1280x720 (HD)",
                    aspectRatioStrategy = AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
                    boundSize = Size(1280, 720),
                ),
                buildPreset(
                    label = "16:9 - 640x360 (nHD)",
                    aspectRatioStrategy = AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
                    boundSize = Size(640, 360),
                ),
                buildPreset(
                    label = "16:9 - Highest Available",
                    aspectRatioStrategy = AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY,
                    boundSize = null,
                ),
                buildPreset(
                    label = "4:3 - 1920x1440",
                    aspectRatioStrategy = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY,
                    boundSize = Size(1920, 1440),
                ),
                buildPreset(
                    label = "4:3 - 1440x1080",
                    aspectRatioStrategy = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY,
                    boundSize = Size(1440, 1080),
                ),
                buildPreset(
                    label = "4:3 - 1280x960",
                    aspectRatioStrategy = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY,
                    boundSize = Size(1280, 960),
                ),
                buildPreset(
                    label = "4:3 - 640x480 (VGA)",
                    aspectRatioStrategy = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY,
                    boundSize = Size(640, 480),
                ),
                buildPreset(
                    label = "4:3 - Highest Available",
                    aspectRatioStrategy = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY,
                    boundSize = null,
                ),
            )
    }
}
