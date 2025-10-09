/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.savedstate.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.savedstate.benchmark.utils.BooleanArrayData
import androidx.savedstate.benchmark.utils.CharArrayData
import androidx.savedstate.benchmark.utils.DoubleArrayData
import androidx.savedstate.benchmark.utils.FloatArrayData
import androidx.savedstate.benchmark.utils.IntArrayData
import androidx.savedstate.benchmark.utils.ListIntData
import androidx.savedstate.benchmark.utils.ListStringData
import androidx.savedstate.benchmark.utils.LongArrayData
import androidx.savedstate.benchmark.utils.StringArrayData
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test

@LargeTest
class SavedStateSerializableCollectionsEncodeBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()
    private val sampleSize = 100
    private val savedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT

    @Test
    fun testEncodeListIntData() {
        val data = ListIntData(List(sampleSize) { it })
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeListStringData() {
        val data = ListStringData(List(sampleSize) { "item $it" })
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeBooleanArrayData() {
        val data = BooleanArrayData(BooleanArray(sampleSize) { it % 2 == 0 })
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeCharArrayData() {
        val data = CharArrayData(CharArray(sampleSize) { (it % 26 + 'a'.code).toChar() })
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeDoubleArrayData() {
        val data = DoubleArrayData(DoubleArray(sampleSize) { it.toDouble() })
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeFloatArrayData() {
        val data = FloatArrayData(FloatArray(sampleSize) { it.toFloat() })
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeIntArrayData() {
        val data = IntArrayData(IntArray(sampleSize) { it })
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeLongArrayData() {
        val data = LongArrayData(LongArray(sampleSize) { it.toLong() })
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeStringArrayData() {
        val data = StringArrayData(Array(sampleSize) { "item $it" })
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }
}
