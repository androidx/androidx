/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.compose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.core.bundle.Bundle
import androidx.core.bundle.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateHandle.Companion.validateValue
import kotlin.jvm.JvmName
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Inter-opt between [SavedStateHandle] and [Saver] so that any state holder that is
 * being saved via [rememberSaveable] with a custom [Saver] can also be saved with
 * [SavedStateHandle].
 *
 * The returned state [T] should be the only way that a value is saved or restored from the
 * [SavedStateHandle] with the given [key].
 *
 * Using the same key again with another [SavedStateHandle] method is not supported, as values
 * won't cross-set or communicate updates.
 *
 * @sample androidx.lifecycle.viewmodel.compose.samples.SnapshotStateViewModel
 */
@SavedStateHandleSaveableApi
fun <T : Any> SavedStateHandle.saveable(
    key: String,
    saver: Saver<T, out Any> = autoSaver(),
    init: () -> T,
): T {
    @Suppress("UNCHECKED_CAST")
    saver as Saver<T, Any>
    // value is restored using the SavedStateHandle or created via [init] lambda
    @Suppress("DEPRECATION") // Bundle.get has been deprecated in API 31
    val value = get<Bundle?>(key)?.get("value")?.let(saver::restore) ?: init()

    // Hook up saving the state to the SavedStateHandle
    setSavedStateProvider(key) {
        bundleOf("value" to with(saver) {
            SaverScope(::validateValue).save(value)
        })
    }
    return value
}

/**
 * Inter-opt between [SavedStateHandle] and [Saver] so that any state holder that is
 * being saved via [rememberSaveable] with a custom [Saver] can also be saved with
 * [SavedStateHandle].
 *
 * The returned [MutableState] should be the only way that a value is saved or restored from the
 * [SavedStateHandle] with the given [key].
 *
 * Using the same key again with another [SavedStateHandle] method is not supported, as values
 * won't cross-set or communicate updates.
 *
 * Use this overload if you remember a mutable state with a type which can't be stored in the
 * Bundle so you have to provide a custom saver object.
 *
 * @sample androidx.lifecycle.viewmodel.compose.samples.SnapshotStateViewModel
 */
@SavedStateHandleSaveableApi
fun <T> SavedStateHandle.saveable(
    key: String,
    stateSaver: Saver<T, out Any>,
    init: () -> MutableState<T>
): MutableState<T> = saveable(
    saver = mutableStateSaver(stateSaver),
    key = key,
    init = init
)

/**
 * Inter-opt between [SavedStateHandle] and [Saver] so that any state holder that is
 * being saved via [rememberSaveable] with a custom [Saver] can also be saved with
 * [SavedStateHandle].
 *
 * The key is automatically retrieved as the name of the property this delegate is being used
 * to create.
 *
 * The returned state [T] should be the only way that a value is saved or restored from the
 * [SavedStateHandle] with the automatic key.
 *
 * Using the same key again with another [SavedStateHandle] method is not supported, as values
 * won't cross-set or communicate updates.
 *
 * @sample androidx.lifecycle.viewmodel.compose.samples.SnapshotStateViewModelWithDelegates
 */
@SavedStateHandleSaveableApi
fun <T : Any> SavedStateHandle.saveable(
    saver: Saver<T, out Any> = autoSaver(),
    init: () -> T,
): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, T>> =
    PropertyDelegateProvider { thisRef, property ->
        val classNamePrefix = if (thisRef != null) thisRef::class.qualifiedName + "." else ""
        val value = saveable(
            key = classNamePrefix + property.name,
            saver = saver,
            init = init
        )

        ReadOnlyProperty { _, _ -> value }
    }

/**
 * Inter-opt between [SavedStateHandle] and [Saver] so that any state holder that is
 * being saved via [rememberSaveable] with a custom [Saver] can also be saved with
 * [SavedStateHandle].
 *
 * The key is automatically retrieved as the name of the property this delegate is being used
 * to create.
 *
 * The delegated [MutableState] should be the only way that a value is saved or restored from the
 * [SavedStateHandle] with the automatic key.
 *
 * Using the same key again with another [SavedStateHandle] method is not supported, as values
 * won't cross-set or communicate updates.
 *
 * Use this overload to allow delegating to a mutable state just like you can with
 * `rememberSaveable`:
 * ```
 * var value by savedStateHandle.saveable { mutableStateOf("initialValue") }
 * ```
 *
 * @sample androidx.lifecycle.viewmodel.compose.samples.SnapshotStateViewModelWithDelegates
 */
@SavedStateHandleSaveableApi
@JvmName("saveableMutableState")
fun <T : Any, M : MutableState<T>> SavedStateHandle.saveable(
    stateSaver: Saver<T, out Any> = autoSaver(),
    init: () -> M,
): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> =
    PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> { thisRef, property ->
        val classNamePrefix = if (thisRef != null) thisRef::class.qualifiedName + "." else ""
        val mutableState = saveable(
            key = classNamePrefix + property.name,
            stateSaver = stateSaver,
            init = init
        )

        // Create a property that delegates to the mutableState
        object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T =
                mutableState.getValue(thisRef, property)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
                mutableState.setValue(thisRef, property, value)
        }
    }

/**
 * Copied from RememberSaveable.kt
 */
@Suppress("UNCHECKED_CAST")
private fun <T> mutableStateSaver(inner: Saver<T, out Any>) = with(inner as Saver<T, Any>) {
    Saver<MutableState<T>, MutableState<Any?>>(
        save = { state ->
            require(state is SnapshotMutableState<T>) {
                "If you use a custom MutableState implementation you have to write a custom " +
                    "Saver and pass it as a saver param to rememberSaveable()"
            }
            mutableStateOf(save(state.value), state.policy as SnapshotMutationPolicy<Any?>)
        },
        restore = @Suppress("UNCHECKED_CAST", "ExceptionMessage") {
            require(it is SnapshotMutableState<Any?>)
            mutableStateOf(
                if (it.value != null) restore(it.value!!) else null,
                it.policy as SnapshotMutationPolicy<T?>
            ) as MutableState<T>
        }
    )
}
