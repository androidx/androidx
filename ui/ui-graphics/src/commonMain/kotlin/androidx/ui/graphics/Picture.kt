/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.graphics

import androidx.ui.geometry.Rect

expect class NativePicture()

expect fun NativePicture.getNativeWidth(): Int

expect fun NativePicture.getNativeHeight(): Int

expect fun NativePicture.beginNativeRecording(width: Int, height: Int): NativeCanvas

/**
 * An object representing a sequence of recorded graphical operations.
 *
 * To create a [Picture], use a [PictureRecorder].
 *
 * A [Picture] can be placed in a [Scene] using a [SceneBuilder], via
 * the [SceneBuilder.addPicture] method. A [Picture] can also be
 * drawn into a [Canvas], using the [Canvas.drawPicture] method.
 *
 * To create a [Picture], use a [PictureRecorder].
 */
class Picture(val nativePicture: NativePicture) {

    /**
     * Creates an image from this picture.
     *
     * The picture is rasterized using the number of pixels specified by the
     * given width and height.
     *
     * Although the image is returned synchronously, the picture is actually
     * rasterized the first time the image is drawn and then cached.
     */
    // TODO(Andrey): Native code. also needs Image class
//    fun toImage(width : Int, height : Int) : Image {
//        native 'Picture_toImage';
//    }

    /**
     * Release the resources used by this object. The object is no longer usable
     * after this method is called.
     */
    fun dispose() {
        TODO()
//        native 'Picture_dispose';
    }

    fun cullRect(): Rect {
        return Rect(0.0f,
                0.0f,
                nativePicture.getNativeWidth().toFloat(),
                nativePicture.getNativeHeight().toFloat())
    }
}