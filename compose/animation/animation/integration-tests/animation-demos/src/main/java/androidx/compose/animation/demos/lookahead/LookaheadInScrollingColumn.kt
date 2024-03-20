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
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.demos.layoutanimation.turquoiseColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A simple example showing how [animateBounds] behaves when animating from/to a scrolling layout.
 *
 * Note that despite the items position changing due to the scroll, it does not affect or trigger an
 * animation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
@Preview
fun LookaheadInScrollingColumn() {
    var displayInScroller by remember { mutableStateOf(false) }
    val movableContent = remember {
        movableContentWithReceiverOf<LookaheadScope> {
            Box(
                Modifier.zIndex(1f)
                    .let {
                        if (displayInScroller) {
                            it.height(80.dp).fillMaxWidth()
                        } else {
                            it.size(150.dp)
                        }
                    }
                    .animateBounds(
                        lookaheadScope = this@movableContentWithReceiverOf,
                        boundsTransform = { _, _ ->
                            spring(stiffness = 50f, visibilityThreshold = Rect.VisibilityThreshold)
                        }
                    )
                    .clickable { displayInScroller = !displayInScroller }
                    .background(color, RoundedCornerShape(10.dp))
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        LookaheadScope {
            Column(
                modifier =
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState(0)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Click Yellow box to animate to/from scrolling list.")
                repeat(6) {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(turquoiseColors[it % 6], RoundedCornerShape(10.dp))
                            .height(80.dp)
                    )
                }
                if (displayInScroller) {
                    movableContent()
                }
                repeat(6) {
                    Box(
                        Modifier.animateBounds(lookaheadScope = this@LookaheadScope)
                            .background(turquoiseColors[it % 6], RoundedCornerShape(10.dp))
                            .height(80.dp)
                            .fillMaxWidth()
                    )
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                if (!displayInScroller) {
                    movableContent()
                }
            }
        }
    }
}

private val color = Color(0xffffcc5c)
