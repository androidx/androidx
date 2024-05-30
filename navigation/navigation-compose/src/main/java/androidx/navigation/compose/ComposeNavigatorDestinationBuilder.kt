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

package androidx.navigation.compose

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavDestinationDsl
import androidx.navigation.NavType
import kotlin.reflect.KClass
import kotlin.reflect.KType

/** DSL for constructing a new [ComposeNavigator.Destination] */
@NavDestinationDsl
public class ComposeNavigatorDestinationBuilder :
    NavDestinationBuilder<ComposeNavigator.Destination> {

    private val composeNavigator: ComposeNavigator
    private val content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit)

    var enterTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        null

    var exitTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        null

    var popEnterTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        null

    var popExitTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        null

    var sizeTransform:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)? =
        null

    /**
     * DSL for constructing a new [ComposeNavigator.Destination]
     *
     * @param navigator navigator used to create the destination
     * @param route the destination's unique route
     * @param content composable for the destination
     */
    public constructor(
        navigator: ComposeNavigator,
        route: String,
        content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
    ) : super(navigator, route) {
        this.composeNavigator = navigator
        this.content = content
    }

    /**
     * DSL for constructing a new [ComposeNavigator.Destination]
     *
     * @param navigator navigator used to create the destination
     * @param route the destination's unique route from a [KClass]
     * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
     *   [NavType]. May be empty if [route] does not use custom NavTypes.
     * @param content composable for the destination
     */
    public constructor(
        navigator: ComposeNavigator,
        route: KClass<*>,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
        content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
    ) : super(navigator, route, typeMap) {
        this.composeNavigator = navigator
        this.content = content
    }

    override fun instantiateDestination(): ComposeNavigator.Destination {
        return ComposeNavigator.Destination(composeNavigator, content)
    }

    override fun build(): ComposeNavigator.Destination {
        return super.build().also { destination ->
            destination.enterTransition = enterTransition
            destination.exitTransition = exitTransition
            destination.popEnterTransition = popEnterTransition
            destination.popExitTransition = popExitTransition
            destination.sizeTransform = sizeTransform
        }
    }
}
