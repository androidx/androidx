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

package androidx.xr.compose.testapp.gravityaligned

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.gravityAligned
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.runtime.math.Quaternion

/**
 * Test Activity for the [gravityAligned] modifier. This app displays 5 different test cases in
 * vertical columns to validate the modifier's behavior under various rotation and hierarchy
 * scenarios.
 */
class GravityAlignedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IntegrationTestsAppTheme {
                Subspace(
                    modifier = SubspaceModifier.width(1600.dp).height(2200.dp).testTag("Subspace")
                ) {
                    // Main layout: a Row containing two Columns
                    SpatialRow(
                        modifier = SubspaceModifier.testTag("TestBedRoot"),
                        horizontalArrangement = SpatialArrangement.spacedBy(24.dp),
                    ) {
                        // Left Column (Cases 1 & 2)
                        SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(50.dp)) {
                            // Case 1: No nested rotation, no chained rotate modifiers
                            TestCaseRow(
                                title = "Case 1:\nLevel Row + Panel in Single Rotate Box",
                                rowRotation = Quaternion.Identity,
                            ) {
                                GravityAlignedTestPanel()
                            }

                            // Case 2: No nested rotation, chained rotate modifiers
                            TestCaseRow(
                                title = "Case 2:\nLevel Row + Panel in Double Rotate Box",
                                rowRotation = Quaternion.Identity,
                            ) {
                                GravityAlignedDoubleRotateTestPanel()
                            }
                        }

                        // Right Column (Cases 3, 4, & 5)
                        SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(50.dp)) {
                            // Case 3: Nested rotation, no chained rotate modifiers
                            TestCaseRow(
                                title = "Case 3:\nTilted Row + Panel Single Rotate Box",
                                rowRotation = Quaternion.fromEulerAngles(20f, -20f, 0f),
                            ) {
                                GravityAlignedTestPanel()
                            }

                            // Case 4: Nested rotation, chained rotate modifiers
                            TestCaseRow(
                                title = "Case 4:\nTilted Row + Panel in Double Rotate Box",
                                rowRotation = Quaternion.fromEulerAngles(20f, -17f, -61f),
                            ) {
                                GravityAlignedDoubleRotateTestPanel()
                            }

                            // Case 5: Nested rotation, and chained rotate modifier on the child
                            // panel
                            TestCaseRow(
                                title =
                                    "Case 5:\nTilted Row + Rotated then GravityAligned Panel in Double Rotate Box",
                                rowRotation = Quaternion.fromEulerAngles(-30f, 11f, 22f),
                            ) {
                                CaseChainedGravityTestPanel()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A reusable container for a single test case row. A title panel is on the left and the test target
 * content is on right side. It applies a rotation only to the content.
 */
@SubspaceComposable
@Composable
private fun TestCaseRow(
    title: String,
    rowRotation: Quaternion,
    content: @Composable @SubspaceComposable () -> Unit,
) {
    SpatialRow(
        modifier = SubspaceModifier.width(800.dp).height(300.dp),
        horizontalArrangement = SpatialArrangement.spacedBy(24.dp),
    ) {
        // Title Panel, always level.
        SpatialPanel(modifier = SubspaceModifier.width(300.dp)) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF222233)).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                )
            }
        }

        // Test target, this is the row that gets rotated.
        SpatialRow(
            modifier = SubspaceModifier.width(426.dp).height(400.dp).rotate(rowRotation),
            horizontalArrangement = SpatialArrangement.spacedBy(24.dp),
        ) {
            content()
        }
    }
}

/**
 * A shared, stateless 2D Composable that renders the UI controls (sliders and switch). This removes
 * code duplication from the interactive test panels.
 */
@Composable
private fun GravityTestControls(
    isGravityAligned: Boolean,
    onGravityAlignedChange: (Boolean) -> Unit,
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    roll: Float,
    onRollChange: (Float) -> Unit,
    yaw: Float,
    onYawChange: (Float) -> Unit,
    title: String? = null,
) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(Color(0xFF333333)) // Dark background
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        // Title and Toggle Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (title != null) {
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
            Row( // Inner row to keep toggle and text together
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "gravityAligned()",
                    color = if (isGravityAligned) Color.Cyan else Color.White,
                    fontSize = 20.sp,
                )
                Switch(checked = isGravityAligned, onCheckedChange = onGravityAlignedChange)
            }
        }

        // Sliders for parent rotation
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Parent Pitch: ${pitch.toInt()}°", color = Color.White, fontSize = 16.sp)
            Slider(value = pitch, onValueChange = onPitchChange, valueRange = -90f..90f)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Parent Roll: ${roll.toInt()}°", color = Color.White, fontSize = 16.sp)
            Slider(value = roll, onValueChange = onRollChange, valueRange = -90f..90f)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Parent Yaw: ${yaw.toInt()}°", color = Color.White, fontSize = 16.sp)
            Slider(value = yaw, onValueChange = onYawChange, valueRange = -180f..180f)
        }
    }
}

/**
 * Interactive Test Panel: Tests [SpatialBox(rotate)] -> [SpatialPanel(gravityAligned)] (Used for
 * Cases 1 and 3)
 */
