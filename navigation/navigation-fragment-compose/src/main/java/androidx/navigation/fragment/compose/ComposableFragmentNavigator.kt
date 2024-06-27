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

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.navigation.NavArgument
import androidx.navigation.NavDestination
import androidx.navigation.NavType
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.get

/**
 * This Navigator intercepts the inflation of `navigation` destinations in a Navigation with
 * Fragment XML file, reusing the `android:name` field as the fully qualified name of the composable
 * function to use as the contents of the inflated destination.
 *
 * Internally, this uses a [ComposableFragment] to implement the reflection call.
 */
@Navigator.Name("composable")
class ComposableFragmentNavigator(private val fragmentNavigator: FragmentNavigator) :
    Navigator<FragmentNavigator.Destination>() {

    /**
     * Construct a [ComposableFragmentNavigator] by retrieving the associated [FragmentNavigator]
     * from [provider].
     */
    constructor(provider: NavigatorProvider) : this(provider[FragmentNavigator::class])

    override fun createDestination(): FragmentNavigator.Destination {
        // Note how we associate the destination with the given
        // fragmentNavigator - not this Navigator. This ensures
        // that the default FragmentNavigator controls the actual
        // navigate / popBackStack calls, not us.
        return Destination(fragmentNavigator)
    }

    @NavDestination.ClassType(Composable::class)
    internal class Destination(fragmentNavigator: Navigator<out FragmentNavigator.Destination>) :
        FragmentNavigator.Destination(fragmentNavigator) {
        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            // The className that was parsed out is actually the fully
            // qualified name of the Composable Function to run, so extract
            // that and add it as a default argument on the destination
            val fullyQualifiedName = className
            val navArgument =
                NavArgument.Builder()
                    .setType(NavType.StringType)
                    .setDefaultValue(fullyQualifiedName)
                    .build()
            addArgument(ComposableFragment.FULLY_QUALIFIED_NAME, navArgument)
            // And then ensure that the actual Fragment that is constructed
            // is our ComposableFragment
            setClassName(ComposableFragment::class.java.name)
        }
    }
}
