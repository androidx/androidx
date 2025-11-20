/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.compose.testapp.lookatuser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.billboard
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.lookAtUser
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.TopBarWithBackArrow
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple40
import androidx.xr.compose.testapp.ui.theme.PurpleGrey40
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import androidx.xr.runtime.Config

/**
 * Test Activity for the [billboard] modifier. This activity demonstrates the effect of applying
 * [billboard] to various spatial containers, including [SpatialPanel], [SpatialRow], and
 * [SpatialColumn]. When enabled, the modifier ensures the spatial composable's content always
 * rotates to face the user's current head position, regardless of the user's movement or the
 * composable's initial placement.
 */
class LookAtUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainContent() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SubspaceComposable
    @Composable
    private fun MainContent() {
        val session = checkNotNull(LocalSession.current) { "session must be initialized" }
        // Ensure head tracking is enabled
        session.configure(
            config = session.config.copy(headTracking = Config.HeadTrackingMode.LAST_KNOWN)
        )

        // Global state for billboard toggling
        var isBillboardOn by remember { mutableStateOf(false) }
        // Global state for lookAtUser toggling
        var isLookAtUserOn by remember { mutableStateOf(false) }

        IntegrationTestsAppTheme {
            Subspace(modifier = SubspaceModifier.width(1200.dp).height(1400.dp)) {
                SpatialRow() {
                    SpatialColumn(
                        verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                        // Offset the entire column slightly away from the user for better viewing
                        modifier = SubspaceModifier.offset(y = 100.dp, z = (-100).dp),
                    ) {
                        ControlPanel(
                            feature = "Billboard",
                            isFeatureOn = isBillboardOn,
                            onToggle = { isBillboardOn = it },
                        )
                        TestGrid(feature = "Billboard", isFeatureOn = isBillboardOn)
                    }
                    SpatialColumn(
                        verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                        // Offset the entire column slightly away from the user for better viewing
                        modifier = SubspaceModifier.offset(y = 100.dp, z = (-100).dp),
                    ) {
                        ControlPanel(
                            feature = "LookAtUser",
                            isFeatureOn = isLookAtUserOn,
                            onToggle = { isLookAtUserOn = it },
                        )
                        TestGrid(feature = "LookAtUser", isFeatureOn = isLookAtUserOn)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SubspaceComposable
    @Composable
    private fun ControlPanel(feature: String, isFeatureOn: Boolean, onToggle: (Boolean) -> Unit) {
        // SpatialPanel container for the controls
        SpatialPanel(modifier = SubspaceModifier.width(550.dp).height(200.dp).padding(25.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().background(PurpleGrey80),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // TopBar
                Row(modifier = Modifier.fillMaxWidth()) {
                    TopBarWithBackArrow(
                        scrollBehavior = null,
                        title = "$feature Modifier Test",
                        onClick = { finish() },
                    )
                }

                // Control Row (Toggle)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Enable $feature Modifier:",
                        color = PurpleGrey40,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 20.dp),
                    )
                    Switch(checked = isFeatureOn, onCheckedChange = onToggle)
                }
            }
        }
    }

    /**
     * A reusable container that encapsulates a spatial composable and applies the billboard
     * modifier based on the global state.
     */
    @SubspaceComposable
    @Composable
    private fun TestPanelContainer(
        title: String,
        feature: String,
        isFeatureOn: Boolean,
        extraContent: (@Composable () -> Unit)? = null,
        container:
            @Composable
            @SubspaceComposable
            (SubspaceModifier, @Composable () -> Unit) -> Unit,
    ) {
        var finalModifier = SubspaceModifier.width(400.dp).height(200.dp)
        if (feature == "Billboard") finalModifier = finalModifier.billboard(enabled = isFeatureOn)
        if (feature == "LookAtUser") finalModifier = finalModifier.lookAtUser(enabled = isFeatureOn)

        // The inner content function (passed to the container)
        val innerContent: @Composable () -> Unit = {
            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(if (isFeatureOn) Purple40 else PurpleGrey40)
                        .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Use Column to stack the title and the optional content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    // Conditionally display the optional content
                    extraContent?.invoke()
                }
            }
        }

        container(finalModifier, innerContent)
    }

    @SubspaceComposable
    @Composable
    private fun TestGrid(feature: String, isFeatureOn: Boolean) {
        SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(20.dp)) {
            // SpatialPanel
            TestPanelContainer(
                title = "SpatialPanel",
                feature = feature,
                isFeatureOn = isFeatureOn,
            ) { modifier, content ->
                SpatialPanel(modifier = modifier, content = content)
            }

            // SpatialRow
            TestPanelContainer(
                title = "SpatialRow",
                feature = feature,
                isFeatureOn = isFeatureOn,
                extraContent = {
                    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
                    Row(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StaticBox("1")
                        StaticBox("2")
                    }
                },
            ) { modifier, content ->
                SpatialRow(modifier = modifier) {
                    SpatialPanel(modifier = SubspaceModifier.fillMaxSize(), content = content)
                }
            }

            // SpatialColumn
            TestPanelContainer(
                title = "SpatialColumn",
                feature = feature,
                isFeatureOn = isFeatureOn,
                extraContent = {
                    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StaticBox("1")
                        StaticBox("2")
                    }
                },
            ) { modifier, content ->
                SpatialColumn(modifier = modifier) {
                    SpatialPanel(modifier = SubspaceModifier.fillMaxSize(), content = content)
                }
            }

            // SpatialExternalSurface
            TestPanelContainer(
                title = "SpatialExternalSurface",
                feature = feature,
                isFeatureOn = isFeatureOn,
            ) { modifier, content ->
                SpatialExternalSurface(modifier = modifier, stereoMode = StereoMode.Mono) {
                    SpatialPanel(modifier = SubspaceModifier.fillMaxSize(), content = content)
                }
            }
        }
    }

    @Composable
    private fun StaticBox(title: String) {
        Box(
            modifier = Modifier.width(100.dp).height(50.dp).background(PurpleGrey80),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = PurpleGrey40,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
