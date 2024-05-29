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

package androidx.kruth

import com.google.common.collect.ImmutableTable
import com.google.common.collect.Tables.immutableCell
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TableSubjectTest {

    @Test
    fun tableIsEmpty() {
        val table = ImmutableTable.of<String, String, String>()
        assertThat(table).isEmpty()
    }

    @Test
    fun tableIsEmptyWithFailure() {
        val table = ImmutableTable.of(1, 5, 7)
        assertFailsWith<AssertionError> { assertThat(table).isEmpty() }
    }

    @Test
    fun tableIsNotEmpty() {
        val table = ImmutableTable.of(1, 5, 7)
        assertThat(table).isNotEmpty()
    }

    @Test
    fun tableIsNotEmptyWithFailure() {
        val table = ImmutableTable.of<Int, Int, Int>()
        assertFailsWith<AssertionError> { assertThat(table).isNotEmpty() }
    }

    @Test
    fun hasSize() {
        assertThat(ImmutableTable.of(1, 2, 3)).hasSize(1)
    }

    @Test
    fun hasSizeZero() {
        assertThat(ImmutableTable.of<Any, Any, Any>()).hasSize(0)
    }

    @Test
    fun hasSizeNegative() {
        assertFailsWith<IllegalArgumentException> {
            assertThat(ImmutableTable.of(1, 2, 3)).hasSize(-1)
        }
    }

    @Test
    fun contains() {
        val table = ImmutableTable.of("row", "col", "val")
        assertThat(table).contains("row", "col")
    }

    @Test
    fun containsFailure() {
        val table = ImmutableTable.of("row", "col", "val")

        assertFailsWith<AssertionError> { assertThat(table).contains("row", "otherCol") }
    }

    @Test
    fun doesNotContain() {
        val table = ImmutableTable.of("row", "col", "val")
        assertThat(table).doesNotContain("row", "row")
        assertThat(table).doesNotContain("col", "row")
        assertThat(table).doesNotContain("col", "col")
        assertThat(table).doesNotContain(null, null)
    }

    @Test
    fun doesNotContainFailure() {
        val table = ImmutableTable.of("row", "col", "val")
        assertFailsWith<AssertionError> { assertThat(table).doesNotContain("row", "col") }
    }

    @Test
    fun containsCell() {
        val table = ImmutableTable.of("row", "col", "val")
        assertThat(table).containsCell("row", "col", "val")
        assertThat(table).containsCell(immutableCell("row", "col", "val"))
    }

    @Test
    fun containsCellFailure() {
        val table = ImmutableTable.of("row", "col", "val")
        assertFailsWith<AssertionError> { assertThat(table).containsCell("row", "row", "val") }
    }

    @Test
    fun doesNotContainCell() {
        val table = ImmutableTable.of("row", "col", "val")
        assertThat(table).doesNotContainCell("row", "row", "val")
        assertThat(table).doesNotContainCell("col", "row", "val")
        assertThat(table).doesNotContainCell("col", "col", "val")
        assertThat(table).doesNotContainCell(null, null, null)
        assertThat(table).doesNotContainCell(immutableCell("row", "row", "val"))
        assertThat(table).doesNotContainCell(immutableCell("col", "row", "val"))
        assertThat(table).doesNotContainCell(immutableCell("col", "col", "val"))
        assertThat(table).doesNotContainCell(immutableCell(null, null, null))
    }

    @Test
    fun doesNotContainCellFailure() {
        val table = ImmutableTable.of("row", "col", "val")
        assertFailsWith<AssertionError> {
            assertThat(table).doesNotContainCell("row", "col", "val")
        }
    }
}
