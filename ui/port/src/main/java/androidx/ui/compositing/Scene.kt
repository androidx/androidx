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

/**
 * An opaque object representing a composited scene.
 *
 * To create a Scene object, use a [SceneBuilder].
 *
 * Scene objects can be displayed on the screen using the
 * [Window.render] method.
 */
// TODO(Migration/Andrey): Figure out how to handle native code
class Scene /*extends NativeFieldWrapperClass2*/ {
    /**
     * This class is created by the engine, and should not be instantiated
     * or extended directly.
     *
     * To create a Scene object, use a [SceneBuilder].
     */
    internal constructor()

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
//    String _toImage(int width, int height, _Callback<Image> callback) native 'Scene_toImage';

    /**
     * Releases the resources used by this scene.
     *
     * After calling this function, the scene is cannot be used further.
     */
    fun dispose() {
        TODO()
//        native 'Scene_dispose';
    }
}