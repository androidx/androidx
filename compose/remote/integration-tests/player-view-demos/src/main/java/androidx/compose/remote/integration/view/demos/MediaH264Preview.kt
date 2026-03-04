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

package androidx.compose.remote.integration.view.demos

import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.PixelFormat
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.Text
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.capture.RememberRemoteDocumentInline
import androidx.compose.remote.creation.compose.capture.rememberVirtualDisplay
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.io.ByteArrayInputStream
import java.io.File
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

data class DumperOutputData(val filePath: String)

/** A manually driven clock to ensure perfectly timed frame-by-frame recording. */
@Suppress("RestrictedApiAndroidX")
private class ManualRemoteClock(val baseTimeMillis: Long = 10 * 3600000L + 10 * 60000L) :
    RemoteClock {
    var offsetMillis: Long = 0

    override fun millis() = baseTimeMillis + offsetMillis

    override fun nanoTime() = (baseTimeMillis + offsetMillis) * 1_000_000L

    override fun getZoneId() = "UTC"

    override fun snapshot(millis: Long?): RemoteClock.TimeSnapshot {
        val m = millis ?: (baseTimeMillis + offsetMillis)
        return ManualTimeSnapshot(m)
    }

    @Suppress("RestrictedApiAndroidX")
    private class ManualTimeSnapshot(val m: Long) : RemoteClock.TimeSnapshot {
        override fun getMillis() = m

        override fun getYear() = 2026

        override fun getMonth() = 2

        override fun getDayOfMonth() = 13

        override fun getDayOfYear() = 44

        override fun getHour() = (m / 3600000).toInt() % 24

        override fun getMinute() = (m / 60000).toInt() % 60

        override fun getSecond() = (m / 1000).toInt() % 60

        override fun getMillisOfSecond() = (m % 1000).toInt()

        override fun getDayOfWeek() = 5

        override fun getOffsetSeconds() = 0
    }
}

/** Presentation used to render the RemoteDocument to a VirtualDisplay during recording. */
@Suppress("RestrictedApiAndroidX")
private class RecordingPresentation(
    context: Context,
    virtualDisplay: VirtualDisplay,
    private val document: CoreDocument,
    private val width: Int,
    private val height: Int,
    private val compositionContext: CompositionContext,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
) : Presentation(context, virtualDisplay.display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cv =
            ComposeView(context).apply {
                layoutParams = ViewGroup.LayoutParams(width, height)
                setParentCompositionContext(compositionContext)
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                setContent {
                    MaterialTheme {
                        MaterialSurface {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = width,
                                documentHeight = height,
                                modifier = Modifier.fillMaxSize(),
                                init = { player -> player.setShaderControl { true } },
                            )
                        }
                    }
                }
            }
        setContentView(cv)
    }
}

