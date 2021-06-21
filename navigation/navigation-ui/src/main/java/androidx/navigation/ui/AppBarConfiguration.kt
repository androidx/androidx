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

import android.annotation.SuppressLint
import android.view.Menu
import androidx.customview.widget.Openable
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.ui.AppBarConfiguration.OnNavigateUpListener
import java.util.HashSet

/**
 * Configuration options for [NavigationUI] methods that interact with implementations of the
 * app bar pattern such as [androidx.appcompat.widget.Toolbar],
 * [com.google.android.material.appbar.CollapsingToolbarLayout], and
 * [androidx.appcompat.app.ActionBar].
 */
public class AppBarConfiguration private constructor(
    /**
     * The set of destinations by id considered at the top level of your information hierarchy.
     * The Up button will not be displayed when on these destinations.
     *
     * @return The set of top level destinations by id.
     */
    public val topLevelDestinations: Set<Int>,
    /**
     * The [Openable] layout indicating that the Navigation button should be displayed as
     * a drawer symbol when it is not being shown as an Up button.
     * @return The Openable layout that should be toggled from the Navigation button
     */
    public val openableLayout: Openable?,
    /**
     * The [OnNavigateUpListener] that should be invoked if
     * [androidx.navigation.NavController.navigateUp] returns `false`.
     * @return a [OnNavigateUpListener] for providing custom up navigation logic,
     * if one was set.
     */
    public val fallbackOnNavigateUpListener: OnNavigateUpListener?
) {
    /**
     * Interface for providing custom 'up' behavior beyond what is provided by
     * [androidx.navigation.NavController.navigateUp].
     *
     * @see Builder.setFallbackOnNavigateUpListener
     * @see NavigationUI.navigateUp
     */
    public fun interface OnNavigateUpListener {
        /**
         * Callback for handling the Up button.
         *
         * @return true if the listener successfully navigated 'up'
         */
        public fun onNavigateUp(): Boolean
    }

    /**
     * The [DrawerLayout] indicating that the Navigation button should be displayed as
     * a drawer symbol when it is not being shown as an Up button.
     * @return The DrawerLayout that should be toggled from the Navigation button
     */
    @get:Deprecated("Use {@link #getOpenableLayout()}.")
    public val drawerLayout: DrawerLayout?
        get() = if (openableLayout is DrawerLayout) {
            openableLayout
        } else null

    /**
     * The Builder class for constructing new [AppBarConfiguration] instances.
     */
    public class Builder {
        private val topLevelDestinations: MutableSet<Int> = HashSet()
        private var openableLayout: Openable? = null
        private var fallbackOnNavigateUpListener: OnNavigateUpListener? = null

        /**
         * Create a new Builder whose only top level destination is the start destination
         * of the given [NavGraph]. The Up button will not be displayed when on the
         * start destination of the graph.
         *
         * @param navGraph The NavGraph whose start destination should be considered the only
         * top level destination. The Up button will not be displayed when on the
         * start destination of the graph.
         */
        public constructor(navGraph: NavGraph) {
            topLevelDestinations.add(navGraph.findStartDestination().id)
        }

        /**
         * Create a new Builder using a [Menu] containing all top level destinations. It is
         * expected that the [menu item id][MenuItem.getItemId] of each item corresponds
         * with a destination in your navigation graph. The Up button will not be displayed when
         * on these destinations.
         *
         * @param topLevelMenu A Menu containing MenuItems corresponding with the destinations
         * considered at the top level of your information hierarchy.
         * The Up button will not be displayed when on these destinations.
         */
        public constructor(topLevelMenu: Menu) {
            val size = topLevelMenu.size()
            for (index in 0 until size) {
                val item = topLevelMenu.getItem(index)
                topLevelDestinations.add(item.itemId)
            }
        }

        /**
         * Create a new Builder with a specific set of top level destinations. The Up button will
         * not be displayed when on these destinations.
         *
         * @param topLevelDestinationIds The set of destinations by id considered at the top level
         * of your information hierarchy. The Up button will not be
         * displayed when on these destinations.
         */
        public constructor(vararg topLevelDestinationIds: Int) {
            for (destinationId in topLevelDestinationIds) {
                topLevelDestinations.add(destinationId)
            }
        }

        /**
         * Create a new Builder with a specific set of top level destinations. The Up button will
         * not be displayed when on these destinations.
         *
         * @param topLevelDestinationIds The set of destinations by id considered at the top level
         * of your information hierarchy. The Up button will not be
         * displayed when on these destinations.
         */
        public constructor(topLevelDestinationIds: Set<Int>) {
            topLevelDestinations.addAll(topLevelDestinationIds)
        }

        /**
         * Display the Navigation button as a drawer symbol when it is not being shown as an
         * Up button.
         * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button
         * @return this [Builder]
         */
        @Deprecated("Use {@link #setOpenableLayout(Openable)}.")
        public fun setDrawerLayout(drawerLayout: DrawerLayout?): Builder {
            openableLayout = drawerLayout
            return this
        }

        /**
         * Display the Navigation button as a drawer symbol when it is not being shown as an
         * Up button.
         * @param openableLayout The Openable layout that should be toggled from the Navigation
         * button
         * @return this [Builder]
         */
        public fun setOpenableLayout(openableLayout: Openable?): Builder {
            this.openableLayout = openableLayout
            return this
        }

        /**
         * Adds a [OnNavigateUpListener] that will be called as a fallback if the default
         * behavior of [androidx.navigation.NavController.navigateUp]
         * returns `false`.
         *
         * @param fallbackOnNavigateUpListener Listener that will be invoked if
         * [androidx.navigation.NavController.navigateUp]
         * returns `false`.
         * @return this [Builder]
         */
        public fun setFallbackOnNavigateUpListener(
            fallbackOnNavigateUpListener: OnNavigateUpListener?
        ): Builder {
            this.fallbackOnNavigateUpListener = fallbackOnNavigateUpListener
            return this
        }

        /**
         * Construct the [AppBarConfiguration] instance.
         *
         * @return a valid [AppBarConfiguration]
         */
        @SuppressLint("SyntheticAccessor") /* new AppBarConfiguration() must be private to avoid
                                              conflicting with the public AppBarConfiguration.kt */
        public fun build(): AppBarConfiguration {
            return AppBarConfiguration(
                topLevelDestinations,
                openableLayout,
                fallbackOnNavigateUpListener
            )
        }
    }
}

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
@Suppress("FunctionName", "NOTHING_TO_INLINE") /* Acts like a constructor */
public inline fun AppBarConfiguration(
    navGraph: NavGraph,
    drawerLayout: Openable? = null,
    noinline fallbackOnNavigateUpListener: () -> Boolean = { false }
): AppBarConfiguration = AppBarConfiguration.Builder(navGraph)
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
@Suppress("FunctionName", "NOTHING_TO_INLINE") /* Acts like a constructor */
public inline fun AppBarConfiguration(
    topLevelMenu: Menu,
    drawerLayout: Openable? = null,
    noinline fallbackOnNavigateUpListener: () -> Boolean = { false }
): AppBarConfiguration = AppBarConfiguration.Builder(topLevelMenu)
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
@Suppress("FunctionName", "NOTHING_TO_INLINE") /* Acts like a constructor */
public inline fun AppBarConfiguration(
    topLevelDestinationIds: Set<Int>,
    drawerLayout: Openable? = null,
    noinline fallbackOnNavigateUpListener: () -> Boolean = { false }
): AppBarConfiguration = AppBarConfiguration.Builder(topLevelDestinationIds)
    .setOpenableLayout(drawerLayout)
    .setFallbackOnNavigateUpListener(fallbackOnNavigateUpListener)
    .build()
