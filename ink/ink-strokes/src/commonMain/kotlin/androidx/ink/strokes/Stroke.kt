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
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
     * Erases the [eraserShape] from this stroke and returns the remaining fragments.
     *
     * Each resulting stroke retains the original [inputs] and [brush], but has a newly computed
     * [shape] representing the portion remaining after erasure.
     *
     * @param eraserShape A [PartitionedMesh] representing the geometric region to be erased.
     * @param eraserTransform The [AffineTransform] from eraser coordinates to world coordinates.
     * @param strokeTransform The [AffineTransform] from stroke coordinates to world coordinates.
     * @return The set of [Stroke] fragments remaining after the erasure.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    public fun partialErase(
        eraserShape: PartitionedMesh,
        eraserTransform: AffineTransform,
        strokeTransform: AffineTransform,
    ): Set<Stroke> =
        MultipleStrokes.createWithPartialErase(this, eraserShape, eraserTransform, strokeTransform)

    public companion object {
        /** Construct a [Stroke] from an unowned heap-allocated native pointer to a C++ `Stroke`. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(brush: Brush, nativeAlloc: () -> Long): Stroke {
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

    fun free(nativePointer: Long)
}

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
        fun createWithPartialErase(
            targetStroke: Stroke,
            eraserShape: PartitionedMesh,
            eraserTransform: AffineTransform,
            strokeTransform: AffineTransform,
        ): Set<Stroke> =
            MultipleStrokes(targetStroke.brush) {
                    MultipleStrokesNative.createWithPartialErase(
                        targetStroke.nativePointer,
                        eraserShape.nativePointer,
                        eraserTransform.m00,
                        eraserTransform.m10,
                        eraserTransform.m20,
                        eraserTransform.m01,
                        eraserTransform.m11,
                        eraserTransform.m21,
                        strokeTransform.m00,
                        strokeTransform.m10,
                        strokeTransform.m20,
                        strokeTransform.m01,
                        strokeTransform.m11,
                        strokeTransform.m21,
                    )
                }
                .releaseStrokes()
    }
}

internal expect object MultipleStrokesNative {

    fun createWithPartialErase(
        targetStrokePointer: Long,
        eraserShapePointer: Long,
        eraserA: Float,
        eraserB: Float,
        eraserC: Float,
        eraserD: Float,
        eraserE: Float,
        eraserF: Float,
        strokeA: Float,
        strokeB: Float,
        strokeC: Float,
        strokeD: Float,
        strokeE: Float,
        strokeF: Float,
    ): Long

    fun getStrokeCount(nativePointer: Long): Int

    fun releaseStroke(nativePointer: Long, index: Int): Long

    fun free(nativePointer: Long)
}
