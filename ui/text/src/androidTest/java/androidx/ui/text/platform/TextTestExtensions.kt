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

package androidx.ui.text.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.Layout
import android.text.TextPaint
import androidx.text.TextLayout
import kotlin.math.ceil

internal fun AndroidParagraph.bitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        ceil(this.width).toInt(),
        ceil(this.height).toInt(),
        Bitmap.Config.ARGB_8888
    )
    this.paint(androidx.ui.painting.Canvas(Canvas(bitmap)), 0.0f, 0.0f)
    return bitmap
}

fun Layout.bitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        this.width,
        this.height,
        Bitmap.Config.ARGB_8888
    )
    this.draw(android.graphics.Canvas(bitmap))
    return bitmap
}

fun Typeface.bitmap(): Bitmap {
    return bitmap("abc")
}

fun Typeface.bitmap(text: String): Bitmap {
    val fontSize = 10.0f
    val paint = TextPaint()
    paint.textSize = fontSize
    paint.typeface = this
    // 1.5 is a random number to increase the size of bitmap a little
    val layout = TextLayout(
        charSequence = text,
        textPaint = paint,
        width = text.length * fontSize * 1.5f
    )
    return layout.layout.bitmap()
}