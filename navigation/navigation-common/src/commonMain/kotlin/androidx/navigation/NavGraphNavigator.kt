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

import kotlinx.coroutines.flow.StateFlow

/**
 * A Navigator built specifically for [NavGraph] elements. Handles navigating to the
 * correct destination when the NavGraph is the target of navigation actions.
 *
 * Construct a Navigator capable of routing incoming navigation requests to the proper
 * destination within a [NavGraph].
 *
 * @param navigatorProvider NavigatorProvider used to retrieve the correct
 * [Navigator] to navigate to the start destination
 */
public expect open class NavGraphNavigator(
    navigatorProvider: NavigatorProvider
) : Navigator<NavGraph> {

    /**
     * Gets the backstack of [NavBackStackEntry] associated with this Navigator
     */
    public val backStack: StateFlow<List<NavBackStackEntry>>
}
