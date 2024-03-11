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

    //region constructor
    @Test
    fun constructor_withCloseables_doesNotClose() {
        val resource1 = CloseableResource()
        val resource2 = CloseableResource()
        TestViewModel(resource1, resource2)

        assertThat(resource1.isClosed).isFalse()
        assertThat(resource2.isClosed).isFalse()
    }

    @Test
    fun constructor_withCloseables_onClear_closesResources() {
        val resource1 = CloseableResource()
        val resource2 = CloseableResource()
        val viewModel = TestViewModel(resource1, resource2)

        viewModel.clear()

        assertThat(resource1.isClosed).isTrue()
        assertThat(resource2.isClosed).isTrue()
    }

    @Test
    fun constructor_withDuplicatedCloseables_onClear_closesResources() {
        val resource = CloseableResource()
        val viewModel = TestViewModel(resource)
        viewModel.addCloseable(resource)

        viewModel.clear()

        assertThat(resource.isClosed).isTrue()
    }
    //endregion

    //region addCloseable without keys
    @Test
    fun addCloseable_doesNotClose() {
        val viewModel = TestViewModel()
        val resource = CloseableResource()

        viewModel.addCloseable(resource)

        assertThat(resource.isClosed).isFalse()
    }

    @Test
    fun addCloseable_onClear_closesResource() {
        val viewModel = TestViewModel()
        val resource = CloseableResource()

        viewModel.addCloseable(resource)
        viewModel.clear()

        assertThat(resource.isClosed).isTrue()
    }

    @Test
    fun addCloseable_afterCleared_closesResource() {
        val viewModel = TestViewModel()
        viewModel.clear()

        val resource = CloseableResource()
        viewModel.addCloseable(resource)

        assertThat(resource.isClosed).isTrue()
    }
    //endregion

    //region addCloseable with keys
    @Test
    fun addCloseable_withKey_doesNotClose() {
        val viewModel = TestViewModel()
        val expectedResource = CloseableResource()

        viewModel.addCloseable(key = "key", expectedResource)
        val actualResource = viewModel.getCloseable<CloseableResource>(key = "key")

        assertThat(actualResource).isEqualTo(expectedResource)
        assertThat(actualResource!!.isClosed).isFalse()
    }

    @Test
    fun addCloseable_withKey_onClear_closesResource() {
        val viewModel = TestViewModel()
        val resource = CloseableResource()

        viewModel.addCloseable(key = "totally_not_coroutine_context", resource)
        viewModel.clear()

        assertThat(resource.isClosed).isTrue()
    }

    @Test
    fun addCloseable_withKey_afterCleared_closesResource() {
        val viewModel = TestViewModel()
        viewModel.clear()

        val resource = CloseableResource()
        viewModel.addCloseable(key = "key", resource)

        assertThat(resource.isClosed).isTrue()
    }

    @Test
    fun addCloseable_withDuplicatedKey_replacesPrevious() {
        val viewModel = TestViewModel()
        val resource1 = CloseableResource()
        val resource2 = CloseableResource()

        viewModel.addCloseable(key = "key", resource1)
        viewModel.addCloseable(key = "key", resource2)

        val actualCloseable = viewModel.getCloseable<CloseableResource>(key = "key")
        assertThat(actualCloseable).isNotEqualTo(resource1)
        assertThat(actualCloseable).isEqualTo(resource2)
    }

    @Test
    fun addCloseable_withDuplicatedKey_closesPrevious() {
        val viewModel = TestViewModel()
        val resource1 = CloseableResource()
        val resource2 = CloseableResource()

        viewModel.addCloseable(key = "key", resource1)
        viewModel.addCloseable(key = "key", resource2)

        assertThat(resource1.isClosed).isTrue()
        assertThat(resource2.isClosed).isFalse()
    }
    //endregion

    //region test helpers
    private class TestViewModel(vararg closeables: AutoCloseable) : ViewModel(*closeables)

    private class CloseableResource(var isClosed: Boolean = false) : AutoCloseable {
        override fun close() {
            isClosed = true
        }
    }
    //endregion
}
