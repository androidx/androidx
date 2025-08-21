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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.contextmenu.test.ContextMenuFlagFlipperRunner
import androidx.compose.foundation.text.selection.HandlePressedScope
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.SelectionSubject
import androidx.compose.foundation.text.selection.gestures.util.TextSelectionAsserter
import androidx.compose.foundation.text.selection.gestures.util.applyAndAssert
import androidx.compose.foundation.text.selection.gestures.util.to
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlin.test.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(ContextMenuFlagFlipperRunner::class)
internal class TextSelectionHandlesGesturesTest : AbstractSelectionGesturesTest() {

    override val pointerAreaTag = "selectionContainer"

    private val textContent = mutableStateOf("line1\nline2 text1 text2\nline3")
    private val currentSelection = mutableStateOf<Selection?>(null)

    private lateinit var asserter: TextSelectionAsserter

    @Before
    fun setupAsserter() {
        asserter =
            object :
                TextSelectionAsserter(
                    textContent = textContent.value,
                    rule = rule,
                    textToolbar = textToolbar,
                    spyTextActionModeCallback = spyTextActionModeCallback,
                    hapticFeedback = hapticFeedback,
                    getActual = { currentSelection.value },
                ) {
                override fun subAssert() {
                    Truth.assertAbout(SelectionSubject.withContent(textContent))
                        .that(getActual())
                        .hasSelection(
                            expected = selection,
                            startTextDirection = startLayoutDirection,
                            endTextDirection = endLayoutDirection,
                        )
                }
            }
    }

    @Composable
    override fun Content() {
        SelectionContainer(
            selection = currentSelection.value,
            onSelectionChange = { currentSelection.value = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicText(
                text = textContent.value,
                style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                modifier = Modifier.fillMaxWidth().wrapContentHeight().testTag(pointerAreaTag),
            )
        }
    }

    // TODO(b/316940648)
    //  The TextToolbar at the top of the screen messes up the popup position calculations,
    //  so suppress SDKs that don't have the floating popup.
    @Test
    fun whenTouchHandle_verifyOneCharStaysSelected_withinLine() {
        performTouchGesture { longClick(characterPosition(14)) }

        asserter.applyAndAssert {
            selection = 12 to 17
            selectionHandlesShown = true
            textToolbarShown = true
            hapticsCount++
        }

        withHandlePressed(Handle.SelectionEnd) {
            moveHandleToCharacter(13)
            asserter.applyAndAssert {
                selection = 12 to 13
                magnifierShown = true
                textToolbarShown = false
                hapticsCount++
            }

            moveHandleToCharacter(12)
            // shouldn't allow collapsed selection, but keeps previous single char selection
            asserter.assert()

            moveHandleToCharacter(11)
            asserter.applyAndAssert {
                selection = 12 to 11
                hapticsCount++
            }
        }

        asserter.applyAndAssert {
            textToolbarShown = true
            magnifierShown = false
        }
    }

    @Test
    fun whenTouchHandle_magnifierReplacesToolbar() {
        performTouchGesture { longClick(characterBox(13).centerLeft) }

        asserter.applyAndAssert {
            selection = 12 to 17
            selectionHandlesShown = true
            textToolbarShown = true
            hapticsCount++
        }

        withHandlePressed(Handle.SelectionEnd) {
            asserter.applyAndAssert {
                magnifierShown = true
                textToolbarShown = false
            }
        }

        asserter.applyAndAssert {
            magnifierShown = false
            textToolbarShown = true
        }

        withHandlePressed(Handle.SelectionStart) {
            asserter.applyAndAssert {
                magnifierShown = true
                textToolbarShown = false
            }
        }

        asserter.applyAndAssert {
            magnifierShown = false
            textToolbarShown = true
        }
    }

    // TODO(b/316940648)
    //  The TextToolbar at the top of the screen messes up the popup position calculations,
    //  so suppress SDKs that don't have the floating popup.
    @Test
    fun whenTouchHandle_thenDragLeftOutOfBounds_keepsFirstCharSelected() {
        var finalX: Float? = null
        performTouchGesture {
            finalX = left - 2f
            longClick(characterPosition(9))
        }
        Truth.assertThat(finalX).isNotNull()

        asserter.applyAndAssert {
            selection = 6 to 11
            selectionHandlesShown = true
            textToolbarShown = true
            hapticsCount++
        }

        withHandlePressed(Handle.SelectionEnd) {
            moveHandleToCharacter(8)
            moveHandleToCharacter(7)
            moveHandleToCharacter(6)
            // simulate drag to select only the first character
            asserter.applyAndAssert {
                selection = 6 to 7
                textToolbarShown = false
                magnifierShown = true
                hapticsCount++
            }

            val y = fetchHandleInfo().position.y
            moveHandleTo(Offset(finalX!!, y))
            // drag just outside of the left bound, should be no change.
            // Regression: we want to ensure the selection doesn't travel to a line above the cursor
            asserter.assert()
        }

        asserter.applyAndAssert {
            textToolbarShown = true
            magnifierShown = false
        }
    }

    // Regression test for when a word spanning multiple lines
    // could not shrink selection within a line.
    // TODO(b/316940648)
    //  The TextToolbar at the top of the screen messes up the popup position calculations,
    //  so suppress SDKs that don't have the floating popup.
    @Test
    fun whenTouchHandle_withWordSpanningMultipleLines_selectionCanShrinkWithinLine() {
        val content = "hello".repeat(100)
        textContent.value = content
        rule.waitForIdle()
        asserter.applyAndAssert { textContent = content }

        performTouchGesture { longClick(characterPosition(content.lastIndex)) }

        asserter.applyAndAssert {
            selection = 0 to content.length
            selectionHandlesShown = true
            textToolbarShown = true
            hapticsCount++
        }

        withHandlePressed(Handle.SelectionEnd) {
            // two drags to ensure we get some movement on the same line
            moveHandleToCharacter(10)
            moveHandleToCharacter(5)
            asserter.applyAndAssert {
                selection = 0 to 5
                magnifierShown = true
                textToolbarShown = false
                hapticsCount++
            }
        }

        asserter.applyAndAssert {
            magnifierShown = false
            textToolbarShown = true
        }
    }

    private fun HandlePressedScope.moveHandleToCharacter(characterOffset: Int) {
        val destinationPosition =
            characterBox(characterOffset).run {
                when (fetchHandleInfo().handle) {
                    Handle.SelectionStart -> bottomLeft.nudge(HorizontalDirection.END)
                    Handle.SelectionEnd -> bottomLeft.nudge(HorizontalDirection.START)
                    Handle.Cursor -> fail("Unexpected handle ${Handle.Cursor}")
                }
            }
        moveHandleTo(destinationPosition)
    }

    private fun characterBox(offset: Int): Rect {
        val textLayoutResult = rule.onNodeWithTag(pointerAreaTag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(offset)
    }

    private fun characterPosition(offset: Int): Offset = characterBox(offset).centerLeft
}
