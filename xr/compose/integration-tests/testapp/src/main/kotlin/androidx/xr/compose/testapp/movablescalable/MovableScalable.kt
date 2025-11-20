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

package androidx.xr.compose.testapp.movablescalable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.scale
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.components.ColumnWithCenterText
import androidx.xr.compose.testapp.ui.components.CommonTestPanel
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple80
import androidx.xr.compose.unit.DpVolumeSize

class MovableScalable : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IntegrationTestsAppTheme { MovableScalableApp() } }
    }

    @Composable
    private fun MovableScalableApp() {
        Subspace { SpatialContent() }
    }

    @Composable
    @SubspaceComposable
    private fun SpatialContent() {
        var zOffset by remember { mutableStateOf(0.dp) }
        var changedScale by remember { mutableFloatStateOf(1.0F) }
        var scaleForPanel by remember { mutableFloatStateOf(1.0F) }
        SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(20.dp)) {
            CommonTestPanel(
                size = DpVolumeSize(680.dp, 320.dp, 0.dp),
                title = getString(R.string.movable_scalable_panel_test),
                showBottomBar = true,
                onClickBackArrow = { this@MovableScalable.finish() },
                onClickRecreate = { this@MovableScalable.recreate() },
            ) { padding ->
                ColumnWithCenterText(padding, "Main Panel Content")
            }
            SpatialRow(horizontalArrangement = SpatialArrangement.spacedBy(40.dp)) {
                val density = LocalDensity.current
                SpatialPanel(
                    SubspaceModifier.height(200.dp).width(200.dp).scale(scaleForPanel),
                    dragPolicy =
                        MovePolicy(
                            isEnabled = true,
                            shouldScaleWithDistance = true,
                            onMove = { moveEvent ->
                                with(density) {
                                    zOffset = moveEvent.pose.translation.z.toDp()
                                    changedScale = moveEvent.scale
                                }
                                false
                            },
                        ),
                ) {
                    Box(
                        modifier = Modifier.background(Purple80).fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(text = "Movable Scalable Panel")
                            Button(
                                onClick = { scaleForPanel = if (scaleForPanel == 1.0F) 2F else 1F }
                            ) {
                                Column {
                                    Text(
                                        text =
                                            if (scaleForPanel == 1.0F) {
                                                "Add scale modifier"
                                            } else {
                                                "Remove scale modifier"
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
                SpatialPanel(
                    SubspaceModifier.offset(z = zOffset).height(200.dp).width(200.dp),
                    dragPolicy = MovePolicy(),
                ) {
                    Box(
                        modifier = Modifier.background(Purple80).fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(text = "Movable Non Scalable Panel", textAlign = TextAlign.Center)
                            Text(text = "Offset by z $zOffset", textAlign = TextAlign.Center)
                        }
                    }
                }
                SpatialPanel(SubspaceModifier.height(200.dp).width(200.dp)) {
                    Box(
                        modifier = Modifier.background(Purple80).fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("Panel to show scale factor", textAlign = TextAlign.Center)
                            Text(text = "Scale factor $changedScale", textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
