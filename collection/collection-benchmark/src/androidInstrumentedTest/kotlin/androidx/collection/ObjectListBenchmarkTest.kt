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
package androidx.collection

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ObjectListBenchmarkTest {
    val ObjectCount = 100
    private val list: ObjectList<String> = MutableObjectList<String>(ObjectCount).also { list ->
        repeat(ObjectCount) {
            list += it.toString()
        }
    }

    private val array = Array(ObjectCount) { it.toString() }

    @get:Rule
    val benchmark = BenchmarkRule()

    @Test
    fun forEach() {
        benchmark.measureRepeated {
            @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
            var last: String
            list.forEach { element ->
                last = element
            }
        }
    }

    @Test
    fun add() {
        val mutableList = MutableObjectList<String>(ObjectCount)
        benchmark.measureRepeated {
            repeat(ObjectCount) {
                mutableList += array[it]
            }
            mutableList.clear()
        }
    }

    @Test
    fun contains() {
        benchmark.measureRepeated {
            repeat(ObjectCount) {
                list.contains(array[it])
            }
        }
    }

    @Test
    fun get() {
        benchmark.measureRepeated {
            repeat(ObjectCount) {
                list[it]
            }
        }
    }

    @Test
    fun addAll() {
        val mutableList = MutableObjectList<String>(ObjectCount)
        benchmark.measureRepeated {
            mutableList += list
            mutableList.clear()
        }
    }

    @Test
    fun removeStart() {
        val mutableList = MutableObjectList<String>(ObjectCount)
        benchmark.measureRepeated {
            mutableList += list
            repeat(ObjectCount) {
                mutableList.removeAt(0)
            }
        }
    }

    @Test
    fun removeEnd() {
        val mutableList = MutableObjectList<String>(ObjectCount)
        benchmark.measureRepeated {
            mutableList += list
            for (i in ObjectCount - 1 downTo 0) {
                mutableList.removeAt(i)
            }
        }
    }
}
