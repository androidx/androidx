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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.xr.projected.ProjectedActivityCompat
import androidx.xr.projected.ProjectedInputEvent
import androidx.xr.projected.experimental.ExperimentalProjectedApi

/**
 * The ProjectedInputActivity creates the [ProjectedActivityCompat], starts collecting
 * [ProjectedInputEvent]s and displays corresponding information when an input is received. It
 * closes the [ProjectedActivityCompat] in onDestroy.
 */
@ExperimentalProjectedApi
class ProjectedInputActivity : ComponentActivity() {

    private lateinit var projectedActivityCompat: ProjectedActivityCompat
    private var isCameraOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DisplayProjectedState() }
    }

    @Composable
    private fun DisplayProjectedState() {
        val currentCameraState = remember { mutableStateOf(TURN_OFF_CAMERA_MESSAGE) }

        LaunchedEffect(Unit) {
            projectedActivityCompat = ProjectedActivityCompat.create(this@ProjectedInputActivity)
            projectedActivityCompat.projectedInputEvents.collect { inputEvent ->
                if (
                    inputEvent.inputAction ==
                        ProjectedInputEvent.ProjectedInputAction.TOGGLE_APP_CAMERA
                ) {
                    isCameraOn = !isCameraOn
                    currentCameraState.value =
                        if (isCameraOn) TURN_ON_CAMERA_MESSAGE else TURN_OFF_CAMERA_MESSAGE
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(text = currentCameraState.value)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        projectedActivityCompat.close()
    }

    companion object {
        private const val TURN_ON_CAMERA_MESSAGE = "Camera should be turned ON"
        private const val TURN_OFF_CAMERA_MESSAGE = "Camera should be turned OFF"
    }
}
