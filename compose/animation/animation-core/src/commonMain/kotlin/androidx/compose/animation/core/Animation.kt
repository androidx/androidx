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

package androidx.compose.animation.core

import androidx.annotation.RestrictTo
import androidx.compose.animation.core.internal.JvmDefaultWithCompatibility

/**
 * This interface provides a convenient way to query from an [VectorizedAnimationSpec] or
 * [FloatDecayAnimationSpec]: It spares the need to pass the starting conditions and in some cases
 * ending condition for each value or velocity query, and instead only requires the play time to be
 * passed for such queries.
 *
 * The implementation of this interface should cache the starting conditions and ending conditions
 * of animations as needed.
 *
 * __Note__: [Animation] does not track the lifecycle of an animation. It merely reacts to play time
 * change and returns the new value/velocity as a result. It can be used as a building block for
 * more lifecycle aware animations. In contrast, [Animatable] and [Transition] are stateful and
 * manage their own lifecycles.
 *
 * @see [Animatable]
 * @see [rememberTransition]
 * @see [updateTransition]
 */
@JvmDefaultWithCompatibility
public interface Animation<T, V : AnimationVector> {
    /** This amount of time in nanoseconds that the animation will run before it finishes */
    @get:Suppress("MethodNameUnits") public val durationNanos: Long

    /**
     * The [TwoWayConverter] that will be used to convert value/velocity from any arbitrary data
     * type to [AnimationVector]. This makes it possible to animate different dimensions of the data
     * object independently (e.g. x/y dimensions of the position data).
     */
    public val typeConverter: TwoWayConverter<T, V>

    /** This is the value that the [Animation] will reach when it finishes uninterrupted. */
    public val targetValue: T

    /**
     * Whether or not the [Animation] represents an infinite animation. That is, one that will not
     * finish by itself, one that needs an external action to stop. For examples, an indeterminate
     * progress bar, which will only stop when it is removed from the composition.
     */
    public val isInfinite: Boolean

    /**
     * Returns the value of the animation at the given play time.
     *
     * @param playTimeNanos the play time that is used to determine the value of the animation.
     */
    public fun getValueFromNanos(playTimeNanos: Long): T

    /**
     * Returns the velocity (in [AnimationVector] form) of the animation at the given play time.
     *
     * @param playTimeNanos the play time that is used to calculate the velocity of the animation.
     */
    public fun getVelocityVectorFromNanos(playTimeNanos: Long): V

    /**
     * Returns whether the animation is finished at the given play time.
     *
     * @param playTimeNanos the play time used to determine whether the animation is finished.
     */
    public fun isFinishedFromNanos(playTimeNanos: Long): Boolean {
        return playTimeNanos >= durationNanos
    }
}

internal val Animation<*, *>.durationMillis: Long
    get() = durationNanos / MillisToNanos

internal const val MillisToNanos: Long = 1_000_000L

internal const val SecondsToMillis: Long = 1_000L

/**
 * Returns the velocity of the animation at the given play time.
 *
 * @param playTimeNanos the play time that is used to calculate the velocity of the animation.
 */
public fun <T, V : AnimationVector> Animation<T, V>.getVelocityFromNanos(playTimeNanos: Long): T =
    typeConverter.convertFromVector(getVelocityVectorFromNanos(playTimeNanos))

/**
 * Creates a [TargetBasedAnimation] from a given [VectorizedAnimationSpec] of [AnimationVector]
 * type. This convenient method is intended for when the value being animated (i.e. start value, end
 * value, etc) is of [AnimationVector] type.
 *
 * @param initialValue the value that the animation will start from
 * @param targetValue the value that the animation will end at
 * @param initialVelocity the initial velocity to start the animation at
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public fun <V : AnimationVector> VectorizedAnimationSpec<V>.createAnimation(
    initialValue: V,
    targetValue: V,
    initialVelocity: V
): TargetBasedAnimation<V, V> =
    TargetBasedAnimation(
        animationSpec = this,
        initialValue = initialValue,
        targetValue = targetValue,
        initialVelocityVector = initialVelocity,
        typeConverter = TwoWayConverter({ it }, { it })
    )

/**
 * Creates a [TargetBasedAnimation] with the given start/end conditions of the animation, and the
 * provided [animationSpec].
 *
 * The resulting [Animation] assumes that the start value and velocity, as well as end value do not
 * change throughout the animation, and cache these values. This caching enables much more
 * convenient query for animation value and velocity (where only playtime needs to be passed into
 * the methods).
 *
 * __Note__: When interruptions happen to the [TargetBasedAnimation], a new instance should be
 * created that use the current value and velocity as the starting conditions. This type of
 * interruption handling is the default behavior for both [Animatable] and [Transition]. Consider
 * using those APIs for the interruption handling, as well as built-in animation lifecycle
 * management.
 *
 * @param animationSpec the [AnimationSpec] that will be used to calculate value/velocity
 * @param initialValue the start value of the animation
 * @param targetValue the end value of the animation
 * @param initialVelocity the start velocity (of type [T] of the animation
 * @param typeConverter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
 */
