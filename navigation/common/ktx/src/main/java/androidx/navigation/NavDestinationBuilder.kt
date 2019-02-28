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

@DslMarker
annotation class NavDestinationDsl

/**
 * DSL for constructing a new [NavDestination]
 */
@NavDestinationDsl
open class NavDestinationBuilder<out D : NavDestination>(
    protected val navigator: Navigator<out D>,
    @IdRes val id: Int
) {
    /**
     * The descriptive label of the destination
     */
    var label: CharSequence? = null

    private var arguments = mutableMapOf<String, NavArgument>()

    /**
     * Add a [NavArgument] to this destination.
     */
    fun argument(name: String, argumentBuilder: NavArgumentBuilder.() -> Unit) {
        arguments[name] = NavArgumentBuilder().apply(argumentBuilder).build()
    }

    private var deepLinks = mutableListOf<String>()

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
     */
    fun deepLink(uriPattern: String) {
        deepLinks.add(uriPattern)
    }

    private var actions = mutableMapOf<Int, NavAction>()

    /**
     * Adds a new [NavAction] to the destination
     */
    fun action(actionId: Int, actionBuilder: NavActionBuilder.() -> Unit) {
        actions[actionId] = NavActionBuilder().apply(actionBuilder).build()
    }

    /**
     * Build the NavDestination by calling [Navigator.createDestination].
     */
    open fun build(): D {
        return navigator.createDestination().also { destination ->
            destination.id = id
            destination.label = label
            arguments.forEach { (name, argument) ->
                destination.addArgument(name, argument)
            }
            deepLinks.forEach { deepLink ->
                destination.addDeepLink(deepLink)
            }
            actions.forEach { (actionId, action) ->
                destination.putAction(actionId, action)
            }
        }
    }
}

/**
 * DSL for building a [NavAction].
 */
@NavDestinationDsl
class NavActionBuilder {
    /**
     * The ID of the destination that should be navigated to when this action is used
     */
    var destinationId: Int = 0

    private var navOptions: NavOptions? = null

    /**
     * Sets the [NavOptions] for this action that should be used by default
     */
    fun navOptions(optionsBuilder: NavOptionsBuilder.() -> Unit) {
        navOptions = NavOptionsBuilder().apply(optionsBuilder).build()
    }

    internal fun build() = NavAction(destinationId, navOptions)
}

/**
 * DSL for constructing a new [NavArgument]
 */
@NavDestinationDsl
class NavArgumentBuilder {
    private val builder = NavArgument.Builder()
    private var _type: NavType<*>? = null

    /**
     * Sets the NavType for this argument.
     *
     * If you don't set a type explicitly, it will be inferred
     * from the default value of this argument.
     */
    var type: NavType<*>
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
    var nullable: Boolean = false
        set(value) {
            field = value
            builder.setIsNullable(value)
        }

    /**
     * An optional default value for this argument.
     *
     * Any object that you set here must be compatible with [type], if it was specified.
     */
    var defaultValue: Any? = null
        set(value) {
            field = value
            builder.setDefaultValue(value)
        }

    /**
     * Builds the NavArgument by calling [NavArgument.Builder.build].
     */
    fun build(): NavArgument {
        return builder.build()
    }
}