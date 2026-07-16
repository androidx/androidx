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
import androidx.ink.brush.Brush
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.PartitionedMesh
import androidx.ink.nativeloader.InkInternalOnlyApi
import androidx.ink.nativeloader.NativePointer

/**
 * An immutable object comprised of a [StrokeInputBatch] that represents a user-drawn (or sometimes
 * synthetic) path, a [Brush] that contains information on how that path should be converted into a
 * geometric shape and rendered on screen, and a [PartitionedMesh] which is the geometric shape
 * calculated from the combination of the [StrokeInputBatch] and the [Brush].
 *
 * This can be constructed directly from a [StrokeInputBatch] that has already been completed. To
 * construct a stroke incrementally and render it as input events are received in real time, use
 * `androidx.ink.authoring.InProgressStrokesView` or [InProgressStroke], which will ultimately
 * return a [Stroke] when input is completed.
 */
@OptIn(InkInternalOnlyApi::class)
public class Stroke
private constructor(
    nativeAlloc: () -> Long,
    /**
     * Contains information on how the [inputs] should be used to calculate the [shape] and how that
     * [shape] should be drawn on screen.
     */
    public val brush: Brush,
    inputs: StrokeInputBatch? = null,
    shape: PartitionedMesh? = null,
) {
    /**
     * This is the raw pointer address of a heap-allocated native `Stroke` owned solely by this
     * [Stroke] object, though that may share ownership of the underlying mesh data with other
     * similar (e.g. created by copying) strokes.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @InkInternalOnlyApi
    public val nativePointer: Long by NativePointer(nativeAlloc, StrokeNative::free)

    /** The user-drawn (or perhaps synthetically generated) path that this [Stroke] takes. */
    public val inputs: ImmutableStrokeInputBatch =
        // If the inputs were passed to the constructor, use them.
        inputs?.toImmutable()
            // Otherwise, copy them from the native object.
            ?: ImmutableStrokeInputBatch.wrapNative {
                StrokeNative.newShallowCopyOfInputs(nativePointer)
            }

    /**
     * The geometric shape of the [Stroke], which can be used to render it on screen and to perform
     * geometric calculations. This [PartitionedMesh] will have one render group per brush coat in
     * [brush].
     */
    public val shape: PartitionedMesh =
        // If the mesh was passed to the constructor, use it.
        shape
            // Otherwise, copy it from the native object.
            ?: PartitionedMesh.wrapNative { StrokeNative.newShallowCopyOfShape(nativePointer) }

    init {
        require(this.shape.getRenderGroupCount() == brush.family.coats.size) {
            "The shape must have one render group per brush coat, but found " +
                "${this.shape.getRenderGroupCount()} render groups in shape and ${brush.family.coats.size} " +
                "brush coats in brush."
        }
    }

    /**
     * Construct a [Stroke] given a [Brush], a [StrokeInputBatch], and a [PartitionedMesh].
     *
     * Note that this does not do any validation that [brush] and [inputs] together would produce
     * [shape]. This constructor is primarily intended for deserialization, in cases where the
     * [PartitionedMesh] is being stored in addition to the [Brush] and [StrokeInputBatch].
     */
    public constructor(
        brush: Brush,
        inputs: StrokeInputBatch,
        shape: PartitionedMesh,
    ) : this(
        {
            StrokeNative.createWithBrushInputsAndShape(
                brush.nativePointer,
                inputs.nativePointer,
                shape
                    .also {
                        require(it.getRenderGroupCount() == brush.family.coats.size) {
                            "The shape must have one render group per brush coat, but found " +
                                "${it.getRenderGroupCount()} render groups in shape and " +
                                "${brush.family.coats.size} brush coats in brush."
                        }
                    }
                    .nativePointer,
            )
        },
        brush,
        inputs,
        shape,
    )

    /** Construct a [Stroke] given a [Brush] and a [StrokeInputBatch], generating its [shape]. */
    public constructor(
        brush: Brush,
        inputs: StrokeInputBatch,
    ) : this(
        { StrokeNative.createWithBrushAndInputs(brush.nativePointer, inputs.nativePointer) },
        brush,
        inputs,
    )

    /**
     * Returns a [Stroke] with the brush replaced. This may or may not affect the [shape], but will
     * not change the [inputs].
     */
    public fun copy(brush: Brush): Stroke =
        when {
            // For a pure copy, return the same object because it is immutable.
            brush == this.brush -> this
            otherBrushRequiresDifferentMesh(brush) -> Stroke(brush, this.inputs)
            // Rendering caches use instance comparisons to identify re-usable shapes in the cache.
            // If a
            // new stroke has an unchanged shape, use the same instance of [PartitionedMesh] in the
            // new
            // [Stroke].
            else -> Stroke(brush, this.inputs, this.shape)
        }

    /**
     * Returns true if using the given [brush] instead of the current one would result in a
     * different [PartitionedMesh].
     */
    private fun otherBrushRequiresDifferentMesh(otherBrush: Brush): Boolean {
        if (
            brush.size != otherBrush.size ||
                brush.epsilon != otherBrush.epsilon ||
                brush.family.coats.size != otherBrush.family.coats.size ||
                brush.family.inputModel != otherBrush.family.inputModel
        ) {
            return true
        }
        for (i in 0 until brush.family.coats.size) {
            if (brush.family.coats[i].tip != otherBrush.family.coats[i].tip) {
                return true
            }
            if (
                !otherBrush.family.coats[i].isCompatibleWithMeshFormat(shape.renderGroupFormat(i))
            ) {
                return true
            }
        }
        return false
    }

    public override fun toString(): String {
        return "Stroke(brush=$brush, inputs=$inputs, shape=$shape)"
    }

    /**
     * Subtracts [maskShape] from this stroke and returns the remaining portion as a new [Stroke].
     *
     * The returned stroke has a newly computed [shape] representing the shape after subtraction,
     * but retains the original [inputs] and [brush]. The stroke can have its brush color updated
     * (with [copy]), but modifying other properties (like brush size) will revert the stroke to its
     * original shape and undo the subtraction. The stroke may be empty or contain disconnected
     * geometry -- to separate disconnected regions into independent [Stroke] instances, call
     * [split].
     *
     * Note that [subtract] can be a computationally expensive geometric operation, and should
     * generally be performed on a background (worker) thread to avoid blocking the UI thread.
     *
     * @param maskShape A [PartitionedMesh] representing the geometric region to be subtracted.
     * @param maskToWorldTransform The [AffineTransform] from mask coordinates to world coordinates.
     * @param strokeToWorldTransform The [AffineTransform] from stroke coordinates to world
     *   coordinates.
     * @return The [Stroke] remaining after the subtraction.
     */
    @ExperimentalInkEraserApi
    // TODO(b/534309122): Add WorkerThread annotation once that's supported in KMP-common.
    public fun subtract(
        maskShape: PartitionedMesh,
        maskToWorldTransform: AffineTransform,
        strokeToWorldTransform: AffineTransform,
    ): Stroke =
        Stroke.wrapNative(brush) {
            StrokeNative.createWithSubtract(
                nativePointer,
                maskShape.nativePointer,
                maskToWorldTransform.getM00(),
                maskToWorldTransform.getM10(),
                maskToWorldTransform.getM20(),
                maskToWorldTransform.getM01(),
                maskToWorldTransform.getM11(),
                maskToWorldTransform.getM21(),
                strokeToWorldTransform.getM00(),
                strokeToWorldTransform.getM10(),
                strokeToWorldTransform.getM20(),
                strokeToWorldTransform.getM01(),
                strokeToWorldTransform.getM11(),
                strokeToWorldTransform.getM21(),
            )
        }

    /**
     * Splits this stroke into a set of spatially disconnected [Stroke]s.
     *
     * Two regions of the stroke are considered disconnected if they are further than [tolerance]
     * distance apart, applying the given [strokeToWorldTransform].
     *
     * The [tolerance] parameter is useful to prevent the over-splitting of strokes. This helps
     * maintain continuity in strokes that inherently contain geometric gaps (such as particle
     * brushes), or in solid strokes where minor unintentional gaps are introduced (for example by
     * glancing erases). For splitting a stroke that has been partially erased (with [subtract]) by
     * eraser strokes, it may be natural to set [tolerance] to the size of the eraser brush.
     *
     * Each resulting stroke has a new shape representing its portion, but retains the original
     * [inputs] and [brush]. Each returned stroke can have its brush color updated (with brush
     * [copy]), but modifying other properties (like brush size) will revert the stroke to the shape
     * of the original stroke.
     *
     * Note that [split] can be a computationally expensive geometric operation, and should
     * generally be performed on a background (worker) thread to avoid blocking the UI thread.
     *
     * @param strokeToWorldTransform The [AffineTransform] from stroke coordinates to world
     *   coordinates.
     * @param tolerance The maximum distance in world coordinates between two points to consider
     *   them connected.
     */
    @ExperimentalInkEraserApi
    // TODO(b/534309122): Add WorkerThread annotation once that's supported in KMP-common.
    public fun split(strokeToWorldTransform: AffineTransform, tolerance: Float): Set<Stroke> =
        MultipleStrokes.createWithSplit(this, strokeToWorldTransform, tolerance)

    public companion object {
        /** Construct a [Stroke] from an unowned heap-allocated native pointer to a C++ `Stroke`. */
        internal fun wrapNative(brush: Brush, nativeAlloc: () -> Long): Stroke {
            return Stroke(nativeAlloc, brush)
        }
    }
}

