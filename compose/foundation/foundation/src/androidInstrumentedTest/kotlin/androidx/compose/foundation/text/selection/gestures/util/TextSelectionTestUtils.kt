/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalTestApi::class)

package androidx.compose.foundation.text.selection.gestures.util

import androidx.compose.foundation.MagnifierPositionInRoot
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.getSelectionHandleInfo
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.util.lerp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.test.fail

internal fun ComposeTestRule.assertSelectionHandlesShown(startShown: Boolean, endShown: Boolean) {
    listOf(Handle.SelectionStart to startShown, Handle.SelectionEnd to endShown).forEach {
        (handle, shown) ->
        if (shown) {
            assertHandleShown(handle)
        } else {
            assertHandleNotShown(handle)
        }
    }
}

private fun ComposeTestRule.assertHandleShown(handle: Handle) {
    val node = onNode(isSelectionHandle(handle))
    node.assertExists()
    val info = node.fetchSemanticsNode().getSelectionHandleInfo()
    val message = "Handle exists but is not visible."
    assertWithMessage(message).that(info.visible).isTrue()
}

private fun ComposeTestRule.assertHandleNotShown(handle: Handle) {
    val nodes = onAllNodes(isSelectionHandle(handle)).fetchSemanticsNodes()
    when (nodes.size) {
        0 -> {
            // There are no handles, so this passes
        }
        1 -> {
            // verify the single handle is not visible
            val info = nodes.single().getSelectionHandleInfo()
            val message = "Handle exists and is visible."
            assertWithMessage(message).that(info.visible).isFalse()
        }
        else -> {
            fail("Found multiple $handle handles.")
        }
    }
}

private fun ComposeTestRule.assertMagnifierShown(shown: Boolean) {
    if (!isPlatformMagnifierSupported()) return

    val magShown =
        onAllNodes(SemanticsMatcher.keyIsDefined(MagnifierPositionInRoot))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .also { waitForIdle() }
            .any { it.config[MagnifierPositionInRoot].invoke().isSpecified }

    assertWithMessage("Magnifier should${if (shown) " " else " not "}be shown.")
        .that(magShown)
        .isEqualTo(shown)
}

private fun TextToolbar.assertShown(shown: Boolean = true) {
    assertWithMessage("Text toolbar status was not as expected.")
        .that(status)
        .isEqualTo(if (shown) TextToolbarStatus.Shown else TextToolbarStatus.Hidden)
}

private fun FakeHapticFeedback.assertPerformedAtLeastThenClear(times: Int) {
    assertThat(invocationCountMap[HapticFeedbackType.TextHandleMove] ?: 0).isAtLeast(times)
    invocationCountMap.clear()
}

internal class FakeHapticFeedback : HapticFeedback {
    val invocationCountMap = mutableMapOf<HapticFeedbackType, Int>().withDefault { 0 }

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        invocationCountMap[hapticFeedbackType] = 1 + (invocationCountMap[hapticFeedbackType] ?: 0)
    }
}

internal abstract class SelectionAsserter<S>(
    var textContent: String,
    private val rule: ComposeTestRule,
    private val textToolbar: TextToolbar,
    private val hapticFeedback: FakeHapticFeedback,
    protected val getActual: () -> S,
) {
    /**
     * Set to true if both handles are expected to be shown.
     *
     * Overridden by [startSelectionHandleShown] and [endSelectionHandleShown].
     */
    var selectionHandlesShown = false

    /**
     * Set to true if the start handle is expected to be shown.
     *
     * Overrides [selectionHandlesShown] unless this is `null`.
     */
    var startSelectionHandleShown: Boolean? = null

    /**
     * Set to true if the end handle is expected to be shown.
     *
     * Overrides [selectionHandlesShown] unless this is `null`.
     */
    var endSelectionHandleShown: Boolean? = null
    var textToolbarShown = false
    var magnifierShown = false
    var hapticsCount = 0
    var startLayoutDirection = ResolvedTextDirection.Ltr
    var endLayoutDirection = ResolvedTextDirection.Ltr

    open fun assert() {
        subAssert()
        rule.assertSelectionHandlesShown(
            startShown = startSelectionHandleShown ?: selectionHandlesShown,
            endShown = endSelectionHandleShown ?: selectionHandlesShown
        )
        textToolbar.assertShown(textToolbarShown)
        rule.assertMagnifierShown(magnifierShown)
        // reset haptics every assert.
        // with gestures, we could get multiple haptics per tested move
        hapticFeedback.assertPerformedAtLeastThenClear(hapticsCount).also { hapticsCount = 0 }
    }

    protected abstract fun subAssert()
}

