/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto.server

import androidx.annotation.RestrictTo
import okio.ByteString
import perfetto.protos.QueryResult

/**
 * Wrapper class around [QueryResult] returned after executing a query on perfetto. The parsing
 * logic is copied from the python project:
 * https://github.com/google/perfetto/blob/master/python/perfetto/trace_processor/api.py#L89
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // for internal benchmarking only
class QueryResultIterator internal constructor(queryResult: QueryResult) :
    Iterator<Map<String, Any?>> {

    private val dataLists = object {
        val stringBatches = mutableListOf<String>()
        val varIntBatches = mutableListOf<Long>()
        val float64Batches = mutableListOf<Double>()
        val blobBatches = mutableListOf<ByteString>()

        var stringIndex = 0
        var varIntIndex = 0
        var float64Index = 0
        var blobIndex = 0
    }

    private val cells = mutableListOf<QueryResult.CellsBatch.CellType>()
    private val columnNames = queryResult.column_names
    private val columnCount: Int
    private val count: Int

    private var currentIndex = 0

    init {

        // Parsing every batch
        for (batch in queryResult.batch) {
            val stringsBatch = batch.string_cells!!.split(0x00.toChar()).dropLast(1)
            dataLists.stringBatches.addAll(stringsBatch)
            dataLists.varIntBatches.addAll(batch.varint_cells)
            dataLists.float64Batches.addAll(batch.float64_cells)
            dataLists.blobBatches.addAll(batch.blob_cells)
            cells.addAll(batch.cells)
        }

        columnCount = columnNames.size
        count = if (columnCount > 0) cells.size / columnCount else 0
    }

    /**
     * Returns the number of rows in the query result.
     */
    fun size(): Int {
        return count
    }

    /**
     * Returns true whether there are no results stored in this iterator, false otherwise.
     */
    fun isEmpty(): Boolean {
        return count == 0
    }

    override fun hasNext(): Boolean {
        return currentIndex < count
    }

    /**
     * Returns a map containing the next row of the query results.
     *
     * @throws IllegalArgumentException if the query returns an invalid cell type
     * @throws NoSuchElementException if the query has no next row.
     */
    override fun next(): Map<String, Any?> {
        if (!hasNext()) throw NoSuchElementException()

        val row = mutableMapOf<String, Any?>()
        val baseCellIndex = currentIndex * columnCount

        for ((num, columnName) in columnNames.withIndex()) {
            val colType = cells[baseCellIndex + num]
            val colIndex: Int
            row[columnName] = when (colType) {
                QueryResult.CellsBatch.CellType.CELL_STRING -> {
                    colIndex = dataLists.stringIndex
                    dataLists.stringIndex += 1
                    dataLists.stringBatches[colIndex]
                }
                QueryResult.CellsBatch.CellType.CELL_VARINT -> {
                    colIndex = dataLists.varIntIndex
                    dataLists.varIntIndex += 1
                    dataLists.varIntBatches[colIndex]
                }
                QueryResult.CellsBatch.CellType.CELL_FLOAT64 -> {
                    colIndex = dataLists.float64Index
                    dataLists.float64Index += 1
                    dataLists.float64Batches[colIndex]
                }
                QueryResult.CellsBatch.CellType.CELL_BLOB -> {
                    colIndex = dataLists.blobIndex
                    dataLists.blobIndex += 1
                    dataLists.blobBatches[colIndex]
                }
                QueryResult.CellsBatch.CellType.CELL_INVALID ->
                    throw IllegalArgumentException("Invalid cell type")
                QueryResult.CellsBatch.CellType.CELL_NULL ->
                    null
            }
        }

        currentIndex += 1
        return row
    }

    /**
     * Converts this iterator to a list of [T] using the given mapping function.
     * Note that this method is provided for convenience and exhausts the iterator.
     */
    fun <T> toList(mapFunc: (Map<String, Any?>) -> (T)): List<T> {
        return this.asSequence().map(mapFunc).toList()
    }
}
