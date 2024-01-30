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

import androidx.annotation.IntRange
import androidx.collection.MutableIntList
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.KeyframesSpec.KeyframesSpecConfig
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt

object AnimationConstants {
    /**
     * The default duration used in [VectorizedAnimationSpec]s and [AnimationSpec].
     */
    const val DefaultDurationMillis: Int = 300

    /**
     * The value that is used when the animation time is not yet set.
     */
    const val UnspecifiedTime: Long = Long.MIN_VALUE
}

/**
 * [AnimationSpec] stores the specification of an animation, including 1) the data type to be
 * animated, and 2) the animation configuration (i.e. [VectorizedAnimationSpec]) that will be used
 * once the data (of type [T]) has been converted to [AnimationVector].
 *
 * Any type [T] can be animated by the system as long as a [TwoWayConverter] is supplied to convert
 * the data type [T] from and to an [AnimationVector]. There are a number of converters
 * available out of the box. For example, to animate [androidx.compose.ui.unit.IntOffset] the system
 * uses [IntOffset.VectorConverter][IntOffset.Companion.VectorConverter] to convert the object to
 * [AnimationVector2D], so that both x and y dimensions are animated independently with separate
 * velocity tracking. This enables multidimensional objects to be animated in a true
 * multi-dimensional way. It is particularly useful for smoothly handling animation interruptions
 * (such as when the target changes during the animation).
 */
interface AnimationSpec<T> {
    /**
     * Creates a [VectorizedAnimationSpec] with the given [TwoWayConverter].
     *
     * The underlying animation system operates on [AnimationVector]s. [T] will be converted to
     * [AnimationVector] to animate. [VectorizedAnimationSpec] describes how the
     * converted [AnimationVector] should be animated. E.g. The animation could simply
     * interpolate between the start and end values (i.e.[TweenSpec]), or apply spring physics
     * to produce the motion (i.e. [SpringSpec]), etc)
     *
     * @param converter converts the type [T] from and to [AnimationVector] type
     */
    fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedAnimationSpec<V>
}

/**
 * [FiniteAnimationSpec] is the interface that all non-infinite [AnimationSpec]s implement,
 * including: [TweenSpec], [SpringSpec], [KeyframesSpec], [RepeatableSpec], [SnapSpec], etc. By
 * definition, [InfiniteRepeatableSpec] __does not__ implement this interface.
 *
 * @see [InfiniteRepeatableSpec]
 */
interface FiniteAnimationSpec<T> : AnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V>
}

/**
 * Creates a TweenSpec configured with the given duration, delay, and easing curve.
 *
 * @param durationMillis duration of the [VectorizedTweenSpec] animation.
 * @param delay the number of milliseconds the animation waits before starting, 0 by default.
 * @param easing the easing curve used by the animation. [FastOutSlowInEasing] by default.
 */
@Immutable
class TweenSpec<T>(
    val durationMillis: Int = DefaultDurationMillis,
    val delay: Int = 0,
    val easing: Easing = FastOutSlowInEasing
) : DurationBasedAnimationSpec<T> {

    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<T, V>) =
        VectorizedTweenSpec<V>(durationMillis, delay, easing)

    override fun equals(other: Any?): Boolean =
        if (other is TweenSpec<*>) {
            other.durationMillis == this.durationMillis &&
                other.delay == this.delay &&
                other.easing == this.easing
        } else {
            false
        }

    override fun hashCode(): Int {
        return (durationMillis * 31 + easing.hashCode()) * 31 + delay
    }
}

/**
 *  This describes [AnimationSpec]s that are based on a fixed duration, such as [KeyframesSpec],
 *  [TweenSpec], and [SnapSpec]. These duration based specs can repeated when put into a
 *  [RepeatableSpec].
 */
interface DurationBasedAnimationSpec<T> : FiniteAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<T, V>):
        VectorizedDurationBasedAnimationSpec<V>
}

/**
 * Creates a [SpringSpec] that uses the given spring constants (i.e. [dampingRatio] and
 * [stiffness]. The optional [visibilityThreshold] defines when the animation
 * should be considered to be visually close enough to round off to its target.
 *
 * @param dampingRatio damping ratio of the spring. [Spring.DampingRatioNoBouncy] by default.
 * @param stiffness stiffness of the spring. [Spring.StiffnessMedium] by default.
 * @param visibilityThreshold specifies the visibility threshold
 */
