/*
 * Copyright (C) 2017 The Android Open Source Project
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

@file:Suppress("AddVarianceModifier")

package androidx.room.processor.cache

import androidx.room.processor.FieldProcessor
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Entity
import androidx.room.vo.Pojo
import androidx.room.vo.Warning
import java.util.LinkedHashSet
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

/**
 * A cache key can be used to avoid re-processing elements.
 * <p>
 * Each context has a cache variable that uses the same backing storage as the Root Context but
 * adds current adapters and warning suppression list to the key.
 */
class Cache(val parent: Cache?, val converters: LinkedHashSet<TypeMirror>,
            val suppressedWarnings: Set<Warning>) {
    val entities: Bucket<EntityKey, Entity> = Bucket(parent?.entities)
    val pojos: Bucket<PojoKey, Pojo> = Bucket(parent?.pojos)

    inner class Bucket<K, T>(source: Bucket<K, T>?) {
        private val entries: MutableMap<FullKey<K>, T> = source?.entries ?: mutableMapOf()
        fun get(key: K, calculate: () -> T): T {
            val fullKey = FullKey(converters, suppressedWarnings, key)
            return entries.getOrPut(fullKey, {
                calculate()
            })
        }
    }

    /**
     * Key for Entity cache
     */
    data class EntityKey(val element: Element)

    /**
     * Key for Pojo cache
     */
    data class PojoKey(
            val element: Element,
            val scope: FieldProcessor.BindingScope,
            val parent: EmbeddedField?)

    /**
     * Internal key representation with adapters & warnings included.
     * <p>
     * Converters are kept in a linked set since the order is important for the TypeAdapterStore.
     */
    private data class FullKey<T>(
            val converters: LinkedHashSet<TypeMirror>,
            val suppressedWarnings: Set<Warning>,
            val key: T)
}
