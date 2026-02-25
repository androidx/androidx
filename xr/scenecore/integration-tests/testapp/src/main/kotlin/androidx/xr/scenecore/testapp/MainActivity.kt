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
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
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
import androidx.xr.scenecore.testapp.ui.BuildInfoRecyclerViewAdapter
import androidx.xr.scenecore.testapp.ui.TestCasesRecyclerViewAdapter
import androidx.xr.scenecore.testapp.visibility.VisibilityActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var session: Session? = null

    private val sessionManager = SessionManager(this)
    private var pendingPanelSize: FloatSize2d? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        createSessionAndSetupUi()

        // Top bar
        createTopToolBarView()

        // Build info
        createBuildInfoRecyclerView()

        // Test cases & bottom bar
        createTestCasesRecyclerView()
    }

    private fun createSessionAndSetupUi() {
        // Create the session in a separate thread to avoid StrictMode DiskRead Violations
        lifecycleScope.launch {
            val createdSession = withContext(Dispatchers.IO) { sessionManager.createSession() }
            if (createdSession == null) {
                finish()
            } else {
                session = createdSession
                session?.scene?.keyEntity = session?.scene?.mainPanelEntity
                setUpMainPanelMovable()
                pendingPanelSize?.let {
                    session?.scene?.mainPanelEntity?.size = it // restore panel size
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

    private fun createTopToolBarView() {
        // Set toolbar
        val toolbar: Toolbar = findViewById(R.id.top_app_bar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { this.finish() }
    }

    private fun createBuildInfoRecyclerView() {
        // Set build info
        val infoDataSet =
            arrayOf(
                getString(R.string.build_fingerprint_label),
                getString(R.string.build_hardware_label),
                getString(R.string.build_date_label),
            )
        val buildInfoAdapter = BuildInfoRecyclerViewAdapter(infoDataSet)
        val buildInfoView: RecyclerView = findViewById(R.id.build_info_recycler)
        buildInfoView.layoutManager = LinearLayoutManager(this)
        buildInfoView.adapter = buildInfoAdapter
    }

    private fun createTestCasesRecyclerView() {
        // Set test text and buttons
        val dataset =
            arrayOf(
                getString(R.string.cuj_standalone_test),
                getString(R.string.cuj_environment_test),
                getString(R.string.cuj_anchor_test),
                getString(R.string.cuj_field_of_view_visibility_test),
                getString(R.string.cuj_transformation_test),
                getString(R.string.cuj_head_locked_ui_test),
                getString(R.string.cuj_scene_viewer_test),
                getString(R.string.cuj_fsm_hsm_transition_test),
                getString(R.string.cuj_activity_panel_test),
                getString(R.string.cuj_input_move_resize_test),
                getString(R.string.cuj_movable_component_test),
                getString(R.string.cuj_resizeable_component_test),
                getString(R.string.cuj_gltf_model_material_texture_test),
                getString(R.string.cuj_movable_test),
                getString(R.string.cuj_spatial_user_test),
                getString(R.string.cuj_visibility_test),
                getString(R.string.cuj_spatial_capabilities_test),
                getString(R.string.cuj_spatial_audio_test),
                getString(R.string.cuj_spatial_audio_ambisonic_test),
                getString(R.string.cuj_spatial_audio_setting_pointsourceparams_test),
                getString(R.string.cuj_panel_rounded_corner),
                getString(R.string.cuj_hit_test),
                getString(R.string.cuj_accessibility_test),
                getString(R.string.dev_memory_leak_test),
                getString(R.string.cuj_surface_entity_interaction_test),
                getString(R.string.cuj_surface_entity_playbacktest),
                getString(R.string.cuj_surface_entity_imagetest),
                getString(R.string.cuj_panel_coordinates_test),
                getString(R.string.cuj_gltf_model_animation_test),
                getString(R.string.cuj_surface_entity_custom_mesh_test),
            )
        val customAdapter = TestCasesRecyclerViewAdapter(dataset)
        val recyclerView: RecyclerView = findViewById(R.id.cuj_buttons_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = customAdapter
        customAdapter.setOnClickListener(
            object : TestCasesRecyclerViewAdapter.OnClickListener {
                override fun onClick(position: Int) {
                    runTest(position)
                }
            }
        )
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

                    val defaultPanelSizeWidth: Float? =
                        data?.getFloatExtra("defaultPanelSizeWidth", 1.28f)
                    val defaultPanelSizeHeight: Float? =
                        data?.getFloatExtra("defaultPanelSizeHeight", 0.8f)

                    if (defaultPanelSizeWidth != null && defaultPanelSizeHeight != null) {
                        val defaultPanelSize =
                            FloatSize2d(defaultPanelSizeWidth, defaultPanelSizeHeight)
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
                            session?.scene?.mainPanelEntity?.size = defaultPanelSize
                        }
                    }
                }
            }
        }

    companion object {
        const val ACTIVITY_NAME = "MainActivity"
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