// TODO: annotate damping/stiffness with FloatRange
@Immutable
class SpringSpec<T>(
    val dampingRatio: Float = Spring.DampingRatioNoBouncy,
    val stiffness: Float = Spring.StiffnessMedium,
    val visibilityThreshold: T? = null
) : FiniteAnimationSpec<T> {

    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<T, V>) =
        VectorizedSpringSpec(dampingRatio, stiffness, converter.convert(visibilityThreshold))

    override fun equals(other: Any?): Boolean =
        if (other is SpringSpec<*>) {
            other.dampingRatio == this.dampingRatio &&
                other.stiffness == this.stiffness &&
                other.visibilityThreshold == this.visibilityThreshold
        } else {
            false
        }

    override fun hashCode(): Int =
        (visibilityThreshold.hashCode() * 31 + dampingRatio.hashCode()) * 31 + stiffness.hashCode()
}

private fun <T, V : AnimationVector> TwoWayConverter<T, V>.convert(data: T?): V? {
    if (data == null) {
        return null
    } else {
        return convertToVector(data)
    }
}

/**
 * This class defines the two types of [StartOffset]: [StartOffsetType.Delay] and
 * [StartOffsetType.FastForward].
 * [StartOffsetType.Delay] delays the start of the animation, whereas [StartOffsetType.FastForward]
 * starts the animation right away from a given play time in the animation.
 *
 * @see repeatable
 * @see infiniteRepeatable
 * @see StartOffset
 */
@kotlin.jvm.JvmInline
value class StartOffsetType private constructor(internal val value: Int) {
    companion object {
        /**
         * Delays the start of the animation.
         */
        val Delay = StartOffsetType(-1)

        /**
         * Fast forwards the animation to a given play time, and starts it immediately.
         */
        val FastForward = StartOffsetType(1)
    }
}

/**
 * This class defines a start offset for [repeatable] and [infiniteRepeatable]. There are two types
 * of start offsets: [StartOffsetType.Delay] and [StartOffsetType.FastForward].
 * [StartOffsetType.Delay] delays the start of the animation, whereas [StartOffsetType.FastForward]
 * fast forwards the animation to a given play time and starts it right away.
 *
 * @sample androidx.compose.animation.core.samples.InfiniteProgressIndicator
 */
// This is an inline of Long so that when adding a StartOffset param to the end of constructor
// param list, it won't be confused with/clash with the mask param generated by constructors.
@kotlin.jvm.JvmInline
value class StartOffset private constructor(internal val value: Long) {
    /**
     * This creates a start offset for [repeatable] and [infiniteRepeatable]. [offsetType] can be
     * either of the following: [StartOffsetType.Delay] and [StartOffsetType.FastForward].
     * [offsetType] defaults to [StartOffsetType.Delay].
     *
     * [StartOffsetType.Delay] delays the start of the animation by [offsetMillis], whereas
     * [StartOffsetType.FastForward] starts the animation right away from [offsetMillis] in the
     * animation.
     */
    constructor(offsetMillis: Int, offsetType: StartOffsetType = StartOffsetType.Delay) : this(
        (offsetMillis * offsetType.value).toLong()
    )

    /**
     * Returns the number of milliseconds to offset the start of the animation.
     */
    val offsetMillis: Int
        get() = abs(this.value.toInt())

    /**
     * Returns the offset type of the provided [StartOffset].
     */
    val offsetType: StartOffsetType
        get() = when (this.value > 0) {
            true -> StartOffsetType.FastForward
            false -> StartOffsetType.Delay
        }
}

/**
 * [RepeatableSpec] takes another [DurationBasedAnimationSpec] and plays it [iterations] times. For
 * creating infinitely repeating animation spec, consider using [InfiniteRepeatableSpec].
 *
 * __Note__: When repeating in the [RepeatMode.Reverse] mode, it's highly recommended to have an
 * __odd__ number of iterations. Otherwise, the animation may jump to the end value when it finishes
 * the last iteration.
 *
 * [initialStartOffset] can be used to either delay the start of the animation or to fast forward
 * the animation to a given play time. This start offset will **not** be repeated, whereas the delay
 * in the [animation] (if any) will be repeated. By default, the amount of offset is 0.
 *
 * @see repeatable
 * @see InfiniteRepeatableSpec
 * @see infiniteRepeatable
 *
 * @param iterations the count of iterations. Should be at least 1.
 * @param animation the [AnimationSpec] to be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *                  [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 */
