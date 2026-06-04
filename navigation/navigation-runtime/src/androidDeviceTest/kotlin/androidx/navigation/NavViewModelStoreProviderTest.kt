/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavViewModelStoreProviderTest {

    @Test
    fun testGetViewModelStore() {
        val graphId = UUID.randomUUID().toString()
        val provider = NavViewModelStoreProvider()
        val store = provider.get(graphId)
        assertThat(provider.get(graphId)).isSameInstanceAs(store)
    }

    @Test
    fun testClear() {
        val provider = NavViewModelStoreProvider(ViewModelStore())
        val graphId = UUID.randomUUID().toString()
        val store = provider.get(graphId)
        assertThat(store).isNotNull()

        provider.clear(graphId)
        assertThat(provider.get(graphId)).isNotSameInstanceAs(store)
    }

    @Test
    fun testOnCleared() {
        val parentStore = ViewModelStore()
        val provider = NavViewModelStoreProvider(parentStore)
        val graphId = UUID.randomUUID().toString()
        val graphStore = provider.get(graphId)
        // test clearing two viewmodel stores.
        provider.get(UUID.randomUUID().toString())

        assertThat(graphStore).isNotNull()

        parentStore.clear()
        assertThat(provider.get(graphId)).isNotSameInstanceAs(graphStore)
    }
}
