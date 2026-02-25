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

sealed class Screen(val route: String) {
    object MainScreen : Screen("mainScreen")

    object RemoteButtonDemosScreen : Screen("remoteButtonDemosScreen")

    object RemoteIconButtonDemosScreen : Screen("remoteIconButtonDemosScreen")

    object RemoteTextButtonDemosScreen : Screen("remoteTextButtonDemosScreen")

    object RemoteButtonGroupDemosScreen : Screen("remoteButtonGroupDemosScreen")

    object RemoteIconDemosScreen : Screen("remoteIconDemosScreen")

    object RemoteCircularProgressIndicatorDemosScreen :
        Screen("remoteCircularProgressIndicatorDemosScreen")
}
