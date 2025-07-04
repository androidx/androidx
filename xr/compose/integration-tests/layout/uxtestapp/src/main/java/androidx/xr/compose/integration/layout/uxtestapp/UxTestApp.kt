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

package androidx.xr.compose.integration.layout.uxtestapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.xr.compose.spatial.SpatialElevationLevel
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import kotlin.math.roundToInt

class UxTestApp : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.width(2000.dp).height(400.dp).movable()) {
                    Row(
                        Modifier.background(Color.DarkGray),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TestPanel(SpatialElevationLevel.Level0, "Level0")
                        Spacer(Modifier.size(20.dp))
                        TestPanel(SpatialElevationLevel.Level1, "Level1")
                        Spacer(Modifier.size(20.dp))
                        TestPanel(SpatialElevationLevel.Level2, "Level2")
                        Spacer(Modifier.size(20.dp))
                        TestPanel(SpatialElevationLevel.Level3, "Level3")
                        Spacer(Modifier.size(20.dp))
                        TestPanel(SpatialElevationLevel.Level4, "Level4")
                        Spacer(Modifier.size(20.dp))
                        TestPanel(SpatialElevationLevel.Level5, "Level5")
                    }
                }
            }
        }

        isDebugInspectorInfoEnabled = true
    }
}

@Composable
fun TestPanel(elevationLevel: Dp, levelName: String) {
    var elevation by remember(elevationLevel) { mutableStateOf(elevationLevel) }
    Column(Modifier.width(200.dp)) {
        Box(Modifier.background(Color.White)) {
            Text("Initial Level: $levelName at $elevationLevel", Modifier.padding(10.dp))
        }

        Subspace {
            SpatialPanel(SubspaceModifier.offset(z = elevation)) {
                Box(
                    Modifier.size(200.dp).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${elevation.value.roundToInt().dp}", fontSize = 8.em)
                }
            }
        }

        Spacer(Modifier.size(20.dp))

        with(LocalDensity.current) {
            Slider(
                value = elevation.toPx(),
                onValueChange = { elevation = it.toDp() },
                valueRange = 0f..200.dp.toPx(),
            )
        }
    }
}
