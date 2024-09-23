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

import androidx.annotation.RestrictTo
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

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var nativePointer: Long = nativePointer
        private set

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
        StrokeInputBatchNative.populate(
            nativePointer,
            index,
            outStrokeInput,
            InputToolType::class.java
        )
        return outStrokeInput
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public abstract fun asImmutable(): ImmutableStrokeInputBatch

    protected fun finalize() {
        // NOMUTANTS--Not tested post garbage collection.
        if (nativePointer == 0L) return
        StrokeInputBatchNative.freeNativePeer(nativePointer)
        nativePointer = 0
    }

    // Declared as a target for extension functions.
    public companion object
}

/**
 * An immutable implementation of [StrokeInputBatch]. For a mutable alternative, see
 * [MutableStrokeInputBatch].
 */
public class ImmutableStrokeInputBatch
/**
 * Constructor for Kotlin [ImmutableStrokeInputBatch] objects that are originally created in native
 * code and later surfaced to Kotlin. The underlying memory will be freed upon finalize() of this
 * [ImmutableStrokeInputBatch] object.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(nativePointer: Long) : StrokeInputBatch(nativePointer) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public override fun asImmutable(): ImmutableStrokeInputBatch = this

    public override fun toString(): String = "ImmutableStrokeInputBatch(size=$size)"

    public companion object {
        /** An empty [ImmutableStrokeInputBatch]. */
        @JvmField
        public val EMPTY: ImmutableStrokeInputBatch =
            ImmutableStrokeInputBatch(StrokeInputBatchNative.createNativePeer())
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
public class MutableStrokeInputBatch : StrokeInputBatch(StrokeInputBatchNative.createNativePeer()) {

    public fun clear(): Unit = MutableStrokeInputBatchNative.clear(nativePointer)

    /**
     * Validates and appends an [input]. Invalid [input] will result in no change. An exception will
     * be thrown for invalid additions.
     */
    public fun addOrThrow(input: StrokeInput): MutableStrokeInputBatch =
        add(input, throwOnError = true)

    /**
     * Validates and appends an input. Invalid input will result in no change. An exception will be
     * thrown for invalid additions.
     */
    @JvmOverloads
    public fun addOrThrow(
        type: InputToolType,
        x: Float,
        y: Float,
        elapsedTimeMillis: Long,
        strokeUnitLengthCm: Float = StrokeInput.NO_STROKE_UNIT_LENGTH,
        pressure: Float = StrokeInput.NO_PRESSURE,
        tiltRadians: Float = StrokeInput.NO_TILT,
        orientationRadians: Float = StrokeInput.NO_ORIENTATION,
    ): MutableStrokeInputBatch =
        add(
            type,
            x,
            y,
            elapsedTimeMillis,
            strokeUnitLengthCm,
            pressure,
            tiltRadians,
            orientationRadians,
            throwOnError = true,
        )

    /**
     * Validates and appends an [input]. Invalid [input] will result in no change. No exception will
     * be thrown for invalid additions.
     */
    public fun addOrIgnore(input: StrokeInput): MutableStrokeInputBatch =
        add(input, throwOnError = false)

    /**
     * Validates and appends an input. Invalid input will result in no change. No exception will be
     * thrown for invalid additions.
     */
    @JvmOverloads
    public fun addOrIgnore(
        type: InputToolType,
        x: Float,
        y: Float,
        elapsedTimeMillis: Long,
        strokeUnitLengthCm: Float = StrokeInput.NO_STROKE_UNIT_LENGTH,
        pressure: Float = StrokeInput.NO_PRESSURE,
        tiltRadians: Float = StrokeInput.NO_TILT,
        orientationRadians: Float = StrokeInput.NO_ORIENTATION,
    ): MutableStrokeInputBatch =
        add(
            type,
            x,
            y,
            elapsedTimeMillis,
            strokeUnitLengthCm,
            pressure,
            tiltRadians,
            orientationRadians,
            throwOnError = false,
        )

    /**
     * Validates and appends an [input]. Invalid [input] will result in no change. If [throwOnError]
     * is true, an exception will be thrown for invalid additions.
     */
    private fun add(input: StrokeInput, throwOnError: Boolean = false): MutableStrokeInputBatch {
        return add(
            input.toolType,
            input.x,
            input.y,
            input.elapsedTimeMillis,
            input.strokeUnitLengthCm,
            input.pressure,
            input.tiltRadians,
            input.orientationRadians,
            throwOnError,
        )
    }

    /**
     * Validates and appends an input. Invalid input will result in no change. If [throwOnError] is
     * true, an exception will be thrown for invalid additions.
     */
    private fun add(
        type: InputToolType,
        x: Float,
        y: Float,
        elapsedTimeMillis: Long,
        strokeUnitLengthCm: Float,
        pressure: Float,
        tiltRadians: Float,
        orientationRadians: Float,
        throwOnError: Boolean,
    ): MutableStrokeInputBatch {
        val errorMessage =
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
        if (throwOnError) {
            require(errorMessage == null) { errorMessage!! }
        }
        return this
    }

