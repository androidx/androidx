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
import androidx.compose.foundation.text.selection.gestures.AbstractSelectionGesturesTest.HorizontalDirection.START
import androidx.compose.foundation.text.selection.gestures.AbstractSelectionGesturesTest.VerticalDirection.DOWN
import androidx.compose.foundation.text.selection.gestures.AbstractSelectionGesturesTest.VerticalDirection.UP
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
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
internal abstract class TextSelectionGesturesTest : AbstractSelectionGesturesTest() {

    override val pointerAreaTag = "selectionContainer"

    protected val selection = mutableStateOf<Selection?>(null)

    /**
     * Word to use in one-off tests. Subclasses may choose a RTL or BiDi 5 letter word for example.
     */
    protected abstract val word: String
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

    protected abstract fun characterPosition(offset: Int): Offset

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
        val content = ""
        textContent.value = content
        asserter.textContent = content
        rule.waitForIdle()

        performTouchGesture {
            longClick(center)
        }

        asserter.applyAndAssert {
            selection = 0.collapsed
        }
    }

    @Test
    fun whenTouch_withNoTextThenLongPressAndDrag_noSelection() {
        val content = ""
        textContent.value = content
        asserter.textContent = content
        rule.waitForIdle()

        performTouchGesture {
            longPress(center)
        }

        asserter.applyAndAssert {
            selection = 0.collapsed
        }

        touchDragTo(centerStart)

        asserter.assert()

        performTouchGesture {
            up()
        }

        asserter.assert()
    }

    @Test
    fun whenTouch_withLongPressInEndPaddingOfEmptyLine_entersSelectionMode() {
        val content = "$word\n\n$word"
        textContent.value = content
        asserter.textContent = content
        rule.waitForIdle()

        performTouchGesture {
            longPress(centerEnd)
        }

        asserter.applyAndAssert {
            selection = 6.collapsed
            hapticsCount++
        }

        // we want to test at least one drag that shouldn't affect selection as well
        touchDragBy(Offset(-1f, 0f))

        asserter.assert()

        performTouchGesture {
            up()
        }

        asserter.assert()
    }

    @Test
    fun whenTouch_withLongPressInEndPaddingOfEmptyFinalLine_entersSelectionMode() {
        val content = "$word\n\n"
        textContent.value = content
        asserter.textContent = content
        rule.waitForIdle()

        performTouchGesture {
            longPress(bottomEnd)
        }

        asserter.applyAndAssert {
            selection = 7.collapsed
            selectionHandlesShown = false
            hapticsCount++
        }

        // we want to test at least one drag that shouldn't affect selection as well
        touchDragBy(Offset.Zero.nudge(START))

        asserter.assert()

        touchDragTo(centerEnd)

        asserter.applyAndAssert {
            selection = 7 to 6
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(topEnd)

        asserter.applyAndAssert {
            selection = 7 to 0
            hapticsCount++
        }

        touchDragTo(topStart)

        asserter.applyAndAssert {
            magnifierShown = true
        }

        // take a stop in the middle of the word, otherwise we may not shrink selection at all,
        // and then word adjustment will be used once we move pointer to the end of the line.
        touchDragTo(characterPosition(4))

        asserter.applyAndAssert {
            selection = 7 to 4
            hapticsCount++
        }

        touchDragTo(topEnd)

        asserter.applyAndAssert {
            selection = 7 to 5
            magnifierShown = false // pointer is too far from text to show magnifier
            hapticsCount++
        }

        touchDragTo(centerEnd)

        asserter.applyAndAssert {
            selection = 7 to 6
            hapticsCount++
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
            selection = null
            selectionHandlesShown = false
            textToolbarShown = false
            hapticsCount++
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
        touchDragTo(topStart.nudge(START, UP))
        asserter.assert()

        // below bottom line, should be end of text completely
        touchDragTo(bottomStart.nudge(START, DOWN))
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
        touchLongPressThenDragForwardsAndBackTest(
            forwardOffset = characterPosition(8),
            forwardSelection = 12 to 6,
            backwardOffset = characterPosition(9),
            backwardSelection = 12 to 9,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragUpAndBack_selectsWordsThenChars() {
        touchLongPressThenDragForwardsAndBackTest(
            forwardOffset = characterPosition(0),
            forwardSelection = 12 to 0,
            backwardOffset = characterPosition(3),
            backwardSelection = 12 to 3,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragRightAndBack_selectsWordsThenChars() {
        touchLongPressThenDragForwardsAndBackTest(
            forwardOffset = characterPosition(22),
            forwardSelection = 12 to 23,
            backwardOffset = characterPosition(19),
            backwardSelection = 12 to 19,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragDownAndBack_selectsWordsThenChars() {
        // entering the bottom paragraph will select word since it is a forward selection,
        // as we continue animating towards the forward position,
        // it will be selecting by character because it will be backwards selecting at that point.
        touchLongPressThenDragForwardsAndBackTest(
            forwardOffset = characterPosition(27),
            forwardSelection = 12 to 27,
            backwardOffset = characterPosition(26),
            backwardSelection = 12 to 26,
        )
    }

    private fun touchLongPressThenDragForwardsAndBackTest(
        forwardOffset: Offset,
        forwardSelection: TextRange?,
        backwardOffset: Offset,
        backwardSelection: TextRange?,
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
            selection = 0 to 5
            selectionHandlesShown = true
            hapticsCount++
        }

        // we want to test at least one drag that shouldn't affect selection as well
        touchDragBy(Offset.Zero.nudge(START))

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
            selection = 18 to 23
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(topEnd)

        asserter.applyAndAssert {
            selection = 18 to 0
            hapticsCount++
        }

        // do it again for a regression where selection was only wrong the second time
        touchDragTo(centerEnd)

        asserter.applyAndAssert {
            selection = 18 to 23
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(topEnd)

        asserter.applyAndAssert {
            selection = 18 to 0
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
            selection = 18 to 23
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(bottomEnd)

        asserter.applyAndAssert {
            selection = 18 to 29
            hapticsCount++
        }

        // do it again for a regression where selection was only wrong the second time
        touchDragTo(centerEnd)

        asserter.applyAndAssert {
            selection = 18 to 23
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(bottomEnd)

        asserter.applyAndAssert {
            selection = 18 to 29
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
            selection = 24 to 29
            selectionHandlesShown = true
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

    // regression test for abnormal selection behavior when dragging
    // from bottom to middle end padding
    @Test
    fun whenTouch_withLongPressInFinalLineEndPaddingThenDragToMidEndPadding_entersSelectionMode() {
        performTouchGesture {
            longPress(bottomEnd)
        }

        asserter.applyAndAssert {
            selection = 24 to 29
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(centerEnd)

        asserter.applyAndAssert {
            selection = 24 to 18
            hapticsCount++
        }

        // do it again for a regression where selection was only wrong the second time
        touchDragTo(bottomEnd)

        asserter.applyAndAssert {
            selection = 24 to 29
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(centerEnd)

        asserter.applyAndAssert {
            selection = 24 to 18
            hapticsCount++
        }

        performTouchGesture {
            up()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
        }
    }

    // Regression test for a touch long press in end padding selecting the entire Text
    @Test
    fun whenTouch_withLongPressInEndPadding_selectsFinalWord() {
        performTouchGesture {
            longPress(centerEnd)
        }

        asserter.applyAndAssert {
            selection = 18 to 23
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
            endOffset = characterPosition(20),
            endSelection = 13 to 20,
        )
    }

    @Test
    fun whenMouse_withSingleClickThenDragDown_selectsCharacters() {
        mouseSingleClickThenDragTest(
            endOffset = characterPosition(26),
            endSelection = 13 to 26,
        )
    }

    private fun mouseSingleClickThenDragTest(
        endOffset: Offset,
        endSelection: TextRange?
    ) {
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
            endOffset = characterPosition(20),
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

    private fun mouseDoubleClickThenDragTest(
        endOffset: Offset,
        endSelection: TextRange?
    ) {
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
            endOffset = characterPosition(20),
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

    private fun mouseTripleClickThenDragTest(
        endOffset: Offset,
        endSelection: TextRange?
    ) {
        mouseClicksThenDragTest(
            numClicks = 3,
            startOffset = characterPosition(13),
            endOffset = endOffset,
            startSelection = 6 to 23,
            endSelection = endSelection,
        )
    }

    private fun mouseClicksThenDragTest(
        numClicks: Int,
        startOffset: Offset,
        endOffset: Offset,
        startSelection: TextRange?,
        endSelection: TextRange?
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
    fun whenMouse_thenSingleClickAndDragUpToEndPadding_selectsCharacters() {
        mouseClickThenDragUpToPaddingTest(
            numClicks = 1,
            endSelection = 13 to 5,
        )
    }

    @Test
    fun whenMouse_thenDoubleClickAndDragUpToEndPadding_selectsWords() {
        mouseClickThenDragUpToPaddingTest(
            numClicks = 2,
            endSelection = 17 to 0,
        )
    }

    @Test
    fun whenMouse_thenTripleClickAndDragUpToEndPadding_selectsParagraph() {
        mouseClickThenDragUpToPaddingTest(
            numClicks = 3,
            endSelection = 23 to 0,
        )
    }

    private fun mouseClickThenDragUpToPaddingTest(
        numClicks: Int,
        endSelection: TextRange?
    ) {
        performMouseGesture {
            moveTo(position = characterPosition(13))
            press()
            repeat(numClicks - 1) {
                advanceEventTime()
                release()
                advanceEventTime()
                press()
            }
        }

        mouseDragTo(position = topEnd)

        asserter.applyAndAssert {
            selection = endSelection
        }
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
    fun whenMouseCollapsedSelection_thenTouch_noUiElements() {
        performMouseGesture {
            click(characterPosition(13))
        }

        asserter.applyAndAssert {
            selection = 13.collapsed
        }

        performTouchGesture {
            enterTouchMode()
        }

        asserter.assert()
    }

    @Test
    fun whenTouchCollapsedSelection_thenMouse_noUiElements() {
        performTouchGesture {
            click(characterPosition(13))
        }

        asserter.assert()
        enterMouseMode()
        asserter.assert()
    }

    // this is a collapsed selection in multi-text and a selection of just a newline in single text.
    @Test
    open fun whenMouseCollapsedSelectionAcrossLines_thenTouch_showUi() {
        performMouseGesture {
            moveTo(centerEnd)
            press()
        }

        asserter.applyAndAssert {
            selection = 23.collapsed
        }

        mouseDragTo(characterPosition(offset = 24))

        asserter.applyAndAssert {
            selection = 23 to 24
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
    fun whenMouse_thenTripleClickInEndPadding_selectsOnlyCurrentParagraph() {
        performMouseGesture {
            moveTo(position = centerEnd)
            press()
            repeat(2) {
                advanceEventTime()
                release()
                advanceEventTime()
                press()
            }
            release()
        }

        asserter.applyAndAssert {
            selection = 6 to 23
        }
    }
}