@SubspaceComposable
@Composable
private fun GravityAlignedTestPanel(modifier: SubspaceModifier = SubspaceModifier) {
    var parentPitch by remember { mutableFloatStateOf(17f) }
    var parentYaw by remember { mutableFloatStateOf(29f) }
    var parentRoll by remember { mutableFloatStateOf(39f) }
    var isGravityAligned by remember { mutableStateOf(true) }
    val parentRotation by remember {
        derivedStateOf {
            Quaternion.fromEulerAngles(pitch = parentPitch, yaw = parentYaw, roll = parentRoll)
        }
    }
    SpatialBox(
        modifier =
            SubspaceModifier.width(450.dp)
                .height(400.dp)
                .rotate(parentRotation)
                .testTag("SingleRotateBox")
    ) {
        val updatedModifier = if (isGravityAligned) modifier.gravityAligned() else modifier
        SpatialPanel(modifier = updatedModifier.testTag("TestPanel")) {
            GravityTestControls(
                isGravityAligned = isGravityAligned,
                onGravityAlignedChange = { isGravityAligned = it },
                pitch = parentPitch,
                onPitchChange = { parentPitch = it },
                roll = parentRoll,
                onRollChange = { parentRoll = it },
                yaw = parentYaw,
                onYawChange = { parentYaw = it },
            )
        }
    }
}

/**
 * Interactive Test Panel: Tests [SpatialBox(rotate.rotate)] -> [SpatialPanel(gravityAligned)] (Used
 * for Cases 2 and 4)
 */
@SubspaceComposable
@Composable
private fun GravityAlignedDoubleRotateTestPanel(modifier: SubspaceModifier = SubspaceModifier) {
    var pitch by remember { mutableFloatStateOf(30f) }
    var roll by remember { mutableFloatStateOf(30f) }
    var yaw by remember { mutableFloatStateOf(0f) }
    var isGravityAligned by remember { mutableStateOf(true) }
    // Create two separate rotation objects from the same slider state
    val outerRotation by remember {
        derivedStateOf { Quaternion.fromEulerAngles(pitch = pitch, yaw = yaw, roll = roll) }
    }
    val innerRotation by remember {
        derivedStateOf { Quaternion.fromEulerAngles(pitch = pitch, yaw = yaw, roll = roll) }
    }

    SpatialBox(
        modifier =
            SubspaceModifier.width(426.dp)
                .height(400.dp)
                .rotate(innerRotation)
                .rotate(outerRotation)
                .testTag("DoubleRotateBox")
    ) {
        val updatedModifier = if (isGravityAligned) modifier.gravityAligned() else modifier
        SpatialPanel(modifier = updatedModifier.testTag("TestPanel")) {
            GravityTestControls(
                isGravityAligned = isGravityAligned,
                onGravityAlignedChange = { isGravityAligned = it },
                pitch = pitch,
                onPitchChange = { pitch = it },
                roll = roll,
                onRollChange = { roll = it },
                yaw = yaw,
                onYawChange = { yaw = it },
            )
        }
    }
}

/**
 * Interactive Test Panel: Tests [SpatialBox(rotate)] -> [SpatialPanel(rotate.gravityAligned)] This
 * panel lives in a tilted row, AND has its OWN local rotation controlled by sliders, which is then
 * passed to the gravityAligned modifier. (Used for Case 5)
 */
@SubspaceComposable
@Composable
private fun CaseChainedGravityTestPanel(modifier: SubspaceModifier = SubspaceModifier) {
    var pitch by remember { mutableFloatStateOf(17f) }
    var yaw by remember { mutableFloatStateOf(29f) }
    var roll by remember { mutableFloatStateOf(-29f) }
    var isGravityAligned by remember { mutableStateOf(true) }
    // This is the static rotation of the Box BETWEEN the Tilted Row and the Panel.
    val parentBoxRotation = Quaternion.fromEulerAngles(10f, -50f, 10f)
    // This rotation is applied LOCALLY to the panel, *before* gravityAligned
    val childLocalRotation by remember {
        derivedStateOf { Quaternion.fromEulerAngles(pitch = pitch, yaw = yaw, roll = roll) }
    }
    val panelModifier =
        if (isGravityAligned) {
            SubspaceModifier.rotate(childLocalRotation).gravityAligned()
        } else {
            SubspaceModifier.rotate(childLocalRotation)
        }
    SpatialBox(
        modifier =
            SubspaceModifier.width(426.dp)
                .height(400.dp)
                .rotate(parentBoxRotation) // Add the extra layer of parent rotation
                .testTag("ChainedTestBox")
    ) {
        SpatialPanel(modifier = panelModifier.testTag("ChainedTestPanel")) {
            GravityTestControls(
                isGravityAligned = isGravityAligned,
                onGravityAlignedChange = { isGravityAligned = it },
                pitch = pitch,
                onPitchChange = { pitch = it },
                roll = roll,
                onRollChange = { roll = it },
                yaw = yaw,
                onYawChange = { yaw = it },
            )
        }
    }
}
