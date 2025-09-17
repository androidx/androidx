/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigation3.runtime

import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SavedStateNavEntryDecoratorTest {

    @get:Rule val composeTestRule = createComposeRule()

    data class Data1(val arg: Int)

    data class Data2(val arg: Int)

    @Test
    fun testDataClassSamePropertyNotDuplicates() {
        val data1 = Data1(1)
        val data2 = Data2(2)

        lateinit var numberOnScreen1: MutableState<Int>
        val backStack = mutableStateListOf<Any>(data1)
        composeTestRule.setContent {
            val entries =
                rememberDecoratedNavEntries(
                    backStack = backStack,
                    entryDecorators = listOf(rememberSavedStateNavEntryDecorator()),
                    entryProvider = {
                        when (it) {
                            is Data1 ->
                                NavEntry(data1) {
                                    numberOnScreen1 = rememberSaveable { mutableStateOf(0) }
                                    Text("numberOnScreen1: ${numberOnScreen1.value}")
                                }
                            is Data2 ->
                                NavEntry(data2) {
                                    numberOnScreen1 = rememberSaveable { mutableStateOf(0) }
                                    Text("numberOnScreen1: ${numberOnScreen1.value}")
                                }
                            else -> error("Unknown key")
                        }
                    },
                )

            entries.lastOrNull()?.Content()
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(0)
            numberOnScreen1.value++
            numberOnScreen1.value++
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 2").isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(data2) }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(0)
            numberOnScreen1.value++
            numberOnScreen1.value++
            numberOnScreen1.value++
            numberOnScreen1.value++
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 4").isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.removeAt(backStack.size - 1) }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be restored")
                .that(numberOnScreen1.value)
                .isEqualTo(2)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 2").isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(data2) }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(0)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 0").isDisplayed()).isTrue()
    }
}
