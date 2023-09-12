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
import android.os.Build
import android.util.Log
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.toArgb
import androidx.core.widget.RemoteViewsCompat.setTextViewHeight
import androidx.core.widget.RemoteViewsCompat.setTextViewWidth
import androidx.core.widget.RemoteViewsCompat.setViewBackgroundColor
import androidx.core.widget.RemoteViewsCompat.setViewBackgroundColorResource
import androidx.core.widget.RemoteViewsCompat.setViewBackgroundResource
import androidx.core.widget.RemoteViewsCompat.setViewClipToOutline
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.BackgroundModifier
import androidx.glance.GlanceModifier
import androidx.glance.Visibility
import androidx.glance.VisibilityModifier
import androidx.glance.action.ActionModifier
import androidx.glance.appwidget.action.applyAction
import androidx.glance.color.DayNightColorProvider
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.semantics.SemanticsModifier
import androidx.glance.semantics.SemanticsProperties
import androidx.glance.unit.Dimension
import androidx.glance.unit.FixedColorProvider
import androidx.glance.unit.ResourceColorProvider

internal fun applyModifiers(
    translationContext: TranslationContext,
    rv: RemoteViews,
    modifiers: GlanceModifier,
    viewDef: InsertedViewInfo,
) {
    val context = translationContext.context
    var widthModifier: WidthModifier? = null
    var heightModifier: HeightModifier? = null
    var paddingModifiers: PaddingModifier? = null
    var cornerRadius: Dimension? = null
    var visibility = Visibility.Visible
    var actionModifier: ActionModifier? = null
    var enabled: EnabledModifier? = null
    var clipToOutline: ClipToOutlineModifier? = null
    var semanticsModifier: SemanticsModifier? = null
    modifiers.foldIn(Unit) { _, modifier ->
        when (modifier) {
            is ActionModifier -> {
                if (actionModifier != null) {
                    Log.w(
                        GlanceAppWidgetTag,
                        "More than one clickable defined on the same GlanceModifier, " +
                            "only the last one will be used."
                    )
                }
                actionModifier = modifier
            }

            is WidthModifier -> widthModifier = modifier
            is HeightModifier -> heightModifier = modifier
            is BackgroundModifier -> applyBackgroundModifier(context, rv, modifier, viewDef)
            is PaddingModifier -> {
                paddingModifiers = paddingModifiers?.let { it + modifier } ?: modifier
            }

            is VisibilityModifier -> visibility = modifier.visibility
            is CornerRadiusModifier -> cornerRadius = modifier.radius
            is AppWidgetBackgroundModifier -> {
                // This modifier is handled somewhere else.
            }

            is SelectableGroupModifier -> {
                if (!translationContext.canUseSelectableGroup) {
                    error(
                        "GlanceModifier.selectableGroup() can only be used on Row or Column " +
                            "composables."
                    )
                }
            }

            is AlignmentModifier -> {
                // This modifier is handled somewhere else.
            }

            is ClipToOutlineModifier -> clipToOutline = modifier
            is EnabledModifier -> enabled = modifier
            is SemanticsModifier -> semanticsModifier = modifier
            else -> {
                Log.w(GlanceAppWidgetTag, "Unknown modifier '$modifier', nothing done.")
            }
        }
    }
    applySizeModifiers(translationContext, rv, widthModifier, heightModifier, viewDef)
    actionModifier?.let { applyAction(translationContext, rv, it.action, viewDef.mainViewId) }
    cornerRadius?.let { applyRoundedCorners(rv, viewDef.mainViewId, it) }
    paddingModifiers?.let { padding ->
        val absolutePadding = padding.toDp(context.resources).toAbsolute(translationContext.isRtl)
        val displayMetrics = context.resources.displayMetrics
        rv.setViewPadding(
            viewDef.mainViewId,
            absolutePadding.left.toPixels(displayMetrics),
            absolutePadding.top.toPixels(displayMetrics),
            absolutePadding.right.toPixels(displayMetrics),
            absolutePadding.bottom.toPixels(displayMetrics)
        )
    }
    clipToOutline?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setBoolean(viewDef.mainViewId, "setClipToOutline", true)
        }
    }
    enabled?.let {
        rv.setBoolean(viewDef.mainViewId, "setEnabled", it.enabled)
    }
    semanticsModifier?.let { semantics ->
        val contentDescription: List<String>? =
            semantics.configuration.getOrNull(SemanticsProperties.ContentDescription)
        if (contentDescription != null) {
            rv.setContentDescription(viewDef.mainViewId, contentDescription.joinToString())
        }
    }
    rv.setViewVisibility(viewDef.mainViewId, visibility.toViewVisibility())
}

private fun Visibility.toViewVisibility() =
    when (this) {
        Visibility.Visible -> View.VISIBLE
        Visibility.Invisible -> View.INVISIBLE
        Visibility.Gone -> View.GONE
    }

