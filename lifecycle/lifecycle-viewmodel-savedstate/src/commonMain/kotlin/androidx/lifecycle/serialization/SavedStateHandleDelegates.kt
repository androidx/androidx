/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lifecycle.serialization

import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Returns a property delegate that uses [SavedStateHandle] to save and restore a value of type [T]
 * with the default serializer.
 *
 * @sample androidx.lifecycle.delegateExplicitKey
 * @param key An optional [String] key to use for storing the value in the [SavedStateHandle]. A
 *   default key will be generated if it's omitted or when 'null' is passed.
 * @param init The function to provide the initial value of the property.
 * @return A property delegate that manages the saving and restoring of the value.
 */
public inline fun <reified T : Any> SavedStateHandle.saved(
    key: String? = null,
    noinline init: () -> T,
): ReadWriteProperty<Any?, T> {
    return saved(serializer(), key, init)
}

/**
 * Returns a property delegate that uses [SavedStateHandle] to save and restore a value of type [T].
 *
 * @sample androidx.lifecycle.delegateExplicitKeyAndSerializer
 * @param serializer The [KSerializer] to use for serializing and deserializing the value.
 * @param key An optional [String] key to use for storing the value in the [SavedStateHandle]. A
 *   default key will be generated if it's omitted or when 'null' is passed.
 * @param init The function to provide the initial value of the property.
 * @return A property delegate that manages the saving and restoring of the value.
 */
public fun <T : Any> SavedStateHandle.saved(
    serializer: KSerializer<T>,
    key: String? = null,
    init: () -> T,
): ReadWriteProperty<Any?, T> {
    return SavedStateHandleDelegate(
        savedStateHandle = this,
        key = key,
        serializer = serializer,
        init = init
    )
}

private class SavedStateHandleDelegate<T : Any>(
    private val savedStateHandle: SavedStateHandle,
    private val key: String?,
    private val serializer: KSerializer<T>,
    private val init: () -> T,
) : ReadWriteProperty<Any?, T> {
    private lateinit var value: T

    private fun loadValue(key: String): T? {
        return savedStateHandle.get<SavedState>(key)?.let { decodeFromSavedState(serializer, it) }
    }

    private fun registerSave(key: String) {
        savedStateHandle.setSavedStateProvider(key) { encodeToSavedState(serializer, this.value) }
    }

    private fun createDefaultKey(thisRef: Any?, property: KProperty<*>): String {
        val classNamePrefix = if (thisRef != null) thisRef::class.qualifiedName + "." else ""
        return classNamePrefix + property.name
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!::value.isInitialized) {
            val qualifiedKey = key ?: createDefaultKey(thisRef, property)
            registerSave(qualifiedKey)
            this.value = loadValue(qualifiedKey) ?: init()
        }
        return this.value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (!::value.isInitialized) {
            val qualifiedKey = key ?: createDefaultKey(thisRef, property)
            registerSave(qualifiedKey)
        }
        this.value = value
    }
}