@Immutable
class RepeatableSpec<T>(
    val iterations: Int,
    val animation: DurationBasedAnimationSpec<T>,
    val repeatMode: RepeatMode = RepeatMode.Restart,
    val initialStartOffset: StartOffset = StartOffset(0)
) : FiniteAnimationSpec<T> {

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "This constructor has been deprecated"
    )
    constructor(
        iterations: Int,
        animation: DurationBasedAnimationSpec<T>,
        repeatMode: RepeatMode = RepeatMode.Restart
    ) : this(iterations, animation, repeatMode, StartOffset(0))

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V> {
        return VectorizedRepeatableSpec(
            iterations, animation.vectorize(converter), repeatMode, initialStartOffset
        )
    }

    override fun equals(other: Any?): Boolean =
        if (other is RepeatableSpec<*>) {
            other.iterations == this.iterations &&
                other.animation == this.animation &&
                other.repeatMode == this.repeatMode &&
                other.initialStartOffset == this.initialStartOffset
        } else {
            false
        }

    override fun hashCode(): Int {
        return ((iterations * 31 + animation.hashCode()) * 31 + repeatMode.hashCode()) * 31 +
            initialStartOffset.hashCode()
    }
}

/**
 * [InfiniteRepeatableSpec] repeats the provided [animation] infinite amount of times. It will
 * never naturally finish. This means the animation will only be stopped via some form of manual
 * cancellation. When used with transition or other animation composables, the infinite animations
 * will stop when the composable is removed from the compose tree.
 *
 * For non-infinite repeating animations, consider [RepeatableSpec].
 *
 * [initialStartOffset] can be used to either delay the start of the animation or to fast forward
 * the animation to a given play time. This start offset will **not** be repeated, whereas the delay
 * in the [animation] (if any) will be repeated. By default, the amount of offset is 0.
 *
 * @sample androidx.compose.animation.core.samples.InfiniteProgressIndicator
 *
 * @param animation the [AnimationSpec] to be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *                  [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 * @see infiniteRepeatable
 */
// TODO: Consider supporting repeating spring specs
class InfiniteRepeatableSpec<T>(
    val animation: DurationBasedAnimationSpec<T>,
    val repeatMode: RepeatMode = RepeatMode.Restart,
    val initialStartOffset: StartOffset = StartOffset(0)
) : AnimationSpec<T> {

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "This constructor has been deprecated"
    )
    constructor(
        animation: DurationBasedAnimationSpec<T>,
        repeatMode: RepeatMode = RepeatMode.Restart
    ) : this(animation, repeatMode, StartOffset(0))

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedAnimationSpec<V> {
        return VectorizedInfiniteRepeatableSpec(
            animation.vectorize(converter), repeatMode, initialStartOffset
        )
    }

    override fun equals(other: Any?): Boolean =
        if (other is InfiniteRepeatableSpec<*>) {
            other.animation == this.animation && other.repeatMode == this.repeatMode &&
                other.initialStartOffset == this.initialStartOffset
        } else {
            false
        }

    override fun hashCode(): Int {
        return (animation.hashCode() * 31 + repeatMode.hashCode()) * 31 +
            initialStartOffset.hashCode()
    }
}

/**
 * Repeat mode for [RepeatableSpec] and [VectorizedRepeatableSpec].
 */
enum class RepeatMode {
    /**
     * [Restart] will restart the animation and animate from the start value to the end value.
     */
    Restart,

    /**
     * [Reverse] will reverse the last iteration as the animation repeats.
     */
    Reverse
}

/**
 * [SnapSpec] describes a jump-cut type of animation. It immediately snaps the animating value to
 * the end value.
 *
 * @param delay the amount of time (in milliseconds) that the animation should wait before it
 *              starts. Defaults to 0.
 */
@Immutable
class SnapSpec<T>(val delay: Int = 0) : DurationBasedAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedDurationBasedAnimationSpec<V> = VectorizedSnapSpec(delay)

    override fun equals(other: Any?): Boolean =
        if (other is SnapSpec<*>) {
            other.delay == this.delay
        } else {
            false
        }

    override fun hashCode(): Int {
        return delay
    }
}

