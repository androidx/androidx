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
import androidx.lifecycle.viewmodel.IgnoreWebTarget
import kotlin.test.Test

@IgnoreWebTarget
class ViewModelStoreTest {

    @Test
    fun testClear() {
        val store = ViewModelStore()
        val viewModel1 = TestViewModel()
        val viewModel2 = TestViewModel()
        store.put(key = "a", viewModel1)
        store.put(key = "b", viewModel2)
        assertThat(viewModel1.cleared).isFalse()
        assertThat(viewModel2.cleared).isFalse()
        store.clear()
        assertThat(viewModel1.cleared).isTrue()
        assertThat(viewModel2.cleared).isTrue()
        assertThat(store["a"]).isNull()
        assertThat(store["b"]).isNull()
    }

    @Test
    fun testToString() {
        class SubStore : ViewModelStore()
        val store = SubStore()
        store.put("key1", TestViewModel())

        // Verify identity included (discourage parsing) and keys listed.
        val identity = store.hashCode().toString(16)
        assertThat(store.toString()).isEqualTo("SubStore#$identity(keys=[key1])")
    }

    @Test
    fun testClear_withReentrantAdditionOnLastItem_doesNotCrash() {
        val store = ViewModelStore()

        val viewModelReentrant = TestViewModel()
        val viewModel1 = TestViewModel()
        val viewModel2 = TestViewModel { store.put("reentrant", viewModelReentrant) }

        store.put("a", viewModel1)
        store.put("b", viewModel2)

        store.clear()

        // Validates that adding a 'ViewModel' to the store during another 'onCleared' does not
        // crash the iterator or leave the store in an inconsistent state.
        assertThat(viewModelReentrant.cleared).isFalse()

        assertThat(viewModel1.cleared).isTrue()
        assertThat(viewModel2.cleared).isTrue()
        assertThat(store.keys()).containsExactly("reentrant")
    }

    @Test
    fun testClear_withReentrantAdditionOnFirstItem_doesNotCrash() {
        val store = ViewModelStore()

        val viewModelReentrant = TestViewModel()
        val viewModel1 = TestViewModel { store.put("reentrant", viewModelReentrant) }
        val viewModel2 = TestViewModel()

        store.put("a", viewModel1)
        store.put("b", viewModel2)

        store.clear()

        // Validates that adding a 'ViewModel' to the store during another 'onCleared' does not
        // crash the iterator or leave the store in an inconsistent state.
        assertThat(viewModelReentrant.cleared).isFalse()

        assertThat(viewModel1.cleared).isTrue()
        assertThat(viewModel2.cleared).isTrue()
        assertThat(store.keys()).containsExactly("reentrant")
    }

    @Test
    fun testClear_withReentrantClearOnFirstItem_doesNotCrash() {
        val store = ViewModelStore()

        val viewModel1 = TestViewModel { store.clear() }
        val viewModel2 = TestViewModel()

        store.put("a", viewModel1)
        store.put("b", viewModel2)

        // Simulates a 'onCleared' causing the entire 'ViewModelStore' to be cleared again.
        // The store must be able to handle nested 'clear()' calls safely without crashing.
        store.clear()

        assertThat(viewModel1.cleared).isTrue()
        assertThat(viewModel2.cleared).isTrue()
        assertThat(store.keys()).isEmpty()
    }

    @Test
    fun testClear_withReentrantClearOnLastItem_doesNotCrash() {
        val store = ViewModelStore()

        val viewModel1 = TestViewModel()
        val viewModel2 = TestViewModel { store.clear() }

        store.put("a", viewModel1)
        store.put("b", viewModel2)

        // Simulates a 'onCleared' causing the entire 'ViewModelStore' to be cleared again.
        // The store must be able to handle nested 'clear()' calls safely without crashing.
        store.clear()

        assertThat(viewModel1.cleared).isTrue()
        assertThat(viewModel2.cleared).isTrue()
        assertThat(store.keys()).isEmpty()
    }

    private class TestViewModel(private val onCleared: () -> Unit = {}) : ViewModel() {
        var cleared = false
            private set

        override fun onCleared() {
            cleared = true
            onCleared.invoke()
        }
    }
}
