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

package androidx.compose.material3.adaptive.layout.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.util.fastForEach
import kotlin.reflect.KProperty

@Composable
internal inline fun <Key, Value : Any> rememberPersistentlyWithKey(
    key: Key,
    keySaver: Saver<Key, Any> = autoSaver(),
    valueSaver: Saver<Value, Any> = autoSaver(),
    crossinline calculation: @DisallowComposableCalls () -> Value,
): Value {
    val map = rememberSaveable(saver = MapSaver(keySaver, valueSaver)) { mutableMapOf() }
    return map.getOrPut(key, calculation)
}

private fun <Key, Value : Any> MapSaver(
    keySaver: Saver<Key, Any>,
    valueSaver: Saver<Value, Any>,
): Saver<MutableMap<Key, Value>, *> =
    listSaver<MutableMap<Key, Value>, Any>(
        save = {
            val entrySaver = MapEntrySaver(keySaver, valueSaver)
            buildList { it.forEach { entry -> add(with(entrySaver) { save(entry) }!!) } }
        },
        restore = {
            val entrySaver = MapEntrySaver(keySaver, valueSaver)
            val map = mutableMapOf<Key, Value>()
            it.fastForEach { entry ->
                with(entrySaver) { restore(entry) }!!.apply { map[key] = value }
            }
            map
        },
    )

private fun <Key, Value : Any> MapEntrySaver(
    keySaver: Saver<Key, Any>,
    valueSaver: Saver<Value, Any>,
): Saver<Map.Entry<Key, Value>, Any> =
    listSaver(
        save = { listOf(with(keySaver) { save(it.key) }, with(valueSaver) { save(it.value) }) },
        restore = {
            val key = with(keySaver) { restore(it[0]!!) }
            val value = with(valueSaver) { restore(it[1]!!) }
            object : Map.Entry<Key, Value> {
                override val key = key!!
                override val value = value!!
            }
        },
    )

/**
 * This is a helper function to remember a mutable value that the value change will not trigger
 * recomposition. For example we may want to remember a certain variable value of the last
 * composition to do some comparison. This can be a light-weight replacement of using states in
 * certain cases.
 */
@Composable
internal fun <T> rememberRef(initialValue: T): RefHolder<T> = remember { RefHolder(initialValue) }

@Composable
internal fun <T> rememberUpdatedRef(value: T): RefHolder<T> =
    rememberRef(value).apply { this.value = value }

internal class RefHolder<T>(var value: T)

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun <T> RefHolder<T>.getValue(thisObj: Any?, property: KProperty<*>): T =
    value

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun <T> RefHolder<T>.setValue(
    thisObj: Any?,
    property: KProperty<*>,
    value: T,
) {
    this.value = value
}
