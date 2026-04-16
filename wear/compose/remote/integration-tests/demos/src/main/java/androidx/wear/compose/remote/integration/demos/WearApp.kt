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

package androidx.wear.compose.remote.integration.demos

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavHostController
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.remote.integration.demos.components.LocalUseDynamicColor
import androidx.wear.compose.remote.integration.demos.components.RemoteAppCardDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteButtonDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteButtonGroupDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteCardDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteCircularProgressIndicatorDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteCompactButtonDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteIconButtonDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteIconDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteTextButtonDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteTextDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteTitleCardDemos
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "remote_material3_demos")
private val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")

@Composable
fun WearApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberSwipeDismissableNavController(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val useDynamicColorFlow = remember {
        context.dataStore.data.map { preferences -> preferences[USE_DYNAMIC_COLOR] ?: true }
    }
    val useDynamicColor by useDynamicColorFlow.collectAsState(initial = true)

    CompositionLocalProvider(LocalUseDynamicColor provides useDynamicColor) {
        AppScaffold(modifier = modifier) {
            SwipeDismissableNavHost(
                startDestination = Screen.MainScreen.route,
                navController = navController,
            ) {
                composable(route = Screen.MainScreen.route) {
                    MainScreen(
                        useDynamicColor = useDynamicColor,
                        onUseDynamicColorChange = { newValue ->
                            coroutineScope.launch {
                                context.dataStore.edit { preferences ->
                                    preferences[USE_DYNAMIC_COLOR] = newValue
                                }
                            }
                        },
                        navigateToRoute = navController::navigate,
                    )
                }
                composable(route = Screen.RemoteButtonDemosScreen.route) { RemoteButtonDemos() }
                composable(route = Screen.RemoteCompactButtonDemosScreen.route) {
                    RemoteCompactButtonDemos()
                }
                composable(route = Screen.RemoteIconButtonDemosScreen.route) {
                    RemoteIconButtonDemos()
                }
                composable(route = Screen.RemoteTextButtonDemosScreen.route) {
                    RemoteTextButtonDemos()
                }
                composable(route = Screen.RemoteButtonGroupDemosScreen.route) {
                    RemoteButtonGroupDemos()
                }
                composable(route = Screen.RemoteIconDemosScreen.route) { RemoteIconDemos() }
                composable(route = Screen.RemoteCircularProgressIndicatorDemosScreen.route) {
                    RemoteCircularProgressIndicatorDemos()
                }
                composable(route = Screen.RemoteAppCardDemosScreen.route) { RemoteAppCardDemos() }
                composable(route = Screen.RemoteCardDemosScreen.route) { RemoteCardDemos() }
                composable(route = Screen.RemoteTitleCardDemosScreen.route) {
                    RemoteTitleCardDemos()
                }
                composable(route = Screen.RemoteTextDemosScreen.route) { RemoteTextDemos() }
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun WearAppPreview() {
    WearApp()
}
