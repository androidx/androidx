/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.animation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.CustomizedLookaheadAnimationVisualDebugging
import androidx.compose.animation.ExperimentalLookaheadAnimationVisualDebugApi
import androidx.compose.animation.LookaheadAnimationVisualDebugging
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLookaheadAnimationVisualDebugApi::class)
@Sampled
@Composable
fun LookaheadAnimationVisualDebuggingSample() {
    var isExpanded by mutableStateOf(false)
    // Wrap content with LookaheadAnimationVisualDebugging to enable visual debugging.
    // Optional parameters allow color customization and decision of whether to show key labels.
    // Note that enabling LookaheadAnimationVisualDebugging affects the entire UI subtree generated
    // by the content lambda. It applies to all descendants, regardless of whether they are defined
    // within the same lexical scope.
    LookaheadAnimationVisualDebugging(isShowKeyLabelEnabled = true) {
        SharedTransitionLayout(Modifier.fillMaxSize().clickable { isExpanded = !isExpanded }) {
            AnimatedVisibility(visible = isExpanded) {
                Box(
                    Modifier.offset(100.dp, 100.dp)
                        .size(200.dp)
                        .sharedElement(
                            rememberSharedContentState(key = "box"),
                            animatedVisibilityScope = this,
                        )
                        .background(Color.Red)
                )
            }
            AnimatedVisibility(visible = !isExpanded) {
                Box(
                    Modifier.offset(0.dp, 0.dp)
                        .size(50.dp)
                        .sharedElement(
                            rememberSharedContentState(key = "box"),
                            animatedVisibilityScope = this,
                        )
                        .background(Color.Blue)
                )
            }
        }
    }
}

@OptIn(ExperimentalLookaheadAnimationVisualDebugApi::class)
@Sampled
@Composable
fun CustomizedLookaheadAnimationVisualDebuggingSample() {
    var isExpanded by mutableStateOf(false)
    // Wrap content with LookaheadAnimationVisualDebugging to enable visual debugging.
    // Optional parameters allow color customization and decision of whether to show key labels.
    // Note that enabling LookaheadAnimationVisualDebugging affects the entire UI subtree generated
    // by the content lambda. It applies to all descendants, regardless of whether they are defined
    // within the same lexical scope.
    LookaheadAnimationVisualDebugging {
        // Wrap content with CustomizedLookaheadAnimationVisualDebugging to customize the color of
        // the bounds visualizations in the specified scope.
        CustomizedLookaheadAnimationVisualDebugging(Color.Black) {
            SharedTransitionLayout(Modifier.fillMaxSize().clickable { isExpanded = !isExpanded }) {
                AnimatedVisibility(visible = isExpanded) {
                    Box(
                        Modifier.offset(100.dp, 100.dp)
                            .size(200.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "box"),
                                animatedVisibilityScope = this,
                            )
                            .background(Color.Red)
                    )
                }
                AnimatedVisibility(visible = !isExpanded) {
                    Box(
                        Modifier.offset(0.dp, 0.dp)
                            .size(50.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "box"),
                                animatedVisibilityScope = this,
                            )
                            .background(Color.Blue)
                    )
                }
            }
        }
    }
}
