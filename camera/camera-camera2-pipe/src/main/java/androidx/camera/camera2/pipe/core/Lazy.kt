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

package androidx.camera.camera2.pipe.core

/**
 * Utility function for creating inline [Lazy] instances that default to false if the block throws
 * an exception while trying to read or compute the value.
 */
internal inline fun lazyOrFalse(
    crossinline blockNameFn: () -> String,
    crossinline block: () -> Boolean
): Lazy<Boolean> =
    lazy(LazyThreadSafetyMode.PUBLICATION) {
        val blockName = blockNameFn()
        try {
            Debug.trace(blockName) { block() }
        } catch (e: Throwable) {
            Log.warn(e) { "Failed to get $blockName! Caching false and ignoring exception." }
            false
        }
    }

internal inline fun lazyOrFalse(
    blockName: String,
    crossinline block: () -> Boolean
): Lazy<Boolean> = lazyOrFalse({ blockName }, block)

/**
 * Utility function for creating [Lazy] instances that default to an empty set if the block throws
 * an exception while trying to read or compute the value.
 */
internal inline fun <T> lazyOrEmptySet(
    crossinline blockNameFn: () -> String,
    crossinline block: () -> Set<T>?
): Lazy<Set<T>> =
    lazy(LazyThreadSafetyMode.PUBLICATION) {
        val blockName = blockNameFn()
        try {
            Debug.trace(blockName) { block() ?: emptySet() }
        } catch (e: Throwable) {
            Log.warn(e) { "Failed to get $blockName! Caching {} and ignoring exception." }
            emptySet()
        }
    }

internal inline fun <T> lazyOrEmptySet(
    blockName: String,
    crossinline block: () -> Set<T>?
): Lazy<Set<T>> = lazyOrEmptySet({ blockName }, block)

/**
 * Utility function for creating [Lazy] instances that default to an empty list if the block throws
 * an exception while trying to read or compute the value.
 */
internal inline fun <T> lazyOrEmptyList(
    crossinline blockNameFn: () -> String,
    crossinline block: () -> List<T>?
): Lazy<List<T>> =
    lazy(LazyThreadSafetyMode.PUBLICATION) {
        val blockName = blockNameFn()
        try {
            Debug.trace(blockName) { block() ?: emptyList() }
        } catch (e: Throwable) {
            Log.warn(e) { "Failed to get $blockName! Caching [] and ignoring exception." }
            emptyList()
        }
    }

internal inline fun <T> lazyOrEmptyList(
    blockName: String,
    crossinline block: () -> List<T>
): Lazy<List<T>> = lazyOrEmptyList({ blockName }, block)
