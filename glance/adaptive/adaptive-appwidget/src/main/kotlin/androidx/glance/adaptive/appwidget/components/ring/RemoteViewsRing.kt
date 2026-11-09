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

package androidx.glance.adaptive.appwidget.components.ring

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.compose.ui.unit.Dp
import androidx.glance.adaptive.appwidget.R
import androidx.glance.adaptive.appwidget.components.createProgressRingBitmap
import androidx.glance.adaptive.appwidget.components.density
import androidx.glance.adaptive.appwidget.components.dpToPx
import androidx.glance.adaptive.appwidget.components.fitMetric
import androidx.glance.adaptive.appwidget.components.setMaxSize
import androidx.glance.adaptive.appwidget.components.setTextSizeSp
import androidx.glance.adaptive.appwidget.components.toPx
import androidx.glance.adaptive.appwidget.components.withAlpha
import androidx.glance.adaptive.appwidget.matrix.ContainedRing
import androidx.glance.adaptive.appwidget.matrix.Ring

/**
 * Draws the ring for the leading slot, [size] across.
 *
 * @param context Context the views are built for.
 * @param size Edge length the matrix gives the ring.
 * @return The ring's views.
 */
internal fun Ring.buildRemoteViews(context: Context, size: Dp): RemoteViews {
    val views =
        RemoteViews(
            context.packageName,
            R.layout.glance_adaptive_appwidget_remoteviews_progress_ring,
        )
    views.setProgressRing(
        context = context,
        progress = progress,
        sizeDp = size.value,
        containerColor = R.color.glance_adaptive_appwidget_remoteviews_secondary_container,
        iconRes = icon,
        iconRatio = RingDefaults.IconRatio,
    )
    return views
}

/**
 * Draws the ring for the bulb slot, in a circle [containerSize] across.
 *
 * The circle is drawn by the component rather than by rounding the widget's own bounds: an oblong
 * box rounded to half its shorter axis keeps a flat run along its longer one, which reads as a
 * circle with its top and bottom sliced off. It centers itself in the bulb.
 *
 * @param context Context the views are built for.
 * @param containerSize Diameter of the circle the bulb gives the ring.
 * @return The contained ring's views.
 */
internal fun ContainedRing.buildRemoteViews(context: Context, containerSize: Dp): RemoteViews {
    val geometry = RingDefaults.containedRingGeometry(containerSize)
    val density = context.density
    val views =
        RemoteViews(
            context.packageName,
            R.layout.glance_adaptive_appwidget_remoteviews_contained_ring,
        )

    // The circle wraps the ring, so it is padded out to the container diameter. The two halves can
    // differ by a pixel, which leaves the ring where centering it in the container would.
    val containerPx = containerSize.toPx(density)
    val ringPx = geometry.ringSize.toPx(density)
    val inset = (containerPx - ringPx).coerceAtLeast(0)
    val leading = inset / 2
    views.setViewPadding(
        R.id.glance_adaptive_appwidget_remoteviews_contained_circle,
        leading,
        leading,
        inset - leading,
        inset - leading,
    )

    views.setProgressRing(
        context = context,
        progress = progress,
        sizeDp = geometry.ringSize.value,
        containerColor = R.color.glance_adaptive_appwidget_remoteviews_widget_background,
        iconRes = icon,
        iconRatio = RingDefaults.ContainedIconRatio,
    )

    val valueId = R.id.glance_adaptive_appwidget_remoteviews_ring_value
    views.setTextViewText(
        valueId,
        fitMetric(value, geometry.valueMaxWidth.value, geometry.fontSize),
    )
    views.setTextSizeSp(valueId, geometry.fontSize.value)
    // The value sits over the ring and must not widen the circle past it.
    views.setInt(valueId, "setMaxWidth", ringPx)
    return views
}

/**
 * Sets the progress ring bitmap and sizes the ring and its glyph.
 *
 * The arc is rasterized (see [createProgressRingBitmap]) at the ring's final pixel size, rounded
 * the way the host rounds the view's size, so the host does not have to scale it. Neither image can
 * be sized directly before API 31; both wrap their drawable and are capped with [setMaxSize]
 * instead, so each drawable has to be at least as large as its cap once the host has scaled it to
 * the display density. For the glyph that means the drawable behind [iconRes] must be declared at
 * least `sizeDp * iconRatio` large.
 *
 * @param context Context the colors and display metrics are resolved against.
 * @param progress Normalized progress in `[0.0, 1.0]`, or `null` to draw an empty ring.
 * @param sizeDp Edge length of the ring.
 * @param containerColor Fill inside the ring.
 * @param iconRes Glyph drawn on the ring, or `null` for none.
 * @param iconRatio Edge length of the glyph as a fraction of [sizeDp].
 */
private fun RemoteViews.setProgressRing(
    context: Context,
    @FloatRange(from = 0.0, to = 1.0) progress: Float?,
    sizeDp: Float,
    @ColorRes containerColor: Int,
    @DrawableRes iconRes: Int?,
    iconRatio: Float,
) {
    val metrics = context.resources.displayMetrics
    val density = metrics.density
    val primary = context.getColor(R.color.glance_adaptive_appwidget_remoteviews_primary)
    val ringPx = sizeDp.dpToPx(density)
    val bitmap =
        createProgressRingBitmap(
            sizePx = ringPx,
            strokeWidthPx = sizeDp * RingDefaults.StrokeRatio * density,
            progress = progress,
            trackColor = primary.withAlpha(RingDefaults.TrackAlpha),
            progressColor = primary,
            containerColor = context.getColor(containerColor),
        )

    // The bitmap is exactly the ring's size, unless createProgressRingBitmap clamped it to its size
    // limits. A clamped bitmap is declared at a different density instead, so the host scales it to
    // the ring's size rather than showing it at its own. A zero-sized ring counts as one pixel.
    bitmap.density = (bitmap.width * metrics.densityDpi / ringPx.coerceAtLeast(1)).coerceAtLeast(1)
    setImageViewBitmap(R.id.glance_adaptive_appwidget_remoteviews_ring, bitmap)
    setMaxSize(R.id.glance_adaptive_appwidget_remoteviews_ring, ringPx)

    val glyphId = R.id.glance_adaptive_appwidget_remoteviews_ring_glyph
    if (iconRes == null) {
        setViewVisibility(glyphId, View.GONE)
    } else {
        setViewVisibility(glyphId, View.VISIBLE)
        setImageViewResource(glyphId, iconRes)
        setMaxSize(glyphId, (sizeDp * iconRatio).dpToPx(density))
    }
}
