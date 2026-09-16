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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Type-safe Navigation3 destinations for WindowInsetsApp. */
@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Menu : Screen

    @Serializable data object Recents : Screen

    @Serializable data object GlobalStatusBarSandbox : Screen

    @Serializable data object HorizontalPager : Screen

    @Serializable data object VerticalPager : Screen

    @Serializable data object SelfRenderedSandbox : Screen

    @Serializable data object StepperInScaffold : Screen

    @Serializable data object StepperOutOfScaffold : Screen

    @Serializable data object TimePickerInScaffold : Screen

    @Serializable data object TimePickerOutOfScaffold : Screen

    @Serializable data object DatePickerInScaffold : Screen

    @Serializable data object DatePickerOutOfScaffold : Screen

    @Serializable data object DialogInScaffold : Screen

    @Serializable data object DialogOutOfScaffold : Screen

    @Serializable data object CustomSuppressStatusBarInScaffold : Screen

    @Serializable data object CustomSuppressStatusBarOutOfScaffold : Screen

    @Serializable data object PagerWithSuppression : Screen
}

val Screen.title: String
    get() =
        when (this) {
            Screen.Menu -> "Menu"
            Screen.Recents -> "Recents"
            Screen.GlobalStatusBarSandbox -> "Global Status Bar Sandbox"
            Screen.HorizontalPager -> "Horizontal Pager"
            Screen.VerticalPager -> "Vertical Pager"
            Screen.SelfRenderedSandbox -> "Self Rendered Sandbox"
            Screen.StepperInScaffold -> "Stepper (In Scaffold)"
            Screen.StepperOutOfScaffold -> "Stepper (Out of Scaffold)"
            Screen.TimePickerInScaffold -> "TimePicker (In Scaffold)"
            Screen.TimePickerOutOfScaffold -> "TimePicker (Out of Scaffold)"
            Screen.DatePickerInScaffold -> "DatePicker (In Scaffold)"
            Screen.DatePickerOutOfScaffold -> "DatePicker (Out of Scaffold)"
            Screen.DialogInScaffold -> "Dialog (In Scaffold)"
            Screen.DialogOutOfScaffold -> "Dialog (Out of Scaffold)"
            Screen.CustomSuppressStatusBarInScaffold -> "SuppressStatusBar (In Scaffold)"
            Screen.CustomSuppressStatusBarOutOfScaffold -> "SuppressStatusBar (Out of Scaffold)"
            Screen.PagerWithSuppression -> "Pager With Suppression"
        }

class RecentsHandler(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    val recents = mutableStateListOf<Screen>()

    init {
        val saved = prefs.getString(KEY_RECENTS, "") ?: ""
        if (saved.isNotEmpty()) {
            try {
                val list = Json.decodeFromString<List<Screen>>(saved)
                recents.addAll(list)
            } catch (e: SerializationException) {
                // Ignore parsing errors from corrupted SharedPreferences data
            } catch (e: IllegalArgumentException) {
                // Ignore parsing errors from corrupted SharedPreferences data
            }
        }
    }

    fun addRecent(screen: Screen) {
        if (screen == Screen.Menu || screen == Screen.Recents) return
        recents.remove(screen)
        recents.add(0, screen)
        while (recents.size > MAX_RECENTS) recents.removeAt(MAX_RECENTS)

        val saved = Json.encodeToString(recents.toList())
        prefs.edit().putString(KEY_RECENTS, saved).apply()
    }

    companion object {
        private const val MAX_RECENTS = 20
        private const val PREF_NAME = "windowinsets_recents"
        private const val KEY_RECENTS = "recents"
    }
}

@Composable
fun WindowInsetsApp() {
    val context = LocalContext.current
    val recentsHandler = remember { RecentsHandler(context.applicationContext) }

    val backStack = rememberNavBackStack(Screen.Menu)

    val onNavigateTo: (Screen) -> Unit =
        remember(recentsHandler, backStack) {
            { screen ->
                recentsHandler.addRecent(screen)
                backStack.add(screen)
            }
        }

    NavDisplay(
        backStack = backStack,
        sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
        entryProvider =
            entryProvider {
                entry<Screen.Menu> { MenuScreen(onNavigateTo = onNavigateTo) }

                entry<Screen.Recents> {
                    RecentsScreen(recents = recentsHandler.recents, onNavigateTo = onNavigateTo)
                }

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

                entry<Screen.StepperInScaffold> {
                    StepperInScaffoldScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }

                entry<Screen.StepperOutOfScaffold> {
                    StepperOutOfScaffoldScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }

                entry<Screen.TimePickerInScaffold> {
                    TimePickerInScaffoldScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }

                entry<Screen.TimePickerOutOfScaffold> {
                    TimePickerOutOfScaffoldScreen(
                        onBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                }

                entry<Screen.DatePickerInScaffold> {
                    DatePickerInScaffoldScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }

                entry<Screen.DatePickerOutOfScaffold> {
                    DatePickerOutOfScaffoldScreen(
                        onBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                }

                entry<Screen.DialogInScaffold> {
                    DialogInScaffoldScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }

                entry<Screen.DialogOutOfScaffold> {
                    DialogOutOfScaffoldScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }

                entry<Screen.CustomSuppressStatusBarInScaffold> {
                    CustomSuppressStatusBarInScaffoldScreen(
                        onBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                }

                entry<Screen.CustomSuppressStatusBarOutOfScaffold> {
                    CustomSuppressStatusBarOutOfScaffoldScreen(
                        onBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                }

                entry<Screen.PagerWithSuppression> {
                    PagerWithSuppressionScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                }
            },
    )
}
