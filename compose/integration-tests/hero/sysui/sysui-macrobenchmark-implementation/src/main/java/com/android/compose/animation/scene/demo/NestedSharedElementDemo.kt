/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.compose.animation.scene.demo

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.MutableSceneTransitionLayoutState
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.compose.animation.scene.Swipe
import com.android.compose.animation.scene.ValueKey
import com.android.compose.animation.scene.animateContentColorAsState
import com.android.compose.animation.scene.rememberMutableSceneTransitionLayoutState
import com.android.compose.animation.scene.transitions
import com.android.compose.modifiers.animatedBackground

object ParentSTL {
    object Scenes {
        val Start = SceneKey("Start")
        val End = SceneKey("End")
    }
}

object ChildSTL {
    object Scenes {
        val Top = SceneKey("Top")
        val Bottom = SceneKey("Bottom")
    }
}

object Elements {
    val Shared = ElementKey("Shared")
    val NotShared = ElementKey("NotShared")
}

object ValueKeys {
    val ColorContentKey = ValueKey("Color")
}

@Composable
fun NestedSharedElementDemo(modifier: Modifier = Modifier) {
    Column(modifier) {
        val state =
            rememberMutableSceneTransitionLayoutState(
                ParentSTL.Scenes.Start,
                transitions {
                    from(ParentSTL.Scenes.Start, to = ParentSTL.Scenes.End) {
                        spec = tween(1500)
                        translate(Elements.NotShared, y = (-100).dp)
                        fade(Elements.NotShared)
                        scaleSize(Elements.NotShared, 0.5f, 0.5f)
                    }
                },
            )
        val childState =
            rememberMutableSceneTransitionLayoutState(
                ChildSTL.Scenes.Top,
                transitions {
                    from(ChildSTL.Scenes.Top, to = ChildSTL.Scenes.Bottom) {
                        spec = tween(1500)
                        translate(Elements.NotShared, x = 100.dp)
                        fade(Elements.NotShared)
                        scaleSize(Elements.NotShared, 0.5f, 0.5f)
                    }
                },
            )
        val scope = rememberCoroutineScope()
        SceneTransitionLayout(
            state,
            Modifier.padding(16.dp)
                .border(3.dp, Color.Blue)
                .clickable(null, null) {
                    val targetScene =
                        when (state.currentScene) {
                            ParentSTL.Scenes.Start -> ParentSTL.Scenes.End
                            else -> ParentSTL.Scenes.Start
                        }
                    state.setTargetScene(targetScene, scope)
                }
                .padding(16.dp),
        ) {
            scene(ParentSTL.Scenes.Start, mapOf(Swipe.End to ParentSTL.Scenes.End)) {
                Box(Modifier.fillMaxSize()) {
                    ChildSTL(
                        childState,
                        Modifier.align(Alignment.Center).fillMaxSize(fraction = 0.5f),
                    )
                }
            }
            scene(ParentSTL.Scenes.End, mapOf(Swipe.Start to ParentSTL.Scenes.Start)) {
                Box(Modifier.fillMaxSize()) {
                    SharedElement(Color.Magenta, Modifier.size(30.dp).align(Alignment.TopEnd))
                }
            }
        }
    }
}

@Composable
private fun ContentScope.ChildSTL(
    state: MutableSceneTransitionLayoutState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    NestedSceneTransitionLayout(
        state,
        modifier.border(3.dp, Color.Red).clickable(null, null) {
            val targetScene =
                when (state.currentScene) {
                    ChildSTL.Scenes.Top -> ChildSTL.Scenes.Bottom
                    else -> ChildSTL.Scenes.Top
                }
            state.setTargetScene(targetScene, scope)
        },
    ) {
        scene(ChildSTL.Scenes.Top, mapOf(Swipe.Down to ChildSTL.Scenes.Bottom)) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.TopEnd)
                        .element(Elements.NotShared)
                        .size(80.dp)
                        .background(Color.Blue)
                )
                SharedElement(Color.Green, Modifier.size(100.dp))
            }
        }
        scene(ChildSTL.Scenes.Bottom, mapOf(Swipe.Up to ChildSTL.Scenes.Top)) {
            Box(Modifier.fillMaxSize()) {
                SharedElement(Color.Yellow, Modifier.align(Alignment.BottomStart).size(60.dp))
            }
        }
    }
}

@Composable
private fun ContentScope.SharedElement(color: Color, modifier: Modifier = Modifier) {
    val color by animateContentColorAsState(color, ValueKeys.ColorContentKey)
    Box(
        modifier
            .element(key = Elements.Shared)
            .animatedBackground(color = { color }, shape = CircleShape)
    )
}
