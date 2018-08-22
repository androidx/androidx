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

package androidx.ui.painting

// Opaque handle to raw decoded image data (pixels).
// /
// / To obtain an [Image] object, use [instantiateImageCodec].
// /
// / To draw an [Image], use one of the methods on the [Canvas] class, such as
// / [Canvas.drawImage].
class Image /* extends NativeFieldWrapperClass2 */ {

    // TODO(Migration/Filip): Let's just wrap Android's Bitmap here
    val width: Int = 0
    val height: Int = 0

//    /// This class is created by the engine, and should not be instantiated
//    /// or extended directly.
//    ///
//    /// To obtain an [Image] object, use [instantiateImageCodec].
//    Image._();
//
//    /// The number of image pixels along the image's horizontal axis.
//    int get width native 'Image_width';
//
//    /// The number of image pixels along the image's vertical axis.
//    int get height native 'Image_height';
//
//    /// Converts the [Image] object into a byte array.
//    ///
//    /// The [format] argument specifies the format in which the bytes will be
//    /// returned.
//    ///
//    /// Returns a future that completes with the binary image data or an error
//    /// if encoding fails.
//    Future<ByteData> toByteData({ImageByteFormat format: ImageByteFormat.rawRgba}) {
//        return _futurize((_Callback<ByteData> callback) {
//            return _toByteData(format.index, (Uint8List encoded) {
//                callback(encoded?.buffer?.asByteData());
//            });
//        });
//    }
//
//    /// Returns an error message on failure, null on success.
//    String _toByteData(int format, _Callback<Uint8List> callback) native 'Image_toByteData';
//
//    /// Release the resources used by this object. The object is no longer usable
//    /// after this method is called.
//    void dispose() native 'Image_dispose';
//
//    @override
//    String toString() => '[$width\u00D7$height]';
}