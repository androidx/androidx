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

package androidx.xr.compose.testapp.anchorable

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ExperimentalSpatialGltfModelApi
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialGltfModel
import androidx.xr.compose.subspace.SpatialGltfModelSource
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.ExperimentalMoveAnchorPolicy
import androidx.xr.compose.subspace.layout.MovePolicy
import androidx.xr.compose.subspace.layout.PlaneOrientation
import androidx.xr.compose.subspace.layout.PlaneSemantic
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.rememberSpatialGltfModelState
import androidx.xr.compose.testapp.ui.components.TopBarWithBackArrow
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple40
import androidx.xr.compose.testapp.ui.theme.PurpleGrey40
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import java.nio.file.Paths

/**
 * Integration test activity for the anchorable modifier.
 *
 * This activity provides a visual playground to test moving and anchoring various spatial
 * composables to physical environment planes using Horizontal, Vertical or Any plane orientation
 * filters.
 */
@OptIn(ExperimentalMoveAnchorPolicy::class)
class AnchorableActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainContent() }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalSpatialGltfModelApi::class)
    @SubspaceComposable
    @Composable
    private fun MainContent() {
        val session = checkNotNull(LocalSession.current) { "session must be initialized" }
        session.configure(
            Config.Builder(session.config)
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .build()
        )

        var isAnchorableOn by remember { mutableStateOf(true) }
        var selectedOrientation by remember { mutableStateOf("Any") }
        var selectedSemantic by remember { mutableStateOf("Any") }

        IntegrationTestsAppTheme {
            Subspace(modifier = SubspaceModifier.width(1800.dp).height(1000.dp)) {
                SpatialRow(
                    modifier = SubspaceModifier.offset(y = 100.dp),
                    horizontalArrangement = SpatialArrangement.spacedBy(40.dp),
                ) {
                    ControlPanel(
                        isAnchorableOn = isAnchorableOn,
                        onToggleAnchorable = { isAnchorableOn = it },
                        selectedOrientation = selectedOrientation,
                        onOrientationChange = { selectedOrientation = it },
                        selectedSemantic = selectedSemantic,
                        onSemanticChange = { selectedSemantic = it },
                    )

                    // Column 1: Standard Spatial Panels, Surfaces, and Row
                    SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(20.dp)) {
                        TestPanelContainer(
                            title = "SpatialPanel",
                            isAnchorableOn = isAnchorableOn,
                            orientationStr = selectedOrientation,
                            semanticStr = selectedSemantic,
                        ) { modifier, content ->
                            SpatialPanel(modifier = modifier, content = content)
                        }

                        TestPanelContainer(
                            title = "SpatialExternalSurface",
                            isAnchorableOn = isAnchorableOn,
                            orientationStr = selectedOrientation,
                            semanticStr = selectedSemantic,
                        ) { modifier, content ->
                            SpatialExternalSurface(
                                modifier = modifier,
                                stereoMode = StereoMode.Mono,
                            ) {
                                SpatialPanel(
                                    modifier = SubspaceModifier.fillMaxSize(),
                                    content = content,
                                )
                            }
                        }

                        TestPanelContainer(
                            title = "SpatialRow",
                            isAnchorableOn = isAnchorableOn,
                            orientationStr = selectedOrientation,
                            semanticStr = selectedSemantic,
                        ) { modifier, content ->
                            SpatialRow(
                                modifier = modifier,
                                horizontalArrangement = SpatialArrangement.spacedBy(20.dp),
                            ) {
                                SpatialPanel(
                                    modifier = SubspaceModifier.width(170.dp).height(200.dp),
                                    content = content,
                                )
                                SpatialPanel(
                                    modifier = SubspaceModifier.width(170.dp).height(200.dp)
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.fillMaxSize()
                                                .background(PurpleGrey40)
                                                .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Row Child 2",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Column 2: Spatial Column, Box, and GltfModel
                    SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(20.dp)) {
                        TestPanelContainer(
                            title = "SpatialColumn",
                            isAnchorableOn = isAnchorableOn,
                            orientationStr = selectedOrientation,
                            semanticStr = selectedSemantic,
                        ) { modifier, content ->
                            SpatialColumn(
                                modifier = modifier,
                                verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                            ) {
                                SpatialPanel(
                                    modifier = SubspaceModifier.width(360.dp).height(90.dp),
                                    content = content,
                                )
                                SpatialPanel(
                                    modifier = SubspaceModifier.width(360.dp).height(90.dp)
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.fillMaxSize()
                                                .background(PurpleGrey40)
                                                .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Col Child 2",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }

                        TestPanelContainer(
                            title = "SpatialBox",
                            isAnchorableOn = isAnchorableOn,
                            orientationStr = selectedOrientation,
                            semanticStr = selectedSemantic,
                        ) { modifier, content ->
                            SpatialBox(modifier = modifier) {
                                SpatialPanel(
                                    modifier = SubspaceModifier.fillMaxSize(),
                                    content = content,
                                )
                                SpatialPanel(
                                    modifier =
                                        SubspaceModifier.width(180.dp)
                                            .height(100.dp)
                                            .offset(x = 90.dp, y = 50.dp, z = 50.dp)
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.fillMaxSize()
                                                .background(PurpleGrey40)
                                                .padding(8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Box Child 2 (Z+50dp)",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }

                        // SpatialGltfModel: Renders XYZ Arrows 3D model and supports anchorable
                        // tracking
                        val arrowsState =
                            rememberSpatialGltfModelState(
                                source =
                                    SpatialGltfModelSource.fromPath(
                                        Paths.get("models", "xyzArrows.glb")
                                    )
                            )
                        val modelModifier =
                            SubspaceModifier.width(200.dp)
                                .height(200.dp)
                                .movable(
                                    enabled = isAnchorableOn,
                                    movePolicy =
                                        MovePolicy.anchor(
                                            anchorPlaneOrientations =
                                                parseOrientations(selectedOrientation),
                                            anchorPlaneSemantics = parseSemantics(selectedSemantic),
                                        ),
                                )
                        SpatialGltfModel(state = arrowsState, modifier = modelModifier)
                    }
                }
            }
        }
    }

    @SubspaceComposable
    @Composable
    private fun ControlPanel(
        isAnchorableOn: Boolean,
        onToggleAnchorable: (Boolean) -> Unit,
        selectedOrientation: String,
        onOrientationChange: (String) -> Unit,
        selectedSemantic: String,
        onSemanticChange: (String) -> Unit,
    ) {
        SpatialPanel(modifier = SubspaceModifier.width(400.dp).height(780.dp).padding(15.dp)) {
            ControlPanelContent(
                isAnchorableOn = isAnchorableOn,
                onToggleAnchorable = onToggleAnchorable,
                selectedOrientation = selectedOrientation,
                onOrientationChange = onOrientationChange,
                selectedSemantic = selectedSemantic,
                onSemanticChange = onSemanticChange,
                onBackClick = { finish() },
            )
        }
    }

    @Composable
    private fun SettingCard(title: String, content: @Composable () -> Unit) {
        Card(
            modifier = Modifier.padding(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(8.dp).fillMaxWidth()) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Purple40,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                content()
            }
        }
    }

    @Composable
    private fun LabeledRadioButton(
        text: String,
        selected: Boolean = false,
        onClick: () -> Unit = {},
    ) {
        Row(
            Modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected, onClick)
            Text(text, fontSize = 14.sp, color = Color.Black)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ControlPanelContent(
        isAnchorableOn: Boolean,
        onToggleAnchorable: (Boolean) -> Unit,
        selectedOrientation: String,
        onOrientationChange: (String) -> Unit,
        selectedSemantic: String,
        onSemanticChange: (String) -> Unit,
        onBackClick: () -> Unit,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(PurpleGrey80).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TopBarWithBackArrow(
                    scrollBehavior = null,
                    title = "Anchorable Test",
                    onClick = onBackClick,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingCard(title = "Anchor Configuration") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Enable",
                            fontSize = 14.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Switch(checked = isAnchorableOn, onCheckedChange = onToggleAnchorable)
                    }
                }

                SettingCard(title = "Allowed Orientations") {
                    Column {
                        ALLOWED_ORIENTATIONS.forEach { item ->
                            LabeledRadioButton(
                                text = item,
                                selected = selectedOrientation == item,
                                onClick = { onOrientationChange(item) },
                            )
                        }
                    }
                }

                SettingCard(title = "Allowed Plane Semantics") {
                    Column {
                        ALLOWED_SEMANTICS.forEach { item ->
                            LabeledRadioButton(
                                text = item,
                                selected = selectedSemantic == item,
                                onClick = { onSemanticChange(item) },
                            )
                        }
                    }
                }
            }
        }
    }

    @SubspaceComposable
    @Composable
    private fun TestPanelContainer(
        title: String,
        isAnchorableOn: Boolean,
        orientationStr: String,
        semanticStr: String,
        modifier: SubspaceModifier = SubspaceModifier,
        width: Int = 360,
        height: Int = 200,
        container:
            @Composable
            @SubspaceComposable
            (SubspaceModifier, @Composable () -> Unit) -> Unit,
    ) {
        var finalModifier = modifier.width(width.dp).height(height.dp)
        finalModifier =
            finalModifier.movable(
                enabled = isAnchorableOn,
                movePolicy =
                    MovePolicy.anchor(
                        anchorPlaneOrientations = parseOrientations(orientationStr),
                        anchorPlaneSemantics = parseSemantics(semanticStr),
                    ),
            )

        val innerContent: @Composable () -> Unit = {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(if (isAnchorableOn) Purple40 else PurpleGrey40)
                        .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        container(finalModifier, innerContent)
    }

    companion object {
        val ALLOWED_ORIENTATIONS = listOf("Any", "Horizontal", "Vertical")
        val ALLOWED_SEMANTICS = listOf("Any", "Table", "Floor", "Ceiling", "Wall")

        fun parseOrientations(orientationStr: String): Set<PlaneOrientation> =
            when (orientationStr) {
                "Horizontal" -> setOf(PlaneOrientation.Horizontal)
                "Vertical" -> setOf(PlaneOrientation.Vertical)
                else -> setOf(PlaneOrientation.Any)
            }

        fun parseSemantics(semanticStr: String): Set<PlaneSemantic> =
            when (semanticStr) {
                "Table" -> setOf(PlaneSemantic.Table)
                "Floor" -> setOf(PlaneSemantic.Floor)
                "Ceiling" -> setOf(PlaneSemantic.Ceiling)
                "Wall" -> setOf(PlaneSemantic.Wall)
                else -> setOf(PlaneSemantic.Any)
            }
    }
}
