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

package androidx.room

import androidx.annotation.RestrictTo
import java.util.Locale

/**
 * Utility class for resolving and mapping ambiguous columns from a query result.
 *
 * Given an ordered list containing the result columns of a query along with a collection containing
 * the set of columns per object, the algorithm will return a new collection containing the indices
 * of the result columns of each object column.
 *
 * The algorithm works as follow:
 * 1. The input is normalized by making all result column names and mapping column names lowercase.
 * SQLite is case insensitive.
 * 2. The result columns might contain columns who are not part of any object, these are ignored by
 * creating a new list containing only useful columns, the original indices are preserved and used
 * in the solution.
 * 3.a Next, we find the range of continuous indices where each mapping can be found in the useful
 * result columns list. The order in which the columns are found is not important as long as they
 * are continuous, this accounts for table migrations. The Rabin-Karp algorithm is used for the
 * search, since it has good performance. The has cumulative hash function is simply the sum of
 * the hash codes of each column name.
 * 3.b It is expected to find at least one range for each mapping, if none is found via Rabin-Karp
 * then we fallback to a depth first search approach, which is slower but removes the requirement
 * that the columns must be found continuously.
 * 4. With various possible matches found for each mapping, the last step is to find the 'best'
 * solution, the algorithm is heuristic. We go through each combination of matched indices ranges
 * using a depth first traversal, comparing a solution made up of such combination with whatever
 * is the current best. The algorithms prefers a solution whose matches ranges don't overlap and
 * are continuous.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AmbiguousColumnResolver {

    /**
     * Maps query result column indices to result object columns.
     *
     * @param resultColumns The ordered result column names.
     * @param mappings      An array containing the list of result object column names that must be
     *                      mapped to indices of `resultColumns`.
     * @return An array with the same dimensions as `mappings` whose values correspond to the
     * index in `resultColumns` that match the object column at `mappings[i][j]`.
     */
    @JvmStatic
    public fun resolve(
        resultColumns: Array<String>,
        mappings: Array<Array<String>>
    ): Array<IntArray> {
        // Step 1 - Transform all input columns to lowercase
        for (i in resultColumns.indices) {
            // For result columns, apply workarounds in CursorUtil.getColumnIndex(), i.e. backtick
            // trimming.
            val column = resultColumns[i]
            resultColumns[i] = if (column[0] == '`' && column[column.length - 1] == '`') {
                column.substring(1, column.length - 1)
            } else {
                column
            }.lowercase(Locale.US)
        }
        for (i in mappings.indices) {
            for (j in mappings[i].indices) {
                mappings[i][j] = mappings[i][j].lowercase(Locale.US)
            }
        }

        // Step 2 - Check requested columns and create a useful list that ignores unused columns.
        val requestedColumns = buildSet { mappings.forEach { addAll(it) } }
        val usefulResultColumns = buildList {
            resultColumns.forEachIndexed { index, columnName ->
                if (requestedColumns.contains(columnName)) {
                    add(ResultColumn(columnName, index))
                }
            }
        }

        // Step 3 - Find all sublist from results columns that match mapping columns unordered.
        val mappingMatches = List(mappings.size) { mutableListOf<Match>() }
        mappings.forEachIndexed { mappingIndex, mapping ->
            // Step 3.a - Quick searching using a rolling hash
            rabinKarpSearch(
                content = usefulResultColumns,
                pattern = mapping
            ) { startIndex, endIndex, resultColumnsSublist ->
                val resultIndices = mapping.map { mappingColumnName ->
                    val resultColumn = resultColumnsSublist.firstOrNull { (resultColumnName, _) ->
                        // TODO: Incorporate workarounds in CursorUtil.getColumnIndex()
                        mappingColumnName == resultColumnName
                    }
                    resultColumn?.index ?: return@rabinKarpSearch
                }
                mappingMatches[mappingIndex].add(
                    Match(
                        resultRange = IntRange(startIndex, endIndex - 1),
                        resultIndices = resultIndices
                    )
                )
            }
            // Step 3.b - Failed to quickly find a continuous match, widen the search (slow!)
            if (mappingMatches[mappingIndex].isEmpty()) {
                val foundIndices = mapping.map { mappingColumnName ->
                    buildList {
                        usefulResultColumns.forEach { resultColumn ->
                            if (mappingColumnName == resultColumn.name) {
                                add(resultColumn.index)
                            }
                        }
                    }.also {
                        check(it.isNotEmpty()) { "Column $mappingColumnName not found in result" }
                    }
                }
                dfs(foundIndices) { indices ->
                    val first = indices.minOf { it }
                    val last = indices.maxOf { it }
                    mappingMatches[mappingIndex].add(
                        Match(
                            resultRange = IntRange(first, last),
                            resultIndices = indices
                        )
                    )
                }
            }
        }
        check(mappingMatches.all { it.isNotEmpty() }) { "Failed to find matches for all mappings" }

        // Step 4 - Depth first search through combinations finding the best solution
        var bestSolution = Solution.NO_SOLUTION
        dfs(mappingMatches) {
            val currentSolution = Solution.build(it)
            if (currentSolution < bestSolution) {
                bestSolution = currentSolution
            }
        }
        return bestSolution.matches.map { it.resultIndices.toIntArray() }.toTypedArray()
    }

    private fun rabinKarpSearch(
        content: List<ResultColumn>,
        pattern: Array<String>,
        onHashMatch: (Int, Int, List<ResultColumn>) -> Unit
    ) {
        val mappingHash = pattern.sumOf { it.hashCode() } // Commutative hash
        var startIndex = 0 // inclusive
        var endIndex = pattern.size // exclusive
        var rollingHash = content.subList(startIndex, endIndex).sumOf { it.name.hashCode() }
        while (true) {
            if (mappingHash == rollingHash) {
                onHashMatch(startIndex, endIndex, content.subList(startIndex, endIndex))
            }
            startIndex++
            endIndex++
            if (endIndex > content.size) {
                break
            }
            // Rolling hash adjustment
            rollingHash -= content[startIndex - 1].name.hashCode()
            rollingHash += content[endIndex - 1].name.hashCode()
        }
    }

    private fun <T> dfs(
        content: List<List<T>>,
        current: MutableList<T> = mutableListOf(),
        depth: Int = 0,
        block: (List<T>) -> Unit
    ) {
        if (depth == content.size) {
            block(current.toList())
            return
        }
        content[depth].forEach {
            current.add(it)
            dfs(content, current, depth + 1, block)
            current.removeLast()
        }
    }

    private data class ResultColumn(val name: String, val index: Int)

    private class Match(
        val resultRange: IntRange,
        val resultIndices: List<Int>,
    )

    /**
     * A good solution is one that has no overlaps and whose coverage offset is zero, where coverage
     * offset is the difference between a mapping size and its matching range, i.e. preferring
     * continuous matches vs those with index gaps.
     */
    private class Solution(
        val matches: List<Match>,
        val coverageOffset: Int, // amount of indices covered by matches
        val overlaps: Int, // amount of indices that overlap
    ) : Comparable<Solution> {

        override fun compareTo(other: Solution): Int {
            val overlapCmp = this.overlaps.compareTo(other.overlaps)
            if (overlapCmp != 0) {
                return overlapCmp
            }
            return this.coverageOffset.compareTo(other.coverageOffset)
        }

        companion object {
            val NO_SOLUTION = Solution(emptyList(), Int.MAX_VALUE, Int.MAX_VALUE)

            fun build(
                matches: List<Match>
            ): Solution {
                val coverageOffset = matches.sumOf {
                    (it.resultRange.last - it.resultRange.first + 1) - it.resultIndices.size
                }
                val min = matches.minOf { it.resultRange.first }
                val max = matches.maxOf { it.resultRange.last }
                val overlaps = (min..max).count { i ->
                    var count = 0
                    matches.forEach {
                        if (it.resultRange.contains(i)) {
                            count++
                        }
                        if (count > 1) {
                            return@count true
                        }
                    }
                    return@count false
                }
                return Solution(
                    matches = matches,
                    coverageOffset = coverageOffset,
                    overlaps = overlaps
                )
            }
        }
    }
}
