/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.testapp.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.requestFullSpace
import androidx.xr.compose.platform.requestHomeSpace
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import kotlinx.coroutines.launch

/** Test activity demonstrating the use of Jetpack Navigation in Compose XR. */
class SpatialNavigationActivity : ComponentActivity() {

    object Destinations {
        const val HOME_2D = "home_2d"
        const val SPATIAL_3D_WITH_MAIN = "spatial_3d_with_main"
        const val SPATIAL_3D_WITHOUT_MAIN = "spatial_3d_without_main"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IntegrationTestsAppTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Destinations.HOME_2D,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) {
                    composable(Destinations.HOME_2D) { Home2DScreen(navController = navController) }

                    composable(Destinations.SPATIAL_3D_WITH_MAIN) {
                        Spatial3DScreen(navController = navController, showMainPanel = true)
                    }

                    composable(Destinations.SPATIAL_3D_WITHOUT_MAIN) {
                        Spatial3DScreen(navController = navController, showMainPanel = false)
                    }
                }
            }
        }

        isDebugInspectorInfoEnabled = true
    }

    @Composable
    private fun Home2DScreen(navController: NavController) {
        val scope = rememberCoroutineScope()
        val isSpatialUiEnabled = LocalSpatialCapabilities.current.isSpatialUiEnabled

        CommonTestScaffold(
            title = "Navigation: 2D Screen (No Subspace)",
            showBottomBar = true,
            bottomBarText = "Active: 2D Destination",
            onClickBackArrow = { finish() },
            onClickRecreate = { recreate() },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Card(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "2D Content Destination",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        )
                        Text(
                            "This screen contains only 2D content rendered in the MainPanel " +
                                "without any active Subspace. Select an option below to test " +
                                "navigating into a 3D Subspace layout with or without a " +
                                "SpatialMainPanel.",
                            fontSize = 16.sp,
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            if (isSpatialUiEnabled) {
                                requestHomeSpace()
                            } else {
                                requestFullSpace()
                            }
                        }
                    }
                ) {
                    Text(if (isSpatialUiEnabled) "Request Home Space" else "Request Full Space")
                }

                Button(onClick = { navController.navigate(Destinations.SPATIAL_3D_WITH_MAIN) }) {
                    Text("3D Layout WITH Main Panel →")
                }

                Button(onClick = { navController.navigate(Destinations.SPATIAL_3D_WITHOUT_MAIN) }) {
                    Text("3D Layout WITHOUT Main Panel →")
                }
            }
        }
    }

    @Composable
    private fun Spatial3DScreen(navController: NavController, showMainPanel: Boolean) {
        val scope = rememberCoroutineScope()
        val isSpatialUiEnabled = LocalSpatialCapabilities.current.isSpatialUiEnabled
        var showNestedSubspace by remember { mutableStateOf(false) }

        // 2D MainPanel Content (displayed within SpatialMainPanel when showMainPanel is true)
        CommonTestScaffold(
            title =
                if (showMainPanel) "Navigation: 3D Screen (With Main Panel)"
                else "Navigation: 3D Screen (Without Main Panel)",
            showBottomBar = true,
            bottomBarText =
                if (showMainPanel) "Active: 3D Destination (Main Panel Visible)"
                else "Active: 3D Destination (Main Panel Hidden)",
            onClickBackArrow = { navController.popBackStack() },
            onClickRecreate = { recreate() },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Text("Spatial Main Panel", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Center panel in a SpatialRow with left and right SpatialPanels.",
                    fontSize = 16.sp,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (isSpatialUiEnabled) {
                                    requestHomeSpace()
                                } else {
                                    requestFullSpace()
                                }
                            }
                        }
                    ) {
                        Text(if (isSpatialUiEnabled) "Home Space" else "Full Space")
                    }

                    Button(onClick = { showNestedSubspace = !showNestedSubspace }) {
                        Text(
                            if (showNestedSubspace) "Hide Nested Subspace"
                            else "Create Nested Subspace"
                        )
                    }

                    Button(onClick = { navController.popBackStack() }) { Text("← Back to 2D") }
                }
            }
        }

        // 3D Subspace Content: SpatialRow with dynamic sizing via weights and fillMaxHeight
        Subspace {
            SpatialRow(
                modifier =
                    SubspaceModifier.width(if (showMainPanel) 1300.dp else 800.dp)
                        .height(700.dp)
                        .movable()
                        .resizable(),
                horizontalArrangement = SpatialArrangement.spacedBy(24.dp),
                verticalAlignment = SpatialAlignment.CenterVertically,
            ) {
                // Left Panel (weighted width, fills row height)
                SpatialPanel(modifier = SubspaceModifier.weight(1f).fillMaxHeight()) {
                    Column(
                        modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        Text("Left SpatialPanel", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { navController.popBackStack() }) { Text("← Back to 2D") }
                    }
                }

                // Center SpatialMainPanel (only rendered when showMainPanel is true)
                if (showMainPanel) {
                    SpatialMainPanel(modifier = SubspaceModifier.weight(2f).fillMaxHeight())
                }

                // Right Panel (weighted width, fills row height)
                SpatialPanel(modifier = SubspaceModifier.weight(1f).fillMaxHeight()) {
                    Column(
                        modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        Text("Right SpatialPanel", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { showNestedSubspace = !showNestedSubspace }) {
                            Text(
                                if (showNestedSubspace) "Hide Nested Subspace"
                                else "Create Nested Subspace"
                            )
                        }
                        if (!showMainPanel) {
                            Button(onClick = { navController.popBackStack() }) {
                                Text("← Back to 2D")
                            }
                        }
                    }
                }
            }
        }

        // Nested Subspace with a SpatialPanel
        if (showNestedSubspace) {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(400.dp)
                            .height(300.dp)
                            .offset(y = 500.dp, z = 150.dp)
                            .movable()
                            .resizable()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        Text(
                            "Nested Subspace Panel",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("Rendered inside a nested Subspace", fontSize = 14.sp)
                        Button(onClick = { showNestedSubspace = false }) {
                            Text("Close Nested Subspace")
                        }
                    }
                }
            }
        }
    }
}
