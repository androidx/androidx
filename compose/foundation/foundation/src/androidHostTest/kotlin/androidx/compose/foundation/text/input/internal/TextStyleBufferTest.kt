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

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextStyleBufferTest {

    @Test
    fun basicAddAndGet() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style1", 0, 10)
        buffer.addStyle("style2", 5, 15)

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
        buffer.addStyle("a", 0, 10)
        buffer.removeStyle("a", 0, 10)
        assertThat(buffer.getAllStyles()).isEmpty()
    }

    @Test
    fun orderIsPreserved() {
        val buffer = TextStyleBuffer<Int>()
        buffer.addStyle(1, 10, 20)
        buffer.addStyle(2, 0, 30)
        buffer.addStyle(3, 5, 15)

        // Verifies that styles are returned in insertion order, regardless of their ranges.
        assertThat(buffer.getStyles(0, 30).map { it.item }).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun equalsAndHashCode() {
        val buffer1 =
            TextStyleBuffer<String>().apply {
                addStyle("a", 0, 10)
                addStyle("b", 10, 20)
            }
        val buffer2 =
            TextStyleBuffer<String>().apply {
                addStyle("a", 0, 10)
                addStyle("b", 10, 20)
            }
        val buffer3 =
            TextStyleBuffer<String>().apply {
                addStyle("b", 10, 20)
                addStyle("a", 0, 10)
            }

        assertThat(buffer1).isEqualTo(buffer2)
        assertThat(buffer1.hashCode()).isEqualTo(buffer2.hashCode())

        // Order matters for equality because it affects the rendered result.
        assertThat(buffer1).isNotEqualTo(buffer3)
    }

    @Test
    fun copy_isEqualAndIndependent() {
        val original = TextStyleBuffer<String>().apply { addStyle("a", 0, 10) }

        val copy = TextStyleBuffer(original)
        assertThat(copy).isEqualTo(original)

        copy.addStyle("b", 10, 20)
        assertThat(copy).isNotEqualTo(original)
        assertThat(original.getAllStyles()).hasSize(1)
        assertThat(copy.getAllStyles()).hasSize(2)
    }

    @Test
    fun clear() {
        val buffer = TextStyleBuffer<String>().apply { addStyle("a", 0, 10) }
        buffer.clear()
        assertThat(buffer.getAllStyles()).isEmpty()
    }

    @Test
    fun replaceText_noOverlap() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", 10, 20)

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
        buffer.addStyle("style", 10, 20)

        // Replace [8, 12) with length 4. (Net 0)
        // [8, 12) deleted. Style start 10 was inside, moves to 12.
        buffer.replaceText(8, 12, 4)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 12, 20))
    }

    @Test
    fun replaceText_overlappingEnd() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", 10, 20)

        // Replace [18, 22) with length 4. (Net 0)
        // [18, 22) deleted. Style end 20 was inside, moves to 18 (start of deletion).
        // 4 characters inserted at 18, style end is extended to 22.
        buffer.replaceText(18, 22, 4)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 10, 22))
    }

    @Test
    fun replaceText_fullyContained() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", 10, 20)

        // Replace [12, 18) with length 10. (Net +4)
        buffer.replaceText(12, 18, 10)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 10, 24))
    }

    @Test
    fun replaceText_fullyCovering() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", 10, 20)

        // Replace [5, 25) with length 5.
        // Style is fully within [5, 25), so it should be removed.
        buffer.replaceText(5, 25, 5)
        assertThat(buffer.getAllStyles()).isEmpty()
    }

    @Test
    fun replaceText_exactlyCovering() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", 10, 20)

        buffer.replaceText(10, 20, 5)
        assertThat(buffer.getAllStyles()).isEmpty()
    }

    @Test
    fun replaceText_boundaryBehavior() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("style", 10, 20)

        // Insert at 10. (Exclusive at start)
        buffer.replaceText(10, 10, 5)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 15, 25))

        // Insert at 25 (original 20). (Inclusive at end)
        buffer.replaceText(25, 25, 5)
        assertThat(buffer.getAllStyles()).containsExactly(AnnotatedString.Range("style", 15, 30))
    }

    @Test
    fun replaceText_multipleStyles() {
        val buffer = TextStyleBuffer<String>()
        buffer.addStyle("a", 10, 20)
        buffer.addStyle("b", 15, 25)

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
                        buffer.addStyle(style, start, end)
                        reference.addStyle(style, start, end)
                    }
                }
                1 -> { // removeStyle
                    if (reference.styles.isNotEmpty()) {
                        val index = random.nextInt(reference.styles.size)
                        val range = reference.styles[index]

                        val removed = buffer.removeStyle(range.item, range.start, range.end)
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
                    val actual = buffer.getStyles(gapIndex, gapIndex)
                    val expected = reference.getStyles(gapIndex, gapIndex)
                    assertThat(actual).isEqualTo(expected)
                }
            }
            assertThat(buffer.getAllStyles()).isEqualTo(reference.styles)
        }
    }
}

private class ReferenceTextStyleBuffer<T>(initialTextLength: Int) {
    var textLength = initialTextLength
    val styles = mutableListOf<AnnotatedString.Range<T>>()

    fun addStyle(style: T, start: Int, end: Int) {
        styles.add(AnnotatedString.Range(style, start, end))
    }

    fun removeAt(index: Int) {
        styles.removeAt(index)
    }

    fun getStyles(start: Int, end: Int): List<AnnotatedString.Range<T>> {
        return styles.filter { range -> intersect(start, end, range.start, range.end) }
    }

    fun replaceText(start: Int, end: Int, newLength: Int) {
        val newStyles = mutableListOf<AnnotatedString.Range<T>>()
        for (range in styles) {
            val newStart =
                if (range.start < start) {
                    range.start
                } else if (range.start < end) {
                    start + newLength
                } else {
                    range.start - (end - start) + newLength
                }
            val newEnd =
                if (range.end < start) {
                    range.end
                } else if (range.end < end) {
                    start + newLength
                } else {
                    range.end - (end - start) + newLength
                }

            if (newStart < newEnd) {
                newStyles.add(AnnotatedString.Range(range.item, newStart, newEnd))
            }
        }
        styles.clear()
        styles.addAll(newStyles)
        textLength += newLength - (end - start)
    }
}
