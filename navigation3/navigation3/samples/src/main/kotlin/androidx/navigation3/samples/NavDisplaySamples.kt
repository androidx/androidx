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

package androidx.navigation3.samples

import androidx.activity.compose.BackHandler
import androidx.annotation.Sampled
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavLocalProvider
import androidx.navigation3.NavBackStackProvider
import androidx.navigation3.NavEntry
import androidx.navigation3.NavLocalProvider
import androidx.navigation3.SavedStateNavLocalProvider
import androidx.navigation3.SinglePaneNavDisplay
import androidx.navigation3.entry
import androidx.navigation3.entryProvider

class ProfileViewModel : ViewModel() {
    val name = "no user"
}

@Sampled
@Composable
fun BaseNav() {
    val backStack = rememberMutableStateListOf(Profile)
    val showDialog = remember { mutableStateOf(false) }
    SinglePaneNavDisplay(
        backStack = backStack,
        localProviders = listOf(SavedStateNavLocalProvider, ViewModelStoreNavLocalProvider),
        onBack = { backStack.removeLast() },
        entryProvider =
            entryProvider({ NavEntry(Unit) { Text(text = "Invalid Key") } }) {
                entry<Profile>(
                    SinglePaneNavDisplay.transition(
                        slideInHorizontally { it },
                        slideOutHorizontally { it }
                    )
                ) {
                    val viewModel = viewModel<ProfileViewModel>()
                    Profile(viewModel, { backStack.add(it) }) { backStack.removeLast() }
                }
                entry<Scrollable>(
                    SinglePaneNavDisplay.transition(
                        slideInHorizontally { it },
                        slideOutHorizontally { it }
                    )
                ) {
                    Scrollable({ backStack.add(it) }) { backStack.removeLast() }
                }
                entry<DialogBase> {
                    DialogBase(onClick = { showDialog.value = true }) { backStack.removeLast() }
                }
                entry<Dashboard>(
                    SinglePaneNavDisplay.transition(
                        slideInHorizontally { it },
                        slideOutHorizontally { it }
                    )
                ) { dashboardArgs ->
                    val userId = dashboardArgs.userId
                    Dashboard(userId, onBack = { backStack.removeLast() })
                }
            }
    )
    if (showDialog.value) {
        DialogContent(onDismissRequest = { showDialog.value = false })
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Sampled
@Composable
fun <T : Any> NavSharedElementSample() {
    val backStack = rememberMutableStateListOf(CatList)
    SharedTransitionLayout {
        SinglePaneNavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLast() },
            entryProvider =
                entryProvider {
                    entry<CatList> {
                        CatList(this@SharedTransitionLayout) { cat ->
                            backStack.add(CatDetail(cat))
                        }
                    }
                    entry<CatDetail> { args ->
                        CatDetail(args.cat, this@SharedTransitionLayout) { backStack.removeLast() }
                    }
                }
        )
    }
}

@Sampled
@Composable
fun <T : Any> CustomBasicDisplay(
    backstack: List<T>,
    modifier: Modifier = Modifier,
    localProviders: List<NavLocalProvider> = emptyList(),
    onBack: () -> Unit = { if (backstack is MutableList) backstack.removeAt(backstack.size - 1) },
    entryProvider: (key: T) -> NavEntry<out T>
) {
    BackHandler(backstack.size > 1, onBack)
    NavBackStackProvider(backstack, entryProvider, localProviders) { entries ->
        val entry = entries.last()
        Box(modifier = modifier) { entry.content.invoke(entry.key) }
    }
}
