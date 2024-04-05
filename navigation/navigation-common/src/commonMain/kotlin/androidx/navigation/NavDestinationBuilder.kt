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

@DslMarker
public annotation class NavDestinationDsl

/**
 * DSL for constructing a new [NavDestination]
 */
@NavDestinationDsl
public expect open class NavDestinationBuilder<out D : NavDestination>
/**
 * DSL for constructing a new [NavDestination] with a unique route.
 *
 * @param navigator navigator used to create the destination
 * @param route the destination's unique route
 *
 * @return the newly constructed [NavDestination]
 */
public constructor(navigator: Navigator<out D>, route: String?) {
    /**
     * The navigator the destination was created from
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
