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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.gestures.util.MultiSelectionSubject
import androidx.compose.foundation.text.selection.gestures.util.TextSelectionAsserter
import androidx.compose.foundation.text.selection.gestures.util.applyAndAssert
import androidx.compose.foundation.text.selection.gestures.util.offsetToLocalOffset
import androidx.compose.foundation.text.selection.gestures.util.offsetToSelectableId
import androidx.compose.foundation.text.selection.gestures.util.textContentIndices
import androidx.compose.foundation.text.selection.gestures.util.to
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.util.fastForEach
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
internal class MultiTextWithSpaceSelectionGesturesRegressionTest : AbstractSelectionGesturesTest() {
    private val textContent = "line1\nline2 text1 text2!\nline3"

    private val texts = textContent
        .split("\n")
        .withIndex()
        .map { (index, str) -> str to "testTag$index" }

    private val textContentIndices = texts.textContentIndices()

    override val pointerAreaTag = "selectionContainer"

    private val selection = mutableStateOf<Selection?>(null)

    private lateinit var asserter: TextSelectionAsserter

    @Composable
    override fun Content() {
        SelectionContainer(
            selection = selection.value,
            onSelectionChange = { selection.value = it },
            modifier = Modifier.testTag(pointerAreaTag)
        ) {
            Column {
                texts.fastForEach { (str, tag) ->
                    BasicText(
                        text = str,
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = fontSize,
                        ),
                        modifier = Modifier.testTag(tag),
                    )
                }
            }
        }
    }

    @Before
    fun setupAsserter() {
        asserter = object : TextSelectionAsserter(
            textContent = textContent,
            rule = rule,
            textToolbar = textToolbar,
            hapticFeedback = hapticFeedback,
            getActual = { selection.value }
        ) {
            override fun subAssert() {
                Truth.assertAbout(MultiSelectionSubject.withContent(texts))
                    .that(getActual())
                    .hasSelection(selection)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun characterPosition(offset: Int): Offset {
        val selectableIndex = textContentIndices.offsetToSelectableId(offset)
        val localOffset = textContentIndices.offsetToLocalOffset(offset)
        val (_, tag) = texts[selectableIndex]
        val nodePosition = rule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot
        val textLayoutResult = rule.onNodeWithTag(tag).fetchTextLayoutResult()
        return textLayoutResult.getBoundingBox(localOffset).translate(nodePosition).center
    }

    // There were cases where moving the cursor outside the bounds of any text would
    // result in the selection being cleared. This test should catch any regression.
    // It was fixed by changing SelectionMode bounds check to be inclusive.
    @Suppress("SameParameterValue")
    @Test
    fun whenMouse_withDoubleClickThenDragUpAndDown_selectsWords() {
        performMouseGesture {
            moveTo(position = characterPosition(18))
            press()
            advanceEventTime()
            release()
            advanceEventTime()
            press()
        }

        asserter.applyAndAssert {
            selection = 18 to 23
        }

        mouseDragTo(position = boundsInRoot.topRight + Offset(-1f, 1f))

        asserter.applyAndAssert {
            selection = 24 to 6
        }

        mouseDragTo(position = boundsInRoot.bottomRight + Offset(-1f, -1f))

        asserter.applyAndAssert {
            selection = 18 to 30
        }

        performMouseGesture {
            release()
        }

        asserter.assert()
    }
}