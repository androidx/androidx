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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.DoNotInline
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.core.widget.setTextViewHeight
import androidx.core.widget.setTextViewWidth
import androidx.core.widget.setViewBackgroundColor
import androidx.glance.BackgroundModifier
import androidx.glance.Modifier
import androidx.glance.action.Action
import androidx.glance.action.ActionModifier
import androidx.glance.action.LaunchActivityAction
import androidx.glance.action.UpdateAction
import androidx.glance.layout.Dimension
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.unit.Color
import androidx.glance.unit.dp
import kotlin.math.roundToInt

internal fun applyModifiers(
    translationContext: TranslationContext,
    rv: RemoteViews,
    modifiers: Modifier,
    layoutDef: LayoutIds
) {
    val context = translationContext.context
    modifiers.foldOut(Unit) { modifier, _ ->
        when (modifier) {
            is ActionModifier -> applyAction(rv, modifier.action, context, layoutDef.mainViewId)
            is PaddingModifier -> applyPadding(
                rv,
                modifier,
                translationContext,
                layoutDef.mainViewId
            )
            is WidthModifier -> applyWidthModifier(
                rv,
                modifier,
                context,
                layoutDef
            )
            is HeightModifier -> applyHeightModifier(
                rv,
                modifier,
                context,
                layoutDef
            )
            is BackgroundModifier -> applyBackgroundModifier(
                rv,
                modifier,
                layoutDef
            )
        }
    }
}

private fun applyAction(
    rv: RemoteViews,
    action: Action,
    context: Context,
    @IdRes viewId: Int
) {
    when (action) {
        is LaunchActivityAction -> {
            val intent = Intent(context, action.activityClass)
            val pendingIntent: PendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_MUTABLE
                )
            rv.setOnClickPendingIntent(viewId, pendingIntent)
        }
        is UpdateAction -> {
            val pendingIntent =
                ActionRunnableBroadcastReceiver.createPendingIntent(context, action.runnableClass)
            rv.setOnClickPendingIntent(viewId, pendingIntent)
        }
        else -> throw IllegalArgumentException("Unrecognized action type.")
    }
}

private fun applyPadding(
    rv: RemoteViews,
    modifier: PaddingModifier,
    translationContext: TranslationContext,
    @IdRes viewId: Int
) {
    val displayMetrics = translationContext.context.resources.displayMetrics
    val isRtl = modifier.rtlAware && translationContext.isRtl
    val start = modifier.start.toPixels(displayMetrics)
    val end = modifier.end.toPixels(displayMetrics)
    rv.setViewPadding(
        viewId,
        if (isRtl) end else start,
        modifier.top.toPixels(displayMetrics),
        if (isRtl) start else end,
        modifier.bottom.toPixels(displayMetrics),
    )
}

private fun applyWidthModifier(
    rv: RemoteViews,
    modifier: WidthModifier,
    context: Context,
    layoutDef: LayoutIds?,
) {
    checkNotNull(layoutDef) { "No layout spec, cannot change size" }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (modifier.width is Dimension.Expand) {
            ApplyModifiersApi31Impl.setViewWidth(rv, layoutDef.mainViewId, Dimension.Dp(0.dp))
        } else {
            ApplyModifiersApi31Impl.setViewWidth(rv, layoutDef.mainViewId, modifier.width)
        }
        return
    }
    val widthPx = modifier.width.toPixels(context)
    // Sizes in pixel must be >= 0 to be valid
    if (widthPx < 0) return
    checkNotNull(layoutDef.sizeViewId) { "The layout specified does not allow specifying the size" }
    rv.setTextViewWidth(layoutDef.sizeViewId, widthPx)
}

/**
 * Returns the dimension, in pixels, from the given context or a negative value if the Dimension is
 * not in Pixels (i.e. Wrap, Fill or Expand).
 *
 * @return the size in Pixel, or a negative number if the dimension cannot be computed (e.g.
 * set by choosing the correct layout).
 */
