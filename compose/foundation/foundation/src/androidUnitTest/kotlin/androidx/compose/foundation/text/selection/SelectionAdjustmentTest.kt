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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.text.TextRange
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class SelectionAdjustmentTest {

    @Test
    fun none_noAdjustment() {
        val layout = getSingleSelectionLayoutFake(
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 5,
        )

        val actualSelection = SelectionAdjustment.None.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 5)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun none_allowCollapsed() {
        val layout = getSingleSelectionLayoutFake(
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 0,
        )

        val actualSelection = SelectionAdjustment.None.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun none_reversed() {
        val layout = getSingleSelectionLayoutFake(
            rawStartHandleOffset = 5,
            rawEndHandleOffset = 0,
        )

        val actualSelection = SelectionAdjustment.None.adjust(layout)
        val expectedSelection = getSelection(startOffset = 5, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun none_multiSelectable() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                getSelectableInfoFake(selectableId = 1L, slot = 1),
                getSelectableInfoFake(selectableId = 2L, slot = 3),
            ),
            currentInfoIndex = 0,
            startSlot = 1,
            endSlot = 3,
        )

        val actualSelection = SelectionAdjustment.None.adjust(layout)
        val expectedSelection = getSelection(
            startSelectableId = 1L,
            startOffset = 0,
            endSelectableId = 2L,
            endOffset = 5
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_notCollapsed_noAdjustment() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world",
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 3,
            isStartHandle = true,
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 3)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedNotReversed_returnOneCharSelectionNotReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello",
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 1,
            previousSelection = getSelection(startOffset = 1, endOffset = 2),
        )

        // The end offset is moving towards the start offset,
        // which makes the new raw text range collapsed.
        // After the adjustment, at least one character should be selected.
        // Since the previousTextRange is not reversed,
        // the adjusted TextRange should also be not reversed.
        // Based the above rules, adjusted text range should be [1, 2).

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 1, endOffset = 2)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedReversed_returnOneCharSelectionReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello",
            rawStartHandleOffset = 2,
            rawEndHandleOffset = 2,
            previousSelection = getSelection(startOffset = 2, endOffset = 1),
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 2, endOffset = 1)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedNotReversed_startBoundary_returnOneCharSelectionNotReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello",
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 0,
            previousSelection = getSelection(startOffset = 0, endOffset = 1),
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 1)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedReversed_startBoundary_returnOneCharSelectionReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello",
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 0,
            previousSelection = getSelection(startOffset = 1, endOffset = 0),
            isStartHandle = true,
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 1, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedNotReversed_endBoundary_returnOneCharSelectionNotReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello",
            rawStartHandleOffset = 5,
            rawEndHandleOffset = 5,
            previousSelection = getSelection(startOffset = 4, endOffset = 5),
            isStartHandle = true,
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 4, endOffset = 5)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedReversed_endBoundary_returnOneCharSelectionReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello",
            rawStartHandleOffset = 5,
            rawEndHandleOffset = 5,
            previousSelection = getSelection(startOffset = 5, endOffset = 4),
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 5, endOffset = 4)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedNotReversed_returnOneUnicodeSelectionNotReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hi\uD83D\uDE00",
            rawStartHandleOffset = 2,
            rawEndHandleOffset = 2,
            previousSelection = getSelection(startOffset = 2, endOffset = 4),
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 2, endOffset = 4)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedReversed_returnOneUnicodeSelectionReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hi\uD83D\uDE00",
            rawStartHandleOffset = 4,
            rawEndHandleOffset = 4,
            previousSelection = getSelection(startOffset = 4, endOffset = 2),
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 4, endOffset = 2)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedNotReversed_returnOneEmojiSelectionNotReversed() {
        // After the adjustment, the unicode sequence representing the keycap # emoji should be
        // selected instead of a single character/unicode that is only part of the emoji.
        val layout = getSingleSelectionLayoutFake(
            text = "#️⃣sharp",
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 0,
            previousSelection = getSelection(startOffset = 0, endOffset = 3),
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 3)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedReversed_returnOneEmojiSelectionReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "#️⃣sharp",
            rawStartHandleOffset = 3,
            rawEndHandleOffset = 3,
            previousSelection = getSelection(startOffset = 3, endOffset = 0),
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(startOffset = 3, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedException_multiText_twoTexts() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                // selection starts at the end of the first text...
                getSelectableInfoFake(
                    selectableId = 1L,
                    slot = 1,
                    text = "hello",
                    rawStartHandleOffset = 5,
                    rawEndHandleOffset = 5,
                ),
                // and ends at the beginning of the second text...
                getSelectableInfoFake(
                    selectableId = 2L,
                    slot = 3,
                    text = "hello",
                    rawStartHandleOffset = 0,
                    rawEndHandleOffset = 0,
                ),
                // so the entire selection covers 0 characters, and thus is collapsed
            ),
            currentInfoIndex = 0,
            startSlot = 1,
            endSlot = 3,
            previousSelection = getSelection(
                startSelectableId = 1L,
                startOffset = 5,
                endSelectableId = 2L,
                endOffset = 1
            )
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(
            startSelectableId = 1L,
            startOffset = 5,
            endSelectableId = 2L,
            endOffset = 0
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedException_multiText_threeTexts() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                // selection starts at the end of the first text...
                getSelectableInfoFake(
                    selectableId = 1L,
                    slot = 1,
                    text = "hello",
                    rawStartHandleOffset = 5,
                    rawEndHandleOffset = 5,
                ),
                // continues through the second empty text...
                getSelectableInfoFake(
                    selectableId = 2L,
                    slot = 3,
                    text = "",
                    rawStartHandleOffset = 0,
                    rawEndHandleOffset = 0,
                ),
                // and ends at the beginning of the third text...
                getSelectableInfoFake(
                    selectableId = 3L,
                    slot = 5,
                    text = "hello",
                    rawStartHandleOffset = 0,
                    rawEndHandleOffset = 0,
                ),
                // so the entire selection covers 0 characters, and thus is collapsed
            ),
            currentInfoIndex = 0,
            startSlot = 1,
            endSlot = 5,
            previousSelection = getSelection(
                startSelectableId = 1L,
                startOffset = 5,
                endSelectableId = 3L,
                endOffset = 1
            )
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(
            startSelectableId = 1L,
            startOffset = 5,
            endSelectableId = 3L,
            endOffset = 0
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun character_collapsedException_previousIsNull() {
        val selection = getSelection(
            startOffset = 0,
            endOffset = 0
        )

        val layout = getSingleSelectionLayoutFake(
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 0,
            rawPreviousHandleOffset = 0,
            previousSelection = null
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        assertThat(actualSelection).isEqualTo(selection)
    }

    @Test
    fun character_collapsedException_emptyText() {
        val layout = getSingleSelectionLayoutFake(
            text = "",
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 0,
        )

        val actualSelection = SelectionAdjustment.Character.adjust(layout)
        val expectedSelection = getSelection(
            startOffset = 0,
            endOffset = 0
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_collapsed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world",
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 1,
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 5)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_collapsed_onStartBoundary() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world",
            rawStartHandleOffset = 6,
            rawEndHandleOffset = 6,
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 6, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_collapsed_onEndBoundary() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world",
            rawStartHandleOffset = 5,
            rawEndHandleOffset = 5,
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 5)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_collapsed_zero() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world",
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 0,
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 5)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_collapsed_lastIndex() {
        val text = "hello world"
        val layout = getSingleSelectionLayoutFake(
            text = text,
            rawStartHandleOffset = text.lastIndex,
            rawEndHandleOffset = text.lastIndex,
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 6, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_collapsed_textLength() {
        val text = "hello world"
        val layout = getSingleSelectionLayoutFake(
            text = text,
            rawStartHandleOffset = text.length,
            rawEndHandleOffset = text.length,
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 6, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_collapsed_emptyString() {
        val layout = getSingleSelectionLayoutFake(
            text = "",
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 0,
            wordBoundaries = listOf(TextRange(0, 0))
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_notReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world",
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 2,
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 5)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world",
            rawStartHandleOffset = 2,
            rawEndHandleOffset = 1,
            wordBoundaries = listOf(TextRange(0, 5), TextRange(6, 11))
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 5, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_crossWords() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 4,
            rawEndHandleOffset = 7,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_crossWords_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 7,
            rawEndHandleOffset = 4,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(startOffset = 11, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_multiText() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                getSelectableInfoFake(
                    selectableId = 1L,
                    slot = 1,
                    text = "hello world",
                    rawStartHandleOffset = 8,
                    rawEndHandleOffset = 11,
                    wordBoundaries = listOf(
                        TextRange(0, 5),
                        TextRange(6, 11),
                    )
                ),
                getSelectableInfoFake(
                    selectableId = 2L,
                    slot = 3,
                    text = "hello world",
                    rawStartHandleOffset = 0,
                    rawEndHandleOffset = 3,
                    wordBoundaries = listOf(
                        TextRange(0, 5),
                        TextRange(6, 11),
                    )
                ),
            ),
            currentInfoIndex = 0,
            startSlot = 1,
            endSlot = 3,
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(
            startSelectableId = 1L,
            startOffset = 6,
            endSelectableId = 2L,
            endOffset = 5,
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun word_multiText_reversed() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                getSelectableInfoFake(
                    selectableId = 1L,
                    slot = 1,
                    text = "hello world",
                    rawStartHandleOffset = 11,
                    rawEndHandleOffset = 8,
                    wordBoundaries = listOf(
                        TextRange(0, 5),
                        TextRange(6, 11),
                    )
                ),
                getSelectableInfoFake(
                    selectableId = 2L,
                    slot = 3,
                    text = "hello world",
                    rawStartHandleOffset = 3,
                    rawEndHandleOffset = 0,
                    wordBoundaries = listOf(
                        TextRange(0, 5),
                        TextRange(6, 11),
                    )
                ),
            ),
            currentInfoIndex = 0,
            startSlot = 3,
            endSlot = 1,
        )

        val actualSelection = SelectionAdjustment.Word.adjust(layout)
        val expectedSelection = getSelection(
            startSelectableId = 2L,
            startOffset = 5,
            endSelectableId = 1L,
            endOffset = 6,
            handlesCrossed = true,
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun paragraph_collapsed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world\nhello world",
            rawStartHandleOffset = 14,
            rawEndHandleOffset = 14,
        )

        val actualSelection = SelectionAdjustment.Paragraph.adjust(layout)
        val expectedSelection = getSelection(startOffset = 12, endOffset = 23)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun paragraph_collapsed_zero() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world\nhello world\nhello world\nhello world",
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 0,
        )

        val actualSelection = SelectionAdjustment.Paragraph.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun paragraph_collapsed_lastIndex() {
        val text = "hello world\nhello world"
        val layout = getSingleSelectionLayoutFake(
            text = text,
            rawStartHandleOffset = text.lastIndex,
            rawEndHandleOffset = text.lastIndex,
        )

        val actualSelection = SelectionAdjustment.Paragraph.adjust(layout)
        val expectedSelection = getSelection(startOffset = 12, endOffset = 23)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun paragraph_collapsed_textLength() {
        val text = "hello world\nhello world"
        val layout = getSingleSelectionLayoutFake(
            text = text,
            rawStartHandleOffset = text.length,
            rawEndHandleOffset = text.length,
        )

        val actualSelection = SelectionAdjustment.Paragraph.adjust(layout)
        val expectedSelection = getSelection(startOffset = 12, endOffset = 23)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun paragraph_emptyString() {
        val layout = getSingleSelectionLayoutFake(
            text = "",
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 0,
        )

        val actualSelection = SelectionAdjustment.Paragraph.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun paragraph_notReversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world\nhello world",
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 2,
        )

        val actualSelection = SelectionAdjustment.Paragraph.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun paragraph_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world\nhello world",
            rawStartHandleOffset = 2,
            rawEndHandleOffset = 1,
        )

        val actualSelection = SelectionAdjustment.Paragraph.adjust(layout)
        val expectedSelection = getSelection(startOffset = 11, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun paragraph_crossParagraph() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world\nhello world\nhello world\nhello world",
            rawStartHandleOffset = 13,
            rawEndHandleOffset = 26,
        )

        val actualSelection = SelectionAdjustment.Paragraph.adjust(layout)
        val expectedSelection = getSelection(startOffset = 12, endOffset = 35)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun paragraph_multiText() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                getSelectableInfoFake(
                    selectableId = 1L,
                    slot = 1,
                    text = "hello world",
                    rawStartHandleOffset = 8,
                    rawEndHandleOffset = 11,
                ),
                getSelectableInfoFake(
                    selectableId = 2L,
                    slot = 3,
                    text = "hello world",
                    rawStartHandleOffset = 0,
                    rawEndHandleOffset = 3,
                ),
            ),
            currentInfoIndex = 0,
            startSlot = 1,
            endSlot = 3,
        )

        val actualSelection = SelectionAdjustment.Paragraph.adjust(layout)
        val expectedSelection = getSelection(
            startSelectableId = 1L,
            startOffset = 0,
            endSelectableId = 2L,
            endOffset = 11,
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun paragraph_multiText_reversed() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                getSelectableInfoFake(
                    selectableId = 1L,
                    slot = 1,
                    text = "hello world",
                    rawStartHandleOffset = 11,
                    rawEndHandleOffset = 8,
                ),
                getSelectableInfoFake(
                    selectableId = 2L,
                    slot = 3,
                    text = "hello world",
                    rawStartHandleOffset = 3,
                    rawEndHandleOffset = 0,
                ),
            ),
            currentInfoIndex = 0,
            startSlot = 3,
            endSlot = 1,
        )

        val actualSelection = SelectionAdjustment.Paragraph.adjust(layout)
        val expectedSelection = getSelection(
            startSelectableId = 2L,
            startOffset = 11,
            endSelectableId = 1L,
            endOffset = 0,
            handlesCrossed = true,
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_initialSelection() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 3,
            rawEndHandleOffset = 3,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        // initial selection just uses SelectionAdjustment.Word.
        val expectedSelection = getSelection(startOffset = 0, endOffset = 5)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandEndWithinWord() {
        // The previous selection is [6, 7) and new selection expand the end to 8. This is
        // considered in-word selection. And it will use character-wise selection
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 6,
            rawEndHandleOffset = 8,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 6, endOffset = 7),
            rawPreviousHandleOffset = 7,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 6, endOffset = 8)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandStartWithinWord_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 8,
            rawEndHandleOffset = 6,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 7, endOffset = 6),
            rawPreviousHandleOffset = 7,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 8, endOffset = 6)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandStartWithinWord() {
        // The previous selection is [7, 11) and new selection expand the start to 8. This is
        // considered in-word selection. And it will use character-wise selection
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 8,
            rawEndHandleOffset = 11,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 7, endOffset = 11),
            rawPreviousHandleOffset = 7,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 8, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandEndWithinWord_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 11,
            rawEndHandleOffset = 8,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 11, endOffset = 7),
            rawPreviousHandleOffset = 7,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 11, endOffset = 8)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandEndOutOfWord() {
        // The previous selection is [6, 11) and the new selection expand the end to 12.
        // Because the previous selection end is at word boundary, it will use word selection mode.
        // The end did exceed start of the next word(offset = 12), the adjusted
        // selection end will be 17, which is the end of the next word.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 6,
            rawEndHandleOffset = 12,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 6, endOffset = 11),
            rawPreviousHandleOffset = 11,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 6, endOffset = 17)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandStartOutOfWord_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 13,
            rawEndHandleOffset = 6,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 11, endOffset = 6),
            rawPreviousHandleOffset = 11,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 17, endOffset = 6)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandStartOutOfWord() {
        // The previous selection is [6, 11) and the new selection expand the start to 5.
        // Because the previous selection start is at word boundary,
        // it will use word selection mode.
        // The start did exceed the end of the previous word(offset = 5),
        // the adjusted selection end will be 0, which is the start of the previous word.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 5,
            rawEndHandleOffset = 11,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 6, endOffset = 11),
            rawPreviousHandleOffset = 6,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandEndOutOfWord_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 11,
            rawEndHandleOffset = 5,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 11, endOffset = 6),
            rawPreviousHandleOffset = 6,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            )
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 11, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandEndToNextLine() {
        // The text line break is as shown (underscore for space):
        //   hello_
        //   world_
        //   hello_
        //   world_
        // The previous selection is [3, 4) and new selection expand the end to 8. Because offset
        // 8 is at the next line, it will use word based selection strategy
        // and the end will be adjusted to word end: 11.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 3,
            rawEndHandleOffset = 7,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 3, endOffset = 4),
            rawPreviousHandleOffset = 4,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(6, 12, 18)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 3, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandStartToNextLine_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 7,
            rawEndHandleOffset = 3,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 4, endOffset = 3),
            rawPreviousHandleOffset = 4,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(6, 12, 18)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 11, endOffset = 3)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandStartToNextLine() {
        // The text line break is as shown(underscore for space):
        //   hello_
        //   world_
        //   hello_
        //   world_
        // The previous selection is [6, 8) and new selection expand the start to 3. Because offset
        // 3 is at the previous line, it will use word based selection strategy.
        // The end will be adjusted to word start: 0.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 4,
            rawEndHandleOffset = 8,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 7, endOffset = 8),
            rawPreviousHandleOffset = 7,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(6, 12, 18)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 8)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandEndToNextLine_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 8,
            rawEndHandleOffset = 3,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 8, endOffset = 7),
            rawPreviousHandleOffset = 7,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(6, 12, 18)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 8, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandEndToNextLine_withinWord() {
        // The text line break is as shown:
        //   hello wo
        //   rld hell
        //   o world
        // The previous selection is [3, 7) and the end is expanded to 9, which is the next line.
        // Because end offset is moving between lines, it will use word based selection. In this
        // case the word "world" crosses 2 lines, so the candidate values for the adjusted end
        // offset are 8(first character of the line) and 11(word end).
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 3,
            rawEndHandleOffset = 9,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 3, endOffset = 7),
            rawPreviousHandleOffset = 7,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(8, 16)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 3, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandStartToNextLine_withinWord_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 9,
            rawEndHandleOffset = 3,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 7, endOffset = 3),
            rawPreviousHandleOffset = 7,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(8, 16)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 11, endOffset = 3)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandStartToNextLine_withinWord() {
        // The text line break is as shown:
        //   hello wo
        //   rld hell
        //   o world
        // The previous selection is [16, 17) and the start is expanded to 15, which is at the
        // previous line.
        // Because start offset is moving between lines, it will use word based selection. In this
        // case the word "hello" crosses 2 lines. The candidate values for the adjusted start
        // offset are 12(word start) and 16(last character of the line). Since we are expanding
        // back, the end offset will be adjusted to the word start at 12.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 15,
            rawEndHandleOffset = 17,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 16, endOffset = 17),
            rawPreviousHandleOffset = 16,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(8, 16)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 12, endOffset = 17)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_expandEndToNextLine_withinWord_reverse() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 17,
            rawEndHandleOffset = 15,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 17, endOffset = 16),
            rawPreviousHandleOffset = 16,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(8, 16)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 17, endOffset = 12)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_shrinkEnd() {
        // The previous selection is [0, 11) and new selection shrink the end to 8. In this case
        // it will use character based selection strategy.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 8,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 0, endOffset = 11),
            rawPreviousHandleOffset = 11,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 8)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_shrinkStart_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 8,
            rawEndHandleOffset = 0,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 11, endOffset = 0),
            rawPreviousHandleOffset = 11,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 8, endOffset = 0)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_shrinkStart() {
        // The previous selection is [0, 8) and new selection shrink the start to 2. In this case
        // it will use character based selection strategy.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 2,
            rawEndHandleOffset = 8,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 0, endOffset = 8),
            rawPreviousHandleOffset = 0,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 2, endOffset = 8)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_shrinkEnd_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 8,
            rawEndHandleOffset = 2,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 8, endOffset = 0),
            rawPreviousHandleOffset = 0,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 8, endOffset = 2)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_shrinkEndToPrevLine() {
        // The text line break is as shown(underscore for space):
        //   hello_
        //   world_
        //   hello_
        //   world_
        // The previous selection is [2, 8) and new selection shrink the end to 4. Because offset
        // 4 is at the previous line, it will use word based selection strategy. And the end will
        // be snap to 5.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 2,
            rawEndHandleOffset = 4,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 2, endOffset = 8),
            rawPreviousHandleOffset = 8,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(6, 12, 18)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 2, endOffset = 5)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_shrinkStartToPrevLine_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 4,
            rawEndHandleOffset = 2,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 8, endOffset = 2),
            rawPreviousHandleOffset = 8,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(6, 12, 18)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 5, endOffset = 2)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_shrinkStartToNextLine() {
        // The text line break is as shown(underscore for space):
        //   hello_
        //   world_
        //   hello_
        //   world_
        // The previous selection is [2, 8) and new selection shrink the end to 7. Because offset
        // 7 is at the next line, it will use word based selection strategy. And the start will
        // be snap to 6.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 7,
            rawEndHandleOffset = 8,
            isStartHandle = true,
            previousSelection = getSelection(startOffset = 2, endOffset = 8),
            rawPreviousHandleOffset = 2,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(6, 12, 18)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 6, endOffset = 8)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_shrinkEndToNextLine_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 8,
            rawEndHandleOffset = 7,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 8, endOffset = 2),
            rawPreviousHandleOffset = 2,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(6, 12, 18)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 8, endOffset = 6)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_crossLineSelection_notCollapsed() {
        // The text line break is as shown(underscore for space):
        //   hello_
        //   world_
        //   hello_
        //   world_
        // The previous selection is [6, 15) and new selection move the end to 7. Because offset
        // 7 is at the previous line, it will use word based selection strategy.
        // Normally, the new end will snap to the closest word boundary,
        // which is 6(the word "world"'s boundaries are 6 and 11).
        // However, in this specific case the selection start offset is already 6,
        // adjusting the end to 6 will result in a collapsed selection [6, 6). So, it should
        // move the end offset to the other word boundary which is 11 instead.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world hello world",
            rawStartHandleOffset = 6,
            rawEndHandleOffset = 7,
            isStartHandle = false,
            previousSelection = getSelection(startOffset = 6, endOffset = 15),
            rawPreviousHandleOffset = 15,
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
                TextRange(12, 17),
                TextRange(18, 23)
            ),
            lineBreaks = listOf(6, 12, 18)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 6, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_betweenSlots_usesCurrentIndex() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                getSelectableInfoFake(
                    text = "hello\nhello\nhello",
                    selectableId = 1L,
                    slot = 1,
                    rawStartHandleOffset = 8,
                    rawEndHandleOffset = 11,
                    lineBreaks = listOf(6, 12),
                    rawPreviousHandleOffset = 6,
                    wordBoundaries = listOf(
                        TextRange(0, 5),
                        TextRange(6, 11),
                        TextRange(12, 17),
                    ),
                ),
            ),
            currentInfoIndex = 0,
            startSlot = 1,
            endSlot = 2, // below the current text
            previousSelection = getSelection(startOffset = 6, endOffset = 11),
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 6, endOffset = 11)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_betweenSlots_usesCurrentIndex_reversed() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                getSelectableInfoFake(
                    text = "hello\nhello\nhello",
                    selectableId = 1L,
                    slot = 1,
                    rawStartHandleOffset = 8,
                    rawEndHandleOffset = 6,
                    lineBreaks = listOf(6, 12),
                    rawPreviousHandleOffset = 6,
                    wordBoundaries = listOf(
                        TextRange(0, 5),
                        TextRange(6, 11),
                        TextRange(12, 17),
                    ),
                ),
            ),
            currentInfoIndex = 0,
            startSlot = 1,
            endSlot = 0, // above the current text
            previousSelection = getSelection(startOffset = 11, endOffset = 6),
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 11, endOffset = 6)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_differentSelectable_usesWordBoundary() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                getSelectableInfoFake(
                    text = "hello",
                    selectableId = 1L,
                    slot = 1,
                    rawStartHandleOffset = 3,
                    rawEndHandleOffset = 5,
                    rawPreviousHandleOffset = 5,
                    wordBoundaries = listOf(TextRange(0, 5)),
                ),
                getSelectableInfoFake(
                    text = "hello",
                    selectableId = 2L,
                    slot = 3,
                    rawStartHandleOffset = 0,
                    rawEndHandleOffset = 3,
                    rawPreviousHandleOffset = 2,
                    wordBoundaries = listOf(TextRange(0, 5)),
                ),
            ),
            currentInfoIndex = 0,
            startSlot = 1,
            endSlot = 3,
            isStartHandle = true,
            previousSelection = getSelection(
                startSelectableId = 2L,
                startOffset = 2,
                endSelectableId = 2L,
                endOffset = 3,
                handlesCrossed = false,
            ),
        )

        // selection goes from the second text at [2, 3] and moves the start handle to the third
        // offset of the third text. Because it moves texts, it uses word based adjustment.

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(
            startSelectableId = 1L,
            startOffset = 0,
            endSelectableId = 2L,
            endOffset = 3,
            handlesCrossed = false,
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_differentSelectable_usesWordBoundary_reversed() {
        val layout = getSelectionLayoutFake(
            infos = listOf(
                getSelectableInfoFake(
                    text = "hello",
                    selectableId = 1L,
                    slot = 1,
                    rawStartHandleOffset = 5,
                    rawEndHandleOffset = 3,
                    rawPreviousHandleOffset = 5,
                    wordBoundaries = listOf(TextRange(0, 5)),
                ),
                getSelectableInfoFake(
                    text = "hello",
                    selectableId = 2L,
                    slot = 3,
                    rawStartHandleOffset = 3,
                    rawEndHandleOffset = 0,
                    rawPreviousHandleOffset = 2,
                    wordBoundaries = listOf(TextRange(0, 5)),
                ),
            ),
            currentInfoIndex = 0,
            startSlot = 3,
            endSlot = 1,
            isStartHandle = false,
            previousSelection = getSelection(
                startSelectableId = 2L,
                startOffset = 3,
                endSelectableId = 2L,
                endOffset = 2,
                handlesCrossed = true,
            ),
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(
            endSelectableId = 1L,
            endOffset = 0,
            startSelectableId = 2L,
            startOffset = 3,
            handlesCrossed = true,
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_collapsed_usesCorrectCross() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello",
            rawStartHandleOffset = 5,
            rawEndHandleOffset = 5,
            rawPreviousHandleOffset = 5,
            wordBoundaries = listOf(TextRange(0, 5)),
            isStartHandle = false,
            previousSelection = getSelection(
                startSelectableId = 1L,
                startOffset = 5,
                endSelectableId = 2L,
                endOffset = 0,
                handlesCrossed = false,
            ),
        )

        // The selection goes from a collapsed selection from selectable one to selectable two to
        // a collapsed selection at the end of selectable one.
        // Because the end handle goes from selectable two to one, it uses word adjustment.
        // We want to ensure that the handle cross state updates correctly, since this is a case
        // of a collapsed cross state from the layout.

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(
            startSelectableId = 1L,
            startOffset = 5,
            endSelectableId = 1L,
            endOffset = 0,
            handlesCrossed = true,
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_multiSelectableCollapsed_usesCorrectCross_reversed() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello",
            rawStartHandleOffset = 5,
            rawEndHandleOffset = 5,
            rawPreviousHandleOffset = 5,
            wordBoundaries = listOf(TextRange(0, 5)),
            isStartHandle = true,
            previousSelection = getSelection(
                startSelectableId = 2L,
                startOffset = 0,
                endSelectableId = 1L,
                endOffset = 5,
                handlesCrossed = false,
            ),
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(
            startSelectableId = 1L,
            startOffset = 0,
            endSelectableId = 1L,
            endOffset = 5,
            handlesCrossed = false,
        )
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    @Test
    fun characterWithWordAccelerate_largeOffsetJump_cross_sameAsPrevious() {
        // emulates a BiDi layout where the offset may jump a large amount.
        // In this case, a 0-8 selection has its start handle jump to 10.
        // The previous handle also is set to 10 as this emulates a selection handle performing its
        // first drag. This results in the old selection being re-used, but the "crossStatus"
        // changing. The layout thinks it is crossed, while the re-used selection is not.
        // It should go with the value of selection's handlesCrossed.
        val layout = getSingleSelectionLayoutFake(
            text = "hello world",
            rawStartHandleOffset = 10,
            rawEndHandleOffset = 8,
            rawPreviousHandleOffset = 10,
            isStartHandle = true,
            previousSelection = getSelection(
                startOffset = 0,
                endOffset = 8
            ),
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
            ),
            rtlRanges = listOf(0..0, 11..11)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 0, endOffset = 8)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }

    /*
     * TODO(b/281587713)
     *  This is a reproduction of a BiDi case that is currently broken.
     *  Because the start handle goes from 11 to 1, the selection counts as both expanding and
     *  the previous offset is on a word boundary, leading to the selection using word adjustment
     *  for the start handle. We want it to use character adjustment.
     */
    @Ignore
    @Test
    fun characterWithWordAccelerate_largeOffsetJump_cross_updatesSelection() {
        val layout = getSingleSelectionLayoutFake(
            text = "hello world",
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 8,
            rawPreviousHandleOffset = 11,
            isStartHandle = true,
            previousSelection = getSelection(
                startOffset = 0,
                endOffset = 8
            ),
            wordBoundaries = listOf(
                TextRange(0, 5),
                TextRange(6, 11),
            ),
            rtlRanges = listOf(0..0, 11..11)
        )

        val actualSelection = SelectionAdjustment.CharacterWithWordAccelerate.adjust(layout)
        val expectedSelection = getSelection(startOffset = 1, endOffset = 8)
        assertThat(actualSelection).isEqualTo(expectedSelection)
    }
}
