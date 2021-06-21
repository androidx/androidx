/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.testutils

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.Navigator

/**
 * A simple Navigator that doesn't actually navigate anywhere, but does dispatch correctly
 */
@Navigator.Name("test")
open class TestNavigator : Navigator<TestNavigator.Destination>() {

    val backStack: List<NavBackStackEntry>
        get() = state.backStack.value

    val current: NavBackStackEntry
        get() = backStack.lastOrNull()
            ?: throw IllegalStateException("Nothing on the back stack")

    fun popCurrent() {
        state.pop(current, false)
    }

    override fun createDestination(): Destination {
        return Destination(this)
    }

    /**
     * A simple Test destination
     */
    open class Destination constructor(
        navigator: Navigator<out NavDestination>
    ) : NavDestination(navigator)
}
