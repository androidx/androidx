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

abstract class ContainerLayer : Layer() {

    val layers = mutableListOf<Layer>()

    fun Add(layer: Layer) {
        layer.parent = this
        layers.add(layer)
    }

    override fun Preroll(context: PrerollContext, matrix: SkMatrix) {
//        TRACE_EVENT0("flutter", "ContainerLayer::Preroll");
        val child_paint_bounds = PrerollChildren(context, matrix, Rect.zero)
        paint_bounds = child_paint_bounds
    }

//    #if defined(OS_FUCHSIA)
//    void UpdateScene(SceneUpdateContext& context) override;
//    #endif  // defined(OS_FUCHSIA)

    protected open fun PrerollChildren(
        context: PrerollContext,
        child_matrix: SkMatrix,
        child_paint_bounds: Rect
    ): Rect {
        var child_bounds = child_paint_bounds
        for (layer in layers) {
            val child_context = context
            layer.Preroll(child_context, child_matrix)

            if (layer.needs_system_composite) {
                needs_system_composite = true
            }
            child_bounds = child_bounds.join(layer.paint_bounds)
        }
        return child_bounds
    }

    protected fun PaintChildren(context: PaintContext) {
        assert(needs_painting) // FML_DCHECK(needs_painting());

        // Intentionally not tracing here as there should be no self-time
        // and the trace event on this common function has a small overhead.
        for (layer in layers) {
            if (layer.needs_painting) { layer.Paint(context) }
        }
    }

//    #if defined(OS_FUCHSIA)
//
//    void UpdateScene(SceneUpdateContext& context) {
//        UpdateSceneChildren(context);
//    }
//
//    void UpdateSceneChildren(SceneUpdateContext& context) {
//        FML_DCHECK(needs_system_composite());
//
//        // Paint all of the layers which need to be drawn into the container.
//        // These may be flattened down to a containing
//        for (auto& layer : layers_) {
//            if (layer->needs_system_composite()) {
//            layer->UpdateScene(context);
//        }
//        }
//    }
//
//    #endif  // defined(OS_FUCHSIA)
}