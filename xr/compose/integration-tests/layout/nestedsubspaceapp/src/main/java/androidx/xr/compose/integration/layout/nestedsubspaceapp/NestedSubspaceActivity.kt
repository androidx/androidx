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
package androidx.xr.compose.integration.layout.nestedsubspaceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.width

class NestedSubspaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.height(400.dp).width(800.dp).movable().resizable()) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        Spacer(Modifier.fillMaxWidth(0.25f).fillMaxHeight().background(Color.Cyan))
                        Spacer(Modifier.background(Color.Magenta))
                        Spacer(Modifier.background(Color.White))
                        Box(Modifier.background(Color.Yellow).fillMaxHeight().weight(1.0f)) {
                            // Here we have a nested Subspace that is capable of rendering a 3D
                            // layout within the
                            // scope of this 2D panel.
                            Subspace {
                                var count by remember { mutableIntStateOf(0) }
                                SpatialRow(
                                    modifier = SubspaceModifier.fillMaxSize(),
                                    alignment = SpatialAlignment.Center,
                                ) {
                                    SpatialPanel(
                                        SubspaceModifier.fillMaxSize(0.5f).offset(z = 150.dp)
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier.background(Color.Green)
                                                    .fillMaxSize(0.9f)
                                                    .border(20.dp, Color.White),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Button(onClick = { count++ }) { Text("Increase") }
                                        }
                                    }
                                    SpatialLayoutSpacer(SubspaceModifier.size(50.dp))
                                    SpatialPanel(SubspaceModifier.offset(z = 250.dp)) {
                                        Box(
                                            modifier =
                                                Modifier.background(Color.Blue).padding(20.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(text = "$count", fontSize = 50.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