/** Background thread that coordinates the off-screen rendering and H264 encoding. */
@Suppress("RestrictedApiAndroidX")
private class VideoEncodeThread(
    private val context: Context,
    private val sampleName: String,
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val bitrate: Int,
    private val durationMillis: Long,
    private val videoDocument: RemoteDocument,
    private val virtualDisplay: VirtualDisplay,
    private val compositionContext: CompositionContext,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val onStatusUpdate: (String) -> Unit,
    private val onFinished: (DumperOutputData) -> Unit,
) : Thread() {
    private val threadRunning = AtomicBoolean(true)

    fun stopRecording() {
        threadRunning.set(false)
    }

    @Suppress("BanThreadSleep")
    override fun run() {
        var mediaCodec: MediaCodec? = null
        var mediaMuxer: MediaMuxer? = null
        var presentation: Presentation? = null
        var imageReader: ImageReader? = null
        var lastBitmap: Bitmap? = null

        try {
            val videoFormat =
                MediaFormat.createVideoFormat("video/avc", width, height).apply {
                    setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
                    )
                    setInteger(
                        MediaFormat.KEY_BITRATE_MODE,
                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR,
                    )
                    setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    setFloat(MediaFormat.KEY_MAX_FPS_TO_ENCODER, fps.toFloat())
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
            mediaCodec = MediaCodec.createEncoderByType("video/avc")
            mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val codecSurface = mediaCodec.createInputSurface()
            mediaCodec.start()

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)

            val durationSeconds = durationMillis / 1000
            val bitrateKbps = bitrate / 1000
            val outputFile =
                File(
                    context.cacheDir,
                    "${sampleName}_${width}x${height}_${durationSeconds}s_${fps}fps_${bitrateKbps}kbps.mp4",
                )
            mediaMuxer =
                MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            onStatusUpdate("Recording...")

            // Setup Presentation on the Main Thread
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                virtualDisplay.surface = imageReader!!.surface
                presentation =
                    RecordingPresentation(
                        context,
                        virtualDisplay,
                        videoDocument.document,
                        width,
                        height,
                        compositionContext,
                        lifecycleOwner,
                        viewModelStoreOwner,
                        savedStateRegistryOwner,
                    )
                presentation?.show()
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var videoTrackIndex = -1
            val frameIntervalUs = 1_000_000L / fps
            var frameIndex = 0
            val totalFrames = (durationMillis * 1000L / frameIntervalUs).toInt()
            val timestampQueue = ArrayDeque<Long>()
            val manualClock = videoDocument.clock as ManualRemoteClock
            val encodingStartTime = System.currentTimeMillis()

            while (threadRunning.get() && frameIndex < totalFrames) {
                val currentTimestampUs = frameIndex * 1_000_000L / fps
                val elapsedTimeMs = currentTimestampUs / 1000L
                manualClock.offsetMillis = elapsedTimeMs

                val targetWallTime = encodingStartTime + elapsedTimeMs
                val delay = targetWallTime - System.currentTimeMillis()
                if (delay > 0) sleep(delay)

                timestampQueue.add(currentTimestampUs)

                imageReader!!.acquireLatestImage()?.let { image ->
                    image.hardwareBuffer?.let { hwBuffer ->
                        lastBitmap =
                            Bitmap.wrapHardwareBuffer(
                                hwBuffer,
                                ColorSpace.get(ColorSpace.Named.SRGB),
                            )
                        hwBuffer.close()
                    }
                    image.close()
                }

                lastBitmap?.let { bitmap ->
                    val canvas = codecSurface.lockHardwareCanvas()
                    try {
                        canvas.drawBitmap(bitmap, 0f, 0f, null)
                    } finally {
                        codecSurface.unlockCanvasAndPost(canvas)
                    }
                }

                videoTrackIndex =
                    drainEncoder(
                        mediaCodec,
                        mediaMuxer,
                        bufferInfo,
                        videoTrackIndex,
                        timestampQueue,
                        false,
                    )
                frameIndex++
            }

            mediaCodec.signalEndOfInputStream()
            drainEncoder(mediaCodec, mediaMuxer, bufferInfo, videoTrackIndex, timestampQueue, true)

            if (videoTrackIndex >= 0) {
                mediaMuxer.stop()
                onStatusUpdate("Finished")
                onFinished(DumperOutputData(outputFile.absolutePath))
            } else {
                onStatusUpdate("No frames recorded")
            }
        } catch (e: Exception) {
            onStatusUpdate("Error: ${e.message}")
        } finally {
            cleanup(presentation, virtualDisplay, imageReader, mediaCodec, mediaMuxer)
        }
    }

    private fun drainEncoder(
        mediaCodec: MediaCodec,
        mediaMuxer: MediaMuxer?,
        bufferInfo: MediaCodec.BufferInfo,
        trackIndex: Int,
        timestampQueue: ArrayDeque<Long>,
        isFinalDrain: Boolean,
    ): Int {
        var currentTrackIndex = trackIndex
        val timeoutUs = if (isFinalDrain) 10000L else 0L

        while (true) {
            val index = mediaCodec.dequeueOutputBuffer(bufferInfo, timeoutUs)

            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // In both cases, if we get a timeout, we return to the caller.
                // During final drain, this typically means the EOS flag was already found
                // or the encoder is done.
                break
            }

            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (currentTrackIndex < 0) {
                    currentTrackIndex = mediaMuxer!!.addTrack(mediaCodec.outputFormat)
                    mediaMuxer.start()
                }
                continue
            }

            if (index >= 0) {
                val buffer = mediaCodec.getOutputBuffer(index)
                if (buffer != null && bufferInfo.size != 0 && currentTrackIndex >= 0) {
                    // Only apply manual timestamps to data buffers, not config buffers.
                    if (!isConfigBuffer(bufferInfo)) {
                        if (!timestampQueue.isEmpty()) {
                            bufferInfo.presentationTimeUs = timestampQueue.removeFirst()
                        }
                    }
                    mediaMuxer!!.writeSampleData(currentTrackIndex, buffer, bufferInfo)
                }
                mediaCodec.releaseOutputBuffer(index, false)

                if (isEndOfStream(bufferInfo)) {
                    break
                }
            }
        }
        return currentTrackIndex
    }

    private fun isEndOfStream(info: MediaCodec.BufferInfo): Boolean =
        (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

    private fun isConfigBuffer(info: MediaCodec.BufferInfo): Boolean =
        (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0

    private fun cleanup(
        presentation: Presentation?,
        virtualDisplay: VirtualDisplay,
        imageReader: ImageReader?,
        mediaCodec: MediaCodec?,
        mediaMuxer: MediaMuxer?,
    ) {
        android.os.Handler(android.os.Looper.getMainLooper()).post { presentation?.dismiss() }
        virtualDisplay.surface = null
        imageReader?.close()
        try {
            mediaCodec?.stop()
        } catch (e: Exception) {}
        mediaCodec?.release()
        mediaMuxer?.release()
    }
}

@OptIn(ExperimentalRemoteCreationComposeApi::class)
@Suppress("RestrictedApiAndroidX", "COMPOSE_APPLIER_CALL_MISMATCH")
@androidx.compose.runtime.ComposableTarget(applier = "androidx.compose.ui.UiComposable")
@Composable
fun mediaH264Preview(
    context: Context,
    sample: DumperSample,
    width: Int,
    height: Int,
    durationMillis: Long,
    fps: Int,
    bitrate: Int,
): DumperOutputData? {
    var status by remember { mutableStateOf("Initializing...") }
    var outputData by remember { mutableStateOf<DumperOutputData?>(null) }

    val config = LocalConfiguration.current
    val creationDisplayInfo =
        remember(sample, width, height) { CreationDisplayInfo(width, height, config.densityDpi) }
    val virtualDisplay = rememberVirtualDisplay(creationDisplayInfo)

    var videoDocument by remember(sample) { mutableStateOf<RemoteDocument?>(null) }

    // Capture owners once for the background Presentation
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
    val compositionContext = rememberCompositionContext()

    // Use a cleaner capture mechanism that disposes itself properly
    if (sample is DumperSample.ComposableSample && videoDocument == null) {
        RememberRemoteDocumentInline(
            profile = RcPlatformProfiles.ANDROIDX,
            onDocument = { doc ->
                val wireBuffer = doc.buffer.buffer
                val bytes = wireBuffer.getBuffer().copyOf(wireBuffer.size())
                val vDoc = CoreDocument(ManualRemoteClock())
                vDoc.initFromBuffer(
                    RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes))
                )
                videoDocument = RemoteDocument(vDoc)
            },
            content = { sample.content() },
        )
    } else if (sample is DumperSample.Context && videoDocument == null) {
        LaunchedEffect(sample) {
            val rcContext = sample.getContext()
            val wireBuffer = rcContext.buffer.buffer
            val bytes = wireBuffer.getBuffer().copyOf(wireBuffer.size())
            val vDoc = CoreDocument(ManualRemoteClock())
            vDoc.initFromBuffer(RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes)))
            videoDocument = RemoteDocument(vDoc)
        }
    }

    val isReady = videoDocument != null

    DisposableEffect(videoDocument, isReady) {
        if (!isReady || videoDocument == null) return@DisposableEffect onDispose {}

        val recorder =
            VideoEncodeThread(
                context,
                sample.name,
                width,
                height,
                fps,
                bitrate,
                durationMillis,
                videoDocument!!,
                virtualDisplay,
                compositionContext,
                lifecycleOwner,
                viewModelStoreOwner!!,
                savedStateRegistryOwner,
                onStatusUpdate = { status = it },
                onFinished = { outputData = it },
            )
        recorder.start()

        onDispose { recorder.stopRecording() }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = status, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        val density = LocalDensity.current
        Column(
            modifier =
                Modifier.size(with(density) { width.toDp() }, with(density) { height.toDp() })
        ) {
            videoDocument?.let {
                RemoteDocumentPlayer(
                    document = it.document,
                    documentWidth = width,
                    documentHeight = height,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    return outputData
}
