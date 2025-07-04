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

package androidx.xr.compose.testapp.spatialcompose

import android.content.Intent
import android.graphics.Color.BLACK
import android.graphics.Color.LTGRAY
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.LocalSpatialConfiguration
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ExperimentalSubspaceVolumeApi
import androidx.xr.compose.subspace.SceneCoreEntity
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.SpatialAndroidViewPanel
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.PlaneOrientation
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.anchorable
import androidx.xr.compose.subspace.layout.depth
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.common.AnotherActivity
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.components.TestDialog
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import java.nio.file.Paths
import java.time.Clock
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

class SpatialCompose : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 2D Content rendered to the MainPanel
            MainPanelContent()

            // 3D Content
            Subspace {
                PanelGrid()
                XyzArrows(
                    SubspaceModifier.width(.5.meters.toDp())
                        .height(0.5.meters.toDp())
                        .depth(0.5.meters.toDp())
                        .offset(x = 1.meters.toDp(), z = -0.5.meters.toDp())
                )
            }
        }

        isDebugInspectorInfoEnabled = true
    }

    @Composable
    fun MainPanelContent() {
        var title = intent.getStringExtra("TITLE")
        if (title == null) title = "Spatial Compose Test"
        CommonTestScaffold(
            title = title,
            showBottomBar = true,
            onClickBackArrow = { this@SpatialCompose.finish() },
            onClickRecreate = { this@SpatialCompose.recreate() },
        ) {
            PanelContent {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Panel Center - main task window")
                    val isSpatialUiEnabled = LocalSpatialCapabilities.current.isSpatialUiEnabled
                    val config = LocalSpatialConfiguration.current

                    Button(
                        onClick = {
                            if (isSpatialUiEnabled) {
                                config.requestHomeSpaceMode()
                            } else {
                                config.requestFullSpaceMode()
                            }
                        }
                    ) {
                        Text("Switch Space Mode")
                    }

                    Button(
                        onClick = {
                            val intent =
                                Intent(this@SpatialCompose, SpatialComposeVideoPlayer::class.java)
                            startActivity(intent)
                        }
                    ) {
                        Text("Launch Video Player")
                    }

                    Button(
                        onClick = {
                            val intent =
                                Intent(this@SpatialCompose, SpatialComposeWindowManager::class.java)
                            startActivity(intent)
                        }
                    ) {
                        Text("Launch Window Manager JXR Test")
                    }

                    Button(
                        onClick = {
                            val intent =
                                Intent(this@SpatialCompose, SpatialComposeStateTest::class.java)
                            startActivity(intent)
                        }
                    ) {
                        Text("Launch Application State Test")
                    }
                }
            }
        }
    }

    @Composable
    @SubspaceComposable
    fun PanelGrid() {
        val sidePanelModifier = SubspaceModifier.fillMaxWidth().height(200.dp)
        val curveRadius = 1025.dp
        SpatialColumn(SubspaceModifier.testTag("PanelGridColumn")) {
            SpatialCurvedRow(
                modifier = SubspaceModifier.width(2000.dp).height(1200.dp).testTag("PanelGridRow"),
                alignment = SpatialAlignment.BottomCenter,
                curveRadius = curveRadius,
            ) {
                SpatialColumn(
                    modifier = SubspaceModifier.width(200.dp).fillMaxHeight().testTag("LeftColumn")
                ) {
                    Orbiter(
                        position = ContentEdge.Start,
                        offset = 8.dp,
                        offsetType = OrbiterOffsetType.InnerEdge,
                        shape = SpatialRoundedCornerShape(CornerSize(16.dp)),
                    ) {
                        Surface {
                            Text(
                                text = "Subspace Orbiter",
                                modifier = Modifier.width(80.dp).padding(8.dp),
                            )
                        }
                    }

                    AppPanel(modifier = sidePanelModifier, text = "Panel Top Left")
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(40.dp))
                    AnchorPanel(
                        modifier = SubspaceModifier.height(200.dp),
                        text = "Anchorable Panel",
                    )
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(40.dp))
                    ViewBasedAppPanel(
                        modifier = sidePanelModifier,
                        text = "Panel Bottom Left (View)",
                    )
                }
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(800.dp)
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp)
                            .testTag("CenterColumn"),
                    alignment = SpatialAlignment.TopCenter,
                ) {
                    SpatialMainPanel(modifier = SubspaceModifier.fillMaxWidth().height(600.dp))
                    val intent = Intent(this@SpatialCompose, AnotherActivity::class.java)
                    intent.putExtra("SHOW_BOTTOM_BAR", true)
                    intent.putExtra("TITLE", "Top Bar")
                    intent.putExtra("BOTTOM_BAR_TEXT", "Bottom Bar")
                    SpatialActivityPanel(
                        intent = intent,
                        modifier =
                            SubspaceModifier.width(800.dp)
                                .height(600.dp)
                                .movable(true)
                                .testTag("ActivityPanel"),
                    )
                }
                SpatialColumn(
                    modifier = SubspaceModifier.width(200.dp).fillMaxHeight().testTag("RightColumn")
                ) {
                    AppPanel(modifier = sidePanelModifier, text = "Panel Top Right")
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(40.dp))
                    AppPanel(modifier = sidePanelModifier, text = "Panel Bottom Right")
                }
            }
        }
    }

    @SubspaceComposable
    @Composable
    fun AppPanel(modifier: SubspaceModifier = SubspaceModifier, text: String = "") {
        var moveResizeLocked by remember { mutableStateOf(true) }
        SpatialPanel(
            modifier =
                modifier
                    .testTag(text)
                    .movable(enabled = !moveResizeLocked)
                    .resizable(enabled = !moveResizeLocked)
        ) {
            PanelContent { Text(text) }

            Orbiter(
                position = ContentEdge.Bottom,
                offset = 24.dp,
                shape = SpatialRoundedCornerShape(size = CornerSize(50)),
                shouldRenderInNonSpatial = false,
            ) {
                IconToggleButton(
                    checked = moveResizeLocked,
                    onCheckedChange = { moveResizeLocked = !moveResizeLocked },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription =
                            if (moveResizeLocked) "Enable Move/Resize" else "Disable Move/Resize",
                    )
                }
            }
        }
    }

    @SubspaceComposable
    @Composable
    fun AnchorPanel(modifier: SubspaceModifier = SubspaceModifier, text: String = "") {
        // TODO(b/424834805): It's possible to have multiple movable overloads in place which are
        // not compatible with each other.
        SpatialPanel(
            modifier = modifier.anchorable(anchorPlaneOrientations = setOf(PlaneOrientation.Any))
        ) {
            Column(
                modifier = Modifier.background(Color.LightGray).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text)
            }
        }
    }

    @Composable
    fun PanelContent(content: @Composable () -> Unit) {
        var showArrows by remember { mutableStateOf(false) }
        var addHighlight by remember { mutableStateOf(false) }
        val borderWidth by remember { derivedStateOf { if (addHighlight) 3.dp else 0.dp } }
        Column(
            modifier =
                Modifier.background(Color.LightGray)
                    .fillMaxSize()
                    .border(width = borderWidth, color = Color.Cyan),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (showArrows) {
                Subspace { XyzArrows() }
            }
            content()
            Orbiter(
                position = ContentEdge.End,
                offset = 24.dp,
                shape = SpatialRoundedCornerShape(size = CornerSize(50)),
                shouldRenderInNonSpatial = false,
            ) {
                IconButton(
                    onClick = {
                        addHighlight = !addHighlight
                        showArrows = !showArrows
                    },
                    modifier = Modifier.background(Color.Gray),
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Add highlight")
                }
            }
            Spacer(modifier = Modifier.size(20.dp))
            TestDialog {
                Surface(color = Color.White, modifier = Modifier.clip(RoundedCornerShape(5.dp))) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("This is a SpatialDialog", modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun ViewBasedAppPanel(modifier: SubspaceModifier = SubspaceModifier, text: String = "") {
        val context = LocalContext.current
        val textView = remember {
            TextView(context).apply {
                setText(text)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setBackgroundColor(LTGRAY)
                setTextColor(BLACK)
                setGravity(Gravity.CENTER)
            }
        }

        SpatialAndroidViewPanel(factory = { textView }, modifier = modifier)
    }

    @OptIn(ExperimentalSubspaceVolumeApi::class)
    @Composable
    fun XyzArrows(modifier: SubspaceModifier = SubspaceModifier) {
        val session = LocalSession.current ?: return
        var rotation by remember { mutableStateOf(Quaternion.Identity) }
        var gltfModel by remember { mutableStateOf<GltfModel?>(null) }

        LaunchedEffect(Unit) {
            gltfModel = GltfModel.create(session, Paths.get("models", "xyzArrows.glb"))
            val pi = 3.14159F
            val timeSource = Clock.systemUTC()
            val startTime = timeSource.millis()
            val rotateTimeMs = 10000F

            while (true) {
                delay(16L)
                val elapsedMs = timeSource.millis() - startTime
                val angle = (2 * pi) * (elapsedMs / rotateTimeMs)

                val normalized = Vector3(1.0f, 1.0f, 1.0f).toNormalized()

                val qX = normalized.x * sin(angle / 2)
                val qY = normalized.y * sin(angle / 2)
                val qZ = normalized.z * sin(angle / 2)
                val qW = cos(angle / 2)

                rotation = Quaternion(qX, qY, qZ, qW)
            }
        }

        if (gltfModel != null) {
            SceneCoreEntity(
                factory = { GltfModelEntity.create(session, gltfModel!!) },
                modifier = modifier.rotate(rotation),
            )
        }
    }
}
