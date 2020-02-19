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

import androidx.navigation.NavController
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
open class DynamicNavHostFragment : NavHostFragment() {

    override fun onCreateNavController(navController: NavController) {
        super.onCreateNavController(navController)

        val installManager = DynamicInstallManager(requireContext(), createSplitInstallManager())
        val navigatorProvider = navController.navigatorProvider

        navigatorProvider += DynamicActivityNavigator(requireActivity(), installManager)

        val fragmentNavigator = DynamicFragmentNavigator(requireContext(),
            childFragmentManager, id, installManager)
        navigatorProvider += fragmentNavigator

        val graphNavigator = DynamicGraphNavigator(
            navigatorProvider,
            installManager
        )
        graphNavigator.installDefaultProgressDestination {
            fragmentNavigator.createDestination().apply {
                className = DefaultProgressFragment::class.java.name
                id = R.id.dfn_progress_fragment
            }
        }
        navigatorProvider += graphNavigator

        navigatorProvider += DynamicIncludeGraphNavigator(requireContext(),
            navigatorProvider, navController.navInflater, installManager)
    }

    /**
     * Create a new [SplitInstallManager].
     */
    protected open fun createSplitInstallManager(): SplitInstallManager =
        SplitInstallManagerFactory.create(requireContext())
}
