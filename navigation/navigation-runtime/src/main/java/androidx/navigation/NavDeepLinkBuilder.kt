/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.navigation

import android.app.Activity
import android.content.Intent
import android.content.ComponentName
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.annotation.NavigationRes
import androidx.core.app.TaskStackBuilder
import androidx.navigation.NavDestination.Companion.createRoute

/**
 * Class used to construct deep links to a particular destination in a [NavGraph].
 *
 * When this deep link is triggered:
 *
 *  1. The task is cleared.
 *  2. The destination and all of its parents will be on the back stack.
 *  3. Calling [NavController.navigateUp] will navigate to the parent of the
 * destination.
 *
 * The parent of the destination is the [start destination][NavGraph.getStartDestination]
 * of the containing [navigation graph][NavGraph]. In the cases where the destination is
 * the start destination of its containing navigation graph, the start destination of its
 * grandparent is used.
 *
 * You can construct an instance directly with [NavDeepLinkBuilder] or build one
 * using an existing [NavController] via [NavController.createDeepLink].
 *
 * If the context passed in here is not an [Activity], this method will use
 * [android.content.pm.PackageManager.getLaunchIntentForPackage] as the
 * default activity to launch, if available.
 *
 * @param context Context used to create deep links
 * @see NavDeepLinkBuilder.setComponentName
 */
public class NavDeepLinkBuilder(private val context: Context) {
    private class DeepLinkDestination constructor(
        val destinationId: Int,
        val arguments: Bundle?
    )

