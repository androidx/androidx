/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.flow.layers

import androidx.ui.engine.geometry.Rect
import androidx.ui.skia.SkMatrix

/**
 * Represents a single composited layer. Created on the UI thread but then
 * subquently used on the Rasterizer thread.
 */
abstract class Layer {

    var parent: ContainerLayer? = null
    var needs_system_composite: Boolean = false
    // This must be set by the time Preroll() returns otherwise the layer will
    // be assumed to have empty paint bounds (paints no content).
    var paint_bounds: Rect = Rect.zero

    open fun Preroll(context: PrerollContext, matrix: SkMatrix) {}

//    // Calls SkCanvas::saveLayer and restores the layer upon destruction. Also
//    // draws a checkerboard over the layer if that is enabled in the PaintContext.
//    class AutoSaveLayer {
//        fun AutoSaveLayer(const PaintContext& paint_context,
//        const SkRect& bounds,
//        const SkPaint* paint)
//        : paint_context_(paint_context), bounds_(bounds) {
//            paint_context_.canvas.saveLayer(bounds_, paint);
//        }
//
//        fun AutoSaveLayer(const PaintContext& paint_context,
//        const SkCanvas::SaveLayerRec& layer_rec)
//        : paint_context_(paint_context), bounds_(*layer_rec.fBounds) {
//            paint_context_.canvas.saveLayer(layer_rec);
//        }
//
//
//        fun ~AutoSaveLayer() {
//            if (paint_context_.checkerboard_offscreen_layers) {
//                DrawCheckerboard(& paint_context_ . canvas, bounds_);
//            }
//            paint_context_.canvas.restore();
//        }
//
//        private:
//        const PaintContext& paint_context_;
//        const SkRect bounds_;
//    };

    abstract fun Paint(context: PaintContext)

//    #if defined(OS_FUCHSIA)
//    // Updates the system composited scene.
//    virtual void UpdateScene(SceneUpdateContext& context) {}
//    #endif

    val needs_painting: Boolean get() = !paint_bounds.isEmpty()
}