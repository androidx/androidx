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

package androidx.xr.projected.testapp.inputorchestration

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi

/**
 * Host Phone Activity displayed on the phone screen with buttons to launch the projected activity,
 * toggle continuous audio stream perceptibility, and set/clear the activity input receiver.
 */
@OptIn(ExperimentalProjectedApi::class)
class InputOrchestrationActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val isPlayingAudio = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PhoneScreen() }
    }

    @Composable
    private fun PhoneScreen() {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Input Orchestration Test",
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(32.dp))

                Button(onClick = { launchProjectedActivity() }) { Text("Launch Activity") }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { startAudio() }, enabled = !isPlayingAudio.value) {
                        Text("Play Audio")
                    }
                    Button(onClick = { stopAudio() }, enabled = isPlayingAudio.value) {
                        Text("Stop Audio")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            sendActionToProjectedActivity(
                                InputOrchestrationProjectedActivity.ACTION_SET_INPUT_RECEIVER
                            )
                        }
                    ) {
                        Text("Set PendingIntent")
                    }
                    Button(
                        onClick = {
                            sendActionToProjectedActivity(
                                InputOrchestrationProjectedActivity.ACTION_CLEAR_INPUT_RECEIVER
                            )
                        }
                    ) {
                        Text("Clear PendingIntent")
                    }
                }
            }
        }
    }

    private fun createProjectedContext(): Context? {
        return try {
            ProjectedContext.createProjectedDeviceContext(this)
        } catch (e: Exception) {
            Log.w(TAG, "Error creating projected context: $e", e)
            null
        }
    }

    private fun launchProjectedActivity() {
        val projectedContext = createProjectedContext()
        if (projectedContext == null) {
            Log.e(TAG, "Projected Context is null. Cannot launch activity.")
            return
        }
        startActivity(
            Intent(this, InputOrchestrationProjectedActivity::class.java),
            ProjectedContext.createProjectedActivityOptions(projectedContext).toBundle(),
        )
    }

    private fun sendActionToProjectedActivity(action: String) {
        val projectedContext = createProjectedContext()
        if (projectedContext == null) {
            Log.e(TAG, "Projected Context is null. Cannot send action.")
            return
        }
        val intent = Intent(this, InputOrchestrationProjectedActivity::class.java)
        intent.putExtra(InputOrchestrationProjectedActivity.ACTION_KEY, action)
        startActivity(
            intent,
            ProjectedContext.createProjectedActivityOptions(projectedContext).toBundle(),
        )
    }

    private fun startAudio() {
        if (isPlayingAudio.value) return
        try {
            val assetFileDescriptor = assets.openFd("audio/audio_stream.mp3")
            val player =
                MediaPlayer().apply {
                    setDataSource(
                        assetFileDescriptor.fileDescriptor,
                        assetFileDescriptor.startOffset,
                        assetFileDescriptor.length,
                    )
                    assetFileDescriptor.close()
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            mediaPlayer = player
            isPlayingAudio.value = true
            Log.d(TAG, "Started playing audio stream track")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio playback", e)
            isPlayingAudio.value = false
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio player", e)
        } finally {
            mediaPlayer = null
            isPlayingAudio.value = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }

    companion object {
        private const val TAG = "InputOrchestrationAct"
    }
}
