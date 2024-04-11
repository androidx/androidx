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
import kotlin.reflect.KClass
import kotlin.reflect.KType

@DslMarker
public annotation class NavDeepLinkDsl

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
 * @param T The route from KClass to extract deeplink arguments from
 * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
 * [NavType]. May be empty if [T] does not use custom NavTypes.
 * @param deepLinkBuilder the builder used to construct the deeplink
 */
@ExperimentalSafeArgsApi
public inline fun <reified T : Any> navDeepLink(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    noinline deepLinkBuilder: NavDeepLinkDslBuilder.() -> Unit
): NavDeepLink = navDeepLink(T::class, typeMap, deepLinkBuilder)

// public delegation for reified version to call internal build()
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T : Any> navDeepLink(
    route: KClass<T>,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    deepLinkBuilder: NavDeepLinkDslBuilder.() -> Unit
): NavDeepLink = NavDeepLinkDslBuilder(route, typeMap).apply(deepLinkBuilder).build()

/**
 * DSL for constructing a new [NavDeepLink]
 */
@NavDeepLinkDsl
public class NavDeepLinkDslBuilder {
    private val builder = NavDeepLink.Builder()

    private var route: KClass<*>? = null
    private var typeMap: Map<KType, NavType<*>> = emptyMap()

    constructor()

    /**
     * DSl for constructing a new [NavDeepLink] with a route
     *
     * Extracts deeplink arguments from [route] and appends it to the base uri path. The base
     * uri path should be set with [uriPattern].
     *
     * @param route The route from KClass to extract deeplink arguments from
     * @param typeMap map of destination arguments' kotlin type [KType] to its respective custom
     * [NavType]. May be empty if [route] does not use custom NavTypes.
     */
    internal constructor(
        route: KClass<*>,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>
    ) {
        this.route = route
        this.typeMap = typeMap
    }

    /**
     * The uri pattern of the deep link
     *
     * If used with safe args, this forms the base uri to which arguments are appended. See docs on
     * the safe args version of [NavDeepLink.Builder.setUriPattern] for the final uriPattern's
     * generation logic.
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

    /**
     * MimeType for the deep link
     */
    public var mimeType: String? = null

    internal fun build() = builder.apply {
        check(!(uriPattern == null && action == null && mimeType == null)) {
            ("The NavDeepLink must have an uri, action, and/or mimeType.")
        }
        if (route != null) {
            checkNotNull(uriPattern) {
                "Failed to build NavDeepLink from KClass. Ensure base path is set " +
                    "through uriPattern."
            }
            setUriPattern(uriPattern!!, route!!, typeMap)
        } else {
            uriPattern?.let { setUriPattern(it) }
        }
        action?.let { setAction(it) }
        mimeType?.let { setMimeType(it) }
    }.build()
}
