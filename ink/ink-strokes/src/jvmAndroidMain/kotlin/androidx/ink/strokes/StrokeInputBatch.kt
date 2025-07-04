/*
 * Copyright (C) 2024-2025 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/**
 * A read-only view of an object that stores multiple [StrokeInput] values together in a more
 * memory-efficient manner than just `List<StrokeInput>`. The input points in this batch are
 * guaranteed to be consistent with one another – for example, they all have the same [toolType] and
 * the same set of optional fields like pressure/tilt/orientation, and their timestamps are all
 * monotonically non-decreasing. This can be an [ImmutableStrokeInputBatch] for data that cannot
 * change, and a [MutableStrokeInputBatch] for data that is meant to be modified or incrementally
 * built.
 */
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public abstract class StrokeInputBatch internal constructor(nativePointer: Long) {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long = nativePointer

    /** Number of [StrokeInput] objects in the batch. */
    public val size: Int
        get() = StrokeInputBatchNative.getSize(nativePointer)

    /** `true` if there are no [StrokeInput] objects in the batch, and `false` otherwise. */
    public fun isEmpty(): Boolean = size == 0

    /**
     * How this input stream should be interpreted, as coming from a [InputToolType.MOUSE],
     * [InputToolType.TOUCH], or [InputToolType.STYLUS].
     */
    public fun getToolType(): InputToolType =
        InputToolType.from(StrokeInputBatchNative.getToolType(nativePointer))

    /** The duration between the first and last input in milliseconds. */
    public fun getDurationMillis(): Long = StrokeInputBatchNative.getDurationMillis(nativePointer)

    /**
     * The physical distance in centimeters that the pointer must travel in order to produce an
     * input motion of one stroke unit. For stylus/touch, this is the real-world distance that the
     * stylus/fingertip must move in physical space; for mouse, this is the visual distance that the
     * mouse pointer must travel along the surface of the display.
     *
     * A value of [StrokeInput.NO_STROKE_UNIT_LENGTH] indicates that the relationship between stroke
     * space and physical space is unknown or ill-defined.
     */
    public fun getStrokeUnitLengthCm(): Float =
        StrokeInputBatchNative.getStrokeUnitLengthCm(nativePointer)

    /**
     * Whether [strokeUnitLengthCm] has a valid value, which is something other than
     * [StrokeInput.NO_STROKE_UNIT_LENGTH].
     */
    public fun hasStrokeUnitLength(): Boolean =
        StrokeInputBatchNative.hasStrokeUnitLength(nativePointer)

    /**
     * Whether all of the individual inputs have a defined value for [StrokeInput.pressure]. If not,
     * then no input items have a pressure value.
     */
    public fun hasPressure(): Boolean = StrokeInputBatchNative.hasPressure(nativePointer)

    /**
     * Whether all of the individual inputs have a defined value for [StrokeInput.tiltRadians]. If
     * not, then no input items have a tilt value.
     */
    public fun hasTilt(): Boolean = StrokeInputBatchNative.hasTilt(nativePointer)

    /**
     * Whether all of the individual inputs have a defined value for
     * [StrokeInput.orientationRadians]. If not, then no input items have an orientation value.
     */
    public fun hasOrientation(): Boolean = StrokeInputBatchNative.hasOrientation(nativePointer)

    /**
     * Returns the seed value that should be used for seeding any noise generators for brush
     * behaviors when a full stroke is regenerated with this input batch. If no seed value has yet
     * been set for this input batch, returns the default seed of zero.
     */
    @ExperimentalInkCustomBrushApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun getNoiseSeed(): Int = StrokeInputBatchNative.getNoiseSeed(nativePointer)

    /**
     * Gets the value of the i-th input. Requires that [index] is positive and less than [size].
     *
     * In performance-sensitive code, prefer to use [populate] to pass in a pre-allocated instance
     * and reuse that instance across multiple calls to this function.
     */
    public operator fun get(index: Int): StrokeInput = populate(index, StrokeInput())

    /**
     * Gets the value of the i-th input and overwrites [outStrokeInput], which it then returns.
     * Requires that [index] is positive and less than [size].
     */
    public fun populate(index: Int, outStrokeInput: StrokeInput): StrokeInput {
        require(index < size && index >= 0) { "index ($index) must be in [0, size=$size)" }
        StrokeInputBatchNative.populate(nativePointer, index, outStrokeInput)
        return outStrokeInput
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public abstract fun asImmutable(): ImmutableStrokeInputBatch

    protected fun finalize() {
        // NOMUTANTS--Not tested post garbage collection.
        StrokeInputBatchNative.free(nativePointer)
    }

    // Declared as a target for extension functions.
    public companion object
}

/**
 * An immutable implementation of [StrokeInputBatch]. For a mutable alternative, see
 * [MutableStrokeInputBatch].
 */
public class ImmutableStrokeInputBatch private constructor(nativePointer: Long) :
    StrokeInputBatch(nativePointer) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public override fun asImmutable(): ImmutableStrokeInputBatch = this

    public override fun toString(): String = "ImmutableStrokeInputBatch(size=$size)"

    public companion object {
        /** Wrap a native `ink::StrokeInputBatch` with an [ImmutableStrokeInputBatch]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(nativePointer: Long): ImmutableStrokeInputBatch {
            return ImmutableStrokeInputBatch(nativePointer)
        }

        /** An empty [ImmutableStrokeInputBatch]. */
        @JvmField
        public val EMPTY: ImmutableStrokeInputBatch =
            ImmutableStrokeInputBatch(StrokeInputBatchNative.create())
    }
}

