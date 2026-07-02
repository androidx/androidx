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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.drawable.Drawable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.roundToInt

/**
 * Renders an [android.graphics.drawable.Drawable] as a Compose [Painter] so a host-provided
 * [RcImageLoader] result can flow into a standard `Image` (honoring `contentScale`/`alpha`) without
 * any image-loading library dependency. The drawable is stretched to the painter's draw size; its
 * intrinsic size (if known) lets `Image` size itself when no explicit size is given.
 */
internal class DrawablePainter(private val drawable: Drawable) : Painter() {
    private var drawAlpha: Float = 1f

    override val intrinsicSize: Size
        get() =
            if (drawable.intrinsicWidth >= 0 && drawable.intrinsicHeight >= 0) {
                Size(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
            } else {
                Size.Unspecified
            }

    override fun applyAlpha(alpha: Float): Boolean {
        drawAlpha = alpha
        return true
    }

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            drawable.alpha = (drawAlpha * 255f).roundToInt().coerceIn(0, 255)
            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
            drawable.draw(canvas.nativeCanvas)
        }
    }
}
