/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test.android

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.view.PixelCopy
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.ui.geometry.Rect
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
internal fun captureRegionToBitmap(
    globalRect: Rect,
    handler: Handler,
    window: Window
): Bitmap {
    val destBitmap = Bitmap.createBitmap(
        globalRect.width.roundToInt(),
        globalRect.height.roundToInt(),
        Bitmap.Config.ARGB_8888)

    // TODO: This could go to some Android specific extensions.
    val srcRect = android.graphics.Rect(
        globalRect.left.roundToInt(),
        globalRect.top.roundToInt(),
        globalRect.right.roundToInt(),
        globalRect.bottom.roundToInt())

    val latch = CountDownLatch(1)
    var copyResult = 0
    val onCopyFinished = object : PixelCopy.OnPixelCopyFinishedListener {
        override fun onPixelCopyFinished(result: Int) {
            copyResult = result
            latch.countDown()
        }
    }

    PixelCopy.request(window, srcRect, destBitmap, onCopyFinished, handler)

    if (!latch.await(1, TimeUnit.SECONDS)) {
        throw AssertionError("Failed waiting for PixelCopy!")
    }
    if (copyResult != PixelCopy.SUCCESS) {
        throw AssertionError("PixelCopy failed!")
    }
    return destBitmap
}
