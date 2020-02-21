/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.material.samples

import androidx.animation.TweenBuilder
import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.animation.animatedFloat
import androidx.ui.core.Text
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.VerticalScroller
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.foundation.shape.corner.CutCornerShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.Image
import androidx.ui.graphics.painter.ImagePainter
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutPadding
import androidx.ui.material.AppBarIcon
import androidx.ui.material.BottomAppBar
import androidx.ui.material.DrawerState
import androidx.ui.material.FloatingActionButton
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Scaffold
import androidx.ui.material.ScaffoldState
import androidx.ui.material.TopAppBar
import androidx.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private val colors = listOf(
    Color(0xFFffd7d7.toInt()),
    Color(0xFFffe9d6.toInt()),
    Color(0xFFfffbd0.toInt()),
    Color(0xFFe3ffd9.toInt()),
    Color(0xFFd0fff8.toInt())
)

@Sampled
@Composable
fun SimpleScaffoldWithTopBar(navigationImage: Image) {
    val scaffoldState = remember { ScaffoldState() }
    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = { Text("Drawer content") },
        topAppBar = {
            TopAppBar(
                title = { Text("Simple Scaffold Screen") },
                navigationIcon = {
                    AppBarIcon(ImagePainter(navigationImage), onClick = {
                        scaffoldState.drawerState = DrawerState.Opened
                    })
                }
            )
        },
        floatingActionButtonPosition = Scaffold.FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(onClick = { /* fab click handler */ }) {
                Text("Inc")
            }
        },
        bodyContent = { modifier ->
            VerticalScroller {
                Column(modifier) {
                    repeat(100) {
                        ColoredRect(color = colors[it % colors.size], height = 50.dp)
                    }
                }
            }
        }
    )
}

@Sampled
@Composable
fun ScaffoldWithBottomBarAndCutout(navigationImage: Image) {
    val scaffoldState = remember { ScaffoldState() }

    // Consider negative values to mean 'cut corner' and positive values to mean 'round corner'
    val sharpEdgePercent = -50f
    val roundEdgePercent = 45f
    // Start with sharp edges
    val animatedProgress = animatedFloat(sharpEdgePercent)
    // animation value to animate shape
    val progress = animatedProgress.value.roundToInt()

    // When progress is 0, there is no modification to the edges so we are just drawing a rectangle.
    // This allows for a smooth transition between cut corners and round corners.
    val fabShape = if (progress < 0) {
        CutCornerShape(abs(progress))
    } else if (progress == roundEdgePercent.toInt()) {
        CircleShape
    } else {
        RoundedCornerShape(progress)
    }
    // lambda to call to trigger shape animation
    val changeShape = {
        val target = animatedProgress.targetValue
        val nextTarget = if (target == roundEdgePercent) sharpEdgePercent else roundEdgePercent
        animatedProgress.animateTo(
            targetValue = nextTarget,
            anim = TweenBuilder<Float>().apply { duration = 600 }
        )
    }

    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = { Text("Drawer content") },
        topAppBar = { TopAppBar(title = { Text("Scaffold with bottom cutout") }) },
        bottomAppBar = { fabConfiguration ->
            BottomAppBar(fabConfiguration = fabConfiguration, cutoutShape = fabShape) {
                AppBarIcon(ImagePainter(navigationImage)) {
                    scaffoldState.drawerState = DrawerState.Opened
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = changeShape,
                shape = fabShape,
                color = MaterialTheme.colors().secondary
            ) {
                Text("Change shape", modifier = LayoutPadding(12.dp))
            }
        },
        floatingActionButtonPosition = Scaffold.FabPosition.CenterDocked,
        bodyContent = { modifier ->
            VerticalScroller {
                Column(modifier) {
                    repeat(100) {
                        ColoredRect(color = colors[it % colors.size], height = 50.dp)
                    }
                }
            }
        }
    )
}