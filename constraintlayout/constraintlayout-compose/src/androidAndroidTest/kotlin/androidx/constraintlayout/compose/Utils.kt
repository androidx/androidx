/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

/**
 * Applies the [id] to the [layoutId] and [testTag] Modifiers.
 *
 * This allows using syntax such as: `rule.onNodeWithTag(id)...`
 */
internal fun Modifier.layoutTestId(id: Any): Modifier = testTag(id.toString()).layoutId(id)

/**
 * Helper method that will simulate a swipe on the given [SemanticsNodeInteraction].
 *
 * Use [from] and [to] to calculate the starting and ending position. [TouchInjectionScope]
 * includes the dimension of the layout: [TouchInjectionScope.left], [TouchInjectionScope.center],
 * etc.
 *
 * If [endWithUp] is false, the touch pointer will remain down at [to]. In which case you'll have
 * to make sure you lift the pointer later on, eg:
 *
 * ```
 * rule.onNodeWithTag("MyTag")
 *     .performTouchInput {
 *         up()
 *     }
 * ```
 */
internal fun SemanticsNodeInteraction.performSwipe(
    from: TouchInjectionScope.() -> Offset,
    to: TouchInjectionScope.() -> Offset,
    endWithUp: Boolean = true
) {
    performTouchInput {
        // Do a periodic swipe between two points that lasts 500ms
        val start = from()
        val end = to()
        val durationMillis = 500L
        val durationMillisFloat = durationMillis.toFloat()

        // Start touch input
        down(0, start)

        val steps = (durationMillisFloat / eventPeriodMillis.toFloat()).roundToInt()
        var step = 0

        val getPositionAt: (Long) -> Offset = {
            lerp(start, end, it.toFloat() / durationMillis)
        }

        var tP = 0L
        while (step++ < steps) {
            val progress = step / steps.toFloat()
            val tn = lerp(0, durationMillis, progress)
            updatePointerTo(0, getPositionAt(tn))
            move(tn - tP)
            tP = tn
        }
        if (endWithUp) {
            up()
        }
    }
}
