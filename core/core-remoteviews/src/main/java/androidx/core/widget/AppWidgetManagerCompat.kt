@file:JvmName("AppWidgetManagerCompat")

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

package androidx.core.widget

import android.appwidget.AppWidgetManager
import android.content.res.Resources
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.util.SizeFCompat
import kotlin.math.ceil

/** Returns whether this size is approximately at least as big as [other] in all dimensions. */
internal infix fun SizeFCompat.approxDominates(other: SizeFCompat): Boolean {
    return ceil(width) + 1 >= other.width && ceil(height) + 1 >= other.height
}

internal val SizeFCompat.area: Float
    get() = width * height

/**
 * Updates the app widget with [appWidgetId], creating a [RemoteViews] for each size assigned to
 * the app widget by [AppWidgetManager], invoking [factory] to create each alternative view.
 *
 * This provides ["exact" sizing](https://developer.android.com/guide/topics/appwidgets/layouts#provide-exact-layouts)
 * , which allows you to tailor your app widget appearance to the exact size at which it is
 * displayed. If you are only concerned with a small number of size thresholds, it is preferable
 * to use "responsive" sizing by providing a fixed set of sizes that your app widget supports.
 *
 * As your [factory] may be invoked multiple times, if there is expensive computation of state that
 * is shared among each size, it is recommended to perform that computation before calling this
 * and cache the results as necessary.
 *
 * To handle resizing of your app widget, it is necessary to call this function during both
 * [android.appwidget.AppWidgetProvider.onUpdate] and
 * [android.appwidget.AppWidgetProvider.onAppWidgetOptionsChanged].
 *
 * @param appWidgetId the id of the app widget
 * @param factory a function to create a [RemoteViews] for a given width and height (in dp)
 */
public fun AppWidgetManager.updateAppWidget(
    appWidgetId: Int,
    factory: (SizeFCompat) -> RemoteViews
) {
    updateAppWidget(appWidgetId, createExactSizeAppWidget(this, appWidgetId, factory))
}

/**
 * Creates a [RemoteViews] associated with each size assigned to the app widget by
 * [AppWidgetManager], invoking [factory] to create each alternative view.
 *
 * This provides ["exact" sizing](https://developer.android.com/guide/topics/appwidgets/layouts#provide-exact-layouts)
 * , which allows you to tailor your app widget appearance to the exact size at which it is
 * displayed. If you are only concerned with a small number of size thresholds, it is preferable
 * to use "responsive" sizing by providing a fixed set of sizes that your app widget supports.
 *
 * As your [factory] may be invoked multiple times, if there is expensive computation of state that
 * is shared among each size, it is recommended to perform that computation before calling this
 * and cache the results as necessary.
 *
 * To handle resizing of your app widget, it is necessary to call [AppWidgetManager.updateAppWidget]
 * during both [android.appwidget.AppWidgetProvider.onUpdate] and
 * [android.appwidget.AppWidgetProvider.onAppWidgetOptionsChanged].
 *
 * @param appWidgetManager the [AppWidgetManager] to provide information about [appWidgetId]
 * @param appWidgetId the id of the app widget
 * @param factory a function to create a [RemoteViews] for a given width and height (in dp)
 */
public fun createExactSizeAppWidget(
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    factory: (SizeFCompat) -> RemoteViews
): RemoteViews {
    appWidgetManager.requireValidAppWidgetId(appWidgetId)
    return when {
        SDK_INT >= 31 -> {
            AppWidgetManagerApi31Impl.createExactSizeAppWidget(
                appWidgetManager,
                appWidgetId,
                factory
            )
        }
        SDK_INT >= 16 -> {
            AppWidgetManagerApi16Impl.createExactSizeAppWidget(
                appWidgetManager,
                appWidgetId,
                factory
            )
        }
        else -> createAppWidgetFromProviderInfo(appWidgetManager, appWidgetId, factory)
    }
}