/**
 * A mutable implementation of [StrokeInputBatch]. For an immutable alternative, see
 * [ImmutableStrokeInputBatch].
 *
 * Each appended [StrokeInput] value is validated compared to the existing batch contents. This
 * means:
 * 1) All floating point values are required to be finite and the format of all inputs must be
 *    consistent. This means all inputs must have the same set of optional member variables that
 *    hold a value. For example, every input holds a [pressure] value if-and-only-if every other
 *    input holds a [pressure] value. This is also true for [tiltRadians] and [orientationRadians].
 * 2) The sequence of [StrokeInput] values must not contain repeated x-y-t triplets, and the time
 *    values must be non-negative and non-decreasing.
 * 3) Values of [strokeUnitLengthCm] must be finite and positive, or be
 *    [StrokeInput.NO_STROKE_UNIT_LENGTH].
 * 4) Values of [StrokeInput.pressure] must fall within the range of [0, 1] or be
 *    [StrokeInput.NO_PRESSURE]
 * 5) Values of [StrokeInput.tiltRadians] must fall within the range of [0, π/2] or be
 *    [StrokeInput.NO_TILT].
 * 6) Values of [StrokeInput.orientationRadians] must fall within the range of
 *    [0, 2π) or be [StrokeInput.NO_ORIENTATION].
 * 7) The [toolType] and [strokeUnitLengthCm] values must be the same across all inputs.
 */
public class MutableStrokeInputBatch : StrokeInputBatch(StrokeInputBatchNative.create()) {

    public fun clear(): Unit = MutableStrokeInputBatchNative.clear(nativePointer)

    /**
     * Adds an [input] to the batch if valid.
     *
     * Inputs are invalid if they contain values out of the valid range, duplicate a previous input,
     * have an elapsed time before a previous input, or have a different tool type or set different
     * optional fields (pressure, tilt, or orientation) than the inputs already in the batch.
     *
     * Returns this instance to allow call chaining.
     *
     * @param input The [StrokeInput] to add to the batch.
     * @return `this`
     * @throws IllegalArgumentException If the input is not valid. Note that this can be a common
     *   occurrence with real user input on certain devices, in particular due to duplicate or
     *   out-of-order inputs. Therefore, users should either catch and handle this exception or
     *   sanitize the input to avoid ensure validity before passing it to this function.
     */
    public fun add(input: StrokeInput): MutableStrokeInputBatch {
        return add(
            input.toolType,
            input.x,
            input.y,
            input.elapsedTimeMillis,
            input.strokeUnitLengthCm,
            input.pressure,
            input.tiltRadians,
            input.orientationRadians,
        )
    }

