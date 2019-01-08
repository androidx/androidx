/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.painting.Color
import androidx.ui.painting.borders.ShapeBorder
import androidx.ui.painting.borders.lerp

// TODO(Migration|Andrey) Crane-specific typealias and extension constructors as we can't
// TODO(Migration|Andrey) call A + B on any type in Kotlin

/**
 * Provides "begin + (end - begin) * t" implementation for type T
 */
typealias TweenEvaluator<T> = (begin: T, end: T, t: Float) -> T

fun Tween(begin: Float? = null, end: Float? = null) = Tween(begin, end, FloatTweenEvaluator)

fun Tween(begin: Double? = null, end: Double? = null) = Tween(begin, end, DoubleTweenEvaluator)

fun Tween(begin: Int? = null, end: Int? = null) = Tween(begin, end, IntTweenEvaluator)

fun Tween(begin: Long? = null, end: Long? = null) = Tween(begin, end, LongTweenEvaluator)

fun Tween(begin: Color? = null, end: Color? = null) = Tween(begin, end, ColorTweenEvaluator)

fun Tween(begin: Size? = null, end: Size? = null) = Tween(begin, end, SizeTweenEvaluator)

fun Tween(begin: Rect? = null, end: Rect? = null) = Tween(begin, end, RectTweenEvaluator)

fun Tween(begin: ShapeBorder? = null, end: ShapeBorder? = null) =
    Tween(begin, end, ShapeBorderEvaluator)

private object FloatTweenEvaluator : TweenEvaluator<Float> {
    override fun invoke(begin: Float, end: Float, t: Float): Float {
        return begin + ((end - begin) * t)
    }
}

private object DoubleTweenEvaluator : TweenEvaluator<Double> {
    override fun invoke(begin: Double, end: Double, t: Float): Double {
        return begin + (end - begin) * t
    }
}

private object IntTweenEvaluator : TweenEvaluator<Int> {
    override fun invoke(begin: Int, end: Int, t: Float): Int {
        return begin + ((end - begin) * t).toInt()
    }
}

private object LongTweenEvaluator : TweenEvaluator<Long> {
    override fun invoke(begin: Long, end: Long, t: Float): Long {
        return begin + ((end - begin) * t).toLong()
    }
}

private object ColorTweenEvaluator : TweenEvaluator<Color> {
    override fun invoke(begin: Color, end: Color, t: Float): Color {
        return Color.lerp(begin, end, t)!!
    }
}

private object SizeTweenEvaluator : TweenEvaluator<Size> {
    override fun invoke(begin: Size, end: Size, t: Float): Size {
        return Size.lerp(begin, end, t)!!
    }
}

private object RectTweenEvaluator : TweenEvaluator<Rect> {
    override fun invoke(begin: Rect, end: Rect, t: Float): Rect {
        return Rect.lerp(begin, end, t)!!
    }
}

private object ShapeBorderEvaluator : TweenEvaluator<ShapeBorder> {
    override fun invoke(begin: ShapeBorder, end: ShapeBorder, t: Float): ShapeBorder {
        return lerp(begin, end, t)!!
    }
}

// TODO(Migration|Andrey) Provide ready to use evaluators for more classes?