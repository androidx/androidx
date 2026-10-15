/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.compose.testapp.spatialcontinuity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.material3.SpaceToggleButton
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.ExperimentalMoveAnchorPolicy
import androidx.xr.compose.subspace.layout.ExperimentalSpatialContinuityApi
import androidx.xr.compose.subspace.layout.SpatialContinuityCapturer
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.spatialContinuityTarget
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.MainActivity
import androidx.xr.compose.testapp.ui.components.TopBarWithBackArrow
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.math.Pose

// TODO(b/540508810) Add more edge case examples to test activity
@OptIn(ExperimentalSpatialContinuityApi::class)
class SpatialContinuityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IntegrationTestsAppTheme {
                val mainPanelContinuityCapturer = remember { SpatialContinuityCapturer() }
                MainPanelUI(mainPanelContinuityCapturer)
                FullSpaceUI(mainPanelContinuityCapturer)
            }
        }
    }

    @Composable
    private fun MainPanelUI(spatialContinuityCapturer: SpatialContinuityCapturer) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.LightGray),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "SpatialMainPanel", fontSize = 28.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { spatialContinuityCapturer.captureContinuity() }) {
                Text("Set Continuity")
            }
            Spacer(Modifier.height(16.dp))
            Text("Spatial Continuity applied to this panel by default.")
        }
    }

    @Composable
    private fun FullSpaceUI(mainPanelContinuityCapturer: SpatialContinuityCapturer) {
        var showRowOfPanels by remember { mutableStateOf(true) }
        Subspace {
            OriginPanel()
            SpatialRow {
                SpatialMainPanel(
                    modifier =
                        SubspaceModifier.width(400.dp)
                            .height(300.dp)
                            .movable()
                            .spatialContinuityTarget(
                                spatialContinuityCapturer = mainPanelContinuityCapturer
                            )
                )
                ControlPanel(
                    showRowOfPanels = showRowOfPanels,
                    onShowRowOfPanelsChange = { showRowOfPanels = it },
                )
                if (showRowOfPanels) {
                    RowOfPanels()
                }
            }
        }
    }

    @OptIn(ExperimentalMoveAnchorPolicy::class)
    @SubspaceComposable
    @Composable
    private fun ControlPanel(
        modifier: SubspaceModifier = SubspaceModifier,
        showRowOfPanels: Boolean = true,
        onShowRowOfPanelsChange: (Boolean) -> Unit = {},
    ) {
        val session = LocalSession.current
        LaunchedEffect(session) {
            session?.configure(
                Config.Builder(session.config)
                    .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                    .build()
            )
        }
        val spatialContinuityCapturer = remember { SpatialContinuityCapturer() }
        var panelPose by remember { mutableStateOf(Pose.Identity) }
        var keepInFocus by remember { mutableStateOf(true) }

        SpatialPanel(
            modifier
                .width(500.dp)
                .height(600.dp)
                .padding(25.dp)
                .spatialContinuityTarget(spatialContinuityCapturer = spatialContinuityCapturer)
                .movable(
                    //                    movePolicy =
                    //                        MovePolicy.anchor(anchorPlaneOrientations =
                    // setOf(PlaneOrientation.Any)) {
                    //                            event ->
                    //                            panelPose = event.pose
                    //                        }
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().background(PurpleGrey80),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                TopBarWithBackArrow(
                    scrollBehavior = null,
                    title = "Pose Inheritance Test",
                    onClick = {
                        val intent =
                            Intent(this@SpatialContinuityActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    },
                )
                SpaceModeStatus()
                val intent = Intent(LocalContext.current, ActivityB::class.java)
                Button(onClick = { startActivity(intent) }) { Text("Launch Activity B") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            spatialContinuityCapturer.captureContinuity(keepInFocus = keepInFocus)
                        }
                    ) {
                        Text("Set Continuity")
                    }
                    Button(
                        onClick = {
                            session?.let { SpatialContinuityCapturer.resetContinuity(it) }
                        }
                    ) {
                        Text("Reset Continuity")
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "Keep in Focus", fontSize = 20.sp)
                    Switch(checked = keepInFocus, onCheckedChange = { keepInFocus = it })
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "Show Row of Panels", fontSize = 20.sp)
                    Switch(checked = showRowOfPanels, onCheckedChange = onShowRowOfPanelsChange)
                }

                Text(
                    text =
                        "Translation: x=${"%.2f".format(panelPose.translation.x)}, " +
                            "y=${"%.2f".format(panelPose.translation.y)}, " +
                            "z=${"%.2f".format(panelPose.translation.z)}",
                    fontSize = 20.sp,
                )
            }
        }
    }

    @SubspaceComposable
    @Composable
    private fun OriginPanel() {
        SpatialPanel(modifier = SubspaceModifier.height(30.dp).width(100.dp)) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Green),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "ORIGIN", fontSize = 20.sp)
            }
        }
    }

    @SubspaceComposable
    @Composable
    private fun RowOfPanels(modifier: SubspaceModifier = SubspaceModifier) {
        val spatialContinuityCapturer = remember { SpatialContinuityCapturer() }
        SpatialRow(modifier.movable()) {
            StandardPanel {}
            StandardPanel(
                SubspaceModifier.spatialContinuityTarget(
                    spatialContinuityCapturer = spatialContinuityCapturer
                )
            ) {
                Button(onClick = { spatialContinuityCapturer.captureContinuity() }) {
                    Text("Set Continuity")
                }
            }
            StandardPanel {}
        }
    }

    @Composable
    private fun SpaceModeStatus() {
        val isSpatial = LocalSpatialCapabilities.current.isSpatialUiEnabled
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                fontSize = 28.sp,
                text = if (isSpatial) "Current Mode: Full Space" else "Current Mode: Home Space",
            )
            Spacer(Modifier.height(40.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Toggle Space Mode ", fontSize = 20.sp)
                SpaceToggleButton(modifier = Modifier.background(Color.White, shape = CircleShape))
            }
        }
    }

    @SubspaceComposable
    @Composable
    fun StandardPanel(
        modifier: SubspaceModifier = SubspaceModifier,
        content: @Composable @UiComposable () -> Unit,
    ) {
        SpatialPanel(modifier = modifier.height(100.dp).width(200.dp)) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.LightGray),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
