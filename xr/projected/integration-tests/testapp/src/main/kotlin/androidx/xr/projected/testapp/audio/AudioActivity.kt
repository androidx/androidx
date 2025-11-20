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

package androidx.xr.projected.testapp.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** The AudioActivity records and plays back audio on a projected device. */
@OptIn(ExperimentalProjectedApi::class)
class AudioActivity : ComponentActivity() {
    lateinit var audioRecord: AudioRecord
    lateinit var audioTrack: AudioTrack
    lateinit var audioBuffer: ByteArray
    lateinit var connectedFlow: Flow<Boolean>
    var projectedContext: Context? = null
    var errorMessage: String = ""

    private enum class RecordState {
        AWAITING_RECORDING,
        RECORDING,
        AWAITING_PLAYBACK,
        PLAYBACK,
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "Creating AudioActivity")
        super.onCreate(savedInstanceState)
        connectedFlow = ProjectedContext.isProjectedDeviceConnected(this, Dispatchers.Default)
        setContent { CreateAudioUi() }
    }

    @Composable
    private fun CreateAudioUi() {
        val state = remember { mutableStateOf(RecordState.AWAITING_RECORDING) }
        val connectedState = remember { mutableStateOf(false) }
        val audioInitialized = remember { mutableStateOf(false) }
        UpdateConnectedState(connectedState, audioInitialized)
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            if (!connectedState.value || !audioInitialized.value) {
                Text(errorMessage)
                return
            }
            Text("State: ${state.value.name}")
            Button(
                onClick = {
                    when (state.value) {
                        RecordState.AWAITING_RECORDING -> startRecording(state)
                        RecordState.RECORDING -> stopRecording(state)
                        RecordState.AWAITING_PLAYBACK -> startPlayback(state)
                        else -> Log.e(TAG, "Button should be disabled during playback.")
                    }
                },
                enabled = (state.value != RecordState.PLAYBACK),
            ) {
                Text(getButtonText(state.value))
            }
        }
    }

    @Composable
    fun UpdateConnectedState(
        connectedState: MutableState<Boolean>,
        audioInitialized: MutableState<Boolean>,
    ) {
        LaunchedEffect(Unit) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                connectedFlow.collect { connected ->
                    connectedState.value = connected
                    if (connected) {
                        createProjectedContext()
                        projectedContext?.let {
                            if (
                                ActivityCompat.checkSelfPermission(
                                    it,
                                    Manifest.permission.RECORD_AUDIO,
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                errorMessage = "Record Audio permission is required."
                                audioInitialized.value = false
                                return@collect
                            }
                            audioInitialized.value = initAudio(it)
                        }
                        Log.i(TAG, "Projected device is connected")
                    } else {
                        errorMessage = "Projected device is not connected."
                        audioInitialized.value = false
                        Log.w(TAG, "Projected device is not connected")
                    }
                }
            }
        }
    }

    private fun createProjectedContext() {
        try {
            projectedContext = ProjectedContext.createProjectedDeviceContext(this)
        } catch (e: IllegalStateException) {
            errorMessage = "Failed to create Projected Context."
            Log.w(TAG, "Error creating projected context: $e")
        }
    }

    private fun getButtonText(state: RecordState): String =
        when (state) {
            RecordState.AWAITING_RECORDING -> "Record"
            RecordState.RECORDING -> "Stop Recording"
            RecordState.AWAITING_PLAYBACK -> "Play"
            RecordState.PLAYBACK -> "Playback in Progress"
        }

    // Initialize the AudioRecord and AudioTrack for recording and playing back audio.
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initAudio(context: Context): Boolean {
        Log.i(TAG, "Initializing AudioRecord and AudioTrack")
        val audioRecordFormat =
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setSampleRate(SAMPLE_RATE)
                .build()
        val audioRecordingBufferSize =
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        audioBuffer = ByteArray(audioRecordingBufferSize)
        try {
            audioRecord =
                AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
                    .setAudioFormat(audioRecordFormat)
                    .setBufferSizeInBytes(audioRecordingBufferSize)
                    .setContext(context)
                    .build()
        } catch (e: UnsupportedOperationException) {
            errorMessage = "Failed to create AudioRecord."
            Log.e(TAG, "Error creating AudioRecord: $e")
            return false
        }
        val attributes =
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

        val audioTrackFormat =
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setSampleRate(SAMPLE_RATE)
                .build()

        val audioTrackBufferSize =
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        try {
            audioTrack =
                AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(audioTrackFormat)
                    .setBufferSizeInBytes(audioTrackBufferSize)
                    .setContext(context)
                    .build()
        } catch (e: UnsupportedOperationException) {
            errorMessage = "Failed to create AudioTrack."
            Log.e(TAG, "Error creating AudioTrack: $e")
            return false
        }
        Log.i(TAG, "Successfully initialized AudioRecord and AudioTrack")
        return true
    }

    private fun startRecording(state: MutableState<RecordState>) {
        Log.i(TAG, "Starting Recording")
        state.value = RecordState.RECORDING
        audioRecord.startRecording()
        val fileStream = this.openFileOutput(AUDIO_FILE_NAME, Context.MODE_PRIVATE)

        lifecycleScope.launch(Dispatchers.IO) {
            while (state.value == RecordState.RECORDING) {
                audioRecord.read(audioBuffer, /* offsetInBytes= */ 0, audioBuffer.size)
                fileStream?.write(audioBuffer)
            }
            fileStream?.close()
        }
    }

    private fun stopRecording(state: MutableState<RecordState>) {
        Log.i(TAG, "Stopping Recording")
        state.value = RecordState.AWAITING_PLAYBACK
        audioRecord.stop()
    }

    private fun startPlayback(state: MutableState<RecordState>) {
        Log.i(TAG, "Starting Playback")
        state.value = RecordState.PLAYBACK
        val audioFile = File(this.filesDir, AUDIO_FILE_NAME)
        val fileStream = FileInputStream(audioFile)
        val audioData = ByteArray(audioFile.length().toInt())
        fileStream.read(audioData, /* off= */ 0, audioData.size)
        fileStream.close()
        // Add a callback to update the state when audio playback is complete.
        audioTrack.setPlaybackPositionUpdateListener(
            object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(audioTrack: AudioTrack?) {
                    stopPlayback(state)
                }

                override fun onPeriodicNotification(track: AudioTrack?) {}
            }
        )
        audioTrack.notificationMarkerPosition = audioData.size / 2

        lifecycleScope.launch(Dispatchers.IO) {
            audioTrack.play()
            audioTrack.write(audioData, /* offsetInBytes= */ 0, audioData.size)
        }
    }

    private fun stopPlayback(state: MutableState<RecordState>) {
        Log.i(TAG, "Stopping Playback")
        state.value = RecordState.AWAITING_RECORDING
        audioTrack.stop()
    }

    private companion object {
        const val SAMPLE_RATE = 16000
        const val AUDIO_FILE_NAME: String = "audioRecording.wav"
        const val TAG = "AudioActivity"
    }
}
