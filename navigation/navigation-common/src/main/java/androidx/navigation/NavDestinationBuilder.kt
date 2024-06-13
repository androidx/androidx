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
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import androidx.navigation.serialization.generateHashCode
import androidx.navigation.serialization.generateNavArguments
import androidx.navigation.serialization.generateRoutePattern
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

@DslMarker public annotation class NavDestinationDsl

/** DSL for constructing a new [NavDestination] */
@NavDestinationDsl
public open class NavDestinationBuilder<out D : NavDestination>
internal constructor(
    /**
     * The navigator the destination that will be used in [instantiateDestination] to create the
     * destination.
     */
    protected val navigator: Navigator<out D>,
    /** The destination's unique ID. */
    @IdRes public val id: Int,
    /** The destination's unique route. */
    public val route: String?
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
        ReplaceWith("NavDestinationBuilder(navigator, route = id.toString())")
    )
    public constructor(navigator: Navigator<out D>, @IdRes id: Int) : this(navigator, id, null)

    /**
     * DSL for constructing a new [NavDestination] with a unique route.
     *
     * This will also update the [id] of the destination based on route.
     *
     * @param navigator navigator used to create the destination
     * @param route the destination's unique route
     * @return the newly constructed [NavDestination]
     */
    public constructor(navigator: Navigator<out D>, route: String?) : this(navigator, -1, route)

    /**
     * DSL for constructing a new [NavDestination] with a serializable [KClass].
     *
     * This will also update the [id] of the destination based on KClass's serializer.
     *
     * @param navigator navigator used to create the destination
     * @param route the [KClass] of the destination
     * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
     *   [NavType]. May be empty if destination does not use custom NavTypes.
     * @return the newly constructed [NavDestination]
     */
    @OptIn(InternalSerializationApi::class)
    public constructor(
        navigator: Navigator<out D>,
        @Suppress("OptionalBuilderConstructorArgument") route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    ) : this(
        navigator,
        route?.serializer()?.generateHashCode() ?: -1,
        route?.serializer()?.generateRoutePattern(typeMap)
    ) {
        route?.apply {
            serializer().generateNavArguments(typeMap).forEach { arguments[it.name] = it.argument }
        }
        this.typeMap = typeMap
    }

    private lateinit var typeMap: Map<KType, NavType<*>>

    /** The descriptive label of the destination */
    public var label: CharSequence? = null

    private var arguments = mutableMapOf<String, NavArgument>()

    /** Add a [NavArgument] to this destination. */
    public fun argument(name: String, argumentBuilder: NavArgumentBuilder.() -> Unit) {
        arguments[name] = NavArgumentBuilder().apply(argumentBuilder).build()
    }

    /** Add a [NavArgument] to this destination. */
    @Suppress("BuilderSetStyle")
    public fun argument(name: String, argument: NavArgument) {
        arguments[name] = argument
    }

    private var deepLinks = mutableListOf<NavDeepLink>()

    /**
     * Add a deep link to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     * * Uris without a scheme are assumed as http and https. For example, `www.example.com` will
     *   match `http://www.example.com` and `https://www.example.com`.
     * * Placeholders in the form of `{placeholder_name}` matches 1 or more characters. The String
     *   value of the placeholder will be available in the arguments [Bundle] with a key of the same
     *   name. For example, `http://www.example.com/users/{id}` will match
     *   `http://www.example.com/users/4`.
     * * The `.*` wildcard can be used to match 0 or more characters.
     *
     * @param uriPattern The uri pattern to add as a deep link
     * @see deepLink
     */
    public fun deepLink(uriPattern: String) {
        deepLinks.add(NavDeepLink(uriPattern))
    }

    /**
     * Add a deep link to this destination.
     *
     * The arguments in [T] are expected to be identical (in name and type) to the arguments in the
     * [route] from KClass that was used to construct this [NavDestinationBuilder].
     *
     * Extracts deeplink arguments from [T] and appends it to the [basePath]. See docs on the safe
     * args version of [NavDeepLink.Builder.setUriPattern] for the final uriPattern's generation
     * logic.
     *
     * In addition to a direct Uri match, [basePath]s without a scheme are assumed as http and
     * https. For example, `www.example.com` will match `http://www.example.com` and
     * `https://www.example.com`.
     *
     * @param T The deepLink KClass to extract arguments from
     * @param basePath The base uri path to append arguments onto
     * @see NavDeepLink.Builder.setUriPattern for the final uriPattern's generation logic.
     */
    @Suppress("BuilderSetStyle")
    @JvmName("deepLinkSafeArgs")
    public inline fun <reified T : Any> deepLink(
        basePath: String,
    ) {
        deepLink(basePath, T::class) {}
    }

    /**
     * Add a deep link to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     * * Uris without a scheme are assumed as http and https. For example, `www.example.com` will
     *   match `http://www.example.com` and `https://www.example.com`.
     * * Placeholders in the form of `{placeholder_name}` matches 1 or more characters. The String
     *   value of the placeholder will be available in the arguments [Bundle] with a key of the same
     *   name. For example, `http://www.example.com/users/{id}` will match
     *   `http://www.example.com/users/4`.
     * * The `.*` wildcard can be used to match 0 or more characters.
     *
     * @param navDeepLink the NavDeepLink to be added to this destination
     */
    public fun deepLink(navDeepLink: NavDeepLinkDslBuilder.() -> Unit) {
        deepLinks.add(NavDeepLinkDslBuilder().apply(navDeepLink).build())
    }

    /**
     * Add a deep link to this destination.
     *
     * The arguments in [T] are expected to be identical (in name and type) to the arguments in the
     * [route] from KClass that was used to construct this [NavDestinationBuilder].
     *
     * Extracts deeplink arguments from [T] and appends it to the [basePath]. See docs on the safe
     * args version of [NavDeepLink.Builder.setUriPattern] for the final uriPattern's generation
     * logic.
     *
     * In addition to a direct Uri match, [basePath]s without a scheme are assumed as http and
     * https. For example, `www.example.com` will match `http://www.example.com` and
     * `https://www.example.com`.
     *
     * @param T The deepLink KClass to extract arguments from
     * @param basePath The base uri path to append arguments onto
     * @param navDeepLink the NavDeepLink to be added to this destination
     * @see NavDeepLink.Builder.setUriPattern for the final uriPattern's generation logic.
     */
    @Suppress("BuilderSetStyle")
    public inline fun <reified T : Any> deepLink(
        basePath: String,
        noinline navDeepLink: NavDeepLinkDslBuilder.() -> Unit
    ) {
        deepLink(basePath, T::class, navDeepLink)
    }

    /**
     * Public delegation for the reified deepLink overloads.
     *
     * Checks for deepLink validity:
     * 1. They used the safe args constructor since we rely on that constructor to add arguments to
     *    the destination
     * 2. DeepLink does not contain extra arguments not present in the destination KClass. We will
     *    not have its NavType. Even if we do, the destination is not aware of the argument and will
     *    just ignore it. In general we don't want safe args deeplinks to introduce new arguments.
     * 3. DeepLink does not contain different argument type for the same arg name
     *
     * For the case where the deepLink is missing required arguments in the [route], existing checks
     * will catch it.
     */
    @OptIn(InternalSerializationApi::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun <T : Any> deepLink(
        basePath: String,
        route: KClass<T>,
        navDeepLink: NavDeepLinkDslBuilder.() -> Unit
    ) {
        // make sure they used the safe args constructors which automatically adds
        // argument to the destination
        check(this::typeMap.isInitialized) {
            "Cannot add deeplink from KClass [$route]. Use the NavDestinationBuilder " +
                "constructor that takes a KClass with the same arguments."
        }
        val deepLinkArgs = route.serializer().generateNavArguments(typeMap)
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
        deepLink(navDeepLink(basePath, route, typeMap, navDeepLink))
    }

    /**
     * Add a deep link to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     * * Uris without a scheme are assumed as http and https. For example, `www.example.com` will
     *   match `http://www.example.com` and `https://www.example.com`.
     * * Placeholders in the form of `{placeholder_name}` matches 1 or more characters. The String
     *   value of the placeholder will be available in the arguments [Bundle] with a key of the same
     *   name. For example, `http://www.example.com/users/{id}` will match
     *   `http://www.example.com/users/4`.
     * * The `.*` wildcard can be used to match 0 or more characters.
     *
     * @param navDeepLink the NavDeepLink to be added to this destination
     */
    @Suppress("BuilderSetStyle")
    public fun deepLink(navDeepLink: NavDeepLink) {
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

    /**
     * Instantiate a new instance of [D] that will be passed to [build].
     *
     * By default, this calls [Navigator.createDestination] on [navigator], but can be overridden to
     * call a custom constructor, etc.
     */
    @Suppress("BuilderSetStyle")
    protected open fun instantiateDestination(): D = navigator.createDestination()

    /** Build the NavDestination by calling [Navigator.createDestination]. */
    public open fun build(): D {
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
     * All values added here should be able to be added to a [android.os.Bundle].
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
            if (defaultArguments.isEmpty()) null
            else bundleOf(*defaultArguments.toList().toTypedArray())
        )
}

/** DSL for constructing a new [NavArgument] */
@NavDestinationDsl
public class NavArgumentBuilder {
    private val builder = NavArgument.Builder()
    private var _type: NavType<*>? = null

    /**
     * The NavType for this argument.
     *
     * If you don't set a type explicitly, it will be inferred from the default value of this
     * argument.
     */
    public var type: NavType<*>
        set(value) {
            _type = value
            builder.setType(value)
        }
        get() {
            return _type ?: throw IllegalStateException("NavType has not been set on this builder.")
        }

    /** Controls if this argument allows null values. */
    public var nullable: Boolean = false
        set(value) {
            field = value
            builder.setIsNullable(value)
        }

    /**
     * An optional default value for this argument.
     *
     * Any object that you set here must be compatible with [type], if it was specified.
     */
    public var defaultValue: Any? = null
        set(value) {
            field = value
            builder.setDefaultValue(value)
        }

    /**
     * Set whether there is an unknown default value present.
     *
     * Use with caution!! In general you should let [defaultValue] to automatically set this state.
     * This state should be set to true only if all these conditions are met:
     * 1. There is default value present
     * 2. You do not have access to actual default value (thus you can't use [defaultValue])
     * 3. You know the default value will never ever be null if [nullable] is true.
     */
    internal var unknownDefaultValuePresent: Boolean = false
        set(value) {
            field = value
            builder.setUnknownDefaultValuePresent(value)
        }

    /** Builds the NavArgument by calling [NavArgument.Builder.build]. */
    public fun build(): NavArgument {
        return builder.build()
    }
}
