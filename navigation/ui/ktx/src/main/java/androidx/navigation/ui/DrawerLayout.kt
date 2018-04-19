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
import androidx.navigation.NavController

/**
 * Handles the Up button by delegating its behavior to the given [NavController].
 *
 * This is equivalent to calling [NavController.navigateUp] if the [DrawerLayout] is null.
 *
 * @return True if the [NavController] was able to navigate up.
 */
fun DrawerLayout?.navigateUp(navController: NavController): Boolean =
        NavigationUI.navigateUp(this, navController)
