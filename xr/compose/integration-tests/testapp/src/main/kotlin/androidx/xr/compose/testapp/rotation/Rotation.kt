/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.testapp.rotation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.components.CommonTestPanel
import androidx.xr.compose.testapp.ui.components.DigitalClock
import androidx.xr.compose.testapp.ui.components.initializePanelRotationData
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple80
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.math.Vector3

class Rotation : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IntegrationTestsAppTheme {
                Subspace {
                    val (rotation, axisAngle) = initializePanelRotationData()
                    SpatialColumn {
                        SpatialPanelLayout(rotation, axisAngle)
                        SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                        InfoPanel(rotation, axisAngle)
                    }
                }
            }
        }
    }

    @Composable
    private fun SpatialPanelLayout(rotation: Float, axisAngle: Vector3) {
        SpatialColumn {
            SpatialRow {
                SpatialColumn(modifier = SubspaceModifier.rotate(axisAngle, rotation)) {
                    PanelWithClock("top")
                    PanelWithClock("middle")
                    PanelWithClock("bottom")
                }

                SpatialColumn {
                    SpatialRow(modifier = SubspaceModifier.rotate(axisAngle, rotation)) {
                        PanelWithClock("left")
                        PanelWithClock("middle")
                        PanelWithClock("right")
                    }
                }

                SpatialRow { RotatingPanelWithClock(axisAngle, rotation) }
            }
        }
    }

    @Composable
    private fun PanelWithClock(text: String) {
        SpatialPanel(modifier = SubspaceModifier.width(240.dp).height(120.dp)) {
            Column(modifier = Modifier) {
                Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                DigitalClock().CreateDigitalClock()
            }
        }
    }

    @Composable
    private fun RotatingPanelWithClock(axisAngle: Vector3, rotation: Float) {
        SpatialPanel(
            modifier = SubspaceModifier.width(240.dp).height(240.dp).rotate(axisAngle, rotation)
        ) {
            Column {
                Text("Standalone", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                DigitalClock().CreateDigitalClock()
            }
        }
    }

    @Composable
    private fun InfoPanel(rotation: Float, axisAngle: Vector3) {
        CommonTestPanel(
            size = DpVolumeSize(640.dp, 480.dp, 0.dp),
            title = getString(R.string.panel_rotation_test),
            showBottomBar = true,
            onClickBackArrow = { this@Rotation.finish() },
            onClickRecreate = { this@Rotation.recreate() },
        ) { padding ->
            Column(
                modifier = Modifier.background(color = Purple80).padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Rotation: $rotation,\nAxis Angle: $axisAngle",
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        style = TextStyle(fontSize = 26.sp, lineHeight = 42.sp),
                    )
                }
            }
        }
    }
}
