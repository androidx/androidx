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

package androidx.ui.core.demos.gestures

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.longPressGestureFilter
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.Text
import androidx.ui.graphics.compositeOver
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.padding
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

/**
 * Demonstration of how various press/tap gesture interact together in a nested fashion.
 */
@Composable
fun NestedLongPressDemo() {
    Column {
        Text(
            "Demonstrates interaction between nested longPressGestureFitlers  in an " +
                    "edge case that is nevertheless supported (normally regions will be separated" +
                    " by a pressIndicatorGestureFilter, but here they are not)."
        )
        Text(
            "This just demonstrates the interaction between directly nested " +
                    "longPressGestureFilters."
        )
        LongPressableContainer(Modifier.fillMaxSize()) {
            LongPressableContainer(Modifier.padding(48.dp).fillMaxSize()) {
                LongPressableContainer(Modifier.padding(48.dp).fillMaxSize()) {}
            }
        }
    }
}

@Composable
private fun LongPressableContainer(
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    val defaultColor = DefaultBackgroundColor
    val pressedColor = PressedColor

    val currentColor = state { defaultColor }
    val pressed = state { false }

    val onLongPress: (PxPosition) -> Unit = {
        currentColor.value = currentColor.value.next()
    }

    val color = if (pressed.value) {
        pressedColor.compositeOver(currentColor.value)
    } else {
        currentColor.value
    }

    Box(
        modifier.longPressGestureFilter(onLongPress),
        backgroundColor = color,
        gravity = ContentGravity.Center,
        border = Border(2.dp, BorderColor),
        children = children
    )
}
