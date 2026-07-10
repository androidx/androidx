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
import androidx.kruth.assertThat
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test

@LargeTest
class SavedStateListSerializationBenchmark() {
    @get:Rule val benchmarkRule = BenchmarkRule()
    private val sampleSize = 100

    @Test
    fun encodeDecodeListInt() {
        val original = List(sampleSize) { it }
        lateinit var decoded: List<Int>

        benchmarkRule.measureRepeated {
            val encoded = encodeToSavedState(original)
            val platformSavedState = platformEncodeDecode(encoded)
            decoded = decodeFromSavedState<List<Int>>(platformSavedState)
        }

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun encodeDecodeListBoolean() {
        val original = List(sampleSize) { it % 2 == 0 }
        lateinit var decoded: List<Boolean>

        benchmarkRule.measureRepeated {
            val encoded = encodeToSavedState(original)
            val platformSavedState = platformEncodeDecode(encoded)
            decoded = decodeFromSavedState<List<Boolean>>(platformSavedState)
        }

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun encodeDecodeListLong() {
        val original = List(sampleSize) { it.toLong() }
        lateinit var decoded: List<Long>

        benchmarkRule.measureRepeated {
            val encoded = encodeToSavedState(original)
            val platformSavedState = platformEncodeDecode(encoded)
            decoded = decodeFromSavedState<List<Long>>(platformSavedState)
        }

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun encodeDecodeListFloat() {
        val original = List(sampleSize) { it.toFloat() }
        lateinit var decoded: List<Float>

        benchmarkRule.measureRepeated {
            val encoded = encodeToSavedState(original)
            val platformSavedState = platformEncodeDecode(encoded)
            decoded = decodeFromSavedState<List<Float>>(platformSavedState)
        }

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun encodeDecodeListDouble() {
        val original = List(sampleSize) { it.toDouble() }
        lateinit var decoded: List<Double>

        benchmarkRule.measureRepeated {
            val encoded = encodeToSavedState(original)
            val platformSavedState = platformEncodeDecode(encoded)
            decoded = decodeFromSavedState<List<Double>>(platformSavedState)
        }

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun encodeDecodeListChar() {
        val original = List(sampleSize) { (it % 26 + 'a'.code).toChar() }
        lateinit var decoded: List<Char>

        benchmarkRule.measureRepeated {
            val encoded = encodeToSavedState(original)
            val platformSavedState = platformEncodeDecode(encoded)
            decoded = decodeFromSavedState<List<Char>>(platformSavedState)
        }

        assertThat(decoded).isEqualTo(original)
    }
}
