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
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.geometry.PartitionedMesh
import androidx.ink.nativeloader.NativeLoader

/**
 * An immutable object comprised of a [StrokeInputBatch] that represents a user-drawn (or sometimes
 * synthetic) path, a [Brush] that contains information on how that path should be converted into a
 * geometric shape and rendered on screen, and a [PartitionedMesh] which is the geometric shape
 * calculated from the combination of the [StrokeInputBatch] and the [Brush].
 *
 * This can be constructed directly from a [StrokeInputBatch] that has already been completed. To
 * construct a stroke incrementally and render it as input events are received in real time, use
 * [InProgressStrokesView] or [InProgressStroke], which will ultimately return a [Stroke] when input
 * is completed.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
public class Stroke {

    /**
     * Contains information on how the [inputs] should be used to calculate the [shape] and how that
     * [shape] should be drawn on screen.
     */
    public val brush: Brush

    /** The user-drawn (or perhaps synthetically generated) path that this [Stroke] takes. */
    public val inputs: ImmutableStrokeInputBatch

    /**
     * The geometric shape of the [Stroke], which can be used to render it on screen and to perform
     * geometric calculations. This [PartitionedMesh] will have one render group per brush coat in
     * [brush].
     */
    public val shape: PartitionedMesh

    /**
     * This is the raw pointer address of an `ink::Stroke` that has been heap allocated to be owned
     * solely by this JVM [Stroke] object. Although the `ink::Stroke` is owned exclusively by this
     * [Stroke] object, it may be a copy of another `ink::Stroke`, where it has a copy of fairly
     * lightweight metadata but shares ownership of the more heavyweight `ink::Mesh` objects. This
     * class is responsible for freeing the `ink::Stroke`, usually through its [finalize] method but
     * possibly by an explicit [close].
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var nativeAddress: Long
        private set

    /** Construct a [Stroke] given a [Brush] and a [StrokeInputBatch], generating its [shape]. */
    public constructor(brush: Brush, inputs: StrokeInputBatch) {
        this.brush = brush
        this.nativeAddress =
            StrokeJni.createWithBrushAndInputs(brush.nativePointer, inputs.nativePointer)
        this.shape = PartitionedMesh(StrokeJni.allocShallowCopyOfShape(this.nativeAddress))
        this.inputs = inputs.asImmutable()
    }

    /**
     * Returns a [Stroke] with the brush replaced. This may or may not affect the [shape], but will
     * not change the [inputs].
     */
    public fun copy(brush: Brush): Stroke {
        // For a pure copy, return the same object because it is immutable.
        if (brush == this.brush) return this

        // TODO: b/308980197 - Detect when the mesh format cannot support the intended brush and
        // regenerate the shape in that case.
        return if (brushWouldHaveDifferentShape(brush)) {
            Stroke(brush, this.inputs)
        } else {
            // Rendering caches use instance comparisons to identify re-usable shapes in the cache.
            // If a
            // new stroke has an unchanged shape, use the same instance of [PartitionedMesh] in the
            // new
            // [Stroke].
            Stroke(brush, this.inputs, this.shape)
        }
    }

    /**
     * Returns true if using the given [brush] instead of the current one would result in a
     * different [PartitionedMesh].
     */
    private fun brushWouldHaveDifferentShape(brush: Brush): Boolean {
        if (
            this.brush.size != brush.size ||
                this.brush.epsilon != brush.epsilon ||
                this.brush.family.coats.size != brush.family.coats.size
        ) {
            return true
        }
        for (i in 0 until this.brush.family.coats.size) {
            if (this.brush.family.coats[i].tips != brush.family.coats[i].tips) {
                return true
            }
        }
        return false
    }

    /**
     * Wrap an existing native `ink::Stroke` with a [Stroke]. This is used by internal utilities
     * that create strokes in native code, such as deserialization. Never pass in a [nativeAddress]
     * that is already owned by another [Stroke], or that underlying `ink::Stroke` will be freed
     * twice.
     */
    internal constructor(nativeAddress: Long, brush: Brush) {
        val shape = PartitionedMesh(StrokeJni.allocShallowCopyOfShape(nativeAddress))
        require(shape.getRenderGroupCount() == brush.family.coats.size) {
            "The shape must have one render group per brush coat, but found ${shape.getRenderGroupCount()} render groups in shape and ${brush.family.coats.size} brush coats in brush."
        }
        this.nativeAddress = nativeAddress
        this.brush = brush
        this.inputs = ImmutableStrokeInputBatch(StrokeJni.allocShallowCopyOfInputs(nativeAddress))
        this.shape = shape
    }

    /**
     * Construct a [Stroke] given a [Brush], a [StrokeInputBatch], and a [PartitionedMesh].
     *
     * Note that this does not do any validation that [brush] and [inputs] together would produce
     * [shape]. This constructor is primarily intended for deserialization, in cases where the
     * [PartitionedMesh] is being stored in addition to the [Brush] and [StrokeInputBatch].
     */
    public constructor(brush: Brush, inputs: StrokeInputBatch, shape: PartitionedMesh) {
        require(shape.getRenderGroupCount() == brush.family.coats.size) {
            "The shape must have one render group per brush coat, but found ${shape.getRenderGroupCount()} render groups in shape and ${brush.family.coats.size} brush coats in brush."
        }
        this.brush = brush
        this.shape = shape
        this.nativeAddress =
            StrokeJni.createWithBrushInputsAndShape(
                brush.nativePointer,
                inputs.nativePointer,
                shape.getNativeAddress(),
            )
        this.inputs = inputs.asImmutable()
    }

    protected fun finalize() {
        // NOMUTANTS--Not tested post garbage collection.
        if (nativeAddress == 0L) return
        StrokeJni.free(nativeAddress)
        nativeAddress = 0L
    }

    public override fun toString(): String {
        return "Stroke(brush=$brush, inputs=$inputs, shape=$shape)"
    }

    /** Declared as a target for extension functions. */
    public companion object
}

/**
 * Singleton wrapper around native JNI calls.
 *
 * The alternative to this is putting the methods in [Stroke] itself (passes down an unused
 * `jobject`, and doesn't work for native calls used by constructors), or in [Stroke.Companion]
 * (makes the `JNI_METHOD` naming less clear).
 */
private object StrokeJni {
    init {
        NativeLoader.load()
    }

    external fun createWithBrushAndInputs(
        brushNativePointer: Long,
        inputs: Long
    ): Long // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun createWithBrushInputsAndShape(
        brushNativePointer: Long,
        inputs: Long,
        shape: Long,
    ): Long

    /**
     * Returns the address of a new `ink::StrokeInputBatch` that is a shallow copy of the inputs
     * belonging to the `ink::Stroke` given by the [nativeAddress].
     */
    external fun allocShallowCopyOfInputs(
        nativeAddress: Long
    ): Long // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    /**
     * Returns the address of a new `ink::ModeledShape` that is a shallow copy of the shape
     * belonging to the `ink::Stroke` given by the [nativeAddress].
     */
    external fun allocShallowCopyOfShape(
        nativeAddress: Long
    ): Long // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    /** Deletes the `ink::Stroke` given by the [nativeAddress]. */
    external fun free(
        nativeAddress: Long
    ) // TODO: b/355248266 - @Keep must go in Proguard config file instead.
}
