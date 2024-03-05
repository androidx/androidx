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

package androidx.lifecycle.viewmodel

import androidx.kruth.assertThat
import androidx.lifecycle.ViewModel
import kotlin.test.Test
import kotlin.test.fail

class ViewModelInitializerTest {

    @Test
    fun viewModelFactory_withUniqueInitializers_withCreationExtras_returnsViewModels() {
        val key1 = object : CreationExtras.Key<String> {}
        val value1 = "test_value1"
        val extras1 = MutableCreationExtras().apply { set(key1, value1) }

        val key2 = object : CreationExtras.Key<String> {}
        val value2 = "test_value2"
        val extras2 = MutableCreationExtras().apply { set(key2, value2) }

        val factory = viewModelFactory {
            initializer<TestViewModel1> { TestViewModel1(get(key1)) }
            initializer<TestViewModel2> { TestViewModel2(get(key2)) }
        }

        val viewModel1: TestViewModel1 = factory.create(TestViewModel1::class, extras1)
        val viewModel2: TestViewModel2 = factory.create(TestViewModel2::class, extras2)
        assertThat(viewModel1.value).isEqualTo(value1)
        assertThat(viewModel2.value).isEqualTo(value2)
    }

    @Test
    fun viewModelFactory_withDuplicatedInitializers_throwsException() {
        try {
            viewModelFactory {
                initializer<TestViewModel1> { TestViewModel1(null) }
                initializer<TestViewModel1> { TestViewModel1(null) }
            }
            fail("Expected `IllegalArgumentException` but no exception has been throw.")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().isEqualTo(
                "A `initializer` with the same `clazz` has already been added: " +
                    "${TestViewModel1::class.qualifiedName}."
            )
        }
    }

    @Test
    fun viewModelFactory_noInitializers_throwsException() {
        val factory = viewModelFactory { }
        try {
            factory.create(TestViewModel1::class, CreationExtras.Empty)
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().isEqualTo(
                "No initializer set for given class ${TestViewModel1::class.qualifiedName}"
            )
        }
    }
}

private class TestViewModel1(val value: String?) : ViewModel()

private class TestViewModel2(val value: String?) : ViewModel()
