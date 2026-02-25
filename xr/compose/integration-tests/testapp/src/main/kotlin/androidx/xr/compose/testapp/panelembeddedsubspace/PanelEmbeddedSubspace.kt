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

package androidx.xr.compose.testapp.panelembeddedsubspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.PlanarEmbeddedSubspace
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SceneCoreEntity
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SpatialSpacer
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.draw.scale
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.width
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import java.nio.file.Paths

class PanelEmbeddedSubspace : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Subspace {
                SpatialRow {
                    SpatialPanel(
                        SubspaceModifier.height(400.dp).width(800.dp),
                        dragPolicy = MovePolicy(),
                        resizePolicy = ResizePolicy(),
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                            Box(
                                Modifier.fillMaxWidth(0.25f).fillMaxHeight().background(Color.Cyan),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Absolute.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val plainTooltipState = remember { TooltipState() }
                                    @Suppress("DEPRECATION")
                                    TooltipBox(
                                        positionProvider =
                                            TooltipDefaults.rememberTooltipPositionProvider(),
                                        tooltip = { PlainTooltip { Text("Back to main") } },
                                        state = plainTooltipState,
                                    ) {
                                        IconButton(
                                            onClick = { this@PanelEmbeddedSubspace.finish() }
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                "backIcon",
                                                Modifier.size(56.dp).scale(1.5f),
                                                tint = Color.Blue,
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.background(Color.Magenta))
                            Spacer(Modifier.background(Color.White))
                            Box(Modifier.background(Color.Yellow).fillMaxHeight().weight(1.0f)) {
                                // Here we have a PanelEmbeddedSubspace that is capable of rendering
                                // a 3D layout within the scope of this 2D panel.
                                PlanarEmbeddedSubspace {
                                    var count by remember { mutableIntStateOf(0) }
                                    SpatialRow(
                                        modifier = SubspaceModifier.fillMaxSize(),
                                        verticalAlignment = SpatialAlignment.CenterVertically,
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
                                        SpatialSpacer(SubspaceModifier.size(50.dp))
                                        SpatialPanel(SubspaceModifier.offset(z = 250.dp)) {
                                            Box(
                                                modifier =
                                                    Modifier.background(Color.Blue).padding(20.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = "$count",
                                                    fontSize = 50.sp,
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SpatialSpacer(SubspaceModifier.size(100.dp))
                    SpatialPanel(
                        SubspaceModifier.height(800.dp).width(400.dp),
                        dragPolicy = MovePolicy(),
                    ) {
                        Box(Modifier.border(30.dp, Color.White, RoundedCornerShape(10.dp))) {
                            PlanarEmbeddedSubspace {
                                SpatialPanel(SubspaceModifier.offset(z = (-200).dp)) {
                                    Box(Modifier.fillMaxSize().background(Color.Black))
                                }
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(40.dp),
                            ) {
                                for (i in 0..15) {
                                    item {
                                        Box(
                                            contentAlignment = Alignment.BottomCenter,
                                            modifier =
                                                Modifier.border(
                                                        width = 1.dp,
                                                        color = Color.Green,
                                                        shape = RoundedCornerShape(10.dp),
                                                    )
                                                    .aspectRatio(1f),
                                        ) {
                                            PlanarEmbeddedSubspace {
                                                XyzArrows(
                                                    modifier =
                                                        SubspaceModifier.scale(0.05f)
                                                            .offset(
                                                                x = (-10).dp,
                                                                y = (-10).dp,
                                                                z = (-100).dp,
                                                            )
                                                )
                                                SpatialPanel(SubspaceModifier.fillMaxSize()) {
                                                    Box(
                                                        Modifier.padding(10.dp)
                                                            .fillMaxSize()
                                                            .border(
                                                                1.dp,
                                                                Color.Blue,
                                                                RoundedCornerShape(5.dp),
                                                            )
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "==== $i ====",
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.wrapContentSize(),
                                            )
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

@SubspaceComposable
@Composable
fun XyzArrows(modifier: SubspaceModifier = SubspaceModifier) {
    val session = LocalSession.current ?: return
    var gltfModel by remember { mutableStateOf<GltfModel?>(null) }

    if (gltfModel == null) {
        LaunchedEffect(Unit) {
            gltfModel = GltfModel.create(session, Paths.get("models", "xyzArrows.glb"))
        }
    } else {
        SceneCoreEntity(
            factory = { GltfModelEntity.create(session, gltfModel!!) },
            modifier =
                modifier.rotate(
                    Quaternion.fromAxisAngle(Vector3(x = 1f), 45f) +
                        Quaternion.fromAxisAngle(Vector3(y = 1f), -45f)
                ),
        )
    }
}