    /**
     * Variant of [add] that takes individual parameters instead of a [StrokeInput].
     *
     * Returns this instance to allow call chaining.
     *
     * @param type The [InputToolType] to use for the input.
     * @param x The x-coordinate of the input position in stroke space.
     * @param y The y-coordinate of the input position in stroke space.
     * @param elapsedTimeMillis Marks the number of milliseconds since the stroke started. It is a
     *   non-negative timestamp in the [android.os.SystemClock.elapsedRealtime] time base.
     * @param strokeUnitLengthCm The physical distance in centimeters that the pointer must travel
     *   in order to produce an input motion of one stroke unit. For stylus/touch, this is the
     *   real-world distance that the stylus/fingertip must move in physical space; for mouse, this
     *   is the visual distance that the mouse pointer must travel along the surface of the display.
     *   A value of [StrokeInput.NO_STROKE_UNIT_LENGTH] indicates that the relationship between
     *   stroke space and physical space is unknown or ill-defined.
     * @param pressure Should be within [0, 1] but it's not enforced until added to a
     *   [StrokeInputBatch] object. Absence of [pressure] data is represented with
     *   [StrokeInput.NO_PRESSURE].
     * @param tiltRadians The angle in radians between a stylus and the line perpendicular to the
     *   plane of the screen. 0 is perpendicular to the screen and PI/2 is flat against the drawing
     *   surface. Absence of [tiltRadians] data is represented with [StrokeInput.NO_TILT].
     * @param orientationRadians Indicates the direction in which the stylus is pointing in relation
     *   to the positive x axis in radians. A value of 0 means the ray from the stylus tip to the
     *   end is along positive x and values increase towards the positive y-axis. Absence of
     *   [orientationRadians] data is represented with [StrokeInput.NO_ORIENTATION].
     * @return `this`
     * @throws IllegalArgumentException If the input is not valid. Note that this can be a common
     *   occurrence with real user input on certain devices, in particular due to duplicate or
     *   out-of-order inputs. Therefore, users should either catch and handle this exception or
     *   sanitize the input to avoid ensure validity before passing it to this function.
     */
    @JvmOverloads
    public fun add(
        type: InputToolType,
        x: Float,
        y: Float,
        elapsedTimeMillis: Long,
        strokeUnitLengthCm: Float = StrokeInput.NO_STROKE_UNIT_LENGTH,
        pressure: Float = StrokeInput.NO_PRESSURE,
        tiltRadians: Float = StrokeInput.NO_TILT,
        orientationRadians: Float = StrokeInput.NO_ORIENTATION,
    ): MutableStrokeInputBatch {
        val success =
            MutableStrokeInputBatchNative.appendSingle(
                nativePointer,
                type.value,
                x,
                y,
                elapsedTimeMillis,
                strokeUnitLengthCm,
                pressure,
                tiltRadians,
                orientationRadians,
            )
        check(success) { "Should have thrown an exception if add failed." }
        return this
    }

    /**
     * Validates and appends an [inputBatch]. Invalid [inputBatch] will result in no change. An
     * exception will be thrown for invalid additions.
     */
    public fun add(inputBatch: StrokeInputBatch): MutableStrokeInputBatch {
        val success =
            MutableStrokeInputBatchNative.appendBatch(nativePointer, inputBatch.nativePointer)
        check(success) { "Should have thrown an exception if add failed." }
        return this
    }

