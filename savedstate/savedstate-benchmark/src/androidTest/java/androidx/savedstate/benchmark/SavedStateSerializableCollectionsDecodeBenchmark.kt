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
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test

@LargeTest
class SavedStateSerializableCollectionsDecodeBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()
    private val sampleSize = 100
    private val savedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT

    @Test
    fun testDecodeListIntData() {
        val data = ListIntData(List(sampleSize) { it })
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<ListIntData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeListStringData() {
        val data = ListStringData(List(sampleSize) { "item $it" })
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<ListStringData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeBooleanArrayData() {
        val data = BooleanArrayData(BooleanArray(sampleSize) { it % 2 == 0 })
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<BooleanArrayData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeCharArrayData() {
        val data = CharArrayData(CharArray(sampleSize) { (it % 26 + 'a'.code).toChar() })
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<CharArrayData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeDoubleArrayData() {
        val data = DoubleArrayData(DoubleArray(sampleSize) { it.toDouble() })
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<DoubleArrayData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeFloatArrayData() {
        val data = FloatArrayData(FloatArray(sampleSize) { it.toFloat() })
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<FloatArrayData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeIntArrayData() {
        val data = IntArrayData(IntArray(sampleSize) { it })
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<IntArrayData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeLongArrayData() {
        val data = LongArrayData(LongArray(sampleSize) { it.toLong() })
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<LongArrayData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeStringArrayData() {
        val data = StringArrayData(Array(sampleSize) { "item $it" })
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<StringArrayData>(encodedData, savedStateConfiguration)
        }
    }
}
