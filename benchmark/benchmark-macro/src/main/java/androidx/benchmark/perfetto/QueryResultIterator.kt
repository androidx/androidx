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

package androidx.benchmark.perfetto

import perfetto.protos.QueryResult

/** Iterator for results from a [PerfettoTraceProcessor] query. */
internal class QueryResultIterator constructor(queryResult: QueryResult) : Iterator<Row> {
    private val dataLists =
        object {
            val stringBatches = mutableListOf<String>()
            val varIntBatches = mutableListOf<Long>()
            val float64Batches = mutableListOf<Double>()
            val blobBatches = mutableListOf<ByteArray>()

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
            dataLists.blobBatches.addAll(batch.blob_cells.map { it.toByteArray() })
            cells.addAll(batch.cells)
        }

        columnCount = columnNames.size
        count = if (columnCount > 0) cells.size / columnCount else 0
    }

    /** Returns the number of rows in the query result. */
    fun size(): Int {
        return count
    }

    /** Returns true whether there are no results stored in this iterator, false otherwise. */
    fun isEmpty(): Boolean {
        return count == 0
    }

    /** Returns true if there are more rows not yet parsed from the query result. */
    override fun hasNext(): Boolean {
        return currentIndex < count
    }

    /**
     * Returns a map containing the next row of the query results.
     *
     * @throws IllegalArgumentException if the query returns an invalid cell type
     * @throws NoSuchElementException if the query has no next row.
     */
    override fun next(): Row {
        // Parsing logic is copied from the python project:
        // https://github.com/google/perfetto/blob/master/python/perfetto/trace_processor/api.py#L89

        if (!hasNext()) throw NoSuchElementException()

        val row = mutableMapOf<String, Any?>()
        val baseCellIndex = currentIndex * columnCount

        for ((num, columnName) in columnNames.withIndex()) {
            val colType = cells[baseCellIndex + num]
            val colIndex: Int
            row[columnName] =
                when (colType) {
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
                    QueryResult.CellsBatch.CellType.CELL_NULL -> null
                }
        }

        currentIndex += 1
        return Row(row)
    }
}
