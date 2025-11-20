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

/**
 * Represents the result of laying out text vertically.
 *
 * This class encapsulates the result of a vertical text layout process. It stores the layout's
 * properties and provides methods to draw the layout on a [Canvas].
 *
 * NOTE: Currently, this API leverages a platform feature added in API 36 (Android 16). For older
 * API levels, it provides a graceful fallback. We will provide a backport to API 31 in the future.
 */
public class VerticalTextLayout
/**
 * @param text The text to be laid out.
 * @param start The inclusive start offset of the target text range.
 * @param end The exclusive end offset of the target text range.
 * @param paint The [TextPaint] instance used for laying out the text.
 * @param height The height constraint in pixels.
 * @param orientation The text orientation used for building this vertical layout.
 */
@JvmOverloads
constructor(
    text: CharSequence = "",
    start: Int = 0,
    end: Int = text.length,
    paint: TextPaint = TextPaint(),
    @Px height: Float = 0f,
    orientation: Int = TextOrientation.MIXED,
) {
    /** The width constraint of the vertical text in pixels. */
    @get:Px
    public val width: Float
        get() = impl.width

    internal val impl: VerticalTextLayoutImpl

    init {
        require(start <= end && end <= text.length && height >= 0)

        impl =
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> {
                    VerticalTextLayoutApi36Impl(text, start, end, paint, height, orientation)
                }
                else -> {
                    VerticalTextLayoutNoOpImpl()
                }
            }
    }

    /**
     * Draws this text layout onto the specified [Canvas].
     *
     * @param canvas The [Canvas] to draw onto.
     * @param x The horizontal offset in pixels. The drawing origin is the top-right corner.
     * @param y The vertical offset in pixels. The drawing origin is the top-right corner.
     */
    public fun draw(canvas: Canvas, @Px x: Float, @Px y: Float) {
        impl.draw(canvas, x, y)
    }

    /**
     * Capability query to determine whether or not [VerticalTextLayout] supports vertical text
     * painting. If it is false, calling methods will have no effect.
     */
    public fun isVerticalTextSupported(): Boolean {
        return impl.isVerticalTextSupported()
    }
}
