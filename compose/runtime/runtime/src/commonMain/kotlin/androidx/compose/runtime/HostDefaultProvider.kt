/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.runtime

/**
 * A [CompositionLocal] that provides the [HostDefaultProvider] to the composition.
 *
 * This is intended for internal usage by [compositionLocalWithHostDefaultOf] to resolve values from
 * the hosting environment (e.g., `android.view.View`).
 */
@InternalComposeApi
public val LocalHostDefaultProvider: ProvidableCompositionLocal<HostDefaultProvider> =
    compositionLocalOf {
        error("CompositionLocal LocalHostDefaultProvider not present")
    }

/**
 * An interface that allows the hosting environment (the platform embedding the Compose content) to
 * provide default values for [CompositionLocal]s based on a key.
 *
 * This acts as a bridge or service locator, allowing platform-agnostic libraries to retrieve
 * platform-specific components (like `LifecycleOwner` or `ViewModelStoreOwner`) without depending
 * directly on platform artifacts (e.g., `android.view.View`).
 */
@InternalComposeApi
public interface HostDefaultProvider {

    /**
     * Retrieves a default value identified by [key] from the host environment.
     *
     * @param key The identifier for the value to retrieve. The type and meaning of the key are
     *   defined by the host implementation (e.g., an Android resource ID).
     * @return The requested value, or null if not found.
     */
    public fun <T> getHostDefault(key: HostDefaultKey<T>): T
}

/**
 * A type-safe identifier used to define a key for retrieving default values from the hosting
 * environment.
 *
 * This key is strictly required by [compositionLocalWithHostDefaultOf] to establish a mapping
 * between a [CompositionLocal] and a value provided by the host (via the internal
 * [HostDefaultProvider]).
 *
 * The internal representation of this key is platform-specific. For example, on Android, this acts
 * as a wrapper around a Resource ID to query the View hierarchy, while ensuring the retrieved value
 * matches the type [T].
 *
 * @param T The type of the value associated with this key.
 * @see compositionLocalWithHostDefaultOf
 * @see HostDefaultProvider
 */
public expect class HostDefaultKey<T> {
    public constructor()
}
