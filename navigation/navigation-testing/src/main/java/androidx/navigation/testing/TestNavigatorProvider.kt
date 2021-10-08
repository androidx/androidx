/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.navigation.NavDestination
import androidx.navigation.NavGraphNavigator
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider
import java.lang.IllegalStateException

/**
 * A [NavigatorProvider] for testing that only parses
 * [navigation graphs][androidx.navigation.NavGraph] and [destinations][NavDestination].
 */
internal class TestNavigatorProvider : NavigatorProvider() {

    /**
     * A [Navigator] that only supports creating destinations.
     */
    private val navigator = object : Navigator<NavDestination>() {
        override fun createDestination() = NavDestination("test")
    }

    init {
        addNavigator(NavGraphNavigator(this))
        addNavigator("test", navigator)
    }

    override fun <T : Navigator<out NavDestination>> getNavigator(name: String): T {
        return try {
            super.getNavigator(name)
        } catch (e: IllegalStateException) {
            @Suppress("UNCHECKED_CAST")
            navigator as T
        }
    }
}
