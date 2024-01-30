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

package androidx.compose.foundation.text.selection.gestures

import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.gestures.util.TextSelectionAsserter
import androidx.compose.foundation.text.selection.gestures.util.applyAndAssert
import androidx.compose.foundation.text.selection.gestures.util.collapsed
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.foundation.text.selection.gestures.util.to
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
internal abstract class TextSelectionGesturesBidiTest : AbstractSelectionGesturesTest() {

    override val pointerAreaTag = "selectionContainer"

    protected val selection = mutableStateOf<Selection?>(null)

    protected abstract val textContent: MutableState<String>
    protected abstract var asserter: TextSelectionAsserter

    @Composable
    abstract fun TextContent()

    @Composable
    override fun Content() {
        SelectionContainer(
            selection = selection.value,
            onSelectionChange = { selection.value = it },
            modifier = Modifier.testTag(pointerAreaTag)
        ) {
            TextContent()
        }
    }

    protected abstract fun characterPosition(offset: Int, isRtl: Boolean): Offset

    @Test
    fun whenTouch_withLongPress_selectsSingleWord() {
        performTouchGesture {
            longClick(characterPosition(26, isRtl = false))
        }

        asserter.applyAndAssert {
            selection = 24 to 29
            selectionHandlesShown = true
            textToolbarShown = true
            hapticsCount++
        }
    }

