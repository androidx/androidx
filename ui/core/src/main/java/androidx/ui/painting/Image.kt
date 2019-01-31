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

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

// Opaque handle to raw decoded image data (pixels).
/**
 *
 * To obtain an [Image] object, use [instantiateImageCodec].
 *
 * To draw an [Image], use one of the methods on the [Canvas] class, such as
 * [Canvas.drawImage].
 */

/**
 * This class is created by the engine, and should not be instantiated
 * or extended directly.
 *
 * To obtain an [Image] object, use [instantiateImageCodec].
 */
class Image constructor(
    internal val bitmap: android.graphics.Bitmap
)/* extends NativeFieldWrapperClass2 */ {

    /** The number of image pixels along the image's horizontal axis. */
    val width: Int = bitmap.width

    /** The number of image pixels along the image's vertical axis. */
    val height: Int = bitmap.height

    /**
     * Converts the [Image] object into a byte array.
     *
     * The [format] argument specifies the format in which the bytes will be
     * returned.
     *
     * Returns a future that completes with the binary image data or an error
     * if encoding fails.
     */
    fun CoroutineScope.toByteData(
        format: ImageByteFormat = ImageByteFormat.rawRgba
    ): Deferred<ByteBuffer> {
        return async {
            when (format) {
                // Bitmap is already in argb so in either rawRgba or rawUnmodified
                // return the same bytearray
                ImageByteFormat.rawRgba -> {
                    assert(bitmap.config == Bitmap.Config.ARGB_8888)
                    toRawByteArray()
                }
                ImageByteFormat.rawUnmodified -> toRawByteArray()
                ImageByteFormat.png -> toPngByteArray()
            }
        }
    }

    private fun toRawByteArray(): ByteBuffer {
        val byteBuffer = ByteBuffer.allocate(size())
        bitmap.copyPixelsToBuffer(byteBuffer)
        return byteBuffer
    }

    private fun toPngByteArray(): ByteBuffer {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return ByteBuffer.wrap(stream.toByteArray())
    }

    private fun size(): Int {
        return bitmap.byteCount
    }
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
    /**
     * Release the resources used by this object. The object is no longer usable
     * after this method is called.
     */
    fun dispose() = bitmap.recycle()

    override fun toString(): String {
        return String.format("%d * %d", width, height)
    }
}
