/*
 * Copyright (C) 2017 The Android Open Source Project
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

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@RunWith(JUnit4::class)
class ViewModelStoreTest {
    @Test
    fun testClear() {
        val store = ViewModelStore()
        val viewModel1 = TestViewModel()
        val viewModel2 = TestViewModel()
        val mockViewModel = mock(TestViewModel::class.java)
        store.put("a", viewModel1)
        store.put("b", viewModel2)
        store.put("mock", mockViewModel)
        assertThat(viewModel1.cleared, `is`(false))
        assertThat(viewModel2.cleared, `is`(false))
        store.clear()
        assertThat(viewModel1.cleared, `is`(true))
        assertThat(viewModel2.cleared, `is`(true))
        verify(mockViewModel).onCleared()
        verifyNoMoreInteractions(mockViewModel)
        assertThat(store["a"], nullValue())
        assertThat(store["b"], nullValue())
    }

    internal open class TestViewModel : ViewModel() {
        var cleared = false
        public override fun onCleared() {
            cleared = true
        }
    }
}
