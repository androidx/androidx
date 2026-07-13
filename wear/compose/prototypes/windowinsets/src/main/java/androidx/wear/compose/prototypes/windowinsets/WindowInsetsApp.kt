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

package androidx.wear.compose.prototypes.windowinsets

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import kotlinx.serialization.Serializable

/** Type-safe Navigation3 destinations for WindowInsetsApp. */
@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Menu : Screen

    @Serializable data object GlobalStatusBarSandbox : Screen

    @Serializable data object HorizontalPager : Screen

    @Serializable data object VerticalPager : Screen

    @Serializable data object SelfRenderedSandbox : Screen
}

@Composable
fun WindowInsetsApp() {
    // 1. Initialize Nav3 state-driven backstack
    val backStack = rememberNavBackStack(Screen.Menu)

    // 2. Render with NavDisplay at root + Wear Swipe-to-Dismiss Strategy
    NavDisplay(
        backStack = backStack,
        sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
        entryProvider =
            entryProvider {
                entry<Screen.Menu> { MenuScreen(backStack = backStack) }

                entry<Screen.GlobalStatusBarSandbox> {
                    GlobalStatusBarSandboxScreen(
                        onBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                }

                entry<Screen.HorizontalPager> {
                    HorizontalPagerScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }

                entry<Screen.VerticalPager> {
                    VerticalPagerScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }

                entry<Screen.SelfRenderedSandbox> {
                    SelfRenderedSandboxScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }
            },
    )
}
