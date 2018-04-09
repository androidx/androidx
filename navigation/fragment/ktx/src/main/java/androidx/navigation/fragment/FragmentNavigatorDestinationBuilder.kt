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

package androidx.navigation.fragment

import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavGraphBuilder
import androidx.navigation.get
import kotlin.reflect.KClass

/**
 * Construct a new [FragmentNavigator.Destination]
 */
inline fun <reified F : Fragment> NavGraphBuilder.fragment(@IdRes id: Int) = fragment<F>(id) {}

/**
 * Construct a new [FragmentNavigator.Destination]
 */
inline fun <reified F : Fragment> NavGraphBuilder.fragment(
        @IdRes id: Int,
        block: FragmentNavigatorDestinationBuilder.() -> Unit
) = destination(FragmentNavigatorDestinationBuilder(
        provider[FragmentNavigator::class],
        id,
        F::class
).apply(block))

/**
 * DSL for constructing a new [FragmentNavigator.Destination]
 */
@NavDestinationDsl
class FragmentNavigatorDestinationBuilder(
        navigator: FragmentNavigator,
        @IdRes id: Int,
        private val fragmentClass: KClass<out Fragment>
) : NavDestinationBuilder<FragmentNavigator.Destination>(navigator, id) {

    override fun build(): FragmentNavigator.Destination =
            super.build().also { destination ->
                destination.fragmentClass = fragmentClass.java
            }
}
