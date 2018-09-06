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

package androidx.ui.compositing

import androidx.ui.flow.layers.Layer
import androidx.ui.flow.layers.LayerTree

/**
 * An opaque object representing a composited scene.
 *
 * To create a Scene object, use a [SceneBuilder].
 *
 * Scene objects can be displayed on the screen using the
 * [Window.render] method.
 */
class Scene {

    companion object {

        internal fun create(
            rootLayer: Layer?,
            rasterizerTracingThreshold: Int,
            checkerboardRasterCacheImages: Boolean,
            checkerboardOffscreenLayers: Boolean
        ): Scene {
            return Scene(rootLayer,
                    rasterizerTracingThreshold,
                    checkerboardRasterCacheImages,
                    checkerboardOffscreenLayers)
        }

//        static sk_sp<SkImage> CreateSceneSnapshot(GrContext* context,
//        sk_sp<SkPicture> picture,
//        const SkSize& size) {
//            TRACE_EVENT0("flutter", "CreateSceneSnapshot");
//            auto image_info =
//            SkImageInfo::MakeN32Premul(SkISize::Make(size.width(), size.height()));
//
//            sk_sp<SkSurface> surface;
//
//            if (context) {
//                surface = SkSurface::MakeRenderTarget(context, SkBudgeted::kNo, image_info);
//            }
//
//            if (!surface) {
//                surface = SkSurface::MakeRaster(image_info);
//            }
//
//            if (!surface) {
//                return nullptr;
//            }
//
//            auto canvas = surface->getCanvas();
//
//            if (!canvas) {
//                return nullptr;
//            }
//
//            if (picture) {
//                canvas->drawPicture(picture.get());
//            }
//
//            auto snapshot = surface->makeImageSnapshot();
//
//            if (!snapshot) {
//                return nullptr;
//            }
//
//            return snapshot->makeRasterImage();
//        }
    }

    private val m_layerTree: LayerTree

    /**
     * This class is created by the engine, and should not be instantiated
     * or extended directly.
     *
     * To create a Scene object, use a [SceneBuilder].
     */
    internal constructor(
        rootLayer: Layer?,
        rasterizerTracingThreshold: Int,
        checkerboardRasterCacheImages: Boolean,
        checkerboardOffscreenLayers: Boolean
    ) {
        m_layerTree = LayerTree()
        m_layerTree.root_layer = rootLayer
        m_layerTree.rasterizer_tracing_threshold = rasterizerTracingThreshold
        m_layerTree.checkerboard_raster_cache_images = checkerboardRasterCacheImages
        m_layerTree.checkerboard_offscreen_layers = checkerboardOffscreenLayers
    }

    /**
     * Releases the resources used by this scene.
     *
     * After calling this function, the scene is cannot be used further.
     */
    fun dispose() {
//        ClearDartWrapper()
    }

    // TODO(Migration/Andrey): needs Image class
//    /**
//     * Creates a raster image representation of the current state of the scene.
//     * This is a slow operation that is performed on a background thread.
//     */
//    Future<Image> toImage(int width, int height) {
//        if (width <= 0 || height <= 0)
//            throw new Exception('Invalid image dimensions.');
//        return _futurize(
//                (_Callback<Image> callback) => _toImage(width, height, callback)
//        );
//    }
//
//    String _toImage(int width, int height, _Callback<Image> callback) {
//        TRACE_EVENT0("flutter", "Scene::toImage");
//        if (Dart_IsNull(raw_image_callback) || !Dart_IsClosure(raw_image_callback))
//        {
//            return tonic::ToDart("Image callback was invalid");
//        }
//
//        if (!m_layerTree)
//        {
//            return tonic::ToDart("Scene did not contain a layer tree.");
//        }
//
//        if (width == 0 || height == 0)
//        {
//            return tonic::ToDart("Image dimensions for scene were invalid.");
//        }
//
//
//        auto dart_state = UIDartState::Current();
//
//        auto image_callback = std::make_unique<tonic::DartPersistentValue>(
//        dart_state, raw_image_callback);
//
//        // We can't create an image on this task runner because we don't have a
//        // graphics context. Even if we did, it would be slow anyway. Also, this
//        // thread owns the sole reference to the layer tree. So we flatten the layer
//        // tree into a picture and use that as the thread transport mechanism.
//
//        auto bounds_size = SkSize::Make(width, height);
//        auto picture = m_layerTree->Flatten(SkRect::MakeSize(bounds_size));
//        if (!picture)
//        {
//            // Already in Dart scope.
//            return tonic::ToDart("Could not flatten scene into a layer tree.");
//        }
//
//        auto resource_context = dart_state->GetResourceContext();
//        auto ui_task_runner = dart_state->GetTaskRunners().GetUITaskRunner();
//        auto unref_queue = dart_state->GetSkiaUnrefQueue();
//
//
//        // The picture has been prepared on the UI thread.
//        dart_state->GetTaskRunners().GetIOTaskRunner()->PostTask(
//        fml::MakeCopyable([picture = std::move(picture),                    //
//        bounds_size,                                     //
//        resource_context = std::move(resource_context),  //
//        ui_task_runner = std::move(ui_task_runner),      //
//        image_callback = std::move(image_callback),      //
//        unref_queue = std::move(unref_queue)             //
//        ]() mutable
//        {
//            // Snapshot the picture on the IO thread that contains an optional
//            // GrContext.
//            auto image = CreateSceneSnapshot (resource_context.get(),
//            std::move(picture), bounds_size);
//
//            // Send the image back to the UI thread for submission back to the
//            // framework.
//            ui_task_runner->PostTask(
//            fml::MakeCopyable([image = std::move(image),                    //
//                image_callback = std::move(image_callback),  //
//                unref_queue = std::move(unref_queue)         //
//            ]() mutable {
//                auto dart_state = image_callback->dart_state().lock();
//                if (!dart_state) {
//                    // The root isolate could have died in the meantime.
//                    return;
//                }
//                tonic::DartState::Scope scope (dart_state);
//
//                if (!image) {
//                    tonic::DartInvoke(image_callback->Get(), { Dart_Null() });
//                    return;
//                }
//
//                auto dart_image = CanvasImage ::Create();
//                dart_image->set_image({ std::move(image), std::move(unref_queue) });
//                auto raw_dart_image = tonic ::ToDart(std::move(dart_image));
//
//                // All done!
//                tonic::DartInvoke(image_callback->Get(), { raw_dart_image });
//            }));
//        }));
//
//        return Dart_Null();
//    }

    fun takeLayerTree(): LayerTree {
        return m_layerTree
    }
}