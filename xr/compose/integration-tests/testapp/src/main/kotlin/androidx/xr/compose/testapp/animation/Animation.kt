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

package androidx.xr.compose.testapp.animation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.ApplicationSubspace
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.alpha
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.scale
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.components.CUJButton
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.components.TopBarWithBackArrow
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple80

class Animation : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IntegrationTestsAppTheme { ValueBasedAnimationsApp() } }
    }

    private inline fun <reified T : ComponentActivity> startActivity() {
        startActivity(Intent(this, T::class.java))
    }

    @Composable
    @SubspaceComposable
    private fun ValueBasedAnimationsApp() {
        MainPanelContent()

        ApplicationSubspace {
            val (showSidePanel, updateShowSidePanel) = remember { mutableStateOf(false) }
            val toggleSidePanel: () -> Unit = { updateShowSidePanel(!showSidePanel) }
            val desiredWidth = 300.dp
            val desiredHeight = 150.dp
            val zOffset = (-30).dp

            SpatialRow {
                val animatedAlpha = remember { Animatable(0.5f) }
                val mainPanelAnimatedScale = remember { Animatable(1.0f) }

                LaunchedEffect(Unit) { animatedAlpha.animateTo(1.0f, animationSpec = tween(2000)) }
                LaunchedEffect(showSidePanel) {
                    if (showSidePanel) {
                        mainPanelAnimatedScale.animateTo(0.01f, animationSpec = tween(10))
                        mainPanelAnimatedScale.animateTo(2.0f, animationSpec = tween(2000))
                        mainPanelAnimatedScale.animateTo(1.0f, animationSpec = tween(2000))
                    } else {
                        mainPanelAnimatedScale.animateTo(1.0f, animationSpec = tween(500))
                    }
                }

                SpatialMainPanel(
                    modifier =
                        SubspaceModifier.width(600.dp)
                            .height(400.dp)
                            .alpha(animatedAlpha.value)
                            .scale(mainPanelAnimatedScale.value)
                )

                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(desiredWidth)
                            .height(desiredHeight)
                            .offset(z = zOffset * 2)
                            .alpha(animatedAlpha.value)
                ) {
                    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
                    PanelContent(
                        "Faded in content",
                        "Show side Panel",
                        !showSidePanel,
                        toggleSidePanel,
                    )
                }

                if (showSidePanel) {
                    val sidePanelAnimatedScale = remember { Animatable(0.01f) }
                    LaunchedEffect(true) {
                        sidePanelAnimatedScale.animateTo(2.0f, animationSpec = tween(2000))
                        sidePanelAnimatedScale.animateTo(1.0f, animationSpec = tween(2000))
                    }

                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(desiredWidth)
                                .height(desiredHeight)
                                .offset(z = zOffset)
                                .scale(sidePanelAnimatedScale.value)
                    ) {
                        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
                        PanelContent(
                            "Grown content",
                            "Hide side panel",
                            showSidePanel,
                            toggleSidePanel,
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainPanelContent() {
        CommonTestScaffold(
            title = getString(R.string.value_based_animation_test),
            showBottomBar = true,
            bottomBarText = "",
            onClickBackArrow = { this@Animation.finish() },
            onClickRecreate = { this@Animation.recreate() },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.spacedBy(20.dp, alignment = Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "Main Panel Content", fontSize = 20.sp)
                CUJButton("Show sample animations") { startActivity<SampleAnimations>() }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @SubspaceComposable
    fun PanelContent(
        text: String,
        buttonText: String,
        showButton: Boolean,
        buttonOnClick: () -> Unit,
    ) {
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
        Box(modifier = Modifier.background(Purple80).fillMaxSize()) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TopBarWithBackArrow(scrollBehavior = null, title = "Side Panel", onClick = null)
                }
                Row {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Orbiter(position = ContentEdge.Top, offset = 5.dp) {
                            Text(
                                text = text,
                                fontSize = 20.sp,
                                modifier = Modifier.background(Purple80),
                            )
                        }
                        if (showButton) {
                            Button(onClick = buttonOnClick) { Text(text = buttonText) }
                        }
                    }
                }
            }
        }
    }
}
