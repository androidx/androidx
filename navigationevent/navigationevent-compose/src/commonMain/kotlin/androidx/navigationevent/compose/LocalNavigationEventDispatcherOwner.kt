/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigationevent.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.HostDefaultKey
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalWithHostDefaultOf
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner

/** The CompositionLocal containing the current [NavigationEventDispatcher]. */
public object LocalNavigationEventDispatcherOwner {
    private val LocalNavigationEventDispatcherOwner =
        compositionLocalWithHostDefaultOf(NavigationEventDispatcherOwnerHostDefaultKey)

    /**
     * Returns current composition local value for the owner or `null` if one has not been provided
     * nor is one available via [findViewTreeNavigationEventDispatcherOwner] on the current
     * `androidx.compose.ui.platform.LocalView`.
     */
    public val current: NavigationEventDispatcherOwner?
        @Composable get() = LocalNavigationEventDispatcherOwner.current

    /**
     * Associates a [LocalNavigationEventDispatcherOwner] key to a value in a call to
     * [CompositionLocalProvider].
     */
    public infix fun provides(
        navigationEventDispatcherOwner: NavigationEventDispatcherOwner
    ): ProvidedValue<NavigationEventDispatcherOwner?> {
        return LocalNavigationEventDispatcherOwner.provides(navigationEventDispatcherOwner)
    }
}

/**
 * A [HostDefaultKey] used to retrieve the [NavigationEventDispatcherOwner] provided by the current
 * hosting environment.
 *
 * This key allows the composition to access the host's [NavigationEventDispatcherOwner] through a
 * decoupled mechanism, typically used by [compositionLocalWithHostDefaultOf].
 *
 * On platforms where a [NavigationEventDispatcherOwner] is not present or supported, this may
 * resolve to `null`.
 *
 * @see HostDefaultKey
 * @see compositionLocalWithHostDefaultOf
 */
public expect val NavigationEventDispatcherOwnerHostDefaultKey:
    HostDefaultKey<NavigationEventDispatcherOwner?>