internal abstract class TextSelectionAsserter(
    textContent: String,
    rule: ComposeTestRule,
    textToolbar: TextToolbar,
    hapticFeedback: FakeHapticFeedback,
    getActual: () -> Selection?,
) : SelectionAsserter<Selection?>(textContent, rule, textToolbar, hapticFeedback, getActual) {
    var selection: TextRange? = null
}

internal fun <S, T : SelectionAsserter<S>> T.applyAndAssert(block: T.() -> Unit) {
    block()
    assert()
}

internal fun TouchInjectionScope.longPress(offset: Offset) {
    down(offset)
    move(delayMillis = viewConfiguration.longPressTimeoutMillis + 10)
}

internal infix fun Int.to(other: Int): TextRange = TextRange(this, other)

internal val Int.collapsed: TextRange
    get() = TextRange(this, this)

internal fun SemanticsNodeInteraction.touchDragNodeTo(
    position: Offset,
    durationMillis: Long = 200L,
) {
    require(durationMillis > 0) { "Duration cannot be <= 0" }

    var startVar: Offset? = null
    var dragEventPeriodMillisVar: Long? = null
    performTouchInput {
        startVar = requireNotNull(currentPosition()) { "No pointer is down to animate." }
        dragEventPeriodMillisVar = eventPeriodMillis
    }

    val start = startVar!!
    val dragEventPeriodMillis = dragEventPeriodMillisVar!!

    // How many steps will we take in durationMillis?
    // At least 1, and a number that will bring as as close to eventPeriod as possible
    val steps = max(1, (durationMillis / dragEventPeriodMillis.toFloat()).roundToInt())

    var previousTime = 0L
    for (step in 1..steps) {
        val progress = step / steps.toFloat()
        val nextTime = lerp(0, stop = durationMillis, fraction = progress)
        val nextPosition = lerp(start, position, nextTime / durationMillis.toFloat())
        performTouchInput { moveTo(nextPosition, delayMillis = nextTime - previousTime) }
        previousTime = nextTime
    }
}

internal fun SemanticsNodeInteraction.touchDragNodeBy(delta: Offset, durationMillis: Long = 100L) {
    var startVar: Offset? = null
    performTouchInput {
        startVar = requireNotNull(currentPosition()) { "No pointer is down to animate." }
    }
    val start = startVar!!
    touchDragNodeTo(start + delta, durationMillis)
}

internal fun SemanticsNodeInteraction.mouseDragNodeTo(
    position: Offset,
    durationMillis: Long = 200L,
) {
    require(durationMillis > 0) { "Duration cannot be <= 0" }

    var startVar: Offset? = null
    var dragEventPeriodMillisVar: Long? = null
    performMouseInput {
        startVar = currentPosition
        dragEventPeriodMillisVar = eventPeriodMillis
    }
    val start = startVar!!
    val dragEventPeriodMillis = dragEventPeriodMillisVar!!

    // How many steps will we take in durationMillis?
    // At least 1, and a number that will bring as as close to eventPeriod as possible
    val steps = max(1, (durationMillis / dragEventPeriodMillis.toFloat()).roundToInt())

    var previousTime = 0L
    for (step in 1..steps) {
        val progress = step / steps.toFloat()
        val nextTime = lerp(0, stop = durationMillis, fraction = progress)
        val nextPosition = lerp(start, position, nextTime / durationMillis.toFloat())
        performMouseInput { moveTo(nextPosition, delayMillis = nextTime - previousTime) }
        previousTime = nextTime
    }
}

internal fun SemanticsNodeInteraction.mouseDragNodeBy(delta: Offset, durationMillis: Long = 100L) {
    var startVar: Offset? = null
    performMouseInput { startVar = currentPosition }
    val start = startVar!!
    mouseDragNodeTo(start + delta, durationMillis)
}
