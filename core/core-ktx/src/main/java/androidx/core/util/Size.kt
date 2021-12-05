/*
 * Copyright (C) 2018 The Android Open Source Project
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

@file:SuppressLint("ClassVerificationFailure") // Entire file is RequiresApi(21)
@file:Suppress("NOTHING_TO_INLINE")

package androidx.core.util

import android.annotation.SuppressLint
import android.util.Size
import android.util.SizeF
import androidx.annotation.RequiresApi

/**
 * Returns "width", the first component of this [Size].
 *
 * This method allows to use destructuring declarations when working with
 * sizes, for example:
 * ```
 * val (w, h) = mySize
 * ```
 */
@RequiresApi(21)
public inline operator fun Size.component1(): Int = width

/**
 * Returns "height", the second component of this [Size].
 *
 * This method allows to use destructuring declarations when working with
 * sizes, for example:
 * ```
 * val (w, h) = mySize
 * ```
 */
@RequiresApi(21)
public inline operator fun Size.component2(): Int = height

/**
 * Returns "width", the first component of this [SizeF].
 *
 * This method allows to use destructuring declarations when working with
 * sizes, for example:
 * ```
 * val (w, h) = mySize
 * ```
 */
@RequiresApi(21)
public inline operator fun SizeF.component1(): Float = width

/**
 * Returns "height", the second component of this [SizeF].
 *
 * This method allows to use destructuring declarations when working with
 * sizes, for example:
 * ```
 * val (w, h) = mySize
 * ```
 */
@RequiresApi(21)
public inline operator fun SizeF.component2(): Float = height

/**
 * Returns "width", the first component of this [SizeFCompat].
 *
 * This method allows to use destructuring declarations when working with
 * sizes, for example:
 * ```
 * val (w, h) = mySize
 * ```
 */
public inline operator fun SizeFCompat.component1(): Float = width

/**
 * Returns "height", the second component of this [SizeFCompat].
 *
 * This method allows to use destructuring declarations when working with
 * sizes, for example:
 * ```
 * val (w, h) = mySize
 * ```
 */
public inline operator fun SizeFCompat.component2(): Float = height
