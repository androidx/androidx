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

import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

/**
 * Creates a [KSerializer] for a [SnapshotStateList] of [T] representing a back stack.
 *
 * This factory configures element serialization for open or closed polymorphic stacks, returning a
 * serializer suitable for saving/restoring a back stack with kotlinx.serialization.
 *
 * Use this serializer when wiring a back stack ([SnapshotStateList] of [T]) into saved state (e.g.,
 * with [rememberSerializable] or any API that accepts a [KSerializer]).
 *
 * **Important:** If [T] is polymorphic (e.g., a interface hierarchy), the encoder/decoder must be
 * created with the same [SerializersModule] you used to obtain the serializer. The module lives on
 * the [Encoder]/[Decoder], not inside the [KSerializer] instance. That means:
 * - When you pass a custom [SavedStateConfiguration], you must also pass that [configuration] (or
 *   at least its [SerializersModule]) to your [Encoder]/[Decoder] calls. Otherwise, polymorphic
 *   dispatch will fail at runtime with "Serializer for subclass ... is not found".
 * - The Android DEFAULT path uses a reflective serializer that is self-contained and does not
 *   require a module on the [Encoder]/[Decoder].
 *
 * ### Platform behavior:
 * - **Android**:
 *     - With [SavedStateConfiguration.DEFAULT], uses a reflective element serializer that works for
 *       any @Serializable subtype of [T].
 *     - With a custom [SavedStateConfiguration], resolves the element serializer from the provided
 *       [serializersModule] (supporting open or closed polymorphism as configured).
 *     - **Limitation:** reflective lookup does not preserve generic type parameters. For example,
 *       `List<Foo>` and `List<Bar>` are both treated as `List<*>`. If you need generic type
 *       support, supply an explicit [SavedStateConfiguration] with a registered
 *       [SerializersModule].
 * - **Non-Android**:
 *     - Always resolves the element serializer from the provided
 *       [SavedStateConfiguration.serializersModule].
 *     - You must register the hierarchy for [T] (e.g., polymorphic base + subclasses) in that
 *       module and also pass the same module to the [Encoder]/[Decoder].
 *
 * ### Closed vs. open polymorphism:
 * - Closed hierarchies (sealed classes) have all subtypes known at compile time, so serializers can
 *   be generated without a module.
 * - Open hierarchies (interfaces, non-sealed classes) require a [SerializersModule] to register
 *   subtypes for polymorphic dispatch.
 *
 * ### Example
 *
 * ```kotlin
 * interface Screen
 * @Serializable data class Home(val id: String) : Screen
 * @Serializable data class Details(val itemId: Long) : Screen
 *
 * val module = SerializersModule {
 *   polymorphic(Screen::class) {
 *     subclass(Home::class, Home.serializer())
 *     subclass(Details::class, Details.serializer())
 *   }
 * }
 * val configuration = SavedStateConfiguration(serializersModule = module)
 *
 * val serializer = NavBackStackSerializer<Screen>(configuration)
 *
 * // Pass the same configuration (or at least its serializersModule) to encode/decode:
 * val encoded = encodeToSavedState(configuration, serializer, backStack)
 * val decoded = decodeFromSavedState(configuration, serializer, encoded)
 * ```
 *
 * @param T element type stored in the back stack.
 * @param configuration controls how element serializers are resolved. On Android, DEFAULT enables
 *   reflection; otherwise the provided serializersModule is used.
 * @return a [KSerializer] for a [SnapshotStateList] of [T] for a back stack.
 */
@Suppress("FunctionName") // Factory function.
public expect inline fun <reified T : Any> NavBackStackSerializer(
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT
): KSerializer<SnapshotStateList<T>>
