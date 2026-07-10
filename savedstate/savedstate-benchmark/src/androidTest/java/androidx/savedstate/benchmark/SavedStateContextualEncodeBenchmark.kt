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
import androidx.savedstate.benchmark.utils.ContextualData
import androidx.savedstate.benchmark.utils.ContextualType
import androidx.savedstate.benchmark.utils.contextualTestModule
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test

@LargeTest
class SavedStateContextualEncodeBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()

    private val savedStateConfiguration = SavedStateConfiguration {
        serializersModule = contextualTestModule
    }

    @Test
    fun testEncodeContextualType() {
        val data = ContextualType("value1", "value2")
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodeContextualData() {
        val data = ContextualData(ContextualType("value1", "value2"))
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }
}