/**
 * Shared configuration class used as DSL for keyframe based animations.
 */
sealed class KeyframesSpecBaseConfig<T, E : KeyframeBaseEntity<T>> {
    /**
     * Duration of the animation in milliseconds. The minimum is `0` and defaults to
     * [DefaultDurationMillis]
     */
    @get:IntRange(from = 0)
    var durationMillis: Int = DefaultDurationMillis

    /**
     * The amount of time that the animation should be delayed. The minimum is `0` and defaults
     * to 0.
     */
    @get:IntRange(from = 0)
    var delayMillis: Int = 0

    internal val keyframes = mutableIntObjectMapOf<E>()

    /**
     * Method used to delegate instantiation of [E] to implementing classes.
     */
    internal abstract fun createEntityFor(value: T): E

    /**
     * Adds a keyframe so that animation value will be [this] at time: [timeStamp]. For example:
     *
     * @sample androidx.compose.animation.core.samples.floatAtSample
     *
     * @param timeStamp The time in the during when animation should reach value: [this], with
     * a minimum value of `0`.
     * @return an instance of [E] so a custom [Easing] can be added by the [using] method.
     */
    infix fun T.at(@IntRange(from = 0) timeStamp: Int): E {
        val entity = createEntityFor(this)
        keyframes[timeStamp] = entity
        return entity
    }

    /**
     * Adds a keyframe so that the animation value will be the value specified at a fraction of the
     * total [durationMillis] set. It's recommended that you always set [durationMillis] before
     * calling [atFraction]. For example:
     *
     * @sample androidx.compose.animation.core.samples.floatAtFractionSample
     *
     *  @param fraction The fraction when the animation should reach specified value.
     *  @return an instance of [E] so a custom [Easing] can be added by the [using] method
     */
    infix fun T.atFraction(fraction: Float): E {
        return at((durationMillis * fraction).roundToInt())
    }

    /**
     * Adds an [Easing] for the interval started with the just provided timestamp. For example:
     *     0f at 50 using LinearEasing
     *
     * @sample androidx.compose.animation.core.samples.KeyframesBuilderWithEasing
     * @param easing [Easing] to be used for the next interval.
     * @return the same [E] instance so that other implementations can expand on the builder pattern
     */
    infix fun E.using(easing: Easing): E {
        this.easing = easing
        return this
    }
}

/**
 * Base holder class for building a keyframes animation.
 */
sealed class KeyframeBaseEntity<T>(
    internal val value: T,
    internal var easing: Easing
) {
    internal fun <V : AnimationVector> toPair(convertToVector: (T) -> V) =
        convertToVector.invoke(value) to easing
}

/**
 * [KeyframesSpec] creates a [VectorizedKeyframesSpec] animation.
 *
 * [VectorizedKeyframesSpec] animates based on the values defined at different timestamps in
 * the duration of the animation (i.e. different keyframes). Each keyframe can be defined using
 * [KeyframesSpecConfig.at]. [VectorizedKeyframesSpec] allows very specific animation definitions
 * with a precision to millisecond.
 *
 * @sample androidx.compose.animation.core.samples.FloatKeyframesBuilder
 *
 * You can also provide a custom [Easing] for the interval with use of [with] function applied
 * for the interval starting keyframe.
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderWithEasing

 */
@Immutable
class KeyframesSpec<T>(val config: KeyframesSpecConfig<T>) : DurationBasedAnimationSpec<T> {
    /**
     * [KeyframesSpecConfig] stores a mutable configuration of the key frames, including [durationMillis],
     * [delayMillis], and all the key frames. Each key frame defines what the animation value should be
     * at a particular time. Once the key frames are fully configured, the [KeyframesSpecConfig]
     * can be used to create a [KeyframesSpec].
     *
     * @sample androidx.compose.animation.core.samples.KeyframesBuilderForPosition
     * @see keyframes
     */
    class KeyframesSpecConfig<T> : KeyframesSpecBaseConfig<T, KeyframeEntity<T>>() {
        override fun createEntityFor(value: T): KeyframeEntity<T> = KeyframeEntity(value)

