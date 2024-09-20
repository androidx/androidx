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
import androidx.ink.geometry.Angle
import androidx.ink.geometry.AngleRadiansFloat
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.util.Collections.unmodifiableList
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic
import kotlin.math.PI

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@ExperimentalInkCustomBrushApi
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class BrushTip(
    /**
     * 2D scale used to calculate the initial width and height of the tip shape relative to the
     * brush size prior to applying [slant] and [rotation].
     *
     * The base width and height of the tip will be equal to the brush size multiplied by [scaleX]
     * and [scaleY] respectively. Valid values must be finite and non-negative, with at least one
     * value greater than zero.
     */
    @FloatRange(
        from = 0.0,
        fromInclusive = true,
        to = Double.POSITIVE_INFINITY,
        toInclusive = false
    )
    public val scaleX: Float = 1f,

    /**
     * 2D scale used to calculate the initial width and height of the tip shape relative to the
     * brush size prior to applying [slant] and [rotation].
     *
     * The base width and height of the tip will be equal to the brush size multiplied by [scaleX]
     * and [scaleY] respectively. Valid values must be finite and non-negative, with at least one
     * value greater than zero.
     */
    @FloatRange(
        from = 0.0,
        fromInclusive = true,
        to = Double.POSITIVE_INFINITY,
        toInclusive = false
    )
    public val scaleY: Float = 1f,

    /**
     * A normalized value in the range [0, 1] that is used to calculate the initial radius of
     * curvature for the tip's corners. A value of 0 results in sharp corners and a value of 1
     * results in the maximum radius of curvature given the current tip dimensions.
     */
    @FloatRange(from = 0.0, fromInclusive = true, to = 1.0, toInclusive = true)
    public val cornerRounding: Float = 1f,

    /**
     * Angle in readians used to calculate the initial slant of the tip shape prior to applying
     * [rotation].
     *
     * The value should be in the range [-π/2, π/2] radians, and represents the angle by which
     * "vertical" lines of the tip shape will appear rotated about their intersection with the
     * x-axis.
     *
     * More info: This property is similar to the single-arg CSS skew() transformation. Unlike skew,
     * slant tries to preserve the perimeter of the tip shape as opposed to its area. This is akin
     * to "pressing" a rectangle into a parallelogram with non-right angles while preserving the
     * side lengths.
     */
    @FloatRange(from = -PI / 2, fromInclusive = true, to = PI / 2, toInclusive = true)
    @AngleRadiansFloat
    public val slant: Float = Angle.ZERO,

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
    @FloatRange(from = 0.0, fromInclusive = true, to = 1.0, toInclusive = true)
    public val pinch: Float = 0f,

    /**
     * Angle in radians specifying the initial rotation of the tip shape after applying [scaleX],
     * [scaleY], [pinch], and [slant].
     */
    @AngleRadiansFloat public val rotation: Float = Angle.ZERO,

    /**
     * Scales the opacity of the base brush color for this tip, independent of `brush_behavior`s. A
     * possible example application is a highlighter brush.
     *
     * The multiplier must be in the range [0, 2] and the value ultimately applied can be modified
     * by applicable `brush_behavior`s.
     */
    @FloatRange(from = 0.0, fromInclusive = true, to = 2.0, toInclusive = true)
    public val opacityMultiplier: Float = 1f,

    /**
     * Parameter controlling emission of particles as a function of distance traveled by the stroke
     * inputs.
     *
     * When this and [particleGapDurationMillis] are both zero, the stroke will be continuous,
     * unless gaps are introduced dynamically by [BrushBehavior]s. Otherwise, the stroke will be
     * made up of particles. A new particle will be emitted after at least
     * [particleGapDistanceScale] * [Brush.size] distance has been traveled by the stoke inputs.
     */
    @FloatRange(
        from = 0.0,
        fromInclusive = true,
        to = Double.POSITIVE_INFINITY,
        toInclusive = false
    )
    public val particleGapDistanceScale: Float = 0f,

    /**
     * Parameter controlling emission of particles as a function of time elapsed along the stroke.
     *
     * When this and [particleGapDistanceScale] are both zero, the stroke will be continuous, unless
     * gaps are introduced dynamically by `BrushBehavior`s. Otherwise, the stroke will be made up of
     * particles. Particles will be emitted at most once every [particleGapDurationMillis].
     */
    @IntRange(from = 0L) public val particleGapDurationMillis: Long = 0L,

    // The [behaviors] val below is a defensive copy of this parameter.
    behaviors: List<BrushBehavior> = emptyList(),
) {
    /**
     * A list of [BrushBehavior]s that allow dynamic properties of each input to augment the tip
     * shape and color.
     */
    public val behaviors: List<BrushBehavior> = unmodifiableList(behaviors.toList())

    /** A handle to the underlying native [BrushTip] object. */
    internal val nativePointer: Long =
        nativeCreateBrushTip(
            scaleX,
            scaleY,
            cornerRounding,
            slant,
            pinch,
            rotation,
            opacityMultiplier,
            particleGapDistanceScale,
            particleGapDurationMillis,
            behaviors.size,
        )

    init {
        for (behavior in behaviors) {
            nativeAppendBehavior(nativePointer, behavior.nativePointer)
        }
    }

    /**
     * Creates a copy of `this` and allows named properties to be altered while keeping the rest
     * unchanged.
     */
    @JvmSynthetic
    public fun copy(
        scaleX: Float = this.scaleX,
        scaleY: Float = this.scaleY,
        cornerRounding: Float = this.cornerRounding,
        @AngleRadiansFloat slant: Float = this.slant,
        pinch: Float = this.pinch,
        @AngleRadiansFloat rotation: Float = this.rotation,
        opacityMultiplier: Float = this.opacityMultiplier,
        particleGapDistanceScale: Float = this.particleGapDistanceScale,
        particleGapDurationMillis: Long = this.particleGapDurationMillis,
        behaviors: List<BrushBehavior> = this.behaviors,
    ): BrushTip =
        BrushTip(
            scaleX,
            scaleY,
            cornerRounding,
            slant,
            pinch,
            rotation,
            opacityMultiplier,
            particleGapDistanceScale,
            particleGapDurationMillis,
            behaviors,
        )

    /**
     * Returns a [Builder] with values set equivalent to `this`. Java developers, use the returned
     * builder to build a copy of a BrushTip. Kotlin developers, see [copy] method.
     */
    public fun toBuilder(): Builder =
        Builder()
            .setScaleX(scaleX)
            .setScaleY(scaleY)
            .setCornerRounding(cornerRounding)
            .setSlant(slant)
            .setPinch(pinch)
            .setRotation(rotation)
            .setOpacityMultiplier(opacityMultiplier)
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
        private var slant: Float = Angle.ZERO
        private var pinch: Float = 0f
        private var rotation: Float = Angle.ZERO
        private var opacityMultiplier: Float = 1f
        private var particleGapDistanceScale: Float = 0F
        private var particleGapDurationMillis: Long = 0L
        private var behaviors: List<BrushBehavior> = emptyList()

        public fun setScaleX(scaleX: Float): Builder = apply { this.scaleX = scaleX }

        public fun setScaleY(scaleY: Float): Builder = apply { this.scaleY = scaleY }

        public fun setCornerRounding(cornerRounding: Float): Builder = apply {
            this.cornerRounding = cornerRounding
        }

        public fun setSlant(slant: Float): Builder = apply { this.slant = slant }

        public fun setPinch(pinch: Float): Builder = apply { this.pinch = pinch }

        public fun setRotation(rotation: Float): Builder = apply { this.rotation = rotation }

        public fun setOpacityMultiplier(opacityMultiplier: Float): Builder = apply {
            this.opacityMultiplier = opacityMultiplier
        }

        public fun setParticleGapDistanceScale(particleGapDistanceScale: Float): Builder = apply {
            this.particleGapDistanceScale = particleGapDistanceScale
        }

        public fun setParticleGapDurationMillis(particleGapDurationMillis: Long): Builder = apply {
            this.particleGapDurationMillis = particleGapDurationMillis
        }

        public fun setBehaviors(behaviors: List<BrushBehavior>): Builder = apply {
            this.behaviors = behaviors.toList()
        }

        public fun build(): BrushTip =
            BrushTip(
                scaleX,
                scaleY,
                cornerRounding,
                slant,
                pinch,
                rotation,
                opacityMultiplier,
                particleGapDistanceScale,
                particleGapDurationMillis,
                behaviors,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is BrushTip) return false
        return scaleY == other.scaleY &&
            scaleX == other.scaleX &&
            pinch == other.pinch &&
            cornerRounding == other.cornerRounding &&
            slant == other.slant &&
            rotation == other.rotation &&
            particleGapDistanceScale == other.particleGapDistanceScale &&
            particleGapDurationMillis == other.particleGapDurationMillis &&
            opacityMultiplier == other.opacityMultiplier &&
            behaviors == other.behaviors
    }

    override fun hashCode(): Int {
        var result = scaleX.hashCode()
        result = 31 * result + scaleY.hashCode()
        result = 31 * result + pinch.hashCode()
        result = 31 * result + cornerRounding.hashCode()
        result = 31 * result + slant.hashCode()
        result = 31 * result + rotation.hashCode()
        result = 31 * result + opacityMultiplier.hashCode()
        result = 31 * result + particleGapDistanceScale.hashCode()
        result = 31 * result + particleGapDurationMillis.hashCode()
        result = 31 * result + behaviors.hashCode()
        return result
    }

    override fun toString(): String =
        "BrushTip(scale=($scaleX, $scaleY), cornerRounding=$cornerRounding," +
            " slant=$slant, pinch=$pinch, rotation=$rotation, opacityMultiplier=$opacityMultiplier," +
            " particleGapDistanceScale=$particleGapDistanceScale," +
            " particleGapDurationMillis=$particleGapDurationMillis, behaviors=$behaviors)"

    /** Delete native BrushTip memory. */
    protected fun finalize() {
        // NOMUTANTS -- Not tested post garbage collection.
        nativeFreeBrushTip(nativePointer)
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    @UsedByNative
    private external fun nativeCreateBrushTip(
        scaleX: Float,
        scaleY: Float,
        cornerRounding: Float,
        slant: Float,
        pinch: Float,
        rotation: Float,
        opacityMultiplier: Float,
        particleGapDistanceScale: Float,
        particleGapDurationMillis: Long,
        behaviorsCount: Int,
    ): Long

    /**
     * Appends a texture layer to a *mutable* C++ BrushTip object as referenced by [nativePointer].
     * Only call during init{} so to keep this BrushTip object immutable after construction and
     * equivalent across Kotlin and C++.
     */
    @UsedByNative
    private external fun nativeAppendBehavior(tipNativePointer: Long, behaviorNativePointer: Long)

    /** Release the underlying memory allocated in [nativeCreateBrushTip]. */
    @UsedByNative private external fun nativeFreeBrushTip(nativePointer: Long)

    // Companion object gets initialized before anything else.
    public companion object {
        init {
            NativeLoader.load()
        }

        /** Returns a new [BrushTip.Builder]. */
        @JvmStatic public fun builder(): Builder = Builder()
    }
}
