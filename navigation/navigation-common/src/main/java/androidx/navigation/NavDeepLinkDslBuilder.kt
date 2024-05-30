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

package androidx.navigation

import androidx.annotation.RestrictTo
import androidx.navigation.serialization.generateRoutePattern
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

@DslMarker public annotation class NavDeepLinkDsl

/**
 * Construct a new [NavDeepLink]
 *
 * @param deepLinkBuilder the builder used to construct the deeplink
 */
public fun navDeepLink(deepLinkBuilder: NavDeepLinkDslBuilder.() -> Unit): NavDeepLink =
    NavDeepLinkDslBuilder().apply(deepLinkBuilder).build()

/**
 * Construct a new [NavDeepLink]
 *
 * Extracts deeplink arguments from [T] and appends it to the [basePath]. The base path & generated
 * arguments form the final uri pattern for the deeplink.
 *
 * See docs on the safe args version of [NavDeepLink.Builder.setUriPattern] for the final
 * uriPattern's generation logic.
 *
 * @param T The deepLink KClass to extract arguments from
 * @param basePath The base uri path to append arguments onto
 * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
 *   [NavType]. May be empty if [T] does not use custom NavTypes.
 * @param deepLinkBuilder the builder used to construct the deeplink
 */
public inline fun <reified T : Any> navDeepLink(
    basePath: String,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    noinline deepLinkBuilder: NavDeepLinkDslBuilder.() -> Unit = {}
): NavDeepLink = navDeepLink(basePath, T::class, typeMap, deepLinkBuilder)

// public delegation for reified version to call internal build()
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T : Any> navDeepLink(
    basePath: String,
    route: KClass<T>,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    deepLinkBuilder: NavDeepLinkDslBuilder.() -> Unit
): NavDeepLink = NavDeepLinkDslBuilder(basePath, route, typeMap).apply(deepLinkBuilder).build()

/** DSL for constructing a new [NavDeepLink] */
@NavDeepLinkDsl
public class NavDeepLinkDslBuilder {
    private val builder = NavDeepLink.Builder()

    private var route: KClass<*>? = null
    private var typeMap: Map<KType, NavType<*>> = emptyMap()

    constructor()

    /**
     * DSl for constructing a new [NavDeepLink] with a route
     *
     * Extracts deeplink arguments from [route] and appends it to the [basePath]. The base path &
     * generated arguments form the final uri pattern for the deeplink.
     *
     * See docs on the safe args version of [NavDeepLink.Builder.setUriPattern] for the final
     * uriPattern's generation logic.
     *
     * @param basePath The base uri path to append arguments onto
     * @param route The deepLink KClass to extract arguments from
     * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
     *   [NavType]. May be empty if [route] does not use custom NavTypes.
     */
    @OptIn(InternalSerializationApi::class)
    internal constructor(
        basePath: String,
        route: KClass<*>,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>
    ) {
        require(basePath.isNotEmpty()) {
            "The basePath for NavDeepLink from KClass cannot be empty"
        }
        this.uriPattern = route.serializer().generateRoutePattern(typeMap, basePath)
        this.route = route
        this.typeMap = typeMap
    }

    /**
     * The uri pattern of the deep link
     *
     * If used with safe args, this will override the uriPattern from KClass that was set during
     * [NavDestinationBuilder] initialization.
     */
    public var uriPattern: String? = null

    /**
     * Intent action for the deep link
     *
     * @throws IllegalArgumentException if attempting to set to empty.
     */
    public var action: String? = null
        @Suppress("DocumentExceptions")
        set(p) {
            if (p != null && p.isEmpty()) {
                throw IllegalArgumentException("The NavDeepLink cannot have an empty action.")
            }
            field = p
        }

    /** MimeType for the deep link */
    public var mimeType: String? = null

    internal fun build() =
        builder
            .apply {
                check(!(uriPattern == null && action == null && mimeType == null)) {
                    ("The NavDeepLink must have an uri, action, and/or mimeType.")
                }
                uriPattern?.let { setUriPattern(it) }
                action?.let { setAction(it) }
                mimeType?.let { setMimeType(it) }
            }
            .build()
}
