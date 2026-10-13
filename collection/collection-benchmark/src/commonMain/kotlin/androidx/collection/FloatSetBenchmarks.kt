/*
 * Copyright 2024 The Android Open Source Project
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

class FloatSetInsertBenchmark(private val dataSet: FloatArray) : CollectionBenchmark {
    override fun measuredBlock() {
        val set = MutableFloatSet(dataSet.size)
        for (testValue in dataSet) {
            set += testValue
        }
    }
}

class FloatSetRemoveBenchmark(private val dataSet: FloatArray) : CollectionBenchmark {
    private val set = MutableFloatSet(dataSet.size)

    override fun prepare() {
        for (testValue in dataSet) {
            set += testValue
        }
    }

    override fun measuredBlock() {
        for (testValue in dataSet) {
            set.remove(testValue)
        }
    }
}

class FloatSetContainsBenchmark(private val dataSet: FloatArray) : CollectionBenchmark {
    private val set = MutableFloatSet(dataSet.size)

    init {
        for (testValue in dataSet) {
            set += testValue
        }
    }

    override fun measuredBlock() {
        for (testValue in dataSet) {
            set.contains(testValue)
        }
    }
}

fun createFloatSetDataSet(size: Int): FloatArray = FloatArray(size) { index -> (index + 1) * 8.0f }
