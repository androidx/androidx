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

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSingleDestinationSet() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(AmbientContext.current)

            NavHost(navController, startDestination = "first") {
                test("first")
            }
        }

        assertWithMessage("Destination should be added to the graph")
            .that("first" in navController.graph)
            .isTrue()
    }

    @Test
    fun testNavigate() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(AmbientContext.current)

            NavHost(navController, startDestination = "first") {
                test("first")
                test("second")
            }
        }

        assertWithMessage("Destination should be added to the graph")
            .that("first" in navController.graph)
            .isTrue()

        runOnUiThread {
            navController.navigate("second")
        }

        assertWithMessage("second destination should be current")
            .that(
                navController.currentDestination?.hasDeepLink(Uri.parse(createRoute("second")))
            ).isTrue()
    }

    @Test
    fun testNavigateOutsideStateChange() {
        lateinit var navController: NavHostController
        val text = "myButton"
        var counter = 0
        composeTestRule.setContent {
            navController = rememberNavController()
            var state by remember { mutableStateOf(0) }
            Column(Modifier.fillMaxSize()) {
                NavHost(navController, startDestination = "first") {
                    composable("first") { }
                    composable("second") { }
                }
                Button(
                    onClick = {
                        state++
                        counter = state
                    }
                ) {
                    Text(text)
                }
            }
        }

        assertWithMessage("Destination should be added to the graph")
            .that("first" in navController.graph)
            .isTrue()

        composeTestRule.runOnIdle {
            navController.navigate("second")
        }

        composeTestRule.runOnIdle {
            assertWithMessage("second destination should be current")
                .that(
                    navController.currentDestination?.hasDeepLink(Uri.parse(createRoute("second")))
                ).isTrue()
        }

        composeTestRule.onNodeWithText(text)
            .performClick()

        composeTestRule.runOnIdle {
            // ensure our click listener was fired
            assertThat(counter).isEqualTo(1)
            assertWithMessage("second destination should be current")
                .that(
                    navController.currentDestination?.hasDeepLink(Uri.parse(createRoute("second")))
                ).isTrue()
        }
    }

    @Test
    fun testPop() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(AmbientContext.current)

            NavHost(navController, startDestination = "first") {
                test("first")
                test("second")
            }
        }

        runOnUiThread {
            navController.setCurrentDestination("second")
            navController.popBackStack()
        }

        assertWithMessage("First destination should be current")
            .that(
                navController.currentDestination?.hasDeepLink(createRoute("first").toUri())
            )
            .isTrue()
    }

    @Test
    fun testChangeStartDestination() {
        lateinit var navController: TestNavHostController
        lateinit var state: MutableState<String>
        composeTestRule.setContent {
            state = remember { mutableStateOf("first") }
            val context = AmbientContext.current
            navController = remember { TestNavHostController(context) }

            NavHost(navController, startDestination = state.value) {
                test("first")
                test("second")
            }
        }

        runOnUiThread {
            state.value = "second"
        }

        composeTestRule.runOnIdle {
            assertWithMessage("First destination should be current")
                .that(
                    navController.currentDestination?.hasDeepLink(createRoute("first").toUri())
                ).isTrue()
        }
    }

    @Test
    fun testStateOfInactiveScreenIsRestoredWhenWeGoBackToIt() {
        var increment = 0
        var numberOnScreen1 = -1
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "First") {
                composable("First") {
                    numberOnScreen1 = rememberSavedInstanceState { increment++ }
                }
                composable("Second") {}
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0")
                .that(numberOnScreen1)
                .isEqualTo(0)
            numberOnScreen1 = -1
            navController.navigate("Second")
        }

        composeTestRule.runOnIdle {
            navController.popBackStack()
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be restored")
                .that(numberOnScreen1)
                .isEqualTo(0)
        }
    }

    @Test
    fun stateForScreenRemovedFromBackStackIsNotRestored() {
        var increment = 0
        var numberOnScreen2 = -1
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "First") {
                composable("First") {
                }
                composable("Second") {
                    numberOnScreen2 = rememberSavedInstanceState { increment++ }
                }
            }
        }

        composeTestRule.runOnIdle {
            navController.navigate("Second")
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0")
                .that(numberOnScreen2)
                .isEqualTo(0)
            numberOnScreen2 = -1
            navController.popBackStack()
        }

        composeTestRule.runOnIdle {
            navController.navigate("Second")
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number shouldn't be restored")
                .that(numberOnScreen2)
                .isEqualTo(1)
        }
    }
}

operator fun NavGraph.contains(
    route: String
): Boolean = findNode(createRoute(route).hashCode()) != null
