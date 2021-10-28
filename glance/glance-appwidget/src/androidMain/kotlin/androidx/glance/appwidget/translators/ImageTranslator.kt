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
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.LayoutSelector
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.createRemoteViews
import androidx.glance.appwidget.layout.UriImageProvider
import androidx.glance.appwidget.remoteViews
import androidx.glance.layout.AndroidResourceImageProvider
import androidx.glance.layout.BitmapImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.EmittableImage
import androidx.glance.layout.IconImageProvider

internal fun translateEmittableImage(
    translationContext: TranslationContext,
    element: EmittableImage
): RemoteViews {
    val selector = when (element.contentScale) {
        ContentScale.Crop -> LayoutSelector.Type.ImageCrop
        ContentScale.Fit -> LayoutSelector.Type.ImageFit
        ContentScale.FillBounds -> LayoutSelector.Type.ImageFillBounds
        else -> throw IllegalArgumentException("An unsupported ContentScale type was used.")
    }
    val layoutDef = createRemoteViews(translationContext, selector, element.modifier)
    val rv = layoutDef.remoteViews
    rv.setContentDescription(layoutDef.mainViewId, element.contentDescription)
    when (val provider = element.provider) {
        is AndroidResourceImageProvider -> rv.setImageViewResource(
            layoutDef.mainViewId,
            provider.resId
        )
        is BitmapImageProvider -> rv.setImageViewBitmap(layoutDef.mainViewId, provider.bitmap)
        is UriImageProvider -> rv.setImageViewUri(layoutDef.mainViewId, provider.uri)
        is IconImageProvider -> setImageViewIcon(rv, layoutDef.mainViewId, provider)
        else ->
            throw IllegalArgumentException("An unsupported ImageProvider type was used.")
    }
    applyModifiers(translationContext, rv, element.modifier, layoutDef)
    return rv
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
