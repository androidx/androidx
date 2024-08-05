/*
 * Copyright (C) 2013 The Android Open Source Project
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
package androidx.core.util

import androidx.annotation.IntRange

/**
 * Helper class for creating pools of objects. An example use looks like this:
 * ```
 * class MyPooledClass {
 *
 *     fun recycle() {
 *         // Clear state if needed, then return this instance to the Pool
 *         pool.release(this)
 *     }
 *
 *     companion object {
 *         private val pool = Pools.SynchronizedPool<MyPooledClass>(10)
 *
 *         fun obtain() : MyPooledClass {
 *             // Get an instance from the Pool or
 *             // construct a new one if none are available
 *             return pool.acquire() ?: MyPooledClass()
 *         }
 *     }
 * }
 * ```
 */
public class Pools private constructor() {
    /**
     * Interface for managing a pool of objects.
     *
     * @param T The pooled type.
     */
    public interface Pool<T : Any> {
        /** @return An instance from the pool if such, null otherwise. */
        public fun acquire(): T?

        /**
         * Release an instance to the pool.
         *
         * @param instance The instance to release.
         * @return Whether the instance was put in the pool.
         * @throws IllegalStateException If the instance is already in the pool.
         */
        public fun release(instance: T): Boolean
    }

    /**
     * Simple (non-synchronized) pool of objects.
     *
     * @param maxPoolSize The maximum pool size
     * @param T The pooled type.
     */
    public open class SimplePool<T : Any>(
        /** The max pool size */
        @IntRange(from = 1) maxPoolSize: Int
    ) : Pool<T> {
        private val pool: Array<Any?>
        private var poolSize = 0

        init {
            require(maxPoolSize > 0) { "The max pool size must be > 0" }
            pool = arrayOfNulls(maxPoolSize)
        }

        override fun acquire(): T? {
            if (poolSize > 0) {
                val lastPooledIndex = poolSize - 1
                @Suppress("UNCHECKED_CAST") val instance = pool[lastPooledIndex] as T
                pool[lastPooledIndex] = null
                poolSize--
                return instance
            }
            return null
        }

        override fun release(instance: T): Boolean {
            check(!isInPool(instance)) { "Already in the pool!" }
            if (poolSize < pool.size) {
                pool[poolSize] = instance
                poolSize++
                return true
            }
            return false
        }

        private fun isInPool(instance: T): Boolean {
            for (i in 0 until poolSize) {
                if (pool[i] === instance) {
                    return true
                }
            }
            return false
        }
    }

    /**
     * Synchronized pool of objects.
     *
     * @param maxPoolSize The maximum pool size
     * @param T The pooled type.
     */
    public open class SynchronizedPool<T : Any>(maxPoolSize: Int) : SimplePool<T>(maxPoolSize) {
        private val lock = Any()

        override fun acquire(): T? {
            synchronized(lock) {
                return super.acquire()
            }
        }

        override fun release(instance: T): Boolean {
            synchronized(lock) {
                return super.release(instance)
            }
        }
    }
}
