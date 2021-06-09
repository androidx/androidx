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

import androidx.activity.ComponentActivity
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltViewModelComposeTest {

    @get:Rule
    val testRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @FlakyTest(bugId = 190539286)
    @Test
    fun verifyCurrentNavGraphViewModel() {
        lateinit var firstViewModel: SimpleViewModel
        lateinit var secondViewModel: SimpleViewModel
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "One") {
                composable("One") {
                    firstViewModel = hiltViewModel()
                }
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

    @FlakyTest(bugId = 190539286)
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
                        firstViewModel = hiltViewModel(
                            navController.getBackStackEntry("Main")
                        )
                        NavigateButton("Two") { navController.navigate("Two") }
                    }
                    composable("Two") {
                        secondViewModel = hiltViewModel(
                            navController.getBackStackEntry("Main")
                        )
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

    @Composable
    private fun NavigateButton(text: String, listener: () -> Unit = { }) {
        Button(onClick = listener) {
            Text(text = "Navigate to $text")
        }
    }

    @AndroidEntryPoint
    class TestActivity : ComponentActivity()

    @HiltViewModel
    class SimpleViewModel @Inject constructor(
        val handle: SavedStateHandle,
        val logger: MyLogger
    ) : ViewModel()

    class MyLogger @Inject constructor()
}