/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.dynamicfeatures.DynamicNavGraphBuilder
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.fragment
import androidx.navigation.get

/**
 * Construct a new [DynamicFragmentNavigator.Destination]
 * @param id Destination id.
 */
inline fun <reified F : Fragment> DynamicNavGraphBuilder.fragment(
    @IdRes id: Int
) = fragment<F>(id) {}

/**
 * Construct a new [DynamicFragmentNavigator.Destination]
 * @param id Destination id.
 */
inline fun <reified F : Fragment> DynamicNavGraphBuilder.fragment(
    @IdRes id: Int,
    builder: DynamicFragmentNavigatorDestinationBuilder.() -> Unit
) = fragment(id, F::class.java.name, builder)

/**
 * Construct a new [DynamicFragmentNavigator.Destination]
 * @param id Destination id.
 * @param fragmentClassName Fully qualified class name of destination Fragment.
 */
inline fun DynamicNavGraphBuilder.fragment(
    @IdRes id: Int,
    fragmentClassName: String,
    builder: DynamicFragmentNavigatorDestinationBuilder.() -> Unit
) = destination(DynamicFragmentNavigatorDestinationBuilder(
        provider[DynamicFragmentNavigator::class],
        id,
        fragmentClassName
).apply(builder))

/**
 * DSL for constructing a new [DynamicFragmentNavigator.Destination]
 */
@NavDestinationDsl
class DynamicFragmentNavigatorDestinationBuilder(
    navigator: DynamicFragmentNavigator,
    @IdRes id: Int,
    private val fragmentClassName: String
) : NavDestinationBuilder<FragmentNavigator.Destination>(navigator, id) {

    var moduleName: String? = null

    override fun build() =
        (super.build() as DynamicFragmentNavigator.Destination).also { destination ->
                destination.className = fragmentClassName
                destination.moduleName = moduleName
            }
}
