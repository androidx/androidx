/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.animation

import androidx.animation.AnimatedFloat
import androidx.animation.AnimatedValue
import androidx.compose.Model
import androidx.animation.ValueHolder
import androidx.annotation.CheckResult
import androidx.compose.Effect
import androidx.compose.memo
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.lerp

/**
 * The animatedValue effect creates an [AnimatedValue] and positionally memoizes it. When the
 * [AnimatedValue] object gets its value updated, components that rely on that value will be
 * automatically recomposed.
 *
 * @param initVal Initial value to set [AnimatedValue] to.
 * @param interpolator A value interpolator for interpolating two values of type [T]
 */
@CheckResult(suggest = "+")
fun <T> animatedValue(initVal: T, interpolator: (T, T, Float) -> T): Effect<AnimatedValue<T>> =
    memo { AnimatedValue(AnimValueHolder(initVal, interpolator)) }

/**
 * The animatedValue effect creates an [AnimatedFloat] and positionally memoizes it. When the
 * [AnimatedFloat] object gets its value updated, components that rely on that value will be
 * automatically recomposed.
 *
 * @param initVal Initial value to set [AnimatedFloat] to.
 */
@CheckResult(suggest = "+")
fun animatedFloat(initVal: Float): Effect<AnimatedFloat> =
    memo { AnimatedFloat(AnimValueHolder(initVal, ::lerp)) }

/**
 * The animatedValue effect creates an [AnimatedValue] of [Color] and positionally memoizes it. When
 * the [AnimatedValue] object gets its value updated, components that rely on that value will be
 * automatically recomposed.
 *
 * @param initVal Initial value to set [AnimatedValue] to.
 */
@CheckResult(suggest = "+")
fun animatedColor(initVal: Color): Effect<AnimatedValue<Color>> =
    memo { AnimatedValue(AnimValueHolder(initVal, ::lerp)) }

@Model
private class AnimValueHolder<T>(
    override var value: T,
    override val interpolator: (T, T, Float) -> T
) : ValueHolder<T>
