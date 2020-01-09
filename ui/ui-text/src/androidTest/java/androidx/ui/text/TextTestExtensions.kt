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
package androidx.ui.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.ui.text.font.Font
import androidx.ui.unit.IntPx
import androidx.ui.unit.ipx
import kotlin.math.ceil
import kotlin.math.roundToInt

fun Paragraph.bitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        width.toIntPx().value,
        height.toIntPx().value,
        Bitmap.Config.ARGB_8888
    )
    this.paint(androidx.ui.graphics.Canvas(Canvas(bitmap)))
    return bitmap
}

class TestFontResourceLoader(val context: Context) : Font.ResourceLoader {
    override fun load(font: Font): Typeface {
        val resId = context.resources.getIdentifier(
            font.name.substringBefore("."),
            "font",
            context.packageName
        )

        return ResourcesCompat.getFont(context, resId)!!
    }
}

fun Float.toIntPx(): IntPx = ceil(this).roundToInt().ipx