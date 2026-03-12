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

package androidx.navigation

import org.w3c.dom.Window

/**
 * Binds the browser window state to the given navigation controller.
 *
 * If `getBackStackEntryRoute` is null, then:
 * 1) if a browser url contains a destination route on a start then navigates to destination
 * 2) if a user puts a new destination route to the browser address field then navigates to the new
 *    destination
 *
 * If there is a custom `getBackStackEntryRoute` implementation, then we don't have a knowledge how
 * to parse urls to support direct navigation via browser address input. In that case, it should be
 * done on the app's side:
 * ```
 * window.addEventListener("popstate") { event ->
 *     event as PopStateEvent
 *     if (event.state == null) { // empty state means manually entered address
 *         val url = window.location.toString()
 *         navController.navigate(...)
 *     }
 * }
 * ```
 *
 * @param navController The [NavController] instance to bind to browser window navigation.
 * @param getBackStackEntryRoute An optional function that returns the route to show for a given
 *   [NavBackStackEntry].
 */
@Suppress("UnusedReceiverParameter")
@Deprecated(
    message = "Use bindToBrowserNavigation",
    replaceWith = ReplaceWith("navController.bindToBrowserNavigation(getBackStackEntryRoute)"),
)
@ExperimentalBrowserHistoryApi
public suspend fun Window.bindToNavigation(
    navController: NavController,
    getBackStackEntryRoute: ((entry: NavBackStackEntry) -> String)? = null,
) {
    navController.bindToBrowserNavigation(getBackStackEntryRoute)
}

internal actual fun refBrowserWindow(): BrowserWindow = js("window")
