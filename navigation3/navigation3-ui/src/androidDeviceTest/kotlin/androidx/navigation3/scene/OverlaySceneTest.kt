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

package androidx.navigation3.scene

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class OverlaySceneTest {
    @get:Rule val composeTestRule = createComposeRule(StandardTestDispatcher())

    @Test
    fun testSharedTransitionWithDialogScene() {
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            SharedTransitionLayout {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    sceneStrategies = listOf(DialogSceneStrategy()),
                    sharedTransitionScope = this,
                ) {
                    when (it) {
                        first ->
                            NavEntry(first) {
                                Button(onClick = { backStack += second }) { Text(first) }
                            }

                        second ->
                            NavEntry(second, metadata = DialogSceneStrategy.dialog()) {
                                Text(second)
                            }

                        else -> error("Invalid key passed")
                    }
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).performClick()

        composeTestRule.waitForIdle()
        // Both first and second should be showing if we are on a dialog.
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
    }
}

private const val first = "first"
private const val second = "second"
