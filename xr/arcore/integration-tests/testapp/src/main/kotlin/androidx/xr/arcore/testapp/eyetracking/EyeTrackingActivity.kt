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

package androidx.xr.arcore.testapp.eyetracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.arcore.perceptionState
import androidx.xr.arcore.testapp.common.BackToMainActivityButton
import androidx.xr.arcore.testapp.common.SessionLifecycleHelper
import androidx.xr.arcore.testapp.common.asString
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.Session
import kotlinx.coroutines.launch

class EyeTrackingActivity : ComponentActivity() {

    private var gazeRenderer = GazeRenderer()
    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private var config by
        mutableStateOf(
            Config.Builder()
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .setEyeTracking(EyeTrackingMode.COARSE_TRACKING)
                .build()
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionHelper =
            SessionLifecycleHelper(
                this,
                config,
                onSessionAvailable = { newSession ->
                    session = newSession

                    lifecycleScope.launch {
                        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            setContent {
                                Subspace {
                                    SpatialPanel(
                                        modifier =
                                            SubspaceModifier.size(
                                                    DpVolumeSize(640.dp, 480.dp, 0.dp)
                                                )
                                                .movable()
                                                .resizable()
                                    ) {
                                        Main(session)
                                    }
                                }
                            }
                        }
                    }
                },
            )
        sessionHelper.tryCreateSession()
    }

    override fun onPause() {
        super.onPause()
        gazeRenderer.stopRendering()
    }

    override fun onResume() {
        super.onResume()
        gazeRenderer.startRendering(session, lifecycleScope)
    }

    private fun toggleEyeTrackingConfigMode() {
        val currentMode = config.eyeTracking
        val newMode =
            when (currentMode) {
                // cycle through the 2 different eye tracking config modes
                EyeTrackingMode.COARSE_TRACKING -> EyeTrackingMode.FINE_TRACKING
                EyeTrackingMode.FINE_TRACKING -> EyeTrackingMode.COARSE_TRACKING
                else -> {
                    throw IllegalStateException("Invalid Eye Tracking mode")
                }
            }

        // reconfigure the session
        config =
            Config.Builder()
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .setEyeTracking(newMode)
                .build()
        sessionHelper.tryUpdateConfig(config)
    }

    @Composable
    private fun Main(session: Session) {
        val state by session.state.collectAsStateWithLifecycle()
        val perceptionState = state.perceptionState

        Scaffold(
            modifier = Modifier.fillMaxSize().padding(0.dp),
            topBar = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(0.dp).background(color = GoogleYellow),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackToMainActivityButton()
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = title.toString(),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                }
            },
        ) { innerPadding ->
            @Suppress("DEPRECATION")
            Column(
                modifier =
                    Modifier.background(color = Color.White)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (perceptionState == null) {
                    Text("Perception State is null", fontSize = 20.sp)
                } else {
                    val leftEye = perceptionState.leftEyeState
                    val rightEye = perceptionState.rightEyeState
                    Button(onClick = { toggleEyeTrackingConfigMode() }) {
                        // button displays current eyetracking mode, click it to change
                        Text(text = "Mode: ${config.eyeTracking.asString()}", fontSize = 20.sp)
                    }
                    // Display left eye information.
                    Column {
                        if (leftEye != null) {
                            Text(
                                text = "Left Eye Found",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text =
                                    "Left Eye State: ${if (leftEye.isOpen) "Open" else "Closed"}",
                                fontSize = 18.sp,
                            )
                            Text(
                                text =
                                    "Left Eye Tracking State: ${leftEye.trackingState.asString()}",
                                fontSize = 18.sp,
                            )
                            Text(text = "Left Eye Pose: ${leftEye.pose}", fontSize = 14.sp)
                        } else {
                            Text(
                                text = "No Left Eye",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    // Display right eye information.
                    Column {
                        if (rightEye != null) {
                            Text(
                                text = "Right Eye Found",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text =
                                    "Right Eye State: ${if (rightEye.isOpen) "Open" else "Closed"}",
                                fontSize = 18.sp,
                            )
                            Text(
                                text =
                                    "Right Eye Tracking State: ${rightEye.trackingState.asString()}",
                                fontSize = 18.sp,
                            )
                            Text(text = "Right Eye Pose: ${rightEye.pose}", fontSize = 14.sp)
                        } else {
                            Text(
                                text = "No Right Eye",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    // Display eye dot color legend.
                    Column {
                        Text(text = "Color Legend", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(text = "\tGreen = Left Eye", fontSize = 12.sp)
                        Text(text = "\tBlue = Right Eye", fontSize = 12.sp)
                        Text(text = "\tBoxes are opaque when eyes are open", fontSize = 12.sp)
                        Text(text = "\tand translucent when eyes are shut.", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    private fun EyeTrackingMode.asString(): String {
        return when (this) {
            EyeTrackingMode.COARSE_TRACKING -> "Coarse Tracking"
            EyeTrackingMode.FINE_TRACKING -> "Fine Tracking"
            EyeTrackingMode.DISABLED -> "Disabled"
            else -> "Unknown"
        }
    }

    companion object {
        const val ACTIVITY_NAME: String = "EyeTrackingActivity"
    }
}