private fun Dimension.toPixels(context: Context): Int {
    val resources = context.resources
    return when (this) {
        is Dimension.Dp -> dp.toPixels(resources.displayMetrics)
        is Dimension.Resource -> resources.getDimensionPixelSize(res)
        else -> -1
    }
}

private fun applyHeightModifier(
    rv: RemoteViews,
    modifier: HeightModifier,
    context: Context,
    layoutDef: LayoutIds?,
) {
    checkNotNull(layoutDef) { "No layout spec, cannot change size" }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (modifier.height is Dimension.Expand) {
            ApplyModifiersApi31Impl.setViewHeight(rv, layoutDef.mainViewId, Dimension.Dp(0.dp))
        } else {
            ApplyModifiersApi31Impl.setViewHeight(rv, layoutDef.mainViewId, modifier.height)
        }
        return
    }
    val heightPx = modifier.height.toPixels(context)
    if (heightPx < 0) return
    checkNotNull(layoutDef.sizeViewId) { "The layout specified does not allow specifying the size" }
    rv.setTextViewHeight(layoutDef.sizeViewId, heightPx)
}

private fun applyBackgroundModifier(
    rv: RemoteViews,
    modifier: BackgroundModifier,
    layoutDef: LayoutIds
) {
    rv.setViewBackgroundColor(layoutDef.mainViewId, modifier.color.toArgb())
}

// TODO(b/202150620): Use the shared Compose utility when we use the same Color class.
@ColorInt
private fun Color.toArgb(): Int {
    // Converts a value from a float in [0,1] to an int in [0x00,0xFF].
    fun Float.toColorComponent() = (this * 0xFF).roundToInt()
    return android.graphics.Color.argb(
        /* alpha= */ alpha.toColorComponent(),
        /* red= */ red.toColorComponent(),
        /* green= */ green.toColorComponent(),
        /* blue= */ blue.toColorComponent()
    )
}

@RequiresApi(Build.VERSION_CODES.S)
private object ApplyModifiersApi31Impl {
    @DoNotInline
    fun setViewWidth(rv: RemoteViews, viewId: Int, width: Dimension) {
        when (width) {
            is Dimension.Wrap -> {
                rv.setViewLayoutWidth(
                    viewId,
                    ViewGroup.LayoutParams.WRAP_CONTENT.toFloat(),
                    TypedValue.COMPLEX_UNIT_PX
                )
            }
            is Dimension.Expand -> {
                rv.setViewLayoutWidth(
                    viewId,
                    ViewGroup.LayoutParams.MATCH_PARENT.toFloat(),
                    TypedValue.COMPLEX_UNIT_PX
                )
            }
            is Dimension.Dp -> {
                rv.setViewLayoutWidth(viewId, width.dp.value, TypedValue.COMPLEX_UNIT_DIP)
            }
            is Dimension.Resource -> rv.setViewLayoutWidthDimen(viewId, width.res)
        }
    }

    @DoNotInline
    fun setViewHeight(rv: RemoteViews, viewId: Int, height: Dimension) {
        when (height) {
            is Dimension.Wrap -> {
                rv.setViewLayoutHeight(
                    viewId,
                    ViewGroup.LayoutParams.WRAP_CONTENT.toFloat(),
                    TypedValue.COMPLEX_UNIT_PX
                )
            }
            is Dimension.Expand -> {
                rv.setViewLayoutHeight(
                    viewId,
                    ViewGroup.LayoutParams.MATCH_PARENT.toFloat(),
                    TypedValue.COMPLEX_UNIT_PX
                )
            }
            is Dimension.Dp -> {
                rv.setViewLayoutHeight(viewId, height.dp.value, TypedValue.COMPLEX_UNIT_DIP)
            }
            is Dimension.Resource -> rv.setViewLayoutHeightDimen(viewId, height.res)
        }
    }
}