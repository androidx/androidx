/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavBackStackProviderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun callWrapperFunctions() {
        var calledWrapBackStack = false
        var calledWrapContent = false
        val provider =
            object : NavLocalProvider {
                @Composable
                override fun ProvideToBackStack(
                    backStack: List<Any>,
                    content: @Composable () -> Unit
                ) {
                    calledWrapBackStack = true
                    content.invoke()
                }

                @Composable
                override fun <T : Any> ProvideToEntry(entry: NavEntry<T>) {
                    calledWrapContent = true
                }
            }

        composeTestRule.setContent {
            NavBackStackProvider(
                backStack = listOf("something"),
                localProviders = listOf(provider),
                entryProvider = { NavEntry("something") {} }
            ) { records ->
                records.last().content.invoke("something")
            }
        }

        assertThat(calledWrapBackStack).isTrue()
        assertThat(calledWrapContent).isTrue()
    }

    @Test
    fun callWrapperFunctionsOnce() {
        var calledWrapBackStackCount = 0
        var calledWrapContentCount = 0
        val provider =
            object : NavLocalProvider {
                @Composable
                override fun ProvideToBackStack(
                    backStack: List<Any>,
                    content: @Composable () -> Unit
                ) {
                    calledWrapBackStackCount++
                    content.invoke()
                }

                @Composable
                override fun <T : Any> ProvideToEntry(entry: NavEntry<T>) {
                    calledWrapContentCount++
                }
            }

        composeTestRule.setContent {
            NavBackStackProvider(
                backStack = listOf("something"),
                localProviders = listOf(provider, provider),
                entryProvider = { NavEntry("something") {} }
            ) { records ->
                records.last().content.invoke("something")
            }
        }

        assertThat(calledWrapBackStackCount).isEqualTo(1)
        assertThat(calledWrapContentCount).isEqualTo(1)
    }
}
