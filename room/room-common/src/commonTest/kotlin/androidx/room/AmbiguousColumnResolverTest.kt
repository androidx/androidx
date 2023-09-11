/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room

import androidx.kruth.assertThat
import java.util.Locale
import org.junit.Ignore
import org.junit.Test

class AmbiguousColumnResolverTest {

    @Test
    fun simple() {
        // query: SELECT * FROM T1 JOIN T2
        // return: Map<T1, T2>
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "A", "C", "D"),
            arrayOf(
                arrayOf("A", "B"),
                arrayOf("A", "C", "D")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1),
                intArrayOf(2, 3, 4),
            )
        )
    }

    @Test
    fun simple_pojoSwapped() {
        // query: SELECT * FROM T1 JOIN T2
        // return: Map<T2, T1>
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "A", "C", "D"),
            arrayOf(
                arrayOf("A", "C", "D"),
                arrayOf("A", "B")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(2, 3, 4),
                intArrayOf(0, 1),
            )
        )
    }

    @Test
    @Ignore("Algorithm can't solve this as expected.")
    fun simple_oddResultOrder() {
        // query: SELECT User.id, Comment.id, userId, text, name FROM User JOIN Comment
        // return: Map<User, Comment>
        // The result columns are manually ordered and the algorithms relies on all columns from
        // a mapping being neighbors before or after the next columns of another mapping. This can
        // only happen in a non-star projected query.
        // TODO: We might be able to solve this if we order result columns by origin table.
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "A", "B", "C", "D"),
            arrayOf(
                arrayOf("A", "C", "D"),
                arrayOf("A", "B")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(2, 3, 4),
                intArrayOf(0, 1),
            )
        )
    }

    @Test
    fun dupeColumnMigrated_one() {
        // Star projection query where 'A' (a dupe column) was added via migration
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "C", "D", "A"),
            arrayOf(
                arrayOf("A", "B"),
                arrayOf("A", "C", "D")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1),
                intArrayOf(4, 2, 3),
            )
        )
    }

    @Test
    fun dupeColumnMigrated_both() {
        // Star projection query where both dupe columns 'A' were added via migration
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("B", "A", "C", "D", "A"),
            arrayOf(
                arrayOf("A", "B"),
                arrayOf("A", "C", "D")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(1, 0),
                intArrayOf(4, 2, 3),
            )
        )
    }

    @Test
    fun multiple_duplicates() {
        // Mapping multiple dupe columns ('A' and 'C')
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "C", "A", "C", "D"),
            arrayOf(
                arrayOf("A", "B", "C"),
                arrayOf("A", "C", "D")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1, 2),
                intArrayOf(3, 4, 5),
            )
        )
    }

    @Test
    fun multiple_duplicates_noUnique() {
        // Mapping multiple dupe columns and one of the tables have no unique column, i.e. in the
        // result they are all dupes.
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "C", "A", "B"),
            arrayOf(
                arrayOf("A", "B", "C"),
                arrayOf("A", "B")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1, 2),
                intArrayOf(3, 4),
            )
        )
    }

    @Test
    @Ignore("Algorithm can't solve this as expected.")
    fun multiple_duplicates_noUnique_swapped() {
        // Mapping multiple dupe columns and one of the tables have no unique column, i.e. in the
        // result they are all dupes. However, the order of mappings given to the algorithm is
        // changed and due to one of the mapping have no unique columns it is indistinguishable
        // from a subset of the other mapping.
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "C", "A", "B"),
            arrayOf(
                arrayOf("A", "B"),
                arrayOf("A", "B", "C"),
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(3, 4),
                intArrayOf(0, 1, 2),
            )
        )
    }

    @Test
    fun extraResultColumns() {
        // Extra results columns are ignored
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "C", "A", "D", "E", "F"),
            arrayOf(
                arrayOf("A", "B"),
                arrayOf("A", "D", "E")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1),
                intArrayOf(3, 4, 5),
            )
        )
    }

    @Test
    fun extraResultColumns_withGap() {
        // Extra results columns, including causing gaps between POJO columns are ignored
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "X", "B", "C", "A", "D", "E", "F", "B"),
            arrayOf(
                arrayOf("A", "B"),
                arrayOf("A", "B", "D", "E")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 2),
                intArrayOf(4, 8, 5, 6),
            )
        )
    }

    @Test
    fun firstChoice() {
        // When resolving a single solo duplicate column, the algorithm will choose the first one
        // it finds... see firstChoice_swapped() to show why this is an issue.
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "A", "B"),
            arrayOf(
                arrayOf("A"),
                arrayOf("A", "B")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0),
                intArrayOf(1, 2),
            )
        )
    }

    @Test
    fun firstChoice_resultOrderSwapped() {
        // Not what we want, but its likely that in practice either the columns will
        // be aliased, or A at 0 is the same as A at 2 due to being a JOIN column.
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "A"),
            arrayOf(
                arrayOf("A"),
                arrayOf("A", "B")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0),
                intArrayOf(2, 1),
            )
        )
    }

    @Test
    fun firstChoice_bothSolo() {
        // With the current information this is impossible to resolve, it'll be a first found
        // solution just like Cursor.getColumnIndex().
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "A"),
            arrayOf(
                arrayOf("A"),
                arrayOf("A")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0),
                intArrayOf(1),
            )
        )
    }

    @Test
    fun dupesInMapping() {
        // This input shouldn't happen since a single POJO (even with embedded) is not allowed
        // to have duplicate columns.
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "A", "C", "D"),
            arrayOf(
                arrayOf("A", "B", "A"),
                arrayOf("A", "C", "D")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1, 0),
                intArrayOf(2, 3, 4),
            )
        )
    }

    @Test
    fun repeatedColumn() {
        // Both POJOs map the same result (non dupe) column
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "C", "D"),
            arrayOf(
                arrayOf("A", "B"),
                arrayOf("A", "C", "D")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1),
                intArrayOf(0, 2, 3),
            )
        )
    }

    @Test
    fun repeatedColumn_firstChoice() {
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "C"),
            arrayOf(
                arrayOf("B"),
                arrayOf("A", "B", "C")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(1),
                intArrayOf(0, 1, 2),
            )
        )
    }

    @Test
    fun repeatedColumn_withDuplicate() {
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "C", "B"),
            arrayOf(
                arrayOf("A", "B", "C"),
                arrayOf("C", "B"),
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1, 2),
                intArrayOf(2, 3),
            )
        )
    }

    @Test
    fun repeatedColumn_withDuplicate_pojoSwapped() {
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "C", "B"),
            arrayOf(
                arrayOf("C", "B"),
                arrayOf("A", "B", "C"),
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(2, 3),
                intArrayOf(0, 1, 2),
            )
        )
    }

    @Test
    @Ignore("Algorithm can't solve this as expected.")
    fun repeatedColumn_withDuplicate_withGap() {
        // The algorithm finds two solutions but both have the same cost.
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("A", "B", "C", "B", "D"),
            arrayOf(
                arrayOf("A", "B", "C"),
                arrayOf("A", "B", "D"),
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1, 2),
                intArrayOf(0, 3, 4),
            )
        )
    }

    @Test
    fun case_insensitive() {
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("a", "B", "A", "c", "D"),
            arrayOf(
                arrayOf("A", "b"),
                arrayOf("a", "C", "d")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1),
                intArrayOf(2, 3, 4),
            )
        )
    }

    @Test
    fun case_insensitive_tr() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr")) // Turkish has special upper/lowercase i chars
            val result = AmbiguousColumnResolver.resolve(
                arrayOf("i̇", "B", "İ", "C", "D"),
                arrayOf(
                    arrayOf("İ", "b"),
                    arrayOf("i̇", "C", "d")
                )
            )
            assertThat(result).isEqualTo(
                arrayOf(
                    intArrayOf(0, 1),
                    intArrayOf(2, 3, 4),
                )
            )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun case_backticks() {
        val result = AmbiguousColumnResolver.resolve(
            arrayOf("`A`", "B", "A", "`C`", "D"),
            arrayOf(
                arrayOf("A", "B"),
                arrayOf("A", "C", "D")
            )
        )
        assertThat(result).isEqualTo(
            arrayOf(
                intArrayOf(0, 1),
                intArrayOf(2, 3, 4),
            )
        )
    }
}
