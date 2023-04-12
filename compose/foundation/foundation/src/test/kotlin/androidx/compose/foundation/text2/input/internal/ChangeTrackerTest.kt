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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ChangeTrackerTest {

    @Test
    fun initialInsert() {
        val buffer = SimpleBuffer()

        buffer.append("hello")

        assertThat(buffer.toString()).isEqualTo("hello")
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 5))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0))
    }

    @Test
    fun deleteAll() {
        val buffer = SimpleBuffer("hello")

        buffer.replace("hello", "")

        assertThat(buffer.toString()).isEqualTo("")
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 0))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0, 5))
    }

    @Test
    fun multipleDiscontinuousChanges() {
        val buffer = SimpleBuffer("hello world")

        buffer.replace("world", "Compose")
        buffer.replace("hello", "goodbye")

        assertThat(buffer.toString()).isEqualTo("goodbye Compose")
        assertThat(buffer.changes.changeCount).isEqualTo(2)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 7))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0, 5))
        assertThat(buffer.changes.getRange(1)).isEqualTo(TextRange(8, 15))
        assertThat(buffer.changes.getOriginalRange(1)).isEqualTo(TextRange(6, 11))
    }

    @Test
    fun twoAppends() {
        val buffer = SimpleBuffer()

        buffer.append("foo")
        buffer.append("bar")

        assertThat(buffer.toString()).isEqualTo("foobar")
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 6))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0))
    }

    @Test
    fun threeAppends() {
        val buffer = SimpleBuffer()

        buffer.append("foo")
        buffer.append("bar")
        buffer.append("baz")

        assertThat(buffer.toString()).isEqualTo("foobarbaz")
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 9))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0))
    }

    @Test
    fun multipleAdjacentReplaces_whenPerformedInOrder_replacementsShorter() {
        val buffer = SimpleBuffer("abcd")

        buffer.replace("ab", "e") // ecd
        buffer.replace("cd", "f")

        assertThat(buffer.toString()).isEqualTo("ef")
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 2))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0, 4))
    }

    @Test
    fun multipleAdjacentReplaces_whenPerformedInOrder_replacementsLonger() {
        val buffer = SimpleBuffer("abcd")

        buffer.replace("ab", "efg") // efgcd
        buffer.replace("cd", "hij")

        assertThat(buffer.toString()).isEqualTo("efghij")
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 6))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0, 4))
    }

    @Test
    fun multipleAdjacentReplaces_whenPerformedInReverseOrder_replacementsShorter() {
        val buffer = SimpleBuffer("abcd")

        buffer.replace("cd", "f") // abf
        buffer.replace("ab", "e")

        assertThat(buffer.toString()).isEqualTo("ef")
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 2))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0, 4))
    }

    @Test
    fun multipleAdjacentReplaces_whenPerformedInReverseOrder_replacementsLonger() {
        val buffer = SimpleBuffer("abcd")

        buffer.replace("cd", "efg") // abhij
        buffer.replace("ab", "hij")

        assertThat(buffer.toString()).isEqualTo("hijefg")
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 6))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0, 4))
    }

    @Test
    fun multiplePartiallyOverlappingChanges_atStart() {
        val buffer = SimpleBuffer("abcd")

        buffer.replace("bc", "ef") // aefd
        buffer.replace("ae", "gh")

        assertThat(buffer.toString()).isEqualTo("ghfd")
        // Overlapping changes are merged.
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(0, 3))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(0, 3))
    }

    @Test
    fun multiplePartiallyOverlappingChanges_atEnd() {
        val buffer = SimpleBuffer("abcd")

        buffer.replace("bc", "ef") // aefd
        buffer.replace("fd", "gh")

        assertThat(buffer.toString()).isEqualTo("aegh")
        // Overlapping changes are merged.
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(1, 4))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(1, 4))
    }

    @Test
    fun multipleFullyOverlappingChanges() {
        val buffer = SimpleBuffer("abcd")

        buffer.replace("bc", "ef") // aefd
        buffer.replace("ef", "gh")

        assertThat(buffer.toString()).isEqualTo("aghd")
        // Overlapping changes are merged.
        assertThat(buffer.changes.changeCount).isEqualTo(1)
        assertThat(buffer.changes.getRange(0)).isEqualTo(TextRange(1, 3))
        assertThat(buffer.changes.getOriginalRange(0)).isEqualTo(TextRange(1, 3))
    }

    private class SimpleBuffer(initialText: String = "") {
        private val builder = StringBuilder(initialText)
        val changes = ChangeTracker()

        fun append(text: String) {
            changes.trackChange(TextRange(builder.length), text.length)
            builder.append(text)
        }

        fun replace(substring: String, text: String) {
            val start = builder.indexOf(substring)
            if (start != -1) {
                val end = start + substring.length
                changes.trackChange(TextRange(start, end), text.length)
                builder.replace(start, end, text)
            }
        }

        override fun toString(): String = builder.toString()
    }
}