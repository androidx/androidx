/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.Composable
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.DoubleTapGestureDetector
import androidx.ui.core.gesture.LongPressGestureDetector
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.core.gesture.TapGestureDetector
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

/**
 * Demonstration of how various press/tap gesture interact together in a nested fashion.
 */
@Composable
fun NestedPressDemo() {
    PressableContainer(LayoutSize.Fill) {
        PressableContainer(LayoutPadding(48.dp) + LayoutSize.Fill) {
            PressableContainer(LayoutPadding(48.dp) + LayoutSize.Fill)
        }
    }
}

@Composable
private fun PressableContainer(
    modifier: Modifier = Modifier.None,
    children: @Composable() () -> Unit = {}
) {
    val defaultColor = DefaultBackgroundColor
    val pressedColor = PressedColor

    val currentColor = remember { Single(defaultColor) }
    val pressed = state { false }

    val onStart: (Any) -> Unit = {
        pressed.value = true
    }

    val onStop: () -> Unit = {
        pressed.value = false
    }

    val onLongPress = { _: PxPosition ->
        pressed.value = false
        currentColor.value = defaultColor
    }

    val onTap = {
        currentColor.value = currentColor.value.next()
    }

    val onDoubleTap = { _: PxPosition ->
        currentColor.value = currentColor.value.prev().prev()
    }

    val color = if (pressed.value) {
        pressedColor.over(currentColor.value)
    } else {
        currentColor.value
    }

    val gestureDetectors = PressIndicatorGestureDetector(onStart, onStop, onStop) +
            TapGestureDetector(onTap) +
            DoubleTapGestureDetector(onDoubleTap) +
            LongPressGestureDetector(onLongPress)
    Box(
        modifier + gestureDetectors,
        backgroundColor = color, border = Border(2.dp, BorderColor),
        children = children
    )
}

private data class Single<T>(var value: T)