    private val intent: Intent = if (context is Activity) {
        Intent(context, context.javaClass)
    } else {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent ?: Intent()
    }.also {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    private var graph: NavGraph? = null
    private val destinations = mutableListOf<DeepLinkDestination>()
    private var globalArgs: Bundle? = null

    /**
     * @see NavController.createDeepLink
     */
    internal constructor(navController: NavController) : this(navController.context) {
        graph = navController.graph
    }

    /**
     * Sets an explicit Activity to be started by the deep link created by this class.
     *
     * @param activityClass The Activity to start. This Activity should have a [NavController]
     * which uses the same [NavGraph] used to construct this
     * deep link.
     * @return this object for chaining
     */
    public fun setComponentName(activityClass: Class<out Activity?>): NavDeepLinkBuilder {
        return setComponentName(ComponentName(context, activityClass))
    }

    /**
     * Sets an explicit Activity to be started by the deep link created by this class.
     *
     * @param componentName The Activity to start. This Activity should have a [NavController]
     *                      which uses the same [NavGraph] used to construct this
     *                      deep link.
     * @return this object for chaining
     */
    public fun setComponentName(componentName: ComponentName): NavDeepLinkBuilder {
        intent.component = componentName
        return this
    }

    /**
     * Sets the graph that contains the [deep link destination][setDestination].
     *
     * @param navGraphId ID of the [NavGraph] containing the deep link destination
     * @return this object for chaining
     */
    public fun setGraph(@NavigationRes navGraphId: Int): NavDeepLinkBuilder {
        return setGraph(NavInflater(context, PermissiveNavigatorProvider()).inflate(navGraphId))
    }

    /**
     * Sets the graph that contains the [deep link destination][setDestination].
     *
     * If you do not have access to a [NavController], you can create a
     * [NavigatorProvider] and use that to programmatically construct a navigation
     * graph or use [NavInflater][NavInflater].
     *
     * @param navGraph The [NavGraph] containing the deep link destination
     * @return this object for chaining
     */
    public fun setGraph(navGraph: NavGraph): NavDeepLinkBuilder {
        graph = navGraph
        verifyAllDestinations()
        return this
    }

    /**
     * Sets the destination id to deep link to. Any destinations previous added via
     * [addDestination] are cleared, effectively resetting this object
     * back to only this single destination.
     *
     * @param destId destination ID to deep link to.
     * @param args Arguments to pass to this destination and any synthetic back stack created
     * due to this destination being added.
     * @return this object for chaining
     */
    @JvmOverloads
    public fun setDestination(@IdRes destId: Int, args: Bundle? = null): NavDeepLinkBuilder {
        destinations.clear()
        destinations.add(DeepLinkDestination(destId, args))
        if (graph != null) {
            verifyAllDestinations()
        }
        return this
    }

    /**
     * Sets the destination route to deep link to. Any destinations previous added via
     * [.addDestination] are cleared, effectively resetting this object
     * back to only this single destination.
     *
     * @param destRoute destination route to deep link to.
     * @param args Arguments to pass to this destination and any synthetic back stack created
     * due to this destination being added.
     * @return this object for chaining
     */
    @JvmOverloads
    public fun setDestination(destRoute: String, args: Bundle? = null): NavDeepLinkBuilder {
        destinations.clear()
        destinations.add(DeepLinkDestination(createRoute(destRoute).hashCode(), args))
        if (graph != null) {
            verifyAllDestinations()
        }
        return this
    }

    /**
     * Add a new destination id to deep link to. This builds off any previous calls to this method
     * or calls to [setDestination], building the minimal synthetic back stack of
     * start destinations between the previous deep link destination and the newly added
     * deep link destination.
     *
     * @param destId destination ID to deep link to.
     * @param args Arguments to pass to this destination and any synthetic back stack created
     * due to this destination being added.
     * @return this object for chaining
     */
    @JvmOverloads
    public fun addDestination(@IdRes destId: Int, args: Bundle? = null): NavDeepLinkBuilder {
        destinations.add(DeepLinkDestination(destId, args))
        if (graph != null) {
            verifyAllDestinations()
        }
        return this
    }

    /**
     * Add a new destination route to deep link to. This builds off any previous calls to this
     * method or calls to [.setDestination], building the minimal synthetic back stack of
     * start destinations between the previous deep link destination and the newly added
     * deep link destination.
     *
     * @param route destination route to deep link to.
     * @param args Arguments to pass to this destination and any synthetic back stack created
     * due to this destination being added.
     * @return this object for chaining
     */
    @JvmOverloads
    public fun addDestination(route: String, args: Bundle? = null): NavDeepLinkBuilder {
        destinations.add(DeepLinkDestination(createRoute(route).hashCode(), args))
        if (graph != null) {
            verifyAllDestinations()
        }
        return this
    }

    private fun findDestination(@IdRes destId: Int): NavDestination? {
        val possibleDestinations = ArrayDeque<NavDestination>()
        possibleDestinations.add(graph!!)
        while (!possibleDestinations.isEmpty()) {
            val destination = possibleDestinations.removeFirst()
            if (destination.id == destId) {
                return destination
            } else if (destination is NavGraph) {
                for (child in destination) {
                    possibleDestinations.add(child)
                }
            }
        }
        return null
    }

    private fun verifyAllDestinations() {
        for (destination in destinations) {
            val destId = destination.destinationId
            val node = findDestination(destId)
            if (node == null) {
                val dest = NavDestination.getDisplayName(context, destId)
                throw IllegalArgumentException(
                    "Navigation destination $dest cannot be found in the navigation graph $graph"
                )
            }
        }
    }

    private fun fillInIntent() {
        val deepLinkIds = mutableListOf<Int>()
        val deepLinkArgs = ArrayList<Bundle?>()
        var previousDestination: NavDestination? = null
        for (destination in destinations) {
            val destId = destination.destinationId
            val arguments = destination.arguments
            val node = findDestination(destId)
            if (node == null) {
                val dest = NavDestination.getDisplayName(context, destId)
                throw IllegalArgumentException(
                    "Navigation destination $dest cannot be found in the navigation graph $graph"
                )
            }
            for (id in node.buildDeepLinkIds(previousDestination)) {
                deepLinkIds.add(id)
                deepLinkArgs.add(arguments)
            }
            previousDestination = node
        }
        val idArray = deepLinkIds.toIntArray()
        intent.putExtra(NavController.KEY_DEEP_LINK_IDS, idArray)
        intent.putParcelableArrayListExtra(NavController.KEY_DEEP_LINK_ARGS, deepLinkArgs)
    }

    /**
     * Set optional arguments to send onto every destination created by this deep link.
     * @param args arguments to pass to each destination
     * @return this object for chaining
     */
    public fun setArguments(args: Bundle?): NavDeepLinkBuilder {
        globalArgs = args
        intent.putExtra(NavController.KEY_DEEP_LINK_EXTRAS, args)
        return this
    }

    /**
     * Construct the full [task stack][TaskStackBuilder] needed to deep link to the given
     * destination.
     *
     * You must have [set a NavGraph][setGraph] and [set a destination][setDestination]
     * before calling this method.
     *
     * @return a [TaskStackBuilder] which can be used to
     * [send the deep link][TaskStackBuilder.startActivities] or
     * [create a PendingIntent][TaskStackBuilder.getPendingIntent] to deep link to
     * the given destination.
     */
    public fun createTaskStackBuilder(): TaskStackBuilder {
        checkNotNull(graph) {
            "You must call setGraph() before constructing the deep link"
        }
        check(destinations.isNotEmpty()) {
            "You must call setDestination() or addDestination() before constructing the deep link"
        }
        fillInIntent()
        // We create a copy of the Intent to ensure the Intent does not have itself
        // as an extra. This also prevents developers from modifying the internal Intent
        // via taskStackBuilder.editIntentAt()
        val taskStackBuilder = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(Intent(intent))
        for (index in 0 until taskStackBuilder.intentCount) {
            // Attach the original Intent to each Activity so that they can know
            // they were constructed in response to a deep link
            taskStackBuilder.editIntentAt(index)
                ?.putExtra(NavController.KEY_DEEP_LINK_INTENT, intent)
        }
        return taskStackBuilder
    }

    /**
     * Construct a [PendingIntent] to the [deep link destination][setDestination].
     *
     * This constructs the entire [task stack][createTaskStackBuilder] needed.
     *
     * You must have [set a NavGraph][setGraph] and [set a destination][setDestination]
     * before calling this method.
     *
     * @return a PendingIntent constructed with [TaskStackBuilder.getPendingIntent] to deep link
     * to the given destination
     */
    public fun createPendingIntent(): PendingIntent {
        var requestCode = 0
        globalArgs?.let { globalArgs ->
            for (key in globalArgs.keySet()) {
                val value = globalArgs[key]
                requestCode = 31 * requestCode + (value?.hashCode() ?: 0)
            }
        }
        for (destination in destinations) {
            val destId = destination.destinationId
            requestCode = 31 * requestCode + destId
            val arguments = destination.arguments
            if (arguments != null) {
                for (key in arguments.keySet()) {
                    val value = arguments[key]
                    requestCode = 31 * requestCode + (value?.hashCode() ?: 0)
                }
            }
        }
        return createTaskStackBuilder()
            .getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT)!!
    }

    /**
     * A [NavigatorProvider] that only parses the basics: [navigation graphs][NavGraph]
     * and [destinations][NavDestination], effectively only getting the base destination
     * information.
     */
    private class PermissiveNavigatorProvider : NavigatorProvider() {
        /**
         * A Navigator that only parses the [NavDestination] attributes.
         */
        private val mDestNavigator: Navigator<NavDestination> =
            object : Navigator<NavDestination>() {
                override fun createDestination(): NavDestination {
                    return NavDestination("permissive")
                }

                override fun navigate(
                    destination: NavDestination,
                    args: Bundle?,
                    navOptions: NavOptions?,
                    navigatorExtras: Extras?
                ): NavDestination? {
                    throw IllegalStateException("navigate is not supported")
                }

                override fun popBackStack(): Boolean {
                    throw IllegalStateException("popBackStack is not supported")
                }
            }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Navigator<out NavDestination>> getNavigator(name: String): T {
            return try {
                super.getNavigator(name)
            } catch (e: IllegalStateException) {
                mDestNavigator as T
            }
        }

        init {
            addNavigator(NavGraphNavigator(this))
        }
    }
}
