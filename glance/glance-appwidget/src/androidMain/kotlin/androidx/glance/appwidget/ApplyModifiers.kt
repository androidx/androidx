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
import android.util.Log
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.DoNotInline
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.core.widget.setTextViewHeight
import androidx.core.widget.setTextViewWidth
import androidx.core.widget.setViewBackgroundColor
import androidx.core.widget.setViewBackgroundColorResource
import androidx.glance.BackgroundModifier
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.ActionModifier
import androidx.glance.action.LaunchActivityAction
import androidx.glance.action.LaunchActivityClassAction
import androidx.glance.action.LaunchActivityComponentAction
import androidx.glance.action.UpdateContentAction
import androidx.glance.appwidget.action.LaunchActivityIntentAction
import androidx.glance.appwidget.unit.DayNightColorProvider
import androidx.glance.layout.Dimension
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.layout.collectPaddingInDp
import androidx.glance.unit.Color
import androidx.glance.unit.FixedColorProvider
import androidx.glance.unit.ResourceColorProvider
import kotlin.math.roundToInt

internal fun applyModifiers(
    translationContext: TranslationContext,
    rv: RemoteViews,
    modifiers: GlanceModifier,
    layoutDef: RemoteViewsInfo
) {
    val context = translationContext.context
    var widthModifier: WidthModifier? = null
    var heightModifier: HeightModifier? = null
    modifiers.foldIn(Unit) { _, modifier ->
        when (modifier) {
            is ActionModifier -> applyAction(rv, modifier.action, context, layoutDef.mainViewId)
            is WidthModifier -> widthModifier = modifier
            is HeightModifier -> heightModifier = modifier
            is BackgroundModifier -> applyBackgroundModifier(
                rv,
                modifier,
                context,
                layoutDef
            )
            is PaddingModifier -> {
            } // Nothing to do for those
            else -> {
                Log.w(GlanceAppWidgetTag, "Unknown modifier '$modifier', nothing done.")
            }
        }
    }
    applySizeModifiers(rv, widthModifier, heightModifier, translationContext, layoutDef)
    modifiers.collectPaddingInDp(context.resources)
        ?.toAbsolute(translationContext.isRtl)
        ?.let {
            val displayMetrics = context.resources.displayMetrics
            rv.setViewPadding(
                layoutDef.mainViewId,
                it.left.toPixels(displayMetrics),
                it.top.toPixels(displayMetrics),
                it.right.toPixels(displayMetrics),
                it.bottom.toPixels(displayMetrics)
            )
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
            val intent = when (action) {
                is LaunchActivityComponentAction -> Intent().setComponent(action.componentName)
                is LaunchActivityClassAction -> Intent(context, action.activityClass)
                is LaunchActivityIntentAction -> action.intent
                else -> error("Action type not defined in app widget package: $action")
            }

            val pendingIntent: PendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_MUTABLE
                )
            rv.setOnClickPendingIntent(viewId, pendingIntent)
        }
        is UpdateContentAction -> {
            val pendingIntent =
                ActionRunnableBroadcastReceiver.createPendingIntent(
                    context,
                    action.runnableClass,
                    action.parameters
                )
            rv.setOnClickPendingIntent(viewId, pendingIntent)
        }
        else -> throw IllegalArgumentException("Unrecognized action type.")
    }
}

private fun applySizeModifiers(
    rv: RemoteViews,
    widthModifier: WidthModifier?,
    heightModifier: HeightModifier?,
    translationContext: TranslationContext,
    layoutDef: RemoteViewsInfo
) {
    val context = translationContext.context
    if (layoutDef.isSimple) {
        widthModifier?.let { applySimpleWidthModifier(rv, it, context, layoutDef) }
        heightModifier?.let { applySimpleHeightModifier(rv, it, context, layoutDef) }
        return
    }

    val width = widthModifier?.width
    val height = heightModifier?.height

    if (!(width.isFixed || height.isFixed)) {
        // The sizing view is only present and needed for setting fixed dimensions.
        return
    }

    val useMatchSizeWidth = width is Dimension.Fill || width is Dimension.Expand
    val useMatchSizeHeight = height is Dimension.Fill || height is Dimension.Expand
    val sizeViewLayout = when {
        useMatchSizeWidth && useMatchSizeHeight -> R.layout.size_match_match
        useMatchSizeWidth -> R.layout.size_match_wrap
        useMatchSizeHeight -> R.layout.size_wrap_match
        else -> R.layout.size_wrap_wrap
    }

    val sizeTargetViewId = rv.inflateViewStub(translationContext, R.id.sizeViewStub, sizeViewLayout)

    fun Dimension.Dp.toPixels() = dp.toPixels(context)
    fun Dimension.Resource.toPixels() = context.resources.getDimensionPixelSize(res)
    when (width) {
        is Dimension.Dp -> rv.setTextViewWidth(sizeTargetViewId, width.toPixels())
        is Dimension.Resource -> rv.setTextViewWidth(sizeTargetViewId, width.toPixels())
        Dimension.Expand, Dimension.Fill, Dimension.Wrap, null -> {}
    }.let {}
    when (height) {
        is Dimension.Dp -> rv.setTextViewHeight(sizeTargetViewId, height.toPixels())
        is Dimension.Resource -> rv.setTextViewHeight(sizeTargetViewId, height.toPixels())
        Dimension.Expand, Dimension.Fill, Dimension.Wrap, null -> {}
    }.let {}
}