public fun <T, V : AnimationVector> TargetBasedAnimation(
    animationSpec: AnimationSpec<T>,
    typeConverter: TwoWayConverter<T, V>,
    initialValue: T,
    targetValue: T,
    initialVelocity: T
): TargetBasedAnimation<T, V> =
    TargetBasedAnimation(
        animationSpec,
        typeConverter,
        initialValue,
        targetValue,
        typeConverter.convertToVector(initialVelocity)
    )

/**
 * This is a convenient animation wrapper class that works for all target based animations, i.e.
 * animations that has a pre-defined end value, unlike decay.
 *
 * It assumes that the starting value and velocity, as well as ending value do not change throughout
 * the animation, and cache these values. This caching enables much more convenient query for
 * animation value and velocity (where only playtime needs to be passed into the methods).
 *
 * __Note__: When interruptions happen to the [TargetBasedAnimation], a new instance should be
 * created that use the current value and velocity as the starting conditions. This type of
 * interruption handling is the default behavior for both [Animatable] and [Transition]. Consider
 * using those APIs for the interruption handling, as well as built-in animation lifecycle
 * management.
 *
 * @param animationSpec the [VectorizedAnimationSpec] that will be used to calculate value/velocity
 * @param initialValue the start value of the animation
 * @param targetValue the end value of the animation
 * @param typeConverter the [TwoWayConverter] that is used to convert animation type [T] from/to [V]
 * @param initialVelocityVector the start velocity of the animation in the form of [AnimationVector]
 * @see [Transition]
 * @see [rememberTransition]
 * @see [updateTransition]
 * @see [Animatable]
 */
