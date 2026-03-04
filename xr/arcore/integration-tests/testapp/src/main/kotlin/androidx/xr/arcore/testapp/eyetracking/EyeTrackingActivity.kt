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
import androidx.xr.arcore.Eye
import androidx.xr.arcore.perceptionState
import androidx.xr.arcore.testapp.common.BackToMainActivityButton
import androidx.xr.arcore.testapp.common.SessionLifecycleHelper
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.launch

class EyeTrackingActivity : ComponentActivity() {

    private var gazeRenderer = GazeRenderer()
    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private var config: Config =
        Config(
            deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN,
            eyeTracking = EyeTrackingMode.COARSE_TRACKING,
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
                                            ),
                                        dragPolicy = MovePolicy(),
                                        resizePolicy = ResizePolicy(),
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
                // cycle through the 3 different eye tracking config modes
                EyeTrackingMode.COARSE_TRACKING -> EyeTrackingMode.FINE_TRACKING
                EyeTrackingMode.FINE_TRACKING -> EyeTrackingMode.COARSE_TRACKING
                else -> {
                    throw IllegalStateException("Invalid Eye Tracking mode")
                }
            }

        // reconfigure the session
        config =
            Config(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN, eyeTracking = newMode)
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
            Column(
                modifier =
                    Modifier.background(color = Color.White)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
            ) {
                if (perceptionState == null) {
                    Row { Text("Perception State is null", fontSize = 20.sp) }
                } else {
                    val leftEye = getEyePose(perceptionState.leftEye)
                    val rightEye = getEyePose(perceptionState.rightEye)
                    Row {
                        Button(onClick = { toggleEyeTrackingConfigMode() }) {
                            // button displays current eyetracking mode. click it to change.
                            Text(text = config.eyeTracking.asString(), fontSize = 20.sp)
                        }
                    }
                    // Display left eye pose, if found.
                    Row {
                        var text = "No Left Eye"
                        leftEye?.let { text = "Left Eye Found" }
                        Text(text = text, fontSize = 20.sp)
                        leftEye?.let { Text(text = "$it") }
                    }
                    // Display right eye pose, if found.
                    Row {
                        var text = "No Right Eye"
                        rightEye?.let { text = "Right Eye Found" }
                        Text(text = text, fontSize = 20.sp)
                        rightEye?.let { Text(text = "$it") }
                    }
                    // Display eye dot color legend.
                    Row {
                        Text(text = "Color Legend", fontSize = 15.sp)
                        Text(text = "\tGreen = Left Eye", fontSize = 12.sp)
                        Text(text = "\tBlue = Right Eye", fontSize = 12.sp)
                        Text(text = "\tBoxes are opaque when eyes are open", fontSize = 12.sp)
                        Text(text = "\tand translucent when eyes are shut.", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    private fun getEyePose(eye: Eye?): Pose? = eye?.state?.value?.pose

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
