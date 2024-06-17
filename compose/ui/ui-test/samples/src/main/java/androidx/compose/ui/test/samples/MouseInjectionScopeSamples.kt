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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.animateMoveAlong
import androidx.compose.ui.test.animateMoveTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.smoothScroll
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Sampled
fun mouseInputClick() {
    composeTestRule.onNodeWithTag("myComponent").performMouseInput {
        // Click in the middle of the node
        click(center)
    }
}

@Sampled
fun mouseInputAnimateMoveTo() {
    composeTestRule.onNodeWithTag("myComponent").performMouseInput {
        // Hover over the node, making an X shape
        moveTo(topLeft)
        animateMoveTo(bottomRight)
        // Note that an actual user wouldn't be able to instantly
        // move from the bottom right to the top right
        moveTo(topRight)
        animateMoveTo(bottomLeft)
    }
}

@Sampled
fun mouseInputAnimateMoveAlong() {
    composeTestRule.onNodeWithTag("myComponent").performMouseInput {
        // Hover over the node, making a full circle with a radius of 100px
        val r = 100f
        animateMoveAlong(
            curve = {
                val angle = 2 * PI * it / 1000
                center + Offset(r * cos(angle).toFloat(), r * sin(angle).toFloat())
            },
            durationMillis = 1000L
        )
    }
}

@Sampled
fun mouseInputScrollWhileDown() {
    composeTestRule
        .onNodeWithTag("verticalScrollable")
        // Scroll downwards while keeping a button pressed:
        .performMouseInput {
            // Presses the primary mouse button
            press()
            // Scroll the scroll wheel by 6 units
            repeat(6) {
                advanceEventTime()
                scroll(1f)
            }
            // And release the mouse button
            advanceEventTime()
            release()
        }
}

@Sampled
fun mouseInputSmoothScroll() {
    composeTestRule.onNodeWithTag("horizontalScrollable").performMouseInput {
        // Scroll forwards horizontally, which is rightwards
        // unless scroll direction is reversed
        smoothScroll(100f, durationMillis = 500L, ScrollWheel.Horizontal)
        // The 100f scroll delta is equally divided into smaller scrolls,
        // such that the time in between two scroll events is more or less
        // equal to the default time between events, 16ms.
    }
}
