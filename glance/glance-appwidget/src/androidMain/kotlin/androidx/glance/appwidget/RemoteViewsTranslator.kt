@file:OptIn(GlanceInternalApi::class)
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

package androidx.glance.appwidget

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.glance.Emittable
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.PaddingModifier
import androidx.glance.unit.Dp
import kotlin.math.floor

private fun Alignment.Horizontal.toGravity(): Int =
    when (this) {
        Alignment.Horizontal.Start -> Gravity.START
        Alignment.Horizontal.End -> Gravity.END
        Alignment.Horizontal.CenterHorizontally -> Gravity.CENTER_HORIZONTAL
        else -> throw IllegalArgumentException("Unknown horizontal alignment: $this")
    }

private fun Alignment.Vertical.toGravity(): Int =
    when (this) {
        Alignment.Vertical.Top -> Gravity.TOP
        Alignment.Vertical.Bottom -> Gravity.BOTTOM
        Alignment.Vertical.CenterVertically -> Gravity.CENTER_VERTICAL
        else -> throw IllegalArgumentException("Unknown vertical alignment: $this")
    }

private fun Alignment.toGravity() = horizontal.toGravity() or vertical.toGravity()

private fun applyPadding(
    rv: RemoteViews,
    modifier: PaddingModifier,
    resources: Resources
) {
    val displayMetrics = resources.displayMetrics
    val isRtl = modifier.rtlAware &&
        resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    val start = dpToPixel(modifier.start, displayMetrics)
    val end = dpToPixel(modifier.end, displayMetrics)
    rv.setViewPadding(
        R.id.glanceView,
        if (isRtl) end else start,
        dpToPixel(modifier.top, displayMetrics),
        if (isRtl) start else end,
        dpToPixel(modifier.bottom, displayMetrics),
    )
}

private fun applyModifiers(context: Context, rv: RemoteViews, modifiers: Modifier) {
    modifiers.foldOut(Unit) { modifier, _ ->
        when (modifier) {
            is PaddingModifier -> applyPadding(rv, modifier, context.resources)
        }
    }
}

private fun translateEmittableBox(context: Context, element: EmittableBox): RemoteViews =
    remoteViews(context, R.layout.box_layout)
        .also { rv ->
            rv.setInt(R.id.glanceView, "setGravity", element.contentAlignment.toGravity())
            applyModifiers(context, rv, element.modifier)
            element.children.forEach {
                rv.addView(R.id.glanceView, translateChild(context, it))
            }
        }

private fun translateChild(context: Context, element: Emittable): RemoteViews {
    return when (element) {
        is EmittableBox -> translateEmittableBox(context, element)
        else -> throw IllegalArgumentException("Unknown element type ${element::javaClass}")
    }
}

private fun dpToPixel(dp: Dp, displayMetrics: DisplayMetrics) =
    floor(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.value, displayMetrics)).toInt()

private fun remoteViews(context: Context, @LayoutRes layoutId: Int) =
    RemoteViews(context.packageName, layoutId)

internal fun translateComposition(context: Context, element: RemoteViewsRoot): RemoteViews {
    if (element.children.size == 1) {
        return translateChild(context, element.children[0])
    }
    return translateChild(context, EmittableBox().also { it.children.addAll(element.children) })
}
