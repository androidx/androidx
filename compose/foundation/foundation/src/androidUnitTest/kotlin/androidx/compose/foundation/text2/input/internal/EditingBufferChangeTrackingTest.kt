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
class EditingBufferChangeTrackingTest {

    @Test
    fun normalReplaceOperation_reportedAsReplace() {
        val eb = EditingBuffer("abcde", TextRange.Zero)

        eb.replace(2, 4, "bfghi")

        assertThat(eb.changeTracker.changeCount).isEqualTo(1)
        assertThat(eb.changeTracker.getOriginalRange(0)).isEqualTo(TextRange(2, 4))
        assertThat(eb.changeTracker.getRange(0)).isEqualTo(TextRange(2, 7))
    }

    @Test
    fun tailInsertionReportedAsReplace_coercesToInsertion() {
        val eb = EditingBuffer("abcd", TextRange.Zero)

        eb.replace(2, 4, "cde")

        assertThat(eb.changeTracker.changeCount).isEqualTo(1)
        assertThat(eb.changeTracker.getOriginalRange(0)).isEqualTo(TextRange(4))
        assertThat(eb.changeTracker.getRange(0)).isEqualTo(TextRange(4, 5))
    }

    @Test
    fun headInsertionReportedAsReplace_coercesToInsertion() {
        val eb = EditingBuffer("abcd", TextRange.Zero)

        eb.replace(0, 4, "eabcd")

        assertThat(eb.changeTracker.changeCount).isEqualTo(1)
        assertThat(eb.changeTracker.getOriginalRange(0)).isEqualTo(TextRange(0))
        assertThat(eb.changeTracker.getRange(0)).isEqualTo(TextRange(0, 1))
    }

    @Test
    fun tailInsertionInTheMiddle_reportedAsReplace_coercesToInsertion() {
        val eb = EditingBuffer("abcde", TextRange.Zero)

        eb.replace(1, 3, "bcef")

        assertThat(eb.changeTracker.changeCount).isEqualTo(1)
        assertThat(eb.changeTracker.getOriginalRange(0)).isEqualTo(TextRange(3))
        assertThat(eb.changeTracker.getRange(0)).isEqualTo(TextRange(3, 5))
    }

    @Test
    fun headInsertionInTheMiddle_reportedAsReplace_coercesToInsertion() {
        val eb = EditingBuffer("abcde", TextRange.Zero)

        eb.replace(2, 4, "fgcd")

        assertThat(eb.changeTracker.changeCount).isEqualTo(1)
        assertThat(eb.changeTracker.getOriginalRange(0)).isEqualTo(TextRange(2))
        assertThat(eb.changeTracker.getRange(0)).isEqualTo(TextRange(2, 4))
    }

    @Test
    fun tailDeletionReportedAsReplace_coercesToDeletion() {
        val eb = EditingBuffer("abcde", TextRange.Zero)

        eb.replace(0, 5, "abcd")

        assertThat(eb.changeTracker.changeCount).isEqualTo(1)
        assertThat(eb.changeTracker.getOriginalRange(0)).isEqualTo(TextRange(4, 5))
        assertThat(eb.changeTracker.getRange(0)).isEqualTo(TextRange(4))
    }

    @Test
    fun headDeletionReportedAsReplace_coercesToDeletion() {
        val eb = EditingBuffer("abcde", TextRange.Zero)

        eb.replace(0, 5, "bcde")

        assertThat(eb.changeTracker.changeCount).isEqualTo(1)
        assertThat(eb.changeTracker.getOriginalRange(0)).isEqualTo(TextRange(0, 1))
        assertThat(eb.changeTracker.getRange(0)).isEqualTo(TextRange(0))
    }

    @Test
    fun tailDeletionInTheMiddle_reportedAsReplace_coercesToDeletion() {
        val eb = EditingBuffer("abcde", TextRange.Zero)

        eb.replace(1, 4, "b")

        assertThat(eb.changeTracker.changeCount).isEqualTo(1)
        assertThat(eb.changeTracker.getOriginalRange(0)).isEqualTo(TextRange(2, 4))
        assertThat(eb.changeTracker.getRange(0)).isEqualTo(TextRange(2))
    }

    @Test
    fun headDeletionInTheMiddle_reportedAsReplace_coercesToDeletion() {
        val eb = EditingBuffer("abcde", TextRange.Zero)

        eb.replace(1, 4, "d")

        assertThat(eb.changeTracker.changeCount).isEqualTo(1)
        assertThat(eb.changeTracker.getOriginalRange(0)).isEqualTo(TextRange(1, 3))
        assertThat(eb.changeTracker.getRange(0)).isEqualTo(TextRange(1))
    }
}
