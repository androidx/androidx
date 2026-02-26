/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.testapp.spatialgltfmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialGltfModel
import androidx.xr.compose.subspace.SpatialGltfModelSource
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.draw.scale
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.rememberSpatialGltfModelState
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.unit.Meter
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.GltfModelNode
import androidx.xr.scenecore.KhronosPbrMaterial
import java.nio.file.Paths

class SpatialGltfModelActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state = remember { DragonControlState() }
            GltfControlPanel(state)
            Subspace { SpatialContent(state) }
        }
    }

    @Composable
    fun SpatialContent(state: DragonControlState) {
        val session = LocalSession.current
        LaunchedEffect(session) {
            state.initializeSession(checkNotNull(session) { "session must be initialized" })
        }

        SpatialRow {
            DragonModel(state)
            SpatialMainPanel(modifier = SubspaceModifier.width(600.dp).height(800.dp))
        }
    }

    @SuppressLint("PrimitiveInCollection")
    @Composable
    fun GltfControlPanel(state: DragonControlState) {
        var title = intent.getStringExtra("TITLE") ?: "Spatial Gltf Model Test"
        val context = LocalContext.current

        CommonTestScaffold(
            title = title,
            showBottomBar = true,
            onClickBackArrow = { this@SpatialGltfModelActivity.finish() },
            onClickRecreate = { this@SpatialGltfModelActivity.recreate() },
        ) { innerPadding ->
            Column(
                Modifier.padding(innerPadding).fillMaxSize().background(Color.White).padding(16.dp)
            ) {
                Text("Controls", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { state.useRotation = !state.useRotation },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    ) {
                        Text(if (state.useRotation) "No Rotation" else "Rotation")
                    }
                }

                // Show Arrows Toggle
                Button(
                    onClick = { state.showArrows = !state.showArrows },
                    enabled = state.selectedNode != null,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(if (state.showArrows) "Hide XYZ Arrows" else "Show XYZ Arrows")
                }

                // Material Override Controls
                Column(
                    Modifier.fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color.LightGray.copy(alpha = 0.2f))
                        .padding(8.dp)
                ) {
                    Button(
                        onClick = { state.toggleMaterialOverride(context) },
                        enabled = state.selectedNode != null && state.overrideMaterial != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (state.isMaterialOverridden) "Remove Material Override"
                            else "Apply Material Override"
                        )
                    }

                    if (state.isMaterialOverridden) {
                        Spacer(Modifier.height(8.dp))
                        Text("Material Properties", style = MaterialTheme.typography.labelMedium)
                        SliderRow("Metallic", state.metallic, 0f, 1f) {
                            state.updateMaterialProperties(metallic = it)
                        }
                        SliderRow("Roughness", state.roughness, 0f, 1f) {
                            state.updateMaterialProperties(roughness = it)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Nodes List & Transform Controls
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(0.4f).fillMaxSize()) {
                        Text(
                            "Nodes (${state.nodes.size})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        LazyColumn(
                            Modifier.fillMaxWidth()
                                .weight(1f)
                                .background(Color.LightGray.copy(alpha = 0.1f))
                        ) {
                            items(state.nodes) { node ->
                                val isSelected = state.selectedNode == node
                                Text(
                                    text = node.name ?: "Node ${node.index}",
                                    fontSize = 14.sp,
                                    fontWeight =
                                        if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .clickable {
                                                state.selectedNode = node
                                                state.updateFromNode(node)
                                            }
                                            .background(
                                                if (isSelected) Color.Blue.copy(alpha = 0.2f)
                                                else Color.Transparent
                                            )
                                            .padding(8.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(Modifier.weight(0.6f).fillMaxSize()) {
                        if (state.selectedNode != null) {
                            TransformControls(state)
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select a node", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TransformControls(state: DragonControlState) {
        val data = state.transformData

        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            Text("Transform (Local)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn {
                item {
                    Text("Translation", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    SliderRow("Tx", data.translation.x, -2f, 2f) {
                        state.updateTranslation(data.translation.copy(x = it))
                    }
                    SliderRow("Ty", data.translation.y, -2f, 2f) {
                        state.updateTranslation(data.translation.copy(y = it))
                    }
                    SliderRow("Tz", data.translation.z, -2f, 2f) {
                        state.updateTranslation(data.translation.copy(z = it))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
                item {
                    Text("Rotation (Euler)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    SliderRow("Rx", data.rotationEuler.x, -180f, 180f) {
                        state.updateRotation(data.rotationEuler.copy(x = it))
                    }
                    SliderRow("Ry", data.rotationEuler.y, -180f, 180f) {
                        state.updateRotation(data.rotationEuler.copy(y = it))
                    }
                    SliderRow("Rz", data.rotationEuler.z, -180f, 180f) {
                        state.updateRotation(data.rotationEuler.copy(z = it))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
                item {
                    Text("Scale", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    SliderRow("Sx", data.scale.x, 0f, 5f) {
                        state.updateScale(data.scale.copy(x = it))
                    }
                    SliderRow("Sy", data.scale.y, 0f, 5f) {
                        state.updateScale(data.scale.copy(y = it))
                    }
                    SliderRow("Sz", data.scale.z, 0f, 5f) {
                        state.updateScale(data.scale.copy(z = it))
                    }
                }
            }
        }
    }

    @Composable
    fun SliderRow(
        label: String,
        value: Float,
        min: Float,
        max: Float,
        onValueChange: (Float) -> Unit,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label: %.2f".format(value),
                fontSize = 11.sp,
                modifier = Modifier.width(70.dp),
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = min..max,
                modifier = Modifier.weight(1f),
            )
        }
    }

    @SuppressLint("PrimitiveInCollection")
    @Composable
    @SubspaceComposable
    fun DragonModel(state: DragonControlState) {
        val dragonModelState =
            rememberSpatialGltfModelState(
                source = SpatialGltfModelSource.fromPath(Paths.get("models", "Dragon_Evolved.gltf"))
            )

        val arrowsModelState =
            rememberSpatialGltfModelState(
                source = SpatialGltfModelSource.fromPath(Paths.get("models", "xyzArrows.glb"))
            )

        LaunchedEffect(dragonModelState.nodes.size) {
            if (dragonModelState.nodes.isNotEmpty()) {
                state.nodes = dragonModelState.nodes
            }
        }

        SpatialBox {
            SpatialGltfModel(state = dragonModelState) {
                val selectedNode = state.selectedNode
                if (selectedNode != null) {
                    val nodeOffset =
                        createSpatialOffset(
                            translation = selectedNode.modelPose.translation,
                            rotation =
                                if (state.useRotation) selectedNode.modelPose.rotation else null,
                        )

                    SpatialPanel(
                        shape = SpatialRoundedCornerShape(CornerSize(25)),
                        modifier = SubspaceModifier.width(700.dp).height(700.dp).then(nodeOffset),
                    ) {
                        Box(
                            Modifier.fillMaxSize().background(Color.Blue.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = selectedNode.name ?: "Selected",
                                color = Color.White,
                                fontSize = 30.sp,
                            )
                        }
                    }
                    if (state.showArrows) {
                        SpatialGltfModel(
                            state = arrowsModelState,
                            modifier = SubspaceModifier.then(nodeOffset).scale(1.5f),
                        )
                    }
                }
            }
        }
    }

    data class TransformData(
        val translation: Vector3 = Vector3(0f, 0f, 0f),
        val rotationEuler: Vector3 = Vector3(0f, 0f, 0f),
        val scale: Vector3 = Vector3(1f, 1f, 1f),
    )

    class DragonControlState {
        var isAnimating by mutableStateOf(false)
        var nodes by mutableStateOf<List<GltfModelNode>>(emptyList())
        var selectedNode by mutableStateOf<GltfModelNode?>(null)
        var useRotation by mutableStateOf(false)
        var showArrows by mutableStateOf(false)

        // Material Override State
        var overrideMaterial: KhronosPbrMaterial? = null
        var isMaterialOverridden by mutableStateOf(false)

        // New State for Material Properties
        var metallic by mutableFloatStateOf(1.0f)
        var roughness by mutableFloatStateOf(0.0f)

        private var session: Session? = null
        var transformData by mutableStateOf(TransformData())

        suspend fun initializeSession(session: Session) {
            if (this.session == null) {
                this.session = session
                // Create material with initial values
                val mat = KhronosPbrMaterial.create(session, AlphaMode.OPAQUE)
                mat.setMetallicFactor(metallic)
                mat.setRoughnessFactor(roughness)
                this.overrideMaterial = mat
            }
        }

        fun toggleMaterialOverride(context: Context) {
            val node = selectedNode ?: return
            val mat = overrideMaterial ?: return

            try {
                if (isMaterialOverridden) {
                    node.clearMaterialOverride(0)
                    isMaterialOverridden = false
                } else {
                    node.setMaterialOverride(mat, 0)
                    isMaterialOverridden = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Can't apply override: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                isMaterialOverridden = false
            }
        }

        fun updateMaterialProperties(metallic: Float? = null, roughness: Float? = null) {
            val mat = overrideMaterial ?: return

            if (metallic != null) {
                this.metallic = metallic
                mat.setMetallicFactor(metallic)
            }
            if (roughness != null) {
                this.roughness = roughness
                mat.setRoughnessFactor(roughness)
            }
        }

        fun updateFromNode(node: GltfModelNode) {
            isMaterialOverridden = false

            val pose = node.localPose
            val scale = node.localScale
            val euler = pose.rotation.eulerAngles

            transformData =
                TransformData(
                    translation = pose.translation,
                    rotationEuler = Vector3(euler.x, euler.y, euler.z),
                    scale = scale,
                )
        }

        fun updateTranslation(translation: Vector3) {
            transformData = transformData.copy(translation = translation)
            applyTransform()
        }

        fun updateRotation(rotation: Vector3) {
            transformData = transformData.copy(rotationEuler = rotation)
            applyTransform()
        }

        fun updateScale(scale: Vector3) {
            transformData = transformData.copy(scale = scale)
            applyTransform()
        }

        private fun applyTransform() {
            val node = selectedNode ?: return
            val translation = transformData.translation
            val rotation = transformData.rotationEuler
            val scale = transformData.scale

            val rotationQuat = Quaternion.fromEulerAngles(rotation.x, rotation.y, rotation.z)

            node.localPose = Pose(translation, rotationQuat)
            node.localScale = scale
        }
    }

    /** Converts a 3D translation to a SubspaceOffset. */
    fun createSpatialOffset(translation: Vector3, rotation: Quaternion? = null): SubspaceModifier {
        return SubspaceModifier.offset(
                x = Meter(translation.x).toDp(),
                y = Meter(translation.y).toDp(),
                z = Meter(translation.z).toDp(),
            )
            .let { if (rotation != null) it.rotate(rotation) else it }
    }
}
