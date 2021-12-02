/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.splashscreen

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.Drawable

/**
 * A wrapper around a `drawable` that clip it to fit in a circle of diameter `maskDiameter`.
 * @param drawable The drawable to clip
 * @param maskDiameter The diameter of the mask used to clip the drawable.
 */
internal class MaskedDrawable(
    private val drawable: Drawable,
    private val maskDiameter: Float
) : Drawable() {
    private val mask = Path().apply {
        val radius = maskDiameter / 2f
        addCircle(0f, 0f, radius, Path.Direction.CW)
    }

    override fun draw(canvas: Canvas) {
        canvas.clipPath(mask)
        drawable.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        drawable.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        drawable.colorFilter = colorFilter
    }

    @Suppress("DEPRECATION")
    override fun getOpacity() = drawable.opacity

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        bounds ?: return
        drawable.bounds = bounds
        mask.offset(bounds.exactCenterX(), bounds.exactCenterY())
    }
}