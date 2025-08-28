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

package androidx.navigation3.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule

/**
 * Provides a [NavBackStack] that is automatically remembered in the Compose hierarchy across
 * process death and configuration changes.
 *
 * This function uses [NavBackStackSerializer] under the hood to save and restore the back stack via
 * [rememberSerializable].
 *
 * ### Serialization requirements
 * - Each element placed in the [NavBackStack] must be `@Serializable`.
 * - For **closed polymorphism** (sealed hierarchies), the compiler knows all subtypes and generates
 *   serializers automatically. No custom [SavedStateConfiguration] is required.
 * - For **open polymorphism** (interfaces or non-sealed hierarchies):
 *     - On Android, `SavedStateConfiguration.DEFAULT` uses a reflective serializer that can handle
 *       subtypes without registration.
 *     - On other platforms, or when you supply a custom configuration, you must register all
 *       subtypes of [NavKey] in a [SerializersModule] and pass that via [configuration]. You must
 *       also provide the same configuration to the encoder/decoder when saving/restoring state.
 *
 * ### Example
 *
 * ```kotlin
 * @Serializable sealed interface Screen : NavKey
 * @Serializable data class Home(val id: String) : Screen
 * @Serializable data class Details(val itemId: Long) : Screen
 *
 * // Closed polymorphism with sealed interface works out of the box on Android:
 * val backStack = rememberNavBackStack(Home("start"))
 *
 * // Open polymorphism requires registering subtypes (non-Android):
 * val module = SerializersModule {
 *   polymorphic(Screen::class) {
 *     subclass(Home::class, Home.serializer())
 *     subclass(Details::class, Details.serializer())
 *   }
 * }
 * val config = SavedStateConfiguration(serializersModule = module)
 * val backStack = rememberNavBackStack(Home("start"), configuration = config)
 * ```
 *
 * @param elements The initial keys of this back stack.
 * @param configuration Controls how element serializers are resolved. On Android,
 *   [SavedStateConfiguration.DEFAULT] uses reflection; otherwise, the provided [SerializersModule]
 *   is used.
 * @return A [NavBackStack] that survives process death and configuration changes.
 * @see NavBackStackSerializer
 */
@Composable
public inline fun <reified T : NavKey> rememberNavBackStack(
    vararg elements: T,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
): NavBackStack {
    return rememberSerializable(
        configuration = configuration,
        serializer = NavBackStackSerializer<NavKey>(configuration = configuration),
    ) {
        elements.toList().toMutableStateList()
    }
}

/** A List of objects that extend the [NavKey] marker class. */
public typealias NavBackStack = SnapshotStateList<NavKey>
