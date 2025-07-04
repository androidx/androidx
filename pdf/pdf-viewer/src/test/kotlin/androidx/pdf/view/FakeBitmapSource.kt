/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.view

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import androidx.pdf.PdfDocument

/**
 * Fake implementation of [PdfDocument.BitmapSource] that always produces a blank bitmap of the
 * requested size.
 */
internal class FakeBitmapSource(override val pageNumber: Int) : PdfDocument.BitmapSource {

    override suspend fun getBitmap(scaledPageSizePx: Size, tileRegion: Rect?): Bitmap {
        return if (tileRegion != null) {
            Bitmap.createBitmap(tileRegion.width(), tileRegion.height(), Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(
                scaledPageSizePx.width,
                scaledPageSizePx.height,
                Bitmap.Config.ARGB_8888,
            )
        }
    }

    override fun close() {
        /* no-op, fake */
    }
}
