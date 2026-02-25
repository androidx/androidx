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

package androidx.xr.compose.testapp.rotatetolookatuser

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
import androidx.compose.foundation.layout.padding
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
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.gravityAligned
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.rotateToLookAtUser
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.TopBarWithBackArrow
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple40
import androidx.xr.compose.testapp.ui.theme.PurpleGrey40
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3

/**
 * Integration test activity for the [rotateToLookAtUser] modifier.
 *
 * This activity provides a visual test bed to validate how spatial entities track the user's head
 * pose across different container types and modifier combinations.
 *
 * Test Scenarios
 * 1. Standard rotateToLookAtUser: Validates full 3D orientation tracking (pitch, yaw, and roll)
 *    applied to a [SpatialPanel], [SpatialRow], [SpatialColumn], and [SpatialExternalSurface].
 * 2. Nested Hierarchies: Validates that the tracking logic correctly handles coordinate
 *    transformations when the child tracks the user inside a rotated [SpatialBox].
 * 3. Custom Up Vector: Validates tracking behavior when a specific 'up' orientation is provided,
 *    useful for tilted or non-standard tracking requirements.
 * 4. Billboard: Demonstrates and validates the "Billboard" effect, achieved by chaining
 *    [rotateToLookAtUser] with [gravityAligned]. This should result in horizontal-only tracking
 *    while the panel remains vertically upright.
 *
 * Usage
 * - Use the global switch in the top control panel to toggle the tracking behavior for all test
 *   cases simultaneously.
 * - Move the headset or camera around the spatial environment to verify that all panels actively
 *   rotate to maintain a front-facing orientation toward the user.
 */
class RotateToLookAtUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainContent() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SubspaceComposable
    @Composable
    private fun MainContent() {
        val session = checkNotNull(LocalSession.current) { "session must be initialized" }
        session.configure(
            config = session.config.copy(deviceTracking = DeviceTrackingMode.LAST_KNOWN)
        )

        var isRotateToLookAtUserOn by remember { mutableStateOf(false) }

        IntegrationTestsAppTheme {
            Subspace(modifier = SubspaceModifier.width(1600.dp).height(1400.dp)) {
                SpatialRow(
                    modifier = SubspaceModifier.offset(y = 100.dp),
                    horizontalArrangement = SpatialArrangement.spacedBy(40.dp),
                ) {
                    SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(20.dp)) {
                        ControlPanel(
                            isRotateToLookAtUserOn = isRotateToLookAtUserOn,
                            onToggle = { isRotateToLookAtUserOn = it },
                        )
                        TestGrid(isFeatureOn = isRotateToLookAtUserOn)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SubspaceComposable
    @Composable
    private fun ControlPanel(isRotateToLookAtUserOn: Boolean, onToggle: (Boolean) -> Unit) {
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
                        title = "RotateToLookAtUser Modifier Test",
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
                        "rotateToLookAtUser()",
                        color = PurpleGrey40,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 20.dp),
                    )
                    Switch(checked = isRotateToLookAtUserOn, onCheckedChange = onToggle)
                }
            }
        }
    }

    /**
     * A reusable container that encapsulates a spatial composable and applies the
     * rotateToLookAtUser modifier based on the global state.
     */
    @SubspaceComposable
    @Composable
    private fun TestPanelContainer(
        title: String,
        isFeatureOn: Boolean,
        upVector: Vector3? = null,
        width: Int = 400,
        height: Int = 200,
        container:
            @Composable
            @SubspaceComposable
            (SubspaceModifier, @Composable () -> Unit) -> Unit,
    ) {
        var finalModifier = SubspaceModifier.width(width.dp).height(height.dp)
        if (isFeatureOn) {
            finalModifier =
                when {
                    upVector != null -> finalModifier.rotateToLookAtUser(upDirection = upVector)
                    else -> finalModifier.rotateToLookAtUser()
                }
        }

        val innerContent: @Composable () -> Unit = {
            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(if (isFeatureOn) Purple40 else PurpleGrey40)
                        .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        container(finalModifier, innerContent)
    }

    @SubspaceComposable
    @Composable
    private fun TestGrid(isFeatureOn: Boolean) {
        SpatialRow(horizontalArrangement = SpatialArrangement.spacedBy(20.dp)) {
            // Column 1: Standard Composables
            SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(20.dp)) {
                TestPanelContainer(title = "SpatialPanel", isFeatureOn = isFeatureOn) {
                    modifier,
                    content ->
                    SpatialPanel(modifier = modifier, content = content)
                }

                TestPanelContainer(title = "SpatialExternalSurface", isFeatureOn = isFeatureOn) {
                    modifier,
                    content ->
                    SpatialExternalSurface(modifier = modifier, stereoMode = StereoMode.Mono) {
                        SpatialPanel(modifier = SubspaceModifier.fillMaxSize(), content = content)
                    }
                }

                TestPanelContainer(title = "SpatialRow", isFeatureOn = isFeatureOn) {
                    modifier,
                    content ->
                    SpatialRow(modifier = modifier) {
                        SpatialPanel(modifier = SubspaceModifier.fillMaxSize(), content = content)
                    }
                }

                TestPanelContainer(title = "SpatialColumn", isFeatureOn = isFeatureOn) {
                    modifier,
                    content ->
                    SpatialColumn(modifier = modifier) {
                        SpatialPanel(modifier = SubspaceModifier.fillMaxSize(), content = content)
                    }
                }
            }

            // Column 2: Advanced Configurations
            SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(60.dp)) {
                // Nested rotation test: Demonstrates a tracking child within a fixed rotated parent
                val parentRotation = Quaternion.fromEulerAngles(pitch = 0f, yaw = 0f, roll = 10f)
                SpatialBox(
                    modifier = SubspaceModifier.width(400.dp).height(200.dp).rotate(parentRotation)
                ) {
                    SpatialPanel(modifier = SubspaceModifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize().background(PurpleGrey40)) {
                            Text(
                                "PARENT (FIXED ROTATION)",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp).align(Alignment.TopStart),
                            )
                        }
                    }

                    TestPanelContainer(
                        title = "CHILD (TRACKING)",
                        isFeatureOn = isFeatureOn,
                        width = 300,
                        height = 100,
                    ) { modifier, content ->
                        // Offset by 5dp on Z axis to prevent clipping with parent panel
                        SpatialPanel(modifier = modifier.offset(z = 5.dp), content = content)
                    }
                }

                // Custom up vector: Tracking with a specific 'up' orientation
                TestPanelContainer(
                    title = "Custom Up Vector (1, 0, 0)",
                    isFeatureOn = isFeatureOn,
                    upVector = Vector3(1f, 0f, 0f),
                    width = 300,
                ) { modifier, content ->
                    SpatialPanel(modifier = modifier, content = content)
                }

                // Billboard: Horizontal tracking only (upright)
                TestPanelContainer(
                    title = "Billboard (Look + Gravity)",
                    isFeatureOn = isFeatureOn,
                ) { modifier, content ->
                    SpatialPanel(modifier = modifier.gravityAligned(), content = content)
                }
            }
        }
    }
}
