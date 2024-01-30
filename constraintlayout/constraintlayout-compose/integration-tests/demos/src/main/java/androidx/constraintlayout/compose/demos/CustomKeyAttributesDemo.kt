/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalMotionApi::class)

package androidx.constraintlayout.compose.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene
import androidx.constraintlayout.compose.OnSwipe
import androidx.constraintlayout.compose.SwipeDirection
import androidx.constraintlayout.compose.SwipeSide

/**
 * Shows how to use MotionLayout to interpolate custom values such as colors during animation.
 *
 * This example animates the background color of a box from [Color.Red] to [Color.Blue] while using
 * KeyFrames to add intermediate colors [Color.Yellow] and [Color.Green] at 33% and 66% points of
 * the animation, respectively.
 *
 * The animation is driven with an swipe gesture defined by [OnSwipe].
 */
@Preview
@Composable
fun CustomColorInKeyAttributesDemo() {
    val boxId = "box"
    val textId = "text"
    MotionLayout(
        motionScene = MotionScene {
            val box = createRefFor(boxId)
            val text = createRefFor(textId)
            defaultTransition(
                from = constraintSet {
                    constrain(text) {
                        top.linkTo(box.bottom, 10.dp)
                        start.linkTo(box.start)
                    }
                    constrain(box) {
                        width = Dimension.value(50.dp)
                        height = Dimension.value(50.dp)

                        centerVerticallyTo(parent)
                        start.linkTo(parent.start)

                        customColor("background", Color.Red)
                    }
                },
                to = constraintSet {
                    constrain(text) {
                        top.linkTo(box.bottom, 10.dp)
                        end.linkTo(box.end)
                    }
                    constrain(box) {
                        width = Dimension.value(50.dp)
                        height = Dimension.value(50.dp)

                        centerVerticallyTo(parent)
                        end.linkTo(parent.end)

                        customColor("background", Color.Blue)
                    }
                }
            ) {
                onSwipe = OnSwipe(
                    anchor = box,
                    side = SwipeSide.Middle,
                    direction = SwipeDirection.End
                )
                keyAttributes(box) {
                    frame(33) {
                        customColor("background", Color.Yellow)
                    }
                    frame(66) {
                        customColor("background", Color.Green)
                    }
                }
            }
        },
        progress = 0f,
        modifier = Modifier.fillMaxSize()
    ) {
        val background = customColor(boxId, "background")
        Box(
            modifier = Modifier
                .layoutId(boxId)
                .background(background)
        )
        Text(
            modifier = Modifier.layoutId(textId),
            text = "Color: ${background.toArgb().toUInt().toString(16)}"
        )
    }
}
