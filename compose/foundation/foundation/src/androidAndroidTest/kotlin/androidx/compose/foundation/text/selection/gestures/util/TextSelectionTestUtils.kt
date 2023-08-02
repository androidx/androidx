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

package androidx.compose.foundation.text.selection.gestures.util

import androidx.compose.foundation.MagnifierPositionInRoot
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth

private fun ComposeTestRule.assertSelectionHandlesShown(shown: Boolean) {
    listOf(Handle.SelectionStart, Handle.SelectionEnd)
        .map { onNode(isSelectionHandle(it)) }
        .forEach { if (shown) it.assertExists() else it.assertDoesNotExist() }
}

private fun ComposeTestRule.assertMagnifierShown(shown: Boolean) {
    if (!isPlatformMagnifierSupported()) return

    val magShown = onAllNodes(SemanticsMatcher.keyIsDefined(MagnifierPositionInRoot))
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .also { waitForIdle() }
        .any {
            it.config[MagnifierPositionInRoot]
                .invoke()
                .isSpecified
        }

    Truth.assertWithMessage("Magnifier should${if (shown) " " else " not "}be shown.")
        .that(magShown)
        .isEqualTo(shown)
}

private fun TextToolbar.assertShown(shown: Boolean = true) {
    Truth.assertWithMessage("Text toolbar status was not as expected.")
        .that(status)
        .isEqualTo(if (shown) TextToolbarStatus.Shown else TextToolbarStatus.Hidden)
}

private fun FakeHapticFeedback.assertPerformedAtLeastThenClear(times: Int) {
    Truth.assertThat(invocationCountMap[HapticFeedbackType.TextHandleMove] ?: 0).isAtLeast(times)
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
    var selectionHandlesShown = false
    var textToolbarShown = false
    var magnifierShown = false
    var hapticsCount = 0

    fun assert() {
        subAssert()
        rule.assertSelectionHandlesShown(selectionHandlesShown)
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
internal val Int.collapsed: TextRange get() = TextRange(this, this)
