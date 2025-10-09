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
import androidx.savedstate.benchmark.utils.BooleanData
import androidx.savedstate.benchmark.utils.BoxData
import androidx.savedstate.benchmark.utils.ByteData
import androidx.savedstate.benchmark.utils.CharData
import androidx.savedstate.benchmark.utils.DoubleData
import androidx.savedstate.benchmark.utils.EnumData
import androidx.savedstate.benchmark.utils.FloatData
import androidx.savedstate.benchmark.utils.IntData
import androidx.savedstate.benchmark.utils.LongData
import androidx.savedstate.benchmark.utils.NullData
import androidx.savedstate.benchmark.utils.ObjectData
import androidx.savedstate.benchmark.utils.SealedData
import androidx.savedstate.benchmark.utils.SealedImpl1
import androidx.savedstate.benchmark.utils.SealedImpl2
import androidx.savedstate.benchmark.utils.ShortData
import androidx.savedstate.benchmark.utils.StringData
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test

@LargeTest
class SavedStateSerializableEncodeBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()
    private val numericTestValue = 100
    private val savedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT

    @Test
    fun testEncodeIntData() {
        val data = IntData(numericTestValue)
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeLongData() {
        val data = LongData(numericTestValue.toLong())
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeShortData() {
        val data = ShortData(numericTestValue.toShort())
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeByteData() {
        val data = ByteData(numericTestValue.toByte())
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeBooleanData() {
        val data = BooleanData(true)
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeCharData() {
        val data = CharData('a')
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeFloatData() {
        val data = FloatData(numericTestValue.toFloat())
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeDoubleData() {
        val data = DoubleData(numericTestValue.toDouble())
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeStringData() {
        val data = StringData("item $numericTestValue")
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeNullData() {
        val data = NullData(null)
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeEnumData() {
        val data =
            EnumData(
                androidx.savedstate.benchmark.utils.Enum.OptionA,
                androidx.savedstate.benchmark.utils.Enum.OptionB,
            )
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeBoxData() {
        val data = BoxData("test")
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeSealedData() {
        val data = SealedData(SealedImpl1(1), SealedImpl2("test"))
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeObjectData() {
        val data = ObjectData
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }
}
