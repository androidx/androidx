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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionHandleInfoKey
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.SelectionAsserter
import androidx.compose.foundation.text.selection.gestures.util.SelectionSubject
import androidx.compose.foundation.text.selection.gestures.util.applyAndAssert
import androidx.compose.foundation.text.selection.gestures.util.to
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlin.math.sign
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class TextSelectionHandlesGesturesTest : AbstractSelectionGesturesTest() {

    override val pointerAreaTag = "selectionContainer"

    private val textContent = "line1\nline2 text1 text2\nline3"
    private val selection = mutableStateOf<Selection?>(null)

    private lateinit var asserter: SelectionAsserter<Selection?>

    @Before
    fun setupAsserter() {
        performTouchGesture {
            longClick(characterBox(13).centerLeft)
        }

        asserter = object : SelectionAsserter<Selection?>(
            textContent = textContent,
            rule = rule,
            textToolbar = textToolbar,
            hapticFeedback = hapticFeedback,
            getActual = { selection.value },
        ) {
            var selection: TextRange? = null
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
            selection = 12 to 17
            selectionHandlesShown = true
            textToolbarShown = true
            hapticsCount++
        }
    }

    @Composable
    override fun Content() {
        SelectionContainer(
            selection = selection.value,
            onSelectionChange = { selection.value = it },
            modifier = Modifier.fillMaxSize()
        ) {
            BasicText(
                text = textContent,
                style = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .testTag(pointerAreaTag),
            )
        }
    }

    @Test
    fun whenOnlySetup_middleWordIsSelected() {
        asserter.assert()
    }

    @Test
    fun whenTouchHandle_magnifierReplacesToolbar() {
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

    private fun withHandlePressed(
        handle: Handle,
        block: SemanticsNodeInteraction.() -> Unit
    ) = rule.onNode(isSelectionHandle(handle)).run {
        performTouchInput { down(center) }
        block()
        performTouchInput { up() }
    }

    private fun SemanticsNodeInteraction.moveHandleToCharacter(characterOffset: Int) {
        val selectionHandleInfo = fetchSemanticsNode().config[SelectionHandleInfoKey]
        val destinationPosition = characterBox(characterOffset).run {
            if (selectionHandleInfo.handle == Handle.SelectionStart) bottomLeft else bottomRight
        }
        val delta = destinationPosition - selectionHandleInfo.position

        var slop: Offset? = null
        performTouchInput {
            slop = Offset(
                x = viewConfiguration.touchSlop * delta.x.sign,
                y = viewConfiguration.touchSlop * delta.y.sign
            )
        }
        touchDragBy(delta + slop!!)
    }

    private fun characterBox(offset: Int): Rect {
        val textLayoutResult = rule.onNodeWithTag(pointerAreaTag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(offset)
    }
}
