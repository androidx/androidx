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

package androidx.navigation.testing

import android.os.Bundle

import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

import java.util.ArrayDeque

/**
 * A simple Navigator that doesn't actually navigate anywhere, but does dispatch correctly
 */
@Navigator.Name("test")
open class TestNavigator : Navigator<TestNavigator.Destination>() {

    val backStack = ArrayDeque<Pair<Destination, Bundle?>>()

    val current
        get() = backStack.peekLast()
            ?: throw IllegalStateException("Nothing on the back stack")

    override fun createDestination(): Destination {
        return Destination(this)
    }

    override fun navigate(
        destination: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) = if (navOptions != null && navOptions.shouldLaunchSingleTop() && !backStack.isEmpty() &&
            current.first.id == destination.id
        ) {
            backStack.pop()
            backStack.add(destination to args)
            null
        } else {
            backStack.add(destination to args)
            destination
        }

    override fun popBackStack(): Boolean {
        return backStack.pollLast() != null
    }

    /**
     * A simple Test destination
     */
    class Destination internal constructor(
        navigator: Navigator<out NavDestination>
    ) : NavDestination(navigator)
}
