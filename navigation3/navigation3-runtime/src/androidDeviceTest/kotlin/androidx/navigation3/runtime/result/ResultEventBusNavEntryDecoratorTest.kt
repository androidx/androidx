/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3.runtime.result

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import kotlin.test.Test
import kotlinx.serialization.Serializable
import org.junit.Rule

class ResultEventBusNavEntryDecoratorTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testResultEventBusBetweenTwoEntries() {
        lateinit var backStack: NavBackStack<NavKey>
        composeTestRule.setContent {
            backStack = rememberNavBackStack(First)

            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize().wrapContentSize(),
                entryDecorators = listOf(rememberResultEventBusNavEntryDecorator()),
                onBack = { backStack.removeLastOrNull() },
                entryProvider =
                    entryProvider {
                        entry<First> {
                            var result by remember { mutableStateOf(noResult) }
                            ResultEffect<String> { result = it }
                            Text(result)
                        }
                        entry<Second> {
                            val resultBus = LocalResultEventBus.current
                            Button(onClick = { resultBus.sendResult<String>(result = setResult) }) {
                                Text(sendResult)
                            }
                        }
                    },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(noResult).assertIsDisplayed()

        composeTestRule.runOnIdle { backStack.add(Second) }

        composeTestRule.waitForIdle()

        // Send Result
        composeTestRule.onNodeWithText(sendResult).performClick()

        composeTestRule.runOnIdle { backStack.removeLastOrNull() }

        composeTestRule.waitForIdle()

        // Verify Result
        composeTestRule.onNodeWithText(setResult).assertIsDisplayed()
    }

    @Test
    fun testResultEventBusWithDialog() {
        lateinit var backStack: NavBackStack<NavKey>
        composeTestRule.setContent {
            backStack = rememberNavBackStack(First)
            val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

            NavDisplay(
                backStack = backStack,
                entryDecorators = listOf(rememberResultEventBusNavEntryDecorator()),
                onBack = { backStack.removeLastOrNull() },
                sceneStrategies = listOf(dialogStrategy),
                entryProvider =
                    entryProvider {
                        entry<First> {
                            var result by remember { mutableStateOf(noResult) }
                            ResultEffect<String> { result = it }
                            Text(result)
                        }
                        entry<Dialog>(metadata = DialogSceneStrategy.dialog()) {
                            val resultEventBus = LocalResultEventBus.current
                            Button(
                                onClick = {
                                    resultEventBus.sendResult<String>(result = resultFromDialog)
                                }
                            ) {
                                Text(sendResult)
                            }
                        }
                    },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(noResult).assertIsDisplayed()

        composeTestRule.runOnIdle { backStack.add(Dialog) }

        composeTestRule.waitForIdle()

        // Send Result
        composeTestRule.onNodeWithText(sendResult).performClick()

        composeTestRule.runOnIdle { backStack.removeLastOrNull() }

        composeTestRule.waitForIdle()

        // Verify Result
        composeTestRule.onNodeWithText(resultFromDialog).assertIsDisplayed()
    }

    @Test
    fun testConflateAsStateBetweenTwoEntries() {
        lateinit var backStack: NavBackStack<NavKey>
        composeTestRule.setContent {
            backStack = rememberNavBackStack(First)

            NavDisplay(
                backStack = backStack,
                entryDecorators = listOf(rememberResultEventBusNavEntryDecorator()),
                onBack = { backStack.removeLastOrNull() },
                entryProvider =
                    entryProvider {
                        entry<First> {
                            val result =
                                LocalResultEventBus.current.conflateAsState<String>(noResult).value
                            Text(result)
                        }
                        entry<Second> {
                            val resultEventBus = LocalResultEventBus.current
                            Button(
                                onClick = { resultEventBus.sendResult<String>(result = setResult) }
                            ) {
                                Text(sendResult)
                            }
                        }
                    },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(noResult).assertIsDisplayed()

        composeTestRule.runOnIdle { backStack.add(Second) }

        composeTestRule.waitForIdle()

        // Send Result
        composeTestRule.onNodeWithText(sendResult).performClick()

        composeTestRule.runOnIdle { backStack.removeLastOrNull() }

        composeTestRule.waitForIdle()

        // Verify Result
        composeTestRule.onNodeWithText(setResult).assertIsDisplayed()
    }

    @Test
    fun testConflateAsStateWithDialog() {
        lateinit var backStack: NavBackStack<NavKey>
        composeTestRule.setContent {
            backStack = rememberNavBackStack(First)
            val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

            NavDisplay(
                backStack = backStack,
                entryDecorators = listOf(rememberResultEventBusNavEntryDecorator()),
                onBack = { backStack.removeLastOrNull() },
                sceneStrategies = listOf(dialogStrategy),
                entryProvider =
                    entryProvider {
                        entry<First> {
                            val result =
                                LocalResultEventBus.current.conflateAsState<String>(noResult).value
                            Text(result)
                        }
                        entry<Dialog>(metadata = DialogSceneStrategy.dialog()) {
                            val resultEventBus = LocalResultEventBus.current
                            Button(
                                onClick = {
                                    resultEventBus.sendResult<String>(result = resultFromDialog)
                                }
                            ) {
                                Text(sendResult)
                            }
                        }
                    },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(noResult).assertIsDisplayed()

        composeTestRule.runOnIdle { backStack.add(Dialog) }

        composeTestRule.waitForIdle()

        // Send Result
        composeTestRule.onNodeWithText(sendResult).performClick()

        composeTestRule.runOnIdle { backStack.removeLastOrNull() }

        composeTestRule.waitForIdle()

        // Verify Result
        composeTestRule.onNodeWithText(resultFromDialog).assertIsDisplayed()
    }
}

@Serializable internal data object First : NavKey

@Serializable internal data object Second : NavKey

@Serializable internal data object Dialog : NavKey

internal const val noResult = "No Result"
internal const val setResult = "Result is Set"
internal const val resultFromDialog = "Result from Dialog"
internal const val sendResult = "Send Result"
