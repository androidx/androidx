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

package androidx.navigation.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.fragment.app.testing.withFragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.test.R
import androidx.navigation.navGraphViewModels
import androidx.navigation.navigation
import androidx.navigation.plusAssign
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.TestNavigator
import androidx.testutils.test
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavGraphViewModelLazyTest {
    private val navController =
        NavHostController(
            ApplicationProvider.getApplicationContext() as Context
        ).apply {
            navigatorProvider += TestNavigator()
        }

    @Suppress("DEPRECATION")
    @Test
    fun vmInitialization() {
        val scenario = launchFragmentInContainer<TestVMFragment>()
        navController.setViewModelStore(ViewModelStore())
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        val navGraph = navController.navigatorProvider.navigation(
            id = R.id.vm_graph,
            startDestination = R.id.start_destination
        ) {
            test(R.id.start_destination)
        }
        scenario.withFragment {
            navController.setGraph(navGraph, null)
        }

        scenario.onFragment { fragment ->
            assertThat(fragment.viewModel).isNotNull()
            assertThat(fragment.savedStateViewModel).isNotNull()
        }
    }

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

class NavGraphActivity : FragmentActivity(R.layout.activity_nav_graph)

class TestVMFragment : Fragment() {
    val viewModel: TestViewModel by navGraphViewModels(R.id.vm_graph)
    val savedStateViewModel: TestSavedStateViewModel by navGraphViewModels(R.id.vm_graph)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return View(activity)
    }
}

class TestViewModel : ViewModel()
class TestSavedStateViewModel(val savedStateHandle: SavedStateHandle) : ViewModel()
