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
public val LocalHostDefaultProvider: ProvidableCompositionLocal<HostDefaultProvider> =
    compositionLocalOf {
        error("CompositionLocal LocalHostDefaultProvider not present")
    }

/**
 * An interface that allows the hosting environment (e.g., Android, Desktop, or iOS) to provide
 * default values for [CompositionLocal] using [compositionLocalWithHostDefaultOf].
 *
 * This acts as a decoupling layer, allowing platform-agnostic libraries to request
 * platform-specific components ((like `LifecycleOwner` or `ViewModelStoreOwner`) without depending
 * on platform-specific APIs or artifacts.
 */
public interface HostDefaultProvider {
    /** Retrieves a value associated with [key] from the host environment. */
    public fun <T> getHostDefault(key: HostDefaultKey<T>): T
}
