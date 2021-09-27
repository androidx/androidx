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
import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.core.widget.setTextViewHeight
import androidx.core.widget.setTextViewWidth
import androidx.glance.Modifier
import androidx.glance.action.Action
import androidx.glance.action.ActionModifier
import androidx.glance.action.LaunchActivityAction
import androidx.glance.action.UpdateAction
import androidx.glance.layout.Dimension
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.unit.dp

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
    resources: Resources,
    @IdRes viewId: Int
) {
    val displayMetrics = resources.displayMetrics
    val isRtl = modifier.rtlAware &&
        resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
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
                context.resources,
                layoutDef.mainViewId
            )
            is WidthModifier -> applyWidthModifier(
                rv,
                modifier,
                context.resources,
                layoutDef,
                translationContext.sizeContext
            )
            is HeightModifier -> applyHeightModifier(
                rv,
                modifier,
                context.resources,
                layoutDef,
                translationContext.sizeContext
            )
        }
    }
}

private fun applyWidthModifier(
    rv: RemoteViews,
    modifier: WidthModifier,
    resources: Resources,
    layoutDef: LayoutIds?,
    sizeContext: SizeContext,
) {
    checkNotNull(layoutDef) { "No layout spec, cannot change size" }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (modifier.width is Dimension.Expand && sizeContext.allowExpandingWidth) {
            ApplyModifiersApi31Impl.setViewWidth(rv, layoutDef.mainViewId, Dimension.Dp(0.dp))
        } else {
            ApplyModifiersApi31Impl.setViewWidth(rv, layoutDef.mainViewId, modifier.width)
        }
        return
    }
    val width = modifier.width
    if (width !is Dimension.Dp) return
    checkNotNull(layoutDef.sizeViewId) { "The layout specified does not allow specifying the size" }
    rv.setTextViewWidth(layoutDef.sizeViewId, width.dp.toPixels(resources.displayMetrics))
}

private fun applyHeightModifier(
    rv: RemoteViews,
    modifier: HeightModifier,
    resources: Resources,
    layoutDef: LayoutIds?,
    sizeContext: SizeContext,
) {
    checkNotNull(layoutDef) { "No layout spec, cannot change size" }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (modifier.height is Dimension.Expand && sizeContext.allowExpandingHeight) {
            ApplyModifiersApi31Impl.setViewHeight(rv, layoutDef.mainViewId, Dimension.Dp(0.dp))
        } else {
            ApplyModifiersApi31Impl.setViewHeight(rv, layoutDef.mainViewId, modifier.height)
        }
        return
    }
    val height = modifier.height
    if (height !is Dimension.Dp) return
    checkNotNull(layoutDef.sizeViewId) { "The layout specified does not allow specifying the size" }
    rv.setTextViewHeight(layoutDef.sizeViewId, height.dp.toPixels(resources.displayMetrics))
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
        }
    }
}