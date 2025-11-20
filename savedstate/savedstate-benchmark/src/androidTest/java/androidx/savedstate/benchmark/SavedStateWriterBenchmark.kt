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
import androidx.savedstate.savedState
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test

@LargeTest
class SavedStateWriterBenchmark {

    @get:Rule val benchmarkRule = BenchmarkRule()

    @Test
    fun testPutBoolean() {
        benchmarkRule.measureRepeated { savedState { putBoolean("b", false) } }
    }

    @Test
    fun testPutChar() {
        benchmarkRule.measureRepeated { savedState { putChar("c", Char.MIN_VALUE) } }
    }

    @Test
    fun testPutCharSequence() {
        benchmarkRule.measureRepeated { savedState { putCharSequence("cs", "android") } }
    }

    @Test
    fun testPutCharSequenceList() {
        benchmarkRule.measureRepeated {
            savedState { putCharSequenceList("csl", listOf("android", "kotlin")) }
        }
    }

    @Test
    fun testPutDouble() {
        benchmarkRule.measureRepeated { savedState { putDouble("d", Double.MIN_VALUE) } }
    }

    @Test
    fun testPutFloat() {
        benchmarkRule.measureRepeated { savedState { putFloat("f", Float.MIN_VALUE) } }
    }

    @Test
    fun testPutInt() {
        benchmarkRule.measureRepeated { savedState { putInt("i", Int.MIN_VALUE) } }
    }

    @Test
    fun testPutIntList_small() {
        val list = List(SMALL_LIST_SIZE) { it }
        benchmarkRule.measureRepeated { savedState { putIntList("il", list) } }
    }

    @Test
    fun testPutIntList_large() {
        val list = List(LARGE_LIST_SIZE) { it }
        benchmarkRule.measureRepeated { savedState { putIntList("il", list) } }
    }

    @Test
    fun testPutLong() {
        benchmarkRule.measureRepeated { savedState { putLong("l", Long.MIN_VALUE) } }
    }

    @Test
    fun testPutSize() {
        benchmarkRule.measureRepeated { savedState { putSize("sz", Size(1, 1)) } }
    }

    @Test
    fun testPutSizeF() {
        benchmarkRule.measureRepeated { savedState { putSizeF("szf", SizeF(1f, 1f)) } }
    }

    @Test
    fun testPutString() {
        benchmarkRule.measureRepeated { savedState { putString("s", "android") } }
    }

    @Test
    fun testPutStringList_small() {
        val list = List(SMALL_LIST_SIZE) { "android" }
        benchmarkRule.measureRepeated { savedState { putStringList("sl", list) } }
    }

    @Test
    fun testPutStringList_large() {
        val list = List(LARGE_LIST_SIZE) { "android" }
        benchmarkRule.measureRepeated { savedState { putStringList("sl", list) } }
    }

    @Test
    fun testPutBooleanArray_small() {
        val array = List(SMALL_LIST_SIZE) { it % 2 == 0 }.toBooleanArray()
        benchmarkRule.measureRepeated { savedState { putBooleanArray("ba", array) } }
    }

    @Test
    fun testPutBooleanArray_large() {
        val array = List(LARGE_LIST_SIZE) { it % 2 == 0 }.toBooleanArray()
        benchmarkRule.measureRepeated { savedState { putBooleanArray("ba", array) } }
    }

    @Test
    fun testPutCharArray_small() {
        val array = List(SMALL_LIST_SIZE) { 'a' }.toCharArray()
        benchmarkRule.measureRepeated { savedState { putCharArray("ca", array) } }
    }

    @Test
    fun testPutCharArray_large() {
        val array = List(LARGE_LIST_SIZE) { 'a' }.toCharArray()
        benchmarkRule.measureRepeated { savedState { putCharArray("ca", array) } }
    }

    @Test
    fun testPutCharSequenceArray_small() {
        val array: Array<CharSequence> = List(SMALL_LIST_SIZE) { "android" }.toTypedArray()
        benchmarkRule.measureRepeated { savedState { putCharSequenceArray("csa", array) } }
    }

    @Test
    fun testPutCharSequenceArray_large() {
        val array: Array<CharSequence> = List(LARGE_LIST_SIZE) { "android" }.toTypedArray()
        benchmarkRule.measureRepeated { savedState { putCharSequenceArray("csa", array) } }
    }

    @Test
    fun testPutDoubleArray_small() {
        val array = List(SMALL_LIST_SIZE) { it.toDouble() }.toDoubleArray()
        benchmarkRule.measureRepeated { savedState { putDoubleArray("da", array) } }
    }

    @Test
    fun testPutDoubleArray_large() {
        val array = List(LARGE_LIST_SIZE) { it.toDouble() }.toDoubleArray()
        benchmarkRule.measureRepeated { savedState { putDoubleArray("da", array) } }
    }

    @Test
    fun testPutFloatArray_small() {
        val array = List(SMALL_LIST_SIZE) { it.toFloat() }.toFloatArray()
        benchmarkRule.measureRepeated { savedState { putFloatArray("fa", array) } }
    }

    @Test
    fun testPutFloatArray_large() {
        val array = List(LARGE_LIST_SIZE) { it.toFloat() }.toFloatArray()
        benchmarkRule.measureRepeated { savedState { putFloatArray("fa", array) } }
    }

    @Test
    fun testPutIntArray_small() {
        val array = List(SMALL_LIST_SIZE) { it }.toIntArray()
        benchmarkRule.measureRepeated { savedState { putIntArray("ia", array) } }
    }

    @Test
    fun testPutIntArray_large() {
        val array = List(LARGE_LIST_SIZE) { it }.toIntArray()
        benchmarkRule.measureRepeated { savedState { putIntArray("ia", array) } }
    }

    @Test
    fun testPutLongArray_small() {
        val array = List(SMALL_LIST_SIZE) { it.toLong() }.toLongArray()
        benchmarkRule.measureRepeated { savedState { putLongArray("la", array) } }
    }

    @Test
    fun testPutLongArray_large() {
        val array = List(LARGE_LIST_SIZE) { it.toLong() }.toLongArray()
        benchmarkRule.measureRepeated { savedState { putLongArray("la", array) } }
    }

    @Test
    fun testPutStringArray_small() {
        val array = List(SMALL_LIST_SIZE) { "android" }.toTypedArray()
        benchmarkRule.measureRepeated { savedState { putStringArray("sa", array) } }
    }

    @Test
    fun testPutStringArray_large() {
        val array = List(LARGE_LIST_SIZE) { "android" }.toTypedArray()
        benchmarkRule.measureRepeated { savedState { putStringArray("sa", array) } }
    }

    companion object {
        const val SMALL_LIST_SIZE = 10
        const val LARGE_LIST_SIZE = 1000
    }
}
