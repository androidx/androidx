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

package androidx.ui.flow

import androidx.ui.flow.layers.LayerTree
import androidx.ui.painting.Canvas
import androidx.ui.skia.SkMatrix

class CompositorContext {

//    var raster_cache: RasterCache = RasterCache()
//    var texture_registry: TextureRegistry? = null
//    var frame_time: Stopwatch? = null
//    var engine_time: Stopwatch? = null
//    var frame_count: Counter

    fun AcquireFrame(
//        TODO(Migration/Andrey): Not porting GrContext for now
//        gr_context: GrContext,
        canvas: Canvas,
        root_surface_transformation: SkMatrix,
        instrumentation_enabled: Boolean = false
    ): ScopedFrame {
        return ScopedFrame(this,
//                gr_context,
                canvas,
                root_surface_transformation,
                instrumentation_enabled
        )
    }

//    fun OnGrContextCreated() {
//        texture_registry_.OnGrContextCreated();
//    }
//
//    fun OnGrContextDestroyed() {
//        texture_registry_.OnGrContextDestroyed();
//        raster_cache_.Clear();
//    }

    internal fun BeginFrame(frame: ScopedFrame, enable_instrumentation: Boolean) {
        if (enable_instrumentation) {
//            TODO(Migration/Andrey): Not porting StopWatch for now
//            frame_count_.Increment()
//            frame_time_.Start()
        }
    }

    internal fun EndFrame(frame: ScopedFrame, enable_instrumentation: Boolean) {
//        TODO(Migration/Andrey): Not porting RasterCache for now
//        raster_cache.SweepAfterFrame()
        if (enable_instrumentation) {
//            TODO(Migration/Andrey): Not porting StopWatch for now
//            frame_time_.Stop();
        }
    }

    class ScopedFrame(
        val context: CompositorContext,
//        TODO(Migration/Andrey): Not porting GrContext for now
//        val gr_context: GrContext?,
        val canvas: Canvas,
        val root_surface_transformation: SkMatrix,
        private val instrumentation_enabled: Boolean
    ) {

        init {
            context.BeginFrame(this, instrumentation_enabled)
        }

        fun destructor() {
            context.EndFrame(this, instrumentation_enabled)
        }

        fun Raster(layer_tree: LayerTree, ignore_raster_cache: Boolean = true): Boolean {
            layer_tree.Preroll(this, ignore_raster_cache)
            layer_tree.Paint(this)
            return true
        }
    }
}