/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.avsync

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import androidx.camera.core.Logger
import androidx.camera.integration.avsync.model.AudioGenerator
import androidx.camera.integration.avsync.model.CameraHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.util.Preconditions
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext

private const val ACTIVE_LENGTH_SEC: Double = 0.5
private const val ACTIVE_INTERVAL_SEC: Double = 1.0
private const val ACTIVE_DELAY_SEC: Double = 0.0
private const val VOLUME_PERCENTAGE: Double = 1.0
private const val TAG = "SignalGeneratorViewModel"

enum class ActivationSignal {
    Active, Inactive
}

class SignalGeneratorViewModel : ViewModel() {

    private var signalGenerationJob: Job? = null
    private lateinit var audioGenerator: AudioGenerator
    private val cameraHelper = CameraHelper()
    private lateinit var audioManager: AudioManager
    private var originalVolume: Int = 0

    var isGeneratorReady: Boolean by mutableStateOf(false)
        private set
    var isRecorderReady: Boolean by mutableStateOf(false)
        private set
    var isSignalGenerating: Boolean by mutableStateOf(false)
        private set
    var isActivePeriod: Boolean by mutableStateOf(false)
        private set
    var isRecording: Boolean by mutableStateOf(false)
        private set
    var isPaused: Boolean by mutableStateOf(false)
        private set

    suspend fun initialRecorder(context: Context, lifecycleOwner: LifecycleOwner) {
        withContext(Dispatchers.Main) {
            isRecorderReady = cameraHelper.bindCamera(context, lifecycleOwner)
        }
    }

    suspend fun initialSignalGenerator(context: Context, beepFrequency: Int, beepEnabled: Boolean) {
        audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        saveOriginalVolume()

        withContext(Dispatchers.Default) {
            audioGenerator = AudioGenerator(beepEnabled)
            audioGenerator.initial(
                context = context,
                frequency = beepFrequency,
                beepLengthInSec = ACTIVE_LENGTH_SEC,
            )
            isGeneratorReady = true
        }
    }

    private fun setVolume(percentage: Double = VOLUME_PERCENTAGE) {
        Preconditions.checkArgument(percentage in 0.0..1.0)

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (maxVolume * percentage).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
    }

    fun startSignalGeneration() {
        Logger.d(TAG, "Start signal generation.")
        Preconditions.checkState(isGeneratorReady)

        setVolume()

        signalGenerationJob?.cancel()
        isSignalGenerating = true
        signalGenerationJob = activationSignalFlow().map { activationSignal ->
            when (activationSignal) {
                ActivationSignal.Active -> {
                    isActivePeriod = true
                    playBeepSound()
                }
                ActivationSignal.Inactive -> {
                    isActivePeriod = false
                    stopBeepSound()
                }
            }
        }.onCompletion {
            stopBeepSound()
            restoreOriginalVolume()
            isActivePeriod = false
        }.launchIn(viewModelScope)
    }

    fun stopSignalGeneration() {
        Logger.d(TAG, "Stop signal generation.")
        Preconditions.checkState(isGeneratorReady)

        isSignalGenerating = false
        signalGenerationJob?.cancel()
        signalGenerationJob = null
    }

    fun startRecording(context: Context) {
        Logger.d(TAG, "Start recording.")
        Preconditions.checkState(isRecorderReady)

        cameraHelper.startRecording(context)
        isRecording = true
    }

    fun stopRecording() {
        Logger.d(TAG, "Stop recording.")
        Preconditions.checkState(isRecorderReady)

        cameraHelper.stopRecording()
        isRecording = false
        isPaused = false
    }

    fun pauseRecording() {
        Logger.d(TAG, "Pause recording.")
        Preconditions.checkState(isRecorderReady)

        cameraHelper.pauseRecording()
        isPaused = true
    }

    fun resumeRecording() {
        Logger.d(TAG, "Resume recording.")
        Preconditions.checkState(isRecorderReady)

        cameraHelper.resumeRecording()
        isPaused = false
    }

    private fun saveOriginalVolume() {
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    private fun restoreOriginalVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
    }

    private fun activationSignalFlow() = flow {
        delay((ACTIVE_DELAY_SEC * 1000).toLong())
        while (true) {
            emit(ActivationSignal.Active)
            delay((ACTIVE_LENGTH_SEC * 1000).toLong())
            emit(ActivationSignal.Inactive)
            delay((ACTIVE_INTERVAL_SEC * 1000).toLong())
        }
    }

    private fun playBeepSound() {
        audioGenerator.start()
    }

    private fun stopBeepSound() {
        audioGenerator.stop()
    }
}
