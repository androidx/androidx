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

package androidx.xr.compose.integration.subspacecomposableapp

import android.annotation.SuppressLint
import android.graphics.Color.BLACK
import android.graphics.Color.LTGRAY
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.ExperimentalSubspaceVolumeApi
import androidx.xr.compose.subspace.SpatialAndroidViewPanel
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.Volume
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.depth
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import kotlin.math.cos
import kotlin.math.sin

class SubspaceComposableApp : ComponentActivity() {

    val session by lazy { (Session.create(this) as SessionCreateSuccess).session }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainApp() }
        isDebugInspectorInfoEnabled = true
    }

    private object PageRoutes {
        const val HOME = "home"
        const val PANELS = "panels"
        const val ARROWS = "arrows"
    }

    private data class PageContent(val route: String, val icon: @Composable () -> Unit)

    private val pages =
        listOf(
            PageContent(PageRoutes.HOME, { Icon(Icons.Filled.Home, contentDescription = "HOME") }),
            PageContent(
                PageRoutes.PANELS,
                { Icon(Icons.Filled.AccountBox, contentDescription = "PANELS") },
            ),
            PageContent(
                PageRoutes.ARROWS,
                { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "ARROWS") },
            ),
        )

    @Composable
    fun MainApp() {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = PageRoutes.HOME) {
            composable(PageRoutes.HOME) {
                SideEffect {
                    (Session.create(this@SubspaceComposableApp) as SessionCreateSuccess)
                        .session
                        .scene
                        .requestHomeSpaceMode()
                    session.scene.mainPanelEntity.setEnabled(true)
                }

                MainContent(text = "Home Page in Home Space Mode", navController = navController)
            }

            composable(PageRoutes.PANELS) {
                SideEffect {
                    (Session.create(this@SubspaceComposableApp) as SessionCreateSuccess)
                        .session
                        .scene
                        .requestFullSpaceMode()
                    session.scene.mainPanelEntity.setEnabled(false)
                }

                Subspace { PanelGrid(navController = navController) }
            }
            composable(PageRoutes.ARROWS) {
                SideEffect {
                    (Session.create(this@SubspaceComposableApp) as SessionCreateSuccess)
                        .session
                        .scene
                        .requestFullSpaceMode()
                    session.scene.mainPanelEntity.setEnabled(true)
                }

                MainContent(text = "Now some arrows are shown!", navController = navController)

                Subspace {
                    XyzArrows(
                        SubspaceModifier.width(.5.meters.toDp())
                            .height(0.5.meters.toDp())
                            .depth(0.5.meters.toDp())
                            .offset(x = 1.meters.toDp(), z = -0.5.meters.toDp())
                    )
                }
            }
        }
    }

    @Composable
    fun MainContent(text: String, navController: NavController) {
        Row {
            Orbiter(ContentEdge.Start, offset = (-24).dp) {
                NavigationRail(
                    modifier =
                        if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                            Modifier.height(IntrinsicSize.Min)
                        } else {
                            Modifier
                        }
                ) {
                    for (page in pages) {
                        NavigationRailItem(
                            selected = navController.currentDestination?.route == page.route,
                            onClick = { navController.navigate(page.route) },
                            icon = page.icon,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.background(Color.LightGray).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text)
            }
        }
    }

    @Composable
    fun PanelGrid(navController: NavController) {
        var curvePercent by remember { mutableFloatStateOf(0.825f) }
        val curveRadius by remember {
            derivedStateOf { (1000.dp * curvePercent).coerceAtLeast(1.dp) }
        }
        val sidePanelModifier = SubspaceModifier.fillMaxWidth().height(200.dp)
        SpatialColumn {
            SpatialRow(alignment = SpatialAlignment.TopCenter) {
                // Curve radius adjustment panel.
                SpatialPanel(modifier = SubspaceModifier.width(400.dp).height(150.dp)) {
                    Column {
                        Text(text = "curve radius: $curveRadius", fontSize = 20.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Slider(value = curvePercent, onValueChange = { curvePercent = it })
                    }
                }
            }
            SpatialCurvedRow(
                modifier = SubspaceModifier.width(2000.dp).height(600.dp),
                alignment = SpatialAlignment.BottomCenter,
                curveRadius = curveRadius,
            ) {
                SpatialColumn(modifier = SubspaceModifier.width(200.dp).fillMaxHeight()) {
                    AppPanel(modifier = sidePanelModifier, text = "Panel Top Left")
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    ViewBasedAppPanel(
                        modifier = sidePanelModifier,
                        text = "Panel Bottom Left (View)",
                    )
                }
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(800.dp).fillMaxHeight().padding(horizontal = 20.dp)
                ) {
                    SpatialPanel(modifier = SubspaceModifier.fillMaxSize()) {
                        MainContent(
                            text = "This is a 3D Panel Layout!",
                            navController = navController,
                        )
                    }
                }
                SpatialColumn(modifier = SubspaceModifier.width(200.dp).fillMaxHeight()) {
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
        SpatialPanel(modifier = modifier) { PanelContent(text) }
    }

    @UiComposable
    @Composable
    fun PanelContent(vararg text: String) {
        var addHighlight by remember { mutableStateOf(false) }
        val borderWidth by remember { derivedStateOf { if (addHighlight) 3.dp else 0.dp } }
        Box(
            modifier =
                Modifier.background(Color.LightGray)
                    .fillMaxSize()
                    .border(width = borderWidth, color = Color.Cyan),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                for (item in text) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
            Orbiter(position = ContentEdge.End, offset = 24.dp) {
                IconButton(
                    onClick = { addHighlight = !addHighlight },
                    modifier = Modifier.background(Color.Gray),
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Add highlight")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Composable
    fun ViewBasedAppPanel(modifier: SubspaceModifier = SubspaceModifier, text: String = "") {
        SpatialAndroidViewPanel(
            factory = { context ->
                TextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    setBackgroundColor(LTGRAY)
                    setTextColor(BLACK)
                    gravity = Gravity.CENTER
                }
            },
            update = { it.text = text },
            modifier = modifier,
        )
    }

    @OptIn(ExperimentalSubspaceVolumeApi::class)
    @Composable
    @SubspaceComposable
    fun XyzArrows(modifier: SubspaceModifier = SubspaceModifier) {
        val session =
            checkNotNull(LocalSession.current) {
                "LocalSession.current was null. Session must be available."
            }
        var arrows by remember { mutableStateOf<GltfModel?>(null) }
        val gltfEntity = arrows?.let { remember { GltfModelEntity.create(session, it) } }
        if (gltfEntity != null) {
            gltfEntity!!.contentDescription = "Showing arrows"
        }

        LaunchedEffect(Unit) {
            arrows = GltfModel.create(session, Paths.get("models", "xyzArrows.glb"))
        }

        if (gltfEntity != null) {
            val angle by
                rememberInfiniteTransition(label = "infinite")
                    .animateFloat(
                        initialValue = 0.0f,
                        targetValue = (Math.PI * 2).toFloat(),
                        animationSpec = infiniteRepeatable(tween(10000)),
                        label = "rotation animation",
                    )

            LaunchedEffect(angle) {
                val normalized = Vector3(1.0f, 1.0f, 1.0f).toNormalized()

                val qX = normalized.x * sin(angle / 2)
                val qY = normalized.y * sin(angle / 2)
                val qZ = normalized.z * sin(angle / 2)
                val qW = cos(angle / 2)

                val q = Quaternion(qX, qY, qZ, qW)

                gltfEntity.setPose(Pose(rotation = q))
                gltfEntity!!.contentDescription = "Animating arrows"
            }

            Volume(modifier) { gltfEntity.parent = it }
        }
    }
}
