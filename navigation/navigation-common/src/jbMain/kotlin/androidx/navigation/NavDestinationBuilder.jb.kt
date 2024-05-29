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

import androidx.navigation.serialization.generateNavArguments
import androidx.navigation.serialization.generateRoutePattern
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

/**
 * DSL for constructing a new [NavDestination]
 */
@NavDestinationDsl
public actual open class NavDestinationBuilder<out D : NavDestination>

/**
 * DSL for constructing a new [NavDestination] with a unique route.
 *
 * @param navigator navigator used to create the destination
 * @param route the destination's unique route
 *
 * @return the newly constructed [NavDestination]
 */
public actual constructor(
    /**
     * The navigator the destination was created from
     */
    protected actual val navigator: Navigator<out D>,

    /**
     * The destination's unique route.
     */
    public actual val route: String?
) {
    @OptIn(InternalSerializationApi::class)
    public actual constructor(
        navigator: Navigator<out D>,
        @Suppress("OptionalBuilderConstructorArgument") route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    ) : this(
        navigator,
        route?.serializer()?.generateRoutePattern(typeMap)
    ) {
        route?.apply {
            serializer().generateNavArguments(typeMap).forEach {
                arguments[it.name] = it.argument
            }
        }
    }

    /**
     * The descriptive label of the destination
     */
    public actual var label: CharSequence? = null

    private var arguments = mutableMapOf<String, NavArgument>()

    /**
     * Add a [NavArgument] to this destination.
     */
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
    public actual fun deepLink(navDeepLink: NavDeepLink) {
        deepLinks.add(navDeepLink)
    }

    @Suppress("BuilderSetStyle")
    protected actual open fun instantiateDestination(): D = navigator.createDestination()

    /**
     * Build the NavDestination by calling [Navigator.createDestination].
     */
    public actual open fun build(): D {
        return instantiateDestination().also { destination ->
            destination.label = label
            arguments.forEach { (name, argument) ->
                destination.addArgument(name, argument)
            }
            deepLinks.forEach { deepLink ->
                destination.addDeepLink(deepLink)
            }
            if (route != null) {
                destination.route = route
            }
        }
    }
}
