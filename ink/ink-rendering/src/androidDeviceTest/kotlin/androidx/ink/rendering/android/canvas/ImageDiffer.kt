/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.rendering.android.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.os.Build
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden

/** Common logic for image-diff tests against golden. */
internal object ImageDiffer {
    fun diffBitmapWithGolden(
        screenshotRule: AndroidXScreenshotTestRule,
        className: String?,
        bitmap: Bitmap,
        name: String,
    ) {
        checkNotNull(className)
        bitmap.assertAgainstGolden(screenshotRule, "${className}_$name")
    }

    /*
     * Calls the given [block] on a new, white [Canvas] of the given [width] and [height], returning
     * a mutable [Bitmap] representing the result.
     */
    fun createBitmap(width: Int, height: Int, block: (Canvas) -> Unit): Bitmap {
        val picture = Picture()
        val canvas = picture.beginRecording(width, height)
        canvas.drawColor(TestColors.WHITE)
        block(canvas)
        picture.endRecording()
        val bitmap =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Needs to be copied to a mutable Bitmap because HARDWARE Bitmaps don't support
                // getPixels, which is used by AndroidXScreenshotTestRule.
                Bitmap.createBitmap(picture).copy(Bitmap.Config.ARGB_8888, /* isMutable= */ true)
            } else {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
                    Canvas(bmp).drawPicture(picture)
                }
            }
        return bitmap
    }
}
