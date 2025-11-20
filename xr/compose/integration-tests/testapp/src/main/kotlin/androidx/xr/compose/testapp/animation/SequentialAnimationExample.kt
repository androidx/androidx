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

package androidx.xr.compose.testapp.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.SpatialElevationLevel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/*
 * Advanced animation example - Sequential example
 * Chat boxes animating elevation and size
 *
 */
@Composable
@SubspaceComposable
fun SequentialAnimationExample(modifier: SubspaceModifier = SubspaceModifier) {
    val coroutineScope = rememberCoroutineScope()
    val animationDuration = 1000
    val initialBox1Elevation = SpatialElevationLevel.Level1
    val initialBox2Elevation = SpatialElevationLevel.Level0
    val targetBox1Elevation = SpatialElevationLevel.Level4
    val targetBox2Elevation = SpatialElevationLevel.Level5
    val initialBox1Size = 100.dp
    val initialBox2Size = 100.dp
    val targetBox1Size = 200.dp
    val targetBox2Size = 200.dp

    val box1Elevation = remember { Animatable(initialBox1Elevation, Dp.VectorConverter) }
    val box2Elevation = remember { Animatable(initialBox2Elevation, Dp.VectorConverter) }
    val box1Size = remember { Animatable(initialBox1Size, Dp.VectorConverter) }
    val box2Size = remember { Animatable(initialBox2Size, Dp.VectorConverter) }

    SpatialPanel {
        Button(
            onClick = {
                coroutineScope.launch {
                    box1Elevation.snapTo(initialBox1Elevation)
                    box2Elevation.snapTo(initialBox2Elevation)
                    box1Size.snapTo(initialBox1Size)
                    box2Size.snapTo(initialBox2Size)

                    val job1 = async {
                        box1Elevation.animateTo(
                            targetBox1Elevation,
                            animationSpec = tween(animationDuration),
                        )
                    }
                    val job2 = async {
                        box1Size.animateTo(targetBox1Size, animationSpec = tween(animationDuration))
                    }
                    job1.await()
                    job2.await()

                    launch {
                        box1Elevation.animateTo(
                            initialBox1Elevation,
                            animationSpec = tween(animationDuration),
                        )
                    }
                    launch {
                        box1Size.animateTo(
                            initialBox1Size,
                            animationSpec = tween(animationDuration),
                        )
                    }
                    launch {
                        box2Elevation.animateTo(
                            targetBox2Elevation,
                            animationSpec = tween(animationDuration),
                        )
                    }
                    launch {
                        box2Size.animateTo(targetBox2Size, animationSpec = tween(animationDuration))
                    }
                }
            }
        ) {
            Text("Play animation")
        }
    }
    SpatialRow(modifier = modifier) {
        Orbiter(position = ContentEdge.Start, offset = 50.dp, elevation = box1Elevation.value) {
            Surface(modifier = Modifier.size(box1Size.value).background(Color.DarkGray)) {
                PanelContent(text = "Orbiter 1")
            }
        }
        Orbiter(position = ContentEdge.End, offset = 100.dp, elevation = box2Elevation.value) {
            Surface(modifier = Modifier.size(box2Size.value).background(Color.DarkGray)) {
                PanelContent(text = "Orbiter 2")
            }
        }
        SpatialPanel {
            Box(
                modifier = Modifier.background(Color.DarkGray).size(400.dp).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Middle panel")
            }
        }
    }
}

@UiComposable
@Composable
fun PanelContent(text: String) {
    Box(
        modifier = Modifier.background(Color.LightGray).fillMaxSize().padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text)
    }
}
