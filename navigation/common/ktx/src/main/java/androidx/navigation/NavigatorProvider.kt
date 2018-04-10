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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.navigation

import kotlin.reflect.KClass

/**
 * Retrieves a registered [Navigator] by name.
 *
 * @throws IllegalStateException if the Navigator has not been added
 */
inline operator fun <D : NavDestination, T : Navigator<D>> NavigatorProvider.get(name: String): T =
        getNavigator(name)

/**
 * Retrieves a registered [Navigator] using the name provided by the
 * [Navigator.Name annotation][Navigator.Name].
 *
 * @throws IllegalStateException if the Navigator has not been added
 */
inline operator fun <D : NavDestination, T : Navigator<D>> NavigatorProvider.get(
        clazz: KClass<T>
): T = getNavigator(clazz.java)

/**
 * Register a [Navigator] by name. If a navigator by this name is already
 * registered, this new navigator will replace it.
 *
 * @return the previously added [Navigator] for the given name, if any
 */
inline operator fun <D : NavDestination> NavigatorProvider.set(
        name: String,
        navigator: Navigator<D>
) = addNavigator(name, navigator)

/**
 * Register a navigator using the name provided by the
 * [Navigator.Name annotation][Navigator.Name].
 */
inline operator fun <D : NavDestination> NavigatorProvider.plusAssign(navigator: Navigator<D>) {
    addNavigator(navigator)
}
