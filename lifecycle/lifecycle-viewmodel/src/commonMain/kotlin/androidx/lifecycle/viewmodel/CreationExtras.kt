/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.lifecycle.viewmodel

/**
 * Simple map-like object that passed in [ViewModelProvider.Factory.create]
 * to provide an additional information to a factory.
 *
 * It allows making `Factory` implementations stateless, which makes an injection of factories
 * easier because  don't require all information be available at construction time.
 */
public abstract class CreationExtras internal constructor() {
    internal val map: MutableMap<Key<*>, Any?> = mutableMapOf()

    /**
     * Key for the elements of [CreationExtras]. [T] is a type of an element with this key.
     */
    public interface Key<T>

    /**
     * Returns an element associated with the given [key]
     */
    public abstract operator fun <T> get(key: Key<T>): T?

    /**
     * Empty [CreationExtras]
     */
    public object Empty : CreationExtras() {
        override fun <T> get(key: Key<T>): T? = null
    }
}

/**
 * Mutable implementation of [CreationExtras]
 *
 * @param initialExtras extras that will be filled into the resulting MutableCreationExtras
 */
public class MutableCreationExtras(initialExtras: CreationExtras = Empty) : CreationExtras() {

    init {
        map.putAll(initialExtras.map)
    }
    /**
     * Associates the given [key] with [t]
     */
    public operator fun <T> set(key: Key<T>, t: T) {
        map[key] = t
    }

    public override fun <T> get(key: Key<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return map[key] as T?
    }
}
