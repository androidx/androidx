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

package androidx.xr.compose.testapp.spatialcompose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.LocalSpatialConfiguration
import androidx.xr.compose.spatial.ApplicationSubspace
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.components.TestDialog
import kotlinx.coroutines.delay

private const val MAIN_PANEL_EXTRA: String = "main panel"
private const val INITIAL_LAUNCH_EXTRA: String = "launch"

/** Test app for managing state. */
class SpatialComposeStateTest : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val useMainPanel = intent.getIntExtra(MAIN_PANEL_EXTRA, 0) == 0
        val isInitialLaunch = intent.getIntExtra(INITIAL_LAUNCH_EXTRA, 1) == 1

        setContent {
            if (useMainPanel) {
                MainPanelContent("Main Panel", isInitialLaunch = isInitialLaunch)
            }

            ApplicationSubspace {
                SpatialRow {
                    if (useMainPanel) {
                        SpatialMainPanel(modifier = SubspaceModifier.size(600.dp).movable())
                    } else {
                        SpatialPanel(modifier = SubspaceModifier.movable()) {
                            MainPanelContent("Spatial Panel")
                        }
                    }
                    SpatialPanel(modifier = SubspaceModifier.size(200.dp)) {
                        Surface {
                            CommonTestScaffold(
                                title = "Right\nSpatial Panel",
                                showBottomBar = false,
                            ) { padding ->
                                Column(
                                    modifier = Modifier.padding(padding),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Counter("")
                                    CounterOrbiter()
                                }
                            }
                        }
                    }
                }
            }

            ApplicationSubspace {
                SpatialPanel(
                    modifier = SubspaceModifier.size(200.dp).offset(x = 500.dp).movable()
                ) {
                    Surface {
                        CommonTestScaffold(title = "Second\nSubspace", showBottomBar = false) {
                            padding ->
                            Column(modifier = Modifier.padding(padding)) {
                                Counter("")
                                CounterOrbiter()
                            }
                        }
                    }
                }
            }
        }

        isDebugInspectorInfoEnabled = true
    }

    @Composable
    fun MainPanelContent(name: String, isInitialLaunch: Boolean = false) {
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        Surface {
            CommonTestScaffold(
                title = "Application State test",
                showBottomBar = true,
                onClickBackArrow = { dispatcher?.onBackPressed() },
            ) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isInitialLaunch) {
                        Text("This is the initial launch of the app.")
                    }
                    Counter(name)
                    CounterOrbiter()
                    Button(onClick = ::startMainPanelActivity) { Text("Launch Using Main Panel") }
                    Button(onClick = ::startSpatialPanelActivity) {
                        Text("Launch Using Spatial Panel")
                    }
                    SwitchSpaceModeButton()
                    TestDialog {
                        Counter("Dialog")
                        CounterOrbiter()
                        SwitchSpaceModeButton()
                    }
                    val dispatcher =
                        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                    Button(onClick = { dispatcher?.onBackPressed() }) {
                        Text(if (isInitialLaunch) "Exit App" else "Go Back")
                    }
                }
            }
        }
    }

    private fun startMainPanelActivity() {
        startActivity(
            Intent(this, SpatialComposeStateTest::class.java)
                .putExtra(MAIN_PANEL_EXTRA, 0)
                .putExtra(INITIAL_LAUNCH_EXTRA, 0)
        )
    }

    private fun startSpatialPanelActivity() {
        startActivity(
            Intent(this, SpatialComposeStateTest::class.java)
                .putExtra(MAIN_PANEL_EXTRA, 1)
                .putExtra(INITIAL_LAUNCH_EXTRA, 0)
        )
    }
}

@Composable
fun CounterOrbiter() {
    Orbiter(
        position = ContentEdge.Bottom,
        offset = 8.dp,
        offsetType = OrbiterOffsetType.InnerEdge,
        shape = SpatialRoundedCornerShape(CornerSize(percent = 50)),
    ) {
        Surface {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Counter("Orbiter") }
        }
    }
}

@Composable
fun Counter(name: String, modifier: Modifier = Modifier) {
    var count by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            count++
            delay(1000)
        }
    }

    Text("$name Counter: $count", modifier = modifier.padding(16.dp))
}

@Composable
fun SwitchSpaceModeButton() {
    val config = LocalSpatialConfiguration.current
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        Button(onClick = config::requestHomeSpaceMode) { Text("Request Home Space Mode") }
    } else {
        Button(onClick = config::requestFullSpaceMode) { Text("Request Full Space Mode") }
    }
}
