/*
 * Copyright 2020 The Android Open Source Project
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

// For Kotlin source compatibility only.
@file:JvmName("SourceCompatibility_DoNotUseFromJava")

package androidx.collection

import kotlin.DeprecationLevel.WARNING

@Deprecated("Replaced with property", ReplaceWith("this.size"), WARNING)
fun <K, V> ArrayMap<K, V>.size() = size

// Note that there is no extension property ArrayMap<K, V>.isEmpty in JVM. Since ArrayMap is still
// implemented in Java, Kotlin will allow isEmpty to be used both as a function and a property, as
// is the case for any Java boolean isX() getter.

@Deprecated("Replaced with property", ReplaceWith("this.size"), WARNING)
fun <E> ArraySet<E>.size() = size

@Deprecated("Replaced with function", ReplaceWith("this.isEmpty()"), WARNING)
val <E> ArraySet<E>.isEmpty get() = isEmpty()

@Deprecated("Replaced with property", ReplaceWith("this.size"), WARNING)
fun <E> CircularArray<E>.size() = size

@Deprecated("Replaced with function", ReplaceWith("this.isEmpty()"), WARNING)
val <E> CircularArray<E>.isEmpty get() = isEmpty()

@Deprecated("Replaced with property", ReplaceWith("this.size"), WARNING)
fun CircularIntArray.size() = size

@Deprecated("Replaced with function", ReplaceWith("this.isEmpty()"), WARNING)
val CircularIntArray.isEmpty get() = isEmpty()

@Deprecated("Replaced with property", ReplaceWith("this.size"), WARNING)
fun <E> LongSparseArray<E>.size() = size

@Deprecated("Replaced with function", ReplaceWith("this.isEmpty()"), WARNING)
val <E> LongSparseArray<E>.isEmpty get() = isEmpty()

@Deprecated("Replaced with property", ReplaceWith("this.size"), WARNING)
fun <K, V> LruCache<K, V>.size() = size

@Deprecated("Replaced with property", ReplaceWith("this.size"), WARNING)
fun <K, V> SimpleArrayMap<K, V>.size() = size

@Deprecated("Replaced with function", ReplaceWith("this.isEmpty()"), WARNING)
val <K, V> SimpleArrayMap<K, V>.isEmpty get() = isEmpty()

@Deprecated("Replaced with property", ReplaceWith("this.size"), WARNING)
fun <E> SparseArray<E>.size() = size

@Deprecated("Replaced with function", ReplaceWith("this.isEmpty()"), WARNING)
val <E> SparseArray<E>.isEmpty get() = isEmpty()

@Deprecated("Replaced with property", ReplaceWith("this.size"), WARNING)
@Suppress("DEPRECATION") // Don't warn against extension on the deprecated class
fun <E> SparseArrayCompat<E>.size() = size

@Deprecated("Replaced with function", ReplaceWith("this.isEmpty()"), WARNING)
@Suppress("DEPRECATION")
val <E> SparseArrayCompat<E>.isEmpty get() = isEmpty()
