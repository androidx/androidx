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

package androidx.compose.material3.integration.a2ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.integration.a2ui.model.UiComponent
import androidx.compose.material3.integration.a2ui.ui.ComponentDetailScreen
import androidx.compose.material3.integration.a2ui.ui.ExampleDetailScreen
import androidx.compose.material3.integration.a2ui.ui.HomeScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@OptIn(ExperimentalComposeUiApi::class)
class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true
        WindowCompat.enableEdgeToEdge(window)
        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }
            }
            DemoTheme { A2uiDemoApp() }
        }
    }
}

@Composable
fun A2uiDemoApp(viewModel: DemoViewModel = viewModel()) {
    val examples by viewModel.examples.collectAsStateWithLifecycle()
    val activeSurfaces by viewModel.activeSurfaces.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(HomeKey)

    NavDisplay(
        backStack = backStack,
        onBack = {
            viewModel.clearSelectedExample()
            backStack.removeLastOrNull()
        },
        transitionSpec = { ForwardTransitionSpec },
        popTransitionSpec = { BackwardTransitionSpec },
        predictivePopTransitionSpec = { BackwardTransitionSpec },
        entryProvider =
            entryProvider {
                entry<HomeKey> {
                    HomeScreen(
                        examples = examples,
                        onComponentSelected = { component ->
                            val key = ComponentDetailKey(component)
                            if (backStack.lastOrNull() != key) backStack.add(key)
                        },
                        onExampleSelected = { example ->
                            val key = ExampleDetailKey(example.fileName)
                            if (backStack.lastOrNull() != key) {
                                viewModel.selectExample(example)
                                backStack.add(key)
                            }
                        },
                    )
                }
                entry<ComponentDetailKey> { key ->
                    val component = key.component
                    val surfaceId = component.surfaceId
                    val surfaceModel = activeSurfaces.firstOrNull { it.id == surfaceId }
                    ComponentDetailScreen(
                        component = component,
                        surfaceModel = surfaceModel,
                        snackbarMessages = viewModel.snackbarMessages,
                        onUpdateSurface = { components, dataModel ->
                            viewModel.updateComponentSurface(
                                surfaceId = surfaceId,
                                components = components,
                                dataModel = dataModel,
                            )
                        },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<ExampleDetailKey> {
                    val selectedExample by viewModel.selectedExample.collectAsStateWithLifecycle()
                    selectedExample?.let { (example, surfaceModel) ->
                        ExampleDetailScreen(
                            example = example,
                            surfaceModel = surfaceModel,
                            snackbarMessages = viewModel.snackbarMessages,
                            onBack = {
                                viewModel.clearSelectedExample()
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                }
            },
    )
}

@Serializable private data object HomeKey : NavKey

@Serializable private data class ComponentDetailKey(val component: UiComponent) : NavKey

@Serializable private data class ExampleDetailKey(val fileName: String) : NavKey

private const val SlideTransitionDuration = 350

private val ForwardTransitionSpec =
    (slideInHorizontally(tween(SlideTransitionDuration)) { width -> width / 4 } +
            fadeIn(tween(SlideTransitionDuration)))
        .togetherWith(
            slideOutHorizontally(tween(SlideTransitionDuration)) { width -> -width / 4 } +
                fadeOut(tween(SlideTransitionDuration))
        )

private val BackwardTransitionSpec =
    (slideInHorizontally(tween(SlideTransitionDuration)) { width -> -width / 4 } +
            fadeIn(tween(SlideTransitionDuration)))
        .togetherWith(
            slideOutHorizontally(tween(SlideTransitionDuration)) { width -> width / 4 } +
                fadeOut(tween(SlideTransitionDuration))
        )
