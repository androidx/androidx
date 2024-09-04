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

package androidx.compose.animation.core

internal fun VectorizedAnimationSpec<AnimationVector1D>.at(time: Long): Float =
    getValueFromMillis(time, AnimationVector1D(0f), AnimationVector1D(1f), AnimationVector1D(0f))
        .value

internal fun VectorizedAnimationSpec<AnimationVector1D>.at(time: Int): Float = at(time.toLong())

internal fun VectorizedAnimationSpec<AnimationVector1D>.getValue(
    playTime: Long,
    start: Number,
    end: Number,
    startVelocity: Number
) =
    getValueFromMillis(
            playTime,
            AnimationVector1D(start.toFloat()),
            AnimationVector1D(end.toFloat()),
            AnimationVector1D(startVelocity.toFloat())
        )
        .value

internal fun VectorizedAnimationSpec<AnimationVector1D>.getVelocity(
    playTime: Long,
    start: Number,
    end: Number,
    startVelocity: Number
) =
    getVelocityFromNanos(
            playTime * MillisToNanos,
            AnimationVector1D(start.toFloat()),
            AnimationVector1D(end.toFloat()),
            AnimationVector1D(startVelocity.toFloat())
        )
        .value

/**
 * Returns the value of the animation at the given play time.
 *
 * @param playTimeMillis the play time that is used to determine the value of the animation.
 */
internal fun <T> Animation<T, *>.getValueFromMillis(playTimeMillis: Long): T =
    getValueFromNanos(playTimeMillis * MillisToNanos)

/**
 * Returns the velocity (in [AnimationVector] form) of the animation at the given play time.
 *
 * @param playTimeMillis the play time that is used to calculate the velocity of the animation.
 */
internal fun <V : AnimationVector> Animation<*, V>.getVelocityVectorFromMillis(
    playTimeMillis: Long
): V = getVelocityVectorFromNanos(playTimeMillis * MillisToNanos)

/**
 * Returns whether the animation is finished at the given play time.
 *
 * @param playTimeMillis the play time used to determine whether the animation is finished.
 */
internal fun Animation<*, *>.isFinishedFromMillis(playTimeMillis: Long): Boolean {
    return playTimeMillis >= durationMillis
}

internal fun <T, V : AnimationVector> Animation<T, V>.getVelocityFromMillis(
    playTimeMillis: Long
): T = typeConverter.convertFromVector(getVelocityVectorFromMillis(playTimeMillis))

internal fun FloatAnimationSpec.getDurationMillis(
    start: Float,
    end: Float,
    startVelocity: Float
): Long = getDurationNanos(start, end, startVelocity) / MillisToNanos

/**
 * Calculates the value of the animation at given the playtime, with the provided start/end values,
 * and start velocity.
 *
 * @param playTimeMillis time since the start of the animation
 * @param start start value of the animation
 * @param end end value of the animation
 * @param startVelocity start velocity of the animation
 */
// TODO: bring all tests on to `getValueFromNanos`
internal fun FloatAnimationSpec.getValueFromMillis(
    playTimeMillis: Long,
    start: Float,
    end: Float,
    startVelocity: Float
): Float = getValueFromNanos(playTimeMillis * MillisToNanos, start, end, startVelocity)

/**
 * Calculates the velocity of the animation at given the playtime, with the provided start/end
 * values, and start velocity.
 *
 * @param playTimeMillis time since the start of the animation
 * @param start start value of the animation
 * @param end end value of the animation
 * @param startVelocity start velocity of the animation
 */
// TODO: bring all tests on to `getVelocityFromNanos`
internal fun FloatAnimationSpec.getVelocityFromMillis(
    playTimeMillis: Long,
    start: Float,
    end: Float,
    startVelocity: Float
): Float = getVelocityFromNanos(playTimeMillis * MillisToNanos, start, end, startVelocity)

/** Creates a TwoWayConverter for FloatArray and the given AnimationVector type. */
internal inline fun <reified V : AnimationVector> createFloatArrayConverter():
    TwoWayConverter<FloatArray, V> =
    object : TwoWayConverter<FloatArray, V> {
        override val convertToVector: (FloatArray) -> V = {
            when (V::class) {
                AnimationVector1D::class -> {
                    AnimationVector(it.getOrElse(0) { 0f })
                }
                AnimationVector2D::class -> {
                    AnimationVector(
                        it.getOrElse(0) { 0f },
                        it.getOrElse(1) { 0f },
                    )
                }
                AnimationVector3D::class -> {
                    AnimationVector(
                        it.getOrElse(0) { 0f },
                        it.getOrElse(1) { 0f },
                        it.getOrElse(2) { 0f }
                    )
                }
                else -> { // 4D
                    AnimationVector(
                        it.getOrElse(0) { 0f },
                        it.getOrElse(1) { 0f },
                        it.getOrElse(2) { 0f },
                        it.getOrElse(3) { 0f }
                    )
                }
            }
                as V
        }
        override val convertFromVector: (V) -> FloatArray = { vector ->
            FloatArray(vector.size, vector::get)
        }
    }

/** Returns an [AnimationVector] of type [V] filled with the given [value]. */
internal inline fun <reified V : AnimationVector> createFilledVector(value: Float): V =
    when (V::class) {
        AnimationVector1D::class -> AnimationVector1D(value)
        AnimationVector2D::class -> AnimationVector2D(value, value)
        AnimationVector3D::class -> AnimationVector3D(value, value, value)
        else -> AnimationVector4D(value, value, value, value)
    }
        as V
