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

abstract class LayerBuilder protected constructor() {

    companion object {
        fun Create(): LayerBuilder {
            return DefaultLayerBuilder()
        }
    }

    var rasterizer_tracing_threshold = 0
    var checkerboard_raster_cache_images = false
    var checkerboard_offscreen_layers = false

    abstract fun PushTransform(matrix: SkMatrix)

//    virtual void PushClipRect(const SkRect& rect,
//    Clip clip_behavior = Clip::antiAlias) = 0;
//
//    virtual void PushClipRoundedRect(const SkRRect& rect,
//    Clip clip_behavior = Clip::antiAlias) = 0;
//
//    virtual void PushClipPath(const SkPath& path,
//    Clip clip_behavior = Clip::antiAlias) = 0;
//
//    virtual void PushOpacity(int alpha) = 0;
//
//    virtual void PushColorFilter(SkColor color, SkBlendMode blend_mode) = 0;
//
//    virtual void PushBackdropFilter(sk_sp<SkImageFilter> filter) = 0;
//
//    virtual void PushShaderMask(sk_sp<SkShader> shader,
//    const SkRect& rect,
//    SkBlendMode blend_mode) = 0;
//
//    virtual void PushPhysicalShape(const SkPath& path,
//    double elevation,
//    SkColor color,
//    SkColor shadow_color,
//    SkScalar device_pixel_ratio,
//    Clip clip_behavior) = 0;
//
//    virtual void PushPerformanceOverlay(uint64_t enabled_options,
//    const SkRect& rect) = 0;

    abstract fun PushPicture(
        offset: Offset,
        picture: Picture,
        picture_is_complex: Boolean,
        picture_will_change: Boolean
    )

//    virtual void PushTexture(const SkPoint& offset,
//    const SkSize& size,
//    int64_t texture_id,
//    bool freeze) = 0;
//
//    #if defined(OS_FUCHSIA)
//    virtual void PushChildScene(
//    const SkPoint& offset,
//    const SkSize& size,
//    fml::RefPtr<flow::ExportNodeHolder> export_token_holder,
//    bool hit_testable) = 0;
//    #endif  // defined(OS_FUCHSIA)

    abstract fun Pop()

    abstract fun TakeLayer(): Layer?
}