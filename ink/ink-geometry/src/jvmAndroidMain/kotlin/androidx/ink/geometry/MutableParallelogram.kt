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

package androidx.ink.geometry

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo

/**
 * Mutable parallelogram (i.e. a quadrilateral with parallel sides), defined by its [center],
 * [width], [height], [rotation], and [shearFactor].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class MutableParallelogram
private constructor(
    center: Vec,
    width: Float,
    override var height: Float,
    @AngleRadiansFloat rotation: Float,
    override var shearFactor: Float,
) : Parallelogram {

    /* [_center] is a private backing field that is internally constructed such that no
     * caller can obtain a direct reference to it. */
    private var _center: MutableVec = MutableVec(center.x, center.y)
    @AngleRadiansFloat private var _rotation: Float = Angle.normalized(rotation)
    override var rotation: Float
        @AngleRadiansFloat get() = _rotation
        set(@AngleRadiansFloat value) {
            _rotation = Angle.normalized(value)
        }

    override var center: Vec
        get() = _center
        set(value) {
            _center.x = value.x
            _center.y = value.y
        }

    private var _width: Float = width
    override var width: Float
        @FloatRange(from = 0.0) get() = _width
        set(@FloatRange(from = 0.0) value) {
            // A [Parallelogram] may *not* have a negative width. If an operation is performed on
            // [Parallelogram] resulting
            // in a negative width, it will be normalized.
            Parallelogram.normalizeAndRun(value, height, rotation) { w: Float, h: Float, r: Float ->
                _width = w
                height = h
                rotation = r
                this@MutableParallelogram
            }
        }

    public constructor() : this(ImmutableVec(0f, 0f), 0f, 0f, Angle.ZERO, 0f)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // TODO: b/355248266 - @UsedByNative("parallelogram_jni_helper.cc") must go in Proguard config
    // file instead.
    private fun setCenterDimensionsRotationAndShear(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        @AngleRadiansFloat rotation: Float,
        shearFactor: Float,
    ): Unit = run {
        Parallelogram.normalizeAndRun(width, height, rotation) { w: Float, h: Float, r: Float ->
            this.width = w
            this.height = h
            this.rotation = r
            this.shearFactor = shearFactor
            this._center.x = centerX
            this._center.y = centerY
            this
        }
    }

    override fun equals(other: Any?): Boolean =
        other === this || (other is Parallelogram && Parallelogram.areEquivalent(this, other))

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    override fun hashCode(): Int = Parallelogram.hash(this)

    override fun toString(): String = "Mutable${Parallelogram.string(this)}"

    public companion object {

        /**
         * Constructs an [MutableParallelogram] with a given [center], [width] and [height]. The
         * resulting [Parallelogram] has zero [rotation] and [shearFactor]. If the [width] is less
         * than zero, the [Parallelogram] will be normalized.
         */
        @JvmStatic
        public fun fromCenterAndDimensions(
            center: Vec,
            @FloatRange(from = 0.0) width: Float,
            height: Float,
        ): MutableParallelogram =
            Parallelogram.normalizeAndRun(width, height, rotation = Angle.ZERO) {
                w: Float,
                h: Float,
                r: Float ->
                MutableParallelogram(center, w, h, r, shearFactor = 0f)
            }

        /**
         * Constructs an [MutableParallelogram] with a given [center], [width], [height] and
         * [rotation]. The resulting [Parallelogram] has zero [shearFactor]. If the [width] is less
         * than zero or if the [rotation] is not in the range [0, 2π), the [Parallelogram] will be
         * normalized.
         */
        @JvmStatic
        public fun fromCenterDimensionsAndRotation(
            center: Vec,
            @FloatRange(from = 0.0) width: Float,
            height: Float,
            @AngleRadiansFloat rotation: Float,
        ): MutableParallelogram =
            Parallelogram.normalizeAndRun(width, height, rotation) { w: Float, h: Float, r: Float ->
                MutableParallelogram(center, w, h, r, shearFactor = 0f)
            }

        /**
         * Constructs an [MutableParallelogram] with a given [center], [width], [height], [rotation]
         * and [shearFactor]. If the [width] is less than zero or if the [rotation] is not in the
         * range [0, 2π), the [Parallelogram] will be normalized.
         */
        @JvmStatic
        public fun fromCenterDimensionsRotationAndShear(
            center: Vec,
            @FloatRange(from = 0.0) width: Float,
            height: Float,
            @AngleRadiansFloat rotation: Float,
            shearFactor: Float,
        ): MutableParallelogram =
            Parallelogram.normalizeAndRun(width, height, rotation) { w: Float, h: Float, r: Float ->
                MutableParallelogram(center, w, h, r, shearFactor)
            }
    }
}
