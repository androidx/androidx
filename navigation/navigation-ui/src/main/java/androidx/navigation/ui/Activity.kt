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

import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController

/**
 * Sets up the ActionBar returned by [AppCompatActivity.getSupportActionBar] for use
 * with a [NavController].
 *
 * By calling this method, the title in the action bar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.label]).
 *
 * The start destination of your navigation graph is considered the only top level
 * destination. On the start destination of your navigation graph, the ActionBar will show
 * the drawer icon if the given `drawerLayout` is non null. On all other destinations,
 * the ActionBar will show the Up button.
 *
 * You are responsible for calling [NavController.navigateUp] to handle the Navigation button.
 * Typically this is done in [AppCompatActivity.onSupportNavigateUp].
 *
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the action bar.
 * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button
 */
public fun AppCompatActivity.setupActionBarWithNavController(
    navController: NavController,
    drawerLayout: DrawerLayout?
) {
    NavigationUI.setupActionBarWithNavController(
        this, navController,
        AppBarConfiguration(navController.graph, drawerLayout)
    )
}

/**
 * Sets up the ActionBar returned by [AppCompatActivity.getSupportActionBar] for use
 * with a [NavController].
 *
 * By calling this method, the title in the action bar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.label]).
 *
 * The [AppBarConfiguration] you provide controls how the Navigation button is
 * displayed.
 *
 * You are responsible for calling [NavController.navigateUp] to handle the Navigation button.
 * Typically this is done in [AppCompatActivity.onSupportNavigateUp].
 *
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the action bar.
 * @param configuration Additional configuration options for customizing the behavior of the
 *                      ActionBar
 */
public fun AppCompatActivity.setupActionBarWithNavController(
    navController: NavController,
    configuration: AppBarConfiguration = AppBarConfiguration(navController.graph)
) {
    NavigationUI.setupActionBarWithNavController(this, navController, configuration)
}
