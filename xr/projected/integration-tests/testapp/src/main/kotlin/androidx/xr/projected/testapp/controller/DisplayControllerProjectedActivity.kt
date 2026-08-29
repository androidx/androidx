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

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The DisplayControllerProjectedActivity displays projected display state on the projected display.
 */
@OptIn(ExperimentalProjectedApi::class)
class DisplayControllerProjectedActivity : ComponentActivity() {

    internal lateinit var viewModel: DisplayControllerViewModel
    private var projectedDisplayController: ProjectedDisplayController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                val controller =
                    ProjectedDisplayController.create(this@DisplayControllerProjectedActivity)
                projectedDisplayController = controller
                updateKeepScreenOn(viewModel.keepScreenOn.value)
                controller.addPresentationModeChangedListener { flags ->
                    updatePresentationModes(flags)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize ProjectedDisplayController", e)
            }
        }

        setContent { DisplayControllerProjectedScreen(viewModel) }
    }

    private fun updatePresentationModes(flags: ProjectedDisplayController.PresentationModeFlags) {
        val modes = mutableListOf<String>()
        if (flags.hasPresentationMode(PresentationMode.VISUALS_ON)) {
            modes.add("VISUALS_ON")
        }
        if (flags.hasPresentationMode(PresentationMode.AUDIO_ON)) {
            modes.add("AUDIO_ON")
        }
        val modesText = if (modes.isEmpty()) "None" else modes.joinToString(", ")
        viewModel.setPresentationModes(modesText)
    }

    private fun updateKeepScreenOn(keepOn: Boolean) {
        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            try {
                projectedDisplayController?.addLayoutParamsFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add keep screen on flag", e)
            }
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            try {
                projectedDisplayController?.removeLayoutParamsFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove keep screen on flag", e)
            }
        }
    }

    override fun onDestroy() {
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            projectedDisplayController?.close()
        } catch (_: Exception) {}
        projectedDisplayController = null
        super.onDestroy()
    }

    @Composable
    private fun DisplayControllerProjectedScreen(viewModel: DisplayControllerViewModel) {
        val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
        val caps by viewModel.capabilities.collectAsStateWithLifecycle()
        val screenOnState by viewModel.keepScreenOn.collectAsStateWithLifecycle()
        val modesText by viewModel.presentationModes.collectAsStateWithLifecycle()
        val status by viewModel.statusMessage.collectAsStateWithLifecycle()

        DisposableEffect(screenOnState) {
            updateKeepScreenOn(screenOnState)
            onDispose {}
        }

        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!isConnected) {
                Text(status, color = Color.White, fontSize = 24.sp)
                return
            }

            var elapsedSec by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1.seconds)
                    elapsedSec++
                }
            }

            Text(text = "Elapsed: ${elapsedSec}s", color = Color(0xFFFFCC00), fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Capabilities: ${if (caps.isEmpty()) "None" else caps.joinToString()}",
                color = Color.White,
                fontSize = 22.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Keep Screen On: $screenOnState", color = Color(0xFF00FFCC), fontSize = 22.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Presentation Modes: $modesText", color = Color.Green, fontSize = 20.sp)
        }
    }

    private companion object {
        const val TAG = "DisplayControllerProjectedActivity"
    }
}
