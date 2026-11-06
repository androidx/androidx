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

package androidx.xr.projected.testapp.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Controller managing audio hardware, recording, playback, and device discovery. */
@OptIn(ExperimentalProjectedApi::class)
class AudioController(
    private val context: Context,
    private val viewModel: AudioViewModel,
    private val scope: CoroutineScope,
) : AutoCloseable {

    private var deviceController: ProjectedDeviceController? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var audioBuffer: ByteArray = ByteArray(0)

    init {
        startConnectionMonitoring()
    }

    private fun startConnectionMonitoring() {
        scope.launch {
            try {
                ProjectedContext.isProjectedDeviceConnected(context, Dispatchers.Default)
                    .collectLatest { connected ->
                        viewModel.setConnected(connected)
                        if (connected) {
                            viewModel.setStatusMessage("Projected device connected.")
                            initializeDeviceController()
                        } else {
                            viewModel.setStatusMessage("Projected device is not connected.")
                            cleanupDeviceController()
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring connection", e)
                viewModel.setStatusMessage("Connection error: ${e.message}")
            }
        }
    }

    private fun initializeDeviceController() {
        scope.launch {
            try {
                cleanupDeviceController()
                val controller = ProjectedDeviceController.create(context)
                deviceController = controller
                val inputs = mutableListOf<String>()
                val outputs = mutableListOf<String>()
                controller.audioDevices.forEach { device ->
                    if (device.isSource) {
                        inputs.add("${device.productName} (ID: ${device.id})")
                    }
                    if (device.isSink) {
                        outputs.add("${device.productName} (ID: ${device.id})")
                    }
                }
                viewModel.setAudioDevices(inputs, outputs)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ProjectedDeviceController", e)
                viewModel.setStatusMessage("Failed device controller: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.setStatusMessage("Permission RECORD_AUDIO not granted")
            return
        }

        val targetContext =
            try {
                ProjectedContext.createProjectedDeviceContext(context)
            } catch (_: Exception) {
                context
            }

        val bufferSize =
            AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                .coerceAtLeast(1024)
        audioBuffer = ByteArray(bufferSize)

        // Projected devices currently only deliver microphone audio to apps that record with
        // VOICE_COMMUNICATION at 16 kHz mono. Other sources can initialize successfully but only
        // record silence.
        val sources = intArrayOf(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        val contexts = listOf(targetContext, context).distinct()

        var record: AudioRecord? = null
        for (ctx in contexts) {
            for (source in sources) {
                try {
                    val candidate =
                        AudioRecord.Builder()
                            .setAudioSource(source)
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                                    .setSampleRate(SAMPLE_RATE)
                                    .build()
                            )
                            .setBufferSizeInBytes(bufferSize)
                            .setContext(ctx)
                            .build()

                    if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                        record = candidate
                        Log.i(TAG, "Initialized AudioRecord with source $source on context $ctx")
                        break
                    } else {
                        candidate.release()
                    }
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Could not create AudioRecord (source=$source, ctx=$ctx): ${e.message}",
                    )
                }
            }
            if (record != null) break
        }

        if (record == null) {
            viewModel.setStatusMessage("Recording failed: Cannot create AudioRecord on device")
            viewModel.setRecording(false)
            return
        }

        audioRecord = record
        try {
            record.startRecording()
            viewModel.setRecording(true)
            viewModel.setStatusMessage("Recording audio...")

            scope.launch(Dispatchers.IO) {
                val file = File(context.filesDir, AUDIO_FILE_NAME)
                val fos = FileOutputStream(file)
                var isSilent = true
                try {
                    while (viewModel.isRecording.value) {
                        val read = record.read(audioBuffer, 0, audioBuffer.size)
                        if (read > 0) {
                            fos.write(audioBuffer, 0, read)
                            // A 16-bit PCM sample is zero only if both of its bytes are zero.
                            isSilent =
                                isSilent && (0 until read).all { audioBuffer[it] == 0.toByte() }
                        }
                    }
                } finally {
                    try {
                        fos.flush()
                        fos.close()
                    } catch (_: Exception) {}
                }
                val size = file.length()
                val status =
                    when {
                        size == 0L -> "No audio data recorded (0 bytes)."
                        isSilent -> "Recording saved ($size bytes), but it contains only silence."
                        else -> "Recording saved ($size bytes)."
                    }
                Log.i(TAG, status)
                withContext(Dispatchers.Main) { viewModel.setStatusMessage(status) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording audio", e)
            viewModel.setStatusMessage("Recording failed: ${e.message}")
            viewModel.setRecording(false)
        }
    }

    fun stopRecording() {
        viewModel.setRecording(false)
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun startPlayback() {
        val audioFile = File(context.filesDir, AUDIO_FILE_NAME)
        if (!audioFile.exists() || audioFile.length() == 0L) {
            viewModel.setStatusMessage("No recorded audio file found to play.")
            return
        }

        val targetContext =
            try {
                ProjectedContext.createProjectedDeviceContext(context)
            } catch (_: Exception) {
                context
            }

        viewModel.setPlaying(true)
        viewModel.setStatusMessage("Playing audio...")

        val audioData = audioFile.readBytes()
        val bufferSize =
            AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                .coerceAtLeast(audioData.size)

        val contexts = listOf(targetContext, context).distinct()
        var track: AudioTrack? = null
        for (ctx in contexts) {
            try {
                val candidate =
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setContext(ctx)
                        .build()

                if (candidate.state == AudioTrack.STATE_INITIALIZED) {
                    track = candidate
                    Log.i(TAG, "Initialized AudioTrack on context $ctx")
                    break
                } else {
                    candidate.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not create AudioTrack on ctx $ctx: ${e.message}")
            }
        }

        if (track == null) {
            viewModel.setStatusMessage("Playback failed: Cannot create AudioTrack")
            viewModel.setPlaying(false)
            return
        }

        audioTrack = track
        track.notificationMarkerPosition = audioData.size / 2
        track.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    scope.launch(Dispatchers.Main) {
                        stopPlayback()
                        viewModel.setStatusMessage("Playback finished.")
                    }
                }

                override fun onPeriodicNotification(t: AudioTrack?) {}
            }
        )

        scope.launch(Dispatchers.IO) {
            try {
                track.play()
                track.write(audioData, 0, audioData.size)
                val durationMs =
                    (audioData.size.toDouble() / (SAMPLE_RATE * 2) * 1000).toLong() + 500L
                delay(durationMs)
                withContext(Dispatchers.Main) {
                    if (viewModel.isPlaying.value) {
                        stopPlayback()
                        viewModel.setStatusMessage("Playback finished.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing audio", e)
                withContext(Dispatchers.Main) {
                    stopPlayback()
                    viewModel.setStatusMessage("Playback error: ${e.message}")
                }
            }
        }
    }

    fun stopPlayback() {
        viewModel.setPlaying(false)
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun cleanupDeviceController() {
        try {
            deviceController?.close()
        } catch (_: Exception) {}
        deviceController = null
    }

    override fun close() {
        stopRecording()
        stopPlayback()
        cleanupDeviceController()
    }

    companion object {
        const val SAMPLE_RATE = 16000
        const val AUDIO_FILE_NAME = "audioRecording.wav"
        private const val TAG = "AudioController"
    }
}
