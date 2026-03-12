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

package androidx.navigation

import androidx.annotation.IdRes
import androidx.navigation.serialization.generateHashCode
import androidx.navigation.serialization.generateNavArguments
import androidx.navigation.serialization.generateRoutePattern
import androidx.savedstate.SavedState
import androidx.savedstate.savedState
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

@NavDestinationDsl
public actual open class NavDestinationBuilder<out D : NavDestination>
internal constructor(
    protected actual val navigator: Navigator<out D>,
    /** The destination's unique ID. */
    @IdRes public val id: Int,
    public actual val route: String?,
) {

    /**
     * DSL for constructing a new [NavDestination] with a unique id.
     *
     * This sets the destination's [route] to `null`.
     *
     * @param navigator navigator used to create the destination
     * @param id the destination's unique id
     * @return the newly constructed [NavDestination]
     */
    @Deprecated(
        "Use routes to build your NavDestination instead",
        ReplaceWith("NavDestinationBuilder(navigator, route = id.toString())"),
    )
    public constructor(navigator: Navigator<out D>, @IdRes id: Int) : this(navigator, id, null)

    public actual constructor(
        navigator: Navigator<out D>,
        route: String?,
    ) : this(navigator, -1, route)

    @OptIn(InternalSerializationApi::class)
    public actual constructor(
        navigator: Navigator<out D>,
        @Suppress("OptionalBuilderConstructorArgument") route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    ) : this(
        navigator,
        route?.serializer()?.generateHashCode() ?: -1,
        route?.serializer()?.generateRoutePattern(typeMap),
    ) {
        route?.apply {
            serializer().generateNavArguments(typeMap).forEach { arguments[it.name] = it.argument }
        }
        this.typeMap = typeMap
    }

    private lateinit var typeMap: Map<KType, NavType<*>>

    public actual var label: CharSequence? = null

    private var arguments = mutableMapOf<String, NavArgument>()

    public actual fun argument(name: String, argumentBuilder: NavArgumentBuilder.() -> Unit) {
        arguments[name] = NavArgumentBuilder().apply(argumentBuilder).build()
    }

    @Suppress("BuilderSetStyle")
    public actual fun argument(name: String, argument: NavArgument) {
        arguments[name] = argument
    }

    private var deepLinks = mutableListOf<NavDeepLink>()

    public actual fun deepLink(uriPattern: String) {
        deepLinks.add(NavDeepLink(uriPattern))
    }

    @Suppress("BuilderSetStyle")
    @JvmName("deepLinkSafeArgs")
    public actual inline fun <reified T : Any> deepLink(basePath: String) {
        deepLink(T::class, basePath) {}
    }

    public actual fun deepLink(navDeepLink: NavDeepLinkDslBuilder.() -> Unit) {
        deepLinks.add(NavDeepLinkDslBuilder().apply(navDeepLink).build())
    }

    @Suppress("BuilderSetStyle")
    public actual inline fun <reified T : Any> deepLink(
        basePath: String,
        noinline navDeepLink: NavDeepLinkDslBuilder.() -> Unit,
    ) {
        deepLink(T::class, basePath, navDeepLink)
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("BuilderSetStyle")
    public actual fun <T : Any> deepLink(
        route: KClass<T>,
        basePath: String,
        navDeepLink: NavDeepLinkDslBuilder.() -> Unit,
    ) {
        // make sure they used the safe args constructors which automatically adds
        // argument to the destination
        check(this::typeMap.isInitialized) {
            "Cannot add deeplink from KClass [$route]. Use the NavDestinationBuilder " +
                "constructor that takes a KClass with the same arguments."
        }
        val deepLinkArgs = route.serializer().generateNavArguments(typeMap)
        /**
         * Checks for deepLink validity:
         * 1. They used the safe args constructor since we rely on that constructor to add arguments
         *    to the destination
         * 2. DeepLink does not contain extra arguments not present in the destination KClass. We
         *    will not have its NavType. Even if we do, the destination is not aware of the argument
         *    and will just ignore it. In general we don't want safe args deeplinks to introduce new
         *    arguments.
         * 3. DeepLink does not contain different argument type for the same arg name
         *
         * For the case where the deepLink is missing required arguments in the [route], existing
         * checks will catch it.
         */
        deepLinkArgs.forEach {
            val arg = arguments[it.name]
            // make sure deep link doesn't contain extra arguments not present in the route KClass
            // and that it doesn't contain different arg type
            require(arg != null && arg.type == it.argument.type) {
                "Cannot add deeplink from KClass [$route]. DeepLink contains unknown argument " +
                    "[${it.name}]. Ensure deeplink arguments matches the destination's " +
                    "route from KClass"
            }
        }
        deepLink(navDeepLink(route, basePath, typeMap, navDeepLink))
    }

    @Suppress("BuilderSetStyle")
    public actual fun deepLink(navDeepLink: NavDeepLink) {
        deepLinks.add(navDeepLink)
    }

    private var actions = mutableMapOf<Int, NavAction>()

    /** Adds a new [NavAction] to the destination */
    @Deprecated(
        "Building NavDestinations using IDs with the Kotlin DSL has been deprecated in " +
            "favor of using routes. When using routes there is no need for actions."
    )
    public fun action(actionId: Int, actionBuilder: NavActionBuilder.() -> Unit) {
        actions[actionId] = NavActionBuilder().apply(actionBuilder).build()
    }

    @Suppress("BuilderSetStyle")
    protected actual open fun instantiateDestination(): D = navigator.createDestination()

    public actual open fun build(): D {
        return instantiateDestination().also { destination ->
            destination.label = label
            arguments.forEach { (name, argument) -> destination.addArgument(name, argument) }
            deepLinks.forEach { deepLink -> destination.addDeepLink(deepLink) }
            actions.forEach { (actionId, action) -> destination.putAction(actionId, action) }
            if (route != null) {
                destination.route = route
            }
            if (id != -1) {
                destination.id = id
            }
        }
    }
}

/** DSL for building a [NavAction]. */
@NavDestinationDsl
public class NavActionBuilder {
    /** The ID of the destination that should be navigated to when this action is used */
    public var destinationId: Int = 0

    /**
     * The set of default arguments that should be passed to the destination. The keys used here
     * should be the same as those used on the [NavDestinationBuilder.argument] for the destination.
     *
     * All values added here should be able to be added to a [SavedState].
     *
     * @see NavAction.defaultArguments
     */
    public val defaultArguments: MutableMap<String, Any?> = mutableMapOf()

    private var navOptions: NavOptions? = null

    /** Sets the [NavOptions] for this action that should be used by default */
    public fun navOptions(optionsBuilder: NavOptionsBuilder.() -> Unit) {
        navOptions = NavOptionsBuilder().apply(optionsBuilder).build()
    }

    internal fun build() =
        NavAction(
            destinationId,
            navOptions,
            if (defaultArguments.isEmpty()) null else savedState(defaultArguments),
        )
}
