/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.os.Trace
import android.util.DisplayMetrics
import android.util.Log
import android.util.SizeF
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.min

// Retrieves the minimum size of an App Widget, as configured by the App Widget provider.
internal fun appWidgetMinSize(
    displayMetrics: DisplayMetrics,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
): DpSize {
    val info = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return DpSize.Zero
    val minWidth = min(
        info.minWidth,
        if (info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0) {
            info.minResizeWidth
        } else {
            Int.MAX_VALUE
        }
    )
    val minHeight = min(
        info.minHeight,
        if (info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0) {
            info.minResizeHeight
        } else {
            Int.MAX_VALUE
        }
    )
    return DpSize(minWidth.pixelsToDp(displayMetrics), minHeight.pixelsToDp(displayMetrics))
}

// Extract the sizes from the bundle
@Suppress("DEPRECATION")
internal fun Bundle.extractAllSizes(minSize: () -> DpSize): List<DpSize> {
    val sizes = getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
    return if (sizes.isNullOrEmpty()) {
        estimateSizes(minSize)
    } else {
        sizes.map { DpSize(it.width.dp, it.height.dp) }
    }
}

// If the list of sizes is not available, estimate it from the min/max width and height.
// We can assume that the min width and max height correspond to the portrait mode and the max
// width / min height to the landscape mode.
private fun Bundle.estimateSizes(minSize: () -> DpSize): List<DpSize> {
    val minHeight = getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
    val maxHeight = getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
    val minWidth = getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
    val maxWidth = getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
    // If the min / max widths and heights are not specified, fall back to the unique mode,
    // giving the minimum size the app widget may have.
    if (minHeight == 0 || maxHeight == 0 || minWidth == 0 || maxWidth == 0) {
        return listOf(minSize())
    }
    return listOf(DpSize(minWidth.dp, maxHeight.dp), DpSize(maxWidth.dp, minHeight.dp))
}

// Landscape is min height / max width
private fun Bundle.extractLandscapeSize(): DpSize? {
    val minHeight = getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
    val maxWidth = getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
    return if (minHeight == 0 || maxWidth == 0) null else DpSize(maxWidth.dp, minHeight.dp)
}

// Portrait is max height / min width
private fun Bundle.extractPortraitSize(): DpSize? {
    val maxHeight = getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
    val minWidth = getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
    return if (maxHeight == 0 || minWidth == 0) null else DpSize(minWidth.dp, maxHeight.dp)
}

internal fun Bundle.extractOrientationSizes() =
    listOfNotNull(extractLandscapeSize(), extractPortraitSize())

// True if the object fits in the given size.
private infix fun DpSize.fitsIn(other: DpSize) =
    (ceil(other.width.value) + 1 > width.value) &&
        (ceil(other.height.value) + 1 > height.value)

internal fun DpSize.toSizeF(): SizeF = SizeF(width.value, height.value)

private fun squareDistance(widgetSize: DpSize, layoutSize: DpSize): Float {
    val dw = widgetSize.width.value - layoutSize.width.value
    val dh = widgetSize.height.value - layoutSize.height.value
    return dw * dw + dh * dh
}

// Find the best size that fits in the available [widgetSize] or null if no layout fits.
internal fun findBestSize(widgetSize: DpSize, layoutSizes: Collection<DpSize>): DpSize? =
    layoutSizes.mapNotNull { layoutSize ->
        if (layoutSize fitsIn widgetSize) {
            layoutSize to squareDistance(widgetSize, layoutSize)
        } else {
            null
        }
    }.minByOrNull { it.second }?.first

/**
 * @return the minimum size as configured by the App Widget provider.
 */
internal fun AppWidgetProviderInfo.getMinSize(displayMetrics: DisplayMetrics): DpSize {
    val minWidth = min(
        minWidth,
        if (resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0) {
            minResizeWidth
        } else {
            Int.MAX_VALUE
        }
    )
    val minHeight = min(
        minHeight,
        if (resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0) {
            minResizeHeight
        } else {
            Int.MAX_VALUE
        }
    )
    return DpSize(minWidth.pixelsToDp(displayMetrics), minHeight.pixelsToDp(displayMetrics))
}

internal fun Collection<DpSize>.sortedBySize() =
    sortedWith(compareBy({ it.width.value * it.height.value }, { it.width.value }))

internal fun logException(throwable: Throwable) {
    Log.e(GlanceAppWidgetTag, "Error in Glance App Widget", throwable)
}

/**
 * [Tracing] contains methods for tracing sections of GlanceAppWidget.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Tracing {
    val enabled = AtomicBoolean(false)

    fun beginGlanceAppWidgetUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && enabled.get()) {
            TracingApi29Impl.beginAsyncSection("GlanceAppWidget::update", 0)
        }
    }

    fun endGlanceAppWidgetUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && enabled.get()) {
            TracingApi29Impl.endAsyncSection("GlanceAppWidget::update", 0)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
internal object TracingApi29Impl {
    @DoNotInline
    fun beginAsyncSection(
        methodName: String,
        cookie: Int,
    ) = Trace.beginAsyncSection(methodName, cookie)

    @DoNotInline
    fun endAsyncSection(
        methodName: String,
        cookie: Int,
    ) = Trace.endAsyncSection(methodName, cookie)
}