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
import android.util.AttributeSet
import androidx.annotation.RestrictTo
import androidx.core.content.withStyledAttributes
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider

/**
 * Dynamic feature navigator for Activity destinations.
 */
@Navigator.Name("activity")
public class DynamicActivityNavigator(
    context: Context,
    private val installManager: DynamicInstallManager
) : ActivityNavigator(context) {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public val packageName: String = context.packageName

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
            }
        }
        super.navigate(
            listOf(entry),
            navOptions,
            if (extras != null) extras.destinationExtras else navigatorExtras
        )
    }

    override fun createDestination(): Destination = Destination(this)

    /**
     * Destination for [DynamicActivityNavigator].
     */
    public class Destination : ActivityNavigator.Destination {
        /**
         * The module name of this [Destination]'s dynamic feature module. This has to be the
         * same as defined in the dynamic feature module's AndroidManifest.xml file.
         */
        public var moduleName: String? = null

        /**
         * Create a new [Destination] with a [NavigatorProvider].
         * @see ActivityNavigator.Destination
         */
        public constructor(navigatorProvider: NavigatorProvider) : super(navigatorProvider)

        /**
         * Create a new [Destination] with an [ActivityNavigator.Destination].
         * @param activityNavigator The Navigator to use for this [Destination].
         */
        public constructor(
            activityNavigator: Navigator<out ActivityNavigator.Destination>
        ) : super(activityNavigator)

        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            context.withStyledAttributes(attrs, R.styleable.DynamicActivityNavigator) {
                moduleName = getString(R.styleable.DynamicActivityNavigator_moduleName)
            }
        }
    }
}
