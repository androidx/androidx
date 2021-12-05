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
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.BitmapImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.EmittableImage
import androidx.glance.IconImageProvider
import androidx.glance.appwidget.GlanceAppWidgetTag
import androidx.glance.appwidget.LayoutType
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.UriImageProvider
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.insertView

internal fun RemoteViews.translateEmittableImage(
    translationContext: TranslationContext,
    element: EmittableImage
) {
    val selector = when (element.contentScale) {
        ContentScale.Crop -> LayoutType.ImageCrop
        ContentScale.Fit -> LayoutType.ImageFit
        ContentScale.FillBounds -> LayoutType.ImageFillBounds
        else -> {
            Log.w(GlanceAppWidgetTag, "Unsupported ContentScale user: ${element.contentScale}")
            LayoutType.ImageFit
        }
    }
    val viewDef = insertView(translationContext, selector, element.modifier)
    setContentDescription(viewDef.mainViewId, element.contentDescription)
    when (val provider = element.provider) {
        is AndroidResourceImageProvider -> setImageViewResource(
            viewDef.mainViewId,
            provider.resId
        )
        is BitmapImageProvider -> setImageViewBitmap(viewDef.mainViewId, provider.bitmap)
        is UriImageProvider -> setImageViewUri(viewDef.mainViewId, provider.uri)
        is IconImageProvider -> setImageViewIcon(this, viewDef.mainViewId, provider)
        else ->
            throw IllegalArgumentException("An unsupported ImageProvider type was used.")
    }
    applyModifiers(translationContext, this, element.modifier, viewDef)
}

private fun setImageViewIcon(rv: RemoteViews, viewId: Int, provider: IconImageProvider) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        throw IllegalStateException("Cannot use Icon ImageProvider before API 23.")
    }
    ImageTranslatorApi23Impl.setImageViewIcon(rv, viewId, provider.icon)
}

@RequiresApi(Build.VERSION_CODES.M)
private object ImageTranslatorApi23Impl {
    @DoNotInline
    fun setImageViewIcon(rv: RemoteViews, viewId: Int, icon: Icon) {
        rv.setImageViewIcon(viewId, icon)
    }
}
