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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.animateMoveAlong
import androidx.compose.ui.test.animateMoveTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.pan
import androidx.compose.ui.test.performTrackpadInput
import androidx.compose.ui.test.scale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Sampled
fun trackpadInputClick() {
    composeTestRule.onNodeWithTag("myComponent").performTrackpadInput {
        // Click in the middle of the node
        click(center)
    }
}

@Sampled
fun trackpadInputAnimateMoveTo() {
    composeTestRule.onNodeWithTag("myComponent").performTrackpadInput {
        // Hover over the node, making an X shape
        moveTo(topLeft)
        animateMoveTo(bottomRight)
        // Note that an actual user wouldn't be able to instantly
        // move from the bottom right to the top right
        advanceEventTime()
        moveTo(topRight)
        animateMoveTo(bottomLeft)
    }
}

@Sampled
fun trackpadInputAnimateMoveAlong() {
    composeTestRule.onNodeWithTag("myComponent").performTrackpadInput {
        // Hover over the node, making a full circle with a radius of 100px
        val r = 100f
        animateMoveAlong(
            curve = {
                val angle = 2 * PI * it / 1000
                center + Offset(r * cos(angle).toFloat(), r * sin(angle).toFloat())
            },
            durationMillis = 1000L,
        )
    }
}

@Sampled
fun trackpadInputPan() {
    composeTestRule.onNodeWithTag("verticalScrollable").performTrackpadInput {
        pan(Offset(0f, 100f))
    }
}

@Sampled
fun trackpadInputScale() {
    composeTestRule.onNodeWithTag("transformable").performTrackpadInput {
        // Performs a scale with a factor of 0.9f, which corresponds to a "zoom out" gesture.
        scale(0.9f)
    }
}