/**
 * Updates the app widget with [appWidgetId], creating a [RemoteViews] for each size provided in
 * [dpSizes].
 *
 * This provides ["responsive" sizing](https://developer.android.com/guide/topics/appwidgets/layouts#provide-responsive-layouts)
 * , which allows for smoother resizing and a more consistent experience across different host
 * configurations.
 *
 * As your [factory] may be invoked multiple times, if there is expensive computation of state that
 * is shared among each size, it is recommended to perform that computation before calling this
 * and cache the results as necessary.
 *
 * To handle resizing of your app widget, it is necessary to call this function during both
 * [android.appwidget.AppWidgetProvider.onUpdate] and
 * [android.appwidget.AppWidgetProvider.onAppWidgetOptionsChanged]. If your app's minSdk is 31 or
 * higher, it is only necessary to call this function during `onUpdate`.
 *
 * @param appWidgetId the id of the app widget
 * @param dpSizes a collection of sizes (in dp) that your app widget supports. Must not be empty or
 * contain more than 16 elements.
 * @param factory a function to create a [RemoteViews] for a given width and height (in dp). It is
 * guaranteed that [factory] will only ever be called with the values provided in [dpSizes].
 */
public fun AppWidgetManager.updateAppWidget(
    appWidgetId: Int,
    dpSizes: Collection<SizeFCompat>,
    factory: (SizeFCompat) -> RemoteViews
) {
    updateAppWidget(appWidgetId, createResponsiveSizeAppWidget(this, appWidgetId, dpSizes, factory))
}

/**
 * Creating a [RemoteViews] associated with each size provided in [dpSizes].
 *
 * This provides ["responsive" sizing](https://developer.android.com/guide/topics/appwidgets/layouts#provide-responsive-layouts)
 * , which allows for smoother resizing and a more consistent experience across different host
 * configurations.
 *
 * As your [factory] may be invoked multiple times, if there is expensive computation of state that
 * is shared among each size, it is recommended to perform that computation before calling this
 * and cache the results as necessary.
 *
 * To handle resizing of your app widget, it is necessary to call [AppWidgetManager.updateAppWidget]
 * during both [android.appwidget.AppWidgetProvider.onUpdate] and
 * [android.appwidget.AppWidgetProvider.onAppWidgetOptionsChanged]. If your app's minSdk is 31 or
 * higher, it is only necessary to call `updateAppWidget` during `onUpdate`.
 *
 * @param appWidgetManager the [AppWidgetManager] to provide information about [appWidgetId]
 * @param appWidgetId the id of the app widget
 * @param dpSizes a collection of sizes (in dp) that your app widget supports. Must not be empty or
 * contain more than 16 elements.
 * @param factory a function to create a [RemoteViews] for a given width and height (in dp). It is
 * guaranteed that [factory] will only ever be called with the values provided in [dpSizes].
 */
public fun createResponsiveSizeAppWidget(
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    dpSizes: Collection<SizeFCompat>,
    factory: (SizeFCompat) -> RemoteViews
): RemoteViews {
    appWidgetManager.requireValidAppWidgetId(appWidgetId)
    require(dpSizes.isNotEmpty()) { "Sizes cannot be empty" }
    require(dpSizes.size <= 16) { "At most 16 sizes may be provided" }
    return when {
        SDK_INT >= 31 -> AppWidgetManagerApi31Impl.createResponsiveSizeAppWidget(dpSizes, factory)
        SDK_INT >= 16 -> {
            AppWidgetManagerApi16Impl.createResponsiveSizeAppWidget(
                appWidgetManager,
                appWidgetId,
                dpSizes,
                factory
            )
        }
        else -> createAppWidgetFromProviderInfo(appWidgetManager, appWidgetId, factory)
    }
}

private fun AppWidgetManager.requireValidAppWidgetId(appWidgetId: Int) {
    requireNotNull(getAppWidgetInfo(appWidgetId)) { "Invalid app widget id: $appWidgetId" }
}

@RequiresApi(31)
@Suppress("DEPRECATION")
private object AppWidgetManagerApi31Impl {
    @DoNotInline
    fun createExactSizeAppWidget(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        factory: (SizeFCompat) -> RemoteViews
    ): RemoteViews {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
        if (sizes.isNullOrEmpty()) {
            Log.w(
                LogTag,
                "App widget SizeF sizes not found in the options bundle, falling back to the " +
                    "min/max sizes"
            )
            return AppWidgetManagerApi16Impl.createExactSizeAppWidget(
                appWidgetManager,
                appWidgetId,
                factory
            )
        }
        return RemoteViews(sizes.associateWith { factory(it.toSizeFCompat()) })
    }

