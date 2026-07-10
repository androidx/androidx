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

package androidx.xr.projected.testapp.controller

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.ProjectedDisplayController.PresentationModeFlags
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testapp.R
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * The DisplayControllerProjectedActivity displays and updates the ProjectedDisplayController state.
 */
@OptIn(ExperimentalProjectedApi::class)
class DisplayControllerProjectedActivity : ComponentActivity() {

    var statusMessage: String = "Initializing"
    var keepScreenOn = mutableStateOf(false)

    var projectedDisplayController: ProjectedDisplayController? = null
    val displayControllerReady = mutableStateOf(false)

    var projectedDeviceController: ProjectedDeviceController? = null
    val deviceControllerReady = mutableStateOf(false)

    val presentationModesList = mutableStateListOf<PresentationModeFlags>()
    var capabilities = emptySet<Capability>()

    var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "Creating DisplayControllerProjectedActivity")
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        initializeProjectedDeviceController(this)
        initializeProjectedDisplayController(presentationModesList, this)
        setContent { CreateUi() }
    }

    override fun onDestroy() {
        Log.i(TAG, "Destroying DisplayControllerProjectedActivity")
        mediaPlayer?.stop()
        mediaPlayer?.release()
        super.onDestroy()
    }

    @Composable
    private fun CreateUi() {
        val screenOnState = remember { keepScreenOn }
        val presentationModesList = remember { presentationModesList }
        val displayControllerReadyFlag by remember { displayControllerReady }
        val deviceControllerReadyFlag by remember { deviceControllerReady }
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            if (!displayControllerReadyFlag || !deviceControllerReadyFlag) {
                Text(statusMessage)
                return
            }

            var elapsedSec by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1.seconds)
                    elapsedSec++
                }
            }
            // TODO(b/459576296): Currently using a continuously updating UI element to force
            // Compose updates. Without this, there seems to be a race condition right when the
            // display turns on. This should be removed once we understand the problem.
            Text(text = "Elapsed Time: $elapsedSec seconds")
            Text(text = "")
            Text("Projected Capabilities: $capabilities", fontSize = 30.sp)
            Text("Keep Screen On: ${screenOnState.value}", fontSize = 30.sp)
            Text("Presentation Mode Events:")
            PresentationModeFlagsList(presentationModesList.toList())
        }
    }

    private fun initializeProjectedDeviceController(activity: Activity) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                projectedDeviceController = ProjectedDeviceController.create(activity)
                projectedDeviceController?.let {
                    capabilities = it.capabilities
                    if (CAPABILITY_VISUAL_UI in it.capabilities) {
                        mediaPlayer = MediaPlayer.create(activity, R.raw.display_detected)
                        Log.i(TAG, "Loading display detected voice clip")
                    } else {
                        mediaPlayer = MediaPlayer.create(activity, R.raw.no_display)
                        Log.i(TAG, "Loading no display voice clip")
                    }
                }
                deviceControllerReady.value = true
            } catch (e: Exception) {
                statusMessage = "Failed to start ProjectedDeviceController."
                Log.e(TAG, "Failed to start ProjectedDeviceController with error: ${e.message}")
            }
        }
    }

    private fun initializeProjectedDisplayController(
        presentationModesList: SnapshotStateList<PresentationModeFlags>,
        activity: Activity,
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                projectedDisplayController = ProjectedDisplayController.create(activity)
                projectedDisplayController?.let {
                    updateScreenOnFlag()
                    it.addPresentationModeChangedListener { updatedPresentationModes ->
                        presentationModesList.add(updatedPresentationModes)
                    }
                    displayControllerReady.value = true
                }
            } catch (e: Exception) {
                statusMessage = "Failed to start ProjectedDisplayController."
                Log.e(TAG, "Failed to start ProjectedDisplayController with error: ${e.message}")
            }
        }
    }

    @Composable
    private fun PresentationModeFlagsList(presentationModesList: List<PresentationModeFlags>) {
        Column() {
            for (presentationModeFlags in presentationModesList) {
                var modes = "Mode(s):"
                if (presentationModeFlags.hasPresentationMode(PresentationMode.VISUALS_ON)) {
                    modes += " ${PresentationMode.VISUALS_ON}"
                }
                if (presentationModeFlags.hasPresentationMode(PresentationMode.AUDIO_ON)) {
                    modes += " ${PresentationMode.AUDIO_ON}"
                }
                Text(modes)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra("KEEP_SCREEN_ON")) {
            keepScreenOn.value = intent.getBooleanExtra("KEEP_SCREEN_ON", false)
            Log.i(TAG, "Received Intent with KEEP_SCREEN_ON value of: ${keepScreenOn.value}")
            updateScreenOnFlag()
        }
        if (intent.getBooleanExtra("PLAY_SOUND", false)) {
            if (mediaPlayer != null) {
                mediaPlayer?.start()
                Log.i(TAG, "Received Intent with PLAY_SOUND: Playing voice clip")
            } else {
                Log.w(
                    TAG,
                    "Received Intent with PLAY_SOUND: However can't play, voice clip not ready",
                )
            }
        }
    }

    private fun updateScreenOnFlag() {
        projectedDisplayController?.let {
            if (keepScreenOn.value) {
                it.addLayoutParamsFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                it.removeLayoutParamsFlags(
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
        }
    }

    private companion object {
        const val TAG = "DisplayControllerProjectedActivity"
    }
}
