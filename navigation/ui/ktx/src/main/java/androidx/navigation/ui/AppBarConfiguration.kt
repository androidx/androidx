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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.navigation.ui

import android.view.Menu
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavGraph

/**
 * Configuration options for [NavigationUI] methods that interact with implementations of the
 * app bar pattern such as [android.support.v7.widget.Toolbar],
 * [android.support.design.widget.CollapsingToolbarLayout], and
 * [android.support.v7.app.ActionBar].
 *
 * @param navGraph The [NavGraph] whose start destination should be considered the only
 *                 top level destination. The Up button will not be displayed when on the
 *                 start destination of the graph.
 * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button. The
 *                     the Navigation button will show a drawer symbol when it is not being shown
 *                     as an Up button.
 * @param fallbackOnNavigateUpListener Lambda that will be invoked if
 * [androidx.navigation.NavController.navigateUp] returns `false`
 */
@Suppress("FunctionName") /* Acts like a constructor */
inline fun AppBarConfiguration(
    navGraph: NavGraph,
    drawerLayout: DrawerLayout? = null,
    noinline fallbackOnNavigateUpListener: () -> Boolean = { false }
) = AppBarConfiguration.Builder(navGraph)
    .setDrawerLayout(drawerLayout)
    .setFallbackOnNavigateUpListener(fallbackOnNavigateUpListener)
    .build()

/**
 * Configuration options for [NavigationUI] methods that interact with implementations of the
 * app bar pattern such as [android.support.v7.widget.Toolbar],
 * [android.support.design.widget.CollapsingToolbarLayout], and
 * [android.support.v7.app.ActionBar].
 *
 * @param topLevelMenu A Menu containing MenuItems corresponding with the destinations
 *                     considered at the top level of your information hierarchy.
 *                     The Up button will not be displayed when on these destinations.
 * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button. The
 *                     the Navigation button will show a drawer symbol when it is not being shown
 *                     as an Up button.
 * @param fallbackOnNavigateUpListener Lambda that will be invoked if
 * [androidx.navigation.NavController.navigateUp] returns `false`
 */
@Suppress("FunctionName") /* Acts like a constructor */
inline fun AppBarConfiguration(
    topLevelMenu: Menu,
    drawerLayout: DrawerLayout? = null,
    noinline fallbackOnNavigateUpListener: () -> Boolean = { false }
) = AppBarConfiguration.Builder(topLevelMenu)
    .setDrawerLayout(drawerLayout)
    .setFallbackOnNavigateUpListener(fallbackOnNavigateUpListener)
    .build()

/**
 * Configuration options for [NavigationUI] methods that interact with implementations of the
 * app bar pattern such as [android.support.v7.widget.Toolbar],
 * [android.support.design.widget.CollapsingToolbarLayout], and
 * [android.support.v7.app.ActionBar].
 *
 * @param topLevelDestinationIds The set of destinations by id considered at the top level
 *                               of your information hierarchy. The Up button will not be
 *                               displayed when on these destinations.
 * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button. The
 *                     the Navigation button will show a drawer symbol when it is not being shown
 *                     as an Up button.
 * @param fallbackOnNavigateUpListener Lambda that will be invoked if
 * [androidx.navigation.NavController.navigateUp] returns `false`
 */
@Suppress("FunctionName") /* Acts like a constructor */
inline fun AppBarConfiguration(
    topLevelDestinationIds: Set<Int>,
    drawerLayout: DrawerLayout? = null,
    noinline fallbackOnNavigateUpListener: () -> Boolean = { false }
) = AppBarConfiguration.Builder(topLevelDestinationIds)
    .setDrawerLayout(drawerLayout)
    .setFallbackOnNavigateUpListener(fallbackOnNavigateUpListener)
    .build()
