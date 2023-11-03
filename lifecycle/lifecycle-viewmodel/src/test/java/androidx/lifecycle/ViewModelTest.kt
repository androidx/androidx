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

import java.io.Closeable
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito

@RunWith(JUnit4::class)
class ViewModelTest {

    internal class CloseableImpl : Closeable {
        var wasClosed = false
        override fun close() {
            wasClosed = true
        }
    }

    internal open class ViewModel : androidx.lifecycle.ViewModel()
    internal class ConstructorArgViewModel(closeable: Closeable) :
        androidx.lifecycle.ViewModel(closeable)

    @Test
    fun testCloseableTag() {
        val vm = ViewModel()
        val impl = CloseableImpl()
        vm.setTagIfAbsent("totally_not_coroutine_context", impl)
        vm.clear()
        assertTrue(impl.wasClosed)
    }

    @Test
    fun testCloseableTagAlreadyClearedVM() {
        val vm = ViewModel()
        vm.clear()
        val impl = CloseableImpl()
        vm.setTagIfAbsent("key", impl)
        assertTrue(impl.wasClosed)
    }

    @Test
    fun testAlreadyAssociatedKey() {
        val vm = ViewModel()
        assertThat(vm.setTagIfAbsent("key", "first"), `is`("first"))
        assertThat(vm.setTagIfAbsent("key", "second"), `is`("first"))
    }

    @Test
    fun testMockedGetTag() {
        val vm = Mockito.mock(ViewModel::class.java)
        assertThat(vm.getTag("Careless mocks =|"), nullValue())
    }

    @Test
    fun testAddCloseable() {
        val vm = ViewModel()
        val impl = CloseableImpl()
        vm.addCloseable(impl)
        vm.clear()
        assertTrue(impl.wasClosed)
    }

    @Test
    fun testAddCloseableAlreadyClearedVM() {
        val vm = ViewModel()
        vm.clear()
        val impl = CloseableImpl()
        // This shouldn't crash, even though vm already cleared
        vm.addCloseable(impl)
        assertTrue(impl.wasClosed)
    }

    @Test
    fun testConstructorCloseable() {
        val impl = CloseableImpl()
        val vm = ConstructorArgViewModel(impl)
        vm.clear()
        assertTrue(impl.wasClosed)
    }

    @Test
    fun testMockedAddCloseable() {
        val vm = Mockito.mock(ViewModel::class.java)
        val impl = CloseableImpl()
        // This shouldn't crash, even on a mocked object
        vm.addCloseable(impl)
    }
}
