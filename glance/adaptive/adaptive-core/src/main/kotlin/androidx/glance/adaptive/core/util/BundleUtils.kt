/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.core.util

import android.os.Bundle
import android.util.SparseArray

/**
 * Utility functions for operating on [Bundle] instances, supporting deep equality and hashing
 * across primitive values, arrays, lists, nested bundles, and [SparseArray] instances.
 */
internal object BundleUtils {

    /**
     * Compares two [Bundle] instances for deep equality across all keys and values, including
     * nested [Bundle]s, arrays, lists, and [SparseArray]s.
     */
    // Required because bundle.get(key) is deprecated in API 33 in favor of type-safe getters,
    // but generic inspection across heterogeneous Bundle values requires untyped retrieval.
    @Suppress("DEPRECATION")
    fun areBundlesEqual(bundle1: Bundle, bundle2: Bundle): Boolean {
        if (bundle1 === bundle2) return true
        if (bundle1.isEmpty && bundle2.isEmpty) return true
        if (bundle1.size() != bundle2.size()) return false
        val keys1 = bundle1.keySet()
        val keys2 = bundle2.keySet()
        if (keys1 != keys2) return false
        for (key in keys1) {
            val val1 = bundle1.get(key)
            val val2 = bundle2.get(key)
            if (val1 is Bundle && val2 is Bundle) {
                if (!areBundlesEqual(val1, val2)) return false
            } else if (val1 is ByteArray && val2 is ByteArray) {
                if (!val1.contentEquals(val2)) return false
            } else if (val1 is ShortArray && val2 is ShortArray) {
                if (!val1.contentEquals(val2)) return false
            } else if (val1 is CharArray && val2 is CharArray) {
                if (!val1.contentEquals(val2)) return false
            } else if (val1 is IntArray && val2 is IntArray) {
                if (!val1.contentEquals(val2)) return false
            } else if (val1 is LongArray && val2 is LongArray) {
                if (!val1.contentEquals(val2)) return false
            } else if (val1 is FloatArray && val2 is FloatArray) {
                if (!val1.contentEquals(val2)) return false
            } else if (val1 is DoubleArray && val2 is DoubleArray) {
                if (!val1.contentEquals(val2)) return false
            } else if (val1 is BooleanArray && val2 is BooleanArray) {
                if (!val1.contentEquals(val2)) return false
            } else if (val1 is SparseArray<*> && val2 is SparseArray<*>) {
                if (!areSparseArraysEqual(val1, val2)) return false
            } else if (val1 is Array<*> && val2 is Array<*>) {
                if (val1.size != val2.size) return false
                for (i in val1.indices) {
                    val elem1 = val1[i]
                    val elem2 = val2[i]
                    if (elem1 is Bundle && elem2 is Bundle) {
                        if (!areBundlesEqual(elem1, elem2)) return false
                    } else if (elem1 is SparseArray<*> && elem2 is SparseArray<*>) {
                        if (!areSparseArraysEqual(elem1, elem2)) return false
                    } else if (elem1 is Array<*> && elem2 is Array<*>) {
                        if (!elem1.contentDeepEquals(elem2)) return false
                    } else if (elem1 != elem2) {
                        return false
                    }
                }
            } else if (val1 is List<*> && val2 is List<*>) {
                if (val1.size != val2.size) return false
                for (i in val1.indices) {
                    val elem1 = val1[i]
                    val elem2 = val2[i]
                    if (elem1 is Bundle && elem2 is Bundle) {
                        if (!areBundlesEqual(elem1, elem2)) return false
                    } else if (elem1 is SparseArray<*> && elem2 is SparseArray<*>) {
                        if (!areSparseArraysEqual(elem1, elem2)) return false
                    } else if (elem1 != elem2) {
                        return false
                    }
                }
            } else if (val1 != val2) {
                return false
            }
        }
        return true
    }

    /** Computes a deep hash code for a [Bundle] consistent with [areBundlesEqual]. */
    // Required because bundle.get(key) is deprecated in API 33 in favor of type-safe getters,
    // but generic inspection across heterogeneous Bundle values requires untyped retrieval.
    @Suppress("DEPRECATION")
    fun bundleHashCode(bundle: Bundle): Int {
        if (bundle.isEmpty) return 0
        var result = 0
        val sortedKeys = bundle.keySet().sorted()
        for (i in sortedKeys.indices) {
            val key = sortedKeys[i]
            val value = bundle.get(key)
            val valueHash =
                when (value) {
                    is Bundle -> bundleHashCode(value)
                    is ByteArray -> value.contentHashCode()
                    is ShortArray -> value.contentHashCode()
                    is CharArray -> value.contentHashCode()
                    is IntArray -> value.contentHashCode()
                    is LongArray -> value.contentHashCode()
                    is FloatArray -> value.contentHashCode()
                    is DoubleArray -> value.contentHashCode()
                    is BooleanArray -> value.contentHashCode()
                    is SparseArray<*> -> sparseArrayHashCode(value)
                    is Array<*> -> {
                        var arrayHash = 1
                        for (j in value.indices) {
                            val elem = value[j]
                            val elemHash =
                                when (elem) {
                                    is Bundle -> bundleHashCode(elem)
                                    is SparseArray<*> -> sparseArrayHashCode(elem)
                                    is Array<*> -> elem.contentDeepHashCode()
                                    else -> elem?.hashCode() ?: 0
                                }
                            arrayHash = 31 * arrayHash + elemHash
                        }
                        arrayHash
                    }
                    is List<*> -> {
                        var listHash = 1
                        for (j in value.indices) {
                            val elem = value[j]
                            val elemHash =
                                if (elem is Bundle) bundleHashCode(elem)
                                else if (elem is SparseArray<*>) sparseArrayHashCode(elem)
                                else elem?.hashCode() ?: 0
                            listHash = 31 * listHash + elemHash
                        }
                        listHash
                    }
                    else -> value?.hashCode() ?: 0
                }
            result = 31 * result + (key.hashCode() xor valueHash)
        }
        return result
    }

    private fun areSparseArraysEqual(a: SparseArray<*>, b: SparseArray<*>): Boolean {
        if (a === b) return true
        if (a.size() != b.size()) return false
        for (i in 0 until a.size()) {
            if (a.keyAt(i) != b.keyAt(i)) return false
            val elem1 = a.valueAt(i)
            val elem2 = b.valueAt(i)
            if (elem1 is Bundle && elem2 is Bundle) {
                if (!areBundlesEqual(elem1, elem2)) return false
            } else if (elem1 is SparseArray<*> && elem2 is SparseArray<*>) {
                if (!areSparseArraysEqual(elem1, elem2)) return false
            } else if (elem1 is Array<*> && elem2 is Array<*>) {
                if (!elem1.contentDeepEquals(elem2)) return false
            } else if (elem1 != elem2) {
                return false
            }
        }
        return true
    }

    private fun sparseArrayHashCode(sparseArray: SparseArray<*>): Int {
        var result = 1
        for (i in 0 until sparseArray.size()) {
            val key = sparseArray.keyAt(i)
            val value = sparseArray.valueAt(i)
            val valueHash =
                when (value) {
                    is Bundle -> bundleHashCode(value)
                    is SparseArray<*> -> sparseArrayHashCode(value)
                    is Array<*> -> value.contentDeepHashCode()
                    else -> value?.hashCode() ?: 0
                }
            result = 31 * result + (key.hashCode() xor valueHash)
        }
        return result
    }
}
