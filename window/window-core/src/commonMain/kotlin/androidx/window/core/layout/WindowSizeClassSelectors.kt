/*
 * Copyright 2024 The Android Open Source Project
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

@file:JvmName("WindowSizeClassSelectors")

package androidx.window.core.layout

import kotlin.jvm.JvmName

/**
 * Returns the largest [WindowSizeClass] that is within the bounds of ([widthDp], [heightDp]). This
 * method prefers width and uses max height to break ties. If there is no match a default of
 * `WindowSizeClass(0,0)` is returned. Examples: Input: Set: `setOf(WindowSizeClass(300, 300),
 * WindowSizeClass(300, 600)` widthDp: `300.5f` heightDp: `800.5f` Output: `WindowSizeClass(300,
 * 600)` Input: Set: `setOf(WindowSizeClass(300, 300), WindowSizeClass(300, 600)` widthDp: `300`
 * heightDp: `400` Output: `WindowSizeClass(300, 300)`. This is an overload that truncates the
 * floats to integers.
 *
 * @param widthDp the width of the window to match a [WindowSizeClass] to.
 * @param heightDp the height of the window to match a [WindowSizeClass] to.
 * @see computeWindowSizeClass
 */
fun Set<WindowSizeClass>.computeWindowSizeClass(widthDp: Float, heightDp: Float): WindowSizeClass {
    return computeWindowSizeClass(widthDp.toInt(), heightDp.toInt())
}

/**
 * Returns the largest [WindowSizeClass] that is within the bounds of ([widthDp], [heightDp]). This
 * method prefers width and uses max height to break ties. If there is no match a default of
 * `WindowSizeClass(0,0)` is returned. Examples: Input: Set: `setOf(WindowSizeClass(300, 300),
 * WindowSizeClass(300, 600)` widthDp: `300` heightDp: `800` Output: `WindowSizeClass(300, 600)`
 * Input: Set: `setOf(WindowSizeClass(300, 300), WindowSizeClass(300, 600)` widthDp: `300` heightDp:
 * `400` Output: `WindowSizeClass(300, 300)`
 *
 * @param widthDp the width of the window to match a [WindowSizeClass] to.
 * @param heightDp the height of the window to match a [WindowSizeClass] to.
 */
fun Set<WindowSizeClass>.computeWindowSizeClass(widthDp: Int, heightDp: Int): WindowSizeClass {
    var maxWidth = 0
    forEach { bucket ->
        if (bucket.minWidthDp <= widthDp && bucket.minWidthDp > maxWidth) {
            maxWidth = bucket.minWidthDp
        }
    }
    var match = WindowSizeClass(0, 0)
    forEach { bucket ->
        if (
            bucket.minWidthDp == maxWidth &&
                bucket.minHeightDp <= heightDp &&
                match.minHeightDp <= bucket.minHeightDp
        ) {
            match = bucket
        }
    }
    return match
}

/**
 * Returns the largest [WindowSizeClass] that is within the bounds of ([widthDp], [heightDp]). This
 * method prefers height and uses max width to break ties. If there is no match a default of
 * `WindowSizeClass(0,0)` is returned. Examples: Input: Set: `setOf(WindowSizeClass(300, 300),
 * WindowSizeClass(600, 300)` widthDp: `800` heightDp: `300` Output: `WindowSizeClass(600, 300)`
 * Input: Set: `setOf(WindowSizeClass(300, 300), WindowSizeClass(600, 300)` widthDp: `400` heightDp:
 * `300` Output: `WindowSizeClass(300, 300)`
 *
 * @param widthDp the width of the window to match a [WindowSizeClass] to.
 * @param heightDp the height of the window to match a [WindowSizeClass] to.
 */
fun Set<WindowSizeClass>.computeWindowSizeClassPreferHeight(
    widthDp: Int,
    heightDp: Int
): WindowSizeClass {
    var maxHeight = 0
    forEach { bucket ->
        if (bucket.minHeightDp <= heightDp && bucket.minHeightDp > maxHeight) {
            maxHeight = bucket.minHeightDp
        }
    }
    var match = WindowSizeClass(0, 0)
    forEach { bucket ->
        if (
            bucket.minHeightDp == maxHeight &&
                bucket.minWidthDp <= widthDp &&
                match.minWidthDp <= bucket.minWidthDp
        ) {
            match = bucket
        }
    }
    return match
}
