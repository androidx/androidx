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

// TODO(Migration|Andrey) Crane-specific typealias and extension constructors as we can't
// TODO(Migration|Andrey) call A + B on any type in Kotlin

/**
 * Provides "begin + (end - begin) * t" implementation for type T
 */
typealias TweenEvaluator<T> = (begin: T, end: T, t: Double) -> T

fun Tween(begin: Float?, end: Float?) = Tween(begin, end, FloatTweenEvaluator)

fun Tween(begin: Double?, end: Double?) = Tween(begin, end, DoubleTweenEvaluator)

fun Tween(begin: Int?, end: Int?) = Tween(begin, end, IntTweenEvaluator)

fun Tween(begin: Long?, end: Long?) = Tween(begin, end, LongTweenEvaluator)

fun Tween(begin: Color?, end: Color?) = Tween(begin, end, ColorTweenEvaluator)

fun Tween(begin: Size?, end: Size?) = Tween(begin, end, SizeTweenEvaluator)

fun Tween(begin: Rect?, end: Rect?) = Tween(begin, end, RectTweenEvaluator)

private object FloatTweenEvaluator : TweenEvaluator<Float> {
    override fun invoke(begin: Float, end: Float, t: Double): Float {
        return begin + ((end - begin) * t).toFloat()
    }
}

private object DoubleTweenEvaluator : TweenEvaluator<Double> {
    override fun invoke(begin: Double, end: Double, t: Double): Double {
        return begin + (end - begin) * t
    }
}

private object IntTweenEvaluator : TweenEvaluator<Int> {
    override fun invoke(begin: Int, end: Int, t: Double): Int {
        return begin + ((end - begin) * t).toInt()
    }
}

private object LongTweenEvaluator : TweenEvaluator<Long> {
    override fun invoke(begin: Long, end: Long, t: Double): Long {
        return begin + ((end - begin) * t).toLong()
    }
}

private object ColorTweenEvaluator : TweenEvaluator<Color> {
    override fun invoke(begin: Color, end: Color, t: Double): Color {
        return Color.lerp(begin, end, t)!!
    }
}

private object SizeTweenEvaluator : TweenEvaluator<Size> {
    override fun invoke(begin: Size, end: Size, t: Double): Size {
        return Size.lerp(begin, end, t)!!
    }
}

private object RectTweenEvaluator : TweenEvaluator<Rect> {
    override fun invoke(begin: Rect, end: Rect, t: Double): Rect {
        return Rect.lerp(begin, end, t)!!
    }
}

// TODO(Migration|Andrey) Provide ready to use evaluators for more classes?