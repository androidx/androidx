/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class SetSelectionCommandTest : ImeEditCommandTest() {

    @Test
    fun test_set() {
        initialize("ABCDE", TextRange.Zero)

        imeScope.setSelection(1, 4)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(1)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_preserve_ongoing_composition() {
        initialize("ABCDE", TextRange.Zero)

        imeScope.setComposingRegion(1, 3)

        imeScope.setSelection(2, 4)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition).isNotNull()
        assertThat(state.composition?.start).isEqualTo(1)
        assertThat(state.composition?.end).isEqualTo(3)
    }

    @Test
    fun test_cancel_ongoing_selection() {
        initialize("ABCDE", TextRange(1, 4))

        imeScope.setSelection(2, 5)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(2)
        assertThat(state.selection.end).isEqualTo(5)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_set_reversed() {
        initialize("ABCDE", TextRange.Zero)

        imeScope.setSelection(4, 1)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(4)
        assertThat(state.selection.end).isEqualTo(1)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_set_too_small() {
        initialize("ABCDE", TextRange.Zero)

        imeScope.setSelection(-1000, -1000)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_set_too_large() {
        initialize("ABCDE", TextRange.Zero)

        imeScope.setSelection(1000, 1000)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(5)
        assertThat(state.selection.end).isEqualTo(5)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_set_too_too_large() {
        initialize("ABCDE", TextRange.Zero)

        imeScope.setSelection(0, 1000)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(5)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_set_too_large_reversed() {
        initialize("ABCDE", TextRange.Zero)

        imeScope.setSelection(1000, 0)

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(5)
        assertThat(state.selection.end).isEqualTo(0)
        assertThat(state.composition).isNull()
    }
}
