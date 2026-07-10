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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.text.input.ExpandPolicy
import androidx.compose.foundation.text.input.TrackedRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextStyleBufferTest {

    @Test
    fun basicAddAndGet() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style1", Interval(0, 10))
        buffer.addStyle("style2", Interval(5, 15))

        assertThat(buffer.getAllStyles())
            .containsExactly(
                AnnotatedString.Range("style1", 0, 10),
                AnnotatedString.Range("style2", 5, 15),
            )
            .inOrder()
    }

    @Test
    fun removeStyle() {
        val buffer = TextStyleBuffer<String>()
        val handle = buffer.addStyle("a", Interval(0, 10))
        buffer.removeStyle(handle)
        assertThat(buffer.getAllStyles()).isEmpty()
    }

    @Test
    fun orderIsPreserved() {
        val buffer = TextStyleBuffer<Int>()
        buffer.addStyle(1, Interval(10, 20))
        buffer.addStyle(2, Interval(0, 30))
        buffer.addStyle(3, Interval(5, 15))

        // Verifies that styles are returned in insertion order, regardless of their ranges.
        assertThat(buffer.getStyles<Int>(0, 30).map { buffer.getItem<Int>(it) })
            .containsExactly(1, 2, 3)
            .inOrder()
    }

    @Test
    fun equalsAndHashCode() {
        val buffer1 =
            TextStyleBuffer<String>().apply {
                addStyle("a", Interval(0, 10))
                addStyle("b", Interval(10, 20))
            }
        val buffer2 =
            TextStyleBuffer<String>().apply {
                addStyle("a", Interval(0, 10))
                addStyle("b", Interval(10, 20))
            }
        val buffer3 =
            TextStyleBuffer<String>().apply {
                addStyle("b", Interval(10, 20))
                addStyle("a", Interval(0, 10))
            }

        assertThat(buffer1).isEqualTo(buffer2)
        assertThat(buffer1.hashCode()).isEqualTo(buffer2.hashCode())

        // Order matters for equality because it affects the rendered result.
        assertThat(buffer1).isNotEqualTo(buffer3)

        // Inserts 1 character at buffer1's gap.
        // This operation only updates the gap and does not modify the internal interval tree.
        // Note that the equals() and hashCode() methods must account for the difference
        // in gap state.
        buffer1.replaceText(buffer1.gapStart, buffer1.gapStart, 1)
        assertThat(buffer1).isNotEqualTo(buffer2)
        assertThat(buffer1.hashCode()).isNotEqualTo(buffer2.hashCode())
    }

    @Test
    fun copy_isEqualAndIndependent() {
        val original = TextStyleBuffer<String>().apply { addStyle("a", Interval(0, 10)) }

        val copy = TextStyleBuffer(original)
        assertThat(copy).isEqualTo(original)

        copy.addStyle("b", Interval(10, 20))
        assertThat(copy).isNotEqualTo(original)
        assertThat(original.getAllStyles()).hasSize(1)
        assertThat(copy.getAllStyles()).hasSize(2)
    }

    @Test
    fun clear() {
        val buffer = TextStyleBuffer<String>().apply { addStyle("a", Interval(0, 10)) }
        buffer.clear()
        assertThat(buffer.getAllStyles()).isEmpty()
    }

    @Test
    fun replaceText_noOverlap() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", Interval(10, 20))

        // Replace [0, 5) with length 10. (Net +5)
        buffer.replaceText(0, 5, 10)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 15, 25))

        // Replace [30, 40) with length 0. (Net -10)
        buffer.replaceText(30, 40, 0)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 15, 25))
    }

    @Test
    fun replaceText_overlappingStart() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", Interval(10, 20))

        // Replace [8, 12) with length 4. (Net 0)
        // [8, 12) deleted. Style start 10 was inside, moves to 12.
        buffer.replaceText(8, 12, 4)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 12, 20))
    }

    @Test
    fun replaceText_overlappingEnd() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", Interval(10, 20, flag1 = false, flag2 = true))

        // Replace [18, 22) with length 4. (Net 0)
        // [18, 22) deleted. Style end 20 was inside, moves to 18 (start of deletion).
        // 4 characters inserted at 18, style end is extended to 22.
        buffer.replaceText(18, 22, 4)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 10, 22))
    }

    @Test
    fun replaceText_fullyContained() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", Interval(10, 20))

        // Replace [12, 18) with length 10. (Net +4)
        buffer.replaceText(12, 18, 10)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 10, 24))
    }

    @Test
    fun replaceText_fullyCovering() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", Interval(10, 20))

        // Replace [5, 25) with length 5.
        // Style is fully within [5, 25), so it should be removed.
        buffer.replaceText(5, 25, 5)
        assertThat(buffer.getAllStyles()).isEmpty()
    }

    @Test
    fun replaceText_exactlyCovering() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", Interval(10, 20))

        buffer.replaceText(10, 20, 5)
        assertThat(buffer.getAllStyles()).isEmpty()
    }

    @Test
    fun replaceText_boundaryBehavior() {
        val buffer = TextStyleBuffer<String>()
        // TextStyleBuffer uses flag1(flag2) to indicates if start(end) expands when text is
        // inserted at start(end).
        buffer.addStyle("style", Interval(10, 20, flag1 = false, flag2 = true))

        // Insert at 10. (Exclusive at start by default)
        buffer.replaceText(10, 10, 5)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 15, 25))

        // Insert at 25 (original 20). (Inclusive at end by default)
        buffer.replaceText(25, 25, 5)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 15, 30))
    }

    @Test
    fun replaceText_expandFlags_atStart() {
        val buffer = TextStyleBuffer<String>()

        // EXPAND_AT_START means it should include text inserted at the start index.
        buffer.addStyle("style", Interval(10, 20, flag1 = true, flag2 = false))

        buffer.replaceText(10, 10, 5)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 10, 25))
    }

    @Test
    fun replaceText_expandFlags_none() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", Interval(10, 20, flag1 = false, flag2 = false))

        // Insert at 10 (start). Should shift.
        buffer.replaceText(10, 10, 5)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 15, 25))

        // Insert at 25 (end). Should NOT expand.
        buffer.replaceText(25, 25, 5)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 15, 25))
    }

    @Test
    fun replaceText_expandFlags_both() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", Interval(10, 20, flag1 = true, flag2 = true))

        // Insert at 10. Should expand start.
        buffer.replaceText(10, 10, 5)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 10, 25))

        // Insert at 25. Should expand end.
        buffer.replaceText(25, 25, 5)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 10, 30))
    }

    @Test
    fun replaceText_multipleStyles() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("a", Interval(10, 20))
        buffer.addStyle("b", Interval(15, 25))

        // Replace [12, 18) with length 2. (Net -4)
        // this is equivalent to delete [12, 18) then insert 2 characters at 12.
        // "a": [10, 20) -> 10 is before 12. 20 is after 18. New end: 20 - 4 = 16. -> [10, 16)
        // "b": [15, 25) -> after deletion of [12, 18) 15 is moved to 12 and 25 moves to 19.
        // then 2 characters are inserted at 12. Resulting in [14, 21)
        buffer.replaceText(12, 18, 2)

        assertThat(buffer.getAllStyles())
            .containsExactly(AnnotatedString.Range("a", 10, 16), AnnotatedString.Range("b", 14, 21))
            .inOrder()
    }

    @Test
    fun stressTest() {
        val random = Random(42)
        val buffer = TextStyleBuffer<Int>()
        val reference = ReferenceTextStyleBuffer<Int>(1000)

        repeat(5000) {
            when (random.nextInt(3)) {
                0 -> { // addStyle
                    val start =
                        if (reference.textLength == 0) 0
                        else random.nextInt(reference.textLength + 1)
                    val end =
                        if (reference.textLength == start) start
                        else random.nextInt(start, reference.textLength + 1)
                    if (start < end) {
                        val style = random.nextInt(100)
                        val startExpands = random.nextBoolean()
                        val endExpands = random.nextBoolean()

                        val interval = Interval(start, end, startExpands, endExpands)
                        val trackedRange = buffer.addStyle(style, interval)
                        reference.addStyle(style, interval, trackedRange)
                    }
                }
                1 -> { // removeStyle
                    if (reference.items.isNotEmpty()) {
                        val index = random.nextInt(reference.items.size)
                        val range = reference.trackedRanges[index]
                        val removed = buffer.removeStyle(range)
                        assertThat(removed).isTrue()
                        reference.removeAt(index)
                    }
                }
                2 -> { // replaceText
                    val start =
                        if (reference.textLength == 0) 0
                        else random.nextInt(reference.textLength + 1)
                    val end =
                        if (reference.textLength == start) start
                        else random.nextInt(start, reference.textLength + 1)
                    val newLength = random.nextInt(20)

                    buffer.replaceText(start, end, newLength)
                    reference.replaceText(start, end, newLength)

                    // query styles right at the gap to surface any bugs related to gap logic
                    val gapIndex = start + newLength
                    val actual =
                        buffer.getStyles<Int>(gapIndex, gapIndex).map {
                            AnnotatedString.Range(
                                buffer.getItem<Int>(it)!!,
                                buffer.getRange(it).start,
                                buffer.getRange(it).end,
                            )
                        }
                    val expected = reference.getStyles(gapIndex, gapIndex)
                    assertThat(actual).isEqualTo(expected)
                }
            }
            verifyIntegrity(buffer.intervalTree)
            assertThat(buffer.getAllStyles()).isEqualTo(reference.getAllStyles())
        }
    }

    @Test
    fun testTransitions_aroundGap() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("a", Interval(0, 10, false, false))
        buffer.addStyle("b", Interval(20, 30, false, false))

        // Transitions currently at 0, 10, 20, 30
        assertThat(buffer.nextTransition(0, 40)).isEqualTo(10)
        assertThat(buffer.nextTransition(10, 40)).isEqualTo(20)
        assertThat(buffer.nextTransition(20, 40)).isEqualTo(30)

        // Insert 10 characters at index 15
        buffer.replaceText(15, 15, 10)

        // Style "b" is shifted by 10 to [30, 40)
        // Transitions currently at 0, 10, 30, 40
        assertThat(buffer.nextTransition(0, 50)).isEqualTo(10)
        assertThat(buffer.nextTransition(10, 50)).isEqualTo(30)
        assertThat(buffer.nextTransition(30, 50)).isEqualTo(40)

        assertThat(buffer.previousTransition(50, 0)).isEqualTo(40)
        assertThat(buffer.previousTransition(40, 0)).isEqualTo(30)
        assertThat(buffer.previousTransition(30, 0)).isEqualTo(10)
        assertThat(buffer.previousTransition(10, 0)).isEqualTo(0)
    }

    @Test
    fun testTransitions_insideGap() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("a", Interval(0, 10, false, false))

        // Insert text inside the style, splitting it or expanding it depending on edit
        // Here we just insert 10 at index 5. "a" remains at 0, 10. Wait, "a" shifted its end to 20.
        buffer.replaceText(5, 5, 10)

        // gap is now at 15
        // "a" was at [0, 10). It is now [0, 20) because it was inserted inside.
        // Wait, insertion strictly inside extends the end.
        // So transitions are at 0 and 20.
        assertThat(buffer.nextTransition(0, 30)).isEqualTo(20)
        assertThat(buffer.nextTransition(5, 30)).isEqualTo(20)
        assertThat(buffer.previousTransition(30, 0)).isEqualTo(20)
        assertThat(buffer.previousTransition(20, 0)).isEqualTo(0)
    }

    @Test
    fun testTransitions_insideGap_startExpands() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("a", Interval(0, 10, true, false))

        // Insert text strictly at the start index.
        buffer.replaceText(0, 0, 10)

        // Since startExpands = true, the style includes the new text. It is now [0, 20).
        // Transitions are at 0 and 20.
        assertThat(buffer.nextTransition(0, 30)).isEqualTo(20)
        assertThat(buffer.nextTransition(5, 30)).isEqualTo(20)
        assertThat(buffer.previousTransition(30, 0)).isEqualTo(20)
        assertThat(buffer.previousTransition(20, 0)).isEqualTo(0)
    }

    @Test
    fun testTransitions_insideGap_endExpands() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("a", Interval(0, 10, false, true))

        // Insert text strictly at the end index.
        buffer.replaceText(10, 10, 10)

        // Since endExpands = true, the style includes the new text. It is now [0, 20).
        // Transitions are at 0 and 20.
        assertThat(buffer.nextTransition(0, 30)).isEqualTo(20)
        assertThat(buffer.nextTransition(5, 30)).isEqualTo(20)
        assertThat(buffer.previousTransition(30, 0)).isEqualTo(20)
        assertThat(buffer.previousTransition(20, 0)).isEqualTo(0)
    }

    @Test
    fun testTransitions_insideGap_noExpand() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("a", Interval(0, 10, false, false))

        // Insert text strictly at the start index.
        buffer.replaceText(0, 0, 10)

        // Since startExpands = false, the style is shifted. It is now [10, 20).
        // Transitions are at 10 and 20.
        assertThat(buffer.nextTransition(0, 30)).isEqualTo(10)
        assertThat(buffer.nextTransition(10, 30)).isEqualTo(20)
        assertThat(buffer.previousTransition(30, 0)).isEqualTo(20)
        assertThat(buffer.previousTransition(20, 0)).isEqualTo(10)
    }

    @Test
    fun trackedRange_throwsIfUsedOutsideCreator() {
        val buffer1 = TextStyleBuffer<String>()
        val trackedRange = buffer1.addStyle("style", Interval(10, 20))

        val buffer2 = TextStyleBuffer<String>()

        assertThat(buffer2.isValid(trackedRange)).isFalse()
        assertThat(buffer2.removeStyle(trackedRange)).isFalse()

        assertFailsWith<IllegalArgumentException> { buffer2.getRange(trackedRange) }
        assertFailsWith<IllegalArgumentException> {
            buffer2.setRange(trackedRange, TextRange(15, 25))
        }
        assertFailsWith<IllegalArgumentException> { buffer2.getItem<String>(trackedRange) }
        assertFailsWith<IllegalArgumentException> { buffer2.setItem(trackedRange, "new_style") }
        assertFailsWith<IllegalArgumentException> { buffer2.getExpandPolicy(trackedRange) }
        assertFailsWith<IllegalArgumentException> {
            buffer2.setExpandPolicy(trackedRange, ExpandPolicy.AtBoth)
        }
    }

    @Test
    fun trackedRange_getRange() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20))
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(10, 20))
    }

    @Test
    fun trackedRange_setRange() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20))
        buffer.setRange(trackedRange, TextRange(15, 25))
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(15, 25))
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 15, 25))
    }

    @Test
    fun trackedRange_setRange_throwsIfReversedOrCollapsed() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20))
        assertFailsWith<IllegalArgumentException> {
            buffer.setRange(trackedRange, TextRange(15, 10))
        }
        assertFailsWith<IllegalArgumentException> {
            buffer.setRange(trackedRange, TextRange(15, 15))
        }
    }

    @Test
    fun trackedRange_getExpandPolicy() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange1 = buffer.addStyle("style1", Interval(10, 20, false, false))
        assertThat(buffer.getExpandPolicy(trackedRange1)).isEqualTo(ExpandPolicy.InsideOnly)

        val trackedRange2 = buffer.addStyle("style2", Interval(10, 20, true, false))
        assertThat(buffer.getExpandPolicy(trackedRange2)).isEqualTo(ExpandPolicy.AtStart)

        val trackedRange3 = buffer.addStyle("style3", Interval(10, 20, false, true))
        assertThat(buffer.getExpandPolicy(trackedRange3)).isEqualTo(ExpandPolicy.AtEnd)

        val trackedRange4 = buffer.addStyle("style4", Interval(10, 20, true, true))
        assertThat(buffer.getExpandPolicy(trackedRange4)).isEqualTo(ExpandPolicy.AtBoth)
    }

    @Test
    fun trackedRange_setExpandPolicy() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20, false, false))
        buffer.setExpandPolicy(trackedRange, ExpandPolicy.AtBoth)
        assertThat(buffer.getExpandPolicy(trackedRange)).isEqualTo(ExpandPolicy.AtBoth)
    }

    @Test
    fun trackedRange_getItem() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20))
        assertThat(buffer.getItem<String>(trackedRange)).isEqualTo("style")
    }

    @Test
    fun trackedRange_setItem() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20))
        buffer.setItem(trackedRange, "new_style")
        assertThat(buffer.getItem<String>(trackedRange)).isEqualTo("new_style")
        assertThat(buffer.getAllStyles())
            .containsExactly(AnnotatedString.Range("new_style", 10, 20))
    }

    @Test
    fun trackedRange_exists() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20))
        assertThat(buffer.isValid(trackedRange)).isTrue()

        buffer.removeStyle(trackedRange)
        assertThat(buffer.isValid(trackedRange)).isFalse()
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_beforeRange() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20))
        buffer.replaceText(0, 5, 10) // insert 10, delete 5 => net +5
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(15, 25))
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_insideRange() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20))
        buffer.replaceText(12, 18, 10) // insert 10, delete 6 => net +4
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(10, 24))
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_afterRange() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20))
        buffer.replaceText(30, 40, 0)
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(10, 20))
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_removedWhenFullyDeleted() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20))
        buffer.replaceText(5, 25, 0)
        assertThat(buffer.isValid(trackedRange)).isFalse()
        assertFailsWith<IllegalStateException> { buffer.getRange(trackedRange) }
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_expandAtStart() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20, flag1 = true, flag2 = false))
        buffer.replaceText(10, 10, 5) // insert 5 at start
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(10, 25))
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_expandAtEnd() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20, flag1 = false, flag2 = true))
        buffer.replaceText(20, 20, 5) // insert 5 at end
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(10, 25))
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_expandAtBoth() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20, flag1 = true, flag2 = true))
        buffer.replaceText(10, 10, 5) // insert 5 at start
        buffer.replaceText(25, 25, 5) // insert 5 at end
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(10, 30))
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_expandInsideOnly() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20, flag1 = false, flag2 = false))
        buffer.replaceText(10, 10, 5) // insert 5 at start
        buffer.replaceText(25, 25, 5) // insert 5 at end
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(15, 25))
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_withChangedExpandPolicy() {
        val buffer = TextStyleBuffer<String>()
        val trackedRange = buffer.addStyle("style", Interval(10, 20, flag1 = false, flag2 = false))

        buffer.setExpandPolicy(trackedRange, ExpandPolicy.AtBoth)

        buffer.replaceText(10, 10, 5) // insert 5 at start
        buffer.replaceText(25, 25, 5) // insert 5 at end

        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(10, 30))
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_withChangedExpandPolicy_atGap() {
        val buffer = TextStyleBuffer<String>()
        // Move gap to 5
        buffer.replaceText(5, 5, 0)

        // Add a style at [5, 15] where gap is exactly at the start boundary.
        // Initially start expands.
        val trackedRange = buffer.addStyle("style", Interval(5, 15, flag1 = true, flag2 = false))

        // Change policy so start no longer expands.
        buffer.setExpandPolicy(trackedRange, ExpandPolicy.InsideOnly)

        buffer.replaceText(5, 5, 5) // insert 5 at the start boundary

        // Since it's InsideOnly, the inserted text should push the range forward.
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(10, 20))
    }

    @Test
    fun trackedRange_updatesAfterReplaceText_withChangedExpandPolicy_atGap_endBoundary() {
        val buffer = TextStyleBuffer<String>()
        // Move gap to 15
        buffer.replaceText(15, 15, 0)

        // Add a style at [5, 15] where gap is exactly at the end boundary.
        // Initially end expands.
        val trackedRange = buffer.addStyle("style", Interval(5, 15, flag1 = false, flag2 = true))

        // Change policy so end no longer expands.
        buffer.setExpandPolicy(trackedRange, ExpandPolicy.InsideOnly)

        buffer.replaceText(15, 15, 5) // insert 5 at the end boundary

        // Since it's InsideOnly, the inserted text should NOT expand the range.
        assertThat(buffer.getRange(trackedRange)).isEqualTo(TextRange(5, 15))
    }
}

