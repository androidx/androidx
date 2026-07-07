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

import android.graphics.Canvas
import android.graphics.Matrix

@ExperimentalInkCustomShapeWorkflowApi
/** Implement this interface to render an [InProgressShape]. */
public interface InProgressShapeRenderer<in InProgressShapeT : InProgressShape<*, *>> {
    /**
     * Draw the given [InProgressShape] onto the provided [Canvas], with the given transform. This
     * will be called on the render thread.
     *
     * @param canvas The [Canvas] to draw to.
     * @param shape The [InProgressShape] to draw.
     * @param strokeToScreenTransform A [Matrix] to transform the [InProgressShape] from its local
     *   coordinate space to the screen coordinate space.
     */
    public fun draw(canvas: Canvas, shape: InProgressShapeT, strokeToScreenTransform: Matrix)
}