        /**
         * Adds an [Easing] for the interval started with the just provided timestamp. For example:
         *     0f at 50 with LinearEasing
         *
         * @sample androidx.compose.animation.core.samples.KeyframesBuilderWithEasing
         * @param easing [Easing] to be used for the next interval.
         * @return the same [KeyframeEntity] instance so that other implementations can expand on
         * the builder pattern
         */
        @Deprecated(
            message = "Use version that returns an instance of the entity so it can be re-used" +
                " in other keyframe builders.",
            replaceWith = ReplaceWith("this using easing") // Expected usage pattern
        )
        infix fun KeyframeEntity<T>.with(easing: Easing) {
            this.easing = easing
        }
    }

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedKeyframesSpec<V> {
        @Suppress("PrimitiveInCollection") // Consumed by stable public API
        val vectorizedKeyframes = mutableMapOf<Int, Pair<V, Easing>>()
        config.keyframes.forEach { key, value ->
            vectorizedKeyframes[key] = value.toPair(converter.convertToVector)
        }
        return VectorizedKeyframesSpec(
            keyframes = vectorizedKeyframes,
            durationMillis = config.durationMillis,
            delayMillis = config.delayMillis
        )
    }

    /**
     * Holder class for building a keyframes animation.
     */
    class KeyframeEntity<T> internal constructor(
        value: T,
        easing: Easing = LinearEasing
    ) : KeyframeBaseEntity<T>(value = value, easing = easing) {

        override fun equals(other: Any?): Boolean {
            return other is KeyframeEntity<*> && other.value == value && other.easing == easing
        }

        override fun hashCode(): Int {
            return value.hashCode() * 31 + easing.hashCode()
        }
    }
}

/**
 * [KeyframesWithSplineSpec] creates a keyframe based [DurationBasedAnimationSpec] using the
 * Monotone cubic Hermite spline to interpolate between the values in [config].
 *
 * [KeyframesWithSplineSpec] is best used with 2D values such as [Offset]. For example:
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForOffsetWithSplines
 *
 * @see keyframesWithSpline
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForIntOffsetWithSplines
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForDpOffsetWithSplines
 */
@ExperimentalAnimationSpecApi
@Immutable
class KeyframesWithSplineSpec<T>(val config: KeyframesWithSplineSpecConfig<T>) :
    DurationBasedAnimationSpec<T> {

    @ExperimentalAnimationSpecApi
    class KeyframesWithSplineSpecConfig<T> :
        KeyframesSpecBaseConfig<T, KeyframesSpec.KeyframeEntity<T>>() {

        override fun createEntityFor(value: T): KeyframesSpec.KeyframeEntity<T> =
            KeyframesSpec.KeyframeEntity(value)
    }

    override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<T, V>):
        VectorizedDurationBasedAnimationSpec<V> {
        // TODO(b/292114811): Finish Easing support, user input is currently ignored
        val timestamps = MutableIntList()
        val timeToVectorMap = MutableIntObjectMap<V>()

        config.keyframes.forEach { key, value ->
            timestamps.add(key)
            timeToVectorMap[key] = converter.convertToVector(value.value)
        }
        timestamps.sort()
        return VectorizedMonoSplineKeyframesSpec(
            timestamps = timestamps,
            keyframes = timeToVectorMap,
            durationMillis = config.durationMillis,
            delayMillis = config.delayMillis
        )
    }
}

/**
 * Creates a [TweenSpec] configured with the given duration, delay and easing curve.
 *
 * @param durationMillis duration of the animation spec
 * @param delayMillis the amount of time in milliseconds that animation waits before starting
 * @param easing the easing curve that will be used to interpolate between start and end
 */
@Stable
fun <T> tween(
    durationMillis: Int = DefaultDurationMillis,
    delayMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing
): TweenSpec<T> = TweenSpec(durationMillis, delayMillis, easing)

/**
 * Creates a [SpringSpec] that uses the given spring constants (i.e. [dampingRatio] and
 * [stiffness]. The optional [visibilityThreshold] defines when the animation
 * should be considered to be visually close enough to round off to its target.
 *
 * @param dampingRatio damping ratio of the spring. [Spring.DampingRatioNoBouncy] by default.
 * @param stiffness stiffness of the spring. [Spring.StiffnessMedium] by default.
 * @param visibilityThreshold optionally specifies the visibility threshold.
 */
