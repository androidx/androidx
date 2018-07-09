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

import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.Toolbar
import androidx.navigation.NavController

/**
 * Sets up a [CollapsingToolbarLayout] and [Toolbar] for use with a [NavController].
 *
 * By calling this method, the title in the Toolbar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.getLabel]).
 *
 * The Toolbar will also display the Up button when you are on a non-root destination and
 * the drawer icon when on the root destination, automatically animating between them. This
 * method will call [DrawerLayout.navigateUp] when the navigation icon
 * is clicked.
 *
 * @param toolbar The Toolbar that should be kept in sync with changes to the NavController.
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the Toolbar.
 * @param drawerLayout The DrawerLayout that should be toggled from the home button
 */
fun CollapsingToolbarLayout.setupWithNavController(
    toolbar: Toolbar,
    navController: NavController,
    drawerLayout: DrawerLayout? = null
) {
    NavigationUI.setupWithNavController(this, toolbar, navController, drawerLayout)
}
