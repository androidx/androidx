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

package androidx.xr.compose.integration.layout.spatialcomposeapp

import android.annotation.SuppressLint
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.xr.compose.integration.common.AnotherActivity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.EdgeOffset.Companion.inner
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterEdge
import androidx.xr.compose.spatial.OrbiterEdge.Companion.Top
import androidx.xr.compose.spatial.OrbiterSettings
import androidx.xr.compose.spatial.SpatialDialog
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MainPanel
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.Volume
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.depth
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.Session
import java.time.Clock
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/**
 * Main activity for the Spatial Compose App.
 *
 * This activity demonstrates the use of Spatial Compose to create a mixed 2D/3D UI. It includes
 * examples of rendering 2D content to the MainPanel and 3D content within a Subspace.
 */
class SpatialComposeAppActivity : ComponentActivity() {

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
        PanelContent {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Panel Center - main task window")
                val isSpatialUiEnabled = LocalSpatialCapabilities.current.isSpatialUiEnabled
                val session = Session.create(this@SpatialComposeAppActivity)
                Button(
                    onClick = {
                        if (isSpatialUiEnabled) {
                            session.spatialEnvironment.requestHomeSpaceMode()
                        } else {
                            session.spatialEnvironment.requestFullSpaceMode()
                        }
                    }
                ) {
                    Text("Switch Space Mode")
                }
            }
        }
    }

    @Composable
    @SubspaceComposable
    fun PanelGrid() {
        val sidePanelModifier = SubspaceModifier.fillMaxWidth().height(200.dp)
        val curveRadius = 1025.dp
        SpatialColumn(name = "PanelGridColumn") {
            Orbiter(
                position = Top,
                offset = inner(8.dp),
                shape = SpatialRoundedCornerShape(CornerSize(16.dp)),
            ) {
                Surface { Text(text = "Subspace Compose App", modifier = Modifier.padding(8.dp)) }
            }

            SpatialRow(
                modifier = SubspaceModifier.width(2000.dp).height(1200.dp),
                alignment = SpatialAlignment.BottomCenter,
                curveRadius = curveRadius,
                name = "PanelGridRow",
            ) {
                SpatialColumn(
                    modifier = SubspaceModifier.width(200.dp).fillMaxHeight(),
                    name = "LeftColumn",
                ) {
                    AppPanel(modifier = sidePanelModifier, text = "Panel Top Left")
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    ViewBasedAppPanel(
                        modifier = sidePanelModifier,
                        text = "Panel Bottom Left (View)"
                    )
                }
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(800.dp).fillMaxHeight().padding(horizontal = 20.dp),
                    alignment = SpatialAlignment.TopCenter,
                    name = "CenterColumn",
                ) {
                    MainPanel(modifier = SubspaceModifier.fillMaxWidth().height(600.dp))
                    SpatialPanel(
                        modifier = SubspaceModifier.fillMaxWidth().height(400.dp),
                        name = "ActivityPanel",
                        intent =
                            Intent(this@SpatialComposeAppActivity, AnotherActivity::class.java),
                    )
                }
                SpatialColumn(
                    modifier = SubspaceModifier.width(200.dp).fillMaxHeight(),
                    name = "RightColumn",
                ) {
                    AppPanel(modifier = sidePanelModifier, text = "Panel Top Right")
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    AppPanel(modifier = sidePanelModifier, text = "Panel Bottom Right")
                }
            }
        }
    }

    @SubspaceComposable
    @Composable
    fun AppPanel(modifier: SubspaceModifier = SubspaceModifier, text: String = "") {
        SpatialPanel(modifier = modifier, name = text) { PanelContent(text) }
    }

    @UiComposable
    @SubspaceComposable
    @Composable
    fun PanelContent(vararg texts: String) {
        PanelContent {
            Column {
                for (text in texts) {
                    Text(text)
                }
            }
        }
    }

    @UiComposable
    @Composable
    fun PanelContent(content: @Composable () -> Unit) {
        var addHighlight by remember { mutableStateOf(false) }
        val borderWidth by remember { derivedStateOf { if (addHighlight) 3.dp else 0.dp } }
        var showDialog by remember { mutableStateOf(false) }
        Column(
            modifier =
                Modifier.background(Color.LightGray)
                    .fillMaxSize()
                    .border(width = borderWidth, color = Color.Cyan),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
            Orbiter(
                position = OrbiterEdge.End,
                offset = 24.dp,
                shape = SpatialRoundedCornerShape(size = CornerSize(50)),
                settings = OrbiterSettings(shouldRenderInNonSpatial = false),
            ) {
                IconButton(
                    onClick = { addHighlight = !addHighlight },
                    modifier = Modifier.background(Color.Gray),
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Add highlight")
                }
            }
            Spacer(modifier = Modifier.size(20.dp))
            Button(onClick = { showDialog = true }) { Text("show dialog") }
            if (showDialog) {
                SpatialDialog(onDismissRequest = { showDialog = false }) {
                    Surface(
                        color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(5.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("This is a SpatialDialog", modifier = Modifier.padding(10.dp))
                            Button(onClick = { showDialog = false }) { Text("Dismiss") }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
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

        SpatialPanel(view = textView, modifier = modifier)
    }

    @Composable
    fun XyzArrows(modifier: SubspaceModifier = SubspaceModifier) {
        val session =
            checkNotNull(LocalSession.current) {
                "LocalSession.current was null. Session must be available."
            }
        var arrows by remember { mutableStateOf<GltfModel?>(null) }
        val gltfEntity = arrows?.let { remember { GltfModelEntity.create(session, it) } }

        LaunchedEffect(Unit) { arrows = GltfModel.create(session, "models/xyzArrows.glb").await() }

        if (gltfEntity != null) {
            Volume(modifier) {
                gltfEntity.setParent(it)

                lifecycleScope.launch {
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

                        val q = Quaternion(qX, qY, qZ, qW)

                        gltfEntity.setPose(Pose(rotation = q))
                    }
                }
            }
        }
    }
}