    @DoNotInline
    fun createResponsiveSizeAppWidget(
        dpSizes: Collection<SizeFCompat>,
        factory: (SizeFCompat) -> RemoteViews
    ): RemoteViews {
        return RemoteViews(dpSizes.associate { it.toSizeF() to factory(it) })
    }

    private fun SizeF.toSizeFCompat() = SizeFCompat.toSizeFCompat(this)
}

@RequiresApi(16)
private object AppWidgetManagerApi16Impl {
    @DoNotInline
    fun createExactSizeAppWidget(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        factory: (SizeFCompat) -> RemoteViews
    ): RemoteViews {
        val (landscapeSize, portraitSize) =
            getSizesFromOptionsBundle(appWidgetManager, appWidgetId)
                ?: run {
                    Log.w(
                        LogTag,
                        "App widget sizes not found in the options bundle, falling back to the " +
                            "provider size"
                    )
                    return createAppWidgetFromProviderInfo(appWidgetManager, appWidgetId, factory)
                }
        return createAppWidget(landscapeSize = landscapeSize, portraitSize = portraitSize, factory)
    }

    @DoNotInline
    fun createResponsiveSizeAppWidget(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        sizes: Collection<SizeFCompat>,
        factory: (SizeFCompat) -> RemoteViews
    ): RemoteViews {
        val minSize = sizes.minByOrNull { it.area } ?: error("Sizes cannot be empty")
        val (landscapeSize, portraitSize) =
            getSizesFromOptionsBundle(appWidgetManager, appWidgetId)
                ?: run {
                    Log.w(
                        LogTag,
                        "App widget sizes not found in the options bundle, falling back to the " +
                            "smallest supported size ($minSize)"
                    )
                    LandscapePortraitSizes(minSize, minSize)
                }
        val effectiveLandscapeSize =
            sizes.filter { landscapeSize approxDominates it }.maxByOrNull { it.area } ?: minSize
        val effectivePortraitSize =
            sizes.filter { portraitSize approxDominates it }.maxByOrNull { it.area } ?: minSize
        return createAppWidget(
            landscapeSize = effectiveLandscapeSize,
            portraitSize = effectivePortraitSize,
            factory
        )
    }

    private fun createAppWidget(
        landscapeSize: SizeFCompat,
        portraitSize: SizeFCompat,
        factory: (SizeFCompat) -> RemoteViews
    ): RemoteViews {
        return if (landscapeSize == portraitSize) {
            factory(landscapeSize)
        } else {
            RemoteViews(
                /* landscape= */ factory(landscapeSize),
                /* portrait= */ factory(portraitSize)
            )
        }
    }

    private fun getSizesFromOptionsBundle(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ): LandscapePortraitSizes? {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

        val portWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1)
        val portHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, -1)
        if (portWidthDp < 0 || portHeightDp < 0) return null

        val landWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, -1)
        val landHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1)
        if (landWidthDp < 0 || landHeightDp < 0) return null

        return LandscapePortraitSizes(
            landscape = SizeFCompat(landWidthDp.toFloat(), landHeightDp.toFloat()),
            portrait = SizeFCompat(portWidthDp.toFloat(), portHeightDp.toFloat())
        )
    }
}

internal fun createAppWidgetFromProviderInfo(
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    factory: (SizeFCompat) -> RemoteViews
): RemoteViews {
    return factory(appWidgetManager.getSizeFromProviderInfo(appWidgetId))
}

internal fun AppWidgetManager.getSizeFromProviderInfo(appWidgetId: Int): SizeFCompat {
    val providerInfo = getAppWidgetInfo(appWidgetId)
    fun pxToDp(value: Int) = (value / Resources.getSystem().displayMetrics.density)
    val width = pxToDp(providerInfo.minWidth)
    val height = pxToDp(providerInfo.minHeight)
    return SizeFCompat(width, height)
}

internal data class LandscapePortraitSizes(val landscape: SizeFCompat, val portrait: SizeFCompat)

private const val LogTag = "AppWidgetManagerCompat"
