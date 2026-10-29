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

package androidx.xr.compose.testapp.modifierchain

import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ExperimentalSpatialGltfModelApi
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialGltfModel
import androidx.xr.compose.subspace.SpatialGltfModelSource
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.draw.alpha
import androidx.xr.compose.subspace.draw.scale
import androidx.xr.compose.subspace.layout.ExperimentalMoveAnchorPolicy
import androidx.xr.compose.subspace.layout.ExperimentalRotateToLookAtUserApi
import androidx.xr.compose.subspace.layout.MovePolicy
import androidx.xr.compose.subspace.layout.PlaneOrientation
import androidx.xr.compose.subspace.layout.ResizePolicy
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.aspectRatio
import androidx.xr.compose.subspace.layout.gravityAligned
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.rotateToLookAtUser
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.rememberSpatialGltfModelState
import androidx.xr.compose.testapp.ui.components.TopBarWithBackArrow
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple40
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import java.nio.file.Paths

@OptIn(
    ExperimentalMoveAnchorPolicy::class,
    ExperimentalRotateToLookAtUserApi::class,
    ExperimentalSpatialGltfModelApi::class,
)
class ModifierChainActivity : ComponentActivity() {

    private enum class TargetComposableType(val label: String) {
        PANEL("SpatialPanel"),
        GLTF("SpatialGltfModel (Dragon)"),
        EXTERNAL_SURFACE("SpatialExternalSurface"),
    }

