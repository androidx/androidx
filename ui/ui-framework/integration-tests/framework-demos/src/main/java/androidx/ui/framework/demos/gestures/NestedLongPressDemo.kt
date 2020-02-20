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

package androidx.ui.framework.demos.gestures

import android.app.Activity
import android.os.Bundle
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.gesture.LongPressGestureDetector
import androidx.ui.core.setContent
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.unit.Dp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

/**
 * Demonstration of how various press/tap gesture interact together in a nested fashion.
 */
class NestedLongPressDemo : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LongPressableContainer(
                paddingLeft = 48.dp,
                paddingRight = 48.dp,
                paddingTop = 96.dp,
                paddingBottom = 96.dp
            ) {
                LongPressableContainer(
                    paddingLeft = 48.dp,
                    paddingRight = 48.dp,
                    paddingTop = 96.dp,
                    paddingBottom = 96.dp
                ) {
                    LongPressableContainer {}
                }
            }
        }
    }
}

@Composable
fun LongPressableContainer(
    paddingLeft: Dp = 0.dp,
    paddingTop: Dp = 0.dp,
    paddingRight: Dp = 0.dp,
    paddingBottom: Dp = 0.dp,
    children: @Composable() () -> Unit
) {
    val defaultColor = DefaultBackgroundColor
    val pressedColor = PressedColor

    val currentColor = state { defaultColor }
    val pressed = state { false }

    val onLongPress: (PxPosition) -> Unit = {
        currentColor.value = currentColor.value.next()
    }

    val color = if (pressed.value) {
        pressedColor.over(currentColor.value)
    } else {
        currentColor.value
    }

    LongPressGestureDetector(onLongPress) {
        Box(
            paddingLeft = paddingLeft,
            paddingTop = paddingTop,
            paddingRight = paddingRight,
            paddingBottom = paddingBottom,
            backgroundColor = color,
            gravity = ContentGravity.Center,
            border = Border(2.dp, BorderColor),
            children = children
        )
    }
}