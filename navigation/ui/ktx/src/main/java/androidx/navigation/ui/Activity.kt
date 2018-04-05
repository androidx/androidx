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
import android.support.v7.app.AppCompatActivity
import androidx.navigation.NavController

/**
 * Sets up the ActionBar returned by [AppCompatActivity.getSupportActionBar] for use
 * with a [NavController].
 *
 * By calling this method, the title in the action bar will automatically be updated when
 * the destination changes (assuming there is a valid
 * [label][androidx.navigation.NavDestination.getLabel]).
 *
 * The action bar will also display the Up button when you are on a non-root destination and
 * the drawer icon when on the root destination, automatically animating between them.
 * Call [DrawerLayout.navigateUp] to handle the Up button.
 *
 * @param navController The NavController whose navigation actions will be reflected
 *                      in the title of the action bar.
 * @param drawerLayout The DrawerLayout that should be toggled from the home button
 */
fun AppCompatActivity.setupActionBarWithNavController(
        navController: NavController,
        drawerLayout: DrawerLayout? = null
) {
    NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)
}
