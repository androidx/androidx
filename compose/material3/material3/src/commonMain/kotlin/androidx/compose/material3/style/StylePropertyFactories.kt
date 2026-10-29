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

package androidx.compose.material3.style

import androidx.compose.foundation.style.Breadth
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Fill
import androidx.compose.foundation.style.ProvidableStyleProperty
import androidx.compose.foundation.style.StylePropertyAccessorScope
import androidx.compose.foundation.style.StylePropertyProviderScope
import androidx.compose.foundation.style.lerp as dpLerp
import androidx.compose.foundation.style.stylePropertyOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Interpolatable
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.unit.Dp

/**
 * Creates a [ProvidableStyleProperty] of [Dp] that can be set from a [StylePropertyProviderScope]
 * and resolved from a [StylePropertyAccessorScope]. This is a convenient wrapper of
 * [stylePropertyOf] with a default interpolating function and a default value provider.
 *
 * @param name debug/tooling name for the property
 * @param interpolate interpolate the values `a` and `b` on `t` which is a fraction between 0f and
 *   1f; [androidx.compose.foundation.style.lerp] will be used by default
 * @param defaultValue fallback value provider when the property is not provided; by default
 *   [Dp.Unspecified] will be provided
 * @see stylePropertyOf
 */
@OptIn(ExperimentalFoundationStyleApi::class)
internal fun dpStylePropertyOf(
    name: String,
    interpolate: (a: Dp, b: Dp, t: Float) -> Dp = DpLerp,
    defaultValue: () -> Dp = DpDefault,
): ProvidableStyleProperty<Dp> = stylePropertyOf(name, interpolate, defaultValue)

/**
 * Creates a [ProvidableStyleProperty] of [Color] that can be set from a
 * [StylePropertyProviderScope] and resolved from a [StylePropertyAccessorScope]. This is a
 * convenient wrapper of [stylePropertyOf] with a default interpolating function and a default value
 * provider.
 *
 * @param name debug/tooling name for the property
 * @param interpolate interpolate the values `a` and `b` on `t` which is a fraction between 0f and
 *   1f; [androidx.compose.ui.graphics.lerp] will be used by default
 * @param defaultValue fallback value provider when the property is not provided; by default
 *   [Color.Unspecified] will be provided
 * @see stylePropertyOf
 */
@OptIn(ExperimentalFoundationStyleApi::class)
internal fun colorStylePropertyOf(
    name: String,
    interpolate: (a: Color, b: Color, t: Float) -> Color = ColorLerp,
    defaultValue: () -> Color = ColorDefault,
): ProvidableStyleProperty<Color> = stylePropertyOf(name, interpolate, defaultValue)

