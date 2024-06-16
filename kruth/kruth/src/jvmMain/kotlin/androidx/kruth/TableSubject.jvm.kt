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

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.simpleFact
import com.google.common.collect.Table
import com.google.common.collect.Table.Cell
import com.google.common.collect.Tables.immutableCell

class TableSubject<R, C, V>
internal constructor(
    actual: Table<R, C, V>,
    metadata: FailureMetadata = FailureMetadata(),
) : Subject<Table<R, C, V>>(actual, metadata, typeDescriptionOverride = null) {

    /** Fails if the table is not empty. */
    fun isEmpty() {
        requireNonNull(actual)

        if (!actual.isEmpty) {
            failWithActual(simpleFact("expected to be empty"))
        }
    }

    /** Fails if the table is empty. */
    fun isNotEmpty() {
        requireNonNull(actual)

        if (actual.isEmpty) {
            failWithoutActual(simpleFact("expected not to be empty"))
        }
    }

    /** Fails if the table does not have the given size. */
    fun hasSize(expectedSize: Int) {
        require(expectedSize >= 0) { "expectedSize($expectedSize) must be >= 0" }
        requireNonNull(actual)

        check("size()").that(actual.size()).isEqualTo(expectedSize)
    }

    /** Fails if the table does not contain a mapping for the given row key and column key. */
    fun contains(rowKey: R, columnKey: C) {
        requireNonNull(actual)

        if (!actual.contains(rowKey, columnKey)) {
            failWithActual(
                simpleFact("expected to contain mapping for row-column key pair"),
                fact("row key", rowKey),
                fact("column key", columnKey),
            )
        }
    }

    /** Fails if the table contains a mapping for the given row key and column key. */
    fun doesNotContain(rowKey: R, columnKey: C) {
        requireNonNull(actual)

        if (actual.contains(rowKey, columnKey)) {
            failWithoutActual(
                simpleFact("expected not to contain mapping for row-column key pair"),
                fact("row key", rowKey),
                fact("column key", columnKey),
                fact("but contained value", actual[rowKey, columnKey]),
                fact("full contents", actual),
            )
        }
    }

    /** Fails if the table does not contain the given cell. */
    fun containsCell(rowKey: R, colKey: C, value: V) {
        containsCell(immutableCell(rowKey, colKey, value))
    }

    /** Fails if the table does not contain the given cell. */
    fun containsCell(cell: Cell<R, C, V>?) {
        requireNonNull(cell)
        requireNonNull(actual)

        checkNoNeedToDisplayBothValues("cellSet()").that(actual.cellSet()).contains(cell)
    }

    /** Fails if the table contains the given cell. */
    fun doesNotContainCell(rowKey: R, colKey: C, value: V) {
        doesNotContainCell(immutableCell(rowKey, colKey, value))
    }

    /** Fails if the table contains the given cell. */
    fun doesNotContainCell(cell: Cell<R, C, V>?) {
        requireNonNull(cell)
        requireNonNull(actual)

        checkNoNeedToDisplayBothValues("cellSet()").that(actual.cellSet()).doesNotContain(cell)
    }

    /** Fails if the table does not contain the given row key. */
    fun containsRow(rowKey: R) {
        requireNonNull(actual)

        check("rowKeySet()").that(actual.rowKeySet()).contains(rowKey)
    }

    /** Fails if the table does not contain the given column key. */
    fun containsColumn(columnKey: C) {
        requireNonNull(actual)

        check("columnKeySet()").that(actual.columnKeySet()).contains(columnKey)
    }

    /** Fails if the table does not contain the given value. */
    fun containsValue(value: V) {
        requireNonNull(actual)

        check("values()").that(actual.values()).contains(value)
    }
}
