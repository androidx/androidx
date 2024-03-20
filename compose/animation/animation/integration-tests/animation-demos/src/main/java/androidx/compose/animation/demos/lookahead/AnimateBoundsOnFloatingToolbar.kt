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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.demos.visualaid.EasingItemDemo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp

/**
 * Example using [animateBounds] with nested movable content.
 *
 * Animates an Icon component from a Toolbar to a FAB position, the toolbar is also animated to hide
 * it under the FAB.
 */
@Preview
@Composable
fun AnimateBoundsOnFloatingToolbarDemo() {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            val sampleText = remember { LoremIpsum().values.first() }
            Text(
                text = "Click on the Toolbar to animate",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6
            )
            Text(text = sampleText)
        }
        FloatingFabToolbar(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp)
                .padding(bottom = 24.dp)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FloatingFabToolbar(modifier: Modifier = Modifier) {
    var mode by remember { mutableStateOf(FabToolbarMode.Toolbar) }

    val animationDuration = 600
    val animEasing = EasingItemDemo.EmphasizedEasing.function

    val editIconPadding by
        animateDpAsState(
            targetValue = if (mode == FabToolbarMode.Fab) 12.dp else 0.dp,
            animationSpec = tween(animationDuration, easing = animEasing),
            label = "Edit Icon Padding"
        )

    val myEditIcon = remember {
        movableContentWithReceiverOf<LookaheadScope, Modifier> { iconModifier ->
            Box(
                modifier =
                    iconModifier
                        .let {
                            if (mode == FabToolbarMode.Toolbar) {
                                it.fillMaxSize()
                            } else {
                                it.aspectRatio(1f, matchHeightConstraintsFirst = true)
                            }
                        }
                        .animateBounds(
                            lookaheadScope = this,
                            modifier = Modifier,
                            boundsTransform = { _, _ ->
                                tween(animationDuration, easing = animEasing)
                            },
                        ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onPrimary,
                    modifier =
                        Modifier.background(MaterialTheme.colors.primary, RoundedCornerShape(16.dp))
                            .fillMaxSize()
                            .padding(editIconPadding.coerceAtLeast(0.dp))
                )
            }
        }
    }

    // Toolbar container + Toolbar
    val myToolbar = remember {
        movableContentWithReceiverOf<LookaheadScope, Modifier> { toolbarMod ->
            // Toolbar container
            Box(
                modifier =
                    toolbarMod
                        .animateBounds(
                            lookaheadScope = this,
                            boundsTransform = { _, _ ->
                                tween(animationDuration, easing = animEasing)
                            },
                        )
                        .background(MaterialTheme.colors.background, RoundedCornerShape(50))
                        .let {
                            if (mode == FabToolbarMode.Toolbar) {
                                // Respect toolbar content size when in Toolbar mode
                                it.wrapContentSize().padding(8.dp)
                            } else {
                                // Resize the container so that it doesn't go beyond the Fab box,
                                // clipping the toolbar as needed
                                it.fillMaxWidth().wrapContentHeight().padding(8.dp)
                            }
                        }
            ) {
                // Toolbar - Fixed Size
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconSize = DpSize(30.dp, 20.dp)
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(iconSize)
                    )
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(iconSize)
                    )
                    Box(modifier = Modifier.size(iconSize)) {
                        // Slot for the Edit Icon when position on the toolbar
                        if (mode == FabToolbarMode.Toolbar) {
                            myEditIcon(Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }

    LookaheadScope {
        Box(
            modifier.clickable {
                mode =
                    if (mode == FabToolbarMode.Fab) {
                        FabToolbarMode.Toolbar
                    } else {
                        FabToolbarMode.Fab
                    }
            }
        ) {
            Box(
                Modifier.align(Alignment.Center),
            ) {
                // Slot 0 - Toolbar position
                if (mode == FabToolbarMode.Toolbar) {
                    // The Toolbar container should also place the Edit Icon at this state
                    myToolbar(Modifier.align(Alignment.Center))
                }
            }
            Box(Modifier.size(80.dp).align(Alignment.CenterEnd)) {
                // Slot 1 - Fab position
                if (mode == FabToolbarMode.Fab) {
                    // We pull out the Edit Icon in this state
                    myToolbar(Modifier.align(Alignment.Center))
                    myEditIcon(Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

enum class FabToolbarMode {
    Fab,
    Toolbar
}