public class TargetBasedAnimation<T, V : AnimationVector>
internal constructor(
    internal val animationSpec: VectorizedAnimationSpec<V>,
    override val typeConverter: TwoWayConverter<T, V>,
    initialValue: T,
    targetValue: T,
    initialVelocityVector: V? = null
) : Animation<T, V> {
    internal var mutableTargetValue: T = targetValue
        set(value) {
            if (field != value) {
                field = value
                targetValueVector = typeConverter.convertToVector(value)
                _endVelocity = null
                _durationNanos = -1L
            }
        }

    internal var mutableInitialValue: T = initialValue
        set(value) {
            if (value != field) {
                field = value
                initialValueVector = typeConverter.convertToVector(value)
                _endVelocity = null
                _durationNanos = -1L
            }
        }

    public val initialValue: T
        get() = mutableInitialValue

    override val targetValue: T
        get() = mutableTargetValue

    /**
     * Creates a [TargetBasedAnimation] with the given start/end conditions of the animation, and
     * the provided [animationSpec].
     *
     * The resulting [Animation] assumes that the start value and velocity, as well as end value do
     * not change throughout the animation, and cache these values. This caching enables much more
     * convenient query for animation value and velocity (where only playtime needs to be passed
     * into the methods).
     *
     * __Note__: When interruptions happen to the [TargetBasedAnimation], a new instance should be
     * created that use the current value and velocity as the starting conditions. This type of
     * interruption handling is the default behavior for both [Animatable] and [Transition].
     * Consider using those APIs for the interruption handling, as well as built-in animation
     * lifecycle management.
     *
     * @param animationSpec the [AnimationSpec] that will be used to calculate value/velocity
     * @param typeConverter the [TwoWayConverter] that is used to convert animation type [T] from/to
     *   [V]
     * @param initialValue the start value of the animation
     * @param targetValue the end value of the animation
     * @param initialVelocityVector the start velocity vector, null by default (meaning 0 velocity).
     */
    public constructor(
        animationSpec: AnimationSpec<T>,
        typeConverter: TwoWayConverter<T, V>,
        initialValue: T,
        targetValue: T,
        initialVelocityVector: V? = null
    ) : this(
        animationSpec.vectorize(typeConverter),
        typeConverter,
        initialValue,
        targetValue,
        initialVelocityVector
    )

    private var initialValueVector = typeConverter.convertToVector(initialValue)
    private var targetValueVector = typeConverter.convertToVector(targetValue)
    private val initialVelocityVector =
        initialVelocityVector?.copy() ?: typeConverter.convertToVector(initialValue).newInstance()

    override val isInfinite: Boolean
        get() = animationSpec.isInfinite

    override fun getValueFromNanos(playTimeNanos: Long): T {
        return if (!isFinishedFromNanos(playTimeNanos)) {
            animationSpec
                .getValueFromNanos(
                    playTimeNanos,
                    initialValueVector,
                    targetValueVector,
                    initialVelocityVector
                )
                .let {
                    // TODO: Remove after b/232030217
                    for (i in 0 until it.size) {
                        checkPrecondition(!it.get(i).isNaN()) {
                            "AnimationVector cannot contain a NaN. $it. Animation: $this," +
                                " playTimeNanos: $playTimeNanos"
                        }
                    }
                    typeConverter.convertFromVector(it)
                }
        } else {
            targetValue
        }
    }

    private var _durationNanos: Long = -1L

    @get:Suppress("MethodNameUnits")
    override val durationNanos: Long
        get() {
            if (_durationNanos < 0L) {
                _durationNanos =
                    animationSpec.getDurationNanos(
                        initialValue = initialValueVector,
                        targetValue = targetValueVector,
                        initialVelocity = this.initialVelocityVector
                    )
            }
            return _durationNanos
        }

    private var _endVelocity: V? = null

    private val endVelocity
        get() =
            _endVelocity
                ?: animationSpec
                    .getEndVelocity(
                        initialValueVector,
                        targetValueVector,
                        this.initialVelocityVector
                    )
                    .also { _endVelocity = it }

    override fun getVelocityVectorFromNanos(playTimeNanos: Long): V {
        return if (!isFinishedFromNanos(playTimeNanos)) {
            animationSpec.getVelocityFromNanos(
                playTimeNanos,
                initialValueVector,
                targetValueVector,
                initialVelocityVector
            )
        } else {
            endVelocity
        }
    }

    override fun toString(): String {
        return "TargetBasedAnimation: $initialValue -> $targetValue," +
            "initial velocity: $initialVelocityVector, duration: $durationMillis ms," +
            "animationSpec: $animationSpec"
    }
}

/**
 * [DecayAnimation] is an animation that slows down from [initialVelocityVector] as time goes on.
 * [DecayAnimation] is stateless, and it does not have any concept of lifecycle. It serves as an
 * animation calculation engine that supports convenient query of value/velocity given a play time.
 * To achieve that, [DecayAnimation] stores all the animation related information: [initialValue],
 * [initialVelocityVector], decay animation spec, [typeConverter].
 *
 * __Note__: Unless there's a need to control the timing manually, it's generally recommended to use
 * higher level animation APIs that build on top [DecayAnimation], such as
 * [Animatable.animateDecay], [AnimationState.animateDecay], etc.
 *
 * @see Animatable.animateDecay
 * @see AnimationState.animateDecay
 */
