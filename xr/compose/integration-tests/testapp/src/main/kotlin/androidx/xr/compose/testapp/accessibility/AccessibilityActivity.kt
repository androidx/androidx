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

package androidx.xr.compose.testapp.accessibility

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateResourcesExhausted
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.common.AnotherActivity
import androidx.xr.compose.testapp.ui.components.ColumnWithCenterText
import androidx.xr.compose.testapp.ui.components.CommonTestPanel
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.ExrImage
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import kotlinx.coroutines.launch

class AccessibilityActivity : ComponentActivity() {
    private val activity = this
    private val TAG = "AccessibilityTest"
    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }
    private var spatialEnvironmentPreference: SpatialEnvironmentPreference? = null

    enum class PanelType {
        None,
        SpatialPanel,
        ActivityPanel,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f

        setContent {
            var title = intent.getStringExtra("TITLE")
            if (title == null) title = "Accessibility Test"
            MainContent()
        }
    }

    @Composable
    private fun MainContent() {
        var panelType by remember { mutableStateOf(PanelType.None) }
        Subspace {
            SpatialRow {
                CommonTestPanel(
                    size = DpVolumeSize(800.dp, 650.dp, 0.dp),
                    title = getString(R.string.accessibility_test),
                    showBottomBar = false,
                    onClickBackArrow = { this@AccessibilityActivity.finish() },
                ) { padding ->
                    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(padding)) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f).padding(6.dp),
                        ) {
                            Card("Virtual Environment") { GeometryUI() }
                            Card("GLTF Entities") { GltfEntityUI() }
                            Card("Anchor Entity") { AnchorEntityUI() }
                        }
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f).padding(6.dp),
                        ) {
                            Card("Surface Entity") { SurfaceEntityUI() }
                            Card("Panel Entity") { PanelEntityUI({ panelType = it }) }
                        }
                    }
                }
                when (panelType) {
                    PanelType.SpatialPanel -> {
                        SpatialColumn {
                            SimpleSpatialPanel("Spatial Panel 1", "Regular Spatial Panel 1")
                            SimpleSpatialPanel("Spatial Panel 2", "Regular Spatial Panel 2")
                            SimpleSpatialPanel("Spatial Panel 3", "Regular Spatial Panel 3")
                        }
                    }

                    PanelType.ActivityPanel -> {
                        SpatialColumn {
                            SpatialActivityPanel(
                                intent =
                                    Intent(activity, AnotherActivity::class.java)
                                        .putExtra("INSIDE_TEXT", "Spatial Activity Panel 1")
                                        .putExtra("TITLE", "Activity Panel 1"),
                                modifier = SubspaceModifier.width(300.dp).height(150.dp),
                            )
                            SpatialActivityPanel(
                                intent =
                                    Intent(activity, AnotherActivity::class.java)
                                        .putExtra("INSIDE_TEXT", "Spatial Activity Panel 2")
                                        .putExtra("TITLE", "Activity Panel 2"),
                                modifier = SubspaceModifier.width(300.dp).height(150.dp),
                            )
                            SpatialActivityPanel(
                                intent =
                                    Intent(activity, AnotherActivity::class.java)
                                        .putExtra("INSIDE_TEXT", "Spatial Activity Panel 3")
                                        .putExtra("TITLE", "Activity Panel 3"),
                                modifier = SubspaceModifier.width(300.dp).height(150.dp),
                            )
                        }
                    }

                    PanelType.None -> {
                        // No-op as nothing needs to be shown
                    }
                }
            }
        }
    }

    @Composable
    fun SimpleSpatialPanel(title: String = "Spatial Panel", insideText: String = "Spatial Panel") {
        IntegrationTestsAppTheme {
            SpatialPanel(SubspaceModifier.width(300.dp).height(150.dp)) {
                CommonTestScaffold(title = title, showBottomBar = false, onClickBackArrow = null) {
                    padding ->
                    ColumnWithCenterText(padding = padding, text = insideText)
                }
            }
        }
    }

    private fun setSkyboxAndGeometry(skybox: ExrImage?, geometry: GltfModel?) {
        spatialEnvironmentPreference =
            if (skybox == null && geometry == null) {
                null
            } else {
                SpatialEnvironmentPreference(skybox, geometry)
            }
        session.scene.spatialEnvironment.preferredSpatialEnvironment = spatialEnvironmentPreference
    }

    @Composable
    fun Card(title: String, content: @Composable () -> Unit) {
        Card(modifier = Modifier.padding(8.dp)) {
            Column(Modifier.padding(10.dp)) {
                Text("$title Card", fontSize = 25.sp)
                content()
            }
        }
    }

    @Composable
    fun GeometryUI() {
        var envGeometry by remember { mutableStateOf<GltfModel?>(null) }
        var blueSkybox by remember { mutableStateOf<ExrImage?>(null) }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button({
                scope.launch {
                    if (envGeometry == null) {
                        envGeometry =
                            GltfModel.create(session, Paths.get("models", "GroundGeometry.glb"))
                        blueSkybox =
                            ExrImage.createFromZip(session, Paths.get("skyboxes", "BlueSkybox.zip"))
                    }
                    setSkyboxAndGeometry(blueSkybox, envGeometry)
                }
            }) {
                Text("Create Environment", fontSize = 20.sp)
            }
            Button({ setSkyboxAndGeometry(null, null) }) {
                Text("Remove Environment", fontSize = 20.sp)
            }
        }
    }

    @Composable
    fun LabeledRadioButton(text: String, selected: Boolean = false, onClick: () -> Unit = {}) {
        Row(
            Modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected, onClick)
            Text(text)
        }
    }

    @Composable
    fun SurfaceEntityUI() {
        var surfaceEntity by remember { mutableStateOf<SurfaceEntity?>(null) }
        val radius = 0.65f
        var shape by remember {
            mutableStateOf<SurfaceEntity.Shape>(SurfaceEntity.Shape.Hemisphere(radius))
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column {
                LabeledRadioButton(
                    "Quad",
                    shape is SurfaceEntity.Shape.Quad,
                    { shape = SurfaceEntity.Shape.Quad(FloatSize2d(radius * 2f, radius * 2f)) },
                )
                LabeledRadioButton(
                    "Hemisphere",
                    shape is SurfaceEntity.Shape.Hemisphere,
                    { shape = SurfaceEntity.Shape.Hemisphere(radius) },
                )
                LabeledRadioButton(
                    "Sphere",
                    shape is SurfaceEntity.Shape.Sphere,
                    { shape = SurfaceEntity.Shape.Sphere(radius) },
                )
            }
            Button({
                if (surfaceEntity == null) {
                    surfaceEntity =
                        SurfaceEntity.create(
                            session,
                            pose = Pose(Vector3(-1f, 0f, -0.5f)),
                            shape = shape,
                        )
                    surfaceEntity?.contentDescription = "${shape.javaClass.simpleName} Surface"
                }
            }) {
                Text("Create Surface", fontSize = 20.sp)
            }
            Button({
                surfaceEntity?.dispose()
                surfaceEntity = null
            }) {
                Text("Remove Surface", fontSize = 20.sp)
            }
        }
    }

    fun createModelEntity(model: GltfModel, desc: String, translation: Vector3): GltfModelEntity {
        val entity: GltfModelEntity
        entity = GltfModelEntity.create(session, model, Pose(translation))

        entity.setScale(0.5f)
        entity.contentDescription = desc
        return entity
    }

    @Composable
    fun GltfEntityUI() {
        val dragonEntities = remember { List(3) { mutableStateOf<GltfModelEntity?>(null) } }
        var entitiesCreated by remember { mutableStateOf(false) }
        var dragonModel by remember { mutableStateOf<GltfModel?>(null) }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button({
                scope.launch {
                    if (!entitiesCreated) {
                        if (dragonModel == null) {
                            dragonModel =
                                GltfModel.create(
                                    session,
                                    Paths.get("models", "Dragon_Evolved.gltf"),
                                )
                        }
                        var offset = -1.5f
                        entitiesCreated = true
                        dragonEntities.forEachIndexed { index, entity ->
                            val translation = Vector3(offset, 0f, -1f)
                            entity.value =
                                createModelEntity(
                                    dragonModel!!,
                                    "Dragon Entity ${index+1} at $translation",
                                    translation,
                                )
                            offset += 1.5f
                        }
                    }
                }
            }) {
                Text("Create Models", fontSize = 20.sp)
            }
            Button({
                if (entitiesCreated) {
                    entitiesCreated = false
                    dragonEntities.forEach {
                        it.value?.dispose()
                        it.value = null
                    }
                }
            }) {
                Text("Remove Models", fontSize = 20.sp)
            }
        }
    }

    @Composable
    fun PanelEntityUI(createPanelType: (PanelType) -> Unit) {
        var type by remember { mutableStateOf(PanelType.SpatialPanel) }
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row {
                LabeledRadioButton(
                    "Spatial Panel",
                    type == PanelType.SpatialPanel,
                    { type = PanelType.SpatialPanel },
                )
                LabeledRadioButton(
                    "Activity Spatial Panel",
                    type == PanelType.ActivityPanel,
                    { type = PanelType.ActivityPanel },
                )
            }
            Button({ createPanelType(type) }) { Text("Create Panels", fontSize = 20.sp) }
            Button({ createPanelType(PanelType.None) }) { Text("Remove Panels", fontSize = 20.sp) }
        }
    }

    @Composable
    fun AnchorEntityUI() {
        val gltfEntity = remember { mutableStateOf<GltfModelEntity?>(null) }
        val anchorEntity = remember { mutableStateOf<AnchorEntity?>(null) }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button({
                scope.launch {
                    val anchorPose = Pose(Vector3(0f, -0.5f, -0.5f))
                    val anchorResult = Anchor.create(session, anchorPose)
                    when (anchorResult) {
                        is AnchorCreateSuccess -> {
                            val model =
                                GltfModel.create(session, Paths.get("models", "xyzArrows.glb"))
                            gltfEntity.value = createModelEntity(model, "", anchorPose.translation)
                            anchorEntity.value =
                                AnchorEntity.create(session, anchor = anchorResult.anchor)
                            gltfEntity.value?.parent = anchorEntity?.value
                            anchorEntity.value?.contentDescription =
                                "Anchor Entity at ${anchorPose.translation}"
                        }

                        is AnchorCreateResourcesExhausted -> {
                            Log.e(TAG, "Failed to create anchor: anchor resources exhausted.")
                        }

                        else -> {
                            Log.e(TAG, "Failed to create anchor: ${anchorResult::class.simpleName}")
                        }
                    }
                }
            }) {
                Text("Create Anchor", fontSize = 20.sp)
            }
            Button({
                anchorEntity.value?.dispose()
                anchorEntity.value = null
                gltfEntity.value?.dispose()
                gltfEntity.value = null
            }) {
                Text("Remove Anchor", fontSize = 20.sp)
            }
        }
    }
}
