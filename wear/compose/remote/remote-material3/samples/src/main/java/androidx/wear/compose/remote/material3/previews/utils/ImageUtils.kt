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

package androidx.wear.compose.remote.material3.previews.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

fun createImage(tw: Int, th: Int): Bitmap {
    val image = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
    image.eraseColor(android.graphics.Color.BLUE)
    val paint = Paint()
    val canvas = Canvas(image)
    paint.strokeWidth = 3f
    paint.isAntiAlias = true
    paint.setColor(android.graphics.Color.RED)
    canvas.drawLine(0f, 0f, tw.toFloat(), th.toFloat(), paint)
    canvas.drawLine(0f, th.toFloat(), tw.toFloat(), 0f, paint)
    return image
}
