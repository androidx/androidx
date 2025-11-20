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

@file:JvmName("RememberNavBackStackKt")
@file:JvmMultifileClass

package androidx.navigation3.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule

/**
 * Provides a [NavBackStack] that is automatically remembered in the Compose hierarchy across
 * process death and configuration changes.
 *
 * This function uses [NavBackStackSerializer] under the hood to save and restore the back stack via
 * [rememberSerializable]. It is designed specifically for **open polymorphism** of the [NavKey]
 * type.
 *
 * ### Serialization requirements
 * - All destination keys must be `@Serializable` and implement the [NavKey] interface.
 * - You **must** provide a [SavedStateConfiguration] containing a [SerializersModule] that
 *   registers all subtypes of [NavKey]. This is required to handle open polymorphism correctly
 *   across all platforms.
 *
 * On Android, an overload of this function is available that does not require a
 * [SavedStateConfiguration]. That version uses reflection internally and does not require subtypes
 * to be registered, but it is not available on other platforms.
 *
 * @sample androidx.navigation3.runtime.samples.rememberNavBackStack_withSerializersModule
 * @param configuration The [SavedStateConfiguration] containing a [SerializersModule] configured
 *   for [NavKey] polymorphism. This configuration must be provided and cannot be the default.
 * @param elements The initial [NavKey] elements of this back stack.
 * @return A [NavBackStack] that survives process death and configuration changes.
 * @throws IllegalArgumentException If the provided [configuration] uses the default
 *   [SerializersModule], as explicit [NavKey] subtype registration is required.
 * @see NavBackStackSerializer
 * @see NavKey
 */
@Composable
public fun rememberNavBackStack(
    configuration: SavedStateConfiguration,
    vararg elements: NavKey,
): NavBackStack<NavKey> {
    require(configuration.serializersModule != SavedStateConfiguration.DEFAULT.serializersModule) {
        "You must pass a `SavedStateConfiguration.serializersModule` configured to handle " +
            "`NavKey` open polymorphism. Define it with: `polymorphic(NavKey::class) { ... }`"
    }
    return rememberSerializable(
        configuration = configuration,
        serializer = NavBackStackSerializer(PolymorphicSerializer(NavKey::class)),
    ) {
        NavBackStack(*elements)
    }
}
