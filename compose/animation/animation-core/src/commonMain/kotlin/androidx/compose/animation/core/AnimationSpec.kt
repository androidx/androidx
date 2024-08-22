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

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.collection.MutableIntList
import androidx.collection.MutableIntObjectMap
import androidx.collection.emptyIntObjectMap
import androidx.collection.intListOf
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.ArcMode.Companion.ArcBelow
import androidx.compose.animation.core.ArcMode.Companion.ArcLinear
import androidx.compose.animation.core.KeyframesSpec.KeyframesSpecConfig
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.abs

public object AnimationConstants {
    /** The default duration used in [VectorizedAnimationSpec]s and [AnimationSpec]. */
    public const val DefaultDurationMillis: Int = 300

    /** The value that is used when the animation time is not yet set. */
    public const val UnspecifiedTime: Long = Long.MIN_VALUE
}

/**
 * [AnimationSpec] stores the specification of an animation, including 1) the data type to be
 * animated, and 2) the animation configuration (i.e. [VectorizedAnimationSpec]) that will be used
 * once the data (of type [T]) has been converted to [AnimationVector].
 *
 * Any type [T] can be animated by the system as long as a [TwoWayConverter] is supplied to convert
 * the data type [T] from and to an [AnimationVector]. There are a number of converters available
 * out of the box. For example, to animate [androidx.compose.ui.unit.IntOffset] the system uses
 * [IntOffset.VectorConverter][IntOffset.Companion.VectorConverter] to convert the object to
 * [AnimationVector2D], so that both x and y dimensions are animated independently with separate
 * velocity tracking. This enables multidimensional objects to be animated in a true
 * multi-dimensional way. It is particularly useful for smoothly handling animation interruptions
 * (such as when the target changes during the animation).
 */
