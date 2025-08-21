/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.animation.demos.sharedelement

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun SharedElementClipReveal() {
    var target by remember { mutableStateOf(true) }
    BackHandler { target = !target }
    SharedTransitionLayout {
        AnimatedContent<Boolean>(
            targetState = target,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) {
            if (it) {
                Box(Modifier.fillMaxSize()) {
                    Button(
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .sharedBounds(
                                    rememberSharedContentState("clip"),
                                    this@AnimatedContent,
                                ),
                        onClick = { target = false },
                    ) {
                        Text("Toggle State")
                    }
                }
            } else {
                Column(
                    Modifier.sharedBounds(
                            rememberSharedContentState("clip"),
                            this@AnimatedContent,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        )
                        .clipToBounds()
                        .skipToLookaheadSize()
                        .skipToLookaheadPosition()
                        .fillMaxSize()
                        .background(Color.Black),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Hello", fontSize = 80.sp, color = Color.White)
                    Text("Shared", fontSize = 80.sp, color = Color.White)
                    Text("Clip", fontSize = 80.sp, color = Color.White)
                    Text("Bounds", fontSize = 80.sp, color = Color.White)
                }
            }
        }
    }
}
