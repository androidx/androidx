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

package androidx.xr.compose.integration.layout.animationexplorationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterEdge
import androidx.xr.compose.spatial.SpatialElevation
import androidx.xr.compose.spatial.SpatialElevationLevel
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.alpha
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.scale
import androidx.xr.compose.subspace.layout.width
import kotlinx.coroutines.launch

class AnimationExplorationApp : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val (showSidePanel, updateShowSidePanel) = remember { mutableStateOf(false) }
            val toggleSidePanel: () -> Unit = { updateShowSidePanel(!showSidePanel) }
            val desiredWidth = 300.dp
            val desiredHeight = 150.dp

            // Main Panel content.
            Box(
                modifier =
                    Modifier.background(Color.LightGray)
                        .fillMaxSize()
                        .border(width = 3.dp, color = Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Main Panel content")
            }

            // 3D content.
            Subspace {
                SpatialRow {
                    val animatedAlpha = remember { Animatable(0.5f) }
                    val mainPanelAnimatedScale = remember { Animatable(1.0f) }

                    LaunchedEffect(Unit) {
                        launch { animatedAlpha.animateTo(1.0f, animationSpec = tween(2000)) }
                    }

                    MainPanel(
                        modifier =
                            SubspaceModifier.width(600.dp)
                                .height(400.dp)
                                .alpha(animatedAlpha.value)
                                .scale(mainPanelAnimatedScale.value)
                    )

                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(desiredWidth)
                                .height(desiredHeight)
                                .alpha(animatedAlpha.value),
                        name = "FadeInPanel",
                    ) {
                        PanelContent(
                            "Faded in content",
                            "Show side Panel",
                            !showSidePanel,
                            toggleSidePanel
                        )
                    }

                    if (showSidePanel) {
                        val sidePanelAnimatedScale = remember { Animatable(0.01f) }

                        LaunchedEffect(Unit) {
                            mainPanelAnimatedScale.animateTo(0.01f, animationSpec = tween(10))
                            mainPanelAnimatedScale.animateTo(2.0f, animationSpec = tween(2000))
                            mainPanelAnimatedScale.animateTo(1.0f, animationSpec = tween(2000))
                        }

                        LaunchedEffect(Unit) {
                            sidePanelAnimatedScale.animateTo(2.0f, animationSpec = tween(2000))
                            sidePanelAnimatedScale.animateTo(1.0f, animationSpec = tween(2000))
                        }

                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(desiredWidth)
                                    .height(desiredHeight)
                                    .scale(sidePanelAnimatedScale.value)
                        ) {
                            PanelContent(
                                "Grown content",
                                "Hide side panel",
                                showSidePanel,
                                toggleSidePanel
                            )
                        }
                    }
                }
            }
        }
    }

    @UiComposable
    @Composable
    fun PanelContent(
        text: String,
        buttonText: String,
        showButton: Boolean,
        buttonOnClick: () -> Unit,
    ) {
        Box(
            modifier =
                Modifier.background(Color.LightGray)
                    .fillMaxSize()
                    .border(width = 3.dp, color = Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                Orbiter(position = OrbiterEdge.Top, offset = 5.dp) {
                    Text(
                        text = text,
                        fontSize = 20.sp,
                        modifier =
                            Modifier.background(Color.LightGray)
                                .border(width = 1.dp, color = Color.Black),
                    )
                }
                if (showButton) {
                    SpatialElevation(spatialElevationLevel = SpatialElevationLevel.Level3) {
                        Button(onClick = buttonOnClick) { Text(text = buttonText) }
                    }
                }
            }
        }
    }
}