    @Test
    fun whenTouch_withLongPressThenDragLeftAndBack_selectsWordsThenChars() {
        touchLongPressThenDragForwardsAndBackTest(
            forwardOffset = characterPosition(22, isRtl = true),
            forwardSelection = 24 to 22,
            forwardEndDirection = ResolvedTextDirection.Rtl,
            backwardOffset = characterPosition(19, isRtl = true),
            backwardSelection = 24 to 19,
            backwardEndDirection = ResolvedTextDirection.Rtl,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragUpAndBack_ltrToRtl_selectsWordsThenChars() {
        touchLongPressThenDragForwardsAndBackTest(
            forwardOffset = characterPosition(3, isRtl = false),
            forwardSelection = 24 to 0,
            forwardEndDirection = ResolvedTextDirection.Ltr,
            backwardOffset = characterPosition(8, isRtl = true),
            backwardSelection = 24 to 8,
            backwardEndDirection = ResolvedTextDirection.Rtl,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragUpAndBack_rtlToLtr_selectsWordsThenChars() {
        touchLongPressThenDragForwardsAndBackTest(
            forwardOffset = characterPosition(8, isRtl = true),
            forwardSelection = 24 to 6,
            forwardEndDirection = ResolvedTextDirection.Rtl,
            backwardOffset = characterPosition(13, isRtl = false),
            backwardSelection = 24 to 13,
            backwardEndDirection = ResolvedTextDirection.Ltr,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragRightAndBack_selectsWordsThenChars() {
        touchLongPressThenDragForwardsAndBackTest(
            forwardOffset = characterPosition(31, isRtl = true),
            forwardSelection = 24 to 31,
            forwardEndDirection = ResolvedTextDirection.Rtl,
            backwardOffset = characterPosition(34, isRtl = true),
            backwardSelection = 24 to 34,
            backwardEndDirection = ResolvedTextDirection.Rtl,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragDownAndBack_ltrToRtl_selectsWordsThenChars() {
        touchLongPressThenDragForwardsAndBackTest(
            forwardOffset = characterPosition(51, isRtl = false),
            forwardSelection = 24 to 53,
            forwardEndDirection = ResolvedTextDirection.Ltr,
            backwardOffset = characterPosition(44, isRtl = true),
            backwardSelection = 24 to 44,
            backwardEndDirection = ResolvedTextDirection.Rtl,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragDownAndBack_rtlToLtr_selectsWordsThenChars() {
        touchLongPressThenDragForwardsAndBackTest(
            forwardOffset = characterPosition(44, isRtl = true),
            forwardSelection = 24 to 47,
            forwardEndDirection = ResolvedTextDirection.Ltr,
            backwardOffset = characterPosition(38, isRtl = false),
            backwardSelection = 24 to 38,
            backwardEndDirection = ResolvedTextDirection.Ltr,
        )
    }

    private fun touchLongPressThenDragForwardsAndBackTest(
        forwardOffset: Offset,
        forwardSelection: TextRange?,
        forwardEndDirection: ResolvedTextDirection,
        backwardOffset: Offset,
        backwardSelection: TextRange?,
        backwardEndDirection: ResolvedTextDirection,
    ) {
        performTouchGesture {
            longPress(characterPosition(26, isRtl = false))
        }

        asserter.applyAndAssert {
            selection = 24 to 29
            selectionHandlesShown = true
            magnifierShown = true
            hapticsCount++
        }

        touchDragTo(forwardOffset)

        asserter.applyAndAssert {
            selection = forwardSelection
            endLayoutDirection = forwardEndDirection
            hapticsCount++
        }

        touchDragTo(backwardOffset)

        asserter.applyAndAssert {
            selection = backwardSelection
            endLayoutDirection = backwardEndDirection
            hapticsCount++
        }

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
            magnifierShown = false
        }
    }

    @Test
    fun whenMouse_withSingleClick_collapsedSelectionAtClick() {
        performMouseGesture {
            click(characterPosition(26, isRtl = false))
        }

        asserter.applyAndAssert {
            selection = 26.collapsed
        }
    }

    @Test
    fun whenMouse_withSingleClickThenRelease_collapsedSelection() {
        performMouseGesture {
            moveTo(position = characterPosition(26, isRtl = false))
            press()
        }

        asserter.applyAndAssert {
            selection = 26.collapsed
        }

        performMouseGesture {
            release()
        }

        asserter.assert()
    }

    @Test
    fun whenMouse_withSingleClickThenDragLeft_selectsCharacters() {
        mouseSingleClickThenDragTest(
            endOffset = characterPosition(21, isRtl = true),
            endSelection = 26 to 21,
        )
    }

    @Test
    fun whenMouse_withSingleClickThenDragUp_selectsCharacters() {
        mouseSingleClickThenDragTest(
            endOffset = characterPosition(8, isRtl = true),
            endSelection = 26 to 8,
        )
    }

    @Test
    fun whenMouse_withSingleClickThenDragRight_selectsCharacters() {
        mouseSingleClickThenDragTest(
            endOffset = characterPosition(32, isRtl = true),
            endSelection = 26 to 32,
        )
    }

    @Test
    fun whenMouse_withSingleClickThenDragDown_selectsCharacters() {
        mouseSingleClickThenDragTest(
            endOffset = characterPosition(44, isRtl = true),
            endSelection = 26 to 44,
        )
    }

    private fun mouseSingleClickThenDragTest(
        endOffset: Offset,
        endSelection: TextRange?
    ) {
        mouseClicksThenDragTest(
            numClicks = 1,
            firstOffset = characterPosition(26, isRtl = false),
            firstSelection = 26.collapsed,
            firstStartDirection = ResolvedTextDirection.Ltr,
            secondOffset = endOffset,
            secondSelection = endSelection,
            secondStartDirection = ResolvedTextDirection.Ltr,
            secondEndDirection = ResolvedTextDirection.Rtl,
        )
    }

    @Test
    fun whenMouse_withDoubleClick_selectsWord() {
        performMouseGesture {
            repeat(2) { click(characterPosition(26, isRtl = false)) }
        }

        asserter.applyAndAssert {
            selection = 24 to 29
        }
    }

    @Test
    fun whenMouse_withDoubleClickThenDragLeft_selectsWords() {
        mouseDoubleClickThenDragTest(
            endOffset = characterPosition(21, isRtl = true),
            endSelection = 29 to 18,
            endDirection = ResolvedTextDirection.Rtl,
        )
    }

    @Test
    fun whenMouse_withDoubleClickThenDragUp_selectsWords() {
        mouseDoubleClickThenDragTest(
            endOffset = characterPosition(8, isRtl = true),
            endSelection = 29 to 6,
            endDirection = ResolvedTextDirection.Rtl,
        )
    }

    @Test
    fun whenMouse_withDoubleClickThenDragRight_selectsWords() {
        mouseDoubleClickThenDragTest(
            endOffset = characterPosition(32, isRtl = true),
            endSelection = 24 to 35,
            endDirection = ResolvedTextDirection.Ltr,
        )
    }

    @Test
    fun whenMouse_withDoubleClickThenDragDown_selectsWords() {
        mouseDoubleClickThenDragTest(
            endOffset = characterPosition(44, isRtl = true),
            endSelection = 24 to 47,
            endDirection = ResolvedTextDirection.Ltr,
        )
    }

    private fun mouseDoubleClickThenDragTest(
        endOffset: Offset,
        endSelection: TextRange?,
        endDirection: ResolvedTextDirection,
    ) {
        mouseClicksThenDragTest(
            numClicks = 2,
            firstOffset = characterPosition(26, isRtl = false),
            firstSelection = 24 to 29,
            firstStartDirection = ResolvedTextDirection.Ltr,
            secondOffset = endOffset,
            secondSelection = endSelection,
            secondStartDirection = ResolvedTextDirection.Ltr,
            secondEndDirection = endDirection,
        )
    }

    @Test
    fun whenMouse_withTripleClick_selectsParagraph() {
        performMouseGesture {
            repeat(3) { click(characterPosition(26, isRtl = false)) }
        }

        asserter.applyAndAssert {
            selection = 18 to 35
            startLayoutDirection = ResolvedTextDirection.Rtl
        }
    }

    @Test
    fun whenMouse_withTripleClickThenDragLeft_selectsParagraphs() {
        mouseTripleClickThenDragTest(
            endOffset = characterPosition(21, isRtl = true),
            endSelection = 18 to 35,
            startDirection = ResolvedTextDirection.Rtl,
        )
    }

    @Test
    fun whenMouse_withTripleClickThenDragUp_selectsParagraphs() {
        mouseTripleClickThenDragTest(
            endOffset = characterPosition(8, isRtl = true),
            endSelection = 35 to 0,
            startDirection = ResolvedTextDirection.Ltr,
        )
    }

    @Test
    fun whenMouse_withTripleClickThenDragRight_selectsParagraphs() {
        mouseTripleClickThenDragTest(
            endOffset = characterPosition(32, isRtl = true),
            endSelection = 18 to 35,
            startDirection = ResolvedTextDirection.Rtl,
        )
    }

    @Test
    fun whenMouse_withTripleClickThenDragDown_selectsParagraphs() {
        mouseTripleClickThenDragTest(
            endOffset = characterPosition(44, isRtl = true),
            endSelection = 18 to 53,
            startDirection = ResolvedTextDirection.Rtl,
        )
    }

    private fun mouseTripleClickThenDragTest(
        endOffset: Offset,
        endSelection: TextRange?,
        startDirection: ResolvedTextDirection,
    ) {
        mouseClicksThenDragTest(
            numClicks = 3,
            firstOffset = characterPosition(26, isRtl = false),
            firstSelection = 18 to 35,
            firstStartDirection = ResolvedTextDirection.Rtl,
            secondOffset = endOffset,
            secondSelection = endSelection,
            secondStartDirection = startDirection,
            secondEndDirection = ResolvedTextDirection.Ltr,
        )
    }

    private fun mouseClicksThenDragTest(
        numClicks: Int,
        firstOffset: Offset,
        firstSelection: TextRange?,
        firstStartDirection: ResolvedTextDirection,
        secondOffset: Offset,
        secondSelection: TextRange?,
        secondStartDirection: ResolvedTextDirection,
        secondEndDirection: ResolvedTextDirection,
    ) {
        check(numClicks > 0) { "Must be at least one click" }
        performMouseGesture {
            moveTo(firstOffset)
            press()
            repeat(numClicks - 1) {
                advanceEventTime()
                release()
                advanceEventTime()
                press()
            }
        }

        asserter.applyAndAssert {
            selection = firstSelection
            startLayoutDirection = firstStartDirection
            endLayoutDirection = ResolvedTextDirection.Ltr
        }

        mouseDragTo(secondOffset)

        asserter.applyAndAssert {
            selection = secondSelection
            startLayoutDirection = secondStartDirection
            endLayoutDirection = secondEndDirection
        }

        performMouseGesture {
            release()
        }

        asserter.assert()
    }
}