private fun applySimpleWidthModifier(
    rv: RemoteViews,
    modifier: WidthModifier,
    context: Context,
    layoutDef: RemoteViewsInfo,
) {
    // These layouts already have the appropriate attribute in the xml, so no action is needed.
    val width = modifier.width
    if (
        width.resolveDimension(context) in listOf(Dimension.Wrap, Dimension.Fill, Dimension.Expand)
    ) {
        return
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        throw IllegalArgumentException(
            "Using a width of $width requires a complex layout before API 31, but used $layoutDef"
        )
    }

    ApplyModifiersApi31Impl.setViewWidth(rv, layoutDef.mainViewId, width)
}

private fun applySimpleHeightModifier(
    rv: RemoteViews,
    modifier: HeightModifier,
    context: Context,
    layoutDef: RemoteViewsInfo,
) {
    // These layouts already have the appropriate attribute in the xml, so no action is needed.
    val height = modifier.height
    if (
        height.resolveDimension(context) in listOf(Dimension.Wrap, Dimension.Fill, Dimension.Expand)
    ) {
        return
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        throw IllegalArgumentException(
            "Using a height of $height requires a complex layout before API 31, but used $layoutDef"
        )
    }

    ApplyModifiersApi31Impl.setViewHeight(rv, layoutDef.mainViewId, height)
}

private fun applyBackgroundModifier(
    rv: RemoteViews,
    modifier: BackgroundModifier,
    context: Context,
    layoutDef: RemoteViewsInfo
) {
    val viewId = layoutDef.mainViewId
    when (val colorProvider = modifier.colorProvider) {
        is FixedColorProvider -> rv.setViewBackgroundColor(viewId, colorProvider.color.toArgb())
        is ResourceColorProvider -> rv.setViewBackgroundColorResource(viewId, colorProvider.resId)
        is DayNightColorProvider -> {
            if (Build.VERSION.SDK_INT >= 31) {
                rv.setViewBackgroundColor(
                    viewId,
                    colorProvider.day.toArgb(),
                    colorProvider.night.toArgb()
                )
            } else {
                rv.setViewBackgroundColor(viewId, colorProvider.resolve(context).toArgb())
            }
        }
        else -> Log.w(GlanceAppWidgetTag, "Unexpected background color modifier: $colorProvider")
    }
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
                rv.setViewLayoutWidth(viewId, WRAP_CONTENT.toFloat(), COMPLEX_UNIT_PX)
            }
            is Dimension.Expand -> rv.setViewLayoutWidth(viewId, 0f, COMPLEX_UNIT_PX)
            is Dimension.Dp -> rv.setViewLayoutWidth(viewId, width.dp.value, COMPLEX_UNIT_DIP)
            is Dimension.Resource -> rv.setViewLayoutWidthDimen(viewId, width.res)
            Dimension.Fill -> rv.setViewLayoutWidth(viewId, MATCH_PARENT.toFloat(), COMPLEX_UNIT_PX)
        }.let {}
    }

    @DoNotInline
    fun setViewHeight(rv: RemoteViews, viewId: Int, height: Dimension) {
        when (height) {
            is Dimension.Wrap -> {
                rv.setViewLayoutHeight(viewId, WRAP_CONTENT.toFloat(), COMPLEX_UNIT_PX)
            }
            is Dimension.Expand -> rv.setViewLayoutHeight(viewId, 0f, COMPLEX_UNIT_PX)
            is Dimension.Dp -> rv.setViewLayoutHeight(viewId, height.dp.value, COMPLEX_UNIT_DIP)
            is Dimension.Resource -> rv.setViewLayoutHeightDimen(viewId, height.res)
            Dimension.Fill -> {
                rv.setViewLayoutHeight(viewId, MATCH_PARENT.toFloat(), COMPLEX_UNIT_PX)
            }
        }.let {}
    }
}

private val Dimension?.isFixed: Boolean
    get() = when (this) {
        is Dimension.Dp, is Dimension.Resource -> true
        Dimension.Expand, Dimension.Fill, Dimension.Wrap, null -> false
    }