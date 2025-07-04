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

package androidx.xr.compose.testapp.modechange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.SpatialElevation
import androidx.xr.compose.spatial.SpatialElevationLevel
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.components.CommonTestPanel
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple40
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.scenecore.scene

class ModeChange : ComponentActivity() {
    private var renderingSession: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IntegrationTestsAppTheme {
                if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                    FullSpaceMainPanel()
                } else {
                    HomeSpaceMainPanel()
                }
            }
        }
    }

    @Composable
    private fun FullSpaceMainPanel() {
        renderingSession = LocalSession.current
        Subspace {
            SpatialRow {
                CommonTestPanel(
                    size = DpVolumeSize(320.dp, 240.dp, 0.dp),
                    title = "Left Panel",
                    showBottomBar = false,
                    onClickBackArrow = null,
                ) {}

                // Middle panel
                CommonTestPanel(
                    size = DpVolumeSize(640.dp, 480.dp, 0.dp),
                    title = getString(R.string.mode_change_test),
                    showBottomBar = true,
                    onClickBackArrow = { this@ModeChange.finish() },
                    onClickRecreate = { this@ModeChange.recreate() },
                ) { padding ->
                    PanelContent(padding, "FullSpace Mode", "Transition to HomeSpace Mode") {
                        renderingSession!!.scene.requestHomeSpaceMode()
                    }
                }

                // Right panel
                CommonTestPanel(
                    size = DpVolumeSize(320.dp, 240.dp, 0.dp),
                    title = "Right Panel",
                    showBottomBar = false,
                    onClickBackArrow = null,
                ) {}
            }
        }
    }

    @Composable
    private fun HomeSpaceMainPanel() {
        CommonTestScaffold(
            title = getString(R.string.mode_change_test),
            showBottomBar = true,
            onClickBackArrow = { this@ModeChange.finish() },
            onClickRecreate = { this@ModeChange.recreate() },
        ) { padding ->
            PanelContent(padding, "HomeSpace Mode", "Transition to FullSpace Mode") {
                renderingSession!!.scene.requestFullSpaceMode()
            }
        }
    }

    @Composable
    private fun PanelContent(
        paddingValues: PaddingValues,
        orbiterText: String,
        buttonText: String,
        buttonOnClick: () -> Unit,
    ) {
        Box(
            modifier = Modifier.background(PurpleGrey80).fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                Orbiter(position = ContentEdge.Top, offset = 5.dp) {
                    Text(
                        text = orbiterText,
                        fontSize = 20.sp,
                        color = Color.Black,
                        modifier = Modifier.background(PurpleGrey80),
                    )
                }

                SpatialElevation(elevation = SpatialElevationLevel.Level5) {
                    Button(
                        onClick = buttonOnClick,
                        colors =
                            ButtonColors(
                                containerColor = Purple40,
                                contentColor = Color.White,
                                disabledContainerColor = Color.LightGray,
                                disabledContentColor = Color.Gray,
                            ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(text = buttonText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
