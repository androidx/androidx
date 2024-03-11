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
import kotlin.test.Test

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

    private open class TestViewModel : ViewModel() {
        var cleared = false
        public override fun onCleared() {
            cleared = true
        }
    }
}
