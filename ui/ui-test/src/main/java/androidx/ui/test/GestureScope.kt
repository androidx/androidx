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

package androidx.ui.test

import androidx.ui.core.Duration
import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.milliseconds

/**
 * An object that has an associated component in which one can inject gestures. The gestures can
 * be injected by calling methods defined on [GestureScope], such as [sendSwipeUp]. The associated
 * component is the [SemanticsTreeNode] found by one of the finder methods such as [findByTag].
 *
 * Example usage:
 * findByTag("myWidget")
 *    .doGesture {
 *        sendSwipeUp()
 *    }
 */
class GestureScope internal constructor(
    internal val semanticsNodeInteraction: SemanticsNodeInteraction
) {
    internal inline val semanticsTreeNode
        get() = semanticsNodeInteraction.semanticsTreeNode
    internal inline val semanticsTreeInteraction
        get() = semanticsNodeInteraction.semanticsTreeInteraction
}

/**
 * The distance of a swipe's start position from the node's edge, in terms of the node's length.
 * We do not start the swipe exactly on the node's edge, but somewhat more inward, since swiping
 * from the exact edge may behave in an unexpected way (e.g. may open a navigation drawer).
 */
private const val edgeFuzzFactor = 0.083f

/**
 * Performs a click gesture on the given coordinate on the associated component. The coordinate
 * ([x], [y]) is in the component's local coordinate system.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendClick(x: Float, y: Float) {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform click on!")
    val xOffset = globalRect.left
    val yOffset = globalRect.top

    semanticsTreeInteraction.sendInput {
        it.sendClick(x + xOffset, y + yOffset)
    }
}

/**
 * Performs a click gesture on the associated component. The click is done in the middle of the
 * component's bounds.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendClick() {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform click on!")
    val x = globalRect.width / 2
    val y = globalRect.height / 2

    sendClick(x, y)
}

/**
 * Performs the swipe gesture on the associated component. The MotionEvents are linearly
 * interpolated between ([x0], [y0]) and ([x1], [y1]). The coordinates are in the component's local
 * coordinate system, i.e. (0, 0) is the top left corner of the component. The default duration is
 * 200 milliseconds.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipe(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    duration: Duration = 200.milliseconds
) {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val xOffset = globalRect.left
    val yOffset = globalRect.top

    semanticsTreeInteraction.sendInput {
        it.sendSwipe(x0 + xOffset, y0 + yOffset, x1 + xOffset, y1 + yOffset, duration)
    }
}

/**
 * Performs a swipe up gesture on the associated component. The gesture starts slightly above the
 * bottom of the component and ends at the top.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeUp() {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val x = globalRect.width / 2
    val y0 = globalRect.height * (1 - edgeFuzzFactor)
    val y1 = 0f

    sendSwipe(x, y0, x, y1, 200.milliseconds)
}

/**
 * Performs a swipe down gesture on the associated component. The gesture starts slightly below the
 * top of the component and ends at the bottom.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeDown() {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val x = globalRect.width / 2
    val y0 = globalRect.height * edgeFuzzFactor
    val y1 = globalRect.height

    sendSwipe(x, y0, x, y1, 200.milliseconds)
}

/**
 * Performs a swipe left gesture on the associated component. The gesture starts slightly left of
 * the right side of the component and ends at the left side.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeLeft() {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val x0 = globalRect.width * (1 - edgeFuzzFactor)
    val x1 = 0f
    val y = globalRect.height / 2

    sendSwipe(x0, y, x1, y, 200.milliseconds)
}

/**
 * Performs a swipe right gesture on the associated component. The gesture starts slightly right of
 * the left side of the component and ends at the right side.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeRight() {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val x0 = globalRect.width * edgeFuzzFactor
    val x1 = globalRect.width
    val y = globalRect.height / 2

    sendSwipe(x0, y, x1, y, 200.milliseconds)
}
