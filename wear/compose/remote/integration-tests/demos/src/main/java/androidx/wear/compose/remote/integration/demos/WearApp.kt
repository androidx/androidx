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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.remote.integration.demos.components.RemoteButtonDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteButtonGroupDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteCircularProgressIndicatorDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteIconButtonDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteIconDemos
import androidx.wear.compose.remote.integration.demos.components.RemoteTextButtonDemos
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
fun WearApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberSwipeDismissableNavController(),
) {
    AppScaffold(modifier = modifier) {
        SwipeDismissableNavHost(
            startDestination = Screen.MainScreen.route,
            navController = navController,
        ) {
            composable(route = Screen.MainScreen.route) {
                MainScreen(navigateToRoute = navController::navigate)
            }
            composable(route = Screen.RemoteButtonDemosScreen.route) { RemoteButtonDemos() }
            composable(route = Screen.RemoteIconButtonDemosScreen.route) { RemoteIconButtonDemos() }
            composable(route = Screen.RemoteTextButtonDemosScreen.route) { RemoteTextButtonDemos() }
            composable(route = Screen.RemoteButtonGroupDemosScreen.route) {
                RemoteButtonGroupDemos()
            }
            composable(route = Screen.RemoteIconDemosScreen.route) { RemoteIconDemos() }
            composable(route = Screen.RemoteCircularProgressIndicatorDemosScreen.route) {
                RemoteCircularProgressIndicatorDemos()
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun WearAppPreview() {
    WearApp()
}
