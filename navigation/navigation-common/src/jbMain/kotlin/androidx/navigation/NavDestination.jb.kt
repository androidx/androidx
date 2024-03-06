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
import androidx.core.bundle.Bundle
import kotlin.jvm.JvmStatic

public actual open class NavDestination actual constructor(
    public actual val navigatorName: String
) {
    public actual var parent: NavGraph? = null
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public set

    public actual var label: CharSequence? = null
    private val deepLinks = mutableListOf<NavDeepLink>()

    private var _arguments: MutableMap<String, NavArgument> = mutableMapOf()

    public actual val arguments: Map<String, NavArgument>
        get() = _arguments.toMap()

    public actual constructor(navigator: Navigator<out NavDestination>) : this(navigator.name)

    public actual var route: String? = null
        set(route) {
            require(route == null || route.isNotBlank()) { "Cannot have an empty route" }
            field = route
            deepLinks.remove(deepLinks.firstOrNull { it.uriPattern == field })
        }

    public actual open val displayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        get() = navigatorName

    public actual fun addDeepLink(uriPattern: String) {
        addDeepLink(NavDeepLink.Builder().setUriPattern(uriPattern).build())
    }

    public actual fun addDeepLink(navDeepLink: NavDeepLink) {
        deepLinks.add(navDeepLink)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun hasRoute(route: String, arguments: Bundle?): Boolean {
        // this matches based on routePattern
        return this.route == route
    }

    public actual fun addArgument(argumentName: String, argument: NavArgument) {
        _arguments[argumentName] = argument
    }

    public actual fun removeArgument(argumentName: String) {
        _arguments.remove(argumentName)
    }

    @Suppress("NullableCollection") // Needed for nullable bundle
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun addInDefaultArgs(args: Bundle?): Bundle? {
        if (args == null && _arguments.isEmpty()) {
            return null
        }
        val defaultArgs = Bundle()
        for ((key, value) in _arguments) {
            value.putDefaultValue(key, defaultArgs)
        }
        if (args != null) {
            defaultArgs.putAll(args)
            for ((key, value) in _arguments) {
                require(value.verify(key, defaultArgs)) {
                    "Wrong argument type for '$key' in argument bundle. ${value.type.name} " +
                        "expected."
                }
            }
        }
        return defaultArgs
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(this::class.simpleName)
        if (!route.isNullOrBlank()) {
            sb.append(" route=")
            sb.append(route)
        }
        if (label != null) {
            sb.append(" label=")
            sb.append(label)
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavDestination) return false

        val equalDeepLinks = deepLinks == other.deepLinks

        val equalArguments = _arguments.size == other._arguments.size &&
            _arguments.asSequence().all {
                other._arguments.containsKey(it.key) &&
                    other._arguments[it.key] == it.value
            }

        return route == other.route &&
            equalDeepLinks &&
            equalArguments
    }

    override fun hashCode(): Int {
        var result = route.hashCode()
        deepLinks.forEach {
            result = 31 * result + it.uriPattern.hashCode()
            result = 31 * result + it.action.hashCode()
            result = 31 * result + it.mimeType.hashCode()
        }
        _arguments.keys.forEach {
            result = 31 * result + it.hashCode()
            result = 31 * result + _arguments[it].hashCode()
        }
        return result
    }

    public actual companion object {
        @JvmStatic
        public actual val NavDestination.hierarchy: Sequence<NavDestination>
            get() = generateSequence(this) { it.parent }
    }
}
