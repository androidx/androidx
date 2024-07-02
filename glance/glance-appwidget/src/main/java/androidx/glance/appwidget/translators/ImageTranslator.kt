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

package androidx.glance.appwidget.translators

import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.widget.RemoteViewsCompat.setImageViewAdjustViewBounds
import androidx.core.widget.RemoteViewsCompat.setImageViewColorFilter
import androidx.core.widget.RemoteViewsCompat.setImageViewColorFilterResource
import androidx.core.widget.RemoteViewsCompat.setImageViewImageAlpha
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.BitmapImageProvider
import androidx.glance.ColorFilterParams
import androidx.glance.EmittableImage
import androidx.glance.IconImageProvider
import androidx.glance.TintColorFilterParams
import androidx.glance.appwidget.GlanceAppWidgetTag
import androidx.glance.appwidget.InsertedViewInfo
import androidx.glance.appwidget.LayoutType
import androidx.glance.appwidget.TintAndAlphaColorFilterParams
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.UriImageProvider
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.insertView
import androidx.glance.color.DayNightColorProvider
import androidx.glance.findModifier
import androidx.glance.isDecorative
import androidx.glance.layout.ContentScale
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.Dimension
import androidx.glance.unit.ResourceColorProvider

internal fun RemoteViews.translateEmittableImage(
    translationContext: TranslationContext,
    element: EmittableImage
) {
    val selector = element.getLayoutSelector()
    val viewDef = insertView(translationContext, selector, element.modifier)
    when (val provider = element.provider) {
        is AndroidResourceImageProvider -> setImageViewResource(viewDef.mainViewId, provider.resId)
        is BitmapImageProvider -> setImageViewBitmap(viewDef.mainViewId, provider.bitmap)
        is UriImageProvider -> setImageViewUri(viewDef.mainViewId, provider.uri)
        is IconImageProvider -> setImageViewIcon(this, viewDef.mainViewId, provider)
        else -> throw IllegalArgumentException("An unsupported ImageProvider type was used.")
    }
    element.colorFilterParams?.let { applyColorFilter(translationContext, this, it, viewDef) }
    applyModifiers(translationContext, this, element.modifier, viewDef)

    // If the content scale is Fit, the developer has expressed that they want the image to
    // maintain its aspect ratio. AdjustViewBounds on ImageView tells the view to rescale to
    // maintain its aspect ratio. This only really makes sense if one of the dimensions is set to
    // wrap, that is, should change to match the content.
    val shouldAdjustViewBounds =
        element.contentScale == ContentScale.Fit &&
            (element.modifier.findModifier<WidthModifier>()?.width == Dimension.Wrap ||
                element.modifier.findModifier<HeightModifier>()?.height == Dimension.Wrap)
    setImageViewAdjustViewBounds(viewDef.mainViewId, shouldAdjustViewBounds)
}

private fun EmittableImage.getLayoutSelector(): LayoutType {
    // Defaults to "decorative" if semantics / contentDescription is not set or contentDescription
    // is null or empty.
    val isDecorative = isDecorative()
    return when (contentScale) {
        ContentScale.Crop ->
            if (isDecorative) {
                LayoutType.ImageCropDecorative
            } else {
                LayoutType.ImageCrop
            }
        ContentScale.Fit ->
            if (isDecorative) {
                LayoutType.ImageFitDecorative
            } else {
                LayoutType.ImageFit
            }
        ContentScale.FillBounds ->
            if (isDecorative) {
                LayoutType.ImageFillBoundsDecorative
            } else {
                LayoutType.ImageFillBounds
            }
        else -> {
            Log.w(GlanceAppWidgetTag, "Unsupported ContentScale user: $contentScale")
            LayoutType.ImageFit
        }
    }
}

private fun applyColorFilter(
    translationContext: TranslationContext,
    rv: RemoteViews,
    colorFilterParams: ColorFilterParams,
    viewDef: InsertedViewInfo
) {
    when (colorFilterParams) {
        is TintColorFilterParams -> {
            val colorProvider = colorFilterParams.colorProvider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ImageTranslatorApi31Impl.applyTintColorFilter(
                    translationContext,
                    rv,
                    colorProvider,
                    viewDef.mainViewId
                )
            } else {
                rv.setImageViewColorFilter(
                    viewDef.mainViewId,
                    colorProvider.getColor(translationContext.context).toArgb()
                )
            }
        }
        is TintAndAlphaColorFilterParams -> {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                val color =
                    colorFilterParams.colorProvider.getColor(translationContext.context).toArgb()
                rv.setImageViewColorFilter(viewDef.mainViewId, color)
                rv.setImageViewImageAlpha(viewDef.mainViewId, android.graphics.Color.alpha(color))
            } else {
                val trace = Throwable()
                Log.e(
                    GlanceAppWidgetTag,
                    "There is no use case yet to support this colorFilter in S+ versions.",
                    trace
                )
            }
        }
        else -> throw IllegalArgumentException("An unsupported ColorFilter was used.")
    }
}

private fun setImageViewIcon(rv: RemoteViews, viewId: Int, provider: IconImageProvider) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        throw IllegalStateException("Cannot use Icon ImageProvider before API 23.")
    }
    ImageTranslatorApi23Impl.setImageViewIcon(rv, viewId, provider.icon)
}

@RequiresApi(Build.VERSION_CODES.M)
private object ImageTranslatorApi23Impl {
    fun setImageViewIcon(rv: RemoteViews, viewId: Int, icon: Icon) {
        rv.setImageViewIcon(viewId, icon)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object ImageTranslatorApi31Impl {
    fun applyTintColorFilter(
        translationContext: TranslationContext,
        rv: RemoteViews,
        colorProvider: ColorProvider,
        viewId: Int
    ) {
        when (colorProvider) {
            is DayNightColorProvider ->
                rv.setImageViewColorFilter(viewId, colorProvider.day, colorProvider.night)
            is ResourceColorProvider ->
                rv.setImageViewColorFilterResource(viewId, colorProvider.resId)
            else ->
                rv.setImageViewColorFilter(
                    viewId,
                    colorProvider.getColor(translationContext.context).toArgb()
                )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun RemoteViews.setImageViewColorFilter(viewId: Int, notNight: Color, night: Color) {
    setImageViewColorFilter(viewId = viewId, notNight = notNight.toArgb(), night = night.toArgb())
}
