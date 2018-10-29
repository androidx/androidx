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
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Picture
import androidx.ui.skia.SkMatrix
import java.util.Stack

class DefaultLayerBuilder : LayerBuilder() {

    companion object {

        val kGiantRect: Rect = Rect.fromLTRB(-1E9, -1E9, 1E9, 1E9)
    }

    override fun PushTransform(matrix: SkMatrix) {
        val inverse_sk_matrix: SkMatrix? = matrix.invert()
        val cullRect: Rect?
        // Perspective projections don't produce rectangles that are useful for
        // culling for some reason.
        if (!matrix.hasPerspective() && inverse_sk_matrix != null) {
            cullRect = inverse_sk_matrix.mapRect(cull_rects_.peek())
        } else {
            cullRect = kGiantRect
        }
        val layer = TransformLayer()
        layer.transform = matrix
        PushLayer(layer, cullRect)
    }

//    // |flow::LayerBuilder|
//    void PushClipRect(const SkRect& rect,
//    Clip clip_behavior = Clip::antiAlias) override {
//        SkRect cullRect;
//        if (!cullRect.intersect(clipRect, cull_rects_.top())) {
//            cullRect = SkRect::MakeEmpty();
//        }
//        auto layer = std::make_unique<flow::ClipRectLayer>(clip_behavior);
//        layer->set_clip_rect(clipRect);
//        PushLayer(std::move(layer), cullRect);
//    }
//
//    // |flow::LayerBuilder|
//    void PushClipRoundedRect(const SkRRect& rect,
//    Clip clip_behavior = Clip::antiAlias) override {
//        SkRect cullRect;
//        if (!cullRect.intersect(rrect.rect(), cull_rects_.top())) {
//            cullRect = SkRect::MakeEmpty();
//        }
//        auto layer = std::make_unique<flow::ClipRRectLayer>(clip_behavior);
//        layer->set_clip_rrect(rrect);
//        PushLayer(std::move(layer), cullRect);
//    }
//
//    // |flow::LayerBuilder|
//    void PushClipPath(const SkPath& path,
//    Clip clip_behavior = Clip::antiAlias) override {
//        FML_DCHECK(clip_behavior != Clip::none);
//        SkRect cullRect;
//        if (!cullRect.intersect(path.getBounds(), cull_rects_.top())) {
//            cullRect = SkRect::MakeEmpty();
//        }
//        auto layer = std::make_unique<flow::ClipPathLayer>(clip_behavior);
//        layer->set_clip_path(path);
//        PushLayer(std::move(layer), cullRect);
//    }
//
//    // |flow::LayerBuilder|
//    void PushOpacity(int alpha) override {
//        auto layer = std::make_unique<flow::OpacityLayer>();
//        layer->set_alpha(alpha);
//        PushLayer(std::move(layer), cull_rects_.top());
//    }
//
//    // |flow::LayerBuilder|
//    void PushColorFilter(SkColor color, SkBlendMode blend_mode) override {
//        auto layer = std::make_unique<flow::ColorFilterLayer>();
//        layer->set_color(color);
//        layer->set_blend_mode(blend_mode);
//        PushLayer(std::move(layer), cull_rects_.top());
//    }
//
//    // |flow::LayerBuilder|
//    void PushBackdropFilter(sk_sp<SkImageFilter> filter) override {
//        auto layer = std::make_unique<flow::BackdropFilterLayer>();
//        layer->set_filter(filter);
//        PushLayer(std::move(layer), cull_rects_.top());
//    }
//
//    // |flow::LayerBuilder|
//    void PushShaderMask(sk_sp<SkShader> shader,
//    const SkRect& rect,
//    SkBlendMode blend_mode) override {
//        auto layer = std::make_unique<flow::ShaderMaskLayer>();
//        layer->set_shader(shader);
//        layer->set_mask_rect(rect);
//        layer->set_blend_mode(blend_mode);
//        PushLayer(std::move(layer), cull_rects_.top());
//    }
//
//    // |flow::LayerBuilder|
//    void PushPhysicalShape(const SkPath& path,
//    double elevation,
//    SkColor color,
//    SkColor shadow_color,
//    SkScalar device_pixel_ratio,
//    Clip clip_behavior) override {
//        SkRect cullRect;
//        if (!cullRect.intersect(sk_path.getBounds(), cull_rects_.top())) {
//            cullRect = SkRect::MakeEmpty();
//        }
//        auto layer = std::make_unique<flow::PhysicalShapeLayer>(clip_behavior);
//        layer->set_path(sk_path);
//        layer->set_elevation(elevation);
//        layer->set_color(color);
//        layer->set_shadow_color(shadow_color);
//        layer->set_device_pixel_ratio(device_pixel_ratio);
//        PushLayer(std::move(layer), cullRect);
//    }
//
//    // |flow::LayerBuilder|
//    void PushPerformanceOverlay(uint64_t enabled_options,
//    const SkRect& rect) override {
//        if (!current_layer_) {
//            return;
//        }
//        auto layer = std::make_unique<flow::PerformanceOverlayLayer>(enabled_options);
//        layer->set_paint_bounds(rect);
//        current_layer_->Add(std::move(layer));
//    }