private fun applySizeModifiers(
    translationContext: TranslationContext,
    rv: RemoteViews,
    widthModifier: WidthModifier?,
    heightModifier: HeightModifier?,
    viewDef: InsertedViewInfo
) {
    val context = translationContext.context
    if (viewDef.isSimple) {
        widthModifier?.let { applySimpleWidthModifier(context, rv, it, viewDef.mainViewId) }
        heightModifier?.let { applySimpleHeightModifier(context, rv, it, viewDef.mainViewId) }
        return
    }

    check(Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        "There is currently no valid use case where a complex view is used on Android S"
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
        Dimension.Expand, Dimension.Fill, Dimension.Wrap, null -> {
        }
    }.let {}
    when (height) {
        is Dimension.Dp -> rv.setTextViewHeight(sizeTargetViewId, height.toPixels())
        is Dimension.Resource -> rv.setTextViewHeight(sizeTargetViewId, height.toPixels())
        Dimension.Expand, Dimension.Fill, Dimension.Wrap, null -> {
        }
    }.let {}
}

internal fun applySimpleWidthModifier(
    context: Context,
    rv: RemoteViews,
    modifier: WidthModifier,
    viewId: Int,
) {
    val width = modifier.width
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        // Prior to Android S, these layouts already have the appropriate attribute in the xml, so
        // no action is needed.
        if (
            width.resolveDimension(context) in listOf(
                Dimension.Wrap,
                Dimension.Fill,
                Dimension.Expand
            )
        ) {
            return
        }
        throw IllegalArgumentException(
            "Using a width of $width requires a complex layout before API 31"
        )
    }
    // Wrap and Expand are done in XML on Android S & Sv2
    if (Build.VERSION.SDK_INT < 33 &&
        width in listOf(Dimension.Wrap, Dimension.Expand)
    ) return
    ApplyModifiersApi31Impl.setViewWidth(rv, viewId, width)
}

internal fun applySimpleHeightModifier(
    context: Context,
    rv: RemoteViews,
    modifier: HeightModifier,
    viewId: Int,
) {
    // These layouts already have the appropriate attribute in the xml, so no action is needed.
    val height = modifier.height
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        // Prior to Android S, these layouts already have the appropriate attribute in the xml, so
        // no action is needed.
        if (
            height.resolveDimension(context) in listOf(
                Dimension.Wrap,
                Dimension.Fill,
                Dimension.Expand
            )
        ) {
            return
        }
        throw IllegalArgumentException(
            "Using a height of $height requires a complex layout before API 31"
        )
    }
    // Wrap and Expand are done in XML on Android S & Sv2
    if (Build.VERSION.SDK_INT < 33 &&
        height in listOf(Dimension.Wrap, Dimension.Expand)
    ) return
    ApplyModifiersApi31Impl.setViewHeight(rv, viewId, height)
}

private fun applyBackgroundModifier(
    context: Context,
    rv: RemoteViews,
    modifier: BackgroundModifier,
    viewDef: InsertedViewInfo
) {
    val viewId = viewDef.mainViewId

    fun applyBackgroundImageModifier(modifier: BackgroundModifier.Image) {
        val imageProvider = modifier.imageProvider
        if (imageProvider is AndroidResourceImageProvider) {
            rv.setViewBackgroundResource(viewId, imageProvider.resId)
        }
        // Otherwise, the background has been transformed and should be ignored
        // (removing modifiers is not really possible).
        return
    }

    fun applyBackgroundColorModifier(modifier: BackgroundModifier.Color) {
        when (val colorProvider = modifier.colorProvider) {
            is FixedColorProvider -> rv.setViewBackgroundColor(
                viewId,
                colorProvider.color.toArgb()
            )

            is ResourceColorProvider -> rv.setViewBackgroundColorResource(
                viewId,
                colorProvider.resId
            )

            is DayNightColorProvider -> {
                if (Build.VERSION.SDK_INT >= 31) {
                    rv.setViewBackgroundColor(
                        viewId,
                        colorProvider.day.toArgb(),
                        colorProvider.night.toArgb()
                    )
                } else {
                    rv.setViewBackgroundColor(viewId, colorProvider.getColor(context).toArgb())
                }
            }

            else -> Log.w(
                GlanceAppWidgetTag,
                "Unexpected background color modifier: $colorProvider"
            )
        }
    }

    when (modifier) {
        is BackgroundModifier.Image -> applyBackgroundImageModifier(modifier)
        is BackgroundModifier.Color -> applyBackgroundColorModifier(modifier)
    }
}

private val Dimension?.isFixed: Boolean
    get() = when (this) {
        is Dimension.Dp, is Dimension.Resource -> true
        Dimension.Expand, Dimension.Fill, Dimension.Wrap, null -> false
    }

private fun applyRoundedCorners(rv: RemoteViews, viewId: Int, radius: Dimension) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ApplyModifiersApi31Impl.applyRoundedCorners(rv, viewId, radius)
        return
    }
    Log.w(GlanceAppWidgetTag, "Cannot set the rounded corner of views before Api 31.")
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
            Dimension.Fill -> {
                rv.setViewLayoutWidth(viewId, MATCH_PARENT.toFloat(), COMPLEX_UNIT_PX)
            }
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

    @DoNotInline
    fun applyRoundedCorners(rv: RemoteViews, viewId: Int, radius: Dimension) {
        rv.setViewClipToOutline(viewId, true)
        when (radius) {
            is Dimension.Dp -> {
                rv.setViewOutlinePreferredRadius(viewId, radius.dp.value, COMPLEX_UNIT_DIP)
            }

            is Dimension.Resource -> {
                rv.setViewOutlinePreferredRadiusDimen(viewId, radius.res)
            }

            else -> error("Rounded corners should not be ${radius.javaClass.canonicalName}")
        }
    }
}