private class ReferenceTextStyleBuffer<T>(initialTextLength: Int) {
    var textLength = initialTextLength
    val items = mutableListOf<T>()
    val intervals = mutableListOf<Interval>()

    /**
     * The [TrackedRange] in the tested [TextStyleBuffer] corresponding to the item in this
     * reference buffer.
     */
    val trackedRanges = mutableListOf<TrackedRange<T>>()

    fun addStyle(style: T, interval: Interval, trackedRange: TrackedRange<T>) {
        items.add(style)
        intervals.add(interval)
        trackedRanges.add(trackedRange)
    }

    fun removeAt(index: Int) {
        items.removeAt(index)
        intervals.removeAt(index)
        trackedRanges.removeAt(index)
    }

    fun getStyles(start: Int, end: Int): List<AnnotatedString.Range<T>> {
        return intervals.mapIndexedNotNull { index, interval ->
            if (interval.overlaps(start, end)) {
                AnnotatedString.Range(items[index], interval.start, interval.end)
            } else {
                null
            }
        }
    }

    fun getAllStyles(): List<AnnotatedString.Range<T>> {
        return intervals.mapIndexed { index, interval ->
            AnnotatedString.Range(items[index], interval.start, interval.end)
        }
    }

    fun replaceText(start: Int, end: Int, newLength: Int) {
        val newIntervals = mutableListOf<Interval>()
        val newItems = mutableListOf<T>()
        val newTrackedRanges = mutableListOf<TrackedRange<T>>()
        for (index in intervals.indices) {
            val interval = intervals[index]
            val offset = end - start

            fun mapIndexAfterDeletion(index: Int): Int {
                return if (index < start) {
                    index
                } else if (index < end) {
                    start
                } else {
                    index - offset
                }
            }

            val startAfterDeletion = mapIndexAfterDeletion(interval.start)
            val endAfterDeletion = mapIndexAfterDeletion(interval.end)

            if (startAfterDeletion < endAfterDeletion) {
                // A replace operation is considered as deleting the range `[start, end)` followed
                // by an insertion at index `start` of `newLength` characters.
                val insertIndex = start
                val newStart =
                    if (startAfterDeletion < insertIndex) {
                        startAfterDeletion
                    } else if (startAfterDeletion == insertIndex && interval.flag1) {
                        insertIndex
                    } else {
                        startAfterDeletion + newLength
                    }

                val newEnd =
                    if (endAfterDeletion < insertIndex) {
                        endAfterDeletion
                    } else if (endAfterDeletion == insertIndex && !interval.flag2) {
                        insertIndex
                    } else {
                        endAfterDeletion + newLength
                    }
                newIntervals.add(Interval(newStart, newEnd, interval.flag1, interval.flag2))
                newItems.add(items[index])
                newTrackedRanges.add(trackedRanges[index])
            }
        }
        intervals.clear()
        intervals.addAll(newIntervals)
        items.clear()
        items.addAll(newItems)
        trackedRanges.clear()
        trackedRanges.addAll(newTrackedRanges)
        textLength += newLength - (end - start)
    }
}
