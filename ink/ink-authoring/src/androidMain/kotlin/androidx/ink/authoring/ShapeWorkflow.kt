/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread

/**
 * An interface that can be implemented to provide custom inking functionality which replaces or
 * augments the standard Jetpack Ink behavior. Providing an implementation of this is an advanced
 * feature that isn't recommended for most apps - most use cases should prefer to use
 * [androidx.ink.brush.StockBrushes], or perhaps create a custom brush with
 * [androidx.ink.storage.BrushFamilySerialization.decode]. But if those don't satisfy the need for
 * fully custom programmatic geometry of stroke-like objects based on pointer input data, this
 * interface can be implemented to control that, but in a way that can still take advantage of Ink's
 * low latency drawing capabilities without needing to implement that from scratch.
 *
 * The primary purpose of this object is to manage the recycling lifecycle of [InProgressShape]
 * instances. A new instance will only be requested via [create] when the internal recycling logic
 * determines it's necessary, and most of the time it will reuse existing recycled instances via
 * [InProgressShape.start].
 *
 * @param ShapeSpecT A styling specification type, typically pure declarative data, that indicates
 *   how pointer inputs are turned into geometry and rendered pixels. A standard, pure Ink
 *   implementation would use [androidx.ink.brush.Brush] for this.
 * @param InProgressShapeT A specific type of [InProgressShape] that includes the logic for how a
 *   [ShapeSpecT] and incrementally added pointer events are turned into geometry and rendered
 *   pixels. A standard, pure Ink implementation would be based on
 *   [androidx.ink.strokes.InProgressStroke].
 * @param CompletedShapeT A type that represents the end result of an [InProgressShapeT], after all
 *   inputs have been delivered to it and it has been fully processed. This is often different from
 *   an [InProgressShapeT] as it can be immutable, and may therefore represent its underlying data
 *   in a more optimized form. A standard, pure Ink implementation would use
 *   [androidx.ink.strokes.Stroke] for this.
 */
@ExperimentalCustomShapeWorkflowApi
public interface ShapeWorkflow<
    in ShapeSpecT : Any,
    InProgressShapeT : InProgressShape<ShapeSpecT, CompletedShapeT>,
    CompletedShapeT : Any,
> {

    /**
     * Return the shape type of an item being drawn with the given [ShapeSpecT] for the purposes of
     * recycling the [InProgressShapeT] instances. The simplest implementation of this can use a
     * single shape type, but multiple shape types are supported. The returned integer value serves
     * as a unique identifier - there are no limits on its value, and the numeric value of one type
     * compared to another is meaningless beyond the fact that they are distinct.
     */
    @WorkerThread public fun getShapeType(shapeSpec: ShapeSpecT): Int

    /**
     * Called when a new [InProgressShape] is needed for the given [shapeType] to represent a shape.
     *
     * The new [InProgressShape] will be used to represent a particular in-progress drawn object
     * using [InProgressShape.start]. Since this object will be reused more than once, it is a good
     * idea to do more expensive initialization once in this function at creation time to avoid
     * unnecessary work in [InProgressShape.start] and the functions that are called at a high
     * frequency in [InProgressShape] like [InProgressShape.enqueueInputs] and
     * [InProgressShape.update].
     */
    @WorkerThread public fun create(shapeType: Int): InProgressShapeT

    /**
     * An object that is called to render an [InProgressShapeT] instance to an
     * [android.graphics.Canvas]. A standard, pure Ink implementation would be based on
     * [androidx.ink.rendering.android.canvas.CanvasStrokeRenderer].
     */
    @get:WorkerThread public val inProgressShapeRenderer: InProgressShapeRenderer<InProgressShapeT>

    /**
     * An object that is called to render a [CompletedShapeT] instance to an
     * [android.graphics.Canvas]. A standard, pure Ink implementation would be based on
     * [androidx.ink.rendering.android.canvas.CanvasStrokeRenderer].
     */
    @get:UiThread public val completedShapeRenderer: CompletedShapeRenderer<CompletedShapeT>
}
