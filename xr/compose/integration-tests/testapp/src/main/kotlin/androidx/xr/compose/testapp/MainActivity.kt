/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.xr.compose.testapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.accessibility.AccessibilityActivity
import androidx.xr.compose.testapp.animation.Animation
import androidx.xr.compose.testapp.curvedlayout.CurvedLayout
import androidx.xr.compose.testapp.depthstacking.DepthStacking
import androidx.xr.compose.testapp.focuschange.FSMFocusChangeActivity
import androidx.xr.compose.testapp.focuschange.HSMFocusChangeActivity
import androidx.xr.compose.testapp.followingsubspace.AnchorFollowingSubspaceActivity
import androidx.xr.compose.testapp.followingsubspace.FollowingSubspaceActivity
import androidx.xr.compose.testapp.fragments.FragmentCompatibilityActivity
import androidx.xr.compose.testapp.gravityaligned.GravityAlignedActivity
import androidx.xr.compose.testapp.lifecycle.LifecycleDataStore
import androidx.xr.compose.testapp.lifecycle.OpenCloseActivity
import androidx.xr.compose.testapp.lifecycle.ResizeActivity
import androidx.xr.compose.testapp.lifecycle.RuntimeSessionActivity
import androidx.xr.compose.testapp.modechange.ModeChange
import androidx.xr.compose.testapp.movable.MovableActivity
import androidx.xr.compose.testapp.movablescalable.MovableScalable
import androidx.xr.compose.testapp.panelembeddedsubspace.PanelEmbeddedSubspace
import androidx.xr.compose.testapp.panelvolume.PanelVolume
import androidx.xr.compose.testapp.performance.LayoutPerformance
import androidx.xr.compose.testapp.permissionsdialog.PermissionsDialog
import androidx.xr.compose.testapp.pose.Pose
import androidx.xr.compose.testapp.resizablepanel.ResizablePanel
import androidx.xr.compose.testapp.rotatetolookatuser.RotateToLookAtUserActivity
import androidx.xr.compose.testapp.rotation.Rotation
import androidx.xr.compose.testapp.rtlawareness.RtlAwareSubspaceModifierActivity
import androidx.xr.compose.testapp.spacemodechange.SpaceModeActivity
import androidx.xr.compose.testapp.spatialalignmentusage.SpatialAlignmentUsageActivity
import androidx.xr.compose.testapp.spatialarrangementusage.SpatialArrangementUsageActivity
import androidx.xr.compose.testapp.spatialcompose.SpatialCompose
import androidx.xr.compose.testapp.spatialelevation.SpatialElevation
import androidx.xr.compose.testapp.spatialgltfmodel.SpatialGltfModelActivity
import androidx.xr.compose.testapp.spatialpanel.SpatialPanelActivity
import androidx.xr.compose.testapp.splitengine.SplitEngine
import androidx.xr.compose.testapp.ui.components.FpsCounterScreen
import androidx.xr.compose.testapp.ui.components.TestCaseButton
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple40
import androidx.xr.compose.testapp.ui.theme.Purple80
import androidx.xr.compose.testapp.videoplayer.VideoPlayerActivity
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        obtainUserPermissions()
        setContent {
            Subspace {
                SpatialPanel(
                    modifier = SubspaceModifier.width(800.dp).height(1000.dp).movable(),
                    resizePolicy = ResizePolicy(),
                ) {
                    IntegrationTestsAppTheme {
                        val scrollBehavior =
                            TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                        Scaffold(
                            modifier =
                                Modifier.fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                            topBar = { TopBar(scrollBehavior) },
                            bottomBar = { BottomBar() },
                        ) { innerPadding ->
                            Column(modifier = Modifier.padding(innerPadding)) {
                                BuildDetails()
                                TestCases()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun obtainUserPermissions() {
        val permissionsLauncher =
            super.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
                if (permissions.values.contains(false)) {
                    Toast.makeText(this, "Missing required permissions", Toast.LENGTH_LONG).show()
                    this.finish()
                }
            }
        permissionsLauncher.launch(
            arrayOf(
                SCENE_UNDERSTANDING_PERMISSION,
                HAND_TRACKING_PERMISSION,
                READ_MEDIA_VIDEO_PERMISSION,
                POST_NOTIFICATIONS_PERMISSION,
            )
        )
    }

    companion object {
        const val HAND_TRACKING_PERMISSION = "android.permission.HAND_TRACKING"
        const val SCENE_UNDERSTANDING_PERMISSION = "android.permission.SCENE_UNDERSTANDING_COARSE"
        const val READ_MEDIA_VIDEO_PERMISSION = "android.permission.READ_MEDIA_VIDEO"
        const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TopBar(scrollBehavior: TopAppBarScrollBehavior) {
        CenterAlignedTopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Purple40,
                    titleContentColor = Color.White,
                ),
            title = {
                Text(
                    "JXR Compose Tests",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 32.sp,
                )
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.jetpack_compose),
                        contentDescription = "Jetpack Compose",
                        tint = Color.Unspecified,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { this@MainActivity.finish() }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close the app",
                        Modifier.size(36.dp),
                        tint = Color.White,
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }

    @Composable
    private fun BottomBar() {
        Box(contentAlignment = Alignment.CenterStart) {
            BottomAppBar(
                actions = {},
                contentColor = Color.White,
                containerColor = Purple40,
                tonalElevation = 5.dp,
            )
            FpsCounterScreen()
        }
    }

    @Composable
    private fun BuildDetails() {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().background(color = Color.DarkGray)) {
                Column(modifier = Modifier.padding(10.dp)) {
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
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(value, modifier = Modifier.weight(3f))
        }
    }

    @Composable
    private fun TestCases() {
        val context = LocalContext.current

        val groups =
            remember(context) {
                listOf(
                    "Video & Media" to
                        listOf(
                            TestCase(getString(R.string.video_player_test)) {
                                startTest<VideoPlayerActivity>()
                            },
                            TestCase(getString(R.string.video_drm_test)) {
                                startTest<VideoPlayerActivity>()
                            },
                            TestCase(getString(R.string.video_spatial_test)) {
                                startTest<SpatialCompose>()
                            },
                            TestCase(getString(R.string.video_spatial_180_360_test)) {
                                startTest<SpatialCompose>()
                            },
                            TestCase(getString(R.string.video_in_panel_test)) {
                                startTest<SpatialCompose>(getString(R.string.video_in_panel_test))
                            },
                        ),
                    "Spatial Entities & Models" to
                        listOf(
                            TestCase(getString(R.string.cuj_gltf_animation_test_case)) {
                                startTest<SplitEngine>(
                                    getString(R.string.cuj_gltf_animation_test_case)
                                )
                            },
                            TestCase(getString(R.string.cuj_gltf_entity_input_test_case)) {
                                startTest<SplitEngine>(
                                    getString(R.string.cuj_gltf_entity_input_test_case)
                                )
                            },
                            TestCase(getString(R.string.spatial_gltf_model_test)) {
                                startTest<SpatialGltfModelActivity>()
                            },
                        ),
                    "Tracking" to
                        listOf(
                            TestCase(getString(R.string.anchor_subspace_app_test)) {
                                startTest<AnchorFollowingSubspaceActivity>()
                            },
                            TestCase(getString(R.string.ardevice_subspace_test_case)) {
                                startTest<FollowingSubspaceActivity>()
                            },
                            TestCase(getString(R.string.rotatetolookatuser_test_case)) {
                                startTest<RotateToLookAtUserActivity>()
                            },
                        ),
                    "Layout" to
                        listOf(
                            TestCase(getString(R.string.spatial_elevation_test)) {
                                startTest<SpatialElevation>()
                            },
                            TestCase(getString(R.string.spatial_layout_test)) {
                                startTest<SpatialCompose>(getString(R.string.spatial_layout_test))
                            },
                            TestCase(getString(R.string.curve_panel_row_test)) {
                                startTest<CurvedLayout>()
                            },
                            TestCase(getString(R.string.movable_panels_test)) {
                                startTest<MovableActivity>()
                            },
                            TestCase(getString(R.string.movable_scalable_panel_test)) {
                                startTest<MovableScalable>()
                            },
                            TestCase(getString(R.string.panel_rotation_test)) {
                                startTest<Rotation>()
                            },
                            TestCase(getString(R.string.backhandling_panel_test)) {
                                startTest<SpatialCompose>(
                                    getString(R.string.backhandling_panel_test)
                                )
                            },
                            TestCase(getString(R.string.pose_test)) { startTest<Pose>() },
                            TestCase(getString(R.string.gravity_aligned_test_case)) {
                                startTest<GravityAlignedActivity>()
                            },
                        ),
                    "Interaction" to
                        listOf(
                            TestCase(getString(R.string.mode_change_test)) {
                                startTest<ModeChange>()
                            },
                            TestCase(getString(R.string.value_based_animation_test)) {
                                startTest<Animation>()
                            },
                            TestCase(getString(R.string.enable_permission_dialog_test)) {
                                startTest<PermissionsDialog>()
                            },
                            TestCase(getString(R.string.accessibility_test)) {
                                startTest<AccessibilityActivity>()
                            },
                        ),
                    "Lifecycle" to
                        listOf(
                            TestCase(getString(R.string.lifecycle_open_close_test)) {
                                LifecycleDataStore.clearAllData(context)
                                startTest<OpenCloseActivity>()
                            },
                            TestCase(getString(R.string.lifecycle_resize_test)) {
                                LifecycleDataStore.clearAllData(context)
                                startTest<ResizeActivity>()
                            },
                            TestCase(getString(R.string.lifecycle_runtime_session_test)) {
                                startTest<RuntimeSessionActivity>()
                            },
                            TestCase(getString(R.string.space_mode_change_test)) {
                                startTest<SpaceModeActivity>()
                            },
                            TestCase(getString(R.string.spatial_panel_test)) {
                                startTest<SpatialPanelActivity>()
                            },
                            TestCase(getString(R.string.hsm_focus_change_test)) {
                                startTest<HSMFocusChangeActivity>()
                            },
                            TestCase(getString(R.string.fsm_focus_change_test)) {
                                startTest<FSMFocusChangeActivity>()
                            },
                        ),
                    "Developer Tests" to
                        listOf(
                            TestCase(getString(R.string.fragment_compatibility_test)) {
                                startTest<FragmentCompatibilityActivity>()
                            },
                            TestCase(getString(R.string.depthstacking_modifier_order_test_case)) {
                                startTest<DepthStacking>()
                            },
                            TestCase(getString(R.string.panel_embedded_subspace_test_case)) {
                                startTest<PanelEmbeddedSubspace>()
                            },
                            TestCase(getString(R.string.panel_volume_test_case)) {
                                startTest<PanelVolume>()
                            },
                            TestCase(getString(R.string.resizable_panel_test_case)) {
                                startTest<ResizablePanel>()
                            },
                            TestCase(getString(R.string.spatial_alignment_usage_test_case)) {
                                startTest<SpatialAlignmentUsageActivity>()
                            },
                            TestCase(getString(R.string.spatial_arrangement_usage_test_case)) {
                                startTest<SpatialArrangementUsageActivity>()
                            },
                            TestCase(
                                getString(R.string.subspace_modifiers_rtl_awareness_test_case)
                            ) {
                                startTest<RtlAwareSubspaceModifierActivity>()
                            },
                            TestCase(getString(R.string.layout_performance)) {
                                startTest<LayoutPerformance>()
                            },
                        ),
                )
            }

        LazyColumn(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            items(groups) { (category, tests) -> TestGroup(category, tests) }
        }
    }

    data class TestCase(val label: String, val onClick: () -> Unit)

    @Composable
    private fun TestGroup(category: String, tests: List<TestCase>) {
        var expanded by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(if (expanded) Purple40 else Color.LightGray)
                        .clickable { expanded = !expanded }
                        .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (expanded) "▼ $category" else "▶ $category",
                        modifier = Modifier.weight(1f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (expanded) Color.White else Color.Black,
                    )
                    Text(
                        text = "${tests.size} Tests",
                        fontSize = 18.sp,
                        color = if (expanded) Color.White else Color.DarkGray,
                    )
                }
            }

            if (expanded) {
                tests.forEach { test -> TestCaseColumnRowItem(test.label, test.onClick) }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    @Composable
    private fun TestCaseColumnRowItem(label: String, onClick: () -> Unit) {
        TestCaseRowItem(label, onClick)
        Spacer(modifier = Modifier.height(1.dp).background(Purple80).fillMaxWidth())
    }

    @Composable
    private fun TestCaseRowItem(label: String, onClick: () -> Unit) {
        Row(modifier = Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                modifier = Modifier.weight(3.5f),
                fontSize = 22.sp,
                textAlign = TextAlign.Left,
            )
            Box(modifier = Modifier.weight(1.5f)) { TestCaseButton("Run Test", onClick) }
        }
    }

    private inline fun <reified T> startTest(title: String? = null) {
        val intent = Intent(this@MainActivity, T::class.java)
        if (title != null) {
            intent.putExtra("TITLE", title)
        }
        startActivity(intent)
    }
}
