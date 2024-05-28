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

package androidx.hilt.navigation.compose

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltViewModelComposeTest {

    @get:Rule val testRule = HiltAndroidRule(this)

    @get:Rule val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun verifyCurrentNavGraphViewModel() {
        lateinit var firstViewModel: SimpleViewModel
        lateinit var secondViewModel: SimpleViewModel
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "One") {
                composable("One") { firstViewModel = hiltViewModel() }
            }
            secondViewModel = hiltViewModel()
        }
        composeTestRule.waitForIdle()
        assertThat(firstViewModel).isNotNull()
        assertThat(firstViewModel.handle).isNotNull()
        assertThat(secondViewModel).isNotNull()
        assertThat(secondViewModel.handle).isNotNull()

        assertThat(firstViewModel).isNotSameInstanceAs(secondViewModel)
    }

    @Test
    fun differentViewModelAcrossRoutes() {
        lateinit var firstViewModel: SimpleViewModel
        lateinit var secondViewModel: SimpleViewModel
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "One") {
                composable("One") {
                    firstViewModel = hiltViewModel()
                    NavigateButton("Two") { navController.navigate("Two") }
                }
                composable("Two") {
                    secondViewModel = hiltViewModel()
                    NavigateButton("One") { navController.navigate("One") }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(firstViewModel).isNotNull()
        assertThat(firstViewModel.handle).isNotNull()

        composeTestRule.onNodeWithText("Navigate to Two").performClick()
        composeTestRule.waitForIdle()

        assertThat(firstViewModel).isNotSameInstanceAs(secondViewModel)
    }

    @Test
    fun sameParentViewModelAcrossRoutes() {
        lateinit var firstViewModel: SimpleViewModel
        lateinit var secondViewModel: SimpleViewModel
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "Main") {
                navigation(startDestination = "One", route = "Main") {
                    composable("One") {
                        firstViewModel = hiltViewModel(navController.getBackStackEntry("Main"))
                        NavigateButton("Two") { navController.navigate("Two") }
                    }
                    composable("Two") {
                        secondViewModel = hiltViewModel(navController.getBackStackEntry("Main"))
                        NavigateButton("One") { navController.navigate("One") }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(firstViewModel).isNotNull()
        assertThat(firstViewModel.handle).isNotNull()

        composeTestRule.onNodeWithText("Navigate to Two").performClick()
        composeTestRule.waitForIdle()

        assertThat(firstViewModel).isSameInstanceAs(secondViewModel)
    }

    @Test
    fun keyedViewModel() {
        lateinit var firstViewModel: SimpleViewModel
        lateinit var secondViewModel: SimpleViewModel
        lateinit var thirdViewModel: SimpleViewModel
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "Main") {
                composable("Main") {
                    firstViewModel = hiltViewModel(key = "One")
                    secondViewModel = hiltViewModel(key = "One")
                    thirdViewModel = hiltViewModel(key = "Two")
                }
            }
        }
        composeTestRule.waitForIdle()

        assertThat(firstViewModel).isNotNull()
        assertThat(secondViewModel).isNotNull()
        assertThat(thirdViewModel).isNotNull()
        assertThat(firstViewModel).isSameInstanceAs(secondViewModel)
        assertThat(firstViewModel).isNotSameInstanceAs(thirdViewModel)
    }

    @Test
    fun hiltViewModelScopes() {
        lateinit var navBackStackEntryScopedVM: SimpleViewModel
        lateinit var anotherNavBackStackEntryScopedVM: SimpleViewModel
        lateinit var fragmentScopedVM: SimpleViewModel
        lateinit var activityScopedVM: SimpleViewModel
        lateinit var anotherActivityScopedVM: SimpleViewModel

        val fragment = TestFragment { fragment ->
            val navController = rememberNavController()
            NavHost(navController, startDestination = "One") {
                composable("One") { navBackStackEntry ->
                    navBackStackEntryScopedVM = hiltViewModel()
                    anotherNavBackStackEntryScopedVM = hiltViewModel(navBackStackEntry)
                    fragmentScopedVM = hiltViewModel(fragment)
                    activityScopedVM = hiltViewModel(composeTestRule.activity)
                    anotherActivityScopedVM = hiltViewModel(composeTestRule.activity)
                }
            }
        }

        composeTestRule.runOnUiThread {
            val view = FragmentContainerView(composeTestRule.activity)
            view.id = 100
            composeTestRule.activity.setContentView(view)
            composeTestRule.activity.supportFragmentManager
                .beginTransaction()
                .replace(100, fragment)
                .commit()
        }

        composeTestRule.waitForIdle()

        assertThat(navBackStackEntryScopedVM).isNotNull()
        assertThat(navBackStackEntryScopedVM.handle).isNotNull()
        assertThat(navBackStackEntryScopedVM.logger).isNotNull()

        assertThat(anotherNavBackStackEntryScopedVM).isNotNull()
        assertThat(anotherNavBackStackEntryScopedVM.handle).isNotNull()
        assertThat(anotherNavBackStackEntryScopedVM.logger).isNotNull()
        assertThat(anotherNavBackStackEntryScopedVM).isSameInstanceAs(navBackStackEntryScopedVM)

        assertThat(fragmentScopedVM).isNotNull()
        assertThat(fragmentScopedVM.handle).isNotNull()
        assertThat(fragmentScopedVM.logger).isNotNull()
        assertThat(navBackStackEntryScopedVM).isNotSameInstanceAs(fragmentScopedVM)

        assertThat(activityScopedVM).isNotNull()
        assertThat(activityScopedVM.handle).isNotNull()
        assertThat(activityScopedVM.logger).isNotNull()
        assertThat(activityScopedVM).isNotSameInstanceAs(navBackStackEntryScopedVM)
        assertThat(activityScopedVM).isNotSameInstanceAs(fragmentScopedVM)

        assertThat(anotherActivityScopedVM).isNotNull()
        assertThat(anotherActivityScopedVM.handle).isNotNull()
        assertThat(anotherActivityScopedVM.logger).isNotNull()
        assertThat(anotherActivityScopedVM).isSameInstanceAs(activityScopedVM)
    }

    @Test
    fun hiltViewModelFactory() {
        lateinit var firstFactory: ViewModelProvider.Factory
        lateinit var secondFactory: ViewModelProvider.Factory
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "Main") {
                composable("Main") { navBackStackEntry ->
                    firstFactory = HiltViewModelFactory(LocalContext.current, navBackStackEntry)
                    secondFactory =
                        HiltViewModelFactory(
                            LocalContext.current,
                            navBackStackEntry.defaultViewModelProviderFactory
                        )
                }
            }
        }
        composeTestRule.waitForIdle()

        assertThat(firstFactory).isNotNull()
        assertThat(secondFactory).isNotNull()
        assertThat(firstFactory).isNotSameInstanceAs(secondFactory)
    }

    @Test
    fun hiltViewModelAssisted() {
        lateinit var viewModel: SimpleAssistedViewModel
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "Main") {
                composable("Main") {
                    viewModel =
                        hiltViewModel<SimpleAssistedViewModel, SimpleAssistedViewModel.Factory> {
                            it.create(42)
                        }
                }
            }
        }
        composeTestRule.waitForIdle()

        assertThat(viewModel).isNotNull()
        assertThat(viewModel.i).isEqualTo(42)
    }

    @Composable
    private fun NavigateButton(text: String, listener: () -> Unit = {}) {
        Button(onClick = listener) { Text(text = "Navigate to $text") }
    }

    @AndroidEntryPoint class TestActivity : FragmentActivity()

    @HiltViewModel
    class SimpleViewModel
    @Inject
    constructor(
        val handle: SavedStateHandle,
        val logger: MyLogger,
        // TODO(kuanyingchou) Remove this after https://github.com/google/dagger/issues/3601 is
        //  resolved.
        @ApplicationContext val context: Context
    ) : ViewModel()

    @HiltViewModel(assistedFactory = SimpleAssistedViewModel.Factory::class)
    class SimpleAssistedViewModel
    @AssistedInject
    constructor(
        val handle: SavedStateHandle,
        val logger: MyLogger,
        // TODO(kuanyingchou) Remove this after https://github.com/google/dagger/issues/3601 is
        //  resolved.
        @ApplicationContext val context: Context,
        @Assisted val i: Int
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(i: Int): SimpleAssistedViewModel
        }
    }

    class TestFragment(val composable: @Composable (Fragment) -> Unit) : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return ComposeView(requireContext()).apply {
                setContent { composable(this@TestFragment) }
            }
        }
    }

    class MyLogger @Inject constructor()
}
