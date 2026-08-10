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

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.xr.projected.ProjectedActivityCompat
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.launch

/**
 * Projected Activity running on the projected display. Listens for input events and plays an earcon
 * sound upon receiving input.
 */
@OptIn(ExperimentalProjectedApi::class)
class InputOrchestrationProjectedActivity : ComponentActivity() {

    private var projectedActivityCompat: ProjectedActivityCompat? = null
    private var soundPool: SoundPool? = null
    private var earconSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        initSoundPool()

        lifecycleScope.launch {
            try {
                val activityCompat =
                    ProjectedActivityCompat.create(this@InputOrchestrationProjectedActivity)
                projectedActivityCompat = activityCompat
                handleIntent(intent)
                activityCompat.projectedInputEvents.collect { playEarconTapSound() }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ProjectedActivityCompat", e)
            }
        }

        setContent { ProjectedScreen() }
    }

    private fun initSoundPool() {
        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(audioAttributes).build()
        soundPool = pool
        try {
            val assetFileDescriptor = assets.openFd("audio/earcon_tap.ogg")
            earconSoundId = pool.load(assetFileDescriptor, 1)
            assetFileDescriptor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading earcon tap sound", e)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            playEarconTapSound()
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            playEarconTapSound()
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            playEarconTapSound()
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.getStringExtra(ACTION_KEY) ?: return
        Log.d(TAG, "Handling intent action: $action")
        when (action) {
            ACTION_SET_INPUT_RECEIVER -> setPendingIntent()
            ACTION_CLEAR_INPUT_RECEIVER -> clearPendingIntent()
        }
    }

    private fun setPendingIntent() {
        val launchIntent = Intent(this, InputOrchestrationProjectedActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)
        val activityCompat = projectedActivityCompat
        if (activityCompat != null) {
            activityCompat.setActivityAsInputReceiver(pendingIntent)
            Log.d(TAG, "setActivityAsInputReceiver succeeded")
        } else {
            Log.w(TAG, "ProjectedActivityCompat is null; cannot set activity as input receiver")
        }
    }

    private fun clearPendingIntent() {
        val activityCompat = projectedActivityCompat
        if (activityCompat != null) {
            activityCompat.clearActivityAsInputReceiver()
            Log.d(TAG, "clearActivityAsInputReceiver succeeded")
        } else {
            Log.w(TAG, "ProjectedActivityCompat is null; cannot clear activity as input receiver")
        }
    }

    private fun playEarconTapSound() {
        val pool = soundPool
        if (pool != null && earconSoundId != 0) {
            pool.play(earconSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    @Composable
    private fun ProjectedScreen() {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Input Orchestration Projected Activity", fontSize = 18.sp)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
        projectedActivityCompat?.close()
        projectedActivityCompat = null
    }

    companion object {
        private const val TAG = "InputOrchestrationProj"
        const val ACTION_KEY = "ACTION"
        const val ACTION_SET_INPUT_RECEIVER = "SET_INPUT_RECEIVER"
        const val ACTION_CLEAR_INPUT_RECEIVER = "CLEAR_INPUT_RECEIVER"
    }
}
