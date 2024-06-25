/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation.fragment.compose

import android.os.Bundle
import androidx.annotation.NavigationRes
import androidx.navigation.NavHostController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.plusAssign

/**
 * A [NavHostFragment] that adds support for [ComposableFragment] instances via
 * [ComposableFragmentNavigator].
 */
public open class ComposableNavHostFragment : NavHostFragment() {

    override fun onCreateNavHostController(navHostController: NavHostController) {
        super.onCreateNavHostController(navHostController)
        val navigatorProvider = navHostController.navigatorProvider
        navigatorProvider += ComposableFragmentNavigator(navigatorProvider)
    }

    /** Companion object for ComposableNavHostFragment */
    public companion object {

        /**
         * Create a new [ComposableNavHostFragment] instance with an inflated
         * [androidx.navigation.NavGraph] resource.
         *
         * @param graphResId Resource id of the navigation graph to inflate.
         * @param startDestinationArgs Arguments to send to the start destination of the graph.
         * @return A new ComposableNavHostFragment instance.
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            @NavigationRes graphResId: Int,
            startDestinationArgs: Bundle? = null
        ): ComposableNavHostFragment {
            return ComposableNavHostFragment().apply {
                arguments =
                    if (graphResId != 0 || startDestinationArgs != null) {
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
