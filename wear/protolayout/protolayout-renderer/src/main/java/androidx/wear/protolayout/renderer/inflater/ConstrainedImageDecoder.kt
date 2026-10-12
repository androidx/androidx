/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Size
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer

internal object ConstrainedImageDecoder {
    /**
     * If an image is larger than this, we won't even attempt to decode it, as we risk taking up all
     * of the device memory and the image is already not suitable for a small screen anyway.
     */
    const val DEFAULT_DECODE_HARD_LIMIT_PX: Int = 2048

    @RequiresApi(28)
    private val imageDecoderListener = ImageDecoder.OnHeaderDecodedListener { _, imageInfo, _ ->
        checkSize(imageInfo.size)
    }

    private fun isColorResource(outValue: TypedValue): Boolean =
        outValue.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT

    @JvmStatic
    @JvmOverloads
    @RequiresApi(28)
    @SuppressLint("ResourceType")
    fun decodeDrawable(
        res: Resources,
        @DrawableRes resId: Int,
        theme: Resources.Theme? = null,
    ): Drawable {
        val outValue = TypedValue()
        res.getValue(resId, outValue, /* resolveRefs= */ true)
        if (isColorResource(outValue)) {
            return res.getDrawable(resId, theme)
        }
        if (outValue.string?.endsWith(".xml") == true) {
            XmlDrawableValidator.verifyDrawableXml(res, resId)
            return res.getDrawable(resId, theme)
        }
        return ImageDecoder.decodeDrawable(
            ImageDecoder.createSource(res, resId),
            imageDecoderListener,
        )
    }

    @JvmStatic
    @RequiresApi(28)
    fun decodeDrawable(contentResolver: ContentResolver, uri: Uri): Drawable {
        return ImageDecoder.decodeDrawable(
            ImageDecoder.createSource(contentResolver, uri),
            imageDecoderListener,
        )
    }

    @JvmStatic
    @RequiresApi(31)
    fun decodeBitmap(data: ByteArray, targetWidthPx: Int, targetHeightPx: Int): Bitmap {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(data))) {
            decoder,
            imageInfo,
            _ ->
            checkSize(imageInfo.size)
            val targetSize = Size(targetWidthPx, targetHeightPx)
            if (imageInfo.size != targetSize) {
                checkSize(targetSize)
                decoder.setTargetSize(targetWidthPx, targetHeightPx)
            }
        }
    }

    internal fun checkSize(imageSize: Size) {
        checkSize(imageSize.width, imageSize.height)
    }

    internal fun checkSize(width: Int, height: Int) {
        if (
            width <= 0 ||
                height <= 0 ||
                width > DEFAULT_DECODE_HARD_LIMIT_PX ||
                height > DEFAULT_DECODE_HARD_LIMIT_PX
        ) {
            throw IllegalArgumentException("Image is too large (${width}x${height}).")
        }
    }
}
