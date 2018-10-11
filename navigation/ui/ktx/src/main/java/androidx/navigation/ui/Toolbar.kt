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

package androidx.navigation.ui

import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.Toolbar
import androidx.navigation.NavController

/**
 * Sets up a [Toolbar] for use with a [NavController].
 *
 * By calling this method, the title in the Toolbar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.getLabel]).
 *
 * The Toolbar will automatically animate between a drawer icon (if `drawerLayout` is non-null)
 * and the Up button when transitioning between one of the `topLevelDestinations` and other
 * destinations in your graph.
 *
 * This method will call [NavController.navigateUp] when the navigation icon is clicked.
 *
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the Toolbar.
 * @param drawerLayout The DrawerLayout that should be toggled from the home button
 * @param topLevelDestinations The set of destinations by id considered at the top level of your
 * information hierarchy. The Up button will not be displayed when on these destinations.
 */
fun Toolbar.setupWithNavController(
    navController: NavController,
    drawerLayout: DrawerLayout? = null,
    topLevelDestinations: Set<Int> = setOf(
        NavigationUI.findStartDestination(navController.graph).id)
) {
    NavigationUI.setupWithNavController(this, navController,
            AppBarConfiguration.Builder(topLevelDestinations)
                    .setDrawerLayout(drawerLayout)
                    .build())
}
