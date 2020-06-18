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

package androidx.ui.test

import androidx.ui.core.AndroidOwner
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.PxBounds
import androidx.ui.unit.height
import androidx.ui.unit.width
import kotlin.math.absoluteValue

private const val floatTolerance = 0.5f

/**
 * Asserts that the layout of this node has width equal to [expectedWidth].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertWidthIsEqualTo(expectedWidth: Dp): SemanticsNodeInteraction {
    return withBoundsInRoot {
        areEqualOrThrow("width", it.width, expectedWidth)
    }
}

/**
 * Asserts that the layout of this node has height equal to [expectedHeight].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertHeightIsEqualTo(expectedHeight: Dp): SemanticsNodeInteraction {
    return withBoundsInRoot {
        areEqualOrThrow("height", it.height, expectedHeight)
    }
}
/**
 * Asserts that the layout of this node has width that is greater ot equal to [expectedMinWidth].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertWidthIsAtLeast(expectedMinWidth: Dp): SemanticsNodeInteraction {
    return withBoundsInRoot {
        isAtLeastOrThrow("width", it.width, expectedMinWidth)
    }
}

/**
 * Asserts that the layout of this node has height that is greater ot equal to [expectedMinHeight].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertHeightIsAtLeast(
    expectedMinHeight: Dp
): SemanticsNodeInteraction {
    return withBoundsInRoot {
        isAtLeastOrThrow("height", it.height, expectedMinHeight)
    }
}

private fun SemanticsNodeInteraction.withBoundsInRoot(
    assertion: Density.(PxBounds) -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
    val density = (node.componentNode.owner as AndroidOwner).density

    assertion.invoke(density, node.boundsInRoot)
    return this
}

private fun Density.areEqualOrThrow(
    subject: String,
    actualPx: Float,
    expected: Dp
) {
    val diff = (actualPx - expected.toPx()).absoluteValue
    if (diff > floatTolerance) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is ${actualPx.toDp()}, expected $expected")
    }
}

private fun Density.isAtLeastOrThrow(
    subject: String,
    actualPx: Float,
    expected: Dp
) {
    if (actualPx + floatTolerance < expected.toPx()) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is ${actualPx.toDp()}, expected at least $expected")
    }
}