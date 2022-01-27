/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.lifecycle.ViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ViewModelInitializerTest {
    @Test
    fun testInitializerFactory() {
        val key = object : CreationExtras.Key<String> {}
        val value1 = "test_value1"
        val extras1 = MutableCreationExtras().apply { set(key, value1) }
        val value2 = "test_value2"
        val extras2 = MutableCreationExtras().apply { set(key, value2) }
        val factory = viewModelFactory {
            initializer { TestViewModel1(extras1[key]) }
            initializer { TestViewModel2(extras2[key]) }
        }
        val viewModel1: TestViewModel1 = factory.create(TestViewModel1::class.java, extras1)
        val viewModel2: TestViewModel2 = factory.create(TestViewModel2::class.java, extras2)
        assertThat(viewModel1.value).isEqualTo(value1)
        assertThat(viewModel2.value).isEqualTo(value2)
    }

    @Test
    fun testInitializerFactoryNoInitializer() {
        val key = object : CreationExtras.Key<String> {}
        val value = "test_value"
        val extras = MutableCreationExtras().apply { set(key, value) }
        val factory = viewModelFactory { }
        try {
            factory.create(TestViewModel1::class.java, extras)
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().isEqualTo(
                "No initializer set for given class ${TestViewModel1::class.java.name}"
            )
        }
    }
}

private class TestViewModel1(val value: String?) : ViewModel()

private class TestViewModel2(val value: String?) : ViewModel()
