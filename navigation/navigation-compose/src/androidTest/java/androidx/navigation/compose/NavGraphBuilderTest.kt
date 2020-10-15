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

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ContextAmbient
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class NavGraphBuilderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCurrentBackStackEntryNavigate() {
        lateinit var navController: NavController
        composeTestRule.setContent {
            navController = TestNavHostController(ContextAmbient.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            navController.graph =
                navController.createGraph(startDestination = generateId(FIRST_DESTINATION)) {
                    composable(FIRST_DESTINATION) { navBackStackEntry ->
                        TestWithArgs(navBackStackEntry.arguments?.get("test") as String)
                    }
                }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(generateId(FIRST_DESTINATION), bundleOf("test" to "arg"))
        }
    }
}

@Composable
fun TestWithArgs(arg: String) {
    assertWithMessage("args should be passed to TestWithArgs Composable")
        .that(arg)
        .isEqualTo("arg")
}

private const val FIRST_DESTINATION = 1