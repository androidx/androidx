/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE") // Aliases to public API.

package androidx.core.util

import android.annotation.SuppressLint
import android.util.Pair as AndroidPair
import kotlin.Pair as KotlinPair

/**
 * Returns the first component of the pair.
 *
 * This method allows to use destructuring declarations when working with pairs, for example:
 * ```
 * val (first, second) = myPair
 * ```
 */
@SuppressLint("UnknownNullness")
@Suppress("HasPlatformType") // Intentionally propagating platform type with unknown nullability.
inline operator fun <F, S> Pair<F, S>.component1() = first

/**
 * Returns the second component of the pair.
 *
 * This method allows to use destructuring declarations when working with pairs, for example:
 * ```
 * val (first, second) = myPair
 * ```
 */
@SuppressLint("UnknownNullness")
@Suppress("HasPlatformType") // Intentionally propagating platform type with unknown nullability.
inline operator fun <F, S> Pair<F, S>.component2() = second

/** Returns this [AndroidX `Pair`][Pair] as a [Kotlin `Pair`][KotlinPair]. */
inline fun <F, S> Pair<F, S>.toKotlinPair() = KotlinPair(first, second)

/** Returns this [Kotlin `Pair`][KotlinPair] as an [AndroidX `Pair`][Pair]. */
// Note: the return type is explicitly specified here to prevent always seeing platform types.
inline fun <F, S> KotlinPair<F, S>.toAndroidXPair(): Pair<F, S> = Pair(first, second)

/**
 * Returns the first component of the pair.
 *
 * This method allows to use destructuring declarations when working with pairs, for example:
 * ```
 * val (first, second) = myPair
 * ```
 */
@SuppressLint("UnknownNullness")
@Suppress("HasPlatformType") // Intentionally propagating platform type with unknown nullability.
inline operator fun <F, S> AndroidPair<F, S>.component1() = first

/**
 * Returns the second component of the pair.
 *
 * This method allows to use destructuring declarations when working with pairs, for example:
 * ```
 * val (first, second) = myPair
 * ```
 */
@SuppressLint("UnknownNullness")
@Suppress("HasPlatformType") // Intentionally propagating platform type with unknown nullability.
inline operator fun <F, S> AndroidPair<F, S>.component2() = second

/** Returns this [Android `Pair`][AndroidPair] as a [Kotlin `Pair`][KotlinPair]. */
inline fun <F, S> AndroidPair<F, S>.toKotlinPair() = KotlinPair(first, second)

/** Returns this [Kotlin `Pair`][KotlinPair] as an [Android `Pair`][AndroidPair]. */
// Note: the return type is explicitly specified here to prevent always seeing platform types.
inline fun <F, S> KotlinPair<F, S>.toAndroidPair(): AndroidPair<F, S> = AndroidPair(first, second)
