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

package androidx.lifecycle.viewmodel.internal

import androidx.kruth.assertThat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.test.Test
import kotlin.test.fail

class DefaultViewModelProviderFactoryTest {

    @Test
    fun create_withConstructorWithZeroArguments_returnsViewModel() {
        val modelClass = TestViewModel1::class
        val factory = DefaultViewModelProviderFactory

        val viewModel = factory.create(modelClass, CreationExtras.Empty)

        assertThat(viewModel).isNotNull()
    }

    @Test
    fun create_withConstructorWithOneArgument_throwsNoSuchMethodException() {
        val modelClass = TestViewModel2::class
        val factory = DefaultViewModelProviderFactory
        try {
            factory.create(modelClass, CreationExtras.Empty)
            fail("Expected `IllegalArgumentException` but no exception has been throw.")
        } catch (e: RuntimeException) {
            assertThat(e).hasCauseThat().isInstanceOf<NoSuchMethodException>()
            assertThat(e).hasMessageThat()
                .contains("Cannot create an instance of class ${TestViewModel2::class.java.name}")
        }
    }

    @org.junit.Test
    fun create_withPrivateConstructor_throwsIllegalAccessException() {
        val modelClass = TestViewModel3::class
        val factory = DefaultViewModelProviderFactory
        try {
            factory.create(modelClass, CreationExtras.Empty)
            fail("Expected `IllegalArgumentException` but no exception has been throw.")
        } catch (e: RuntimeException) {
            assertThat(e).hasCauseThat().isInstanceOf<IllegalAccessException>()
            assertThat(e).hasMessageThat()
                .contains("Cannot create an instance of class ${TestViewModel3::class.java.name}")
        }
    }

    @Test
    fun create_withAbstractConstructor_throwsInstantiationException() {
        val modelClass = ViewModel::class
        val factory = DefaultViewModelProviderFactory
        try {
            factory.create(modelClass, CreationExtras.Empty)
            fail("Expected `IllegalArgumentException` but no exception has been throw.")
        } catch (e: RuntimeException) {
            assertThat(e).hasCauseThat().isInstanceOf<InstantiationException>()
            assertThat(e).hasMessageThat()
                .contains("Cannot create an instance of class ${ViewModel::class.java.name}")
        }
    }

    class TestViewModel1 : ViewModel()
    class TestViewModel2(@Suppress("unused") val unused: Int) : ViewModel()
    private class TestViewModel3 private constructor() : ViewModel()
}