public class DecayAnimation<T, V : AnimationVector> /*@VisibleForTesting*/
constructor(
    private val animationSpec: VectorizedDecayAnimationSpec<V>,
    override val typeConverter: TwoWayConverter<T, V>,
    public val initialValue: T,
    initialVelocityVector: V
) : Animation<T, V> {
    private val initialValueVector: V = typeConverter.convertToVector(initialValue)
    public val initialVelocityVector: V = initialVelocityVector.copy()
    private val endVelocity: V

    override val targetValue: T =
        typeConverter.convertFromVector(
            animationSpec.getTargetValue(initialValueVector, initialVelocityVector)
        )
    @get:Suppress("MethodNameUnits") override val durationNanos: Long

    // DecayAnimation finishes by design
    override val isInfinite: Boolean = false

    /**
     * [DecayAnimation] is an animation that slows down from [initialVelocityVector] as time goes
     * on. [DecayAnimation] is stateless, and it does not have any concept of lifecycle. It serves
     * as an animation calculation engine that supports convenient query of value/velocity given a
     * play time. To achieve that, [DecayAnimation] stores all the animation related information:
     * [initialValue], [initialVelocityVector], decay animation spec, [typeConverter].
     *
     * __Note__: Unless there's a need to control the timing manually, it's generally recommended to
     * use higher level animation APIs that build on top [DecayAnimation], such as
     * [Animatable.animateDecay], [AnimationState.animateDecay], etc.
     *
     * @param animationSpec Decay animation spec that defines the slow-down curve of the animation
     * @param typeConverter Type converter to convert the type [T] from and to [AnimationVector]
     * @param initialValue The starting value of the animation
     * @param initialVelocityVector The starting velocity of the animation in [AnimationVector] form
     * @see Animatable.animateDecay
     * @see AnimationState.animateDecay
     */
    public constructor(
        animationSpec: DecayAnimationSpec<T>,
        typeConverter: TwoWayConverter<T, V>,
        initialValue: T,
        initialVelocityVector: V
    ) : this(
        animationSpec.vectorize(typeConverter),
        typeConverter,
        initialValue,
        initialVelocityVector
    )

    /**
     * [DecayAnimation] is an animation that slows down from [initialVelocity] as time goes on.
     * [DecayAnimation] is stateless, and it does not have any concept of lifecycle. It serves as an
     * animation calculation engine that supports convenient query of value/velocity given a play
     * time. To achieve that, [DecayAnimation] stores all the animation related information:
     * [initialValue], [initialVelocity], [animationSpec], [typeConverter].
     *
     * __Note__: Unless there's a need to control the timing manually, it's generally recommended to
     * use higher level animation APIs that build on top [DecayAnimation], such as
     * [Animatable.animateDecay], [AnimationState.animateDecay], etc.
     *
     * @param animationSpec Decay animation spec that defines the slow-down curve of the animation
     * @param typeConverter Type converter to convert the type [T] from and to [AnimationVector]
     * @param initialValue The starting value of the animation
     * @param initialVelocity The starting velocity of the animation
     * @see Animatable.animateDecay
     * @see AnimationState.animateDecay
     */
    public constructor(
        animationSpec: DecayAnimationSpec<T>,
        typeConverter: TwoWayConverter<T, V>,
        initialValue: T,
        initialVelocity: T
    ) : this(
        animationSpec.vectorize(typeConverter),
        typeConverter,
        initialValue,
        typeConverter.convertToVector(initialVelocity)
    )

    init {
        durationNanos = animationSpec.getDurationNanos(initialValueVector, initialVelocityVector)
        endVelocity =
            animationSpec
                .getVelocityFromNanos(durationNanos, initialValueVector, initialVelocityVector)
                .copy()
        for (i in 0 until endVelocity.size) {
            endVelocity[i] =
                endVelocity[i].coerceIn(
                    -animationSpec.absVelocityThreshold,
                    animationSpec.absVelocityThreshold
                )
        }
    }

    override fun getValueFromNanos(playTimeNanos: Long): T {
        if (!isFinishedFromNanos(playTimeNanos)) {
            return typeConverter.convertFromVector(
                animationSpec.getValueFromNanos(
                    playTimeNanos,
                    initialValueVector,
                    initialVelocityVector
                )
            )
        } else {
            return targetValue
        }
    }

    override fun getVelocityVectorFromNanos(playTimeNanos: Long): V {
        if (!isFinishedFromNanos(playTimeNanos)) {
            return animationSpec.getVelocityFromNanos(
                playTimeNanos,
                initialValueVector,
                initialVelocityVector
            )
        } else {
            return endVelocity
        }
    }
}

/**
 * [DecayAnimation] is an animation that slows down from [initialVelocity] as time goes on.
 * [DecayAnimation] is stateless, and it does not have any concept of lifecycle. It serves as an
 * animation calculation engine that supports convenient query of value/velocity given a play time.
 * To achieve that, [DecayAnimation] stores all the animation related information: [initialValue],
 * [initialVelocity], decay animation spec.
 *
 * __Note__: Unless there's a need to control the timing manually, it's generally recommended to use
 * higher level animation APIs that build on top [DecayAnimation], such as
 * [Animatable.animateDecay], [animateDecay], etc.
 *
 * @param animationSpec decay animation that will be used
 * @param initialValue starting value that will be passed to the decay animation
 * @param initialVelocity starting velocity for the decay animation, 0f by default
 */
public fun DecayAnimation(
    animationSpec: FloatDecayAnimationSpec,
    initialValue: Float,
    initialVelocity: Float = 0f
): DecayAnimation<Float, AnimationVector1D> =
    DecayAnimation(
        animationSpec.generateDecayAnimationSpec(),
        Float.VectorConverter,
        initialValue,
        AnimationVector(initialVelocity)
    )
