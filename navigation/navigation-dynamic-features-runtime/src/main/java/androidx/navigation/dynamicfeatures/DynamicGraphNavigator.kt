/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import androidx.core.content.withStyledAttributes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider

/**
 * Navigator for graphs in dynamic feature modules.
 *
 * This class handles navigating to a progress destination when the installation
 * of a dynamic feature module is required. By default, the progress destination set
 * by [installDefaultProgressDestination] will be used, but this can be overridden
 * by setting the `app:progressDestinationId` attribute in your navigation XML file.
 */
@Navigator.Name("navigation")
public class DynamicGraphNavigator(
    private val navigatorProvider: NavigatorProvider,
    private val installManager: DynamicInstallManager
) : NavGraphNavigator(navigatorProvider) {

    /**
     * @return The progress destination supplier if any is set.
     */
    internal var defaultProgressDestinationSupplier: (() -> NavDestination)? = null
        private set

    internal val destinationsWithoutDefaultProgressDestination = mutableListOf<DynamicNavGraph>()

    /**
     * Navigate to a destination.
     *
     * In case the destination module is installed the navigation will trigger directly.
     * Otherwise the dynamic feature module is requested and navigation is postponed until the
     * module has successfully been installed.
     */
    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        for (entry in entries) {
            navigate(entry, navOptions, navigatorExtras)
        }
    }

    private fun navigate(
        entry: NavBackStackEntry,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        val destination = entry.destination
        val extras = if (navigatorExtras is DynamicExtras) navigatorExtras else null
        if (destination is DynamicNavGraph) {
            val moduleName = destination.moduleName
            if (moduleName != null && installManager.needsInstall(moduleName)) {
                installManager.performInstall(entry, extras, moduleName)
                return
            }
        }
        super.navigate(
            listOf(entry), navOptions,
            if (extras != null) extras.destinationExtras else navigatorExtras
        )
    }

    /**
     * Create a destination for the [DynamicNavGraph].
     *
     * @return The created graph.
     */
    override fun createDestination(): DynamicNavGraph {
        return DynamicNavGraph(this, navigatorProvider)
    }

    /**
     * Installs the default progress destination to this graph via a lambda.
     * This supplies a [NavDestination] to use when the actual destination is not installed at
     * navigation time.
     *
     * This **must** be called before you call [androidx.navigation.NavController.setGraph] to
     * ensure that all [DynamicNavGraph] instances have the correct progress destination
     * installed in [onRestoreState].
     *
     * @param progressDestinationSupplier The default progress destination supplier.
     */
    public fun installDefaultProgressDestination(
        progressDestinationSupplier: () -> NavDestination
    ) {
        this.defaultProgressDestinationSupplier = progressDestinationSupplier
    }

    /**
     * Navigates to a destination after progress is done.
     *
     * @return The destination to navigate to if any.
     */
    internal fun navigateToProgressDestination(
        dynamicNavGraph: DynamicNavGraph,
        progressArgs: Bundle?
    ) {
        var progressDestinationId = dynamicNavGraph.progressDestination
        if (progressDestinationId == 0) {
            progressDestinationId = installDefaultProgressDestination(dynamicNavGraph)
        }

        val progressDestination = dynamicNavGraph.findNode(progressDestinationId)
            ?: throw IllegalStateException(
                "The progress destination id must be set and " +
                    "accessible to the module of this navigator."
            )
        val navigator = navigatorProvider.getNavigator<Navigator<NavDestination>>(
            progressDestination.navigatorName
        )
        val entry = state.createBackStackEntry(progressDestination, progressArgs)
        navigator.navigate(listOf(entry), null, null)
    }

    /**
     * Install the default progress destination
     *
     * @return The [NavDestination#getId] of the newly added progress destination
     */
    private fun installDefaultProgressDestination(dynamicNavGraph: DynamicNavGraph): Int {
        val progressDestinationSupplier = defaultProgressDestinationSupplier
        checkNotNull(progressDestinationSupplier) {
            "You must set a default progress destination " +
                "using DynamicNavGraphNavigator.installDefaultProgressDestination or " +
                "pass in an DynamicInstallMonitor in the DynamicExtras.\n" +
                "Alternatively, when using NavHostFragment make sure to swap it with " +
                "DynamicNavHostFragment. This will take care of setting the default " +
                "progress destination for you."
        }
        val progressDestination = progressDestinationSupplier.invoke()
        dynamicNavGraph.addDestination(progressDestination)
        dynamicNavGraph.progressDestination = progressDestination.id
        return progressDestination.id
    }

    override fun onSaveState(): Bundle? {
        // Return a non-null Bundle to get a callback to onRestoreState
        return Bundle.EMPTY
    }

    override fun onRestoreState(savedState: Bundle) {
        super.onRestoreState(savedState)
        val iterator = destinationsWithoutDefaultProgressDestination.iterator()
        while (iterator.hasNext()) {
            val dynamicNavGraph = iterator.next()
            installDefaultProgressDestination(dynamicNavGraph)
            iterator.remove()
        }
    }

    /**
     * The [NavGraph] for dynamic features.
     */
    public class DynamicNavGraph(
        /**
         * @hide
         */
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        internal val navGraphNavigator: DynamicGraphNavigator,
        /**
         * @hide
         */
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        internal val navigatorProvider: NavigatorProvider
    ) : NavGraph(navGraphNavigator) {

        internal companion object {

            /**
             * Get the [DynamicNavGraph] for a supplied [NavDestination] or throw an
             * exception if it's not a [DynamicNavGraph].
             */
            internal fun getOrThrow(destination: NavDestination): DynamicNavGraph {
                return destination.parent as? DynamicNavGraph
                    ?: throw IllegalStateException(
                        "Dynamic destinations must be part of a DynamicNavGraph.\n" +
                            "You can use DynamicNavHostFragment, which will take care of " +
                            "setting up the NavController for Dynamic destinations.\n" +
                            "If you're not using Fragments, you must set up the " +
                            "NavigatorProvider manually."
                    )
            }
        }

        /**
         * The dynamic feature's module name.
         */
        public var moduleName: String? = null

        /**
         * Resource id of progress destination. This will be preferred over any
         * default progress destination set by [installDefaultProgressDestination].
         */
        public var progressDestination: Int = 0

        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            context.withStyledAttributes(attrs, R.styleable.DynamicGraphNavigator) {
                moduleName = getString(R.styleable.DynamicGraphNavigator_moduleName)
                progressDestination = getResourceId(
                    R.styleable.DynamicGraphNavigator_progressDestination, 0
                )
                if (progressDestination == 0) {
                    navGraphNavigator.destinationsWithoutDefaultProgressDestination
                        .add(this@DynamicNavGraph)
                }
            }
        }
    }
}
