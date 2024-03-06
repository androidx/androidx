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

    /**
     * Build the NavDestination by calling [Navigator.createDestination].
     */
    public actual open fun build(): D {
        return navigator.createDestination().also { destination ->
            destination.label = label
            arguments.forEach { (name, argument) ->
                destination.addArgument(name, argument)
            }
            if (route != null) {
                destination.route = route
            }
        }
    }
}
