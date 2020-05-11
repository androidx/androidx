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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.ui.core.AndroidOwner
import androidx.ui.core.Owner
import androidx.ui.geometry.Rect
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
internal fun captureRegionToBitmap(
    captureRect: Rect,
    owner: Owner
): Bitmap {

    fun Context.getActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> this.baseContext.getActivity()
            else -> null
        }
    }

    // TODO(pavlis): Make sure that the Activity actually hosts the view. As in case of popup
    //  it wouldn't. This will require us rewriting the structure how we collect the nodes.

    // TODO(pavlis): Add support for popups. So if we find composable hosted in popup we can
    //  grab its reference to its window (need to add a hook to popup).

    val window = (owner as AndroidOwner).view.context.getActivity()!!.window
    val handler = Handler(Looper.getMainLooper())
    return captureRegionToBitmap(captureRect, handler, window)
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun captureRegionToBitmap(
    captureRect: Rect,
    handler: Handler,
    window: Window
): Bitmap {
    val destBitmap = Bitmap.createBitmap(
        captureRect.width.roundToInt(),
        captureRect.height.roundToInt(),
        Bitmap.Config.ARGB_8888
    )

    // TODO: This could go to some Android specific extensions.
    val srcRect = android.graphics.Rect(
        captureRect.left.roundToInt(),
        captureRect.top.roundToInt(),
        captureRect.right.roundToInt(),
        captureRect.bottom.roundToInt()
    )

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
