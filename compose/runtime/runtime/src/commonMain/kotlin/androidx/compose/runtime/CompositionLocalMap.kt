/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentMap
import androidx.compose.runtime.internal.persistentCompositionLocalHashMapOf

/**
 * A read-only, immutable snapshot of the [CompositionLocals][CompositionLocal] that are set at a
 * specific position in the composition hierarchy.
 */
sealed interface CompositionLocalMap {
    /**
     * Returns the value of the provided [composition local][key] at the position in the composition
     * hierarchy represented by this [CompositionLocalMap] instance. If the provided [key]
     * is not set at this point in the hierarchy, its default value will be used.
     *
     * For [non-static CompositionLocals][compositionLocalOf], this function will return the latest
     * value of the CompositionLocal, which might change over time across the same instance of the
     * CompositionLocalMap. Reads done in this way are not tracked in the snapshot system.
     *
     * For [static CompositionLocals][staticCompositionLocalOf], this function returns the value
     * at the time of creation of the CompositionLocalMap. When a static CompositionLocal is
     * reassigned, the entire composition hierarchy is recomposed and a new CompositionLocalMap is
     * created with the updated value of the static CompositionLocal.
     */
    operator fun <T> get(key: CompositionLocal<T>): T

    companion object {
        /**
         * An empty [CompositionLocalMap] instance which contains no keys or values.
         */
        val Empty: CompositionLocalMap = persistentCompositionLocalHashMapOf()
    }
}

/**
 * A [CompositionLocal] map is is an immutable map that maps [CompositionLocal] keys to a provider
 * of their current value. It is used to represent the combined scope of all provided
 * [CompositionLocal]s.
 */
internal interface PersistentCompositionLocalMap :
    PersistentMap<CompositionLocal<Any?>, State<Any?>>,
    CompositionLocalMap {

    fun putValue(key: CompositionLocal<Any?>, value: State<Any?>): PersistentCompositionLocalMap

    // Override the builder APIs so that we can create new PersistentMaps that retain the type
    // information of PersistentCompositionLocalMap. If we use the built-in implementation, we'll
    // get back a PersistentMap<CompositionLocal<Any?>, State<Any?>> instead of a
    // PersistentCompositionLocalMap
    override fun builder(): Builder

    interface Builder : PersistentMap.Builder<CompositionLocal<Any?>, State<Any?>> {
        override fun build(): PersistentCompositionLocalMap
    }
}

internal inline fun PersistentCompositionLocalMap.mutate(
    mutator: (MutableMap<CompositionLocal<Any?>, State<Any?>>) -> Unit
): PersistentCompositionLocalMap = builder().apply(mutator).build()

@Suppress("UNCHECKED_CAST")
internal fun <T> PersistentCompositionLocalMap.contains(key: CompositionLocal<T>) =
    this.containsKey(key as CompositionLocal<Any?>)

@Suppress("UNCHECKED_CAST")
internal fun <T> PersistentCompositionLocalMap.getValueOf(key: CompositionLocal<T>) =
    this[key as CompositionLocal<Any?>]?.value as T

internal fun <T> PersistentCompositionLocalMap.read(
    key: CompositionLocal<T>
): T = if (contains(key)) {
    getValueOf(key)
} else {
    key.defaultValueHolder.value
}

@Composable
internal fun compositionLocalMapOf(
    values: Array<out ProvidedValue<*>>,
    parentScope: PersistentCompositionLocalMap
): PersistentCompositionLocalMap {
    val result: PersistentCompositionLocalMap = persistentCompositionLocalHashMapOf()
    return result.mutate {
        for (provided in values) {
            if (provided.canOverride || !parentScope.contains(provided.compositionLocal)) {
                @Suppress("UNCHECKED_CAST")
                it[provided.compositionLocal as CompositionLocal<Any?>] =
                    provided.compositionLocal.provided(provided.value)
            }
        }
    }
}