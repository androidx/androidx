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

package androidx.compose.runtime.saveable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.serialization.serializableSaver
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.SavedStateConfiguration.Companion.DEFAULT
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Remember the value produced by [init], and persist it across activity or process recreation using
 * a [KSerializer] for saving and restoring via the saved instance state mechanism.
 *
 * This function automatically finds a [KSerializer] for the `reified` type `T`, making it a
 * convenient way to use [rememberSaveable] with types that are [Serializable].
 *
 * This behaves similarly to [remember], but will survive configuration changes (such as screen
 * rotations) and process death by saving the value into the instance state.
 *
 * @param inputs A set of inputs which, when changed, will cause the stored state to reset and
 *   [init] to be re-executed. Note that previously saved values are not validated against these
 *   inputs during restoration.
 * @param configuration Optional [SavedStateConfiguration] to customize how the serialized data is
 *   stored and restored. Defaults to [SavedStateConfiguration.DEFAULT].
 * @param init A factory function used to provide the initial value when no previously saved value
 *   exists.
 * @return The remembered and possibly restored value.
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableWithSerializer
 * @see rememberSerializable
 */
@Composable
public inline fun <reified T : Any> rememberSerializable(
    vararg inputs: Any?,
    configuration: SavedStateConfiguration = DEFAULT,
    noinline init: () -> T,
): T {
    return rememberSerializable(
        inputs = inputs,
        serializer = configuration.serializersModule.serializer<T>(),
        configuration = configuration,
        init = init,
    )
}

/**
 * Remember the value produced by [init], and persist it across activity or process recreation using
 * a [KSerializer] for saving and restoring via the saved instance state mechanism.
 *
 * This behaves similarly to [remember], but will survive configuration changes (such as screen
 * rotations) and process death by saving the value into the instance state using a serializer from
 * Kotlinx Serialization.
 *
 * The value will be serialized using the provided [serializer], and the way it's saved can be
 * customized using [configuration].
 *
 * If the type cannot be automatically handled by a default [Saver], this overload provides a
 * simpler and type-safe way to persist complex or custom types using Kotlinx Serialization.
 *
 * @param inputs A set of inputs which, when changed, will cause the stored state to reset and
 *   [init] to be re-executed. Note that previously saved values are not validated against these
 *   inputs during restoration.
 * @param serializer A [KSerializer] used to serialize and deserialize the value.
 * @param configuration Optional [SavedStateConfiguration] to customize how the serialized data is
 *   stored and restored. Defaults to [SavedStateConfiguration.DEFAULT].
 * @param init A factory function used to provide the initial value when no previously saved value
 *   exists.
 * @return The remembered and possibly restored value.
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableWithSerializer
 */
@Composable
public fun <T : Any> rememberSerializable(
    vararg inputs: Any?,
    serializer: KSerializer<T>,
    configuration: SavedStateConfiguration = DEFAULT,
    init: () -> T,
): T {
    val saver = serializableSaver(serializer, configuration)
    @Suppress("DEPRECATION")
    return rememberSaveable(*inputs, saver = saver, key = null, init = init)
}

/**
 * Remember a [MutableState] produced by [init], and persist it across activity or process
 * recreation using a [KSerializer] from `kotlinx.serialization`.
 *
 * This function automatically finds a [KSerializer] for the `reified` type `T`, making it a
 * convenient way to use [rememberSaveable] with types that are [Serializable].
 *
 * This behaves similarly to [remember], but the state will survive configuration changes (like
 * screen rotations) and process recreation. It is designed for state types that cannot be stored in
 * a `Bundle` directly but can be serialized.
 *
 * @param inputs A set of inputs which, when changed, will cause the stored state to reset and
 *   [init] to be re-executed. Note that previously saved values are not validated against these
 *   inputs during restoration.
 * @param configuration Optional [SavedStateConfiguration] to customize how the serialization is
 *   handled, such as specifying a custom format (e.g. JSON). Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @param init A factory function to produce the initial `MutableState` to be remembered.
 * @return The remembered and possibly restored `MutableState`.
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableWithSerializerAndMutableState
 * @see rememberSerializable
 */
@Composable
public inline fun <reified T : Any> rememberSerializable(
    vararg inputs: Any?,
    configuration: SavedStateConfiguration = DEFAULT,
    noinline init: () -> MutableState<T>,
): MutableState<T> {
    return rememberSerializable(
        inputs = inputs,
        stateSerializer = configuration.serializersModule.serializer<T>(),
        configuration = configuration,
        init = init,
    )
}

/**
 * Remember the value produced by [init], and save it across activity or process recreation using a
 * [KSerializer] from kotlinx.serialization.
 *
 * This behaves similarly to [remember], but the value will survive configuration changes (like
 * screen rotations) and process recreation by saving it in the instance state using a
 * serialization-based mechanism.
 *
 * This overload is intended for cases where the state type cannot be stored directly in a Bundle,
 * but can be serialized with [kotlinx.serialization]. This is particularly useful for custom or
 * complex data types that are `@Serializable`.
 *
 * @param inputs A set of inputs such that, when any of them have changed, the state will reset and
 *   [init] will be rerun. Note: state restoration does NOT validate against inputs used before the
 *   value was saved.
 * @param stateSerializer A [KSerializer] used to serialize and deserialize the state value. The
 *   value must be a non-nullable type marked with `@Serializable`.
 * @param configuration Optional [SavedStateConfiguration] to customize how the serialization is
 *   handled, such as specifying a custom format (e.g. JSON).
 * @param init A factory function to produce the initial value to be remembered and saved.
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableWithSerializerAndMutableState
 */
@Composable
public fun <T : Any> rememberSerializable(
    vararg inputs: Any?,
    stateSerializer: KSerializer<T>,
    configuration: SavedStateConfiguration = DEFAULT,
    init: () -> MutableState<T>,
): MutableState<T> {
    val saver = mutableStateSaver(inner = serializableSaver(stateSerializer, configuration))
    @Suppress("DEPRECATION")
    return rememberSaveable(*inputs, saver = saver, key = null, init = init)
}
