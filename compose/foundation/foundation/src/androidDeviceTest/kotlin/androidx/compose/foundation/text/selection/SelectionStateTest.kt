/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.Handle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.withStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Ignore
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class SelectionStateTest : AbstractSelectionContainerTest() {

    @Test
    fun setSelection_persistsAcrossConfigurationChange() {
        val restorationTester = StateRestorationTester(rule)
        var state: SelectionState? = null

        restorationTester.setContent {
            state = rememberSelectionState() // Initialize inside composition
            SelectionContainer(state = state) { TestText(textContent) }
        }

        rule.runOnIdle { state!!.select(TextRange(0, 4)) }

        rule.runOnIdle {
            assertAnchorInfo(state!!.selection?.start, offset = 0, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = 4, selectableId = 1)
            assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(0, 4))
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertAnchorInfo(state!!.selection?.start, offset = 0, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = 4, selectableId = 1)
            assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(0, 4))
        }
    }

    @Test
    fun selectedTexts_nullSelection() {
        val state = SelectionState()

        createSelectionContainerWithState(state) { BasicText(text = "Hello World") }

        rule.runOnIdle { assertThat(state.selectedTexts).isEmpty() }
    }

    @Test
    fun selectedTexts() {
        val state = SelectionState()
        val expectedSpanStyle = SpanStyle(color = Color.Red)
        val customText = buildAnnotatedString {
            withStyle(expectedSpanStyle) { append("Demo") }
            append(" Text")
        }

        with(rule.density) {
            createSelectionContainerWithState(state = state) {
                BasicText(
                    text = customText,
                    style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
                )
            }

            val characterSize = fontSize.toPx()

            rule.onSelectionContainer().performTouchInput {
                longClick(
                    Offset(customText.text.indexOf('m') * characterSize, 0.5f * characterSize)
                )
            }
        }

        rule.runOnIdle {
            val selectedTexts = state.selectedTexts
            assertThat(selectedTexts).isNotEmpty()

            val actualAnnotatedString = selectedTexts.first()
            assertThat(actualAnnotatedString).isNotNull()

            assertThat(actualAnnotatedString!!.text).isEqualTo("Demo")
            assertThat(actualAnnotatedString.spanStyles).isNotEmpty()

            val extractedSpan = actualAnnotatedString.spanStyles.first()
            assertThat(extractedSpan.item).isEqualTo(expectedSpanStyle)
            assertThat(extractedSpan.start).isEqualTo(0)
            assertThat(extractedSpan.end).isEqualTo(4)
        }
    }

    @Test
    fun selectedTexts_partialWord() {
        val state = SelectionState()
        val expectedSpanStyle = SpanStyle(color = Color.Red)
        val customText = buildAnnotatedString {
            withStyle(expectedSpanStyle) { append("Demo") }
            append(" Text")
        }

        createSelectionContainerWithState(state = state) {
            BasicText(
                text = customText,
                style = TextStyle(fontFamily = fontFamily, fontSize = fontSize),
            )
        }

        rule.runOnIdle { state.selectAll() }

        rule.runOnIdle {
            val selectableId = state.selection!!.start.selectableId

            // Select from index 2 ('m') to index 6 ('e', exclusive) -> "mo T"
            state.manager?.setSelection(
                Selection(
                    start = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 2, selectableId),
                    end = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 6, selectableId),
                    handlesCrossed = false,
                )
            )
        }

        rule.runOnIdle {
            val selectedTexts = state.selectedTexts
            assertThat(selectedTexts).isNotEmpty()

            val actualAnnotatedString = selectedTexts.first()
            assertThat(actualAnnotatedString).isNotNull()

            // Verify the extracted text is exactly "mo T"
            assertThat(actualAnnotatedString.text).isEqualTo("mo T")

            assertThat(actualAnnotatedString.spanStyles).isNotEmpty()

            val extractedSpan = actualAnnotatedString.spanStyles.first()
            assertThat(extractedSpan.item).isEqualTo(expectedSpanStyle)
            assertThat(extractedSpan.start).isEqualTo(0)
            assertThat(extractedSpan.end).isEqualTo(2)
        }
    }

    @Test
    fun selectAll() {
        val state = SelectionState()

        createSelectionContainerWithState(state = state) {
            BasicText(text = textContent)
            BasicText(text = "text2")
        }

        rule.runOnIdle { state.selectAll() }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(state.selectedTexts).hasSize(2)
            assertThat(state.selectedTexts.first().text).isEqualTo(textContent)
            assertThat(state.selectedTexts[1].text).isEqualTo("text2")
        }
    }

    @Test
    fun selectAll_afterGesture() {
        val state = SelectionState()

        with(rule.density) {
            createSelectionContainerWithState(state) {
                Column {
                    TestText(textContent)
                    TestText(text = "text2")
                }
            }
            val characterSize = fontSize.toPx()

            // Long Press "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performTouchInput {
                longClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }

            rule.runOnIdle {
                assertAnchorInfo(state.selection?.start, offset = 5, selectableId = 1)
                assertAnchorInfo(state.selection?.end, offset = 9, selectableId = 1)
            }

            rule.runOnIdle { state.selectAll() }

            rule.runOnIdle {
                assertThat(state.selectedTexts).hasSize(2)
                assertThat(state.selectedTexts.first().text).isEqualTo(textContent)
                assertThat(state.selectedTexts[1].text).isEqualTo("text2")
            }
        }
    }

    @Ignore("b/513036248")
    @Test
    fun selectAll_thenGesture() {
        val state = SelectionState()
        with(rule.density) {
            createSelectionContainerWithState(state) { TestText(textContent) }

            val characterSize = fontSize.toPx()

            rule.runOnIdle { state.selectAll() }

            rule.mainClock.advanceTimeByFrame()

            // Drag the start handle (at offset 0) to offset 5 (start of "Demo")
            rule
                .onNode(isSelectionHandle(Handle.SelectionStart), useUnmergedTree = true)
                .performTouchInput {
                    down(center)
                    val deltaX = 5 * characterSize
                    moveBy(Offset(deltaX, 0f))
                    up()
                }

            rule.runOnIdle {
                // Selection should now be from 5 to end (14)
                assertAnchorInfo(state.selection?.start, offset = 5, selectableId = 1)
                assertAnchorInfo(state.selection?.end, offset = 14, selectableId = 1)
            }
        }
    }

    @Test
    fun selectAll_showsHandlesAndToolbar() {
        val state = SelectionState()

        createSelectionContainerWithState(state) { TestText(textContent) }

        rule.runOnIdle { state.selectAll() }

        rule.mainClock.advanceTimeByFrame()

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()

        rule.runOnIdle { assertThat(state.manager?.showToolbar).isTrue() }
    }

    @Test
    fun clear() {
        val state = SelectionState()

        createSelectionContainerWithState(state = state) { TestText(textContent) }

        rule.runOnIdle { state.selectAll() }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle { assertThat(state.selectedTexts).isNotEmpty() }

        rule.runOnIdle { state.clear() }
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(state.selectedTexts).isEmpty()
            assertThat(state.manager?.selection).isNull()
        }
    }

    @Test
    fun clear_clearsHandlesAndToolbar() {
        val state = SelectionState()
        createSelectionContainerWithState(state) { TestText(textContent) }

        rule.runOnIdle { state.selectAll() }

        rule.mainClock.advanceTimeByFrame()

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertExists()

        rule.runOnIdle { assertThat(state.manager?.showToolbar).isTrue() }

        rule.runOnIdle { state.clear() }

        rule.mainClock.advanceTimeByFrame()

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()

        rule.runOnIdle { assertThat(state.manager?.showToolbar).isFalse() }
    }

    @Test
    fun extendSelectionByWord_Ltr() {
        val state = SelectionState()

        createSelectionContainerWithState(state = state) { TestText(text = textContent) }

        rule.runOnIdle { assertThat(state.selectedTexts).isEmpty() }

        rule.runOnIdle { state.extendSelectionByWord() }
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            val selection = state.manager?.selection
            assertThat(selection).isNotNull()

            assertAnchorInfo(selection?.start, offset = 0, selectableId = 1)
            assertAnchorInfo(selection?.end, offset = 4, selectableId = 1)

            assertThat(state.selectedTexts).isNotEmpty()
            assertEquals(state.selectedTexts.first().text, textContent.substring(0, 4))
        }

        rule.runOnIdle { state.extendSelectionByWord() }
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            val selection = state.manager?.selection
            assertThat(selection).isNotNull()

            assertAnchorInfo(selection?.start, offset = 0, selectableId = 1)
            assertAnchorInfo(selection?.end, offset = 9, selectableId = 1)

            assertThat(state.selectedTexts).isNotEmpty()
            assertEquals(state.selectedTexts.first().text, textContent.substring(0, 9))
        }
    }

    @Test
    fun extendSelectionByWord_afterGesture_Ltr() {
        val state = SelectionState()

        with(rule.density) {
            createSelectionContainerWithState(state) { TestText(textContent) }
            val characterSize = fontSize.toPx()

            // Long Press "m" in "Demo", and "Demo" should be selected.
            rule.onSelectionContainer().performTouchInput {
                longClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }

            rule.runOnIdle {
                assertAnchorInfo(state.selection?.start, offset = 5, selectableId = 1)
                assertAnchorInfo(state.selection?.end, offset = 9, selectableId = 1)
            }

            rule.runOnIdle { state.extendSelectionByWord() }

            rule.runOnIdle {
                assertAnchorInfo(state.selection?.start, offset = 5, selectableId = 1)
                assertAnchorInfo(state.selection?.end, offset = 14, selectableId = 1)
                assert(state.selectedTexts.isNotEmpty())
                assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(5, 14))
            }
        }
    }

    @Test
    fun extendSelectionByWord_afterGesture_handlesCrossed_Ltr() {
        val state = SelectionState()

        with(rule.density) {
            createSelectionContainerWithState(state) { TestText(textContent) }
            val characterSize = fontSize.toPx()

            // Long Press "m" to select "Demo".
            rule.onSelectionContainer().performTouchInput {
                longClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }

            rule.runOnIdle {
                assertAnchorInfo(state.selection?.start, offset = 5, selectableId = 1)
                assertAnchorInfo(state.selection?.end, offset = 9, selectableId = 1)
            }

            // Drag the end handle (at offset 9) to the left of the start handle (at offset 5).
            // We move it to offset 4 (the space before "Demo").
            rule.onNode(isSelectionHandle(Handle.SelectionEnd)).performTouchInput {
                down(center)
                val deltaX = (4 - 9) * characterSize
                moveBy(Offset(deltaX, 0f))
                up()
            }

            rule.runOnIdle { state.extendSelectionByWord() }

            rule.runOnIdle {
                // Since handles were crossed and the active handle was dragged to the left (to
                // offset 4),
                // extending by word should extend to the left, capturing "Text" (offsets 0 to 4).
                assertAnchorInfo(state.selection?.start, offset = 5, selectableId = 1)
                assertAnchorInfo(state.selection?.end, offset = 0, selectableId = 1)

                assert(state.selectedTexts.isNotEmpty())
                // The selected text should be "Text " (substring from 0 to 5).
                assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(0, 5))
            }
        }
    }

    @Test
    fun extendSelectionByWord_partialWord_Ltr() {
        val state = SelectionState()

        createSelectionContainerWithState(state) { TestText(textContent) }

        rule.runOnIdle {
            // Select from index 2 ('x') to index 6 ('D', exclusive) -> "xt D"
            state.manager?.setSelection(
                Selection(
                    start = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 2, selectableId = 1),
                    end = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 6, selectableId = 1),
                    handlesCrossed = false,
                )
            )
        }

        rule.runOnIdle {
            assertAnchorInfo(state.selection?.start, offset = 2, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = 6, selectableId = 1)
        }

        rule.runOnIdle { state.extendSelectionByWord() }

        rule.runOnIdle {
            assertAnchorInfo(state.selection?.start, offset = 2, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = 9, selectableId = 1)
            assert(state.selectedTexts.isNotEmpty())
            assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(2, 9))
        }
    }

    @Test
    fun extendSelectionByWord_multipleTexts_Ltr() {
        val state = SelectionState()

        createSelectionContainerWithState(state) {
            Column {
                TestText(textContent)
                TestText("text2")
            }
        }

        rule.runOnIdle {
            // Select "Text" from first TestText
            state.manager?.setSelection(
                Selection(
                    start = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 10, selectableId = 1),
                    end = Selection.AnchorInfo(ResolvedTextDirection.Ltr, 14, selectableId = 1),
                    handlesCrossed = false,
                )
            )
        }

        rule.runOnIdle {
            assertAnchorInfo(state.selection?.start, offset = 10, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = 14, selectableId = 1)
        }

        rule.runOnIdle { state.extendSelectionByWord() }

        rule.runOnIdle {
            assertAnchorInfo(state.selection?.start, offset = 10, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = 5, selectableId = 2)
            assertThat(state.selectedTexts).hasSize(2)
            assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(10, 14))
            assertThat(state.selectedTexts[1].text).isEqualTo("text2")
        }
    }

    @Test
    fun extendSelectionByWord_Rtl() {
        val state = SelectionState()
        val rtlText = "الأول الثاني الثالث الرابع"
        createSelectionContainerWithState(state = state, isRtl = true) { TestText(text = rtlText) }

        rule.runOnIdle { assertThat(state.selectedTexts).isEmpty() }

        rule.runOnIdle { state.extendSelectionByWord() }
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            val selection = state.manager?.selection
            assertThat(selection).isNotNull()

            assertAnchorInfo(
                selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 0,
                selectableId = 1,
            )
            assertAnchorInfo(
                selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 5,
                selectableId = 1,
            )

            assertThat(state.selectedTexts).isNotEmpty()
            assertEquals(rtlText.substring(0, 5), state.selectedTexts.first().text)
        }

        rule.runOnIdle { state.extendSelectionByWord() }
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            val selection = state.manager?.selection
            assertThat(selection).isNotNull()

            assertAnchorInfo(
                selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 0,
                selectableId = 1,
            )
            assertAnchorInfo(
                selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 12,
                selectableId = 1,
            )

            assertThat(state.selectedTexts).isNotEmpty()
            assertEquals(rtlText.substring(0, 12), state.selectedTexts.first().text)
        }
    }

    @Test
    fun extendSelectionByWord_afterGesture_Rtl() {
        val state = SelectionState()
        val rtlText = "الأول الثاني الثالث الرابع"

        with(rule.density) {
            createSelectionContainerWithState(state = state, isRtl = true) { TestText(rtlText) }
            val characterSize = fontSize.toPx()

            // "الثاني" starts at index 6. Let's long-press the 'ث' character (index 8).
            // Note: For RTL, touch input coordinate calculation may vary depending on layout,
            // but physically/visually targeting the 8th character offset from the right boundary.
            val targetIndex = 8
            rule.onSelectionContainer().performTouchInput {
                longClick(Offset(targetIndex * characterSize, 0.5f * characterSize))
            }

            // Long press should select the whole word "الثاني" (indices 6 to 12)
            rule.runOnIdle {
                assertAnchorInfo(
                    state.selection?.start,
                    resolvedTextDirection = ResolvedTextDirection.Rtl,
                    offset = 6,
                    selectableId = 1,
                )
                assertAnchorInfo(
                    state.selection?.end,
                    resolvedTextDirection = ResolvedTextDirection.Rtl,
                    offset = 12,
                    selectableId = 1,
                )
            }

            rule.runOnIdle { state.extendSelectionByWord() }

            // Extending should select to the end of the next word "الثالث" (ends at index 19)
            rule.runOnIdle {
                assertAnchorInfo(
                    state.selection?.start,
                    resolvedTextDirection = ResolvedTextDirection.Rtl,
                    offset = 6,
                    selectableId = 1,
                )
                assertAnchorInfo(
                    state.selection?.end,
                    resolvedTextDirection = ResolvedTextDirection.Rtl,
                    offset = 19,
                    selectableId = 1,
                )
                assert(state.selectedTexts.isNotEmpty())
                assertThat(state.selectedTexts.first().text).isEqualTo(rtlText.substring(6, 19))
            }
        }
    }

    @Test
    fun extendSelectionByWord_partialWord_Rtl() {
        val state = SelectionState()
        val rtlText = "الأول الثاني الثالث الرابع"

        createSelectionContainerWithState(state = state, isRtl = true) { TestText(rtlText) }

        rule.runOnIdle {
            // "الأول" is indices 0-5. "الثاني" is indices 6-12.
            // Select from index 2 ('و') to index 8 ('ث', exclusive) -> "ول الث"
            state.manager?.setSelection(
                Selection(
                    start = Selection.AnchorInfo(ResolvedTextDirection.Rtl, 2, selectableId = 1),
                    end = Selection.AnchorInfo(ResolvedTextDirection.Rtl, 8, selectableId = 1),
                    handlesCrossed = false,
                )
            )
        }

        rule.runOnIdle {
            assertAnchorInfo(
                state.selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 2,
                selectableId = 1,
            )
            assertAnchorInfo(
                state.selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 8,
                selectableId = 1,
            )
        }

        rule.runOnIdle { state.extendSelectionByWord() }

        // Extending should snap the end anchor to the end of the active word "الثاني" (index 12)
        rule.runOnIdle {
            assertAnchorInfo(
                state.selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 2,
                selectableId = 1,
            )
            assertAnchorInfo(
                state.selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 12,
                selectableId = 1,
            )
            assert(state.selectedTexts.isNotEmpty())
            assertThat(state.selectedTexts.first().text).isEqualTo(rtlText.substring(2, 12))
        }
    }

    @Test
    fun extendSelectionByWord_multipleTexts_Rtl() {
        val state = SelectionState()
        val rtlText1 = "الأول الثاني"
        val rtlText2 = "الثالث"

        createSelectionContainerWithState(state = state, isRtl = true) {
            Column {
                TestText(rtlText1)
                TestText(rtlText2)
            }
        }

        rule.runOnIdle {
            // Manually select the second word "الثاني" (indices 6 to 12) from the first TestText
            // (id 1)
            state.manager?.setSelection(
                Selection(
                    start = Selection.AnchorInfo(ResolvedTextDirection.Rtl, 6, selectableId = 1),
                    end = Selection.AnchorInfo(ResolvedTextDirection.Rtl, 12, selectableId = 1),
                    handlesCrossed = false,
                )
            )
        }

        rule.runOnIdle {
            assertAnchorInfo(
                state.selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 6,
                selectableId = 1,
            )
            assertAnchorInfo(
                state.selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 12,
                selectableId = 1,
            )
        }

        rule.runOnIdle { state.extendSelectionByWord() }

        rule.runOnIdle {
            // Selection start stays at index 6 in the first text element (id 1)
            assertAnchorInfo(
                state.selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 6,
                selectableId = 1,
            )
            // Selection end extends to the very end (index 6) of the second text element (id 2)
            assertAnchorInfo(
                state.selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 6,
                selectableId = 2,
            )

            assertThat(state.selectedTexts).hasSize(2)
            assertThat(state.selectedTexts.first().text).isEqualTo(rtlText1.substring(6, 12))
            assertThat(state.selectedTexts[1].text).isEqualTo(rtlText2)
        }
    }

    @Test
    fun extendSelectionByWord_thenGesture() {
        val state = SelectionState()
        with(rule.density) {
            createSelectionContainerWithState(state) { TestText(textContent) }

            val characterSize = fontSize.toPx()

            rule.runOnIdle {
                state.extendSelectionByWord() // Selects first word "Text" (0-4)
            }

            rule.mainClock.advanceTimeByFrame()

            // Drag the end handle (at offset 4) to offset 9 (end of "Demo")
            rule
                .onNode(isSelectionHandle(Handle.SelectionEnd), useUnmergedTree = true)
                .performTouchInput {
                    down(center)
                    val deltaX = (9 - 4) * characterSize
                    moveBy(Offset(deltaX, 0f))
                    up()
                }

            rule.runOnIdle {
                assertAnchorInfo(state.selection?.start, offset = 0, selectableId = 1)
                assertAnchorInfo(state.selection?.end, offset = 9, selectableId = 1)
            }
        }
    }

    @Test
    fun extendSelectionByWord_showsHandlesAndToolbar() {
        val state = SelectionState()

        createSelectionContainerWithState(state) { TestText(textContent) }

        rule.runOnIdle { state.extendSelectionByWord() }

        rule.mainClock.advanceTimeByFrame()

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()

        rule.runOnIdle { assertThat(state.manager?.showToolbar).isTrue() }
    }

    @Test
    fun extendSelectionByWord_whenAlreadyFullySelected() {
        val state = SelectionState()

        createSelectionContainerWithState(state = state) { TestText(textContent) }

        rule.runOnIdle { state.selectAll() }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertAnchorInfo(state.selection?.start, offset = 0, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = textContent.length, selectableId = 1)

            state.extendSelectionByWord()
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertAnchorInfo(state.selection?.start, offset = 0, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = textContent.length, selectableId = 1)

            assertThat(state.selectedTexts.first().text).isEqualTo(textContent)
        }
    }

    @Test
    fun select_Ltr() {
        val state = SelectionState()

        createSelectionContainerWithState(state) { TestText(textContent) }

        rule.runOnIdle {
            // Select "Text" (offsets 0 to 4)
            state.select(TextRange(0, 4))
        }

        rule.runOnIdle {
            assertAnchorInfo(state.selection?.start, offset = 0, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = 4, selectableId = 1)

            assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(0, 4))
        }
    }

    @Test
    fun select_handlesCrossed_Ltr() {
        val state = SelectionState()

        createSelectionContainerWithState(state) { TestText(textContent) }

        rule.runOnIdle {
            // Select from 4 back to 0 "Text"
            state.select(TextRange(4, 0))
        }

        rule.runOnIdle {
            assertAnchorInfo(state.selection?.start, offset = 4, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = 0, selectableId = 1)
            assertThat(state.selection?.handlesCrossed).isTrue()

            assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(0, 4))
        }
    }

    @Test
    fun select_thenGesture_Ltr() {
        val state = SelectionState()

        with(rule.density) {
            createSelectionContainerWithState(state) { TestText(textContent) }
            val characterSize = fontSize.toPx()

            rule.onNode(hasText(textContent)).performClick()

            rule.runOnIdle {
                // Select "Text" (offsets 0 to 4)
                state.select(TextRange(0, 4))
            }

            rule.mainClock.advanceTimeByFrame()

            // Drag the end handle (at offset 4) to offset 9 (end of "Demo")
            rule
                .onNode(isSelectionHandle(Handle.SelectionEnd), useUnmergedTree = true)
                .performTouchInput {
                    down(center)
                    val deltaX = (9 - 4) * characterSize
                    moveBy(Offset(deltaX, 0f))
                    up()
                }

            rule.runOnIdle {
                assertAnchorInfo(state.selection?.start, offset = 0, selectableId = 1)
                assertAnchorInfo(state.selection?.end, offset = 9, selectableId = 1)
                assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(0, 9))
            }
        }
    }

    @Test
    fun select_afterGesture_Ltr() {
        val state = SelectionState()

        with(rule.density) {
            createSelectionContainerWithState(state) { TestText(textContent) }
            val characterSize = fontSize.toPx()

            // Select "Demo" by long-clicking 'm'
            rule.onSelectionContainer().performTouchInput {
                longClick(Offset(textContent.indexOf('m') * characterSize, 0.5f * characterSize))
            }

            rule.runOnIdle {
                // Verify that the gesture created a selection for "Demo" (offsets 5 to 9)
                assertAnchorInfo(state.selection?.start, offset = 5, selectableId = 1)
                assertAnchorInfo(state.selection?.end, offset = 9, selectableId = 1)
                assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(5, 9))
            }

            // Call select programmatically to select "Text" (offsets 0 to 4)
            rule.runOnIdle { state.select(TextRange(0, 4)) }

            rule.runOnIdle {
                assertAnchorInfo(state.selection?.start, offset = 0, selectableId = 1)
                assertAnchorInfo(state.selection?.end, offset = 4, selectableId = 1)

                assertThat(state.selectedTexts.first().text).isEqualTo(textContent.substring(0, 4))
            }
        }
    }

    @Test
    fun select_Rtl() {
        val state = SelectionState()
        val rtlText = "الأول الثاني الثالث الرابع"

        createSelectionContainerWithState(state = state, isRtl = true) { TestText(rtlText) }

        rule.runOnIdle {
            // Select the first word "الأول" (offsets 0 to 5)
            state.select(TextRange(0, 5))
        }

        rule.runOnIdle {
            assertThat(state.selection?.start?.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(state.selection?.end?.direction).isEqualTo(ResolvedTextDirection.Rtl)

            assertThat(state.selectedTexts.first().text).isEqualTo(rtlText.substring(0, 5))
        }
    }

    @Test
    fun select_handlesCrossed_Rtl() {
        val state = SelectionState()
        val rtlText = "الأول الثاني الثالث الرابع"

        createSelectionContainerWithState(state = state, isRtl = true) { TestText(rtlText) }

        rule.runOnIdle {
            // Select from offset 5 back to 0 (reversed selection)
            state.select(TextRange(5, 0))
        }

        rule.runOnIdle {
            assertAnchorInfo(
                state.selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 5,
                selectableId = 1,
            )
            assertAnchorInfo(
                state.selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 0,
                selectableId = 1,
            )

            assertThat(state.selection?.handlesCrossed).isTrue()

            assertThat(state.selection?.start?.direction).isEqualTo(ResolvedTextDirection.Rtl)
            assertThat(state.selection?.end?.direction).isEqualTo(ResolvedTextDirection.Rtl)

            assertThat(state.selectedTexts.first().text).isEqualTo(rtlText.substring(0, 5))
        }
    }

    @Test
    fun select_thenGesture_Rtl() {
        val state = SelectionState()
        val rtlText = "الأول الثاني الثالث الرابع"

        createSelectionContainerWithState(state = state, isRtl = true) { TestText(rtlText) }

        rule.runOnIdle {
            // Programmatically select the second word "الثاني" (offsets 6 to 12)
            state.select(TextRange(6, 12))
        }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertAnchorInfo(
                state.selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 6,
                selectableId = 1,
            )
            assertAnchorInfo(
                state.selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 12,
                selectableId = 1,
            )
        }

        // Find the location of offset 0 (the first character 'ا' of "الأول")
        val textNode = rule.onNode(hasText(rtlText))
        val textLayoutResult = textNode.fetchTextLayoutResult()
        val boundingBox = textLayoutResult.getBoundingBox(0)

        textNode.performTouchInput { longClick(boundingBox.center) }

        rule.runOnIdle {
            // Verify that the selection shifted to the first word "الأول" (offsets 0 to 5)
            assertAnchorInfo(
                state.selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 0,
                selectableId = 1,
            )
            assertAnchorInfo(
                state.selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 5,
                selectableId = 1,
            )

            assertThat(state.selectedTexts.first().text).isEqualTo(rtlText.substring(0, 5))
        }
    }

    @Test
    fun select_afterGesture_Rtl() {
        val state = SelectionState()
        val rtlText = "الأول الثاني الثالث الرابع"

        createSelectionContainerWithState(state = state, isRtl = true) { TestText(rtlText) }

        val boundingBox = rule.onNode(hasText(rtlText)).fetchTextLayoutResult().getBoundingBox(0)
        rule.onNode(hasText(rtlText)).performTouchInput { longClick(boundingBox.center) }

        rule.runOnIdle {
            assertAnchorInfo(
                state.selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 0,
                selectableId = 1,
            )
            assertAnchorInfo(
                state.selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 5,
                selectableId = 1,
            )
        }

        // Programmatically select the second word "الثاني" (offsets 6 to 12)
        rule.runOnIdle { state.select(TextRange(6, 12)) }

        rule.runOnIdle {
            assertAnchorInfo(
                state.selection?.start,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 6,
                selectableId = 1,
            )
            assertAnchorInfo(
                state.selection?.end,
                resolvedTextDirection = ResolvedTextDirection.Rtl,
                offset = 12,
                selectableId = 1,
            )

            assertThat(state.selectedTexts.first().text).isEqualTo(rtlText.substring(6, 12))
        }
    }

    @Test
    fun select_collapsedRange() {
        val state = SelectionState()

        createSelectionContainerWithState(state) { TestText(textContent) }

        rule.runOnIdle { state.select(TextRange(1, 1)) }

        rule.runOnIdle {
            assertAnchorInfo(state.selection?.start, offset = 1, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = 1, selectableId = 1)

            assertThat(state.selectedTexts).isEmpty()
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()
    }

    @Test
    fun select_invalidRange_ignored() {
        val state = SelectionState()

        createSelectionContainerWithState(state) { TestText(textContent) }

        rule.runOnIdle {
            // Try to select out of bounds
            state.select(TextRange(0, 100))
        }

        rule.runOnIdle {
            assertThat(state.selection).isNull()
            assertThat(state.selectedTexts).isEmpty()
            assertThat(state.manager?.showToolbar).isFalse()
        }

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertDoesNotExist()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertDoesNotExist()
    }

    @Test
    fun select_crossesMultipleTexts() {
        val state = SelectionState()

        createSelectionContainerWithState(state) {
            Column {
                TestText("Hello ")
                TestText("World")
            }
        }

        rule.runOnIdle {
            // Select "Hello World" across both texts
            state.select(TextRange(0, 11))
        }

        rule.runOnIdle {
            assertAnchorInfo(state.selection?.start, offset = 0, selectableId = 1)
            assertAnchorInfo(state.selection?.end, offset = 5, selectableId = 2)

            assertThat(state.selectedTexts.size).isEqualTo(2)
            assertThat(state.selectedTexts[0].text).isEqualTo("Hello ")
            assertThat(state.selectedTexts[1].text).isEqualTo("World")
        }
    }

    @Test
    fun select_showsHandlesAndToolbar() {
        val state = SelectionState()

        createSelectionContainerWithState(state) { TestText(textContent) }

        rule.runOnIdle { state.select(TextRange(0, 4)) }

        rule.mainClock.advanceTimeByFrame()

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()

        rule.runOnIdle { assertThat(state.manager?.showToolbar).isTrue() }
    }
}
