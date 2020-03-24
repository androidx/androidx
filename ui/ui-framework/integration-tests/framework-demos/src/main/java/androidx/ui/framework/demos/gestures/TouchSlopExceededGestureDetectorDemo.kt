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
import androidx.compose.state
import androidx.ui.core.Direction
import androidx.ui.core.gesture.TouchSlopExceededGestureDetector
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.dp

/**
 * Simple demo that shows off TouchSlopExceededGestureDetector.
 */
@Composable
fun TouchSlopExceededGestureDetectorDemo() {

    val verticalColor = Color(0xfff44336)
    val horizontalColor = Color(0xff2196f3)

    val orientationVertical = state { true }

    // This would be more efficient if onTouchSlopExceeded were memoized because it's
    // value doesn't need to change for each new composition.  Like this, every time
    // we recompose, a new lambda is created.  Here we aren't memoizing to demonstrate
    // that TouchSlopExceededGestureDetector behaves correctly when it is recomposed
    // because onTouchSlopExceeded changes.
    val onTouchSlopExceeded =
        {
            orientationVertical.value = !orientationVertical.value
        }

    val canDrag =
        if (orientationVertical.value) {
            { direction: Direction ->
                when (direction) {
                    Direction.UP -> true
                    Direction.DOWN -> true
                    else -> false
                }
            }
        } else {
            { direction: Direction ->
                when (direction) {
                    Direction.LEFT -> true
                    Direction.RIGHT -> true
                    else -> false
                }
            }
        }

    val color =
        if (orientationVertical.value) {
            verticalColor
        } else {
            horizontalColor
        }

    Box(
        LayoutSize.Fill +
                LayoutAlign.Center +
                TouchSlopExceededGestureDetector(onTouchSlopExceeded, canDrag) +
                LayoutSize(96.dp),
        backgroundColor = color
    )
}
