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

package androidx.navigation.compose

import androidx.annotation.RestrictTo
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.get
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Add the [Composable] to the [NavGraphBuilder]
 *
 * @param route route for the destination
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param content composable for the destination
 */
@Deprecated(
    message = "Deprecated in favor of composable builder that supports AnimatedContent",
    level = DeprecationLevel.HIDDEN
)
public fun NavGraphBuilder.composable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit
) {
    addDestination(
        ComposeNavigator.Destination(provider[ComposeNavigator::class]) { entry -> content(entry) }
            .apply {
                this.route = route
                arguments.forEach { (argumentName, argument) ->
                    addArgument(argumentName, argument)
                }
                deepLinks.forEach { deepLink -> addDeepLink(deepLink) }
            }
    )
}

/**
 * Add the [Composable] to the [NavGraphBuilder]
 *
 * @param route route for the destination
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param enterTransition callback to determine the destination's enter transition
 * @param exitTransition callback to determine the destination's exit transition
 * @param popEnterTransition callback to determine the destination's popEnter transition
 * @param popExitTransition callback to determine the destination's popExit transition
 * @param content composable for the destination
 */
@Deprecated(
    message = "Deprecated in favor of composable builder that supports sizeTransform",
    level = DeprecationLevel.HIDDEN
)
public fun NavGraphBuilder.composable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        null,
    exitTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        null,
    popEnterTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        enterTransition,
    popExitTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        exitTransition,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    destination(
        ComposeNavigatorDestinationBuilder(provider[ComposeNavigator::class], route, content)
            .apply {
                arguments.forEach { (argumentName, argument) -> argument(argumentName, argument) }
                deepLinks.forEach { deepLink -> deepLink(deepLink) }
                this.enterTransition = enterTransition
                this.exitTransition = exitTransition
                this.popEnterTransition = popEnterTransition
                this.popExitTransition = popExitTransition
            }
    )
}

/**
 * Add the [Composable] to the [NavGraphBuilder]
 *
 * @param route route for the destination
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param enterTransition callback to determine the destination's enter transition
 * @param exitTransition callback to determine the destination's exit transition
 * @param popEnterTransition callback to determine the destination's popEnter transition
 * @param popExitTransition callback to determine the destination's popExit transition
 * @param sizeTransform callback to determine the destination's sizeTransform.
 * @param content composable for the destination
 */
public fun NavGraphBuilder.composable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        null,
    exitTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        null,
    popEnterTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        enterTransition,
    popExitTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        exitTransition,
    sizeTransform:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)? =
        null,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    destination(
        ComposeNavigatorDestinationBuilder(provider[ComposeNavigator::class], route, content)
            .apply {
                arguments.forEach { (argumentName, argument) -> argument(argumentName, argument) }
                deepLinks.forEach { deepLink -> deepLink(deepLink) }
                this.enterTransition = enterTransition
                this.exitTransition = exitTransition
                this.popEnterTransition = popEnterTransition
                this.popExitTransition = popExitTransition
                this.sizeTransform = sizeTransform
            }
    )
}

/**
 * Add the [Composable] to the [NavGraphBuilder]
 *
 * @param T route from a [KClass] for the destination
 * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
 *   [NavType]. May be empty if [T] does not use custom NavTypes.
 * @param deepLinks list of deep links to associate with the destinations
 * @param enterTransition callback to determine the destination's enter transition
 * @param exitTransition callback to determine the destination's exit transition
 * @param popEnterTransition callback to determine the destination's popEnter transition
 * @param popExitTransition callback to determine the destination's popExit transition
 * @param sizeTransform callback to determine the destination's sizeTransform.
 * @param content composable for the destination
 */
public inline fun <reified T : Any> NavGraphBuilder.composable(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline enterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        null,
    noinline exitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        null,
    noinline popEnterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        enterTransition,
    noinline popExitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        exitTransition,
    noinline sizeTransform:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            SizeTransform?)? =
        null,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    destination(
        ComposeNavigatorDestinationBuilder(
                provider[ComposeNavigator::class],
                T::class,
                typeMap,
                content
            )
            .apply {
                deepLinks.forEach { deepLink -> deepLink(deepLink) }
                this.enterTransition = enterTransition
                this.exitTransition = exitTransition
                this.popEnterTransition = popEnterTransition
                this.popExitTransition = popExitTransition
                this.sizeTransform = sizeTransform
            }
    )
}

/**
 * Construct a nested [NavGraph]
 *
 * @sample androidx.navigation.compose.samples.NavWithArgsInNestedGraph
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param builder the builder used to construct the graph
 */
