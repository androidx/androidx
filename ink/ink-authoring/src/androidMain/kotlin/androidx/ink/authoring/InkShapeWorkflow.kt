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

import androidx.annotation.RestrictTo
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke

/**
 * If [InProgressStrokesView.rendererFactory] can be fully removed someday, then we can just pass in
 * a [androidx.ink.brush.TextureBitmapStore] here.
 */
@ExperimentalCustomShapeWorkflowApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
public class InkShapeWorkflow(customRendererFactory: () -> CanvasStrokeRenderer) :
    ShapeWorkflow<Brush, InkInProgressShape, Stroke> {

    // Only one shape type for now, backed by InProgressStroke. But theoretically, if the underlying
    // resources of InProgressShape are tailored to specific brush types (e.g. particles vs.
    // continuous), then that could be differentiated here for further optimization.
    override fun getShapeType(shapeSpec: Brush): Int = 9_14_11 // INK

    override fun create(shapeType: Int): InkInProgressShape = InkInProgressShape()

    // Creates its own instance of CanvasStrokeRenderer to be used on the render thread.
    override val inProgressShapeRenderer: InProgressShapeRenderer<InkInProgressShape> =
        InkInProgressShapeRenderer(customRendererFactory())

    // Creates its own instance of CanvasStrokeRenderer to be used on the UI thread.
    override val completedShapeRenderer: CompletedShapeRenderer<Stroke> =
        InkCompletedShapeRenderer(customRendererFactory())
}
