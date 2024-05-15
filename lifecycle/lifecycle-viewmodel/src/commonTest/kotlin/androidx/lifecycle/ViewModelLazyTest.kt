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

package androidx.lifecycle

import androidx.kruth.assertThat
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass
import kotlin.test.Test

class ViewModelLazyTest {

    @Test
    fun test() {
        val store = ViewModelStore()
        val viewModel by ViewModelLazy(
            viewModelClass = TestViewModel::class,
            storeProducer = { store },
            factoryProducer = { TestFactory() },
        )
        assertThat(viewModel.prop).isEqualTo(expected = "spb")
        assertThat(store.keys()).isNotEmpty()
    }

    private class TestViewModel(val prop: String) : ViewModel()

    private class TestFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
            TestViewModel(prop = "spb") as T
    }
}
