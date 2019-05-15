/*
 * Copyright 2018 The Android Open Source Project
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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ViewModelLazyTest {

    @Test
    fun test() {
        val factoryProducer = { TestFactory() }
        val store = ViewModelStore()
        val vm by ViewModelLazy(TestVM::class, { store }, factoryProducer)
        assertThat(vm.prop).isEqualTo("spb")
        assertThat(store.keys()).isNotEmpty()
    }

    class TestVM(val prop: String) : ViewModel()

    @Suppress("UNCHECKED_CAST")
    class TestFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T = TestVM("spb") as T
    }
}