@Stable
fun <T> spring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    visibilityThreshold: T? = null
): SpringSpec<T> =
    SpringSpec(dampingRatio, stiffness, visibilityThreshold)

/**
 * Creates a [KeyframesSpec] animation, initialized with [init]. For example:
 *
 * @sample androidx.compose.animation.core.samples.FloatKeyframesBuilderInline
 *
 * Keyframes can also be associated with a particular [Easing] function:
 *
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderWithEasing
 *
 * @param init Initialization function for the [KeyframesSpec] animation
 * @see KeyframesSpec.KeyframesSpecConfig
 */
@Stable
fun <T> keyframes(
    init: KeyframesSpec.KeyframesSpecConfig<T>.() -> Unit
): KeyframesSpec<T> {
    return KeyframesSpec(KeyframesSpec.KeyframesSpecConfig<T>().apply(init))
}

/**
 * Creates a [KeyframesWithSplineSpec] animation, initialized with [init]. For example:
 *
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForOffsetWithSplines
 *
 * @param init Initialization function for the [KeyframesWithSplineSpec] animation
 * @see KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForIntOffsetWithSplines
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForDpOffsetWithSplines
 */
@ExperimentalAnimationSpecApi
@Stable
fun <T> keyframesWithSpline(
    init: KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig<T>.() -> Unit
): KeyframesWithSplineSpec<T> =
    KeyframesWithSplineSpec(
        config = KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig<T>().apply(init)
    )

/**
 * Creates a [RepeatableSpec] that plays a [DurationBasedAnimationSpec] (e.g.
 * [TweenSpec], [KeyframesSpec]) the amount of iterations specified by [iterations].
 *
 * The iteration count describes the amount of times the animation will run.
 * 1 means no repeat. Recommend [infiniteRepeatable] for creating an infinity repeating animation.
 *
 * __Note__: When repeating in the [RepeatMode.Reverse] mode, it's highly recommended to have an
 * __odd__ number of iterations. Otherwise, the animation may jump to the end value when it finishes
 * the last iteration.
 *
 * [initialStartOffset] can be used to either delay the start of the animation or to fast forward
 * the animation to a given play time. This start offset will **not** be repeated, whereas the delay
 * in the [animation] (if any) will be repeated. By default, the amount of offset is 0.
 *
 * @param iterations the total count of iterations, should be greater than 1 to repeat.
 * @param animation animation that will be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *                  [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 */
@Stable
fun <T> repeatable(
    iterations: Int,
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart,
    initialStartOffset: StartOffset = StartOffset(0)
): RepeatableSpec<T> =
    RepeatableSpec(iterations, animation, repeatMode, initialStartOffset)

@Stable
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "This method has been deprecated in favor of the repeatable function that accepts" +
        " start offset."
)
fun <T> repeatable(
    iterations: Int,
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart
) = RepeatableSpec(iterations, animation, repeatMode, StartOffset(0))

/**
 * Creates a [InfiniteRepeatableSpec] that plays a [DurationBasedAnimationSpec] (e.g.
 * [TweenSpec], [KeyframesSpec]) infinite amount of iterations.
 *
 * For non-infinitely repeating animations, consider [repeatable].
 *
 * [initialStartOffset] can be used to either delay the start of the animation or to fast forward
 * the animation to a given play time. This start offset will **not** be repeated, whereas the delay
 * in the [animation] (if any) will be repeated. By default, the amount of offset is 0.
 *
 * @sample androidx.compose.animation.core.samples.InfiniteProgressIndicator
 *
 * @param animation animation that will be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *                  [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 */
@Stable
fun <T> infiniteRepeatable(
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart,
    initialStartOffset: StartOffset = StartOffset(0)
): InfiniteRepeatableSpec<T> =
    InfiniteRepeatableSpec(animation, repeatMode, initialStartOffset)

@Stable
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "This method has been deprecated in favor of the infinite repeatable function that" +
        " accepts start offset."
)
fun <T> infiniteRepeatable(
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart
) = InfiniteRepeatableSpec(animation, repeatMode, StartOffset(0))

/**
 * Creates a Snap animation for immediately switching the animating value to the end value.
 *
 * @param delayMillis the number of milliseconds to wait before the animation runs. 0 by default.
 */
@Stable
fun <T> snap(delayMillis: Int = 0) = SnapSpec<T>(delayMillis)
