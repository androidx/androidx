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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.ApplicationSubspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.animation.Animation
import androidx.xr.compose.testapp.curvedlayout.CurvedLayout
import androidx.xr.compose.testapp.depthstacking.DepthStacking
import androidx.xr.compose.testapp.modechange.ModeChange
import androidx.xr.compose.testapp.movable.MovableActivity
import androidx.xr.compose.testapp.movablescalable.MovableScalable
import androidx.xr.compose.testapp.nestedsubspace.NestedSubspace
import androidx.xr.compose.testapp.panelvolume.PanelVolume
import androidx.xr.compose.testapp.permissionsdialog.PermissionsDialog
import androidx.xr.compose.testapp.resizablepanel.ResizablePanel
import androidx.xr.compose.testapp.rotation.Rotation
import androidx.xr.compose.testapp.spatialcompose.SpatialCompose
import androidx.xr.compose.testapp.spatialelevation.SpatialElevation
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
            ApplicationSubspace {
                SpatialPanel(
                    modifier = SubspaceModifier.width(800.dp).height(750.dp).movable().resizable()
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
            arrayOf(SCENE_UNDERSTANDING_PERMISSION, HAND_TRACKING_PERMISSION)
        )
    }

    companion object {
        const val HAND_TRACKING_PERMISSION = "android.permission.HAND_TRACKING"
        const val SCENE_UNDERSTANDING_PERMISSION = "android.permission.SCENE_UNDERSTANDING_COARSE"
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

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())) {
                    TestCaseColumnRowItem(getString(R.string.video_player_test)) {
                        startTest<VideoPlayerActivity>()
                    }
                    TestCaseColumnRowItem(getString(R.string.cuj_gltf_animation_test_case)) {
                        startTest<SplitEngine>(getString(R.string.cuj_gltf_animation_test_case))
                    }
                    TestCaseColumnRowItem(getString(R.string.cuj_gltf_entity_input_test_case)) {
                        startTest<SplitEngine>(getString(R.string.cuj_gltf_entity_input_test_case))
                    }

                    TestCaseColumnRowItem(getString(R.string.spatial_elevation_test)) {
                        startTest<SpatialElevation>()
                    }

                    TestCaseColumnRowItem(getString(R.string.spatial_layout_test)) {
                        startTest<SpatialCompose>(getString(R.string.spatial_layout_test))
                    }
                    TestCaseColumnRowItem(getString(R.string.video_in_panel_test)) {
                        startTest<SpatialCompose>(getString(R.string.video_in_panel_test))
                    }
                    TestCaseColumnRowItem(getString(R.string.backhandling_panel_test)) {
                        startTest<SpatialCompose>(getString(R.string.backhandling_panel_test))
                    }

                    TestCaseColumnRowItem(getString(R.string.mode_change_test)) {
                        startTest<ModeChange>()
                    }
                    TestCaseColumnRowItem(getString(R.string.value_based_animation_test)) {
                        startTest<Animation>()
                    }
                    TestCaseColumnRowItem(getString(R.string.panel_rotation_test)) {
                        startTest<Rotation>()
                    }
                    TestCaseColumnRowItem(getString(R.string.curve_panel_row_test)) {
                        startTest<CurvedLayout>()
                    }
                    TestCaseColumnRowItem(getString(R.string.movable_panels_test)) {
                        startTest<MovableActivity>()
                    }
                    TestCaseColumnRowItem(getString(R.string.enable_permission_dialog_test)) {
                        startTest<PermissionsDialog>()
                    }
                    TestCaseColumnRowItem(getString(R.string.movable_scalable_panel_test)) {
                        startTest<MovableScalable>()
                    }
                    TestCaseBlankRow("THE FOLLOWING ARE JXR COMPOSE DEVELOPER TESTS")
                    TestCaseColumnRowItem(
                        getString(R.string.depthstacking_modifier_order_test_case)
                    ) {
                        startTest<DepthStacking>()
                    }
                    TestCaseColumnRowItem(getString(R.string.nested_subspace_test_case)) {
                        startTest<NestedSubspace>()
                    }
                    TestCaseColumnRowItem(getString(R.string.panel_volume_test_case)) {
                        startTest<PanelVolume>()
                    }
                    TestCaseColumnRowItem(getString(R.string.resizable_panel_test_case)) {
                        startTest<ResizablePanel>()
                    }
                }
            }
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

    @Composable
    private fun TestCaseBlankRow(label: String) {
        Box(modifier = Modifier.background(Color.LightGray)) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    modifier = Modifier.weight(3.5f),
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                )
            }
        }
        Spacer(modifier = Modifier.height(1.dp).background(Purple80).fillMaxWidth())
    }

    private inline fun <reified T> startTest(title: String? = null) {
        val intent = Intent(this@MainActivity, T::class.java)
        if (title != null) {
            intent.putExtra("TITLE", title)
        }
        startActivity(intent)
    }
}
