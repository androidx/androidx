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
@file:OptIn(ExperimentalStdlibApi::class)

package androidx.lifecycle

import androidx.kruth.assertThat
import kotlin.test.Test

class ViewModelTest {

    private class CloseableImpl : AutoCloseable {
        var wasClosed = false
        override fun close() {
            wasClosed = true
        }
    }

    private class TestViewModel : ViewModel()
    private class CloseableTestViewModel(closeable: AutoCloseable) : ViewModel(closeable)

    @Test
    fun testCloseableWithKey() {
        val vm = TestViewModel()
        val impl = CloseableImpl()
        vm.addCloseable("totally_not_coroutine_context", impl)
        vm.clear()
        assertThat(impl.wasClosed).isTrue()
    }

    @Test
    fun testCloseableWithKeyAlreadyClearedVM() {
        val vm = TestViewModel()
        vm.clear()
        val impl = CloseableImpl()
        vm.addCloseable("key", impl)
        assertThat(impl.wasClosed).isTrue()
    }

    @Test
    fun testAddCloseable() {
        val vm = TestViewModel()
        val impl = CloseableImpl()
        vm.addCloseable(impl)
        vm.clear()
        assertThat(impl.wasClosed).isTrue()
    }

    @Test
    fun testAddCloseableAlreadyClearedVM() {
        val vm = TestViewModel()
        vm.clear()
        val impl = CloseableImpl()
        // This shouldn't crash, even though vm already cleared
        vm.addCloseable(impl)
        assertThat(impl.wasClosed).isTrue()
    }

    @Test
    fun testConstructorCloseable() {
        val impl = CloseableImpl()
        val vm = CloseableTestViewModel(impl)
        vm.clear()
        assertThat(impl.wasClosed).isTrue()
    }
}
