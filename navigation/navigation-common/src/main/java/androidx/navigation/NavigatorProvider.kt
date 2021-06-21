/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.annotation.SuppressLint
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import kotlin.reflect.KClass

/**
 * A NavigationProvider stores a set of [Navigator]s that are valid ways to navigate
 * to a destination.
 */
@SuppressLint("TypeParameterUnusedInFormals")
public open class NavigatorProvider {
    private val _navigators: MutableMap<String, Navigator<out NavDestination>> = mutableMapOf()
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val navigators: Map<String, Navigator<out NavDestination>>
        get() = _navigators.toMap()

    /**
     * Retrieves a registered [Navigator] using the name provided by the
     * [Navigator.Name annotation][Navigator.Name].
     *
     * @param navigatorClass class of the navigator to return
     * @return the registered navigator with the given [Navigator.Name]
     *
     * @throws IllegalArgumentException if the Navigator does not have a
     * [Navigator.Name annotation][Navigator.Name]
     * @throws IllegalStateException if the Navigator has not been added
     *
     * @see NavigatorProvider.addNavigator
     */
    public fun <T : Navigator<*>> getNavigator(navigatorClass: Class<T>): T {
        val name = getNameForNavigator(navigatorClass)
        return getNavigator(name)
    }

    /**
     * Retrieves a registered [Navigator] by name.
     *
     * @param name name of the navigator to return
     * @return the registered navigator with the given name
     *
     * @throws IllegalStateException if the Navigator has not been added
     *
     * @see NavigatorProvider.addNavigator
     */
    @Suppress("UNCHECKED_CAST")
    @CallSuper
    public open fun <T : Navigator<*>> getNavigator(name: String): T {
        require(validateName(name)) { "navigator name cannot be an empty string" }
        val navigator = _navigators[name]
            ?: throw IllegalStateException(
                "Could not find Navigator with name \"$name\". You must call " +
                    "NavController.addNavigator() for each navigation type."
            )
        return navigator as T
    }

    /**
     * Register a navigator using the name provided by the
     * [Navigator.Name annotation][Navigator.Name]. [destinations][NavDestination] may
     * refer to any registered navigator by name for inflation. If a navigator by this name is
     * already registered, this new navigator will replace it.
     *
     * @param navigator navigator to add
     * @return the previously added Navigator for the name provided by the
     * [Navigator.Name annotation][Navigator.Name], if any
     */
    public fun addNavigator(
        navigator: Navigator<out NavDestination>
    ): Navigator<out NavDestination>? {
        return addNavigator(getNameForNavigator(navigator.javaClass), navigator)
    }

    /**
     * Register a navigator by name. [destinations][NavDestination] may refer to any
     * registered navigator by name for inflation. If a navigator by this name is already
     * registered, this new navigator will replace it.
     *
     * @param name name for this navigator
     * @param navigator navigator to add
     * @return the previously added Navigator for the given name, if any
     */
    @CallSuper
    public open fun addNavigator(
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
        private val annotationNames = mutableMapOf<Class<*>, String?>()
        internal fun validateName(name: String?): Boolean {
            return name != null && name.isNotEmpty()
        }

        @JvmStatic
        internal fun getNameForNavigator(navigatorClass: Class<out Navigator<*>>): String {
            var name = annotationNames[navigatorClass]
            if (name == null) {
                val annotation = navigatorClass.getAnnotation(
                    Navigator.Name::class.java
                )
                name = annotation?.value
                require(validateName(name)) {
                    "No @Navigator.Name annotation found for ${navigatorClass.simpleName}"
                }
                annotationNames[navigatorClass] = name
            }
            return name!!
        }
    }
}

/**
 * Retrieves a registered [Navigator] by name.
 *
 * @throws IllegalStateException if the Navigator has not been added
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T : Navigator<out NavDestination>> NavigatorProvider.get(
    name: String
): T = getNavigator(name)

/**
 * Retrieves a registered [Navigator] using the name provided by the
 * [Navigator.Name annotation][Navigator.Name].
 *
 * @throws IllegalStateException if the Navigator has not been added
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T : Navigator<out NavDestination>> NavigatorProvider.get(
    clazz: KClass<T>
): T = getNavigator(clazz.java)

/**
 * Register a [Navigator] by name. If a navigator by this name is already
 * registered, this new navigator will replace it.
 *
 * @return the previously added [Navigator] for the given name, if any
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavigatorProvider.set(
    name: String,
    navigator: Navigator<out NavDestination>
): Navigator<out NavDestination>? = addNavigator(name, navigator)

/**
 * Register a navigator using the name provided by the
 * [Navigator.Name annotation][Navigator.Name].
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavigatorProvider.plusAssign(navigator: Navigator<out NavDestination>) {
    addNavigator(navigator)
}
