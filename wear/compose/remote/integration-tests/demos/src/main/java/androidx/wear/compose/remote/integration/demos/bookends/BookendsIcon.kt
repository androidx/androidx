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

package androidx.wear.compose.remote.integration.demos.bookends

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * A simple white "plus" icon, drawn programmatically so the demo and its screenshot tests don't
 * depend on resources or an icon library.
 */
internal fun bookendsIconBitmap(sizePx: Int = 78): ImageBitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = sizePx / 6f
            strokeCap = Paint.Cap.ROUND
        }
    val inset = sizePx / 5f
    val center = sizePx / 2f
    Canvas(bitmap).apply {
        drawLine(inset, center, sizePx - inset, center, paint)
        drawLine(center, inset, center, sizePx - inset, paint)
    }
    return bitmap.asImageBitmap()
}