@Deprecated(
    message = "Deprecated in favor of navigation builder that supports AnimatedContent",
    level = DeprecationLevel.HIDDEN
)
public fun NavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    builder: NavGraphBuilder.() -> Unit
) {
    navigation(startDestination, route, arguments, deepLinks, null, null, null, null, null, builder)
}

/**
 * Construct a nested [NavGraph]
 *
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param enterTransition callback to define enter transitions for destination in this NavGraph
 * @param exitTransition callback to define exit transitions for destination in this NavGraph
 * @param popEnterTransition callback to define pop enter transitions for destination in this
 *   NavGraph
 * @param popExitTransition callback to define pop exit transitions for destination in this NavGraph
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
@Deprecated(
    message = "Deprecated in favor of navigation builder that supports sizeTransform",
    level = DeprecationLevel.HIDDEN
)
public fun NavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        null,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        null,
    popEnterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        enterTransition,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        exitTransition,
    builder: NavGraphBuilder.() -> Unit
) {
    navigation(
        startDestination,
        route,
        arguments,
        deepLinks,
        enterTransition,
        exitTransition,
        popEnterTransition,
        popExitTransition,
        null,
        builder
    )
}

/**
 * Construct a nested [NavGraph]
 *
 * @sample androidx.navigation.compose.samples.SizeTransformNav
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param enterTransition callback to define enter transitions for destination in this NavGraph
 * @param exitTransition callback to define exit transitions for destination in this NavGraph
 * @param popEnterTransition callback to define pop enter transitions for destination in this
 *   NavGraph
 * @param popExitTransition callback to define pop exit transitions for destination in this NavGraph
 * @param sizeTransform callback to define the size transform for destinations in this NavGraph
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public fun NavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        null,
    exitTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        null,
    popEnterTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        enterTransition,
    popExitTransition:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        exitTransition,
    sizeTransform:
        (@JvmSuppressWildcards
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)? =
        null,
    builder: NavGraphBuilder.() -> Unit
) {
    addDestination(
        NavGraphBuilder(provider, startDestination, route).apply(builder).build().apply {
            arguments.forEach { (argumentName, argument) -> addArgument(argumentName, argument) }
            deepLinks.forEach { deepLink -> addDeepLink(deepLink) }
            if (this is ComposeNavGraphNavigator.ComposeNavGraph) {
                this.enterTransition = enterTransition
                this.exitTransition = exitTransition
                this.popEnterTransition = popEnterTransition
                this.popExitTransition = popExitTransition
                this.sizeTransform = sizeTransform
            }
        }
    )
}

/**
 * Construct a nested [NavGraph]
 *
 * @sample androidx.navigation.compose.samples.SizeTransformNav
 * @param T the destination's unique route from a KClass
 * @param startDestination the starting destination's route from [KClass] for this NavGraph
 * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
 *   [NavType]. May be empty if [T] does not use custom NavTypes.
 * @param deepLinks list of deep links to associate with the destinations
 * @param enterTransition callback to define enter transitions for destination in this NavGraph
 * @param exitTransition callback to define exit transitions for destination in this NavGraph
 * @param popEnterTransition callback to define pop enter transitions for destination in this
 *   NavGraph
 * @param popExitTransition callback to define pop exit transitions for destination in this NavGraph
 * @param sizeTransform callback to define the size transform for destinations in this NavGraph
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public inline fun <reified T : Any> NavGraphBuilder.navigation(
    startDestination: KClass<*>,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline enterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        null,
    noinline exitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        null,
    noinline popEnterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        enterTransition,
    noinline popExitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        exitTransition,
    noinline sizeTransform:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            SizeTransform?)? =
        null,
    noinline builder: NavGraphBuilder.() -> Unit
) {
    navigation(
        startDestination,
        T::class,
        typeMap,
        deepLinks,
        enterTransition,
        exitTransition,
        popEnterTransition,
        popExitTransition,
        sizeTransform,
        builder
    )
}

// need to be public for reified navigation
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun NavGraphBuilder.navigation(
    startDestination: KClass<*>,
    route: KClass<*>,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        null,
    exitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        null,
    popEnterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        enterTransition,
    popExitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        exitTransition,
    sizeTransform:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            SizeTransform?)? =
        null,
    builder: NavGraphBuilder.() -> Unit
) {
    addDestination(
        NavGraphBuilder(provider, startDestination, route, typeMap).apply(builder).build().apply {
            deepLinks.forEach { deepLink -> addDeepLink(deepLink) }
            if (this is ComposeNavGraphNavigator.ComposeNavGraph) {
                this.enterTransition = enterTransition
                this.exitTransition = exitTransition
                this.popEnterTransition = popEnterTransition
                this.popExitTransition = popExitTransition
                this.sizeTransform = sizeTransform
            }
        }
    )
}

/**
 * Construct a nested [NavGraph]
 *
 * @sample androidx.navigation.compose.samples.SizeTransformNav
 * @param T the destination's unique route from a KClass
 * @param startDestination the starting destination's route from an Object for this NavGraph
 * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
 *   [NavType]. May be empty if [T] does not use custom NavTypes.
 * @param deepLinks list of deep links to associate with the destinations
 * @param enterTransition callback to define enter transitions for destination in this NavGraph
 * @param exitTransition callback to define exit transitions for destination in this NavGraph
 * @param popEnterTransition callback to define pop enter transitions for destination in this
 *   NavGraph
 * @param popExitTransition callback to define pop exit transitions for destination in this NavGraph
 * @param sizeTransform callback to define the size transform for destinations in this NavGraph
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public inline fun <reified T : Any> NavGraphBuilder.navigation(
    startDestination: Any,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline enterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        null,
    noinline exitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        null,
    noinline popEnterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        enterTransition,
    noinline popExitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        exitTransition,
    noinline sizeTransform:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            SizeTransform?)? =
        null,
    noinline builder: NavGraphBuilder.() -> Unit
) {
    navigation(
        startDestination,
        T::class,
        typeMap,
        deepLinks,
        enterTransition,
        exitTransition,
        popEnterTransition,
        popExitTransition,
        sizeTransform,
        builder
    )
}

// need to be public for reified navigation
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun NavGraphBuilder.navigation(
    startDestination: Any,
    route: KClass<*>,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        null,
    exitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        null,
    popEnterTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            EnterTransition?)? =
        enterTransition,
    popExitTransition:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            ExitTransition?)? =
        exitTransition,
    sizeTransform:
        (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
            SizeTransform?)? =
        null,
    builder: NavGraphBuilder.() -> Unit
) {
    addDestination(
        NavGraphBuilder(provider, startDestination, route, typeMap).apply(builder).build().apply {
            deepLinks.forEach { deepLink -> addDeepLink(deepLink) }
            if (this is ComposeNavGraphNavigator.ComposeNavGraph) {
                this.enterTransition = enterTransition
                this.exitTransition = exitTransition
                this.popEnterTransition = popEnterTransition
                this.popExitTransition = popExitTransition
                this.sizeTransform = sizeTransform
            }
        }
    )
}

/**
 * Add the [Composable] to the [NavGraphBuilder] that will be hosted within a
 * [androidx.compose.ui.window.Dialog]. This is suitable only when this dialog represents a separate
 * screen in your app that needs its own lifecycle and saved state, independent of any other
 * destination in your navigation graph. For use cases such as `AlertDialog`, you should use those
 * APIs directly in the [composable] destination that wants to show that dialog.
 *
 * @param route route for the destination
 * @param arguments list of arguments to associate with destination
 * @param deepLinks list of deep links to associate with the destinations
 * @param dialogProperties properties that should be passed to [androidx.compose.ui.window.Dialog].
 * @param content composable content for the destination that will be hosted within the Dialog
 */
