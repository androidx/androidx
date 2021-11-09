/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation.compose

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.createGraph
import androidx.navigation.get
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.TestNavigator
import androidx.testutils.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCurrentBackStackEntrySetGraph() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        composeTestRule.setContent {
            val navController = rememberNavController(remember { TestNavigator() })

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)
    }

    @Test
    fun testCurrentBackStackEntryNavigate() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        composeTestRule.setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION)
        }

        assertWithMessage("the currentBackStackEntry should be after navigate")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(SECOND_DESTINATION)
    }

    @Test
    fun testCurrentBackStackEntryPop() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION)
            navController.popBackStack()
        }

        assertWithMessage("the currentBackStackEntry should return to first destination after pop")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)
    }

    @Test
    fun testNavigateThenNavigateWithPop() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        composeTestRule.setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        val navigator = navController.navigatorProvider[TestNavigator::class]

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)
        assertThat(navigator.backStack.size).isEqualTo(1)

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION) {
                popUpTo("first") { inclusive = true }
            }
        }

        assertWithMessage("the currentBackStackEntry should be after navigate")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(SECOND_DESTINATION)
        assertWithMessage("the second destination should be the only one on the back stack")
            .that(navigator.backStack.size)
            .isEqualTo(1)
    }

    @Test
    fun testNavigateOptionSingleTop() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        composeTestRule.setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.destination?.route)
            .isEqualTo(FIRST_DESTINATION)
        assertThat(navigator.backStack.size).isEqualTo(1)

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION)
        }

        assertWithMessage("there should be 2 destinations on the back stack after navigate")
            .that(navigator.backStack.size)
            .isEqualTo(2)

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION) {
                launchSingleTop = true
            }
        }

        assertWithMessage("there should be 2 destination on back stack when using singleTop")
            .that(navigator.backStack.size)
            .isEqualTo(2)
    }

    @Test
    fun testNavigateOptionSingleTopDifferentArguments() {
        var value = ""
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first?arg={arg}") {
                composable("first?arg={arg}") { entry ->
                    if (entry.arguments?.containsKey("arg") == true) {
                        value = entry.arguments?.getString("arg", "").toString()
                    }
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate("first?arg=value2") {
                launchSingleTop = true
            }
        }
        composeTestRule.runOnIdle {
            val navigator = navController.navigatorProvider.get<ComposeNavigator>(
                navController.currentDestination?.navigatorName!!
            )
            assertWithMessage("there should be 1 destination on back stack when using singleTop")
                .that(navigator.backStack.value.size)
                .isEqualTo(1)
            assertThat(value).isEqualTo("value2")
        }
    }

    @Test
    fun testGetBackStackEntry() {
        lateinit var navController: NavController
        composeTestRule.setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION)
        }

        assertWithMessage("first destination should be on back stack")
            .that(
                navController.getBackStackEntry(FIRST_DESTINATION).destination.route
            )
            .isEqualTo(FIRST_DESTINATION)

        assertWithMessage("second destination should be on back stack")
            .that(
                navController.getBackStackEntry(SECOND_DESTINATION).destination.route
            )
            .isEqualTo(SECOND_DESTINATION)
    }

    @Test
    fun testGetBackStackEntryNoEntryFound() {
        lateinit var navController: NavController
        composeTestRule.setContent {
            navController = rememberNavController(remember { TestNavigator() })

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION)
        }

        try {
            navController.getBackStackEntry(SECOND_DESTINATION)
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "No destination with route $SECOND_DESTINATION is on the NavController's " +
                        "back stack. The current destination is " +
                        navController.currentBackStackEntry?.destination
                )
        }
    }
}

private const val FIRST_DESTINATION = "first"
private const val SECOND_DESTINATION = "second"
