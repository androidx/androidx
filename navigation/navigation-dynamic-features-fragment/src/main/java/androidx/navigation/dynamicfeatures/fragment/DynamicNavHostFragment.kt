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

package androidx.navigation.dynamicfeatures.fragment

import android.os.Bundle
import androidx.annotation.NavigationRes
import androidx.navigation.NavHostController
import androidx.navigation.dynamicfeatures.DynamicActivityNavigator
import androidx.navigation.dynamicfeatures.DynamicGraphNavigator
import androidx.navigation.dynamicfeatures.DynamicIncludeGraphNavigator
import androidx.navigation.dynamicfeatures.DynamicInstallManager
import androidx.navigation.dynamicfeatures.fragment.ui.DefaultProgressFragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.plusAssign
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory

/**
 * The [NavHostFragment] for dynamic features.
 */
public open class DynamicNavHostFragment : NavHostFragment() {

    override fun onCreateNavHostController(navHostController: NavHostController) {
        super.onCreateNavHostController(navHostController)

        val installManager = DynamicInstallManager(requireContext(), createSplitInstallManager())
        val navigatorProvider = navHostController.navigatorProvider

        navigatorProvider += DynamicActivityNavigator(requireActivity(), installManager)

        val fragmentNavigator = DynamicFragmentNavigator(
            requireContext(),
            childFragmentManager, id, installManager
        )
        navigatorProvider += fragmentNavigator

        val graphNavigator = DynamicGraphNavigator(
            navigatorProvider,
            installManager
        )
        graphNavigator.installDefaultProgressDestination {
            fragmentNavigator.createDestination().apply {
                setClassName(DefaultProgressFragment::class.java.name)
                id = R.id.dfn_progress_fragment
            }
        }
        navigatorProvider += graphNavigator

        navigatorProvider += DynamicIncludeGraphNavigator(
            requireContext(),
            navigatorProvider, navHostController.navInflater, installManager
        )
    }

    /**
     * Create a new [SplitInstallManager].
     */
    protected open fun createSplitInstallManager(): SplitInstallManager =
        SplitInstallManagerFactory.create(requireContext())

    /** Companion object for DynamicNavHostFragment */
    public companion object {

        /**
         * Create a new [DynamicNavHostFragment] instance with an inflated {@link NavGraph} resource.
         *
         * @param graphResId Resource id of the navigation graph to inflate.
         * @param startDestinationArgs Arguments to send to the start destination of the graph.
         * @return A new DynamicNavHostFragment instance.
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            @NavigationRes graphResId: Int,
            startDestinationArgs: Bundle? = null
        ): DynamicNavHostFragment {
            return DynamicNavHostFragment().apply {
                arguments = if (graphResId != 0 || startDestinationArgs != null) {
                    Bundle().apply {
                        if (graphResId != 0) {
                            putInt(KEY_GRAPH_ID, graphResId)
                        }
                        if (startDestinationArgs != null) {
                            putBundle(KEY_START_DESTINATION_ARGS, startDestinationArgs)
                        }
                    }
                } else null
            }
        }
    }
}