    private enum class ModifierOption(
        val label: String,
        val chainRepresentation: String,
        val applyTo: (SubspaceModifier) -> SubspaceModifier,
    ) {
        SIZE(
            label = "size (300.dp)",
            chainRepresentation = "size(300.dp)",
            applyTo = { it.size(300.dp) },
        ),
        ASPECT_RATIO(
            label = "aspect ratio (16:9)",
            chainRepresentation = "aspectRatio(1.78f)",
            applyTo = { it.aspectRatio(16f / 9f) },
        ),
        OFFSET(
            label = "offset (50.dp, 50.dp, 50.dp)",
            chainRepresentation = "offset(50.dp, 50.dp, 50.dp)",
            applyTo = { it.offset(x = 50.dp, y = 50.dp, z = 50.dp) },
        ),
        PADDING(
            label = "padding (32.dp)",
            chainRepresentation = "padding(32.dp)",
            applyTo = { it.padding(32.dp) },
        ),
        ROTATE(
            label = "rotate (10°, 10°, 10°)",
            chainRepresentation = "rotate(10f, 10f, 10f)",
            applyTo = { it.rotate(pitch = 10f, yaw = 10f, roll = 10f) },
        ),
        ROTATE_TO_LOOK_AT_USER(
            label = "rotateToLookAtUser",
            chainRepresentation = "rotateToLookAtUser()",
            applyTo = { it.rotateToLookAtUser() },
        ),
        GRAVITY_ALIGNED(
            label = "gravityAligned",
            chainRepresentation = "gravityAligned()",
            applyTo = { it.gravityAligned() },
        ),
        MOVABLE_SYSTEM(
            label = "movable (system)",
            chainRepresentation = "movable(system)",
            applyTo = { it.movable(movePolicy = MovePolicy.system()) },
        ),
        MOVABLE_ANCHORABLE(
            label = "movable (anchorable)",
            chainRepresentation = "movable(anchor)",
            applyTo = {
                it.movable(
                    movePolicy =
                        MovePolicy.anchor(anchorPlaneOrientations = setOf(PlaneOrientation.Any))
                )
            },
        ),
        RESIZABLE_SYSTEM(
            label = "resizable (system)",
            chainRepresentation = "resizable(system)",
            applyTo = { it.resizable(resizePolicy = ResizePolicy.system()) },
        ),
        ALPHA(
            label = "alpha (0.5f)",
            chainRepresentation = "alpha(0.5f)",
            applyTo = { it.alpha(0.5f) },
        ),
        SCALE(
            label = "scale (1.5f)",
            chainRepresentation = "scale(1.5f)",
            applyTo = { it.scale(1.5f) },
        ),
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainContent() }
    }

    @Composable
    private fun MainContent() {
        val session = LocalSession.current ?: return
        session.configure(
            Config.Builder(session.config)
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                .build()
        )

        val activeModifiers = remember { mutableStateListOf<ModifierOption>() }
        var selectedTarget by remember { mutableStateOf(TargetComposableType.PANEL) }

        val chainString =
            if (activeModifiers.isEmpty()) {
                "SubspaceModifier"
            } else {
                activeModifiers.joinToString(prefix = "SubspaceModifier", separator = "") {
                    ".${it.chainRepresentation}"
                }
            }

        val combinedModifier =
            activeModifiers.fold(SubspaceModifier as SubspaceModifier) { acc, option ->
                option.applyTo(acc)
            }

        IntegrationTestsAppTheme {
            Subspace {
                SpatialRow(
                    horizontalArrangement = SpatialArrangement.spacedBy(80.dp),
                    verticalAlignment = SpatialAlignment.CenterVertically,
                ) {
                    ControlPanel(
                        chainString = chainString,
                        selectedTarget = selectedTarget,
                        onSelectTarget = { selectedTarget = it },
                        activeModifiers = activeModifiers,
                        onAddModifier = { option -> activeModifiers.add(option) },
                        onRemoveModifier = { option -> activeModifiers.removeAll { it == option } },
                    )

                    SpatialBox(
                        modifier = SubspaceModifier.width(500.dp).height(500.dp),
                        alignment = SpatialAlignment.Center,
                    ) {
                        TargetObject(
                            targetType = selectedTarget,
                            modifier = combinedModifier,
                            chainString = chainString,
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SubspaceComposable
    @Composable
    private fun ControlPanel(
        chainString: String,
        selectedTarget: TargetComposableType,
        onSelectTarget: (TargetComposableType) -> Unit,
        activeModifiers: List<ModifierOption>,
        onAddModifier: (ModifierOption) -> Unit,
        onRemoveModifier: (ModifierOption) -> Unit,
    ) {
        SpatialPanel(modifier = SubspaceModifier.width(550.dp).height(900.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().background(PurpleGrey80),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TopBarWithBackArrow(
                        scrollBehavior = null,
                        title = "Modifier Chain Test",
                        onClick = { finish() },
                    )
                }

                Column(
                    modifier =
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Modifier chain display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text(
                                text = "Active Modifier Chain",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Purple40,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            Text(
                                text = chainString,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black,
                            )
                        }
                    }

                    // Target object selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text(
                                text = "Target Composable",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Purple40,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            TargetComposableType.entries.forEach { target ->
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .selectable(
                                                selected = selectedTarget == target,
                                                onClick = { onSelectTarget(target) },
                                                role = Role.RadioButton,
                                            )
                                            .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = selectedTarget == target,
                                        onClick = { onSelectTarget(target) },
                                    )
                                    Text(
                                        text = target.label,
                                        fontSize = 14.sp,
                                        color = Color.Black,
                                    )
                                }
                            }
                        }
                    }

                    // Add and Remove buttons for modifiers
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            Text(
                                text = "Modifiers",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Purple40,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            ModifierOption.entries.forEachIndexed { index, option ->
                                val count = activeModifiers.count { it == option }
                                val isAdded = count > 0
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text =
                                            if (isAdded) {
                                                "${option.label} ($count)"
                                            } else {
                                                option.label
                                            },
                                        fontSize = 14.sp,
                                        fontWeight =
                                            if (isAdded) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isAdded) Purple40 else Color.Black,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onAddModifier(option) }) { Text("Add") }
                                        Button(
                                            onClick = { onRemoveModifier(option) },
                                            enabled = isAdded,
                                        ) {
                                            Text("Remove")
                                        }
                                    }
                                }
                                if (index < ModifierOption.entries.lastIndex) {
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubspaceComposable
    @Composable
    private fun TargetObject(
        targetType: TargetComposableType,
        modifier: SubspaceModifier,
        chainString: String,
    ) {
        when (targetType) {
            TargetComposableType.PANEL -> {
                SpatialPanel(modifier = modifier) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Purple40).padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Target Panel\n\n$chainString",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            TargetComposableType.GLTF -> {
                val dragonState =
                    rememberSpatialGltfModelState(
                        source =
                            SpatialGltfModelSource.fromPath(
                                Paths.get("models", "Dragon_Evolved.gltf")
                            )
                    )
                SpatialGltfModel(state = dragonState, modifier = modifier)
            }
            TargetComposableType.EXTERNAL_SURFACE -> {
                SpatialExternalSurface(stereoMode = StereoMode.Mono, modifier = modifier) {
                    onSurfaceCreated { surface ->
                        val canvas = surface.lockCanvas(null)
                        canvas.drawColor(AndroidColor.BLUE)
                        surface.unlockCanvasAndPost(canvas)
                    }
                }
            }
        }
    }
}
