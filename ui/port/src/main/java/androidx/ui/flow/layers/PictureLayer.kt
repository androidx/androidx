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

import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Picture
import androidx.ui.skia.SkMatrix

class PictureLayer : Layer() {

    override fun Preroll(context: PrerollContext, matrix: SkMatrix) {
        val sk_picture = picture

//        TODO(Migration/Andrey): Not porting RasterCache for now
//        val cache = context.raster_cache
//        if (cache != null) {
//            val ctm = matrix
//            ctm.postTranslate(offset_.x(), offset_.y());
//            #ifndef SUPPORT_FRACTIONAL_TRANSLATION
//                    ctm = RasterCache::GetIntegralTransCTM(ctm);
//            #endif
//            raster_cache_result_ = cache->GetPrerolledImage(
//            context->gr_context, sk_picture, ctm, context->dst_color_space,
//            is_complex_, will_change_);
//        } else {
//            raster_cache_result_ = RasterCacheResult()
//        }

        val bounds = sk_picture!!.cullRect().translate(offset!!.dx, offset!!.dy)
        paint_bounds = bounds
    }

    override fun Paint(context: PaintContext) {
//        TRACE_EVENT0("flutter", "PictureLayer::Paint");
        assert(picture != null) // FML_DCHECK(picture_.get());
        assert(needs_painting) // FML_DCHECK(needs_painting());

//        TODO(Migration/Andrey): Not porting SkAutoCanvasRestore for now
//        SkAutoCanvasRestore save(&context.canvas, true);
        context.canvas.translate(offset!!.dx, offset!!.dy)
//        TODO(Migration/Andrey): Not porting RasterCache for now
//        #ifndef SUPPORT_FRACTIONAL_TRANSLATION
//        context.canvas.setMatrix(
//        RasterCache::GetIntegralTransCTM(context.canvas.getTotalMatrix()));
//        #endif
//        if (raster_cache_result_!!.is_valid()) {
//            raster_cache_result_!!.draw(context.canvas,
//                    context.root_surface_transformation)
//        } else {
            context.canvas.drawPicture(picture!!)
//        }
    }

    var offset: Offset? = null

    // Even though pictures themselves are not GPU resources, they may reference
    // images that have a reference to a GPU resource.
    var picture: Picture? = null

    var is_complex: Boolean = false

    var will_change: Boolean = false

//    TODO(Migration/Andrey): Not porting RasterCache for now
//    private var raster_cache_result_: RasterCacheResult? = null
}