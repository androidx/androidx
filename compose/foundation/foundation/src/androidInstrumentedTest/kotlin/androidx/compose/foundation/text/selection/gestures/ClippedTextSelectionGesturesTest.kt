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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.HandlePressedScope
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.SelectionSubject
import androidx.compose.foundation.text.selection.gestures.util.TextSelectionAsserter
import androidx.compose.foundation.text.selection.gestures.util.applyAndAssert
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.foundation.text.selection.gestures.util.to
import androidx.compose.foundation.text.selection.getSelectionHandleInfo
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlin.test.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class ClippedTextSelectionGesturesTest : AbstractSelectionGesturesTest() {
    override val pointerAreaTag = "selectionContainer"

    private val style = TextStyle(fontFamily = fontFamily, fontSize = fontSize)
    private val textTag = "textTag"
    private var text = "Text ${"Text".repeat(19)}"

    private lateinit var asserter: TextSelectionAsserter
    private val selection = mutableStateOf<Selection?>(null)

    private val maxLinesState = mutableStateOf(Int.MAX_VALUE)
    private val overflowState = mutableStateOf(TextOverflow.Clip)

    @Before
    fun setupAsserter() {
        asserter =
            object :
                    TextSelectionAsserter(
                        textContent = text,
                        rule = rule,
                        textToolbar = textToolbar,
                        hapticFeedback = hapticFeedback,
                        getActual = { selection.value },
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
                .apply {
                    startSelectionHandleShown = false
                    endSelectionHandleShown = false
                }
    }

    @Composable
    override fun Content() {
        SelectionContainer(
            selection = selection.value,
            onSelectionChange = { selection.value = it },
            modifier = Modifier.testTag(pointerAreaTag)
        ) {
            Box(Modifier.padding(32.dp), Alignment.Center) {
                BasicText(
                    text = text,
                    style = style,
                    maxLines = maxLinesState.value,
                    overflow = overflowState.value,
                    modifier = Modifier.width(100.dp).testTag(textTag)
                )
            }
        }
    }

    // Lower apis will return the upstream line when checking the line for offsets.
    // This inherently fixes the issue that is being tested, so we can suppress lower apis.
    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun whenDragBelowText_withOverflowClip_entireTextSelected() {
        dragBelowTextTest(overflow = TextOverflow.Clip)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun whenDragBelowText_withOverflowVisible_entireTextSelected() {
        dragBelowTextTest(overflow = TextOverflow.Visible)
    }

    /**
     * Regression test for:
     * * Crashing when selecting overflowed text generally.
     * * Crashing when dragging selection below an overflowing text.
     */
    private fun dragBelowTextTest(overflow: TextOverflow) {
        maxLinesState.value = 2
        overflowState.value = overflow
        rule.waitForIdle()
        asserter.assert()

        performTouchGesture { longPress(characterPosition(2)) }
        asserter.applyAndAssert {
            selection = 0 to 4
            startSelectionHandleShown = true
            endSelectionHandleShown = true
            magnifierShown = true
            hapticsCount++
        }

        touchDragTo(position = bottomStart)
        asserter.applyAndAssert {
            selection = 0 to text.length
            endSelectionHandleShown = false
            magnifierShown = false
            hapticsCount++
        }

        // drag back to ensure the gesture continues on
        touchDragTo(position = characterPosition(2))
        asserter.applyAndAssert {
            selection = 0 to 4
            endSelectionHandleShown = true
            magnifierShown = true
            hapticsCount++
        }

        performTouchGesture { up() }
        asserter.applyAndAssert {
            magnifierShown = false
            textToolbarShown = true
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun whenDragEndHandleOutOfBounds_selectionAndHandleUpdates() {
        maxLinesState.value = 2
        overflowState.value = TextOverflow.Clip
        rule.waitForIdle()
        asserter.assert()

        performTouchGesture { longPress(characterPosition(offset = 4)) }
        touchDragTo(position = bottomStart)
        asserter.applyAndAssert {
            selection = 0 to text.length
            startSelectionHandleShown = true
            hapticsCount++
        }

        touchDragTo(characterPosition(offset = 2))
        asserter.applyAndAssert {
            selection = 0 to 4
            magnifierShown = true
            endSelectionHandleShown = true
        }

        performTouchGesture { up() }
        asserter.applyAndAssert {
            magnifierShown = false
            textToolbarShown = true
        }

        withHandlePressed(Handle.SelectionEnd) {
            asserter.applyAndAssert {
                magnifierShown = true
                textToolbarShown = false
            }

            moveHandleTo(bottomEnd)
            asserter.applyAndAssert {
                selection = 0 to text.length
                magnifierShown = false
                endSelectionHandleShown = false
                hapticsCount++
            }

            moveHandleToCharacter(characterOffset = 2)
            asserter.applyAndAssert {
                selection = 0 to 4
                magnifierShown = true
                endSelectionHandleShown = true
                hapticsCount++
            }
        }

        asserter.applyAndAssert {
            magnifierShown = false
            textToolbarShown = true
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun whenDragStartHandle_withNoEndHandle_selectionAndHandleUpdates() {
        maxLinesState.value = 2
        overflowState.value = TextOverflow.Clip
        rule.waitForIdle()
        asserter.assert()

        performTouchGesture {
            longPress(characterPosition(offset = 2))
            moveTo(bottomStart)
            up()
        }

        asserter.applyAndAssert {
            selection = 0 to text.length
            startSelectionHandleShown = true
            textToolbarShown = true
            hapticsCount++
        }

        withHandlePressed(Handle.SelectionStart) {
            asserter.applyAndAssert {
                magnifierShown = true
                textToolbarShown = false
            }

            moveHandleToCharacter(characterOffset = 6)
            asserter.applyAndAssert {
                selection = 5 to text.length
                magnifierShown = true
                hapticsCount++
            }

            moveHandleTo(bottomEnd)
            asserter.applyAndAssert {
                selection = (text.length - 1) to text.length
                magnifierShown = false
                startSelectionHandleShown = false
                hapticsCount++
            }

            moveHandleToCharacter(characterOffset = 6)
            asserter.applyAndAssert {
                selection = 5 to text.length
                magnifierShown = true
                startSelectionHandleShown = true
                hapticsCount++
            }
        }

        asserter.applyAndAssert {
            magnifierShown = false
            textToolbarShown = true
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun whenDragInvisibleEndHandle_noSelectionChanges() {
        maxLinesState.value = 2
        overflowState.value = TextOverflow.Clip
        rule.waitForIdle()
        asserter.assert()

        performTouchGesture {
            longPress(characterPosition(offset = 2))
            moveTo(characterPosition(offset = 6))
        }

        asserter.applyAndAssert {
            selection = 0 to text.length
            startSelectionHandleShown = true
            hapticsCount++
        }

        val offsetTwoPosition = characterPosition(offset = 2)
        touchDragTo(offsetTwoPosition)
        asserter.applyAndAssert {
            selection = 0 to 4
            magnifierShown = true
            endSelectionHandleShown = true
        }

        // last position where the handle is shown
        val initialPosition =
            rule
                .onNode(isSelectionHandle(Handle.SelectionEnd))
                .fetchSemanticsNode()
                .getSelectionHandleInfo()
                .position

        // drag straight down, out of text bounds
        touchDragTo(offsetTwoPosition.copy(y = bottomEnd.y))
        asserter.applyAndAssert {
            selection = 0 to text.length
            magnifierShown = false
            endSelectionHandleShown = false
        }

        performTouchGesture { up() }
        asserter.applyAndAssert { textToolbarShown = true }

        withHandlePressed(Handle.SelectionEnd) {
            setInitialGesturePosition(initialPosition)
            asserter.assert()
            moveHandleToCharacter(4)
            asserter.assert()
            moveHandleToCharacter(2)
            asserter.assert()
        }
        asserter.assert()
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

    private fun characterPosition(offset: Int): Offset =
        characterBox(offset).centerLeft.nudge(HorizontalDirection.END)

    private fun characterBox(offset: Int): Rect {
        val pointerAreaPosition =
            rule.onNodeWithTag(pointerAreaTag).fetchSemanticsNode().positionInRoot
        val textPosition = rule.onNodeWithTag(textTag).fetchSemanticsNode().positionInRoot
        val textLayoutResult = rule.onNodeWithTag(textTag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(offset).translate(textPosition - pointerAreaPosition)
    }
}
