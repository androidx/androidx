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
import androidx.customview.widget.Openable
import androidx.navigation.NavGraph

/**
 * Configuration options for [NavigationUI] methods that interact with implementations of the
 * app bar pattern such as [androidx.appcompat.widget.Toolbar],
 * [com.google.android.material.appbar.CollapsingToolbarLayout], and
 * [androidx.appcompat.app.ActionBar].
 *
 * @param navGraph The [NavGraph] whose start destination should be considered the only
 *                 top level destination. The Up button will not be displayed when on the
 *                 start destination of the graph.
 * @param drawerLayout The Openable layout that should be toggled from the Navigation button. The
 *                     the Navigation button will show a drawer symbol when it is not being shown
 *                     as an Up button.
 * @param fallbackOnNavigateUpListener Lambda that will be invoked if
 * [androidx.navigation.NavController.navigateUp] returns `false`
 */
@Suppress("FunctionName") /* Acts like a constructor */
inline fun AppBarConfiguration(
    navGraph: NavGraph,
    drawerLayout: Openable? = null,
    noinline fallbackOnNavigateUpListener: () -> Boolean = { false }
) = AppBarConfiguration.Builder(navGraph)
    .setOpenableLayout(drawerLayout)
    .setFallbackOnNavigateUpListener(fallbackOnNavigateUpListener)
    .build()

/**
 * Configuration options for [NavigationUI] methods that interact with implementations of the
 * app bar pattern such as [androidx.appcompat.widget.Toolbar],
 * [com.google.android.material.appbar.CollapsingToolbarLayout], and
 * [androidx.appcompat.app.ActionBar].
 *
 * @param topLevelMenu A Menu containing MenuItems corresponding with the destinations
 *                     considered at the top level of your information hierarchy.
 *                     The Up button will not be displayed when on these destinations.
 * @param drawerLayout The Openable layout that should be toggled from the Navigation button. The
 *                     the Navigation button will show a drawer symbol when it is not being shown
 *                     as an Up button.
 * @param fallbackOnNavigateUpListener Lambda that will be invoked if
 * [androidx.navigation.NavController.navigateUp] returns `false`
 */
@Suppress("FunctionName") /* Acts like a constructor */
inline fun AppBarConfiguration(
    topLevelMenu: Menu,
    drawerLayout: Openable? = null,
    noinline fallbackOnNavigateUpListener: () -> Boolean = { false }
) = AppBarConfiguration.Builder(topLevelMenu)
    .setOpenableLayout(drawerLayout)
    .setFallbackOnNavigateUpListener(fallbackOnNavigateUpListener)
    .build()

/**
 * Configuration options for [NavigationUI] methods that interact with implementations of the
 * app bar pattern such as [androidx.appcompat.widget.Toolbar],
 * [com.google.android.material.appbar.CollapsingToolbarLayout], and
 * [androidx.appcompat.app.ActionBar].
 *
 * @param topLevelDestinationIds The set of destinations by id considered at the top level
 *                               of your information hierarchy. The Up button will not be
 *                               displayed when on these destinations.
 * @param drawerLayout The Openable layout that should be toggled from the Navigation button. The
 *                     the Navigation button will show a drawer symbol when it is not being shown
 *                     as an Up button.
 * @param fallbackOnNavigateUpListener Lambda that will be invoked if
 * [androidx.navigation.NavController.navigateUp] returns `false`
 */
@Suppress("FunctionName") /* Acts like a constructor */
inline fun AppBarConfiguration(
    topLevelDestinationIds: Set<Int>,
    drawerLayout: Openable? = null,
    noinline fallbackOnNavigateUpListener: () -> Boolean = { false }
) = AppBarConfiguration.Builder(topLevelDestinationIds)
    .setOpenableLayout(drawerLayout)
    .setFallbackOnNavigateUpListener(fallbackOnNavigateUpListener)
    .build()
