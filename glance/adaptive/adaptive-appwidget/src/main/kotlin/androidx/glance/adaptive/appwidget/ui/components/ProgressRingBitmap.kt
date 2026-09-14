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

package androidx.glance.adaptive.appwidget.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.VisibleForTesting

/** Sweep of a full turn, in degrees. */
private const val FULL_SWEEP_DEGREES = 360f

/** 12 o'clock, the angle progress starts from. Canvas angles are measured from 3 o'clock. */
private const val START_ANGLE_DEGREES = -90f

/**
 * Largest bitmap edge, in pixels, the ring is rasterized at.
 *
 * RemoteViews ships bitmaps across Binder, so an oversized ring on a high density display is paid
 * for on every update. The ring is a 56 dp circle, which stays under this cap up to ~4x density.
 */
@VisibleForTesting internal const val MAX_RING_SIZE_PX: Int = 256

/**
 * Rasterizes the determinate progress ring from the Track title slot.
 *
 * Glance 1.1.1 only offers an indeterminate `CircularProgressIndicator`, so the arc is drawn onto a
 * bitmap and emitted through `ImageProvider` instead. Callers are expected to cache the result —
 * see [ProgressRingBlock], which keys a `remember` on every input.
 *
 * @param sizePx Requested edge length in pixels; clamped to [MAX_RING_SIZE_PX].
 * @param strokeWidthPx Ring thickness in pixels.
 * @param progress Normalized progress in `[0.0, 1.0]`, or `null` to draw the track only.
 * @param trackColor Color of the unfilled remainder of the ring.
 * @param progressColor Color of the filled arc.
 * @param containerColor Fill behind the ring, drawn inside the stroke.
 * @return A freshly allocated [Bitmap] containing the ring.
 */
internal fun createProgressRingBitmap(
    sizePx: Int,
    strokeWidthPx: Float,
    @FloatRange(from = 0.0, to = 1.0) progress: Float?,
    @ColorInt trackColor: Int,
    @ColorInt progressColor: Int,
    @ColorInt containerColor: Int,
): Bitmap {
    val edge = sizePx.coerceIn(1, MAX_RING_SIZE_PX)
    val bitmap = Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val stroke = strokeWidthPx.coerceIn(1f, edge / 2f)
    val inset = stroke / 2f
    val bounds = RectF(inset, inset, edge - inset, edge - inset)
    val center = edge / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Container sits inside the stroke so the ring reads as an outline around it, not on top of it.
    paint.style = Paint.Style.FILL
    paint.color = containerColor
    canvas.drawCircle(center, center, center - stroke, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = stroke
    paint.strokeCap = Paint.Cap.ROUND

    paint.color = trackColor
    canvas.drawArc(bounds, START_ANGLE_DEGREES, FULL_SWEEP_DEGREES, false, paint)

    if (progress != null && progress > 0f) {
        paint.color = progressColor
        canvas.drawArc(
            bounds,
            START_ANGLE_DEGREES,
            progress.coerceIn(0f, 1f) * FULL_SWEEP_DEGREES,
            false,
            paint,
        )
    }
    return bitmap
}
