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
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import androidx.ink.geometry.BoxAccumulator
import androidx.ink.geometry.MeshFormat
import androidx.ink.geometry.MutableVec
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

/**
 * Use an [InProgressStroke] to efficiently build a stroke over multiple rendering frames with
 * incremental inputs.
 *
 * To use an [InProgressStroke], you would typically:
 * 1. Begin a stroke by calling [start] with a chosen [Brush].
 * 2. Repeatedly update the stroke:
 *     1. Call [enqueueInputs] with any new real and predicted stroke inputs.
 *     2. Call [updateShape] when [isUpdateNeeded] is `true` and new geometry is needed for
 *        rendering.
 *     3. Render the current stroke mesh or outlines, either via a provided renderer that accepts an
 *        [InProgressStroke] or by using the various getters on this type with a custom renderer.
 * 3. Call [finishInput] once there are no more inputs for this stroke (e.g. the user lifts the
 *    stylus from the screen).
 * 4. Continue to call [updateShape] and render after [finishInput] until [isUpdateNeeded] returns
 *    false (to allow any lingering brush shape animations to complete).
 * 5. Extract the completed stroke by calling [toImmutable].
 * 6. For best performance, reuse this object and go back to step 1 rather than allocating a new
 *    instance.
 */
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class InProgressStroke {

    /** A handle to the underlying native [InProgressStroke] object. */
    internal val nativePointer: Long = InProgressStrokeNative.create()

    /**
     * The [Brush] currently being used to generate the stroke content. To set this, call [start].
     */
    public var brush: Brush? = null
        private set

    /**
     * Incremented when the stroke is changed, to know if data obtained from the other functions on
     * this class is still accurate. This can be used for cache invalidation.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var version: Long = 0L
        private set

    /**
     * Clears the in progress stroke without starting a new one.
     *
     * This includes clearing or resetting any existing inputs, mesh data, and updated region.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun clear() {
        InProgressStrokeNative.clear(nativePointer)
        this.brush = null
        version++
    }

    /**
     * Clears and starts a new stroke with the given [brush].
     *
     * This includes clearing or resetting any existing inputs, mesh data, and updated region. This
     * method must be called at least once after construction before making any calls to
     * [enqueueInputs] or [updateShape].
     */
    @OptIn(ExperimentalInkCustomBrushApi::class)
    public fun start(brush: Brush): Unit = start(brush, noiseSeed = 0)

    /**
     * Clears and starts a new stroke with the given [brush], using the given per-stroke seed value
     * to help seed the brush's noise behaviors, if any.
     *
     * This includes clearing or resetting any existing inputs, mesh data, and updated region. This
     * method must be called at least once after construction before making any calls to
     * [enqueueInputs] or [updateShape].
     */
    @ExperimentalInkCustomBrushApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun start(brush: Brush, noiseSeed: Int) {
        InProgressStrokeNative.start(nativePointer, brush.nativePointer, noiseSeed)
        this.brush = brush
        version++
    }

    /**
     * Enqueues the incremental [realInputs] and sets the prediction to [predictedInputs],
     * overwriting any previous prediction. Queued inputs will be processed on the next call to
     * [updateShape].
     *
     * This method requires that:
     * * [start] has been previously called to set the current [Brush].
     * * [finishInput] has not been called since the last call to [start].
     * * [realInputs] and [predictedInputs] must form a valid stroke input sequence together with
     *   previously added real input. In particular, this means that the first input in [realInputs]
     *   must be valid following the last input in previously added real inputs, and the first input
     *   in [predictedInputs] must be valid following the last input in [realInputs]: They must have
     *   the same [InputToolType], their [StrokeInput.elapsedTimeMillis] values must be
     *   monotonically non-decreasing, and they can not duplicate the previous input.
     *
     * Either one or both of [realInputs] and [predictedInputs] may be empty.
     *
     * @throws [IllegalStateException] If [start] has not been called since construction or the last
     *   call to [finishInput].
     * @throws [IllegalArgumentException] If the input is not valid. Note that this can be a common
     *   occurrence with real user input on certain devices, in particular due to duplicate or
     *   out-of-order inputs. Therefore, users should either catch and handle this exception or
     *   sanitize the input to avoid ensure validity before passing it to this function.
     */
    public fun enqueueInputs(realInputs: StrokeInputBatch, predictedInputs: StrokeInputBatch) {
        val success =
            InProgressStrokeNative.enqueueInputs(
                nativePointer,
                realInputs.nativePointer,
                predictedInputs.nativePointer,
            )
        check(success) { "Should have thrown an exception if enqueueInputs failed." }
        version++
    }

    /**
     * Indicates that the inputs for the current stroke are finished. After calling this, it is an
     * error to call [enqueueInputs] until [start] is called again to start a new stroke. This
     * method is idempotent; it has no effect if [start] was never called, or if this method has
     * already been called since the last call to [start]. This method is synchronous, but the
     * stroke may not be fully finished changing shape due to brush shape animations until
     * [isUpdateNeeded] returns false. Until that condition is met, keep calling [updateShape]
     * periodically and rendering the result.
     */
    public fun finishInput(): Unit =
        InProgressStrokeNative.finishInput(nativePointer).also { version++ }

    /**
     * Updates the stroke geometry up to the given duration since the start of the stroke. This will
     * consume any inputs queued up by calls to [enqueueInputs], and cause brush shape animations
     * (if any) to progress up to the specified time. Any stroke geometry resulting from
     * previously-predicted input from before the previous call to this method will be cleared.
     *
     * This method requires that:
     * * [start] has been previously called to set the current [brush].
     * * If passed, the value of [currentElapsedTimeMillis] passed into this method over the course
     *   of a single stroke must be non-decreasing and non-negative. To have shape animations
     *   progress at their intended rate, pass in values for this field that are in the same time
     *   base as the [StrokeInput.elapsedTimeMillis] values being passed to [enqueueInputs],
     *   repeatedly until [isInputFinished] returns `true`.
     *
     * Clients that do not use brushes with shape animation behaviors can omit
     * [currentElapsedTimeMillis]. Doing so when using brushes with shape animation beaviors will
     * cause the animation to be completed immediately.
     *
     * @throws [IllegalStateException] If [start] has not been called.
     * @throws [IllegalArgumentException] If [currentElapsedTimeMillis] is negative or decreased
     *   from a previous call to this method for the same in-progress stroke.
     */
    public fun updateShape(currentElapsedTimeMillis: Long = Long.MAX_VALUE) {
        val success = InProgressStrokeNative.updateShape(nativePointer, currentElapsedTimeMillis)
        check(success) { "Should have thrown an exception if updateShape failed." }
        version++
    }

    /**
     * Returns `true` if [finishInput] has been called since the last call to [start], or if [start]
     * hasn't been called yet. If this returns `true`, it is an error to call [enqueueInputs].
     */
    public fun isInputFinished(): Boolean = InProgressStrokeNative.isInputFinished(nativePointer)

    /**
     * Returns `true` if calling [updateShape] would have any effect on the stroke (and should thus
     * be called before the next render), or `false` if no calls to [updateShape] are currently
     * needed. Specifically:
     * * If the brush has one or more timed shape animation behavior that are still active (which
     *   can be true even after inputs are finished), returns `true`.
     * * If there are no active shape animation behaviors, but there are pending inputs from an
     *   [enqueueInputs] call that have not yet been consumed by a call to [updateShape], returns
     *   `true`.
     * * Otherwise, returns `false`.
     *
     * Once [isInputFinished] returns `true` and this method returns `false`, the stroke is
     * considered "dry", and will not change any further until the next call to [start].
     */
    public fun isUpdateNeeded(): Boolean = InProgressStrokeNative.isUpdateNeeded(nativePointer)

    /**
     * Copies the current input, brush, and geometry as of the last call to [start] or [updateShape]
     * to a new [Stroke].
     *
     * The resulting [Stroke] will not be modified if further inputs are added to this
     * [InProgressStroke], and a [Stroke] created by another call to this method will not modify or
     * be connected in any way to the prior [Stroke].
     */
    public fun toImmutable(): Stroke {
        return Stroke.wrapNative(
            InProgressStrokeNative.newStrokeFromCopy(nativePointer),
            requireNotNull(brush),
        )
    }

    /**
     * Returns the number of [StrokeInput]s in the stroke so far. This counts all of the real inputs
     * and the most-recently-processed sequence of predicted inputs.
     */
    @IntRange(from = 0)
    public fun getInputCount(): Int = InProgressStrokeNative.getInputCount(nativePointer)

    /* Returns the number of real inputs in the stroke so far, not counting any prediction. */
    @IntRange(from = 0)
    public fun getRealInputCount(): Int = InProgressStrokeNative.getRealInputCount(nativePointer)

    /** Returns the number of inputs in the current stroke prediction. */
    @IntRange(from = 0)
    public fun getPredictedInputCount(): Int =
        InProgressStrokeNative.getPredictedInputCount(nativePointer)

    /**
     * Replace the contents of the [MutableStrokeInputBatch] with the specified range of inputs from
     * the this [InProgressStroke]. By default, all inputs are copied.
     *
     * Returns the passed-in [MutableStrokeInputBatch] to make it easier to chain calls.
     *
     * @return [out]
     */
    @JvmOverloads
    public fun populateInputs(
        out: MutableStrokeInputBatch,
        @IntRange(from = 0) from: Int = 0,
        @IntRange(from = 0) to: Int = getInputCount(),
    ): MutableStrokeInputBatch {
        val size = getInputCount()
        require(from >= 0) { "index ($from) must be >= 0" }
        require(to <= size && to >= from) { "to ($to) must be in [from=$from, inputCount=$size]" }
        InProgressStrokeNative.populateInputs(nativePointer, out.nativePointer, from, to)
        return out
    }

    /**
     * Gets the value of the i-th input and overwrites [out]. Requires that [index] is positive and
     * less than [getInputCount].
     *
     * Returns the passed-in [StrokeInput] to make it easier to chain calls.
     *
     * @return [out]
     */
    public fun populateInput(out: StrokeInput, @IntRange(from = 0) index: Int): StrokeInput {
        val size = getInputCount()
        require(index < size && index >= 0) { "index ($index) must be in [0, inputCount=$size)" }
        InProgressStrokeNative.getAndOverwriteInput(
            nativePointer,
            out,
            index,
            InputToolType::class.java,
        )
        return out
    }

    /**
     * Returns the number of `BrushCoats` for the current brush, or zero if [start] has not been
     * called.
     */
    @IntRange(from = 0)
    public fun getBrushCoatCount(): Int =
        InProgressStrokeNative.getBrushCoatCount(nativePointer).also { check(it >= 0) }

    /** @see getBrushCoatCount */
    @IntRange(from = 0)
    @Deprecated("Renamed to getBrushCoatCount")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun brushCoatCount(): Int = getBrushCoatCount()

    /**
     * Writes to [outBoxAccumulator] the bounding box of the vertex positions of the mesh for brush
     * coat [coatIndex].
     *
     * Returns the passed in [BoxAccumulator] to make it easier to chain calls.
     *
     * @param coatIndex The index of the coat to obtain the bounding box from.
     * @param outMeshBounds The pre-allocated [BoxAccumulator] to be filled with the result.
     * @return [outMeshBounds]
     */
    public fun populateMeshBounds(
        @IntRange(from = 0) coatIndex: Int,
        outMeshBounds: BoxAccumulator,
    ): BoxAccumulator {
        require(coatIndex >= 0 && coatIndex < getBrushCoatCount()) {
            "coatIndex=$coatIndex must be between 0 and brushCoatCount=${getBrushCoatCount()}"
        }
        InProgressStrokeNative.getMeshBounds(nativePointer, coatIndex, outMeshBounds)
        return outMeshBounds
    }

    /**
     * Returns the bounding rectangle of mesh positions added, modified, or removed by calls to
     * [updateShape] since the most recent call to [start] or [resetUpdatedRegion].
     *
     * Returns the passed in [BoxAccumulator] to make it easier to chain calls.
     *
     * @param outUpdatedRegion The pre-allocated [BoxAccumulator] to be filled with the result.
     * @return [outUpdatedRegion]
     */
    public fun populateUpdatedRegion(outUpdatedRegion: BoxAccumulator): BoxAccumulator {
        InProgressStrokeNative.fillUpdatedRegion(nativePointer, outUpdatedRegion)
        return outUpdatedRegion
    }

    /** Call after making use of a value from [populateUpdatedRegion] to reset the accumulation. */
    public fun resetUpdatedRegion(): Unit = InProgressStrokeNative.resetUpdatedRegion(nativePointer)

    /**
     * Returns the number of outlines for the specified brush coat.
     *
     * Calls to functions that accept an outlineIndex must treat the result of this function as an
     * upper bound. Coats with discontinuous geometry will always have multiple outlines, but even
     * continuous geometry may be drawn with multiple overlapping outlines when this improves
     * rendering quality or performance.
     *
     * @param coatIndex Must be between 0 (inclusive) and the result of [getBrushCoatCount]
     *   (exclusive).
     */
    @IntRange(from = 0)
    public fun getOutlineCount(@IntRange(from = 0) coatIndex: Int): Int {
        require(coatIndex >= 0 && coatIndex < getBrushCoatCount()) {
            "coatIndex=$coatIndex must be between 0 and brushCoatCount=${getBrushCoatCount()}"
        }
        return InProgressStrokeNative.getOutlineCount(nativePointer, coatIndex).also {
            check(it >= 0)
        }
    }

    /** @see getOutlineCount */
    @IntRange(from = 0)
    @Deprecated("Renamed to getOutlineCount")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun outlineCount(@IntRange(from = 0) coatIndex: Int): Int = getOutlineCount(coatIndex)

    /**
     * Returns the number of outline points for the specified outline and brush coat.
     * [populateOutlinePosition] must treat the result of this as the upper bound of its
     * outlineVertexIndex parameter.
     *
     * @param coatIndex Must be between 0 (inclusive) and the result of [getBrushCoatCount]
     *   (exclusive).
     * @param outlineIndex Must be between 0 (inclusive) and the result of [getOutlineCount] for the
     *   same [coatIndex] (exclusive).
     */
    @IntRange(from = 0)
    public fun getOutlineVertexCount(
        @IntRange(from = 0) coatIndex: Int,
        @IntRange(from = 0) outlineIndex: Int,
    ): Int {
        require(outlineIndex >= 0 && outlineIndex < getOutlineCount(coatIndex)) {
            "outlineIndex=$outlineIndex must be between 0 and outlineCount=${getOutlineCount(coatIndex)}"
        }
        return InProgressStrokeNative.getOutlineVertexCount(nativePointer, coatIndex, outlineIndex)
            .also { check(it >= 0) }
    }

    /** @see getOutlineVertexCount */
    @Deprecated("Renamed to getOutlineVertexCount")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @IntRange(from = 0)
    public fun outlineVertexCount(
        @IntRange(from = 0) coatIndex: Int,
        @IntRange(from = 0) outlineIndex: Int,
    ): Int = getOutlineVertexCount(coatIndex, outlineIndex)

    /**
     * Fills [outPosition] with the x and y coordinates of the specified outline vertex.
     *
     * Returns the passed-in [MutableVec] to make it easier to chain calls.
     *
     * @param coatIndex Must be between 0 (inclusive) and the result of [getBrushCoatCount]
     *   (exclusive).
     * @param outlineIndex Must be between 0 (inclusive) and the result of [getOutlineCount]
     *   (exclusive) for the same [coatIndex].
     * @param outlineVertexIndex Must be between 0 (inclusive) and the result of
     *   [getOutlineVertexCount] (exclusive) for the same [coatIndex] and [outlineIndex].
     * @param outPosition the pre-allocated [MutableVec] to be filled with the result.
     */
    public fun populateOutlinePosition(
        @IntRange(from = 0) coatIndex: Int,
        @IntRange(from = 0) outlineIndex: Int,
        @IntRange(from = 0) outlineVertexIndex: Int,
        outPosition: MutableVec,
    ): MutableVec {
        val outlineVertexCount = getOutlineVertexCount(coatIndex, outlineIndex)
        require(outlineVertexIndex >= 0 && outlineVertexIndex < outlineVertexCount) {
            "outlineVertexIndex=$outlineVertexIndex must be between 0 and " +
                "outlineVertexCount($outlineVertexIndex)=$outlineVertexCount"
        }
        InProgressStrokeNative.fillOutlinePosition(
            nativePointer,
            coatIndex,
            outlineIndex,
            outlineVertexIndex,
            outPosition,
        )
        return outPosition
    }

    // Internal methods for rendering the MutableMesh(es) of an InProgressStroke. These mesh data
    // accessors are made available via InProgressStroke because the underlying
    // native InProgressStroke manages the memory for its meshes.

    /**
     * Returns the number of individual meshes in the specified brush coat of this stroke.
     *
     * TODO: b/294561921 - Implement multiple meshes. This value is hard coded to 1 in
     *   [in_progress_stroke_jni.cc].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getMeshPartitionCount(@IntRange(from = 0) coatIndex: Int): Int {
        require(coatIndex >= 0 && coatIndex < getBrushCoatCount()) {
            "coatIndex=$coatIndex must be between 0 and brushCoatCount=${getBrushCoatCount()}"
        }
        return InProgressStrokeNative.getMeshPartitionCount(nativePointer, coatIndex)
    }

    /**
     * Gets the number of vertices in the mesh from the mesh at [partitionIndex] for brush coat
     * [coatIndex] which must be less than that coat's [getMeshPartitionCount].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getVertexCount(@IntRange(from = 0) coatIndex: Int, partitionIndex: Int): Int {
        require(partitionIndex in 0 until getMeshPartitionCount(coatIndex)) {
            "Cannot get vertex count at partitionIndex $partitionIndex out of range [0, ${getMeshPartitionCount(coatIndex)})."
        }
        return InProgressStrokeNative.getVertexCount(nativePointer, coatIndex, partitionIndex)
    }

    /**
     * Gets the vertices of the mesh at [partitionIndex] for brush coat [coatIndex] which must be
     * less than that coat's [getMeshPartitionCount].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getRawVertexBuffer(
        @IntRange(from = 0) coatIndex: Int,
        partitionIndex: Int,
    ): ByteBuffer {
        require(partitionIndex in 0 until getMeshPartitionCount(coatIndex)) {
            "Cannot get raw vertex buffer at partitionIndex $partitionIndex out of range [0, ${getMeshPartitionCount(coatIndex)})."
        }
        return (InProgressStrokeNative.getRawVertexData(nativePointer, coatIndex, partitionIndex)
                ?: ByteBuffer.allocate(0))
            .asReadOnlyBuffer()
    }

    /**
     * Gets the triangle indices of the mesh at [partitionIndex] for brush coat [coatIndex] which
     * must be less than that coat's [getMeshPartitionCount].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getRawTriangleIndexBuffer(
        @IntRange(from = 0) coatIndex: Int,
        partitionIndex: Int,
    ): ShortBuffer {
        require(partitionIndex in 0 until getMeshPartitionCount(coatIndex)) {
            "Cannot get raw triangle index buffer at partitionIndex $partitionIndex out of range [0, ${getMeshPartitionCount(coatIndex)})."
        }
        val triangleIndexStride =
            InProgressStrokeNative.getTriangleIndexStride(nativePointer, coatIndex, partitionIndex)
        check(triangleIndexStride == Short.SIZE_BYTES) {
            "Only 16-bit triangle indices are supported, but got stride of $triangleIndexStride"
        }
        // The resulting buffer is writeable, so first make it readonly. Then, because Java
        // ByteBuffers
        // defaults to a fixed endianness instead of using the endianness of the device, insist on
        // ByteOrder.nativeOrder.
        // TODO: b/302535371 - There is a bug in the combined use of .asReadOnlyBuffer() and
        // .order(),
        // such that the returned buffer is NOT readonly.
        return (InProgressStrokeNative.getRawTriangleIndexData(
                nativePointer,
                coatIndex,
                partitionIndex,
            ) ?: ByteBuffer.allocate(0))
            .asReadOnlyBuffer()
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
    }

    /**
     * Gets the [MeshFormat] of the mesh at [partitionIndex] for brush coat [coatIndex] which must
     * be less than that coat's [getMeshPartitionCount].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getMeshFormat(@IntRange(from = 0) coatIndex: Int, partitionIndex: Int): MeshFormat {
        require(partitionIndex in 0 until getMeshPartitionCount(coatIndex)) {
            "Cannot get mesh format at partitionIndex $partitionIndex out of range [0, ${getMeshPartitionCount(coatIndex)})."
        }
        return MeshFormat.wrapNative(
            InProgressStrokeNative.newCopyOfMeshFormat(nativePointer, coatIndex, partitionIndex)
        )
    }

    protected fun finalize() {
        // NOMUTANTS -- Not tested post garbage collection.
        InProgressStrokeNative.free(nativePointer)
    }

    // Declared as a target for extension functions.
    public companion object
}

@UsedByNative
private object InProgressStrokeNative {
    init {
        NativeLoader.load()
    }

    /** Create underlying native object and return reference for all subsequent native calls. */
    @UsedByNative external fun create(): Long

    @UsedByNative external fun clear(nativePointer: Long)

    @UsedByNative external fun start(nativePointer: Long, brushNativePointer: Long, noiseSeed: Int)

    /** Returns whether the inputs were successfully enqueued. */
    @UsedByNative
    external fun enqueueInputs(
        nativePointer: Long,
        realInputsPointer: Long,
        predictedInputsPointer: Long,
    ): Boolean

    /** Returns whether the shape was successfully updated. */
    @UsedByNative external fun updateShape(nativePointer: Long, currentElapsedTime: Long): Boolean

    @UsedByNative external fun finishInput(nativePointer: Long)

    @UsedByNative external fun isInputFinished(nativePointer: Long): Boolean

    @UsedByNative external fun isUpdateNeeded(nativePointer: Long): Boolean

    /** Returns the native pointer for an `ink::Stroke`, to be wrapped by a [Stroke]. */
    @UsedByNative external fun newStrokeFromCopy(nativePointer: Long): Long

    @UsedByNative external fun getInputCount(nativePointer: Long): Int

    @UsedByNative external fun getRealInputCount(nativePointer: Long): Int

    @UsedByNative external fun getPredictedInputCount(nativePointer: Long): Int

    @UsedByNative
    external fun populateInputs(
        nativePointer: Long,
        mutableStrokeInputBatchPointer: Long,
        from: Int,
        to: Int,
    )

    /**
     * The [toolTypeClass] parameter is passed as a convenience to native JNI code, to avoid it
     * needing to do a reflection-based FindClass lookup.
     */
    @UsedByNative
    external fun getAndOverwriteInput(
        nativePointer: Long,
        input: StrokeInput,
        index: Int,
        toolTypeClass: Class<InputToolType>,
    )

    @UsedByNative external fun getBrushCoatCount(nativePointer: Long): Int

    /** Writes the bounding region to [outEnvelope]. */
    @UsedByNative
    external fun getMeshBounds(nativePointer: Long, coatIndex: Int, outEnvelope: BoxAccumulator)

    /** Returns the number of mesh partitions. */
    @UsedByNative external fun getMeshPartitionCount(nativePointer: Long, coatIndex: Int): Int

    /** Returns the number of vertices in the mesh at [partitionIndex]. */
    @UsedByNative
    external fun getVertexCount(nativePointer: Long, coatIndex: Int, partitionIndex: Int): Int

    /**
     * Returns a direct [ByteBuffer] wrapped around the contents of [RawVertexData] for the mesh at
     * [partitionIndex]. It will be writeable, so be sure to only expose a read-only wrapper of it.
     */
    @UsedByNative
    external fun getRawVertexData(
        nativePointer: Long,
        coatIndex: Int,
        partitionIndex: Int,
    ): ByteBuffer?

    /**
     * Returns a direct [ByteBuffer] wrapped around the contents of [RawTriangleData] for the mesh
     * at [partitionIndex]. It will be writeable, so be sure to only expose a read-only wrapper of
     * it.
     */
    @UsedByNative
    external fun getRawTriangleIndexData(
        nativePointer: Long,
        coatIndex: Int,
        partitionIndex: Int,
    ): ByteBuffer?

    @UsedByNative
    external fun getTriangleIndexStride(
        nativePointer: Long,
        coatIndex: Int,
        partitionIndex: Int,
    ): Int

    /**
     * Return the address of a newly allocated copy of the `ink::MeshFormat` belonging to the mesh
     * at [partitionIndex].
     */
    @UsedByNative
    external fun newCopyOfMeshFormat(nativePointer: Long, coatIndex: Int, partitionIndex: Int): Long

    /** Writes the updated region to [outEnvelope]. */
    @UsedByNative external fun fillUpdatedRegion(nativePointer: Long, outEnvelope: BoxAccumulator)

    @UsedByNative external fun resetUpdatedRegion(nativePointer: Long)

    @UsedByNative external fun getOutlineCount(nativePointer: Long, coatIndex: Int): Int

    @UsedByNative
    external fun getOutlineVertexCount(nativePointer: Long, coatIndex: Int, outlineIndex: Int): Int

    @UsedByNative
    external fun fillOutlinePosition(
        nativePointer: Long,
        coatIndex: Int,
        outlineIndex: Int,
        outlineVertexIndex: Int,
        outPosition: MutableVec,
    )

    /** Release the underlying memory allocated in [nativeCreateInProgressStroke]. */
    @UsedByNative external fun free(nativePointer: Long)
}
