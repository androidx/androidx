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

import androidx.kruth.assertThat
import java.io.Closeable
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito

@RunWith(JUnit4::class)
class MockViewModelTest {

    private class CloseableImpl : Closeable {
        var wasClosed = false
        override fun close() {
            wasClosed = true
        }
    }

    private open class TestViewModel : ViewModel()

    @Test
    fun addCloseable_withMock_doesNotThrow() {
        val vm = Mockito.mock(TestViewModel::class.java)
        val impl = CloseableImpl()
        // This shouldn't crash, even on a mocked object
        vm.addCloseable(impl)
    }

    @Test
    fun getCloseable_withMock_doesNotThrow_returnsNull() {
        val key = "key"
        val vm = Mockito.mock(TestViewModel::class.java)
        val impl = CloseableImpl()
        // This shouldn't crash, even on a mocked object
        vm.addCloseable(key, impl)
        val actualCloseable = vm.getCloseable<CloseableImpl>(key)
        assertThat(actualCloseable).isNull()
    }
}
