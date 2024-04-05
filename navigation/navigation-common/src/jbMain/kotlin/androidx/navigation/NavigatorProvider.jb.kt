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

import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

public actual open class NavigatorProvider {
    private val _navigators: MutableMap<String, Navigator<out NavDestination>> = mutableMapOf()
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual val navigators: Map<String, Navigator<out NavDestination>>
        get() = _navigators.toMap()

    @Suppress("UNCHECKED_CAST")
    @CallSuper
    public actual open fun <T : Navigator<*>> getNavigator(name: String): T {
        require(validateName(name)) { "navigator name cannot be an empty string" }
        val navigator = _navigators[name]
            ?: throw IllegalStateException(
                "Could not find Navigator with name \"$name\". You must call " +
                    "NavController.addNavigator() for each navigation type."
            )
        return navigator as T
    }

    public actual fun addNavigator(
        navigator: Navigator<out NavDestination>
    ): Navigator<out NavDestination>? {
        return addNavigator(navigator.name, navigator)
    }

    @CallSuper
    public actual open fun addNavigator(
        name: String,
        navigator: Navigator<out NavDestination>
    ): Navigator<out NavDestination>? {
        require(validateName(name)) { "navigator name cannot be an empty string" }
        val previousNavigator = _navigators[name]
        if (previousNavigator == navigator) {
            return navigator
        }
        check(previousNavigator?.isAttached != true) {
            "Navigator $navigator is replacing an already attached $previousNavigator"
        }
        check(!navigator.isAttached) {
            "Navigator $navigator is already attached to another NavController"
        }
        return _navigators.put(name, navigator)
    }

    internal companion object {
        internal fun validateName(name: String?): Boolean {
            return !name.isNullOrEmpty()
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun <T : Navigator<out NavDestination>> NavigatorProvider.get(
    name: String
): T = getNavigator(name)

@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun NavigatorProvider.set(
    name: String,
    navigator: Navigator<out NavDestination>
): Navigator<out NavDestination>? = addNavigator(name, navigator)

@Suppress("NOTHING_TO_INLINE")
public actual inline operator fun NavigatorProvider.plusAssign(navigator: Navigator<out NavDestination>) {
    addNavigator(navigator)
}
