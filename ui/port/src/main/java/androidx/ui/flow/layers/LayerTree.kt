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
import androidx.ui.flow.CompositorContext
import androidx.ui.skia.SkMatrix

class LayerTree {

    var root_layer: Layer? = null

    // The number of frame intervals missed after which the compositor must
    // trace the rasterized picture to a trace file. Specify 0 to disable all
    // tracing
    var rasterizer_tracing_threshold: Int = 0

    var checkerboard_raster_cache_images: Boolean = false
    var checkerboard_offscreen_layers: Boolean = false

    //    SkISize frame_size_;  // Physical pixels.
    //    fml::TimeDelta construction_time_;

    fun Preroll(frame: CompositorContext.ScopedFrame, ignore_raster_cache: Boolean) {
//        TRACE_EVENT0("flutter", "LayerTree::Preroll");

//        TODO(Migration/Andrey): Not porting SkColorSpace for now
//        val color_space: SkColorSpace? =
//        frame.canvas() ? frame.canvas()->imageInfo().colorSpace() : nullptr

//        TODO(Migration/Andrey): Not porting RasterCache for now
//        frame.context.raster_cache.SetCheckboardCacheImages(checkerboard_raster_cache_images)
        val context = PrerollContext(
//            if (ignore_raster_cache) null else frame.context.raster_cache,
//            TODO(Migration/Andrey): Not porting GrContext for now
//            frame.gr_context,
//            color_space,
            Rect.zero
        )

        root_layer!!.Preroll(context, SkMatrix.I())
    }

//    #if defined(OS_FUCHSIA)
//    fun UpdateScene(SceneUpdateContext& context,
//    scenic::ContainerNode& container) {
//        TRACE_EVENT0("flutter", "LayerTree::UpdateScene");
//        const auto& metrics = context.metrics();
//        SceneUpdateContext::Transform transform(context,                  // context
//        1.0f / metrics->scale_x,  // X
//        1.0f / metrics->scale_y,  // Y
//        1.0f / metrics->scale_z   // Z
//        );
//        SceneUpdateContext::Frame frame(
//                context,
//        SkRRect::MakeRect(
//                SkRect::MakeWH(frame_size_.width(), frame_size_.height())),
//        SK_ColorTRANSPARENT, 0.f);
//        if (root_layer->needs_system_composite()) {
//            root_layer->UpdateScene(context);
//        }
//        if (root_layer->needs_painting()) {
//            frame.AddPaintedLayer(root_layer.get());
//        }
//        container.AddChild(transform.entity_node());
//    }
//    #endif

    fun Paint(frame: CompositorContext.ScopedFrame) {
//        TRACE_EVENT0("flutter", "LayerTree::Paint");
        val context = PaintContext(
            frame.canvas,
            frame.root_surface_transformation,
//            TODO(Migration/Andrey): Not porting Stopwatch and TextureRegistry for now
//            frame.context.frame_time,
//            frame.context.engine_time,
//            frame.context.texture_registry,
            checkerboard_offscreen_layers
        )

        if (root_layer!!.needs_painting) {
            root_layer!!.Paint(context)
        }
    }

//    sk_sp<SkPicture> Flatten(const SkRect& bounds) {
//        TRACE_EVENT0("flutter", "LayerTree::Flatten");
//
//        SkPictureRecorder recorder;
//        auto canvas = recorder.beginRecording(bounds);
//
//        if (!canvas) {
//            return nullptr;
//        }
//
//        Layer::PrerollContext preroll_context{
//            nullptr,              // raster_cache (don't consult the cache)
//            nullptr,              // gr_context  (used for the raster cache)
//            nullptr,              // SkColorSpace* dst_color_space
//            SkRect::MakeEmpty(),  // SkRect child_paint_bounds
//        };
//
//        const Stopwatch unused_stopwatch;
//        TextureRegistry unused_texture_registry;
//        SkMatrix root_surface_transformation;
//        // No root surface transformation. So assume identity.
//        root_surface_transformation.reset();
//
//        Layer::PaintContext paint_context = {
//            *canvas,                      // canvas
//            root_surface_transformation,  // root surface transformation
//            unused_stopwatch,             // frame time (dont care)
//            unused_stopwatch,             // engine time (dont care)
//            unused_texture_registry,      // texture registry (not supported)
//            false                         // checkerboard offscreen layers
//        };
//
//        // Even if we don't have a root layer, we still need to create an empty
//        // picture.
//        if (root_layer) {
//            root_layer->Preroll(&preroll_context, SkMatrix::I());
//            // The needs painting flag may be set after the preroll. So check it after.
//            if (root_layer->needs_painting()) {
//                root_layer->Paint(paint_context);
//            }
//        }
//
//        return recorder.finishRecordingAsPicture();
//    }
//    const SkISize& frame_size() const { return frame_size_; }
//
//    fun set_frame_size(const SkISize& frame_size) { frame_size_ = frame_size; }
//
//    fun set_construction_time(const fml::TimeDelta& delta) {
//        construction_time_ = delta;
//    }
//
//    const fml::TimeDelta& construction_time() const { return construction_time_; }
}