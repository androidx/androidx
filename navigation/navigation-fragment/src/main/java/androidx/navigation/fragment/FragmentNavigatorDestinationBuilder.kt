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

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavGraphBuilder
import androidx.navigation.get
import kotlin.reflect.KClass

/**
 * Construct a new [FragmentNavigator.Destination]
 *
 * @param id the destination's unique id
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your FragmentDestination instead",
    ReplaceWith("fragment<F>(route = id.toString())")
)
public inline fun <reified F : Fragment> NavGraphBuilder.fragment(
    @IdRes id: Int
): Unit = fragment<F>(id) {}

/**
 * Construct a new [FragmentNavigator.Destination]
 *
 * @param id the destination's unique id
 * @param builder the builder used to construct the fragment destination
 */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to create your FragmentDestination instead",
    ReplaceWith("fragment<F>(route = id.toString()) { builder.invoke() }")
)
public inline fun <reified F : Fragment> NavGraphBuilder.fragment(
    @IdRes id: Int,
    builder: FragmentNavigatorDestinationBuilder.() -> Unit
): Unit = destination(
    FragmentNavigatorDestinationBuilder(
        provider[FragmentNavigator::class],
        id,
        F::class
    ).apply(builder)
)

/**
 * Construct a new [FragmentNavigator.Destination]
 *
 * @param route the destination's unique route
 */
public inline fun <reified F : Fragment> NavGraphBuilder.fragment(
    route: String
): Unit = fragment<F>(route) {}

/**
 * Construct a new [FragmentNavigator.Destination]
 *
 * @param route the destination's unique route
 * @param builder the builder used to construct the fragment destination
 */
public inline fun <reified F : Fragment> NavGraphBuilder.fragment(
    route: String,
    builder: FragmentNavigatorDestinationBuilder.() -> Unit
): Unit = destination(
    FragmentNavigatorDestinationBuilder(
        provider[FragmentNavigator::class],
        route,
        F::class
    ).apply(builder)
)

/**
 * DSL for constructing a new [FragmentNavigator.Destination]
 */
@NavDestinationDsl
public class FragmentNavigatorDestinationBuilder :
    NavDestinationBuilder<FragmentNavigator.Destination> {

    private var fragmentClass: KClass<out Fragment>

    /**
     * DSL for constructing a new [FragmentNavigator.Destination]
     *
     * @param navigator navigator used to create the destination
     * @param id the destination's unique id
     * @param fragmentClass The class name of the Fragment to show when you navigate to this
     * destination
     */
    @Suppress("Deprecation")
    @Deprecated(
        "Use routes to build your FragmentNavigatorDestination instead",
        ReplaceWith(
            "FragmentNavigatorDestinationBuilder(navigator, route = id.toString(), fragmentClass) "
        )
    )
    public constructor(
        navigator: FragmentNavigator,
        @IdRes id: Int,
        fragmentClass: KClass<out Fragment>
    ) : super(navigator, id) {
        this.fragmentClass = fragmentClass
    }

    /**
     * DSL for constructing a new [FragmentNavigator.Destination]
     *
     * @param navigator navigator used to create the destination
     * @param route the destination's unique route
     * @param fragmentClass The class name of the Fragment to show when you navigate to this
     * destination
     */
    public constructor(
        navigator: FragmentNavigator,
        route: String,
        fragmentClass: KClass<out Fragment>
    ) : super(navigator, route) {
        this.fragmentClass = fragmentClass
    }

    override fun build(): FragmentNavigator.Destination =
        super.build().also { destination ->
            destination.setClassName(fragmentClass.java.name)
        }
}