/** Singleton wrapper around native JNI calls. */
internal expect object StrokeNative {
    fun createWithBrushAndInputs(brushNativePointer: Long, inputs: Long): Long

    fun createWithBrushInputsAndShape(brushNativePointer: Long, inputs: Long, shape: Long): Long

    /**
     * Returns the address of a new native `StrokeInputBatch` that is a shallow copy of the inputs
     * belonging to the `Stroke` at [nativePointer].
     */
    fun newShallowCopyOfInputs(nativePointer: Long): Long

    /**
     * Returns the address of a new native `PartitionedMesh` that is a shallow copy of the shape
     * belonging to the `Stroke` at [nativePointer].
     */
    fun newShallowCopyOfShape(nativePointer: Long): Long

    fun createWithSubtract(
        targetStrokePointer: Long,
        maskShapePointer: Long,
        maskA: Float,
        maskB: Float,
        maskC: Float,
        maskD: Float,
        maskE: Float,
        maskF: Float,
        strokeA: Float,
        strokeB: Float,
        strokeC: Float,
        strokeD: Float,
        strokeE: Float,
        strokeF: Float,
    ): Long

    fun free(nativePointer: Long)
}

@OptIn(InkInternalOnlyApi::class)
internal class MultipleStrokes
private constructor(private val brush: Brush, pointerAlloc: () -> Long) {

    private val nativePointer by NativePointer(pointerAlloc, MultipleStrokesNative::free)

    private fun releaseStrokes(): Set<Stroke> = buildSet {
        for (i in 0 until MultipleStrokesNative.getStrokeCount(nativePointer)) {
            add(
                Stroke.wrapNative(brush) {
                    MultipleStrokesNative.releaseStroke(nativePointer, i).also {
                        check(it != 0L) { "releaseStrokes can only be called once." }
                    }
                }
            )
        }
    }

    companion object {
        fun createWithSplit(
            targetStroke: Stroke,
            transform: AffineTransform,
            tolerance: Float,
        ): Set<Stroke> =
            MultipleStrokes(targetStroke.brush) {
                    MultipleStrokesNative.createWithSplit(
                        targetStroke.nativePointer,
                        transform.getM00(),
                        transform.getM10(),
                        transform.getM20(),
                        transform.getM01(),
                        transform.getM11(),
                        transform.getM21(),
                        tolerance,
                    )
                }
                .releaseStrokes()
    }
}

internal expect object MultipleStrokesNative {

    fun createWithSplit(
        targetStrokePointer: Long,
        transformA: Float,
        transformB: Float,
        transformC: Float,
        transformD: Float,
        transformE: Float,
        transformF: Float,
        tolerance: Float,
    ): Long

    fun getStrokeCount(nativePointer: Long): Int

    fun releaseStroke(nativePointer: Long, index: Int): Long

    fun free(nativePointer: Long)
}
