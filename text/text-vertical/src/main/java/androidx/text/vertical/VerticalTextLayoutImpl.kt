/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.text.vertical

import android.graphics.Canvas
import android.os.Build
import android.text.TextPaint
import androidx.annotation.Px
import androidx.annotation.RequiresApi

internal interface VerticalTextLayoutImpl {
    @get:Px val width: Float

    fun draw(canvas: Canvas, @Px x: Float, @Px y: Float)

    fun isVerticalTextSupported(): Boolean
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
internal class VerticalTextLayoutApi36Impl(
    public val text: CharSequence,
    public val start: Int,
    public val end: Int,
    public val paint: TextPaint,
    @Px public val height: Float,
    @OrientationMode public val orientation: Int,
    private val result: LineBreaker.Result =
        LineBreaker.breakTextIntoLines(text, start, end, paint, height, orientation),
) : VerticalTextLayoutImpl {
    @get:Px
    override val width: Float
        get() = result.width

    override fun draw(canvas: Canvas, @Px x: Float, @Px y: Float) {
        result.draw(canvas, x, y, paint)
    }

    override fun isVerticalTextSupported(): Boolean {
        return true
    }
}

internal class VerticalTextLayoutNoOpImpl : VerticalTextLayoutImpl {
    override val width: Float
        get() = 0f

    override fun draw(canvas: Canvas, @Px x: Float, @Px y: Float) {
        // no-op, fallback
    }

    override fun isVerticalTextSupported(): Boolean {
        return false
    }
}
