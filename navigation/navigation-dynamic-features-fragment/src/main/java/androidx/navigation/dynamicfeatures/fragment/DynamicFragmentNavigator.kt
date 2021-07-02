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

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider
import androidx.navigation.dynamicfeatures.DynamicExtras
import androidx.navigation.dynamicfeatures.DynamicInstallManager
import androidx.navigation.fragment.FragmentNavigator

/**
 * The [Navigator] that enables navigating to destinations within dynamic feature modules.
 */
@Navigator.Name("fragment")
public class DynamicFragmentNavigator(
    context: Context,
    manager: FragmentManager,
    containerId: Int,
    private val installManager: DynamicInstallManager
) : FragmentNavigator(context, manager, containerId) {

    override fun createDestination(): Destination = Destination(this)

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        for (entry in entries) {
            navigate(entry, navOptions, navigatorExtras)
        }
    }

    private fun navigate(
        entry: NavBackStackEntry,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        val destination = entry.destination
        val extras = navigatorExtras as? DynamicExtras
        if (destination is Destination) {
            val moduleName = destination.moduleName
            if (moduleName != null && installManager.needsInstall(moduleName)) {
                installManager.performInstall(entry, extras, moduleName)
                return
            }
        }
        super.navigate(
            listOf(entry),
            navOptions,
            if (extras != null) extras.destinationExtras else navigatorExtras
        )
    }

    /**
     * Destination for dynamic feature navigator.
     */
    public class Destination : FragmentNavigator.Destination {
        public var moduleName: String? = null

        @Suppress("unused")
        public constructor(navigatorProvider: NavigatorProvider) : super(navigatorProvider)

        public constructor(
            fragmentNavigator: Navigator<out FragmentNavigator.Destination>
        ) : super(fragmentNavigator)

        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            context.withStyledAttributes(attrs, R.styleable.DynamicFragmentNavigator) {
                moduleName = getString(R.styleable.DynamicFragmentNavigator_moduleName)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is Destination) return false
            return super.equals(other) && moduleName == other.moduleName
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + moduleName.hashCode()
            return result
        }
    }
}
