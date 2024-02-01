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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.Handle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import com.google.common.truth.Truth.assertWithMessage

/**
 * Matches selection handles by looking for the [SelectionHandleInfoKey] property that has a
 * [SelectionHandleInfo] with the given [handle]. If [handle] is null (the default), then all
 * handles are matched.
 */
internal fun isSelectionHandle(handle: Handle? = null) =
    SemanticsMatcher("is ${handle ?: "any"} handle") { node ->
        if (handle == null) {
            SelectionHandleInfoKey in node.config
        } else {
            node.config.getOrNull(SelectionHandleInfoKey)?.handle == handle
        }
    }

/**
 * Asserts about the [SelectionHandleInfo.position] for the matching node. This is the position of
 * the handle's _anchor_, not the position of the popup itself. E.g. for a cursor handle this is the
 * position of the bottom of the cursor, which will be in the center of the popup.
 */
internal fun SemanticsNodeInteraction.assertHandlePositionMatches(
    expectedX: Dp,
    expectedY: Dp
) {
    val node = fetchSemanticsNode()
    with(node.layoutInfo.density) {
        val positionFound = node.getSelectionHandleInfo().position
        val positionFoundX = positionFound.x.toDp()
        val positionFoundY = positionFound.y.toDp()
        val message = "Expected position ($expectedX, $expectedY), " +
            "but found ($positionFoundX, $positionFoundY)"
        assertWithMessage(message).that(positionFoundX.value)
            .isWithin(5f).of(expectedX.value)
        assertWithMessage(message).that(positionFoundY.value)
            .isWithin(5f).of(expectedY.value)
    }
}

internal fun SemanticsNodeInteraction.assertHandleAnchorMatches(
    anchor: SelectionHandleAnchor
) {
    val actualAnchor = fetchSemanticsNode().getSelectionHandleInfo().anchor
    val message = "Expected anchor ($anchor), but found ($actualAnchor)"
    assertWithMessage(message).that(actualAnchor).isEqualTo(anchor)
}

internal fun SemanticsNode.getSelectionHandleInfo(): SelectionHandleInfo {
    val message = "Expected node to have SelectionHandleInfo."
    assertWithMessage(message).that(SelectionHandleInfoKey in config).isTrue()
    return config[SelectionHandleInfoKey]
}

internal fun ComposeTestRule.withHandlePressed(
    handle: Handle,
    block: HandlePressedScope.() -> Unit
) {
    onNode(isSelectionHandle(handle)).run {
        assertExists()
        performTouchInput { down(center) }
        HandlePressedScope(this@withHandlePressed, this@run).block()
        performTouchInput { up() }
    }
}

// TODO(b/308691180) The necessity of this class is due to the test input mechanism using root
//  coordinates as its base rather than screen coordinates. As the handle moves, so does the
//  root, so we have to take into account the root changing positions into our gestures.
//  Once the test input mechanism supports screen coordinates, this class should be able to be
//  refactored to contain less logic or removed completely.
internal class HandlePressedScope(
    private val rule: ComposeTestRule,
    private val interaction: SemanticsNodeInteraction,
) {
    // position of the popup before the previous move, relative to the container.
    private var previousPopupPosition: Offset = Offset.Unspecified

    // position of the gesture in progress, relative to the container.
    private var gesturePosition: Offset = fetchHandleInfo().position

    fun setInitialGesturePosition(initialGesturePosition: Offset) {
        check(gesturePosition.isUnspecified) {
            "Can't set the gesture position if it is already specified. " +
                "You should only set the position if the handle is invisible " +
                "and only after instantiation."
        }
        check(initialGesturePosition.isSpecified) {
            "Can't initialize the gesture position to unspecified."
        }
        gesturePosition = initialGesturePosition
    }

    fun fetchHandleInfo(): SelectionHandleInfo =
        interaction.fetchSemanticsNode().getSelectionHandleInfo()

    fun moveHandleTo(containerOffset: Offset) {
        checkGesturePositionSpecified()
        val delta = containerOffset - gesturePosition
        moveHandleBy(delta)
    }

    private fun moveHandleBy(delta: Offset) {
        checkGesturePositionSpecified()
        val currentPopupPosition = fetchHandleInfo().position
        val popupDelta =
            if (previousPopupPosition.isSpecified && currentPopupPosition.isSpecified) {
                currentPopupPosition - previousPopupPosition
            } else {
                Offset.Zero
            }

        // Avoid updating previous position from specified to unspecified.
        // In these cases, the handle disappears, but does not move positions,
        // so we should maintain the previous position.
        if (currentPopupPosition.isSpecified || previousPopupPosition.isUnspecified) {
            previousPopupPosition = currentPopupPosition
        }

        // Since the underlying input framework uses the local coordinates of the gesture,
        // and the window may have moved under it, we have to apply the window movement to our
        // gesture as well to get the gesture to truly match our intention at the screen level.
        val adjustedDelta = delta - popupDelta
        performTouchInput {
            moveBy(adjustedDelta)
            gesturePosition += delta
        }
        rule.waitForIdle()
    }

    private fun checkGesturePositionSpecified() {
        check(gesturePosition.isSpecified) {
            "gesturePosition must be specified. If you try to move an invisible handle, " +
                "you must specify the initialGesturePosition yourself (The last spot it was" +
                "at before it turned invisible)."
        }
    }

    private fun performTouchInput(block: TouchInjectionScope.() -> Unit) {
        interaction.performTouchInput(block)
    }
}
