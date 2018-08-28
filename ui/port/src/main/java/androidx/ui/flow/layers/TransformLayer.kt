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

class TransformLayer : ContainerLayer() {

    var transform: SkMatrix? = null

    override fun Preroll(context: PrerollContext, matrix: SkMatrix) {
        val child_matrix = SkMatrix.concat(matrix, transform!!)
        val child_paint_bounds =
                PrerollChildren(context, child_matrix, Rect.zero)

        paint_bounds = transform!!.mapRect(child_paint_bounds)
    }

    override fun Paint(context: PaintContext) {
//        TRACE_EVENT0("flutter", "TransformLayer::Paint");
        assert(needs_painting) // FML_DCHECK(needs_painting())

//        TODO(Migration/Andrey): Not porting SkAutoCanvasRestore for now
//        SkAutoCanvasRestore save(context.canvas, true)
        context.canvas.concat(transform!!)
        PaintChildren(context)
    }

//    #if defined(OS_FUCHSIA)
//    void UpdateScene(SceneUpdateContext& context) override {
//        FML_DCHECK(needs_system_composite());
//
//        SceneUpdateContext::Transform transform(context, transform_);
//        UpdateSceneChildren(context);
//    }
//    #endif  // defined(OS_FUCHSIA)
}