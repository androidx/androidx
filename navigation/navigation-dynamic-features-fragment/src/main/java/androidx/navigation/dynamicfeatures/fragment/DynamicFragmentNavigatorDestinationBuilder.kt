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
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your DynamicFragmentDestination instead",
    ReplaceWith("fragment(route = id.toString())")
)
public inline fun <reified F : Fragment> DynamicNavGraphBuilder.fragment(
    @IdRes id: Int
): Unit = fragment<F>(id) {}

/**
 * Construct a new [DynamicFragmentNavigator.Destination]
 * @param id Destination id.
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your DynamicFragmentDestination instead",
    ReplaceWith("fragment(route = id.toString()) { builder.invoke() }")
)
public inline fun <reified F : Fragment> DynamicNavGraphBuilder.fragment(
    @IdRes id: Int,
    builder: DynamicFragmentNavigatorDestinationBuilder.() -> Unit
): Unit = fragment(id, F::class.java.name, builder)

/**
 * Construct a new [DynamicFragmentNavigator.Destination]
 * @param id Destination id.
 * @param fragmentClassName Fully qualified class name of destination Fragment.
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your DynamicFragmentDestination instead",
    ReplaceWith("fragment(route = id.toString(), fragmentClassName) { builder.invoke() }")
)
public inline fun DynamicNavGraphBuilder.fragment(
    @IdRes id: Int,
    fragmentClassName: String,
    builder: DynamicFragmentNavigatorDestinationBuilder.() -> Unit
): Unit = destination(
    DynamicFragmentNavigatorDestinationBuilder(
        provider[DynamicFragmentNavigator::class],
        id,
        fragmentClassName
    ).apply(builder)
)

/**
 * Construct a new [DynamicFragmentNavigator.Destination]
 * @param route Destination route.
 */
public inline fun <reified F : Fragment> DynamicNavGraphBuilder.fragment(
    route: String
): Unit = fragment<F>(route) {}

/**
 * Construct a new [DynamicFragmentNavigator.Destination]
 * @param route Destination route.
 */
public inline fun <reified F : Fragment> DynamicNavGraphBuilder.fragment(
    route: String,
    builder: DynamicFragmentNavigatorDestinationBuilder.() -> Unit
): Unit = fragment(route, F::class.java.name, builder)

/**
 * Construct a new [DynamicFragmentNavigator.Destination]
 * @param route Destination route.
 * @param fragmentClassName Fully qualified class name of destination Fragment.
 */
public inline fun DynamicNavGraphBuilder.fragment(
    route: String,
    fragmentClassName: String,
    builder: DynamicFragmentNavigatorDestinationBuilder.() -> Unit
): Unit = destination(
    DynamicFragmentNavigatorDestinationBuilder(
        provider[DynamicFragmentNavigator::class],
        route,
        fragmentClassName
    ).apply(builder)
)

/**
 * DSL for constructing a new [DynamicFragmentNavigator.Destination]
 */
@NavDestinationDsl
public class DynamicFragmentNavigatorDestinationBuilder :
    NavDestinationBuilder<FragmentNavigator.Destination> {

    private var fragmentClassName: String

    @Suppress("Deprecation")
    @Deprecated(
        "Use routes to create your DynamicFragmentDestinationBuilder instead",
        ReplaceWith(
            "DynamicFragmentNavigatorDestinationBuilder(navigator, route = id.toString(), " +
                "fragmentClassName)"
        )
    )
    public constructor(
        navigator: DynamicFragmentNavigator,
        @IdRes id: Int,
        fragmentClassName: String
    ) : super(navigator, id) {
        this.fragmentClassName = fragmentClassName
    }

    public constructor(
        navigator: DynamicFragmentNavigator,
        route: String,
        fragmentClassName: String
    ) : super(navigator, route) {
        this.fragmentClassName = fragmentClassName
    }

    public var moduleName: String? = null

    override fun build(): DynamicFragmentNavigator.Destination =
        (super.build() as DynamicFragmentNavigator.Destination).also { destination ->
            destination.setClassName(fragmentClassName)
            destination.moduleName = moduleName
        }
}
