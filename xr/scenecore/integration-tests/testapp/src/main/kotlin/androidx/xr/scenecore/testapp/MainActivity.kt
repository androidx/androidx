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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.xr.runtime.Session
import androidx.xr.scenecore.testapp.accessibilitytest.AccessibilityTestActivity
import androidx.xr.scenecore.testapp.activitypanel.ActivityPanelActivity
import androidx.xr.scenecore.testapp.anchorentity.AnchorEntityActivity
import androidx.xr.scenecore.testapp.common.createSession
import androidx.xr.scenecore.testapp.environment.EnvironmentActivity
import androidx.xr.scenecore.testapp.fieldofviewvisibility.FieldOfViewVisibilityActivity
import androidx.xr.scenecore.testapp.fsmhsmtransition.FsmHsmTransitionActivity
import androidx.xr.scenecore.testapp.headlockedui.HeadLockedUiActivity
import androidx.xr.scenecore.testapp.hittest.HitTestActivity
import androidx.xr.scenecore.testapp.inputmoveresize.InputMoveResizeTestActivity
import androidx.xr.scenecore.testapp.movable.MovableActivity
import androidx.xr.scenecore.testapp.panelroundedcorner.PanelRoundedCornerActivity
import androidx.xr.scenecore.testapp.sceneviewer.SceneViewerActivity
import androidx.xr.scenecore.testapp.spatialaudio.SpatialAudioActivity
import androidx.xr.scenecore.testapp.spatialcapabilities.SpatialCapabilitiesActivity
import androidx.xr.scenecore.testapp.spatialuser.SpatialUserActivity
import androidx.xr.scenecore.testapp.standalone.StandaloneActivity
import androidx.xr.scenecore.testapp.transformation.TransformationActivity
import androidx.xr.scenecore.testapp.ui.BuildInfoRecyclerViewAdapter
import androidx.xr.scenecore.testapp.ui.TestCasesRecyclerViewAdapter
import androidx.xr.scenecore.testapp.visibility.VisibilityActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val session: Session? = createSession(this)
        if (session == null) this.finish()

        // Top bar
        createTopToolBarView()

        // Build info
        createBuildInfoRecyclerView()

        // Test cases & bottom bar
        createTestCasesRecyclerView()
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

            Tests.FSM_HSM_TRANSITION_TEST.test ->
                startActivity(createIntent<FsmHsmTransitionActivity>())

            Tests.SPATIAL_USER_TEST.test -> startActivity(createIntent<SpatialUserActivity>())

            Tests.ENVIRONMENT_TEST.test -> startActivity(createIntent<EnvironmentActivity>())

            Tests.TRANSFORMATION_TEST.test -> startActivity(createIntent<TransformationActivity>())

            Tests.STANDALONE_TEST.test -> startActivity(createIntent<StandaloneActivity>())

            Tests.PANEL_ROUNDED_CORNER_TEST.test ->
                startActivity(createIntent<PanelRoundedCornerActivity>())

            Tests.SCENE_VIEWER_TEST.test -> startActivity(createIntent<SceneViewerActivity>())

            Tests.HEAD_LOCKED_UI_TEST.test -> startActivity(createIntent<HeadLockedUiActivity>())

            Tests.MOVABLE_PANEL_TEST.test -> startActivity(createIntent<MovableActivity>())

            Tests.INPUT_MOVE_RESIZE_1_TEST.test -> {
                val intent = createIntent<InputMoveResizeTestActivity>()
                intent.putExtra("MAIN_PANEL_TITLE", getString(R.string.cuj_input_move_resize_test))
                startActivity(intent)
            }

            Tests.INPUT_MOVE_RESIZE_2_TEST.test -> {
                val intent = createIntent<InputMoveResizeTestActivity>()
                intent.putExtra("MAIN_PANEL_TITLE", getString(R.string.cuj_movable_component_test))
                startActivity(intent)
            }

            Tests.INPUT_MOVE_RESIZE_3_TEST.test -> {
                val intent = createIntent<InputMoveResizeTestActivity>()
                intent.putExtra(
                    "MAIN_PANEL_TITLE",
                    getString(R.string.cuj_resizeable_component_test),
                )
                startActivity(intent)
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

            else -> {
                Log.i(ACTIVITY_NAME, "DO_NOTHING")
            }
        }
    }

    private inline fun <reified T> createIntent(): Intent = Intent(this@MainActivity, T::class.java)

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
        MOVABLE_PANEL_TEST(12),
        SPATIAL_USER_TEST(13),
        VISIBILITY_TEST(14),
        SPATIAL_CAPABILITIES_TEST(15),
        SPATIAL_AUDIO_1_TEST(16),
        SPATIAL_AUDIO_2_TEST(17),
        SPATIAL_AUDIO_3_TEST(18),
        PANEL_ROUNDED_CORNER_TEST(19),
        DIGITAL_HIT_TEST(20),
        ACCESSIBILITY_TEST(21),
    }
}
