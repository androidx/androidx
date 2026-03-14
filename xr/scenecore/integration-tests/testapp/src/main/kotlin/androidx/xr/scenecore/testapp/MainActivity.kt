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

package androidx.xr.scenecore.testapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.accessibilitytest.AccessibilityTestActivity
import androidx.xr.scenecore.testapp.activitypanel.ActivityPanelActivity
import androidx.xr.scenecore.testapp.anchorentity.AnchorEntityActivity
import androidx.xr.scenecore.testapp.common.managers.SessionManager
import androidx.xr.scenecore.testapp.environment.EnvironmentActivity
import androidx.xr.scenecore.testapp.fieldofviewvisibility.FieldOfViewVisibilityActivity
import androidx.xr.scenecore.testapp.fsmhsmtransition.FsmHsmTransitionActivity
import androidx.xr.scenecore.testapp.headlockedui.HeadLockedUiActivity
import androidx.xr.scenecore.testapp.hittest.HitTestActivity
import androidx.xr.scenecore.testapp.inputmoveresize.InputMoveResizeTestActivity
import androidx.xr.scenecore.testapp.memoryleak.MemoryLeakActivity
import androidx.xr.scenecore.testapp.model.GltfModelAnimationActivity
import androidx.xr.scenecore.testapp.model.GltfModelMaterialTextureActivity
import androidx.xr.scenecore.testapp.movable.MovableActivity
import androidx.xr.scenecore.testapp.panelcoordinate.PanelCoordinateActivity
import androidx.xr.scenecore.testapp.panelroundedcorner.PanelRoundedCornerActivity
import androidx.xr.scenecore.testapp.sceneviewer.SceneViewerActivity
import androidx.xr.scenecore.testapp.spatialaudio.SpatialAudioActivity
import androidx.xr.scenecore.testapp.spatialcapabilities.SpatialCapabilitiesActivity
import androidx.xr.scenecore.testapp.spatialuser.SpatialUserActivity
import androidx.xr.scenecore.testapp.standalone.StandaloneActivity
import androidx.xr.scenecore.testapp.surfacecustommesh.SurfaceEntityCustomMeshActivity
import androidx.xr.scenecore.testapp.surfaceimage.SurfaceEntityImageActivity
import androidx.xr.scenecore.testapp.surfaceinteraction.SurfaceEntityInteractionActivity
import androidx.xr.scenecore.testapp.surfaceplayback.SurfaceEntityPlaybackActivity
import androidx.xr.scenecore.testapp.transformation.TransformationActivity
import androidx.xr.scenecore.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.scenecore.testapp.visibility.VisibilityActivity
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private var session: Session? = null
    private val sessionManager = SessionManager(this)
    private var pendingPanelSize: IntSize2d? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createSessionAndSetupUi()

        setContent {
            IntegrationTestsAppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopBar() },
                    bottomBar = { BottomBar() },
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        BuildDetails()
                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                            items(TEST_GROUPS) { (category, tests) -> TestGroup(category, tests) }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TopBar() {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight().align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = { this@MainActivity.finish() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close the app",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                Text(
                    text = "JXR Scenecore Tests",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }

    @Composable
    private fun BottomBar() {
        Box(contentAlignment = Alignment.CenterStart) {
            BottomAppBar(
                actions = {},
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary,
                tonalElevation = 5.dp,
            )
        }
    }

    @Composable
    private fun BuildDetails() {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(11.dp)) {
                    val buildDate =
                        SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH)
                            .format(Build.TIME)
                    BuildInfoRowItem("Build Fingerprint: ", Build.FINGERPRINT)
                    BuildInfoRowItem("Build Device: ", Build.DEVICE)
                    BuildInfoRowItem("Build Date: ", buildDate)
                }
            }
        }
    }

    @Composable
    private fun BuildInfoRowItem(label: String, value: String) {
        Row {
            Text(
                label,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                modifier = Modifier.weight(3f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    data class TestCase(val labelResId: Int, val test: Int)

    @Composable
    private fun TestGroup(category: String, tests: List<TestCase>) {
        var expanded by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(
                            if (expanded) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary
                        )
                        .clickable { expanded = !expanded }
                        .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (expanded) "▼ $category" else "▶ $category",
                        modifier = Modifier.weight(1f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (expanded) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondary,
                    )
                    Text(
                        text = "${tests.size} Tests",
                        fontSize = 18.sp,
                        color =
                            if (expanded) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.outline,
                    )
                }
            }

            if (expanded) {
                tests.forEach { test ->
                    TestCaseColumnRowItem(test.labelResId) { runTest(test.test) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    @Composable
    private fun TestCaseColumnRowItem(labelResId: Int, onClick: () -> Unit) {
        TestCaseRowItem(labelResId, onClick)
        Spacer(
            modifier =
                Modifier.height(1.dp).background(MaterialTheme.colorScheme.secondary).fillMaxWidth()
        )
    }

    @Composable
    private fun TestCaseRowItem(labelResId: Int, onClick: () -> Unit) {
        Row(modifier = Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = labelResId),
                modifier = Modifier.weight(3.5f),
                fontSize = 22.sp,
                textAlign = TextAlign.Left,
            )
            Box(modifier = Modifier.weight(1.5f)) { TestCaseButton(onClick) }
        }
    }

    @Composable
    private fun TestCaseButton(onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.padding(2.dp).fillMaxWidth(),
            shape = RoundedCornerShape(3.dp),
            colors =
                ButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    disabledContentColor = Color.Gray,
                    disabledContainerColor = Color.DarkGray,
                ),
        ) {
            Text("Run Test", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }

    private fun createSessionAndSetupUi() {
        // Create the session in a separate thread to avoid StrictMode DiskRead Violations
        lifecycleScope.launch {
            val createdSession = withContext(Dispatchers.IO) { sessionManager.createSession() }
            if (createdSession == null) {
                finish()
            } else {
                session = createdSession
                val initialSize = FloatSize2d(1f, 1f)
                session?.scene?.mainPanelEntity?.size = initialSize
                session?.scene?.keyEntity = session?.scene?.mainPanelEntity
                setUpMainPanelMovable()
                pendingPanelSize?.let {
                    session?.scene?.mainPanelEntity?.sizeInPixels = it // restore panel size
                    pendingPanelSize = null // reset
                }
            }
        }
    }

    private fun setUpMainPanelMovable() {
        val movableComponent = MovableComponent.createSystemMovable(session!!)
        session!!.scene.mainPanelEntity.addComponent(movableComponent)
        val contentViewRoot = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        contentViewRoot.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            movableComponent.size = session!!.scene.mainPanelEntity.size.to3d()
        }
    }

    private fun runTest(index: Int) {
        when (index) {
            Tests.ACTIVITY_PANEL_TEST.test -> startActivity(createIntent<ActivityPanelActivity>())

            Tests.ANCHOR_TEST.test -> startActivity(createIntent<AnchorEntityActivity>())

            Tests.FIELD_OF_VIEW_VISIBILITY_TEST.test ->
                startActivity(createIntent<FieldOfViewVisibilityActivity>())

            Tests.SPATIAL_CAPABILITIES_TEST.test ->
                startActivity(createIntent<SpatialCapabilitiesActivity>())

            Tests.VISIBILITY_TEST.test -> startActivity(createIntent<VisibilityActivity>())

            Tests.FSM_HSM_TRANSITION_TEST.test -> {
                val intent = Intent(this@MainActivity, FsmHsmTransitionActivity::class.java)
                activityLauncher.launch(intent)
            }

            Tests.SPATIAL_USER_TEST.test -> startActivity(createIntent<SpatialUserActivity>())

            Tests.ENVIRONMENT_TEST.test -> startActivity(createIntent<EnvironmentActivity>())

            Tests.TRANSFORMATION_TEST.test -> startActivity(createIntent<TransformationActivity>())

            Tests.STANDALONE_TEST.test -> startActivity(createIntent<StandaloneActivity>())

            Tests.PANEL_ROUNDED_CORNER_TEST.test ->
                startActivity(createIntent<PanelRoundedCornerActivity>())

            Tests.SCENE_VIEWER_TEST.test -> startActivity(createIntent<SceneViewerActivity>())

            Tests.HEAD_LOCKED_UI_TEST.test -> startActivity(createIntent<HeadLockedUiActivity>())

            Tests.GLTF_MODEL_MATERIAL_TEXTURE_TEST.test ->
                startActivity(createIntent<GltfModelMaterialTextureActivity>())

            Tests.MOVABLE_PANEL_TEST.test -> startActivity(createIntent<MovableActivity>())

            Tests.INPUT_MOVE_RESIZE_1_TEST.test -> {
                val intent = Intent(this@MainActivity, InputMoveResizeTestActivity::class.java)
                intent.putExtra("MAIN_PANEL_TITLE", getString(R.string.cuj_input_move_resize_test))
                activityLauncher.launch(intent)
            }

            Tests.INPUT_MOVE_RESIZE_2_TEST.test -> {
                val intent = Intent(this@MainActivity, InputMoveResizeTestActivity::class.java)
                intent.putExtra("MAIN_PANEL_TITLE", getString(R.string.cuj_movable_component_test))
                activityLauncher.launch(intent)
            }

            Tests.INPUT_MOVE_RESIZE_3_TEST.test -> {
                val intent = Intent(this@MainActivity, InputMoveResizeTestActivity::class.java)
                intent.putExtra(
                    "MAIN_PANEL_TITLE",
                    getString(R.string.cuj_resizeable_component_test),
                )
                activityLauncher.launch(intent)
            }

            Tests.SPATIAL_AUDIO_1_TEST.test -> {
                val intent = createIntent<SpatialAudioActivity>()
                intent.putExtra("MAIN_PANEL_TITLE", getString(R.string.cuj_spatial_audio_test))
                startActivity(intent)
            }

            Tests.SPATIAL_AUDIO_2_TEST.test -> {
                val intent = createIntent<SpatialAudioActivity>()
                intent.putExtra(
                    "MAIN_PANEL_TITLE",
                    getString(R.string.cuj_spatial_audio_ambisonic_test),
                )
                startActivity(intent)
            }

            Tests.SPATIAL_AUDIO_3_TEST.test -> {
                val intent = createIntent<SpatialAudioActivity>()
                intent.putExtra(
                    "MAIN_PANEL_TITLE",
                    getString(R.string.cuj_spatial_audio_setting_pointsourceparams_test),
                )
                startActivity(intent)
            }

            Tests.PANEL_ROUNDED_CORNER_TEST.test ->
                startActivity(createIntent<PanelRoundedCornerActivity>())

            Tests.DIGITAL_HIT_TEST.test -> startActivity(createIntent<HitTestActivity>())

            Tests.ACCESSIBILITY_TEST.test ->
                startActivity(createIntent<AccessibilityTestActivity>())

            Tests.MEMORY_LEAK_TEST.test -> startActivity(createIntent<MemoryLeakActivity>())

            Tests.SURFACE_CUSTOM_MESH_TEST.test ->
                startActivity(createIntent<SurfaceEntityCustomMeshActivity>())

            Tests.SURFACE_ENTITY_IMAGE_TEST.test ->
                startActivity(createIntent<SurfaceEntityImageActivity>())

            Tests.SURFACE_INTERACTION_TEST.test ->
                startActivity(createIntent<SurfaceEntityInteractionActivity>())

            Tests.SURFACE_PLAYBACK_TEST.test ->
                startActivity(createIntent<SurfaceEntityPlaybackActivity>())

            Tests.PANEL_COORDINATES_TEST.test ->
                startActivity(createIntent<PanelCoordinateActivity>())

            Tests.GLTF_MODEL_ANIMATION_TEST.test ->
                startActivity(createIntent<GltfModelAnimationActivity>())

            else -> {
                Log.i(ACTIVITY_NAME, "DO_NOTHING")
            }
        }
    }

    private inline fun <reified T> createIntent(): Intent = Intent(this@MainActivity, T::class.java)

    // TODO: b/451293148 - Main Panel size is changed after a child activity with a resizable
    //  component resized the panel and finished.
    private val activityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            run {
                if (result.resultCode == RESULT_OK) {
                    val data: Intent? = result.data

                    val defaultPanelSizeWidth: Int? =
                        data?.getIntExtra("defaultPanelSizeWidth", 2048)
                    val defaultPanelSizeHeight: Int? =
                        data?.getIntExtra("defaultPanelSizeHeight", 1280)

                    if (defaultPanelSizeWidth != null && defaultPanelSizeHeight != null) {
                        val defaultPanelSize =
                            IntSize2d(defaultPanelSizeWidth, defaultPanelSizeHeight)
                        if (session == null) {
                            Log.d(
                                ACTIVITY_NAME,
                                "Session is null, pending size update: $defaultPanelSize",
                            )
                            pendingPanelSize = defaultPanelSize
                        } else {
                            Log.d(
                                ACTIVITY_NAME,
                                "Session exists, recover defaultPanelSize directly: $defaultPanelSize",
                            )
                            session?.scene?.mainPanelEntity?.sizeInPixels = defaultPanelSize
                        }
                    }
                }
            }
        }

    companion object {
        const val ACTIVITY_NAME = "MainActivity"

        private val TEST_GROUPS =
            listOf(
                "ENVIRONMENT & ANCHORS" to
                    listOf(
                        TestCase(R.string.cuj_standalone_test, Tests.STANDALONE_TEST.test),
                        TestCase(R.string.cuj_environment_test, Tests.ENVIRONMENT_TEST.test),
                        TestCase(R.string.cuj_anchor_test, Tests.ANCHOR_TEST.test),
                        TestCase(R.string.cuj_spatial_user_test, Tests.SPATIAL_USER_TEST.test),
                        TestCase(R.string.cuj_visibility_test, Tests.VISIBILITY_TEST.test),
                        TestCase(
                            R.string.cuj_field_of_view_visibility_test,
                            Tests.FIELD_OF_VIEW_VISIBILITY_TEST.test,
                        ),
                        TestCase(R.string.cuj_scene_viewer_test, Tests.SCENE_VIEWER_TEST.test),
                    ),
                "PANELS & UI" to
                    listOf(
                        TestCase(R.string.cuj_transformation_test, Tests.TRANSFORMATION_TEST.test),
                        TestCase(R.string.cuj_head_locked_ui_test, Tests.HEAD_LOCKED_UI_TEST.test),
                        TestCase(
                            R.string.cuj_panel_rounded_corner,
                            Tests.PANEL_ROUNDED_CORNER_TEST.test,
                        ),
                        TestCase(
                            R.string.cuj_panel_coordinates_test,
                            Tests.PANEL_COORDINATES_TEST.test,
                        ),
                        TestCase(R.string.cuj_activity_panel_test, Tests.ACTIVITY_PANEL_TEST.test),
                    ),
                "INTERACTION & MOVEMENT" to
                    listOf(
                        TestCase(
                            R.string.cuj_input_move_resize_test,
                            Tests.INPUT_MOVE_RESIZE_1_TEST.test,
                        ),
                        TestCase(
                            R.string.cuj_movable_component_test,
                            Tests.INPUT_MOVE_RESIZE_2_TEST.test,
                        ),
                        TestCase(
                            R.string.cuj_resizeable_component_test,
                            Tests.INPUT_MOVE_RESIZE_3_TEST.test,
                        ),
                        TestCase(R.string.cuj_movable_test, Tests.MOVABLE_PANEL_TEST.test),
                        TestCase(R.string.cuj_hit_test, Tests.DIGITAL_HIT_TEST.test),
                    ),
                "SURFACES & MEDIA" to
                    listOf(
                        TestCase(
                            R.string.cuj_surface_entity_interaction_test,
                            Tests.SURFACE_INTERACTION_TEST.test,
                        ),
                        TestCase(
                            R.string.cuj_surface_entity_playbacktest,
                            Tests.SURFACE_PLAYBACK_TEST.test,
                        ),
                        TestCase(
                            R.string.cuj_surface_entity_imagetest,
                            Tests.SURFACE_ENTITY_IMAGE_TEST.test,
                        ),
                        TestCase(
                            R.string.cuj_surface_entity_custom_mesh_test,
                            Tests.SURFACE_CUSTOM_MESH_TEST.test,
                        ),
                    ),
                "MODELS & AUDIO" to
                    listOf(
                        TestCase(
                            R.string.cuj_gltf_model_material_texture_test,
                            Tests.GLTF_MODEL_MATERIAL_TEXTURE_TEST.test,
                        ),
                        TestCase(
                            R.string.cuj_gltf_model_animation_test,
                            Tests.GLTF_MODEL_ANIMATION_TEST.test,
                        ),
                        TestCase(R.string.cuj_spatial_audio_test, Tests.SPATIAL_AUDIO_1_TEST.test),
                        TestCase(
                            R.string.cuj_spatial_audio_ambisonic_test,
                            Tests.SPATIAL_AUDIO_2_TEST.test,
                        ),
                        TestCase(
                            R.string.cuj_spatial_audio_setting_pointsourceparams_test,
                            Tests.SPATIAL_AUDIO_3_TEST.test,
                        ),
                    ),
                "SYSTEM & CAPABILITIES" to
                    listOf(
                        TestCase(
                            R.string.cuj_spatial_capabilities_test,
                            Tests.SPATIAL_CAPABILITIES_TEST.test,
                        ),
                        TestCase(
                            R.string.cuj_fsm_hsm_transition_test,
                            Tests.FSM_HSM_TRANSITION_TEST.test,
                        ),
                        TestCase(R.string.cuj_accessibility_test, Tests.ACCESSIBILITY_TEST.test),
                        TestCase(R.string.dev_memory_leak_test, Tests.MEMORY_LEAK_TEST.test),
                    ),
            )
    }

    enum class Tests(val test: Int) {
        STANDALONE_TEST(0),
        ENVIRONMENT_TEST(1),
        ANCHOR_TEST(2),
        FIELD_OF_VIEW_VISIBILITY_TEST(3),
        TRANSFORMATION_TEST(4),
        HEAD_LOCKED_UI_TEST(5),
        SCENE_VIEWER_TEST(6),
        FSM_HSM_TRANSITION_TEST(7),
        ACTIVITY_PANEL_TEST(8),
        INPUT_MOVE_RESIZE_1_TEST(9),
        INPUT_MOVE_RESIZE_2_TEST(10),
        INPUT_MOVE_RESIZE_3_TEST(11),
        GLTF_MODEL_MATERIAL_TEXTURE_TEST(12),
        MOVABLE_PANEL_TEST(13),
        SPATIAL_USER_TEST(14),
        VISIBILITY_TEST(15),
        SPATIAL_CAPABILITIES_TEST(16),
        SPATIAL_AUDIO_1_TEST(17),
        SPATIAL_AUDIO_2_TEST(18),
        SPATIAL_AUDIO_3_TEST(19),
        PANEL_ROUNDED_CORNER_TEST(20),
        DIGITAL_HIT_TEST(21),
        ACCESSIBILITY_TEST(22),
        MEMORY_LEAK_TEST(23),
        SURFACE_INTERACTION_TEST(24),
        SURFACE_PLAYBACK_TEST(25),
        SURFACE_ENTITY_IMAGE_TEST(26),
        PANEL_COORDINATES_TEST(27),
        GLTF_MODEL_ANIMATION_TEST(28),
        SURFACE_CUSTOM_MESH_TEST(29),
    }
}
