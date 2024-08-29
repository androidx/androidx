/*
 * Copyright (C) 2024 The Android Open Source Project
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

@file:JvmName("BrushUtil")

package androidx.ink.brush

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi

/**
 * The brush color as an [android.graphics.Color] instance, which can express colors in several
 * different color spaces. sRGB and Display P3 are supported; a color in any other color space will
 * be converted to Display P3.
 *
 * Unless an instance of [android.graphics.Color] is actually needed, prefer to use
 * [Brush.colorLong] to get the color without causing an allocation, especially in
 * performance-sensitive code. [Brush.colorLong] is fully compatible with the [Long] representation
 * of [android.graphics.Color].
 */
@CheckResult
@RequiresApi(Build.VERSION_CODES.O)
public fun Brush.createAndroidColor(): AndroidColor = AndroidColor.valueOf(colorLong)

/**
 * Creates a copy of `this` [Brush] and allows named properties to be altered while keeping the rest
 * unchanged. The color is specified as an [android.graphics.Color] instance, which can encode
 * several different color spaces. sRGB and Display P3 are supported; a color in any other color
 * space will be converted to Display P3.
 */
@CheckResult
@RequiresApi(Build.VERSION_CODES.O)
public fun Brush.copyWithAndroidColor(
    color: AndroidColor,
    family: BrushFamily = this.family,
    size: Float = this.size,
    epsilon: Float = this.epsilon,
): Brush = copyWithColorLong(color.pack(), family, size, epsilon)

/**
 * Set the color on a [Brush.Builder] as an [android.graphics.Color] instance. sRGB and Display P3
 * are supported; a color in any other color space will be converted to Display P3.
 *
 * Java callers should prefer [toBuilderWithAndroidColor] or [createBrushBuilderWithAndroidColor] as
 * a more fluent API.
 */
@CheckResult
@RequiresApi(Build.VERSION_CODES.O)
public fun Brush.Builder.setAndroidColor(color: AndroidColor): Brush.Builder =
    setColorLong(color.pack())

/**
 * Returns a [Brush.Builder] with values set equivalent to the [Brush] and the color specified by an
 * [android.graphics.Color] instance, which can encode several different color spaces. sRGB and
 * Display P3 are supported; a color in any other color space will be converted to Display P3. Java
 * developers, use the returned builder to build a copy of a Brush. Kotlin developers, see
 * [copyWithAndroidColor] method.
 *
 * In Kotlin, calling this is equivalent to calling [Brush.toBuilder] followed by
 * [Brush.Builder.setAndroidColor]. For Java callers, this function allows more fluent call
 * chaining.
 */
@CheckResult
@RequiresApi(Build.VERSION_CODES.O)
public fun Brush.toBuilderWithAndroidColor(color: AndroidColor): Brush.Builder =
    toBuilder().setAndroidColor(color)

/**
 * Returns a new, blank [Brush.Builder] with the color specified by an [android.graphics.Color]
 * instance, which can encode several different color spaces. sRGB and Display P3 are supported; a
 * color in any other color space will be converted to Display P3.
 *
 * In Kotlin, calling this is equivalent to calling [Brush.builder] followed by
 * [Brush.Builder.setAndroidColor]. For Java callers, this function allows more fluent call
 * chaining.
 */
@JvmName("createBuilderWithAndroidColor")
@CheckResult
@RequiresApi(Build.VERSION_CODES.O)
public fun createBrushBuilderWithAndroidColor(color: AndroidColor): Brush.Builder =
    Brush.Builder().setAndroidColor(color)

/**
 * Returns a new [Brush] with the color specified by an [android.graphics.Color] instance, which can
 * encode several different color spaces. sRGB and Display P3 are supported; a color in any other
 * color space will be converted to Display P3.
 *
 * Java callers should prefer `BrushUtil.createWithAndroidColor` ([createBrushWithAndroidColor]).
 */
@CheckResult
@RequiresApi(Build.VERSION_CODES.O)
public fun Brush.Companion.createWithAndroidColor(
    family: BrushFamily,
    color: AndroidColor,
    size: Float,
    epsilon: Float,
): Brush = createWithColorLong(family, color.pack(), size, epsilon)

/**
 * Returns a new [Brush] with the color specified by an [android.graphics.Color] instance, which can
 * encode several different color spaces. sRGB and Display P3 are supported; a color in any other
 * color space will be converted to Display P3.
 *
 * Kotlin callers should prefer [Brush.Companion.createWithAndroidColor].
 */
@JvmName("createWithAndroidColor")
@CheckResult
@RequiresApi(Build.VERSION_CODES.O)
public fun createBrushWithAndroidColor(
    family: BrushFamily,
    color: AndroidColor,
    size: Float,
    epsilon: Float,
): Brush = Brush.createWithAndroidColor(family, color, size, epsilon)
