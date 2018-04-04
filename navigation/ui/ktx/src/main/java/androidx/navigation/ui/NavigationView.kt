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

import android.support.design.widget.NavigationView
import androidx.navigation.NavController

/**
 * Sets up a [NavigationView] for use with a [NavController]. This will call
 * [android.view.MenuItem.onNavDestinationSelected] when a menu item is selected.
 *
 * The selected item in the NavigationView will automatically be updated when the destination
 * changes.
 */
fun NavigationView.setupWithNavController(navController: NavController) {
    NavigationUI.setupWithNavController(this, navController)
}
