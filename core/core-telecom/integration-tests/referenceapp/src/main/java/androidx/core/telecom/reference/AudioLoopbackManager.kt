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

package androidx.core.telecom.reference

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*

class AudioLoopbackManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioLoopbackManager"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var loopbackJob: Job? = null
    private val isLooping = AtomicBoolean(false)

    // Get AudioManager instance internally
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // --- Configuration  ---
    private val minInputBufferSize =
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
    private val minOutputBufferSize =
        AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)
    private val bufferSize = (minInputBufferSize * 4).coerceAtLeast(minOutputBufferSize * 4)
    private val audioBuffer = ByteArray(bufferSize)

    fun isLoopbackRunning(): Boolean = isLooping.get()

    @SuppressLint("MissingPermission")
    fun startLoopback(scope: CoroutineScope): Boolean {
        if (!hasRecordAudioPermission()) {
            Log.e(TAG, "RECORD_AUDIO permission not granted.")
            return false
        }

        // *** Add Mute Check before starting ***
        if (audioManager.isMicrophoneMute) {
            Log.w(TAG, "Cannot start loopback: Global mute is active.")
            return false
        }

        if (isLooping.compareAndSet(false, true)) {
            Log.d(TAG, "Starting loopback...")

            try {
                // --- Configure AudioRecord
                audioRecord = AudioRecord.Builder().build()

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord initialization failed.")
                    releaseResources()
                    return false
                }

                // --- Configure AudioTrack
                audioTrack = AudioTrack.Builder().build()

                if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioTrack initialization failed.")
                    releaseResources()
                    return false
                }

                // --- Start Processing Coroutine ---
                loopbackJob =
                    scope.launch(Dispatchers.IO) {
                        Log.d(TAG, "Loopback coroutine started.")
                        try {
                            // Log initial state
                            Log.d(TAG, "AudioManager mode: ${audioManager.mode}")
                            Log.d(
                                TAG,
                                "Initial global mute state: ${audioManager.isMicrophoneMute}",
                            )

                            audioRecord?.startRecording()
                            audioTrack?.play()

                            Log.i(TAG, "Loopback audio flowing. Buffer size: $bufferSize bytes.")

                            while (isActive && isLooping.get()) {
                                // *** Add Mute Check within the loop ***
                                if (audioManager.isMicrophoneMute) {
                                    Log.i(
                                        TAG,
                                        "Global mute detected active during loopback. Stopping.",
                                    )
                                    // Setting isLooping false will gracefully exit the loop
                                    // and trigger the finally block for cleanup.
                                    isLooping.set(false)
                                    continue // Skip reading/writing for this iteration
                                }

                                val bytesRead =
                                    audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1

                                if (bytesRead > 0) {
                                    audioTrack?.write(audioBuffer, 0, bytesRead)
                                } else if (bytesRead < 0) {
                                    Log.e(TAG, "Error reading from AudioRecord: $bytesRead")
                                    isLooping.set(false) // Stop loop on error
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception in loopback coroutine", e)
                        } finally {
                            Log.d(TAG, "Loopback coroutine finishing. Releasing resources.")
                            releaseResourcesInternal()
                            isLooping.set(false) // Ensure state reflects loop end
                            Log.d(TAG, "Loopback coroutine finally block complete.")
                        }
                    }
                return true // Started successfully
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during initialization.", e)
                releaseResources()
                return false
            } catch (e: Exception) {
                Log.e(TAG, "Exception during initialization", e)
                releaseResources()
                return false
            }
        } else {
            Log.w(TAG, "Loopback already running or failed to set state.")
            return false
        }
    }

    fun stopLoopback() {
        if (isLooping.compareAndSet(true, false)) {
            Log.d(TAG, "Stopping loopback requested...")
            // Setting isLooping to false is the primary signal.
            // The loop checks this flag and the mute state.
            // We can still cancel the job for immediate interruption if needed,
            // but letting the loop check handle it is often cleaner.
            loopbackJob?.cancel("Explicit stop requested")
        } else {
            Log.d(TAG, "Loopback not running or already stopping.")
        }
    }

    fun releaseResources() {
        Log.d(TAG, "Explicitly releasing resources.")
        isLooping.set(false)
        loopbackJob?.cancel()
        releaseResourcesInternal()
    }

    private fun releaseResourcesInternal() {
        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
            Log.w(TAG, "AudioRecord already stopped or not initialized.")
        }
        audioRecord?.release()
        audioRecord = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "AudioTrack already stopped or not initialized.")
        }
        audioTrack?.release()
        audioTrack = null

        Log.d(TAG, "Audio resources released.")
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }
}