    /**
     * Validates and appends an [inputBatch]. Invalid [inputBatch] will result in no change. No
     * exception will be thrown for invalid additions.
     */
    public fun addOrIgnore(inputBatch: StrokeInputBatch): MutableStrokeInputBatch =
        add(inputBatch.nativePointer, throwOnError = false)

    /**
     * Validates and appends an [inputBatch]. Invalid [inputBatch] will result in no change. An
     * exception will be thrown for invalid additions.
     */
    public fun addOrThrow(inputBatch: StrokeInputBatch): MutableStrokeInputBatch =
        add(inputBatch.nativePointer, throwOnError = true)

    /**
     * Validates and appends the native representation of a [StrokeInputBatch]. Invalid inputs will
     * result in no change. If [throwOnError] is true, an exception will be thrown for invalid
     * additions.
     */
    private fun add(inputBatchNativePointer: Long, throwOnError: Boolean): MutableStrokeInputBatch {
        val errorMessage =
            MutableStrokeInputBatchNative.appendBatch(nativePointer, inputBatchNativePointer)
        if (throwOnError) {
            require(errorMessage == null) { errorMessage!! }
        }
        return this
    }

    /**
     * Validates and appends a collection of [StrokeInput]. Invalid [inputs] will result in no
     * change. No exception will be thrown for invalid additions.
     */
    public fun addOrIgnore(inputs: Collection<StrokeInput>): MutableStrokeInputBatch =
        add(inputs, throwOnError = false)

    /**
     * Validates and appends a collection of [StrokeInput]. Invalid [inputs] will result in no
     * change. An exception will be thrown for invalid additions.
     */
    public fun addOrThrow(inputs: Collection<StrokeInput>): MutableStrokeInputBatch =
        add(inputs, throwOnError = true)

    /**
     * Validates and appends a collection of [StrokeInput]. Invalid [inputs] will result in no
     * change. If [throwOnError] is true, an exception will be thrown for invalid additions.
     */
    private fun add(
        inputs: Collection<StrokeInput>,
        throwOnError: Boolean = false,
    ): MutableStrokeInputBatch {
        val tempBatchBuilder = MutableStrokeInputBatch()
        var errorMessage: String?

        // Confirm all inputs are valid by first adding them to their own StrokeInputBatch in order
        // to
        // perform a group add operation to *this*
        // batch.
        for (input in inputs) {
            errorMessage =
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
            if (throwOnError) {
                require(errorMessage == null) { errorMessage!! }
            }
        }
        errorMessage =
            MutableStrokeInputBatchNative.appendBatch(nativePointer, tempBatchBuilder.nativePointer)
        if (throwOnError) {
            require(errorMessage == null) { errorMessage!! }
        }
        return this
    }

    /** Create [ImmutableStrokeInputBatch] with the accumulated StrokeInputs. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public override fun asImmutable(): ImmutableStrokeInputBatch =
        if (isEmpty()) {
            ImmutableStrokeInputBatch.EMPTY
        } else {
            ImmutableStrokeInputBatch(MutableStrokeInputBatchNative.copy(nativePointer))
        }

    public override fun toString(): String = "MutableStrokeInputBatch(size=$size)"
}

private object StrokeInputBatchNative {

    init {
        NativeLoader.load()
    }

    @UsedByNative external fun createNativePeer(): Long

    @UsedByNative external fun freeNativePeer(nativePointer: Long)

    @UsedByNative external fun getSize(nativePointer: Long): Int

    @UsedByNative external fun getToolType(nativePointer: Long): Int

    @UsedByNative external fun getDurationMillis(nativePointer: Long): Long

    @UsedByNative external fun getStrokeUnitLengthCm(nativePointer: Long): Float

    @UsedByNative external fun hasStrokeUnitLength(nativePointer: Long): Boolean

    @UsedByNative external fun hasPressure(nativePointer: Long): Boolean

    @UsedByNative external fun hasTilt(nativePointer: Long): Boolean

    @UsedByNative external fun hasOrientation(nativePointer: Long): Boolean

    /**
     * The [toolTypeClass] parameter is passed as a convenience to native JNI code, to avoid it
     * needing to do a reflection-based FindClass lookup.
     */
    @UsedByNative
    external fun populate(
        nativePointer: Long,
        index: Int,
        input: StrokeInput,
        toolTypeClass: Class<InputToolType>,
    )
}

private object MutableStrokeInputBatchNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun clear(nativePointer: Long)

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
    ): String?

    @UsedByNative external fun appendBatch(nativePointer: Long, addedNativePointer: Long): String?

    @UsedByNative external fun copy(nativePointer: Long): Long
}