public interface AnimationSpec<T> {
    /**
     * Creates a [VectorizedAnimationSpec] with the given [TwoWayConverter].
     *
     * The underlying animation system operates on [AnimationVector]s. [T] will be converted to
     * [AnimationVector] to animate. [VectorizedAnimationSpec] describes how the converted
     * [AnimationVector] should be animated. E.g. The animation could simply interpolate between the
     * start and end values (i.e.[TweenSpec]), or apply spring physics to produce the motion (i.e.
     * [SpringSpec]), etc)
     *
     * @param converter converts the type [T] from and to [AnimationVector] type
     */
    public fun <V : AnimationVector> vectorize(
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
public interface FiniteAnimationSpec<T> : AnimationSpec<T> {
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
public class TweenSpec<T>(
    public val durationMillis: Int = DefaultDurationMillis,
    public val delay: Int = 0,
    public val easing: Easing = FastOutSlowInEasing
) : DurationBasedAnimationSpec<T> {

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedTweenSpec<V> = VectorizedTweenSpec<V>(durationMillis, delay, easing)

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
 * This describes [AnimationSpec]s that are based on a fixed duration, such as [KeyframesSpec],
 * [TweenSpec], and [SnapSpec]. These duration based specs can repeated when put into a
 * [RepeatableSpec].
 */
public interface DurationBasedAnimationSpec<T> : FiniteAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedDurationBasedAnimationSpec<V>
}

/**
 * Creates a [SpringSpec] that uses the given spring constants (i.e. [dampingRatio] and [stiffness].
 * The optional [visibilityThreshold] defines when the animation should be considered to be visually
 * close enough to round off to its target.
 *
 * @param dampingRatio damping ratio of the spring. [Spring.DampingRatioNoBouncy] by default.
 * @param stiffness stiffness of the spring. [Spring.StiffnessMedium] by default.
 * @param visibilityThreshold specifies the visibility threshold
 */
// TODO: annotate damping/stiffness with FloatRange
@Immutable
public class SpringSpec<T>(
    public val dampingRatio: Float = Spring.DampingRatioNoBouncy,
    public val stiffness: Float = Spring.StiffnessMedium,
    public val visibilityThreshold: T? = null
) : FiniteAnimationSpec<T> {

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedSpringSpec<V> =
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
 * [DurationBasedAnimationSpec] that interpolates 2-dimensional values using arcs of quarter of an
 * Ellipse.
 *
 * To interpolate with [keyframes] use [KeyframesSpecConfig.using] with an [ArcMode].
 *
 * &nbsp;
 *
 * As such, it's recommended that [ArcAnimationSpec] is only used for positional values such as:
 * [Offset], [IntOffset] or [androidx.compose.ui.unit.DpOffset].
 *
 * &nbsp;
 *
 * The orientation of the arc is indicated by the given [mode].
 *
 * Do note, that if the target value being animated only changes in one dimension, you'll only be
 * able to get a linear curve.
 *
 * Similarly, one-dimensional values will always only interpolate on a linear curve.
 *
 * @param mode Orientation of the arc.
 * @param durationMillis Duration of the animation. [DefaultDurationMillis] by default.
 * @param delayMillis Time the animation waits before starting. 0 by default.
 * @param easing [Easing] applied on the animation curve. [FastOutSlowInEasing] by default.
 * @sample androidx.compose.animation.core.samples.OffsetArcAnimationSpec
 * @see ArcMode
 * @see keyframes
 */
@ExperimentalAnimationSpecApi
@Immutable
public class ArcAnimationSpec<T>(
    public val mode: ArcMode = ArcBelow,
    public val durationMillis: Int = DefaultDurationMillis,
    public val delayMillis: Int = 0,
    public val easing: Easing = FastOutSlowInEasing // Same default as tween()
) : DurationBasedAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedDurationBasedAnimationSpec<V> =
        VectorizedKeyframesSpec(
            timestamps = intListOf(0, durationMillis),
            keyframes = emptyIntObjectMap(),
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            defaultEasing = easing,
            initialArcMode = mode
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArcAnimationSpec<*>) return false

        if (mode != other.mode) return false
        if (durationMillis != other.durationMillis) return false
        if (delayMillis != other.delayMillis) return false
        return easing == other.easing
    }

    override fun hashCode(): Int {
        var result = mode.hashCode()
        result = 31 * result + durationMillis
        result = 31 * result + delayMillis
        result = 31 * result + easing.hashCode()
        return result
    }
}

/**
 * This class defines the two types of [StartOffset]: [StartOffsetType.Delay] and
 * [StartOffsetType.FastForward]. [StartOffsetType.Delay] delays the start of the animation, whereas
 * [StartOffsetType.FastForward] starts the animation right away from a given play time in the
 * animation.
 *
 * @see repeatable
 * @see infiniteRepeatable
 * @see StartOffset
 */
@kotlin.jvm.JvmInline
public value class StartOffsetType private constructor(internal val value: Int) {
    public companion object {
        /** Delays the start of the animation. */
        public val Delay: StartOffsetType = StartOffsetType(-1)

        /** Fast forwards the animation to a given play time, and starts it immediately. */
        public val FastForward: StartOffsetType = StartOffsetType(1)
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
public value class StartOffset private constructor(internal val value: Long) {
    /**
     * This creates a start offset for [repeatable] and [infiniteRepeatable]. [offsetType] can be
     * either of the following: [StartOffsetType.Delay] and [StartOffsetType.FastForward].
     * [offsetType] defaults to [StartOffsetType.Delay].
     *
     * [StartOffsetType.Delay] delays the start of the animation by [offsetMillis], whereas
     * [StartOffsetType.FastForward] starts the animation right away from [offsetMillis] in the
     * animation.
     */
    public constructor(
        offsetMillis: Int,
        offsetType: StartOffsetType = StartOffsetType.Delay
    ) : this((offsetMillis * offsetType.value).toLong())

    /** Returns the number of milliseconds to offset the start of the animation. */
    public val offsetMillis: Int
        get() = abs(this.value.toInt())

    /** Returns the offset type of the provided [StartOffset]. */
    public val offsetType: StartOffsetType
        get() =
            when (this.value > 0) {
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
 * @param iterations the count of iterations. Should be at least 1.
 * @param animation the [AnimationSpec] to be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *   [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 * @see repeatable
 * @see InfiniteRepeatableSpec
 * @see infiniteRepeatable
 */
@Immutable
public class RepeatableSpec<T>(
    public val iterations: Int,
    public val animation: DurationBasedAnimationSpec<T>,
    public val repeatMode: RepeatMode = RepeatMode.Restart,
    public val initialStartOffset: StartOffset = StartOffset(0)
) : FiniteAnimationSpec<T> {

    @Deprecated(level = DeprecationLevel.HIDDEN, message = "This constructor has been deprecated")
    public constructor(
        iterations: Int,
        animation: DurationBasedAnimationSpec<T>,
        repeatMode: RepeatMode = RepeatMode.Restart
    ) : this(iterations, animation, repeatMode, StartOffset(0))

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V> {
        return VectorizedRepeatableSpec(
            iterations,
            animation.vectorize(converter),
            repeatMode,
            initialStartOffset
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
 * [InfiniteRepeatableSpec] repeats the provided [animation] infinite amount of times. It will never
 * naturally finish. This means the animation will only be stopped via some form of manual
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
 * @param animation the [AnimationSpec] to be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *   [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 * @see infiniteRepeatable
 */
// TODO: Consider supporting repeating spring specs
public class InfiniteRepeatableSpec<T>(
    public val animation: DurationBasedAnimationSpec<T>,
    public val repeatMode: RepeatMode = RepeatMode.Restart,
    public val initialStartOffset: StartOffset = StartOffset(0)
) : AnimationSpec<T> {

    @Deprecated(level = DeprecationLevel.HIDDEN, message = "This constructor has been deprecated")
    public constructor(
        animation: DurationBasedAnimationSpec<T>,
        repeatMode: RepeatMode = RepeatMode.Restart
    ) : this(animation, repeatMode, StartOffset(0))

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedAnimationSpec<V> {
        return VectorizedInfiniteRepeatableSpec(
            animation.vectorize(converter),
            repeatMode,
            initialStartOffset
        )
    }

    override fun equals(other: Any?): Boolean =
        if (other is InfiniteRepeatableSpec<*>) {
            other.animation == this.animation &&
                other.repeatMode == this.repeatMode &&
                other.initialStartOffset == this.initialStartOffset
        } else {
            false
        }

    override fun hashCode(): Int {
        return (animation.hashCode() * 31 + repeatMode.hashCode()) * 31 +
            initialStartOffset.hashCode()
    }
}

/** Repeat mode for [RepeatableSpec] and [VectorizedRepeatableSpec]. */
public enum class RepeatMode {
    /** [Restart] will restart the animation and animate from the start value to the end value. */
    Restart,

    /** [Reverse] will reverse the last iteration as the animation repeats. */
    Reverse
}

/**
 * [SnapSpec] describes a jump-cut type of animation. It immediately snaps the animating value to
 * the end value.
 *
 * @param delay the amount of time (in milliseconds) that the animation should wait before it
 *   starts. Defaults to 0.
 */
@Immutable
public class SnapSpec<T>(public val delay: Int = 0) : DurationBasedAnimationSpec<T> {
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

/** Shared configuration class used as DSL for keyframe based animations. */
public sealed class KeyframesSpecBaseConfig<T, E : KeyframeBaseEntity<T>> {
    /**
     * Duration of the animation in milliseconds. The minimum is `0` and defaults to
     * [DefaultDurationMillis]
     */
    @get:IntRange(from = 0L)
    @setparam:IntRange(from = 0L)
    public var durationMillis: Int = DefaultDurationMillis

    /**
     * The amount of time that the animation should be delayed. The minimum is `0` and defaults
     * to 0.
     */
    @get:IntRange(from = 0L) @setparam:IntRange(from = 0L) public var delayMillis: Int = 0

    internal val keyframes = mutableIntObjectMapOf<E>()

    /** Method used to delegate instantiation of [E] to implementing classes. */
    internal abstract fun createEntityFor(value: T): E

    /**
     * Adds a keyframe so that animation value will be [this] at time: [timeStamp]. For example:
     *
     * @sample androidx.compose.animation.core.samples.floatAtSample
     * @param timeStamp The time in the during when animation should reach value: [this], with a
     *   minimum value of `0`.
     * @return an instance of [E] so a custom [Easing] can be added by the [using] method.
     */
    // needed as `open` to guarantee binary compatibility in KeyframesSpecConfig
    public open infix fun T.at(@IntRange(from = 0) timeStamp: Int): E {
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
     * @param fraction The fraction when the animation should reach specified value.
     * @return an instance of [E] so a custom [Easing] can be added by the [using] method
     */
    // needed as `open` to guarantee binary compatibility in KeyframesSpecConfig
    public open infix fun T.atFraction(@FloatRange(from = 0.0, to = 1.0) fraction: Float): E {
        return at((durationMillis * fraction).fastRoundToInt())
    }

    /**
     * Adds an [Easing] for the interval started with the just provided timestamp. For example: 0f
     * at 50 using LinearEasing
     *
     * @sample androidx.compose.animation.core.samples.KeyframesBuilderWithEasing
     * @param easing [Easing] to be used for the next interval.
     * @return the same [E] instance so that other implementations can expand on the builder pattern
     */
    public infix fun E.using(easing: Easing): E {
        this.easing = easing
        return this
    }
}

/** Base holder class for building a keyframes animation. */
public sealed class KeyframeBaseEntity<T>(internal val value: T, internal var easing: Easing) {
    internal fun <V : AnimationVector> toPair(convertToVector: (T) -> V) =
        convertToVector.invoke(value) to easing
}

/**
 * [KeyframesSpec] creates a [VectorizedKeyframesSpec] animation.
 *
 * [VectorizedKeyframesSpec] animates based on the values defined at different timestamps in the
 * duration of the animation (i.e. different keyframes). Each keyframe can be defined using
 * [KeyframesSpecConfig.at]. [VectorizedKeyframesSpec] allows very specific animation definitions
 * with a precision to millisecond.
 *
 * @sample androidx.compose.animation.core.samples.FloatKeyframesBuilder
 *
 * For each interval, you may provide a custom [Easing] by use of the [KeyframesSpecConfig.using]
 * function.
 *
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderWithEasing
 *
 * By default, values are animated linearly from one interval to the next (similar to [tween]),
 * however for 2-dimensional values you may animate them using arcs of quarter of an Ellipse with
 * [KeyframesSpecConfig.using] and [ArcMode]:
 *
 * @sample androidx.compose.animation.core.samples.OffsetKeyframesWithArcsBuilder
 *
 * If instead, you wish to have a smooth curvy animation across all intervals, consider using
 * [KeyframesWithSplineSpec].
 */
@Immutable
public class KeyframesSpec<T>(public val config: KeyframesSpecConfig<T>) :
    DurationBasedAnimationSpec<T> {
    /**
     * [KeyframesSpecConfig] stores a mutable configuration of the key frames, including
     * [durationMillis], [delayMillis], and all the key frames. Each key frame defines what the
     * animation value should be at a particular time. Once the key frames are fully configured, the
     * [KeyframesSpecConfig] can be used to create a [KeyframesSpec].
     *
     * @sample androidx.compose.animation.core.samples.KeyframesBuilderForPosition
     * @see keyframes
     */
    public class KeyframesSpecConfig<T> : KeyframesSpecBaseConfig<T, KeyframeEntity<T>>() {
        @OptIn(ExperimentalAnimationSpecApi::class)
        override fun createEntityFor(value: T): KeyframeEntity<T> = KeyframeEntity(value)

        /**
         * Adds a keyframe so that animation value will be [this] at time: [timeStamp]. For example:
         * 0.8f at 150 // ms
         *
         * @param timeStamp The time in the during when animation should reach value: [this], with a
         *   minimum value of `0`.
         * @return an [KeyframeEntity] so a custom [Easing] can be added by [with] method.
         */
        // TODO: Need a IntRange equivalent annotation
        // overrides `at` for binary compatibility. It should explicitly return KeyframeEntity.
        override infix fun T.at(@IntRange(from = 0) timeStamp: Int): KeyframeEntity<T> {
            @OptIn(ExperimentalAnimationSpecApi::class)
            return KeyframeEntity(this).also { keyframes[timeStamp] = it }
        }

        /**
         * Adds a keyframe so that the animation value will be the value specified at a fraction of
         * the total [durationMillis] set. For example: 0.8f atFraction 0.50f // half of the overall
         * duration set
         *
         * @param fraction The fraction when the animation should reach specified value.
         * @return an [KeyframeEntity] so a custom [Easing] can be added by [with] method
         */
        // overrides `atFraction` for binary compatibility. It should explicitly return
        // KeyframeEntity.
        override infix fun T.atFraction(
            @FloatRange(from = 0.0, to = 1.0) fraction: Float
        ): KeyframeEntity<T> {
            return at((durationMillis * fraction).fastRoundToInt())
        }

        /**
         * Adds an [Easing] for the interval started with the just provided timestamp. For example:
         * 0f at 50 with LinearEasing
         *
         * @sample androidx.compose.animation.core.samples.KeyframesBuilderWithEasing
         * @param easing [Easing] to be used for the next interval.
         * @return the same [KeyframeEntity] instance so that other implementations can expand on
         *   the builder pattern
         */
        @Deprecated(
            message =
                "Use version that returns an instance of the entity so it can be re-used" +
                    " in other keyframe builders.",
            replaceWith = ReplaceWith("this using easing") // Expected usage pattern
        )
        public infix fun KeyframeEntity<T>.with(easing: Easing) {
            this.easing = easing
        }

        /**
         * [ArcMode] applied from this keyframe to the next.
         *
         * Note that arc modes are meant for objects with even dimensions (such as [Offset] and its
         * variants). Where each value pair is animated as an arc. So, if the object has odd
         * dimensions the last value will always animate linearly.
         *
         * &nbsp;
         *
         * The order of each value in an object with multiple dimensions is given by the applied
         * vector converter in [KeyframesSpec.vectorize].
         *
         * E.g.: [RectToVector] assigns its values as `[left, top, right, bottom]` so the pairs of
         * dimensions animated as arcs are: `[left, top]` and `[right, bottom]`.
         */
        public infix fun KeyframeEntity<T>.using(arcMode: ArcMode): KeyframeEntity<T> {
            this.arcMode = arcMode
            return this
        }
    }

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedKeyframesSpec<V> {
        // Max capacity is +2 to account for when the start/end timestamps are not included
        val timestamps = MutableIntList(config.keyframes.size + 2)
        val timeToInfoMap =
            MutableIntObjectMap<VectorizedKeyframeSpecElementInfo<V>>(config.keyframes.size)
        config.keyframes.forEach { key, value ->
            timestamps.add(key)
            timeToInfoMap[key] =
                VectorizedKeyframeSpecElementInfo(
                    vectorValue = converter.convertToVector(value.value),
                    easing = value.easing,
                    arcMode = value.arcMode
                )
        }

        if (!config.keyframes.contains(0)) {
            timestamps.add(0, 0)
        }
        if (!config.keyframes.contains(config.durationMillis)) {
            timestamps.add(config.durationMillis)
        }
        timestamps.sort()

        return VectorizedKeyframesSpec(
            timestamps = timestamps,
            keyframes = timeToInfoMap,
            durationMillis = config.durationMillis,
            delayMillis = config.delayMillis,
            defaultEasing = LinearEasing,
            initialArcMode = ArcLinear
        )
    }

    /** Holder class for building a keyframes animation. */
    public class KeyframeEntity<T>
    internal constructor(
        value: T,
        easing: Easing = LinearEasing,
        internal var arcMode: ArcMode = ArcMode.ArcLinear
    ) : KeyframeBaseEntity<T>(value = value, easing = easing) {

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is KeyframeEntity<*>) return false

            return other.value == value && other.easing == easing && other.arcMode == arcMode
        }

        override fun hashCode(): Int {
            var result = value?.hashCode() ?: 0
            result = 31 * result + arcMode.hashCode()
            result = 31 * result + easing.hashCode()
            return result
        }
    }
}

/**
 * [KeyframesWithSplineSpec] creates a keyframe based [DurationBasedAnimationSpec] using the
 * Monotone cubic Hermite spline to interpolate between the values in [config].
 *
 * [KeyframesWithSplineSpec] may be used to animate any n-dimensional values, but you'll likely use
 * it most to animate positional 2D values such as [Offset]. For example:
 *
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForOffsetWithSplines
 *
 * You may also provide a [periodicBias] value (between 0f and 1f) to make a periodic spline.
 * Periodic splines adjust the initial and final velocity to be the same. This is useful to create
 * smooth repeatable animations. Such as an infinite pulsating animation:
 *
 * @sample androidx.compose.animation.core.samples.PeriodicKeyframesWithSplines
 *
 * The [periodicBias] value (from 0.0 to 1.0) indicates how much of the original starting and final
 * velocity are modified to achieve periodicity:
 * - 0f: Modifies only the starting velocity to match the final velocity
 * - 1f: Modifies only the final velocity to match the starting velocity
 * - 0.5f: Modifies both velocities equally, picking the average between the two
 *
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForIntOffsetWithSplines
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForDpOffsetWithSplines
 * @see keyframesWithSpline
 */
@Immutable
public class KeyframesWithSplineSpec<T>(
    public val config: KeyframesWithSplineSpecConfig<T>,
) : DurationBasedAnimationSpec<T> {
    // Periodic bias property, NaN by default. Only meant to be set by secondary constructor
    private var periodicBias: Float = Float.NaN

    /**
     * Constructor that returns a periodic spline implementation.
     *
     * @param config Keyframe configuration of the spline, should contain the set of values,
     *   timestamps and easing curves to animate through.
     * @param periodicBias A value from 0f to 1f, indicating how much the starting or ending
     *   velocities are modified respectively to achieve periodicity.
     */
    public constructor(
        config: KeyframesWithSplineSpecConfig<T>,
        @FloatRange(0.0, 1.0) periodicBias: Float
    ) : this(config) {
        this.periodicBias = periodicBias
    }

    /**
     * Keyframe configuration class for [KeyframesWithSplineSpec].
     *
     * Since [keyframesWithSpline] uses the values across all the given intervals to calculate the
     * shape of the animation, [KeyframesWithSplineSpecConfig] does not allow setting a specific
     * [ArcMode] between intervals (compared to [KeyframesSpecConfig] used for [keyframes]).
     */
    public class KeyframesWithSplineSpecConfig<T> :
        KeyframesSpecBaseConfig<T, KeyframesSpec.KeyframeEntity<T>>() {

        override fun createEntityFor(value: T): KeyframesSpec.KeyframeEntity<T> =
            KeyframesSpec.KeyframeEntity(value)
    }

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedDurationBasedAnimationSpec<V> {
        // Allocate so that we don't resize the list even if the initial/last timestamps are missing
        val keyframes = config.keyframes
        val timestamps = MutableIntList(keyframes.size + 2)
        val timeToVectorMap = MutableIntObjectMap<Pair<V, Easing>>(keyframes.size)
        keyframes.forEach { key, value ->
            timestamps.add(key)
            timeToVectorMap[key] = Pair(converter.convertToVector(value.value), value.easing)
        }
        if (!keyframes.contains(0)) {
            timestamps.add(0, 0)
        }
        if (!keyframes.contains(config.durationMillis)) {
            timestamps.add(config.durationMillis)
        }
        timestamps.sort()
        return VectorizedMonoSplineKeyframesSpec(
            timestamps = timestamps,
            keyframes = timeToVectorMap,
            durationMillis = config.durationMillis,
            delayMillis = config.delayMillis,
            periodicBias = periodicBias
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
public fun <T> tween(
    durationMillis: Int = DefaultDurationMillis,
    delayMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing
): TweenSpec<T> = TweenSpec(durationMillis, delayMillis, easing)

/**
 * Creates a [SpringSpec] that uses the given spring constants (i.e. [dampingRatio] and [stiffness].
 * The optional [visibilityThreshold] defines when the animation should be considered to be visually
 * close enough to round off to its target.
 *
 * @param dampingRatio damping ratio of the spring. [Spring.DampingRatioNoBouncy] by default.
 * @param stiffness stiffness of the spring. [Spring.StiffnessMedium] by default.
 * @param visibilityThreshold optionally specifies the visibility threshold.
 */
@Stable
public fun <T> spring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    visibilityThreshold: T? = null
): SpringSpec<T> = SpringSpec(dampingRatio, stiffness, visibilityThreshold)

/**
 * Creates a [KeyframesSpec] animation, initialized with [init]. For example:
 *
 * @sample androidx.compose.animation.core.samples.FloatKeyframesBuilderInline
 *
 * Keyframes can also be associated with a particular [Easing] function:
 *
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderWithEasing
 *
 * Values can be animated using arcs of quarter of an Ellipse with [KeyframesSpecConfig.using] and
 * [ArcMode]:
 *
 * @sample androidx.compose.animation.core.samples.OffsetKeyframesWithArcsBuilder
 *
 * For a smooth, curvy animation across all the intervals in the keyframes, consider using
 * [keyframesWithSpline] instead.
 *
 * @param init Initialization function for the [KeyframesSpec] animation
 * @see KeyframesSpec.KeyframesSpecConfig
 */
@Stable
public fun <T> keyframes(init: KeyframesSpecConfig<T>.() -> Unit): KeyframesSpec<T> {
    return KeyframesSpec(KeyframesSpecConfig<T>().apply(init))
}

/**
 * Creates a [KeyframesWithSplineSpec] animation, initialized with [init].
 *
 * For more details on implementation, see [KeyframesWithSplineSpec].
 *
 * Use overload that takes a [Float] parameter to use periodic splines.
 *
 * Example:
 *
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForOffsetWithSplines
 * @param init Initialization function for the [KeyframesWithSplineSpec] animation
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForIntOffsetWithSplines
 * @sample androidx.compose.animation.core.samples.KeyframesBuilderForDpOffsetWithSplines
 * @see KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig
 */
public fun <T> keyframesWithSpline(
    init: KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig<T>.() -> Unit
): KeyframesWithSplineSpec<T> =
    KeyframesWithSplineSpec(
        config = KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig<T>().apply(init)
    )

/**
 * Creates a *periodic* [KeyframesWithSplineSpec] animation, initialized with [init].
 *
 * Use overload without [periodicBias] parameter for the non-periodic implementation.
 *
 * A periodic spline is one such that the starting and ending velocities are equal. This makes them
 * useful to crete smooth repeatable animations. Such as an infinite pulsating animation:
 *
 * @sample androidx.compose.animation.core.samples.PeriodicKeyframesWithSplines
 *
 * The [periodicBias] value (from 0.0 to 1.0) indicates how much of the original starting and final
 * velocity are modified to achieve periodicity:
 * - 0f: Modifies only the starting velocity to match the final velocity
 * - 1f: Modifies only the final velocity to match the starting velocity
 * - 0.5f: Modifies both velocities equally, picking the average between the two
 *
 * @param periodicBias A value from 0f to 1f, indicating how much the starting or ending velocities
 *   are modified respectively to achieve periodicity.
 * @param init Initialization function for the [KeyframesWithSplineSpec] animation
 * @see KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig
 */
public fun <T> keyframesWithSpline(
    @FloatRange(0.0, 1.0) periodicBias: Float,
    init: KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig<T>.() -> Unit
): KeyframesWithSplineSpec<T> =
    KeyframesWithSplineSpec(
        config = KeyframesWithSplineSpec.KeyframesWithSplineSpecConfig<T>().apply(init),
        periodicBias = periodicBias,
    )

/**
 * Creates a [RepeatableSpec] that plays a [DurationBasedAnimationSpec] (e.g. [TweenSpec],
 * [KeyframesSpec]) the amount of iterations specified by [iterations].
 *
 * The iteration count describes the amount of times the animation will run. 1 means no repeat.
 * Recommend [infiniteRepeatable] for creating an infinity repeating animation.
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
 *   [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 */
@Stable
public fun <T> repeatable(
    iterations: Int,
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart,
    initialStartOffset: StartOffset = StartOffset(0)
): RepeatableSpec<T> = RepeatableSpec(iterations, animation, repeatMode, initialStartOffset)

@Stable
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message =
        "This method has been deprecated in favor of the repeatable function that accepts" +
            " start offset."
)
public fun <T> repeatable(
    iterations: Int,
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart
): RepeatableSpec<T> = RepeatableSpec(iterations, animation, repeatMode, StartOffset(0))

/**
 * Creates a [InfiniteRepeatableSpec] that plays a [DurationBasedAnimationSpec] (e.g. [TweenSpec],
 * [KeyframesSpec]) infinite amount of iterations.
 *
 * For non-infinitely repeating animations, consider [repeatable].
 *
 * [initialStartOffset] can be used to either delay the start of the animation or to fast forward
 * the animation to a given play time. This start offset will **not** be repeated, whereas the delay
 * in the [animation] (if any) will be repeated. By default, the amount of offset is 0.
 *
 * @sample androidx.compose.animation.core.samples.InfiniteProgressIndicator
 * @param animation animation that will be repeated
 * @param repeatMode whether animation should repeat by starting from the beginning (i.e.
 *   [RepeatMode.Restart]) or from the end (i.e. [RepeatMode.Reverse])
 * @param initialStartOffset offsets the start of the animation
 */
@Stable
public fun <T> infiniteRepeatable(
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart,
    initialStartOffset: StartOffset = StartOffset(0)
): InfiniteRepeatableSpec<T> = InfiniteRepeatableSpec(animation, repeatMode, initialStartOffset)

@Stable
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message =
        "This method has been deprecated in favor of the infinite repeatable function that" +
            " accepts start offset."
)
public fun <T> infiniteRepeatable(
    animation: DurationBasedAnimationSpec<T>,
    repeatMode: RepeatMode = RepeatMode.Restart
): InfiniteRepeatableSpec<T> = InfiniteRepeatableSpec(animation, repeatMode, StartOffset(0))

/**
 * Creates a Snap animation for immediately switching the animating value to the end value.
 *
 * @param delayMillis the number of milliseconds to wait before the animation runs. 0 by default.
 */
@Stable public fun <T> snap(delayMillis: Int = 0): SnapSpec<T> = SnapSpec<T>(delayMillis)

/**
 * Returns an [AnimationSpec] that is the same as [animationSpec] with a delay of [startDelayNanos].
 */
@Stable
internal fun <T> delayed(animationSpec: AnimationSpec<T>, startDelayNanos: Long): AnimationSpec<T> =
    StartDelayAnimationSpec(animationSpec, startDelayNanos)

/**
 * A [VectorizedAnimationSpec] that wraps [vectorizedAnimationSpec], giving it a start delay of
 * [startDelayNanos].
 */
@Immutable
private class StartDelayVectorizedAnimationSpec<V : AnimationVector>(
    val vectorizedAnimationSpec: VectorizedAnimationSpec<V>,
    val startDelayNanos: Long
) : VectorizedAnimationSpec<V> {
    override val isInfinite: Boolean
        get() = vectorizedAnimationSpec.isInfinite

    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long =
        vectorizedAnimationSpec.getDurationNanos(
            initialValue = initialValue,
            targetValue = targetValue,
            initialVelocity = initialVelocity
        ) + startDelayNanos

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V =
        if (playTimeNanos < startDelayNanos) {
            initialVelocity
        } else {
            vectorizedAnimationSpec.getVelocityFromNanos(
                playTimeNanos = playTimeNanos - startDelayNanos,
                initialValue = initialValue,
                targetValue = targetValue,
                initialVelocity = initialVelocity
            )
        }

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V =
        if (playTimeNanos < startDelayNanos) {
            initialValue
        } else {
            vectorizedAnimationSpec.getValueFromNanos(
                playTimeNanos = playTimeNanos - startDelayNanos,
                initialValue = initialValue,
                targetValue = targetValue,
                initialVelocity = initialVelocity
            )
        }

    override fun hashCode(): Int {
        return 31 * vectorizedAnimationSpec.hashCode() + startDelayNanos.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is StartDelayVectorizedAnimationSpec<*>) {
            return false
        }
        return other.startDelayNanos == startDelayNanos &&
            other.vectorizedAnimationSpec == vectorizedAnimationSpec
    }
}

/** An [AnimationSpec] that wraps [animationSpec], giving it a start delay of [startDelayNanos]. */
@Immutable
private class StartDelayAnimationSpec<T>(
    val animationSpec: AnimationSpec<T>,
    val startDelayNanos: Long
) : AnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedAnimationSpec<V> {
        val vecSpec = animationSpec.vectorize(converter)
        return StartDelayVectorizedAnimationSpec(vecSpec, startDelayNanos)
    }

    override fun hashCode(): Int {
        return 31 * animationSpec.hashCode() + startDelayNanos.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is StartDelayAnimationSpec<*>) {
            return false
        }
        return other.startDelayNanos == startDelayNanos && other.animationSpec == animationSpec
    }
}
