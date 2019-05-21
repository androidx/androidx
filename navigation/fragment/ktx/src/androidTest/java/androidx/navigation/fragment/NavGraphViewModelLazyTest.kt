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
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.Navigation
import androidx.navigation.navGraphViewModels
import androidx.navigation.navigation
import androidx.navigation.plusAssign
import androidx.navigation.testing.TestNavigator
import androidx.navigation.testing.test
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class NavGraphViewModelLazyTest {
    private val navController =
        NavHostController(
            ApplicationProvider.getApplicationContext() as Context
        ).apply {
            navigatorProvider += TestNavigator()
        }

    @Test
    fun vmInitialization() {
        val scenario = launchFragmentInContainer<TestVMFragment>()
        navController.setViewModelStore(ViewModelStore())
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        val navGraph = navController.navigatorProvider.navigation(
            id = GRAPH_ID,
            startDestination = DESTINATION_ID
        ) {
            test(DESTINATION_ID)
        }
        navController.setGraph(navGraph, null)

        scenario.onFragment { fragment ->
            assertThat(fragment.viewModel).isNotNull()
        }
    }
}

class TestVMFragment : Fragment() {
    val viewModel: TestViewModel by navGraphViewModels(GRAPH_ID)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return View(activity)
    }
}

class TestViewModel : ViewModel()

private const val GRAPH_ID = 1
private const val DESTINATION_ID = 2