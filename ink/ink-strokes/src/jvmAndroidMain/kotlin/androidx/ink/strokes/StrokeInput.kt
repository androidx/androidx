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

package androidx.ink.strokes

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.ink.brush.InputToolType

/**
 * A single input specifying position, time since the start of the stream, and optionally
 * [pressure], [tiltRadians], and [orientationRadians].
 *
 * This data type is used as an input to [StrokeInputBatch] and [InProgressStroke]. If these are to
 * be created as part of real-time input, it is recommended to use some sort of object pool so that
 * new usages can make use of existing objects that have been recycled, rather than allocating new
 * ones which could introduce unpredictable garbage collection related delays to the time-sensitive
 * input path. This class has the [update] method for that purpose, rather than being immutable.
 */
public class StrokeInput {
    /** The x-coordinate of the input position in stroke space. */
    public var x: Float = 0F
        private set
        get // TODO: b/355248266 - @UsedByNative("stroke_input_jni_helper.cc") must go in Proguard

    // config file instead.

    /** The y-coordinate of the input position in stroke space. */
    public var y: Float = 0F
        private set
        get // TODO: b/355248266 - @UsedByNative("stroke_input_jni_helper.cc") must go in Proguard

    // config file instead.

    /** Time elapsed since the start of the stroke. */
    public var elapsedTimeMillis: Long = 0L
        private set
        get // TODO: b/355248266 - @UsedByNative("stroke_input_jni_helper.cc") must go in Proguard

    // config file instead.

    /** The input device used to generate this stroke input. */
    public var toolType: InputToolType = InputToolType.UNKNOWN
        private set
        get // TODO: b/355248266 - @UsedByNative("stroke_input_jni_helper.cc") must go in Proguard

    // config file instead.

    /**
     * The physical distance in centimeters that the pointer must travel in order to produce an
     * input motion of one stroke unit. For stylus/touch, this is the real-world distance that the
     * stylus/fingertip must move in physical space; for mouse, this is the visual distance that the
     * mouse pointer must travel along the surface of the display.
     *
     * A value of [NO_STROKE_UNIT_LENGTH] indicates that the relationship between stroke space and
     * physical space is unknown or ill-defined.
     */
    public var strokeUnitLengthCm: Float = NO_STROKE_UNIT_LENGTH
        private set
        get // TODO: b/355248266 - @UsedByNative("stroke_input_jni_helper.cc") must go in Proguard

    // config file instead.

    /**
     * Pressure value in the normalized, unitless range of [0, 1] indicating the force exerted
     * during input.
     *
     * A value of [NO_PRESSURE] indicates that pressure is not reported, which can be checked with
     * [hasPressure].
     */
    public var pressure: Float = NO_PRESSURE
        private set
        get // TODO: b/355248266 - @UsedByNative("stroke_input_jni_helper.cc") must go in Proguard

    // config file instead.

    /** Whether the [pressure] field contains a valid pressure value. */
    @get:JvmName("hasPressure")
    public val hasPressure: Boolean
        get() = pressure != NO_PRESSURE

    /**
     * The angle between a stylus and the line perpendicular to the plane of the screen. The value
     * should be normalized to fall between 0 and π/2 in radians, where 0 is perpendicular to the
     * screen and π/2 is flat against the drawing surface.
     *
     * [NO_TILT] indicates that tilt is not reported, which can be checked with [hasTilt].
     */
    public var tiltRadians: Float = NO_TILT
        private set
        get // TODO: b/355248266 - @UsedByNative("stroke_input_jni_helper.cc") must go in Proguard

    // config file instead.

    /** Whether the [tiltRadians] field contains a valid tilt value. */
    @get:JvmName("hasTilt")
    public val hasTilt: Boolean
        get() = tiltRadians != NO_TILT

    /**
     * The angle that indicates the direction in which the stylus is pointing in relation to the
     * positive x axis. The value should be normalized to fall between 0 and 2π in radians, where 0
     * means the ray from the stylus tip to the end is along positive x and values increase towards
     * the positive y-axis.
     *
     * When [tiltRadians] is equal to π/2, the value for [orientationRadians] is indeterminant.
     *
     * [NO_ORIENTATION] indicates that orientation is not reported, which can be checked with
     * [hasOrientation]. Note, that this is a separate condition from the orientation being
     * indeterminant when [tiltRadians] is π/2.
     */
    public var orientationRadians: Float = NO_ORIENTATION
        private set
        get // TODO: b/355248266 - @UsedByNative("stroke_input_jni_helper.cc") must go in Proguard

    // config file instead.

    /** Whether the [orientationRadians] field contains a valid orientation value. */
    @get:JvmName("hasOrientation")
    public val hasOrientation: Boolean
        get() = orientationRadians != NO_ORIENTATION

