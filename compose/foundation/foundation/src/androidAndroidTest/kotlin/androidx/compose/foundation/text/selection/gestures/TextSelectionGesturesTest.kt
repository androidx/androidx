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

    protected val textContent = mutableStateOf("line1\nline2 text1 text2\nline3")
    protected val selection = mutableStateOf<Selection?>(null)

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
    fun whenTouch_withNoTextThenLongPress_noSelection() {
        val content = ""
        textContent.value = content
        asserter.textContent = content
        rule.waitForIdle()

        performTouchGesture {
            longClick(boundsInRoot.center)
        }

        asserter.applyAndAssert {
            selection = 0.collapsed
            hapticsCount++
        }
    }

    @Test
    fun whenTouch_withNoTextThenLongPressAndDrag_noSelection() {
        val content = ""
        textContent.value = content
        asserter.textContent = content
        rule.waitForIdle()

        performTouchGesture {
            longPress(boundsInRoot.center)
        }

        asserter.applyAndAssert {
            selection = 0.collapsed
            hapticsCount++
        }

        touchDragTo(boundsInRoot.centerLeft)

        asserter.assert()

        performTouchGesture {
            up()
        }

        asserter.assert()
    }

    @Test
    fun whenTouch_withLongPressInEndPaddingOfEmptyLine_entersSelectionMode() {
        val content = "Line1\n\nLine3"
        textContent.value = content
        asserter.textContent = content
        rule.waitForIdle()

        performTouchGesture {
            longPress(boundsInRoot.centerRight + Offset(-1f, 0f))
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
        val content = "Line1\n\n"
        textContent.value = content
        asserter.textContent = content
        rule.waitForIdle()

        performTouchGesture {
            longPress(boundsInRoot.bottomRight + Offset(-1f, -1f))
        }

        asserter.applyAndAssert {
            selection = 7.collapsed
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
            forwardOffset = characterPosition(1),
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
            endOffset = boundsInRoot.topRight + Offset(-1f, 1f),
            endSelection = 12 to 0,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragToMiddleEndPaddingAndBack_selectsWordsThenChars() {
        touchLongPressThenDragToEndPaddingTest(
            endOffset = boundsInRoot.centerRight + Offset(-1f, 0f),
            endSelection = 12 to 23,
        )
    }

    @Test
    fun whenTouch_withLongPressThenDragToLowerEndPaddingAndBack_selectsWordsThenChars() {
        touchLongPressThenDragToEndPaddingTest(
            endOffset = boundsInRoot.bottomRight + Offset(-1f, -1f),
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
            longPress(boundsInRoot.topRight + Offset(-1f, 1f))
        }

        asserter.applyAndAssert {
            selection = 0 to 5
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

    @Test
    fun whenTouch_withLongPressInEndPaddingThenDragToUpperEndPadding_selectsParagraphAndNewLine() {
        performTouchGesture {
            longPress(boundsInRoot.centerRight + Offset(-1f, 0f))
        }

        asserter.applyAndAssert {
            selection = 18 to 23
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(boundsInRoot.topRight + Offset(-1f, 1f))

        asserter.applyAndAssert {
            selection = 18 to 0
            hapticsCount++
        }

        // do it again for a regression where selection was only wrong the second time
        touchDragTo(boundsInRoot.centerRight + Offset(-1f, 0f))

        asserter.applyAndAssert {
            selection = 18 to 23
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(boundsInRoot.topRight + Offset(-1f, 1f))

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
            longPress(boundsInRoot.centerRight + Offset(-1f, 0f))
        }

        asserter.applyAndAssert {
            selection = 18 to 23
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(boundsInRoot.bottomRight + Offset(-1f, -1f))

        asserter.applyAndAssert {
            selection = 18 to 29
            hapticsCount++
        }

        // do it again for a regression where selection was only wrong the second time
        touchDragTo(boundsInRoot.centerRight + Offset(-1f, 0f))

        asserter.applyAndAssert {
            selection = 18 to 23
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(boundsInRoot.bottomRight + Offset(-1f, -1f))

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
            longPress(boundsInRoot.bottomRight + Offset(-1f, -1f))
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
            longPress(boundsInRoot.bottomRight + Offset(-1f, -1f))
        }

        asserter.applyAndAssert {
            selection = 24 to 29
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(boundsInRoot.centerRight + Offset(-1f, 0f))

        asserter.applyAndAssert {
            selection = 24 to 18
            hapticsCount++
        }

        // do it again for a regression where selection was only wrong the second time
        touchDragTo(boundsInRoot.bottomRight + Offset(-1f, -1f))

        asserter.applyAndAssert {
            selection = 24 to 29
            selectionHandlesShown = true
            hapticsCount++
        }

        touchDragTo(boundsInRoot.centerRight + Offset(-1f, 0f))

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
            longPress(boundsInRoot.centerRight - Offset(1f, 0f))
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

        mouseDragTo(position = boundsInRoot.topRight + Offset(-1f, 1f))

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

    @Test
    fun whenMouse_thenTripleClickInEndPadding_selectsOnlyCurrentParagraph() {
        performMouseGesture {
            moveTo(position = boundsInRoot.centerRight - Offset(1f, 0f))
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