public fun NavGraphBuilder.dialog(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    dialogProperties: DialogProperties = DialogProperties(),
    content: @Composable (NavBackStackEntry) -> Unit
) {
    destination(
        DialogNavigatorDestinationBuilder(
                provider[DialogNavigator::class],
                route,
                dialogProperties,
                content
            )
            .apply {
                arguments.forEach { (argumentName, argument) -> argument(argumentName, argument) }
                deepLinks.forEach { deepLink -> deepLink(deepLink) }
            }
    )
}

/**
 * Add the [Composable] to the [NavGraphBuilder] that will be hosted within a
 * [androidx.compose.ui.window.Dialog]. This is suitable only when this dialog represents a separate
 * screen in your app that needs its own lifecycle and saved state, independent of any other
 * destination in your navigation graph. For use cases such as `AlertDialog`, you should use those
 * APIs directly in the [composable] destination that wants to show that dialog.
 *
 * @param T route from a KClass for the destination
 * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
 *   [NavType]. May be empty if [T] does not use custom NavTypes.
 * @param deepLinks list of deep links to associate with the destinations
 * @param dialogProperties properties that should be passed to [androidx.compose.ui.window.Dialog].
 * @param content composable content for the destination that will be hosted within the Dialog
 */
public inline fun <reified T : Any> NavGraphBuilder.dialog(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    dialogProperties: DialogProperties = DialogProperties(),
    noinline content: @Composable (NavBackStackEntry) -> Unit
) {
    destination(
        DialogNavigatorDestinationBuilder(
                provider[DialogNavigator::class],
                T::class,
                typeMap,
                dialogProperties,
                content
            )
            .apply { deepLinks.forEach { deepLink -> deepLink(deepLink) } }
    )
}
