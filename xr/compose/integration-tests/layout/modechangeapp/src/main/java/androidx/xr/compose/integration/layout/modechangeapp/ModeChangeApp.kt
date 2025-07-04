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

package androidx.xr.compose.integration.layout.modechangeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.SpatialElevation
import androidx.xr.compose.spatial.SpatialElevationLevel
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.scene

class ModeChangeApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                FullSpaceModeContent()
            } else {
                HomeSpaceModeContent()
            }
        }
    }

    @Composable
    private fun FullSpaceModeContent() {
        Subspace {
            SpatialRow {
                SpatialPanel(modifier = SubspaceModifier.width(300.dp).height(300.dp)) {
                    PanelContent("Left Panel", "Unused", false) {}
                }
                SpatialPanel(modifier = SubspaceModifier.width(600.dp).height(400.dp)) {
                    PanelContent("FullSpace Mode", "Transition to HomeSpace Mode", true) {
                        (Session.create(this@ModeChangeApp) as SessionCreateSuccess)
                            .session
                            .scene
                            .requestHomeSpaceMode()
                    }
                }
                SpatialPanel(modifier = SubspaceModifier.width(300.dp).height(300.dp)) {
                    PanelContent("Right Panel", "Unused", false) {}
                }
            }
        }
    }

    @Composable
    private fun HomeSpaceModeContent() {
        PanelContent("HomeSpace Mode", "Transition to FullSpace Mode", true) {
            (Session.create(this@ModeChangeApp) as SessionCreateSuccess)
                .session
                .scene
                .requestFullSpaceMode()
        }
    }

    @UiComposable
    @Composable
    private fun PanelContent(
        orbiterText: String,
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
                Orbiter(position = ContentEdge.Top, offset = 5.dp) {
                    Text(
                        text = orbiterText,
                        fontSize = 20.sp,
                        modifier =
                            Modifier.background(Color.LightGray)
                                .border(width = 1.dp, color = Color.Black),
                    )
                }
                if (showButton) {
                    SpatialElevation(elevation = SpatialElevationLevel.Level3) {
                        Button(onClick = buttonOnClick) { Text(text = buttonText) }
                    }
                }
            }
        }
    }
}
