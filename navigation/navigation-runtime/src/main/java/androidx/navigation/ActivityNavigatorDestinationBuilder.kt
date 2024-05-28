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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.navigation

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.IdRes
import kotlin.reflect.KClass
import kotlin.reflect.KType

/** Construct a new [ActivityNavigator.Destination] */
@Suppress("Deprecation")
@Deprecated(
    "Use routes to build your ActivityDestination instead",
    ReplaceWith("activity(route = id.toString()) { builder.invoke() }")
)
public inline fun NavGraphBuilder.activity(
    @IdRes id: Int,
    builder: ActivityNavigatorDestinationBuilder.() -> Unit
): Unit =
    destination(
        ActivityNavigatorDestinationBuilder(provider[ActivityNavigator::class], id).apply(builder)
    )

/** Construct a new [ActivityNavigator.Destination] */
public inline fun NavGraphBuilder.activity(
    route: String,
    builder: ActivityNavigatorDestinationBuilder.() -> Unit
): Unit =
    destination(
        ActivityNavigatorDestinationBuilder(provider[ActivityNavigator::class], route)
            .apply(builder)
    )

/**
 * Construct a new [ActivityNavigator.Destination]
 *
 * @param T destination's unique route from a [KClass]
 * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
 *   [NavType]. May be empty if [T] does not use custom NavTypes.
 * @param builder the builder used to construct the fragment destination
 */
public inline fun <reified T : Any> NavGraphBuilder.activity(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: ActivityNavigatorDestinationBuilder.() -> Unit
): Unit =
    destination(
        ActivityNavigatorDestinationBuilder(provider[ActivityNavigator::class], T::class, typeMap)
            .apply(builder)
    )

/** DSL for constructing a new [ActivityNavigator.Destination] */
@NavDestinationDsl
public class ActivityNavigatorDestinationBuilder :
    NavDestinationBuilder<ActivityNavigator.Destination> {
    private var context: Context

    @Suppress("Deprecation")
    @Deprecated(
        "Use routes to create your ActivityNavigatorDestinationBuilder instead",
        ReplaceWith("ActivityNavigatorDestinationBuilder(navigator, route = id.toString())")
    )
    public constructor(navigator: ActivityNavigator, @IdRes id: Int) : super(navigator, id) {
        context = navigator.context
    }

    public constructor(navigator: ActivityNavigator, route: String) : super(navigator, route) {
        context = navigator.context
    }

    /**
     * DSL for constructing a new [ActivityNavigator.Destination]
     *
     * @param navigator navigator used to create the destination
     * @param route the route from a [KClass] of the destination
     * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
     *   [NavType]. May be empty if [route] does not use custom NavTypes.
     */
    public constructor(
        navigator: ActivityNavigator,
        route: KClass<out Any>,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    ) : super(navigator, route, typeMap) {
        context = navigator.context
    }

    public var targetPackage: String? = null

    public var activityClass: KClass<out Activity>? = null

    public var action: String? = null

    public var data: Uri? = null

    public var dataPattern: String? = null

    override fun build(): ActivityNavigator.Destination =
        super.build().also { destination ->
            destination.setTargetPackage(targetPackage)
            activityClass?.let { clazz ->
                destination.setComponentName(ComponentName(context, clazz.java))
            }
            destination.setAction(action)
            destination.setData(data)
            destination.setDataPattern(dataPattern)
        }
}
