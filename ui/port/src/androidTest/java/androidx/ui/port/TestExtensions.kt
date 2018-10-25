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
package androidx.ui.port

import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Layout
import androidx.ui.engine.text.Paragraph
import kotlin.math.ceil

fun Paragraph.bitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        ceil(this.width).toInt(),
        ceil(this.height).toInt(),
        Bitmap.Config.ARGB_8888
    )
    this.paint(androidx.ui.painting.Canvas(Canvas(bitmap)), 0.0, 0.0)
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