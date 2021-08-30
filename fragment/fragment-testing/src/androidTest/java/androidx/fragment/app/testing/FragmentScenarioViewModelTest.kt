/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.fragment.app.testing

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

/**
 * End-to-end integration test using a mock [ViewModel] injected in via a
 * [ViewModelProvider.Factory] instance, allowing us to test the Fragment in isolation
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentScenarioViewModelTest {

    @Test
    fun injectViewModelFactoryWithMocks() {
        // Set up a mock ViewModel and a ViewModelProvider.Factory that provides it
        val mockViewModel = mock(InjectedViewModel::class.java)
        val fakeUserName = "test"
        `when`(mockViewModel.getUserName()).thenReturn(fakeUserName)
        val viewModelFactory = object : ViewModelProvider.NewInstanceFactory() {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    InjectedViewModel::class.java -> mockViewModel as T
                    else -> super.create(modelClass)
                }
            }
        }
        // Launch the Fragment with our ViewModelProvider.Factory
        with(
            launchFragment {
                InjectedViewModelFactoryFragment(viewModelFactory)
            }
        ) {
            onFragment { fragment ->
                assertThat(fragment.viewModel)
                    .isSameInstanceAs(mockViewModel)
                assertThat(fragment.viewModel.getUserName())
                    .isEqualTo(fakeUserName)
            }
            // Ensure that the ViewModel survives recreation
            recreate()
            onFragment { fragment ->
                assertThat(fragment.viewModel)
                    .isSameInstanceAs(mockViewModel)
                assertThat(fragment.viewModel.getUserName())
                    .isEqualTo(fakeUserName)
            }
        }
    }
}

open class InjectedViewModel : ViewModel() {
    /**
     * This would normally be more complicated logic or return a LiveData instance
     */
    open fun getUserName() = ""
}

class InjectedViewModelFactoryFragment(
    private val viewModelFactory: ViewModelProvider.Factory
) : Fragment() {
    val viewModel: InjectedViewModel by viewModels { viewModelFactory }
}
