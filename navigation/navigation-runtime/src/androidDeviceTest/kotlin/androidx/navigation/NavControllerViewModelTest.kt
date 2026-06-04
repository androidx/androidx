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
class NavControllerViewModelTest {

    @Test
    fun testGetViewModelStore() {
        val navGraphId = UUID.randomUUID().toString()
        val viewModel = NavControllerViewModel.getInstance(ViewModelStore())
        val viewModelStore = viewModel.get(navGraphId)
        assertThat(viewModel.get(navGraphId)).isSameInstanceAs(viewModelStore)
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
        val viewModelStore = viewModel.get(navGraphId)
        assertThat(viewModelStore).isNotNull()

        viewModel.clear(navGraphId)
        assertThat(viewModel.get(navGraphId)).isNotSameInstanceAs(viewModelStore)
    }

    @Test
    fun testOnCleared() {
        val baseViewModelStore = ViewModelStore()
        val viewModel = NavControllerViewModel.getInstance(baseViewModelStore)
        val navGraphId = UUID.randomUUID().toString()
        val navGraphViewModelStore = viewModel.get(navGraphId)
        // test clearing two viewmodel stores.
        viewModel.get(UUID.randomUUID().toString())

        assertThat(navGraphViewModelStore).isNotNull()

        baseViewModelStore.clear()
        assertThat(viewModel.get(navGraphId)).isNotSameInstanceAs(navGraphViewModelStore)
    }
}
