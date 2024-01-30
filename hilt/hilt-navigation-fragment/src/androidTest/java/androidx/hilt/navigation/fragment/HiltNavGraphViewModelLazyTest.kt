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

import android.content.Context
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
import androidx.navigation.navGraphViewModels
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
            val hiltSavedStateViewModel = withActivity { firstFragment.hiltSavedStateViewModel }
            assertThat(viewModel).isNotNull()
            assertThat(savedStateViewModel).isNotNull()
            assertThat(hiltSavedStateViewModel).isNotNull()
            assertThat(hiltSavedStateViewModel.otherDep).isNotNull()

            // First assert that the initial value is null. Note that we won't get the
            // default value from nav args passed to the destination as this viewmodel
            // is scoped to parent.
            val initialState: String? = savedStateViewModel.savedStateHandle["test"]
            assertThat(initialState).isNull()
            val hiltInitialState: String? = hiltSavedStateViewModel.savedStateHandle["test"]
            assertThat(hiltInitialState).isNull()

            // Now set arguments
            savedStateViewModel.savedStateHandle.set("test", "test")
            hiltSavedStateViewModel.savedStateHandle.set("test", "test")

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
            assertThat(secondFragment.hiltSavedStateViewModel)
                .isSameInstanceAs(hiltSavedStateViewModel)
            val savedValue: String? = secondFragment.savedStateViewModel
                .savedStateHandle["test"]
            assertThat(savedValue).isEqualTo("test")

            // Now recreate the Activity and ensure that when we
            // first request the nav graph ViewModel on the second destination
            // that we get the same ViewModel and data back. Note that this is
            // different to process deaths and the viewmodels are not recreated.
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
            assertThat(recreatedFragment.hiltSavedStateViewModel)
                .isSameInstanceAs(hiltSavedStateViewModel)
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
    val savedStateViewModel: TestSavedStateViewModel by navGraphViewModels(R.id.vm_graph)
    val hiltSavedStateViewModel: TestHiltSavedStateViewModel
            by hiltNavGraphViewModels(R.id.vm_graph)
    // TODO(kuanyingchou) Remove this after https://github.com/google/dagger/issues/3601 is resolved
    @Inject @ApplicationContext
    lateinit var applicationContext: Context

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

class TestSavedStateViewModel constructor(
    val savedStateHandle: SavedStateHandle
) : ViewModel()

@HiltViewModel
class TestHiltSavedStateViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    val otherDep: OtherDep
) : ViewModel()

class OtherDep @Inject constructor()
