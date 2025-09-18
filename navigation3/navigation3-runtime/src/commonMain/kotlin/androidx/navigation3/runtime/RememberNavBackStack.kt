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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.toMutableStateList
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

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
 * @sample androidx.navigation3.runtime.samples.rememberNavBackStack_withSerializersModule
 * @param configuration Controls how element serializers are resolved. On Android,
 *   [SavedStateConfiguration.DEFAULT] uses reflection; otherwise, the provided [SerializersModule]
 *   is used.
 * @param elements The initial keys of this back stack.
 * @return A [NavBackStack] that survives process death and configuration changes.
 * @see NavBackStackSerializer
 */
@Composable
public inline fun <reified T : NavKey> rememberNavBackStack(
    vararg elements: T,
    configuration: SavedStateConfiguration,
): NavBackStack<NavKey> {
    val base =
        rememberSerializable(
            configuration = configuration,
            serializer = NavBackStackSerializer<NavKey>(configuration = configuration),
        ) {
            elements.toList().toMutableStateList()
        }
    return remember { NavBackStack(base) }
}

/**
 * Provides a [NavBackStack] that is automatically remembered in the Compose hierarchy across
 * process death and configuration changes.
 *
 * This overload **does not take a [SavedStateConfiguration]**. It relies on the platform default:
 * on **Android**, state is saved/restored using a **reflection-based serializer**; on **other
 * platforms this will fail at runtime**. If you target non-Android platforms, use the overload that
 * accepts a [SavedStateConfiguration] and register your serializers explicitly.
 *
 * ### When to use this overload
 * - You are on **Android only** and want a simple API that uses reflection under the hood.
 * - Your back stack elements use **closed polymorphism** (sealed hierarchies) or otherwise work
 *   with Android’s reflective serializer.
 *
 * ### Serialization requirements
 * - Each element placed in the [NavBackStack] must be `@Serializable`.
 * - For **closed polymorphism** (sealed hierarchies), the compiler knows all subtypes and generates
 *   serializers; Android’s reflection will also work.
 * - For **open polymorphism** (interfaces or non-sealed hierarchies):
 *     - On Android, the reflection path can handle subtypes without manual registration.
 *     - On non-Android, this overload is **unsupported**; use the configuration overload and
 *       register all subtypes of [NavKey] in a [SerializersModule].
 *
 * @sample androidx.navigation3.runtime.samples.rememberNavBackStack_withReflection
 * @param elements The initial keys of this back stack.
 * @return A [NavBackStack] that survives process death and configuration changes on Android.
 * @see NavBackStackSerializer
 */
@Composable
public fun <T : NavKey> rememberNavBackStack(vararg elements: T): NavBackStack<NavKey> {
    return rememberSerializable(serializer = serializer()) { NavBackStack(*elements) }
}
