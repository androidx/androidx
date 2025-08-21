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

import android.util.Size
import android.util.SizeF
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.savedstate.SavedState
import androidx.savedstate.savedState
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test

@LargeTest
class SavedStateReaderBenchmark {

    @get:Rule val benchmarkRule = BenchmarkRule()

    val savedState: SavedState = savedState {
        putBoolean("b", false)
        putChar("c", Char.MIN_VALUE)
        putCharSequence("cs", "android")
        putDouble("d", Double.MIN_VALUE)
        putFloat("f", Float.MIN_VALUE)
        putInt("i", Int.MIN_VALUE)
        putLong("l", Long.MIN_VALUE)
        putSize("sz", Size(1, 1))
        putSizeF("szf", SizeF(1f, 1f))
        putString("s", "android")

        putBooleanArray("ba-small", BooleanArray(SMALL_LIST_SIZE) { it % 2 == 0 })
        putBooleanArray("ba-large", BooleanArray(LARGE_LIST_SIZE) { it % 2 == 0 })
        putCharArray("ca-small", CharArray(SMALL_LIST_SIZE) { (it % 26 + 'a'.code).toChar() })
        putCharArray("ca-large", CharArray(LARGE_LIST_SIZE) { (it % 26 + 'a'.code).toChar() })
        putCharSequenceArray("csa-small", Array(SMALL_LIST_SIZE) { "android $it" })
        putCharSequenceArray("csa-large", Array(LARGE_LIST_SIZE) { "android $it" })
        putCharSequenceList("csl-small", List(SMALL_LIST_SIZE) { "android $it" })
        putCharSequenceList("csl-large", List(LARGE_LIST_SIZE) { "android $it" })
        putDoubleArray("da-small", DoubleArray(SMALL_LIST_SIZE) { it.toDouble() })
        putDoubleArray("da-large", DoubleArray(LARGE_LIST_SIZE) { it.toDouble() })
        putFloatArray("fa-small", FloatArray(SMALL_LIST_SIZE) { it.toFloat() })
        putFloatArray("fa-large", FloatArray(LARGE_LIST_SIZE) { it.toFloat() })
        putIntArray("ia-small", IntArray(SMALL_LIST_SIZE) { it })
        putIntArray("ia-large", IntArray(LARGE_LIST_SIZE) { it })
        putLongArray("la-small", LongArray(SMALL_LIST_SIZE) { it.toLong() })
        putLongArray("la-large", LongArray(LARGE_LIST_SIZE) { it.toLong() })
        putStringArray("sa-small", Array(SMALL_LIST_SIZE) { "item $it" })
        putStringArray("sa-large", Array(LARGE_LIST_SIZE) { "item $it" })
        putIntList("il-small", List(SMALL_LIST_SIZE) { it })
        putIntList("il-large", List(LARGE_LIST_SIZE) { it })
        putStringList("il-small", List(SMALL_LIST_SIZE) { "item $it" })
        putStringList("il-large", List(LARGE_LIST_SIZE) { "item $it" })
    }

    @Test
    fun testBoolean() {
        benchmarkRule.measureRepeated { savedState.getBoolean("b") }
    }

    @Test
    fun testChar() {
        benchmarkRule.measureRepeated { savedState.getChar("c") }
    }

    @Test
    fun testCharSequence() {
        benchmarkRule.measureRepeated { savedState.getCharSequence("cs") }
    }

    @Test
    fun testDouble() {
        benchmarkRule.measureRepeated { savedState.getDouble("d") }
    }

    @Test
    fun testFloat() {
        benchmarkRule.measureRepeated { savedState.getFloat("f") }
    }

    @Test
    fun testInt() {
        benchmarkRule.measureRepeated { savedState.getInt("i") }
    }

    @Test
    fun testLong() {
        benchmarkRule.measureRepeated { savedState.getLong("l") }
    }

    @Test
    fun testSize() {
        benchmarkRule.measureRepeated { savedState.getSize("sz") }
    }

    @Test
    fun testSizeF() {
        benchmarkRule.measureRepeated { savedState.getSizeF("szf") }
    }

    @Test
    fun testString() {
        benchmarkRule.measureRepeated { savedState.getString("s") }
    }

    @Test
    fun testBooleanArray_small() {
        benchmarkRule.measureRepeated { savedState.getBooleanArray("ba-small") }
    }

    @Test
    fun testBooleanArray_large() {
        benchmarkRule.measureRepeated { savedState.getBooleanArray("ba-large") }
    }

    @Test
    fun testCharArray_small() {
        benchmarkRule.measureRepeated { savedState.getCharArray("ca-small") }
    }

    @Test
    fun testCharArray_large() {
        benchmarkRule.measureRepeated { savedState.getCharArray("ca-large") }
    }

    @Test
    fun testCharSequenceArray_small() {
        benchmarkRule.measureRepeated { savedState.getCharSequenceArray("csa-small") }
    }

    @Test
    fun testCharSequenceArray_large() {
        benchmarkRule.measureRepeated { savedState.getCharSequenceArray("csa-large") }
    }

    @Test
    fun testCharSequenceList_small() {
        benchmarkRule.measureRepeated { savedState.getCharSequenceArrayList("csl-small") }
    }

    @Test
    fun testCharSequenceList_large() {
        benchmarkRule.measureRepeated { savedState.getCharSequenceArrayList("csl-large") }
    }

    @Test
    fun testDoubleArray_small() {
        benchmarkRule.measureRepeated { savedState.getDoubleArray("da-small") }
    }

    @Test
    fun testDoubleArray_large() {
        benchmarkRule.measureRepeated { savedState.getDoubleArray("da-large") }
    }

    @Test
    fun testFloatArray_small() {
        benchmarkRule.measureRepeated { savedState.getFloatArray("fa-small") }
    }

    @Test
    fun testFloatArray_large() {
        benchmarkRule.measureRepeated { savedState.getFloatArray("fa-large") }
    }

    @Test
    fun testIntArray_small() {
        benchmarkRule.measureRepeated { savedState.getIntArray("ia-small") }
    }

    @Test
    fun testIntArray_large() {
        benchmarkRule.measureRepeated { savedState.getIntArray("ia-large") }
    }

    @Test
    fun testIntList_small() {
        benchmarkRule.measureRepeated { savedState.getIntegerArrayList("il-small") }
    }

    @Test
    fun testIntList_large() {
        benchmarkRule.measureRepeated { savedState.getIntegerArrayList("il-large") }
    }

    @Test
    fun testLongArray_small() {
        benchmarkRule.measureRepeated { savedState.getLongArray("la-small") }
    }

    @Test
    fun testLongArray_large() {
        benchmarkRule.measureRepeated { savedState.getLongArray("la-large") }
    }

    @Test
    fun testStringArray_small() {
        benchmarkRule.measureRepeated { savedState.getStringArray("sa-small") }
    }

    @Test
    fun testStringArray_large() {
        benchmarkRule.measureRepeated { savedState.getStringArray("sa-large") }
    }

    @Test
    fun testStringList_small() {
        benchmarkRule.measureRepeated { savedState.getStringArrayList("sl-small") }
    }

    @Test
    fun testStringList_large() {
        benchmarkRule.measureRepeated { savedState.getStringArrayList("sl-large") }
    }

    companion object {
        const val SMALL_LIST_SIZE = 10
        const val LARGE_LIST_SIZE = 1000
    }
}