    /**
     * Validates and appends a collection of [StrokeInput]. Invalid [inputs] will result in no
     * change. An exception will be thrown for invalid additions.
     *
     * Returns this instance to allow call chaining.
     *
     * @param inputs [Collection] of [StrokeInput]s to add to the batch.
     * @return `this`
     * @throws IllegalArgumentException If the input is not valid. Note that this can be a common
     *   occurrence with real user input on certain devices, in particular due to duplicate or
     *   out-of-order inputs. Therefore, users should either catch and handle this exception or
     *   sanitize the input to avoid ensure validity before passing it to this function.
     */
    public fun add(inputs: Collection<StrokeInput>): MutableStrokeInputBatch {
        val tempBatchBuilder = MutableStrokeInputBatch()

        // Confirm all inputs are valid by first adding them to their own StrokeInputBatch in order
        // to
        // perform a group add operation to *this* batch.
        for (input in inputs) {
            val success =
                MutableStrokeInputBatchNative.appendSingle(
                    tempBatchBuilder.nativePointer,
                    input.toolType.value,
                    input.x,
                    input.y,
                    input.elapsedTimeMillis,
                    input.strokeUnitLengthCm,
                    input.pressure,
                    input.tiltRadians,
                    input.orientationRadians,
                )
            check(success) { "Should have thrown an exception if add failed." }
        }
        val success =
            MutableStrokeInputBatchNative.appendBatch(nativePointer, tempBatchBuilder.nativePointer)
        check(success) { "Should have thrown an exception if add failed." }
        return this
    }

    /**
     * Sets the per-stroke seed value that should be used when regenerating a stroke from this input
     * batch.
     */
    @ExperimentalInkCustomBrushApi
    public fun setNoiseSeed(seed: Int): Unit =
        MutableStrokeInputBatchNative.setNoiseSeed(nativePointer, seed)

    /** Create [ImmutableStrokeInputBatch] with the accumulated StrokeInputs. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public override fun asImmutable(): ImmutableStrokeInputBatch =
        @OptIn(ExperimentalInkCustomBrushApi::class)
        if (isEmpty() && getNoiseSeed() == 0) {
            ImmutableStrokeInputBatch.EMPTY
        } else {
            ImmutableStrokeInputBatch.wrapNative(
                MutableStrokeInputBatchNative.newCopy(nativePointer)
            )
        }

    public override fun toString(): String = "MutableStrokeInputBatch(size=$size)"
}

@UsedByNative
private object StrokeInputBatchNative {

    init {
        NativeLoader.load()
    }

    @UsedByNative external fun create(): Long

    @UsedByNative external fun free(nativePointer: Long)

    @UsedByNative external fun getSize(nativePointer: Long): Int

    @UsedByNative external fun getToolType(nativePointer: Long): Int

    @UsedByNative external fun getDurationMillis(nativePointer: Long): Long

    @UsedByNative external fun getStrokeUnitLengthCm(nativePointer: Long): Float

    @UsedByNative external fun hasStrokeUnitLength(nativePointer: Long): Boolean

    @UsedByNative external fun hasPressure(nativePointer: Long): Boolean

    @UsedByNative external fun hasTilt(nativePointer: Long): Boolean

    @UsedByNative external fun hasOrientation(nativePointer: Long): Boolean

    @UsedByNative external fun getNoiseSeed(nativePointer: Long): Int

    @UsedByNative external fun populate(nativePointer: Long, index: Int, input: StrokeInput)
}

@UsedByNative
private object MutableStrokeInputBatchNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun clear(nativePointer: Long)

    /** Returns whether the input was successfully added. */
    @UsedByNative
    external fun appendSingle(
        nativePointer: Long,
        type: Int,
        x: Float,
        y: Float,
        elapsedTimeMillis: Long,
        strokeUnitLengthCm: Float,
        pressure: Float,
        tilt: Float,
        orientation: Float,
    ): Boolean

    /** Returns whether the inputs were successfully added. */
    @UsedByNative external fun appendBatch(nativePointer: Long, addedNativePointer: Long): Boolean

    @UsedByNative external fun newCopy(nativePointer: Long): Long

    @UsedByNative external fun setNoiseSeed(nativePointer: Long, seed: Int)
}