    override fun PushPicture(
        offset: Offset,
        picture: Picture,
        picture_is_complex: Boolean,
        picture_will_change: Boolean
    ) {
        current_layer_?.let {
            val pictureRect = picture.cullRect().translate(offset.dx, offset.dy)
            if (pictureRect.intersect(cull_rects_.peek()).isEmpty()) {
                return
            }
            val layer = PictureLayer()
            layer.offset = offset
            layer.picture = picture
            layer.is_complex = picture_is_complex
            layer.will_change = picture_will_change
            it.Add(layer)
        }
    }

//    // |flow::LayerBuilder|
//    void PushTexture(const SkPoint& offset,
//    const SkSize& size,
//    int64_t texture_id,
//    bool freeze) override {
//        if (!current_layer_) {
//            return;
//        }
//        auto layer = std::make_unique<flow::TextureLayer>();
//        layer->set_offset(offset);
//        layer->set_size(size);
//        layer->set_texture_id(texture_id);
//        layer->set_freeze(freeze);
//        current_layer_->Add(std::move(layer));
//    }
//
//    #if defined(OS_FUCHSIA)
//    // |flow::LayerBuilder|
//    void PushChildScene(const SkPoint& offset,
//    const SkSize& size,
//    fml::RefPtr<flow::ExportNodeHolder> export_token_holder,
//    bool hit_testable) override {
//        if (!current_layer_) {
//            return;
//        }
//        SkRect sceneRect =
//        SkRect::MakeXYWH(offset.x(), offset.y(), size.width(), size.height());
//        if (!SkRect::Intersects(sceneRect, cull_rects_.top())) {
//            return;
//        }
//        auto layer = std::make_unique<flow::ChildSceneLayer>();
//        layer->set_offset(offset);
//        layer->set_size(size);
//        layer->set_export_node_holder(std::move(export_token_holder));
//        layer->set_hit_testable(hit_testable);
//        current_layer_->Add(std::move(layer));
//    }
//    #endif  // defined(OS_FUCHSIA)

    override fun Pop() {
        if (current_layer_ == null) {
            return
        }
        cull_rects_.pop()
        current_layer_ = current_layer_!!.parent
    }

    override fun TakeLayer(): Layer? {
        return root_layer_
    }

    private var root_layer_: ContainerLayer? = null
    private var current_layer_: ContainerLayer? = null
    private var cull_rects_ = Stack<Rect>().apply {
        push(kGiantRect)
    }

    private fun PushLayer(layer: ContainerLayer, cullRect: Rect) {
        cull_rects_.push(cullRect)

        if (root_layer_ == null) {
            root_layer_ = layer
            current_layer_ = root_layer_
            return
        }

        if (current_layer_ == null) {
            return
        }

        current_layer_!!.Add(layer)
        current_layer_ = layer
    }
}