/**
 * Creates a [ProvidableStyleProperty] of [Breadth] that can be set from a
 * [StylePropertyProviderScope] and resolved from a [StylePropertyAccessorScope]. This is a
 * convenient wrapper of [stylePropertyOf] with a default interpolating function and a default value
 * provider.
 *
 * @param name debug/tooling name for the property
 * @param interpolate interpolate the values `a` and `b` on `t` which is a fraction between 0f and
 *   1f; [androidx.compose.ui.graphics.Interpolatable.lerp] will be used by default
 * @param defaultValue fallback value provider when the property is not provided; by default
 *   [Breadth.None] will be provided
 * @see stylePropertyOf
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalFoundationStyleApi::class)
internal fun breadthStylePropertyOf(
    name: String,
    interpolate: (a: Breadth, b: Breadth, t: Float) -> Breadth =
        NonNullLerp as (a: Breadth, b: Breadth, t: Float) -> Breadth,
    defaultValue: () -> Breadth = BreadthDefault,
): ProvidableStyleProperty<Breadth> = nonNullStylePropertyOf(name, interpolate, defaultValue)

/**
 * Creates a [ProvidableStyleProperty] of [Fill] that can be set from a [StylePropertyProviderScope]
 * and resolved from a [StylePropertyAccessorScope]. This is a convenient wrapper of
 * [stylePropertyOf] with a default interpolating function and a default value provider.
 *
 * @param name debug/tooling name for the property
 * @param interpolate interpolate the values `a` and `b` on `t` which is a fraction between 0f and
 *   1f; [androidx.compose.ui.graphics.Interpolatable.lerp] will be used by default
 * @param defaultValue fallback value provider when the property is not provided; by default
 *   [Fill.None] will be provided
 * @see stylePropertyOf
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalFoundationStyleApi::class)
internal fun fillStylePropertyOf(
    name: String,
    interpolate: (a: Fill, b: Fill, t: Float) -> Fill =
        NonNullLerp as (a: Fill, b: Fill, t: Float) -> Fill,
    defaultValue: () -> Fill = FillDefault,
): ProvidableStyleProperty<Fill> = nonNullStylePropertyOf(name, interpolate, defaultValue)

/**
 * Creates a [ProvidableStyleProperty] of any non-null types that can be set from a
 * [StylePropertyProviderScope] and resolved from a [StylePropertyAccessorScope]. This is a
 * convenient wrapper of [stylePropertyOf] with a default interpolating function and a default value
 * provider.
 *
 * @param name debug/tooling name for the property
 * @param interpolate interpolate the values `a` and `b` on `t` which is a fraction between 0f and
 *   1f; [androidx.compose.ui.graphics.Interpolatable.lerp] will be used by default
 * @param defaultValue fallback value provider when the property is not provided
 * @see stylePropertyOf
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalFoundationStyleApi::class)
internal fun <T> nonNullStylePropertyOf(
    name: String,
    interpolate: (a: T, b: T, t: Float) -> T = NonNullLerp as (a: T, b: T, t: Float) -> T,
    defaultValue: () -> T,
): ProvidableStyleProperty<T> = stylePropertyOf(name, interpolate, defaultValue)

/**
 * Creates a [ProvidableStyleProperty] of any nullable types that can be set from a
 * [StylePropertyProviderScope] and resolved from a [StylePropertyAccessorScope]. This is a
 * convenient wrapper of [stylePropertyOf] with a default interpolating function and a default value
 * provider.
 *
 * @param name debug/tooling name for the property
 * @param interpolate interpolate the values `a` and `b` on `t` which is a fraction between 0f and
 *   1f; [androidx.compose.ui.graphics.Interpolatable.lerp] will be used by default
 * @param defaultValue fallback value provider when the property is not provided; by default `null`
 *   will be provided
 * @see stylePropertyOf
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalFoundationStyleApi::class)
internal fun <T> nullableStylePropertyOf(
    name: String,
    interpolate: (a: T?, b: T?, t: Float) -> T? = NullableLerp as (a: T?, b: T?, t: Float) -> T?,
    defaultValue: () -> T? = NullableDefault as () -> T?,
): ProvidableStyleProperty<T?> = stylePropertyOf(name, interpolate, defaultValue)

// Below vals are used to avoid recreating lambda expressions when constructing a property.
@OptIn(ExperimentalFoundationStyleApi::class) private val BreadthDefault = { Breadth.None }
private val DpDefault = { Dp.Unspecified }
private val ColorDefault = { Color.Unspecified }
@OptIn(ExperimentalFoundationStyleApi::class) private val FillDefault = { Fill.None }
private val NullableDefault: () -> Any? = { null }
private val DpLerp: (Dp, Dp, Float) -> Dp = ::dpLerp
private val ColorLerp: (Color, Color, Float) -> Color = ::colorLerp
private val NullableLerp: (Any?, Any?, Float) -> Any? = Interpolatable::lerp
@Suppress("UNCHECKED_CAST")
private val NonNullLerp: (Any, Any, Float) -> Any = Interpolatable::lerp as (Any, Any, Float) -> Any
