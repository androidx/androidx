/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.hilt.navigation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.fragment.test.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/*
 * Copyright 2021 The Android Open Source Project
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

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltNavGraphViewModelLazyTest {

    @get:Rule
    val testRule = HiltAndroidRule(this)

    @Test
    fun sameViewModelAcrossFragments() {
        with(ActivityScenario.launch(NavGraphActivity::class.java)) {
            val navController = withActivity { findNavController(R.id.nav_host_fragment) }
            val firstFragment: TestVMFragment = withActivity {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment)!!
                navHostFragment.childFragmentManager.primaryNavigationFragment as TestVMFragment
            }
            val viewModel = withActivity { firstFragment.viewModel }
            val savedStateViewModel = withActivity { firstFragment.savedStateViewModel }
            assertThat(viewModel).isNotNull()
            assertThat(savedStateViewModel).isNotNull()

            // First assert that we don't have any default value since the
            // navigation graph wasn't sent any arguments
            val initialState: String? = savedStateViewModel.savedStateHandle["test"]
            assertThat(initialState).isNull()

            // Now set arguments
            savedStateViewModel.savedStateHandle.set("test", "test")

            // Navigate to the second destination and ensure it
            // gets the same ViewModels and data
            withActivity {
                navController.navigate(R.id.second_destination)
            }
            val secondFragment: TestVMFragment = withActivity {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment)!!
                navHostFragment.childFragmentManager.primaryNavigationFragment as TestVMFragment
            }
            assertThat(secondFragment.viewModel)
                .isSameInstanceAs(viewModel)
            assertThat(secondFragment.savedStateViewModel)
                .isSameInstanceAs(savedStateViewModel)
            val savedValue: String? = secondFragment.savedStateViewModel
                .savedStateHandle["test"]
            assertThat(savedValue).isEqualTo("test")

            // Now recreate the Activity and ensure that when we
            // first request the nav graph ViewModel on the second destination
            // that we get the same ViewModel and data back
            recreate()
            val recreatedFragment: TestVMFragment = withActivity {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment)!!
                navHostFragment.childFragmentManager.primaryNavigationFragment as TestVMFragment
            }
            assertThat(recreatedFragment.viewModel)
                .isSameInstanceAs(viewModel)
            assertThat(recreatedFragment.savedStateViewModel)
                .isSameInstanceAs(savedStateViewModel)
            val recreatedValue: String? = recreatedFragment.savedStateViewModel
                .savedStateHandle["test"]
            assertThat(recreatedValue).isEqualTo("test")
        }
    }
}

@AndroidEntryPoint
class NavGraphActivity : FragmentActivity(R.layout.activity_nav_graph)

@AndroidEntryPoint
class TestVMFragment : Fragment() {
    val viewModel: TestViewModel by hiltNavGraphViewModels(R.id.vm_graph)
    val savedStateViewModel: TestSavedStateViewModel by hiltNavGraphViewModels(R.id.vm_graph)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return View(activity)
    }
}

@HiltViewModel
class TestViewModel @Inject constructor() : ViewModel()

@HiltViewModel
class TestSavedStateViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle
) : ViewModel()
