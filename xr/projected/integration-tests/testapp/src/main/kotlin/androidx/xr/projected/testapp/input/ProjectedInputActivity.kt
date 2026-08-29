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

package androidx.xr.projected.testapp.input

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.xr.projected.ProjectedActivityCompat
import androidx.xr.projected.ProjectedInputEvent
import androidx.xr.projected.experimental.ExperimentalProjectedApi

/** Activity running on projected display capturing key and input events to update ViewModel. */
@OptIn(ExperimentalProjectedApi::class)
class ProjectedInputActivity : ComponentActivity() {

    internal lateinit var viewModel: InputViewModel
    private var projectedActivityCompat: ProjectedActivityCompat? = null
    private var isCameraOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProjectedInputScreen(viewModel) }
    }

    @Composable
    private fun ProjectedInputScreen(viewModel: InputViewModel) {
        val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
        val cameraState by viewModel.cameraToggleStatus.collectAsStateWithLifecycle()
        val eventLogs by viewModel.eventLogs.collectAsStateWithLifecycle()
        val lastEvent = eventLogs.lastOrNull() ?: "No key events yet"

        LaunchedEffect(Unit) {
            try {
                val controller = ProjectedActivityCompat.create(this@ProjectedInputActivity)
                projectedActivityCompat = controller
                controller.projectedInputEvents.collect { inputEvent ->
                    if (
                        inputEvent.inputAction ==
                            ProjectedInputEvent.ProjectedInputAction.TOGGLE_APP_CAMERA
                    ) {
                        isCameraOn = !isCameraOn
                        val stateMsg =
                            if (isCameraOn) TURN_ON_CAMERA_MESSAGE else TURN_OFF_CAMERA_MESSAGE
                        viewModel.setCameraToggleStatus(stateMsg)
                        viewModel.addEventLog("TOGGLE_APP_CAMERA -> $stateMsg")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed ProjectedActivityCompat", e)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Input Test (Projected)", color = Color.White, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Connection: ${if (isConnected) "Connected" else "Not Connected"}",
                color = if (isConnected) Color.Green else Color.Red,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = cameraState, color = Color(0xFFFFCC00), fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Last Event: $lastEvent", color = Color(0xFF00FFCC), fontSize = 18.sp)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val actionName =
            when (event.action) {
                KeyEvent.ACTION_DOWN -> "ACTION_DOWN"
                KeyEvent.ACTION_UP -> "ACTION_UP"
                else -> "ACTION(${event.action})"
            }
        val keyName = KeyEvent.keyCodeToString(event.keyCode)
        val desc = "$keyName: $actionName"
        viewModel.addEventLog(desc)

        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        try {
            projectedActivityCompat?.close()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    private companion object {
        const val TAG = "ProjectedInputActivity"
        const val TURN_ON_CAMERA_MESSAGE = "Camera should be turned ON"
        const val TURN_OFF_CAMERA_MESSAGE = "Camera should be turned OFF"
    }
}
