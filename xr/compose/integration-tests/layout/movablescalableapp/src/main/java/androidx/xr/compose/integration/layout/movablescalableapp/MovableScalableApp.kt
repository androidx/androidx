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

package androidx.xr.compose.integration.layout.movablescalableapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.scale
import androidx.xr.compose.subspace.layout.width

class MovableScalableApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Subspace { SpatialContent() } }
    }

    @Composable
    @SubspaceComposable
    private fun SpatialContent() {
        var zOffset by remember { mutableStateOf(0.dp) }
        var heightOfScalablePanel by remember { mutableStateOf(200.dp) }
        var scaleForPanel by remember { mutableFloatStateOf(1.0F) }
        var heightOfNonScalablePanel by remember { mutableStateOf(200.dp) }
        SpatialRow {
            val density = LocalDensity.current
            SpatialPanel(
                SubspaceModifier.height(200.dp)
                    .width(200.dp)
                    .scale(scaleForPanel)
                    .movable(
                        enabled = true,
                        scaleWithDistance = true,
                        onMove = { poseChangeEvent ->
                            with(density) {
                                zOffset = poseChangeEvent.pose.translation.z.toDp()
                                heightOfScalablePanel = poseChangeEvent.size.height.toDp()
                            }
                            false
                        },
                    )
            ) {
                Box(
                    modifier = Modifier.background(Color.LightGray).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Movable Scalable Panel")
                    Button(onClick = { scaleForPanel = if (scaleForPanel == 1.0F) 2F else 1F }) {
                        Column { Text(text = "Add scale modifier") }
                    }
                }
            }
            SpatialPanel(
                SubspaceModifier.offset(z = zOffset)
                    .height(heightOfNonScalablePanel)
                    .width(200.dp)
                    .movable()
            ) {
                Box(
                    modifier = Modifier.background(Color.LightGray).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column {
                        Text(text = "Movable Non Scalable Panel")
                        Text(text = "Offset by z ${zOffset}")
                    }
                }
            }
            SpatialPanel(SubspaceModifier.height(200.dp).width(200.dp)) {
                Box(
                    modifier = Modifier.background(Color.LightGray).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column {
                        Text(text = "Panel to show scale factor")
                        Text(
                            text = "Scale factor ${heightOfScalablePanel/heightOfNonScalablePanel}"
                        )
                    }
                }
            }
        }
    }
}
