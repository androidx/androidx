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
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionHandleInfoKey
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.SelectionSubject
import androidx.compose.foundation.text.selection.gestures.util.TextSelectionAsserter
import androidx.compose.foundation.text.selection.gestures.util.applyAndAssert
import androidx.compose.foundation.text.selection.gestures.util.longPress
import androidx.compose.foundation.text.selection.gestures.util.to
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlin.math.sign
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
    private val text = "Text".repeat(20)

    private lateinit var asserter: TextSelectionAsserter
    private val selection = mutableStateOf<Selection?>(null)

    private val maxLinesState = mutableStateOf(Int.MAX_VALUE)
    private val overflowState = mutableStateOf(TextOverflow.Clip)

    @Before
    fun setupAsserter() {
        asserter = object : TextSelectionAsserter(
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
        }.apply {
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
                    modifier = Modifier
                        .width(100.dp)
                        .testTag(textTag)
                )
            }
        }
    }

    // Lower apis will return the upstream line when checking the line for offsets.
    // This inherently fixes the issue that is being tested, so we can suppress lower apis.
    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun whenDragBelowText_withMaxLines1AndOverflowClip_entireTextSelected() {
        dragBelowTextTest(maxLines = 1, overflow = TextOverflow.Clip)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun whenDragBelowText_withMaxLines1AndOverflowVisible_entireTextSelected() {
        dragBelowTextTest(maxLines = 1, overflow = TextOverflow.Visible)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun whenDragBelowText_withMaxLines2AndOverflowClip_entireTextSelected() {
        dragBelowTextTest(maxLines = 2, overflow = TextOverflow.Clip)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun whenDragBelowText_withMaxLines2AndOverflowVisible_entireTextSelected() {
        dragBelowTextTest(maxLines = 2, overflow = TextOverflow.Visible)
    }

    /**
     * Regression test for:
     * * Crashing when selecting overflowed text generally.
     * * Crashing when dragging selection below an overflowing text.
     */
    private fun dragBelowTextTest(maxLines: Int, overflow: TextOverflow) {
        maxLinesState.value = maxLines
        overflowState.value = overflow
        rule.waitForIdle()
        asserter.assert()

        performTouchGesture { longPress(characterPosition(4)) }
        asserter.applyAndAssert {
            selection = 0 to text.length
            startSelectionHandleShown = true
            hapticsCount++
        }

        touchDragTo(position = characterPosition(2))
        asserter.applyAndAssert {
            selection = 0 to 2
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

        performTouchGesture { up() }
        asserter.applyAndAssert {
            textToolbarShown = true
        }
    }

    // Test is irrelevant if the end handle is not hidden, which is the case below api 26.
    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun whenDragStartHandle_withNoEndHandle_selectionAndHandleUpdates() {
        maxLinesState.value = 1
        overflowState.value = TextOverflow.Clip
        rule.waitForIdle()
        asserter.assert()

        performTouchGesture { longClick(characterPosition(4)) }
        asserter.applyAndAssert {
            selection = 0 to text.length
            startSelectionHandleShown = true
            textToolbarShown = true
            hapticsCount++
        }

        withHandlePressed(Handle.SelectionStart) {
            moveHandleToCharacter(characterOffset = 2)
        }
        asserter.applyAndAssert {
            selection = 2 to text.length
            hapticsCount++
        }
    }

    private fun withHandlePressed(
        handle: Handle,
        block: SemanticsNodeInteraction.() -> Unit
    ): SemanticsNodeInteraction = rule.onNode(isSelectionHandle(handle)).run {
        performTouchInput { down(center) }
        block()
        performTouchInput { up() }
    }

    private fun SemanticsNodeInteraction.moveHandleToCharacter(characterOffset: Int) {
        val selectionHandleInfo = fetchSemanticsNode().config[SelectionHandleInfoKey]
        val destinationPosition = characterBox(characterOffset).run {
            when (selectionHandleInfo.handle) {
                Handle.SelectionStart -> bottomLeft
                Handle.SelectionEnd -> bottomRight
                Handle.Cursor -> fail("Unexpected handle ${Handle.Cursor}")
            }
        }
        val delta = destinationPosition - selectionHandleInfo.position
        performTouchInput { movePastSlopBy(delta) }
    }

    private fun TouchInjectionScope.movePastSlopBy(delta: Offset) {
        val slop = Offset(
            x = viewConfiguration.touchSlop * delta.x.sign,
            y = viewConfiguration.touchSlop * delta.y.sign
        )
        moveBy(delta + slop)
    }

    private fun characterPosition(offset: Int): Offset =
        characterBox(offset)
            .centerLeft
            .nudge(HorizontalDirection.END)

    private fun characterBox(offset: Int): Rect {
        val pointerAreaPosition =
            rule.onNodeWithTag(pointerAreaTag).fetchSemanticsNode().positionInRoot
        val textPosition = rule.onNodeWithTag(textTag).fetchSemanticsNode().positionInRoot
        val textLayoutResult = rule.onNodeWithTag(textTag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(offset)
            .translate(textPosition - pointerAreaPosition)
    }
}
