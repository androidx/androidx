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

package androidx.navigation

import androidx.annotation.RestrictTo
import androidx.navigation.serialization.generateNavArguments
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

@DslMarker
public annotation class NavDestinationDsl

/**
 * DSL for constructing a new [NavDestination]
 */
@NavDestinationDsl
public expect open class NavDestinationBuilder<out D : NavDestination> {

    /**
     * DSL for constructing a new [NavDestination] with a unique route.
     *
     * @param navigator navigator used to create the destination
     * @param route the destination's unique route
     *
     * @return the newly constructed [NavDestination]
     */
    public constructor(navigator: Navigator<out D>, route: String?)

    /**
     * DSL for constructing a new [NavDestination] with a serializable [KClass].
     *
     * @param navigator navigator used to create the destination
     * @param route the [KClass] of the destination
     * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
     * [NavType]. Required only when destination contains custom NavTypes.
     *
     * @return the newly constructed [NavDestination]
     */
    public constructor(
        navigator: Navigator<out D>,
        @Suppress("OptionalBuilderConstructorArgument") route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    )

    /**
     * The navigator the destination that will be used in [instantiateDestination]
     * to create the destination.
     */
    protected val navigator: Navigator<out D>

    /**
     * The destination's unique route.
     */
    public val route: String?

    /**
     * The descriptive label of the destination
     */
    public var label: CharSequence?

    /**
     * Add a [NavArgument] to this destination.
     */
    public fun argument(name: String, argumentBuilder: NavArgumentBuilder.() -> Unit)

    /**
     * Add a [NavArgument] to this destination.
     */
    @Suppress("BuilderSetStyle")
    public fun argument(name: String, argument: NavArgument)

    /**
     * Add a deep link to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * *    Uris without a scheme are assumed as http and https. For example,
     *      `www.example.com` will match `http://www.example.com` and
     *      `https://www.example.com`.
     * *    Placeholders in the form of `{placeholder_name}` matches 1 or more
     *      characters. The String value of the placeholder will be available in the arguments
     *      [Bundle] with a key of the same name. For example,
     *      `http://www.example.com/users/{id}` will match
     *      `http://www.example.com/users/4`.
     * *    The `.*` wildcard can be used to match 0 or more characters.
     *
     * @param uriPattern The uri pattern to add as a deep link
     * @see deepLink
     */
    public fun deepLink(uriPattern: String)

    /**
     * Add a deep link to this destination.
     *
     * The arguments in [T] are expected to be identical (in name and type) to the arguments
     * in the [route] from KClass that was used to construct this [NavDestinationBuilder].
     *
     * Extracts deeplink arguments from [T] and appends it to the [basePath]. See docs on the
     * safe args version of [NavDeepLink.Builder.setUriPattern] for the final uriPattern's
     * generation logic.
     *
     * In addition to a direct Uri match, [basePath]s without a scheme are assumed
     * as http and https. For example, `www.example.com` will match `http://www.example.com` and
     * `https://www.example.com`.
     *
     * @param T The deepLink KClass to extract arguments from
     * @param basePath The base uri path to append arguments onto
     *
     * @see NavDeepLink.Builder.setUriPattern for the final uriPattern's
     * generation logic.
     */
    @Suppress("BuilderSetStyle")
    @JvmName("deepLinkSafeArgs")
    public inline fun <reified T : Any> deepLink(
        basePath: String,
    )

    /**
     * Add a deep link to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * *    Uris without a scheme are assumed as http and https. For example,
     *      `www.example.com` will match `http://www.example.com` and
     *      `https://www.example.com`.
     * *    Placeholders in the form of `{placeholder_name}` matches 1 or more
     *      characters. The String value of the placeholder will be available in the arguments
     *      [Bundle] with a key of the same name. For example,
     *      `http://www.example.com/users/{id}` will match
     *      `http://www.example.com/users/4`.
     * *    The `.*` wildcard can be used to match 0 or more characters.
     *
     * @param navDeepLink the NavDeepLink to be added to this destination
     */
    public fun deepLink(navDeepLink: NavDeepLinkDslBuilder.() -> Unit)

    /**
     * Add a deep link to this destination.
     *
     * The arguments in [T] are expected to be identical (in name and type) to the arguments
     * in the [route] from KClass that was used to construct this [NavDestinationBuilder].
     *
     * Extracts deeplink arguments from [T] and appends it to the [basePath]. See docs on the
     * safe args version of [NavDeepLink.Builder.setUriPattern] for the final uriPattern's
     * generation logic.
     *
     * In addition to a direct Uri match, [basePath]s without a scheme are assumed
     * as http and https. For example, `www.example.com` will match `http://www.example.com` and
     * `https://www.example.com`.
     *
     * @param T The deepLink KClass to extract arguments from
     * @param basePath The base uri path to append arguments onto
     * @param navDeepLink the NavDeepLink to be added to this destination
     *
     * @see NavDeepLink.Builder.setUriPattern for the final uriPattern's
     * generation logic.
     */
    @Suppress("BuilderSetStyle")
    public inline fun <reified T : Any> deepLink(
        basePath: String,
        noinline navDeepLink: NavDeepLinkDslBuilder.() -> Unit
    )

    /**
     * Public delegation for the reified deepLink overloads.
     *
     * Checks for deepLink validity:
     * 1. They used the safe args constructor since we rely on that constructor
     * to add arguments to the destination
     * 2. DeepLink does not contain extra arguments not present in the destination
     * KClass. We will not have its NavType. Even if we do, the destination is not aware of the
     * argument and will just ignore it. In general we don't want safe args deeplinks to
     * introduce new arguments.
     * 3. DeepLink does not contain different argument type for the same arg name
     *
     * For the case where the deepLink is missing required arguments in the [route], existing
     * checks will catch it.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun <T : Any> deepLink(
        basePath: String,
        route: KClass<T>,
        navDeepLink: NavDeepLinkDslBuilder.() -> Unit
    )

    /**
     * Add a deep link to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * *    Uris without a scheme are assumed as http and https. For example,
     *      `www.example.com` will match `http://www.example.com` and
     *      `https://www.example.com`.
     * *    Placeholders in the form of `{placeholder_name}` matches 1 or more
     *      characters. The String value of the placeholder will be available in the arguments
     *      [Bundle] with a key of the same name. For example,
     *      `http://www.example.com/users/{id}` will match
     *      `http://www.example.com/users/4`.
     * *    The `.*` wildcard can be used to match 0 or more characters.
     *
     * @param navDeepLink the NavDeepLink to be added to this destination
     */
    @Suppress("BuilderSetStyle")
    public fun deepLink(navDeepLink: NavDeepLink)

    /**
     * Instantiate a new instance of [D] that will be passed to [build].
     *
     * By default, this calls [Navigator.createDestination] on [navigator], but can
     * be overridden to call a custom constructor, etc.
     */
    @Suppress("BuilderSetStyle")
    protected open fun instantiateDestination(): D

    /**
     * Build the NavDestination by calling [Navigator.createDestination].
     */
    public open fun build(): D
}

/**
 * DSL for constructing a new [NavArgument]
 */
@NavDestinationDsl
public class NavArgumentBuilder {
    private val builder = NavArgument.Builder()
    private var _type: NavType<*>? = null

    /**
     * The NavType for this argument.
     *
     * If you don't set a type explicitly, it will be inferred
     * from the default value of this argument.
     */
    public var type: NavType<*>
        set(value) {
            _type = value
            builder.setType(value)
        }
        get() {
            return _type ?: throw IllegalStateException("NavType has not been set on this builder.")
        }

    /**
     * Controls if this argument allows null values.
     */
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
     *
     * 1. There is default value present
     * 2. You do not have access to actual default value (thus you can't use [defaultValue])
     * 3. You know the default value will never ever be null if [nullable] is true.
     */
    internal var unknownDefaultValuePresent: Boolean = false
        set(value) {
            field = value
            builder.setUnknownDefaultValuePresent(value)
        }

    /**
     * Builds the NavArgument by calling [NavArgument.Builder.build].
     */
    public fun build(): NavArgument {
        return builder.build()
    }
}
