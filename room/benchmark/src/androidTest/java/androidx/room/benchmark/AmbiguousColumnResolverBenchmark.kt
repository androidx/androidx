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

package androidx.room.benchmark

import android.os.Build
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room.AmbiguousColumnResolver
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.generateAllEnumerations
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
class AmbiguousColumnResolverBenchmark(
    private val numOfColumns: Int,
    private val numOfTables: Int,
    private val numOfDupes: Int,
) {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun largeResolution() {
        // Create unique result columns
        val resultColumns = mutableListOf<String>().apply {
            for (i in 1..numOfColumns) {
                add("Column_$i")
            }
        }
        // Create dupe columns, each table mapping will contain these columns
        val dupeColumns = buildList {
            for (i in 1..numOfDupes) {
                add("Dupe_Column_$i")
            }
        }
        // Create mappings, with dupe columns inserted
        val columnsPerTable = numOfColumns.floorDiv(numOfTables)
        val mappings = buildList<List<String>> {
            // Split the result columns by the num of tables, each mapping will more or less have
            // the same amount of columns.
            for (i in 0 until numOfTables) {
                val startInclusive = i * columnsPerTable
                val endExclusive =
                    if (startInclusive + columnsPerTable * 2 > numOfColumns) {
                        numOfColumns
                    } else {
                        startInclusive + columnsPerTable
                    }
                // Place each dupe column within the range of result columns for this table mapping.
                val dupeIndices = mutableSetOf<Int>() // To have distinct dupe indices
                dupeColumns.forEach { dupeCol ->
                    var dupeIndex: Int
                    do {
                        dupeIndex = Random.nextInt(startInclusive, endExclusive)
                    } while (dupeIndices.contains(dupeIndex))
                    dupeIndices.add(dupeIndex)
                    resultColumns[dupeIndex] = dupeCol
                }
                add(resultColumns.subList(startInclusive, endExclusive))
            }
        }
        // Create expected result, its just an increasing index assignment
        val expected = buildList {
            var i = 0
            mappings.forEach {
                add(IntArray(it.size) { i++ })
            }
        }

        benchmarkRule.measureRepeated {
            val result = AmbiguousColumnResolver.resolve(
                resultColumns = resultColumns.toTypedArray(),
                mappings = mappings.map { it.toTypedArray() }.toTypedArray()
            )
            runWithTimingDisabled {
                result.forEachIndexed { i, resultMapping ->
                    assertArrayEquals(resultMapping, expected[i])
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "numOfColumns={0}, numOfTables={1}, numOfDupes={2}")
        fun data() = generateAllEnumerations(
            listOf(20, 50, 100),
            listOf(2, 3),
            listOf(2, 4)
        )
    }
}
