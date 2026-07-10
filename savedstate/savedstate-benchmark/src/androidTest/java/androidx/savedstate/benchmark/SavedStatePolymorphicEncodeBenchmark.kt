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
import androidx.savedstate.benchmark.utils.PolymorphicClass
import androidx.savedstate.benchmark.utils.PolymorphicClassData
import androidx.savedstate.benchmark.utils.PolymorphicClassImpl1
import androidx.savedstate.benchmark.utils.PolymorphicClassImpl2
import androidx.savedstate.benchmark.utils.PolymorphicInterface
import androidx.savedstate.benchmark.utils.PolymorphicInterfaceData
import androidx.savedstate.benchmark.utils.PolymorphicInterfaceImpl1
import androidx.savedstate.benchmark.utils.PolymorphicInterfaceImpl2
import androidx.savedstate.benchmark.utils.PolymorphicMixedData
import androidx.savedstate.benchmark.utils.PolymorphicNullMixedData
import androidx.savedstate.benchmark.utils.polymorphicTestModule
import androidx.savedstate.serialization.ClassDiscriminatorMode
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test

@LargeTest
class SavedStatePolymorphicEncodeBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()

    private val savedStateConfiguration = SavedStateConfiguration {
        classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC
        serializersModule = polymorphicTestModule
    }

    @Test
    fun testEncodePolymorphicClass() {
        val data: PolymorphicClass = PolymorphicClassImpl1(1)
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodePolymorphicInterface() {
        val data: PolymorphicInterface = PolymorphicInterfaceImpl1(1)
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodePolymorphicClassData() {
        val data = PolymorphicClassData(PolymorphicClassImpl1(1), PolymorphicClassImpl2("test"))
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodePolymorphicInterfaceData() {
        val data =
            PolymorphicInterfaceData(
                PolymorphicInterfaceImpl1(1),
                PolymorphicInterfaceImpl2("test"),
            )
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodePolymorphicNullMixedData() {
        val data = PolymorphicNullMixedData(PolymorphicClassImpl1(1), null)
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }

    @Test
    fun testEncodePolymorphicMixedData() {
        val data = PolymorphicMixedData(PolymorphicClassImpl1(1), PolymorphicInterfaceImpl2("test"))
        benchmarkRule.measureRepeated { encodeToSavedState(data, savedStateConfiguration) }
    }
}
