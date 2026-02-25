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

package androidx.ink.brush

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.ink.geometry.AngleDegreesFloat
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.util.Collections.unmodifiableList
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic

/**
 * A [BrushTip] consists of parameters that control how stroke inputs are used to model the tip
 * shape and color, and create vertices for the stroke mesh.
 *
 * The specification can be considered in two parts:
 * 1. Parameters for the base shape of the tip as a function of [Brush] size.
 * 2. An array of [BrushBehavior]s that allow dynamic properties of each input to augment the tip
 *    shape and color.
 *
 * Depending on the combination of values, the tip can be shaped as a rounded parallelogram, circle,
 * or stadium. Through [BrushBehavior]s, the tip can produce a per-vertex HSLA color shift that can
 * be used to augment the [Brush] color when drawing. The default values below produce a static
 * circular tip shape with diameter equal to the [Brush] size and no color shift.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalInkCustomBrushApi
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class BrushTip
private constructor(
    /** A handle to the underlying native [BrushTip] object. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long,
    behaviors: List<BrushBehavior>,
) {
    /**
     * A list of [BrushBehavior]s that allow dynamic properties of each input to augment the tip
     * shape and color.
     */
    public val behaviors: List<BrushBehavior> = unmodifiableList(behaviors.toList())

    public constructor(
        @FloatRange(from = 0.0, toInclusive = false) scaleX: Float = 1f,
        @FloatRange(from = 0.0, toInclusive = false) scaleY: Float = 1f,
        @FloatRange(from = 0.0, to = 1.0) cornerRounding: Float = 1f,
        @FloatRange(from = -90.0, to = 90.0) @AngleDegreesFloat slantDegrees: Float = 0f,
        @FloatRange(from = 0.0, to = 1.0) pinch: Float = 0f,
        @AngleDegreesFloat rotationDegrees: Float = 0f,
        @FloatRange(from = 0.0, toInclusive = false) particleGapDistanceScale: Float = 0f,
        @IntRange(from = 0L) particleGapDurationMillis: Long = 0L,
        behaviors: List<BrushBehavior> = emptyList(),
    ) : this(
        BrushTipNative.create(
            scaleX,
            scaleY,
            cornerRounding,
            slantDegrees,
            pinch,
            rotationDegrees,
            particleGapDistanceScale,
            particleGapDurationMillis,
            behaviors.map { it.nativePointer }.toLongArray(),
        ),
        behaviors,
    )

    /**
     * 2D scale used to calculate the initial width and height of the tip shape relative to the
     * brush size prior to applying [slant] and [rotation].
     *
     * The base width and height of the tip will be equal to the brush size multiplied by [scaleX]
     * and [scaleY] respectively. Valid values must be finite and non-negative, with at least one
     * value greater than zero.
     */
    @get:FloatRange(from = 0.0, toInclusive = false)
    public val scaleX: Float
        get() = BrushTipNative.getScaleX(nativePointer)

    /**
     * 2D scale used to calculate the initial width and height of the tip shape relative to the
     * brush size prior to applying [slant] and [rotation].
     *
     * The base width and height of the tip will be equal to the brush size multiplied by [scaleX]
     * and [scaleY] respectively. Valid values must be finite and non-negative, with at least one
     * value greater than zero.
     */
    @get:FloatRange(from = 0.0, toInclusive = false)
    public val scaleY: Float
        get() = BrushTipNative.getScaleY(nativePointer)

    /**
     * A normalized value in the range [0, 1] that is used to calculate the initial radius of
     * curvature for the tip's corners. A value of 0 results in sharp corners and a value of 1
     * results in the maximum radius of curvature given the current tip dimensions.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    public val cornerRounding: Float
        get() = BrushTipNative.getCornerRounding(nativePointer)

    /**
     * Angle in degrees used to calculate the initial slant of the tip shape prior to applying
     * [rotation].
     *
     * The value should be in the range [-90, 90] degrees, and represents the angle by which
     * "vertical" lines of the tip shape will appear rotated about their intersection with the
     * x-axis. A positive value will rotate from the positive x-axis towards the positive y-axis.
     *
     * More info: This property is similar to the single-arg CSS skew() transformation. Unlike skew,
     * slant tries to preserve the perimeter of the tip shape as opposed to its area. This is akin
     * to "pressing" a rectangle into a parallelogram with non-right angles while preserving the
     * side lengths.
     */
    @get:FloatRange(from = -90.0, to = 90.0)
    @get:AngleDegreesFloat
    public val slantDegrees: Float
        get() = BrushTipNative.getSlantDegrees(nativePointer)

    /**
     * A unitless parameter in the range [0, 1] that controls the separation between two of the
     * shape's corners prior to applying [rotation].
     *
     * The two corners affected lie toward the negative y-axis relative to the center of the tip
     * shape. I.e. the "upper edge" of the shape if positive y is chosen to point "down" in stroke
     * coordinates.
     *
     * If [scaleX] is not 0, different values of [pinch] produce the following shapes: A value of 0
     * will leave the corners unaffected as a rectangle or parallelogram. Values between 0 and 1
     * will bring the corners closer together to result in a (possibly slanted) trapezoidal shape. A
     * value of 1 will make the two corners coincide and result in a triangular shape.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    public val pinch: Float
        get() = BrushTipNative.getPinch(nativePointer)

    /**
     * Angle in degrees specifying the initial rotation of the tip shape after applying [scaleX],
     * [scaleY], [pinch], and [slantDegrees]. The rotation is in the direction from the positive
     * x-axis towards the positive y-axis.
     */
    @get:AngleDegreesFloat
    public val rotationDegrees: Float
        get() = BrushTipNative.getRotationDegrees(nativePointer)

    /**
     * Parameter controlling emission of particles as a function of distance traveled by the stroke
     * inputs.
     *
     * When this and [particleGapDurationMillis] are both zero, the stroke will be continuous,
     * unless gaps are introduced dynamically by [BrushBehavior]s. Otherwise, the stroke will be
     * made up of particles. A new particle will be emitted after at least
     * [particleGapDistanceScale] * [Brush.size] distance has been traveled by the stoke inputs.
     */
    @get:FloatRange(from = 0.0, toInclusive = false)
    public val particleGapDistanceScale: Float
        get() = BrushTipNative.getParticleGapDistanceScale(nativePointer)

    /**
     * Parameter controlling emission of particles as a function of time elapsed along the stroke.
     *
     * When this and [particleGapDistanceScale] are both zero, the stroke will be continuous, unless
     * gaps are introduced dynamically by `BrushBehavior`s. Otherwise, the stroke will be made up of
     * particles. Particles will be emitted at most once every [particleGapDurationMillis].
     */
    @get:IntRange(from = 0L)
    public val particleGapDurationMillis: Long
        get() = BrushTipNative.getParticleGapDurationMillis(nativePointer)

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @JvmSynthetic
    @Suppress("Deprecation") // Still considers deprecated opacityMultiplier.
    public fun copy(
        @FloatRange(from = 0.0, toInclusive = false) scaleX: Float = this.scaleX,
        @FloatRange(from = 0.0, toInclusive = false) scaleY: Float = this.scaleY,
        @FloatRange(from = 0.0, to = 1.0) cornerRounding: Float = this.cornerRounding,
        @FloatRange(from = -90.0, to = 90.0)
        @AngleDegreesFloat
        slantDegrees: Float = this.slantDegrees,
        @FloatRange(from = 0.0, to = 1.0) pinch: Float = this.pinch,
        @AngleDegreesFloat rotationDegrees: Float = this.rotationDegrees,
        @FloatRange(from = 0.0, toInclusive = false)
        particleGapDistanceScale: Float = this.particleGapDistanceScale,
        @IntRange(from = 0L) particleGapDurationMillis: Long = this.particleGapDurationMillis,
        behaviors: List<BrushBehavior> = this.behaviors,
    ): BrushTip =
        BrushTip(
            scaleX,
            scaleY,
            cornerRounding,
            slantDegrees,
            pinch,
            rotationDegrees,
            particleGapDistanceScale,
            particleGapDurationMillis,
            behaviors,
        )

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a BrushTip. Kotlin developers, see [copy] method.
     */
    @Suppress("Deprecation") // Still considers deprecated opacityMultiplier.
    public fun toBuilder(): Builder =
        Builder()
            .setScaleX(scaleX)
            .setScaleY(scaleY)
            .setCornerRounding(cornerRounding)
            .setSlantDegrees(slantDegrees)
            .setPinch(pinch)
            .setRotationDegrees(rotationDegrees)
            .setParticleGapDistanceScale(particleGapDistanceScale)
            .setParticleGapDurationMillis(particleGapDurationMillis)
            .setBehaviors(behaviors)

    /**
     * Builder for [BrushTip].
     *
     * Use BrushTip.Builder to construct a [BrushTip] with default values, overriding only as
     * needed.
     */
    @Suppress("ScopeReceiverThis")
    public class Builder {
        private var scaleX: Float = 1f
        private var scaleY: Float = 1f
        private var cornerRounding: Float = 1f
        @AngleDegreesFloat private var slantDegrees: Float = 0f
        private var pinch: Float = 0f
        @AngleDegreesFloat private var rotationDegrees: Float = 0f
        private var particleGapDistanceScale: Float = 0F
        private var particleGapDurationMillis: Long = 0L
        private var behaviors: List<BrushBehavior> = emptyList()

        public fun setScaleX(@FloatRange(from = 0.0, toInclusive = false) scaleX: Float): Builder =
            apply {
                this.scaleX = scaleX
            }

        public fun setScaleY(@FloatRange(from = 0.0, toInclusive = false) scaleY: Float): Builder =
            apply {
                this.scaleY = scaleY
            }

        public fun setCornerRounding(
            @FloatRange(from = 0.0, to = 1.0) cornerRounding: Float
        ): Builder = apply { this.cornerRounding = cornerRounding }

        public fun setSlantDegrees(
            @FloatRange(from = -90.0, to = 90.0) @AngleDegreesFloat degrees: Float
        ): Builder = apply { slantDegrees = degrees }

        public fun setPinch(@FloatRange(from = 0.0, to = 1.0) pinch: Float): Builder = apply {
            this.pinch = pinch
        }

        public fun setRotationDegrees(@AngleDegreesFloat degrees: Float): Builder = apply {
            rotationDegrees = degrees
        }

        public fun setParticleGapDistanceScale(
            @FloatRange(from = 0.0, toInclusive = false) particleGapDistanceScale: Float
        ): Builder = apply { this.particleGapDistanceScale = particleGapDistanceScale }

        public fun setParticleGapDurationMillis(
            @IntRange(from = 0L) particleGapDurationMillis: Long
        ): Builder = apply { this.particleGapDurationMillis = particleGapDurationMillis }

        public fun setBehaviors(behaviors: List<BrushBehavior>): Builder = apply {
            this.behaviors = behaviors.toList()
        }

        public fun build(): BrushTip =
            BrushTip(
                scaleX,
                scaleY,
                cornerRounding,
                slantDegrees,
                pinch,
                rotationDegrees,
                particleGapDistanceScale,
                particleGapDurationMillis,
                behaviors,
            )
    }

    @Suppress("Deprecation") // Still considers deprecated opacityMultiplier.
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushTip) return false
        return scaleY == other.scaleY &&
            scaleX == other.scaleX &&
            pinch == other.pinch &&
            cornerRounding == other.cornerRounding &&
            slantDegrees == other.slantDegrees &&
            rotationDegrees == other.rotationDegrees &&
            particleGapDistanceScale == other.particleGapDistanceScale &&
            particleGapDurationMillis == other.particleGapDurationMillis &&
            behaviors == other.behaviors
    }

    @Suppress("Deprecation") // Still considers deprecated opacityMultiplier.
    override fun hashCode(): Int {
        var result = scaleX.hashCode()
        result = 31 * result + scaleY.hashCode()
        result = 31 * result + pinch.hashCode()
        result = 31 * result + cornerRounding.hashCode()
        result = 31 * result + slantDegrees.hashCode()
        result = 31 * result + rotationDegrees.hashCode()
        result = 31 * result + particleGapDistanceScale.hashCode()
        result = 31 * result + particleGapDurationMillis.hashCode()
        result = 31 * result + behaviors.hashCode()
        return result
    }

    @Suppress("Deprecation") // Still outputs deprecated opacityMultiplier.
    override fun toString(): String =
        "BrushTip(scale=($scaleX, $scaleY), cornerRounding=$cornerRounding," +
            " slantDegrees=$slantDegrees, pinch=$pinch, rotationDegrees=$rotationDegrees," +
            " particleGapDistanceScale=$particleGapDistanceScale," +
            " particleGapDurationMillis=$particleGapDurationMillis, behaviors=$behaviors)"

    /** Delete native BrushTip memory. */
    // NOMUTANTS -- Not tested post garbage collection.
    protected fun finalize() {
        // Note that the instance becomes finalizable at the conclusion of the Object constructor,
        // which
        // in Kotlin is always before any non-default field initialization has been done by a
        // derived
        // class constructor.
        if (nativePointer == 0L) return
        BrushTipNative.free(nativePointer)
    }

    // Companion object gets initialized before anything else.
    public companion object {
        /** Returns a new [BrushTip.Builder]. */
        @JvmStatic public fun builder(): Builder = Builder()

        /**
         * Construct a [BrushTip] from an unowned heap-allocated native pointer to a C++ `BrushTip`.
         *
         * Some objects from legacy deserialization are passed in as parameters while code further
         * down the stack is migrated to use new deserialization.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long): BrushTip =
            BrushTip(
                unownedNativePointer,
                (0 until BrushTipNative.getBehaviorCount(unownedNativePointer)).map { index ->
                    BrushBehavior.wrapNative(
                        BrushTipNative.newCopyOfBrushBehavior(unownedNativePointer, index)
                    )
                },
            )
    }
}

/** Singleton wrapper around native JNI calls. */
@UsedByNative
private object BrushTipNative {
    init {
        NativeLoader.load()
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    @UsedByNative
    external fun create(
        scaleX: Float,
        scaleY: Float,
        cornerRounding: Float,
        slantDegrees: Float,
        pinch: Float,
        rotationDegrees: Float,
        particleGapDistanceScale: Float,
        particleGapDurationMillis: Long,
        behaviorNativePointersArray: LongArray,
    ): Long

    /** Release the underlying memory allocated in [create]. */
    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative external fun getScaleX(nativePointer: Long): Float

    @UsedByNative external fun getScaleY(nativePointer: Long): Float

    @UsedByNative external fun getCornerRounding(nativePointer: Long): Float

    @UsedByNative @AngleDegreesFloat external fun getSlantDegrees(nativePointer: Long): Float

    @UsedByNative external fun getPinch(nativePointer: Long): Float

    @UsedByNative @AngleDegreesFloat external fun getRotationDegrees(nativePointer: Long): Float

    @UsedByNative external fun getParticleGapDistanceScale(nativePointer: Long): Float

    @UsedByNative external fun getParticleGapDurationMillis(nativePointer: Long): Long

    @UsedByNative external fun getBehaviorCount(nativePointer: Long): Int

    @UsedByNative external fun newCopyOfBrushBehavior(nativePointer: Long, index: Int): Long
}
