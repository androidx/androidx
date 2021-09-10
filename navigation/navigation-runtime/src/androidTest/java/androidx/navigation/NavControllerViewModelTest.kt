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
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavControllerViewModelTest {

    @Test
    fun testGetViewModelStore() {
        val navGraphId = UUID.randomUUID().toString()
        val viewModel = NavControllerViewModel()
        val viewModelStore = viewModel.getViewModelStore(navGraphId)
        assertThat(viewModel.getViewModelStore(navGraphId)).isSameInstanceAs(viewModelStore)
    }

    @Test
    fun testGetInstance() {
        val viewModelStore = ViewModelStore()
        val viewModel = NavControllerViewModel.getInstance(viewModelStore)
        assertThat(NavControllerViewModel.getInstance(viewModelStore)).isSameInstanceAs(viewModel)
    }

    @Test
    fun testClear() {
        val viewModel = NavControllerViewModel.getInstance(ViewModelStore())
        val navGraphId = UUID.randomUUID().toString()
        val viewModelStore = viewModel.getViewModelStore(navGraphId)
        assertThat(viewModelStore).isNotNull()

        viewModel.clear(navGraphId)
        assertThat(viewModel.getViewModelStore(navGraphId)).isNotSameInstanceAs(viewModelStore)
    }

    @Test
    fun testOnCleared() {
        val baseViewModelStore = ViewModelStore()
        val viewModel = NavControllerViewModel.getInstance(baseViewModelStore)
        val navGraphId = UUID.randomUUID().toString()
        val navGraphViewModelStore = viewModel.getViewModelStore(navGraphId)
        // test clearing two viewmodel stores.
        viewModel.getViewModelStore(UUID.randomUUID().toString())

        assertThat(navGraphViewModelStore).isNotNull()

        baseViewModelStore.clear()
        assertThat(viewModel.getViewModelStore(navGraphId)).isNotSameInstanceAs(
            navGraphViewModelStore
        )
    }
}
