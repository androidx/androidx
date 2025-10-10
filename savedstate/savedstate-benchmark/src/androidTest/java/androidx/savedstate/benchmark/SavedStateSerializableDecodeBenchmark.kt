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
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test

@LargeTest
class SavedStateSerializableDecodeBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()
    private val numericTestValue = 100
    private val savedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT

    @Test
    fun testDecodeIntData() {
        val data = IntData(numericTestValue)
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<IntData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeLongData() {
        val data = LongData(numericTestValue.toLong())
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<LongData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeShortData() {
        val data = ShortData(numericTestValue.toShort())
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<ShortData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeByteData() {
        val data = ByteData(numericTestValue.toByte())
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<ByteData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeBooleanData() {
        val data = BooleanData(true)
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<BooleanData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeCharData() {
        val data = CharData('a')
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<CharData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeFloatData() {
        val data = FloatData(numericTestValue.toFloat())
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<FloatData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeDoubleData() {
        val data = DoubleData(numericTestValue.toDouble())
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<DoubleData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeStringData() {
        val data = StringData("item $numericTestValue")
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<StringData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeNullData() {
        val data = NullData(null)
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<NullData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeEnumData() {
        val data =
            EnumData(
                androidx.savedstate.benchmark.utils.Enum.OptionA,
                androidx.savedstate.benchmark.utils.Enum.OptionB,
            )
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<EnumData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeBoxData() {
        val data = BoxData("test")
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<BoxData<String>>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeSealedData() {
        val data = SealedData(SealedImpl1(1), SealedImpl2("test"))
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<SealedData>(encodedData, savedStateConfiguration)
        }
    }

    @Test
    fun testDecodeObjectData() {
        val data = ObjectData
        val encodedData = encodeToSavedState(data, savedStateConfiguration)
        benchmarkRule.measureRepeated {
            decodeFromSavedState<ObjectData>(encodedData, savedStateConfiguration)
        }
    }
}