    /**
     * Overwrite this instance with new values.
     *
     * @param x The `x` position coordinate of the input in the stroke's coordinate space.
     * @param y The `y` position coordinate of the input in the stroke's coordinate space.
     * @param elapsedTimeMillis Marks the number of milliseconds since the stroke started. It is a
     *   non-negative timestamp in the [android.os.SystemClock.elapsedRealtime] time base.
     * @param toolType The type of tool used to create this input data.
     * @param strokeUnitLengthCm The physical distance in centimeters that the pointer must travel
     *   in order to produce an input motion of one stroke unit. For stylus/touch, this is the
     *   real-world distance that the stylus/ fingertip must move in physical space; for mouse, this
     *   is the visual distance that the mouse pointer must travel along the surface of the display.
     *   A value of [NO_STROKE_UNIT_LENGTH] indicates that the relationship between stroke space and
     *   physical space is unknown or ill-defined.
     * @param pressure Should be within [0, 1] but it's not enforced until added to a
     *   [StrokeInputBatch] object. Absence of [pressure] data is represented with [NO_PRESSURE].
     * @param tiltRadians The angle in radians between a stylus and the line perpendicular to the
     *   plane of the screen. 0 is perpendicular to the screen and PI/2 is flat against the drawing
     *   surface. Absence of [tiltRadians] data is represented with [NO_TILT].
     * @param orientationRadians Indicates the direction in which the stylus is pointing in relation
     *   to the positive x axis in radians. A value of 0 means the ray from the stylus tip to the
     *   end is along positive x and values increase towards the positive y-axis. Absence of
     *   [orientationRadians] data is represented with [NO_ORIENTATION].
     */
    @JvmOverloads
    public fun update(
        x: Float,
        y: Float,
        @IntRange(from = 0) elapsedTimeMillis: Long,
        toolType: InputToolType = InputToolType.UNKNOWN,
        strokeUnitLengthCm: Float = NO_STROKE_UNIT_LENGTH,
        pressure: Float = NO_PRESSURE,
        tiltRadians: Float = NO_TILT,
        orientationRadians: Float = NO_ORIENTATION,
    ) {
        this.toolType = toolType
        this.x = x
        this.y = y
        this.elapsedTimeMillis = elapsedTimeMillis
        this.strokeUnitLengthCm = strokeUnitLengthCm
        this.pressure = pressure
        this.tiltRadians = tiltRadians
        this.orientationRadians = orientationRadians
    }

    /** @see update */
    // TODO: b/362469375 - Change JNI to use `update` and delete `overwrite`
    // TODO: b/355248266 - @UsedByNative("stroke_input_jni_helper.cc") must go in Proguard config
    // file instead.
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @Deprecated("Renaming to update")
    public fun overwrite(
        x: Float,
        y: Float,
        @IntRange(from = 0) elapsedTimeMillis: Long,
        toolType: InputToolType = InputToolType.UNKNOWN,
        strokeUnitLengthCm: Float = NO_STROKE_UNIT_LENGTH,
        pressure: Float = NO_PRESSURE,
        tiltRadians: Float = NO_TILT,
        orientationRadians: Float = NO_ORIENTATION,
    ) {
        update(
            x,
            y,
            elapsedTimeMillis,
            toolType,
            strokeUnitLengthCm,
            pressure,
            tiltRadians,
            orientationRadians
        )
    }

    public override fun equals(other: Any?): Boolean {
        // NOMUTANTS -- Check the instance first to short circuit faster.
        if (this === other) return true
        if (other !is StrokeInput) return false

        return x == other.x &&
            y == other.y &&
            elapsedTimeMillis == other.elapsedTimeMillis &&
            toolType == other.toolType &&
            strokeUnitLengthCm == other.strokeUnitLengthCm &&
            pressure == other.pressure &&
            tiltRadians == other.tiltRadians &&
            orientationRadians == other.orientationRadians
    }

    // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
    public override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + elapsedTimeMillis.hashCode()
        result = 31 * result + toolType.hashCode()
        result = 31 * result + strokeUnitLengthCm.hashCode()
        result = 31 * result + pressure.hashCode()
        result = 31 * result + tiltRadians.hashCode()
        result = 31 * result + orientationRadians.hashCode()
        return result
    }

    public override fun toString(): String {
        return "StrokeInput(x=$x, y=$y, elapsedTimeMillis=$elapsedTimeMillis, toolType=$toolType, " +
            "strokeUnitLengthCm=$strokeUnitLengthCm, pressure=$pressure, tiltRadians=$tiltRadians, " +
            "orientationRadians=$orientationRadians)"
    }

    public companion object {
        public const val NO_STROKE_UNIT_LENGTH: Float = 0f
        public const val NO_PRESSURE: Float = -1f
        public const val NO_TILT: Float = -1f
        public const val NO_ORIENTATION: Float = -1f

        /**
         * Allocate and return a new [StrokeInput]. Only intended for test code - real code should
         * use a recycling pattern to avoid allocating during latency-sensitive real-time input,
         * using [update] on an instance allocated with the zero-argument constructor.
         */
        @VisibleForTesting
        @JvmStatic
        @JvmOverloads
        public fun create(
            x: Float,
            y: Float,
            @IntRange(from = 0) elapsedTimeMillis: Long,
            toolType: InputToolType = InputToolType.UNKNOWN,
            strokeUnitLengthCm: Float = NO_STROKE_UNIT_LENGTH,
            pressure: Float = NO_PRESSURE,
            tiltRadians: Float = NO_TILT,
            orientationRadians: Float = NO_ORIENTATION,
        ): StrokeInput {
            return StrokeInput().apply {
                update(
                    x,
                    y,
                    elapsedTimeMillis,
                    toolType,
                    strokeUnitLengthCm,
                    pressure,
                    tiltRadians,
                    orientationRadians,
                )
            }
        }
    }
}
