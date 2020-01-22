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
import android.content.res.Resources
import android.os.Bundle
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.navigation.NavDestination
import androidx.navigation.NavInflater
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider
import androidx.navigation.get

/**
 * Navigator for `include-dynamic`.
 *
 * Use it for navigating to NavGraphs contained within a dynamic feature module.
 */
@Navigator.Name("include-dynamic")
class DynamicIncludeGraphNavigator(
    private val context: Context,
    private val navigatorProvider: NavigatorProvider,
    private val navInflater: NavInflater,
    private val installManager: DynamicInstallManager
) : Navigator<DynamicIncludeGraphNavigator.DynamicIncludeNavGraph>() {

    override fun createDestination(): DynamicIncludeNavGraph {
        return DynamicIncludeNavGraph(this)
    }

    /**
     * @throws Resources.NotFoundException if the [destination] does not have a valid
     * `graphResourceName` and `graphPackage`.
     * @throws IllegalStateException if the [destination] does not have a parent.
     */
    override fun navigate(
        destination: DynamicIncludeNavGraph,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination? {
        val extras = navigatorExtras as? DynamicExtras

        val moduleName = destination.moduleName

        if (moduleName != null && installManager.needsInstall(moduleName)) {
            return installManager.performInstall(destination, args, extras, moduleName)
        } else {
            val graphId = context.resources.getIdentifier(
                destination.graphResourceName, "navigation",
                destination.graphPackage)
            if (graphId == 0) {
                throw Resources.NotFoundException(
                    "${destination.graphPackage}:navigation/${destination.graphResourceName}")
            }
            val includedNav = navInflater.inflate(graphId)
            check(!(includedNav.id != 0 && includedNav.id != destination.id)) {
                "The included <navigation>'s id is different from " +
                        "the destination id. Either remove the <navigation> id or make them " +
                        " match."
            }
            includedNav.id = destination.id
            val outerNav = destination.parent
                ?: throw IllegalStateException(
                    "The destination ${destination.id} does not have a parent. " +
                            "Make sure it is attached to a NavGraph.")
            // no need to remove previous destination, id is used as key in map
            outerNav.addDestination(includedNav)
            val navigator: Navigator<NavDestination> = navigatorProvider[includedNav.navigatorName]
            return navigator.navigate(includedNav, args, navOptions, navigatorExtras)
        }
    }

    override fun popBackStack() = true

    /**
     * The graph for dynamic-include.
     *
     * This class contains information to navigate to a DynamicNavGraph which is contained
     * within a dynamic feature module.
     */
    class DynamicIncludeNavGraph
    internal constructor(navGraphNavigator: Navigator<out NavDestination>) :
        NavDestination(navGraphNavigator) {

        /**
         * Resource name of the graph.
         */
        var graphResourceName: String? = null

        /**
         * The graph's package.
         */
        var graphPackage: String? = null

        /**
         * Name of the module containing the included graph, if set.
         */
        var moduleName: String? = null

        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            context.withStyledAttributes(attrs, R.styleable.DynamicIncludeGraphNavigator) {
                graphPackage = getString(R.styleable.DynamicIncludeGraphNavigator_graphPackage)
                require(!graphPackage.isNullOrEmpty()) {
                    "graphPackage must be set for dynamic navigation"
                }

                graphResourceName =
                    getString(R.styleable.DynamicIncludeGraphNavigator_graphResName)
                require(!graphPackage.isNullOrEmpty()) {
                    "graphResName must be set for dynamic navigation"
                }

                moduleName = getString(R.styleable.DynamicIncludeGraphNavigator_moduleName)
            }
        }
    }
}
