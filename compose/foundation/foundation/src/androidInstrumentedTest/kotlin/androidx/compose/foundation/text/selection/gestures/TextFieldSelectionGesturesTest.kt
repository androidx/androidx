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

import androidx.compose.foundation.text.selection.gestures.AbstractSelectionGesturesTest.HorizontalDirection.START
import androidx.compose.foundation.text.selection.gestures.AbstractSelectionGesturesTest.VerticalDirection.DOWN
import androidx.compose.foundation.text.selection.gestures.AbstractSelectionGesturesTest.VerticalDirection.UP
import androidx.compose.foundation.text.selection.gestures.util.TextFieldSelectionAsserter
import androidx.compose.foundation.text.selection.gestures.util.applyAndAssert
import androidx.compose.foundation.text.selection.gestures.util.collapsed
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.foundation.text.selection.gestures.util.to
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
internal abstract class TextFieldSelectionGesturesTest : AbstractSelectionGesturesTest() {

    override val pointerAreaTag = "testTag"

    /**
     * Word to use in one-off tests. Subclasses may choose a RTL or BiDi 5 letter word for example.
     */
    protected abstract val word: String
    protected abstract val textFieldValue: MutableState<TextFieldValue>
    protected abstract var asserter: TextFieldSelectionAsserter

    protected abstract fun characterPosition(offset: Int): Offset

    @Before
    fun setupAsserter() {
        asserter = TextFieldSelectionAsserter(
            textContent = textFieldValue.value.text,
            rule = rule,
            textToolbar = textToolbar,
            hapticFeedback = hapticFeedback,
            getActual = { textFieldValue.value }
        )
    }

    @Test
    fun whenTouch_withLongPressOutOfBounds_nothingHappens() {
        performTouchGesture {
            longPress(topStart.nudge(yDirection = UP))
        }

        asserter.assert()
        touchDragTo(topEnd.nudge(yDirection = UP))
        asserter.assert()
    }

    @Test
    fun whenTouch_withNoTextThenLongPress_noSelection() {
        textFieldValue.value = TextFieldValue()
        rule.waitForIdle()

        performTouchGesture {
            longClick(center)
        }

        asserter.applyAndAssert {
            textContent = ""
            textToolbarShown = true // paste will show up if clipboard is not empty
            hapticsCount++
        }
    }

    @Test
    fun whenTouch_withNoTextThenLongPressAndDrag_noSelection() {
        textFieldValue.value = TextFieldValue()
        rule.waitForIdle()

        performTouchGesture {
            longPress(center)
        }

        asserter.applyAndAssert {
            textContent = ""
            hapticsCount++
        }

        touchDragTo(centerStart)

        asserter.assert()

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true // paste will show up if clipboard is not empty
        }
    }

    // Regression for magnifier not showing when the text field begins empty,
    // then text is added, the magnifier continues not to show.
    @Test
    fun whenTouch_withNoText_thenLongPressAndDrag_thenAddText_longPressAndDragAgain() {
        textFieldValue.value = TextFieldValue()
        rule.waitForIdle()

        performTouchGesture {
            longPress(center)
        }

        asserter.applyAndAssert {
            textContent = ""
            hapticsCount++
        }

        touchDragTo(centerStart)

        asserter.assert()

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true // paste will show up if clipboard is not empty
        }

        val newText = "Some Text"
        rule.onNodeWithTag(pointerAreaTag).performTextInput(newText)
        rule.waitForIdle()

        performTouchGesture {
            longPress(characterPosition(2))
        }

        asserter.applyAndAssert {
            textContent = newText
            selection = 0 to 4
            selectionHandlesShown = true
            magnifierShown = true
            textToolbarShown = false
            hapticsCount++
        }

        touchDragTo(characterPosition(6))

        asserter.applyAndAssert {
            selection = 0 to 9
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
    fun whenTouch_withLongPressThenClear_noSelection() {
        performTouchGesture {
            longClick(characterPosition(13))
        }

        asserter.applyAndAssert {
            selection = 12 to 17
            selectionHandlesShown = true
            textToolbarShown = true
            hapticsCount++
        }

        performTouchGesture {
            click(characterPosition(14))
        }

        asserter.applyAndAssert {
            selection = 14.collapsed
            selectionHandlesShown = false
            textToolbarShown = false
            cursorHandleShown = true
        }
    }

    @Test
    fun whenTouch_withLongPressThenDragLeftOutOfBounds_keepsFirstCharSelected() {
        performTouchGesture {
            longPress(characterPosition(9))
        }

        asserter.applyAndAssert {
            selection = 6 to 11
            selectionHandlesShown = true
            magnifierShown = true
            hapticsCount++
        }

        touchDragTo(centerStart)
        // drag to just inside the left bound, one char should remain selected
        asserter.applyAndAssert {
            selection = 6 to 7
            hapticsCount++
        }

        touchDragTo(centerStart.nudge(START))
        // drag just outside of the left bound, should be no change.
        // Regression: we want to ensure the selection doesn't travel to a line above the cursor
        asserter.assert()

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
            magnifierShown = false
        }
    }

    @Test
    fun whenTouch_withLongPressThenDragLeftOutOfBoundsUpAndDown_selectsLines() {
        performTouchGesture {
            longPress(characterPosition(9))
        }

        // anchor starts at beginning of middle line
        asserter.applyAndAssert {
            selection = 6 to 11
            selectionHandlesShown = true
            magnifierShown = true
            hapticsCount++
        }

        // beginning of middle line
        touchDragTo(characterPosition(6) + Offset(-2f, 0f))
        asserter.applyAndAssert {
            selection = 6 to 7
            hapticsCount++
        }

        // beginning of top line
        touchDragTo(characterPosition(0) + Offset(-2f, 0f))
        asserter.applyAndAssert {
            selection = 6 to 0
            hapticsCount++
        }

        // above top line
        touchDragTo(topStart.nudge(yDirection = UP))
        asserter.assert()

        // below bottom line, should be end of text completely
        touchDragTo(bottomStart.nudge(yDirection = DOWN))
        asserter.applyAndAssert {
            selection = 6 to 29
            hapticsCount++
        }
    }

    @Test
    fun whenTouch_verifyOneCharStaysSelected_withinLine() {
        performTouchGesture {
            longPress(characterPosition(14))
        }

        asserter.applyAndAssert {
            selection = 12 to 17
            selectionHandlesShown = true
            magnifierShown = true
            hapticsCount++
        }

        touchDragTo(characterPosition(13))
        asserter.applyAndAssert {
            selection = 12 to 13
            hapticsCount++
        }

        touchDragTo(characterPosition(12))
        // shouldn't allow collapsed selection, but keeps previous single char selection
        asserter.assert()

        touchDragTo(characterPosition(11))
        asserter.applyAndAssert {
            selection = 12 to 11
            hapticsCount++
        }
    }

    @Test
    fun whenTouch_withLongPress_selectsSingleWord() {
        performTouchGesture {
            longClick(characterPosition(13))
        }

        asserter.applyAndAssert {
            selection = 12 to 17
            selectionHandlesShown = true
            textToolbarShown = true
            hapticsCount++
        }
    }

    @Test
    fun whenTouch_withLongPressesOnMultipleWords_selectsSingleWords() {
        asserter.selectionHandlesShown = true
        asserter.textToolbarShown = true

        fun longClickCharacterPositionThenApplyAndAssert(offset: Int, selection: TextRange) {
            performTouchGesture {
                longClick(characterPosition(offset))
            }

            asserter.applyAndAssert {
                this.selection = selection
                hapticsCount++
            }
        }

        longClickCharacterPositionThenApplyAndAssert(offset = 2, selection = 0 to 5)
        longClickCharacterPositionThenApplyAndAssert(offset = 8, selection = 6 to 11)
        longClickCharacterPositionThenApplyAndAssert(offset = 13, selection = 12 to 17)
        longClickCharacterPositionThenApplyAndAssert(offset = 20, selection = 18 to 23)
        longClickCharacterPositionThenApplyAndAssert(offset = 26, selection = 24 to 29)
    }

    @Test
    fun whenTouch_withLongPressThenDragLeftAndBack_selectsWordsThenChars() {
        touchLongPressThenDragForwardAndBackTest(
            forwardOffset = characterPosition(8),
            forwardSelection = 12 to 6,
            backwardOffset = characterPosition(9),
            backwardSelection = 12 to 9,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragUpAndBack_selectsWordsThenChars() {
        touchLongPressThenDragForwardAndBackTest(
            forwardOffset = characterPosition(2),
            forwardSelection = 12 to 0,
            backwardOffset = characterPosition(3),
            backwardSelection = 12 to 3,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragRightAndBack_selectsWordsThenChars() {
        touchLongPressThenDragForwardAndBackTest(
            forwardOffset = characterPosition(21),
            forwardSelection = 12 to 23,
            backwardOffset = characterPosition(20),
            backwardSelection = 12 to 20,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragDownAndBack_selectsWordsThenChars() {
        // entering the bottom paragraph will select word since it is a forward selection,
        // as we continue animating towards the forward position,
        // it will be selecting by character because it will be backwards selecting at that point.
        touchLongPressThenDragForwardAndBackTest(
            forwardOffset = characterPosition(27),
            forwardSelection = 12 to 27,
            backwardOffset = characterPosition(26),
            backwardSelection = 12 to 26,
        )
    }

    private fun touchLongPressThenDragForwardAndBackTest(
        forwardOffset: Offset,
        forwardSelection: TextRange,
        backwardOffset: Offset,
        backwardSelection: TextRange,
    ) {
        performTouchGesture {
            longPress(characterPosition(13))
        }

        asserter.applyAndAssert {
            selection = 12 to 17
            selectionHandlesShown = true
            magnifierShown = true
            hapticsCount++
        }

        touchDragTo(forwardOffset)

        asserter.applyAndAssert {
            selection = forwardSelection
            hapticsCount++
        }

        touchDragTo(backwardOffset)

        asserter.applyAndAssert {
            selection = backwardSelection
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
    fun whenTouch_withLongPressThenDragToUpperEndPaddingAndBack_selectsWordsThenChars() {
        touchLongPressThenDragToEndPaddingTest(
            endOffset = topEnd,
            endSelection = 12 to 0,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragToMiddleEndPaddingAndBack_selectsWordsThenChars() {
        touchLongPressThenDragToEndPaddingTest(
            endOffset = centerEnd,
            endSelection = 12 to 23,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragToLowerEndPaddingAndBack_selectsWordsThenChars() {
        touchLongPressThenDragToEndPaddingTest(
            endOffset = bottomEnd,
            endSelection = 12 to 29,
        )
    }

    private fun touchLongPressThenDragToEndPaddingTest(
        endOffset: Offset,
        endSelection: TextRange,
    ) {
        performTouchGesture {
            longPress(characterPosition(13))
        }

        asserter.applyAndAssert {
            selection = 12 to 17
            selectionHandlesShown = true
            magnifierShown = true
            hapticsCount++
        }

        touchDragTo(endOffset)

        asserter.applyAndAssert {
            selection = endSelection
            magnifierShown = false
            hapticsCount++
        }

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
        }
    }

    @Test
    fun whenTouch_withLongPressInEndPadding_entersSelectionMode() {
        performTouchGesture {
            longPress(topEnd)
        }

        asserter.applyAndAssert {
            selection = 5.collapsed
            cursorHandleShown = true
            hapticsCount++
        }

        // we want to test at least one drag that shouldn't affect selection as well
        touchDragBy(Offset(-1f, 0f))

        asserter.assert()

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
        }
    }

    @Test
    fun whenTouch_withLongPressInEndPaddingOfEmptyLine_entersSelectionMode() {
        val content = "$word\n\n$word"
        textFieldValue.value = TextFieldValue(content)
        rule.waitForIdle()

        performTouchGesture {
            longPress(centerEnd)
        }

        asserter.applyAndAssert {
            textContent = content
            selection = 6.collapsed
            cursorHandleShown = true
            hapticsCount++
        }

        // we want to test at least one drag that shouldn't affect selection as well
        touchDragBy(Offset(-1f, 0f))

        asserter.assert()

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
        }
    }

    @Test
    fun whenTouch_withLongPressInEndPaddingThenDragToUpperEndPadding_selectsParagraphAndNewLine() {
        performTouchGesture {
            longPress(centerEnd)
        }

        asserter.applyAndAssert {
            selection = 23.collapsed
            cursorHandleShown = true
            hapticsCount++
        }

        touchDragTo(topEnd)

        asserter.applyAndAssert {
            selection = 23 to 0
            cursorHandleShown = false
            selectionHandlesShown = true
            hapticsCount++
        }

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
        }
    }

    @Test
    fun whenTouch_withLongPressInEndPaddingThenDragToLowerEndPadding_selectsNewLineAndParagraph() {
        performTouchGesture {
            longPress(centerEnd)
        }

        asserter.applyAndAssert {
            selection = 23.collapsed
            cursorHandleShown = true
            hapticsCount++
        }

        touchDragTo(bottomEnd)

        asserter.applyAndAssert {
            selection = 23 to 29
            cursorHandleShown = false
            selectionHandlesShown = true
            hapticsCount++
        }

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
        }
    }

    @Test
    fun whenTouch_withLongPressInEndPaddingOfFinalLine_entersSelectionMode() {
        performTouchGesture {
            longPress(bottomEnd)
        }

        asserter.applyAndAssert {
            selection = 29.collapsed
            cursorHandleShown = true
            hapticsCount++
        }

        // we want to test at least one drag that shouldn't affect selection as well
        touchDragBy(Offset(-1f, 0f))

        asserter.assert()

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
        }
    }

    @Test
    fun whenTouch_withLongPressThanDragAcrossSingleWord_onlySelectsSingleWordAndNoOtherChanges() {
        performTouchGesture {
            longPress(characterPosition(15))
        }

        asserter.applyAndAssert {
            selection = 12 to 17
            selectionHandlesShown = true
            magnifierShown = true
            hapticsCount++
        }

        touchDragTo(characterPosition(13))

        asserter.applyAndAssert {
            selection = 12 to 13
            hapticsCount++
        }

        touchDragTo(characterPosition(15))

        asserter.applyAndAssert {
            selection = 12 to 15
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
    fun whenTouch_withLongPressInEndPaddingOfEmptyFinalLine_entersSelectionMode() {
        val content = "$word\n\n"
        textFieldValue.value = TextFieldValue(content)
        rule.waitForIdle()

        performTouchGesture {
            longPress(bottomEnd)
        }

        asserter.applyAndAssert {
            textContent = content
            selection = 7.collapsed
            cursorHandleShown = true
            hapticsCount++
        }

        // we want to test at least one drag that shouldn't affect selection as well
        touchDragBy(Offset(-1f, 0f))

        asserter.assert()

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
        }
    }

    // Regression test for a mouse long click resulting in touch behaviors for selection.
    @Test
    fun whenMouse_withLongClick_collapsedSelectionAtClick() {
        performMouseGesture {
            longClick(characterPosition(13))
        }

        asserter.applyAndAssert {
            selection = 13.collapsed
        }
    }

    @Test
    fun whenMouse_withClick_collapsedSelectionAtClick() {
        performMouseGesture {
            click(characterPosition(13))
        }

        asserter.applyAndAssert {
            selection = 13.collapsed
        }
    }

    @Test
    fun whenMouse_withSingleClick_collapsedSelection() {
        performMouseGesture {
            moveTo(position = characterPosition(13))
            press()
        }

        asserter.applyAndAssert {
            selection = 13.collapsed
        }

        performMouseGesture {
            release()
        }

        asserter.assert()
    }

    @Test
    fun whenMouse_withSingleClickThenDragLeft_selectsCharacters() {
        mouseSingleClickThenDragTest(
            endOffset = characterPosition(8),
            endSelection = 13 to 8,
        )
    }

    @Test
    fun whenMouse_withSingleClickThenDragUp_selectsCharacters() {
        mouseSingleClickThenDragTest(
            endOffset = characterPosition(2),
            endSelection = 13 to 2,
        )
    }

    @Test
    fun whenMouse_withSingleClickThenDragRight_selectsCharacters() {
        mouseSingleClickThenDragTest(
            endOffset = characterPosition(19),
            endSelection = 13 to 19,
        )
    }

    @Test
    fun whenMouse_withSingleClickThenDragDown_selectsCharacters() {
        mouseSingleClickThenDragTest(
            endOffset = characterPosition(26),
            endSelection = 13 to 26,
        )
    }

    private fun mouseSingleClickThenDragTest(endOffset: Offset, endSelection: TextRange) {
        mouseClicksThenDragTest(
            numClicks = 1,
            startOffset = characterPosition(13),
            endOffset = endOffset,
            startSelection = 13.collapsed,
            endSelection = endSelection,
        )
    }

    @Test
    fun whenMouse_withDoubleClick_selectsWord() {
        performMouseGesture {
            repeat(2) { click(characterPosition(13)) }
        }

        asserter.applyAndAssert {
            selection = 12 to 17
        }
    }

    @Test
    fun whenMouse_withDoubleClickThenDragLeft_selectsWords() {
        mouseDoubleClickThenDragTest(
            endOffset = characterPosition(8),
            endSelection = 17 to 6,
        )
    }

    @Test
    fun whenMouse_withDoubleClickThenDragUp_selectsWords() {
        mouseDoubleClickThenDragTest(
            endOffset = characterPosition(2),
            endSelection = 17 to 0,
        )
    }

    @Test
    fun whenMouse_withDoubleClickThenDragRight_selectsWords() {
        mouseDoubleClickThenDragTest(
            endOffset = characterPosition(19),
            endSelection = 12 to 23,
        )
    }

    @Test
    fun whenMouse_withDoubleClickThenDragDown_selectsWords() {
        mouseDoubleClickThenDragTest(
            endOffset = characterPosition(26),
            endSelection = 12 to 29,
        )
    }

    private fun mouseDoubleClickThenDragTest(endOffset: Offset, endSelection: TextRange) {
        mouseClicksThenDragTest(
            numClicks = 2,
            startOffset = characterPosition(13),
            endOffset = endOffset,
            startSelection = 12 to 17,
            endSelection = endSelection,
        )
    }

    @Test
    fun whenMouse_withTripleClick_selectsParagraph() {
        performMouseGesture {
            repeat(3) { click(characterPosition(13)) }
        }

        asserter.applyAndAssert {
            selection = 6 to 23
        }
    }

    @Test
    fun whenMouse_withTripleClickThenDragLeft_selectsParagraphs() {
        mouseTripleClickThenDragTest(
            endOffset = characterPosition(8),
            endSelection = 6 to 23,
        )
    }

    @Test
    fun whenMouse_withTripleClickThenDragUp_selectsParagraphs() {
        mouseTripleClickThenDragTest(
            endOffset = characterPosition(2),
            endSelection = 23 to 0,
        )
    }

    @Test
    fun whenMouse_withTripleClickThenDragRight_selectsParagraphs() {
        mouseTripleClickThenDragTest(
            endOffset = characterPosition(19),
            endSelection = 6 to 23,
        )
    }

    @Test
    fun whenMouse_withTripleClickThenDragDown_selectsParagraphs() {
        mouseTripleClickThenDragTest(
            endOffset = characterPosition(26),
            endSelection = 6 to 29,
        )
    }

    private fun mouseTripleClickThenDragTest(endOffset: Offset, endSelection: TextRange) {
        mouseClicksThenDragTest(
            numClicks = 3,
            startOffset = characterPosition(13),
            endOffset = endOffset,
            startSelection = 6 to 23,
            endSelection = endSelection,
        )
    }

    @Test
    fun whenMouse_withSingleClickOnFirstLetterOfLine_collapsedSelection() {
        mouseFirstLetterOfLineClicksTest(
            numClicks = 1,
            selection = 6.collapsed,
        )
    }

    @Test
    fun whenMouse_withDoubleClickOnFirstLetterOfLine_selectsFirstWord() {
        mouseFirstLetterOfLineClicksTest(
            numClicks = 2,
            selection = 6 to 11,
        )
    }

    @Test
    fun whenMouse_withTripleClickOnFirstLetterOfLine_selectsParagraph() {
        mouseFirstLetterOfLineClicksTest(
            numClicks = 3,
            selection = 6 to 23,
        )
    }

    // regression test for when selections would overflow onto previous line
    private fun mouseFirstLetterOfLineClicksTest(numClicks: Int, selection: TextRange) {
        val initialClickOffset = characterPosition(6)
        mouseClicksThenDragTest(
            numClicks = numClicks,
            startOffset = initialClickOffset,
            endOffset = initialClickOffset + Offset(0f, 1f),
            startSelection = selection,
            endSelection = selection,
        )
    }

    @Test
    fun whenMouse_withSingleClickInEndPaddingOfLine_collapsedSelection() {
        mouseEndPaddingClicksTest(
            numClicks = 1,
            selection = 23.collapsed,
        )
    }

    @Test
    fun whenMouse_withDoubleClickOInEndPaddingOfLine_selectsLastWord() {
        mouseEndPaddingClicksTest(
            numClicks = 2,
            selection = 18 to 23,
        )
    }

    @Test
    fun whenMouse_withTripleClickInEndPaddingOfLine_selectsParagraph() {
        mouseEndPaddingClicksTest(
            numClicks = 3,
            selection = 6 to 23,
        )
    }

    // regression test for when selections would overflow onto next line
    private fun mouseEndPaddingClicksTest(numClicks: Int, selection: TextRange) {
        val initialClickOffset = centerEnd
        mouseClicksThenDragTest(
            numClicks = numClicks,
            startOffset = initialClickOffset,
            endOffset = initialClickOffset.nudge(yDirection = DOWN),
            startSelection = selection,
            endSelection = selection,
        )
    }

    private fun mouseClicksThenDragTest(
        numClicks: Int,
        startOffset: Offset,
        endOffset: Offset,
        startSelection: TextRange,
        endSelection: TextRange
    ) {
        check(numClicks > 0) { "Must be at least one click" }
        performMouseGesture {
            moveTo(startOffset)
            press()
            repeat(numClicks - 1) {
                advanceEventTime()
                release()
                advanceEventTime()
                press()
            }
        }

        asserter.applyAndAssert {
            selection = startSelection
        }

        mouseDragTo(endOffset)

        asserter.applyAndAssert {
            selection = endSelection
        }

        performMouseGesture {
            release()
        }

        asserter.assert()
    }

    @Test
    fun whenMouse_thenTouch_touchBehaviorsAppear() {
        performMouseGesture {
            repeat(2) { click(characterPosition(13)) }
        }

        asserter.applyAndAssert {
            selection = 12 to 17
        }

        performTouchGesture {
            enterTouchMode()
        }

        asserter.applyAndAssert {
            selectionHandlesShown = true
            textToolbarShown = true
        }
    }

    @Test
    fun whenTouch_thenMouse_touchBehaviorsDisappear() {
        performTouchGesture {
            longClick(characterPosition(13))
        }

        asserter.applyAndAssert {
            selection = 12 to 17
            selectionHandlesShown = true
            textToolbarShown = true
            hapticsCount++
        }

        enterMouseMode()

        asserter.applyAndAssert {
            selectionHandlesShown = false
            textToolbarShown = false
        }
    }

    // Regression test for when this would result in text toolbar showing instead of the cursor.
    @Test
    fun whenMouseCollapsedSelection_thenTouch_ToolbarAndCursorAppears() {
        performMouseGesture {
            click(characterPosition(13))
        }

        asserter.applyAndAssert {
            selection = 13.collapsed
        }

        performTouchGesture {
            enterTouchMode()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
            cursorHandleShown = true
        }
    }

    @Test
    fun whenTouchCollapsedSelection_thenMouse_noUiElements() {
        performTouchGesture {
            click(characterPosition(13))
        }

        asserter.applyAndAssert {
            selection = 13.collapsed
            cursorHandleShown = true
        }

        enterMouseMode()

        asserter.applyAndAssert {
            cursorHandleShown = false
        }
    }

    // Regression test for when this instead selected the current and next (if any) paragraph.
    @Test
    fun whenMouse_thenTripleClickInEndPadding_selectsCurrentParagraph() {
        performMouseGesture {
            repeat(3) { click(centerEnd) }
        }

        asserter.applyAndAssert {
            selection = 6 to 23
        }
    }
}
