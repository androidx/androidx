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
import android.view.ViewGroup
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.glance.layout.Dimension
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier
import androidx.compose.ui.unit.dp

/**
 * Information about a generated layout, including the layout id, ids of elements within, and other
 * details about the layout contents.
 */
internal data class LayoutInfo(
    @LayoutRes val layoutId: Int,
    val mainViewId: Int = R.id.glanceView,
)

/**
 * Information about a [RemoteViews] created from generated layouts, including the layout id, ids
 * of elements within, and other details about the layout contents.
 */
internal data class RemoteViewsInfo(
    val remoteViews: RemoteViews,
    val mainViewId: Int = R.id.glanceView,
    val isComplex: Boolean
)

internal val RemoteViewsInfo.isSimple get() = !isComplex

/**
 * The total number of generated layouts.
 */
internal val GeneratedLayoutCount = generatedLayouts.size

/**
 * Layout selector.
 *
 * This class is used to select a particular layout in [generatedLayouts].
 */
internal data class LayoutSelector(
    val type: Type,
    val width: Size,
    val height: Size,
) {

    internal enum class Size {
        Wrap,
        Fixed,
        Expand,
        MatchParent,
    }

    internal enum class Type {
        Row,
        Column,
        Box,
        Text,
        List,
        ListItem,
        CheckBox,
        CheckBoxBackport,
        Button,

        // Note: Java keywords, such as 'switch', can't be used for layout ids.
        Swtch,
        SwtchBackport
    }
}

internal data class ComplexSelector(
    val width: LayoutSelector.Size,
    val height: LayoutSelector.Size,
)

internal fun createRemoteViews(
    translationContext: TranslationContext,
    type: LayoutSelector.Type,
    modifier: GlanceModifier
): RemoteViewsInfo {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val layout = selectApi31Layout(type, modifier)
        return RemoteViewsInfo(
            remoteViews(translationContext, layout.layoutId),
            mainViewId = layout.mainViewId,
            isComplex = false,
        )
    }
    val context = translationContext.context
    val widthMod = modifier.findModifier<WidthModifier>()?.width ?: Dimension.Wrap
    val heightMod = modifier.findModifier<HeightModifier>()?.height ?: Dimension.Wrap
    val width = widthMod.resolveDimension(context).toSpecSize()
    val height = heightMod.resolveDimension(context).toSpecSize()
    val needResize = width == LayoutSelector.Size.Fixed || height == LayoutSelector.Size.Fixed
    if (needResize) {
        val complexLayout = generatedComplexLayouts[ComplexSelector(width, height)]
            ?: throw IllegalArgumentException(
                "Could not find complex layout for width=$width, height=$height"
            )
        val childLayout = generatedLayouts[LayoutSelector(
            type,
            LayoutSelector.Size.MatchParent,
            LayoutSelector.Size.MatchParent
        )]
            ?: throw IllegalArgumentException(
                "Could not find layout for $type, width=${LayoutSelector.Size.MatchParent}, " +
                    "height=${LayoutSelector.Size.MatchParent}"
            )
        val rv = remoteViews(translationContext, complexLayout.layoutId)
        val viewId = rv.inflateViewStub(
            translationContext,
            R.id.glanceViewStub,
            childLayout.layoutId
        )

        fun layoutName(id: Int) =
            context.resources.getResourceEntryName(id)!!
        return RemoteViewsInfo(rv, mainViewId = viewId, isComplex = true)
    }
    val layout = generatedLayouts[LayoutSelector(type, width, height)]
        ?: throw IllegalArgumentException(
            "Could not find layout for $type, width=$width, height=$height"
        )
    val rv = remoteViews(translationContext, layout.layoutId)
    return RemoteViewsInfo(rv, mainViewId = layout.mainViewId, isComplex = false)
}

private fun Dimension.toSpecSize(): LayoutSelector.Size =
    when (this) {
        is Dimension.Wrap -> LayoutSelector.Size.Wrap
        is Dimension.Expand -> LayoutSelector.Size.Expand
        is Dimension.Fill -> LayoutSelector.Size.MatchParent
        is Dimension.Dp, is Dimension.Resource -> LayoutSelector.Size.Fixed
    }

internal fun Dimension.resolveDimension(context: Context): Dimension {
    if (this !is Dimension.Resource) return this
    val sizePx = context.resources.getDimension(res)
    return when (sizePx.toInt()) {
        ViewGroup.LayoutParams.MATCH_PARENT -> Dimension.Fill
        ViewGroup.LayoutParams.WRAP_CONTENT -> Dimension.Wrap
        else -> Dimension.Dp((sizePx / context.resources.displayMetrics.density).dp)
    }
}

/**
 * For API 31, we will always select layouts marked as non-resizable, as starting Android S, we
 * can always resize views and we want the simplest layout possible.
 */
private fun selectApi31Layout(
    type: LayoutSelector.Type,
    modifier: GlanceModifier
): LayoutInfo {
    val widthMod = modifier.findModifier<WidthModifier>()?.width ?: Dimension.Wrap
    val heightMod = modifier.findModifier<HeightModifier>()?.height ?: Dimension.Wrap
    val width = widthMod.toSpecSize()
    val height = heightMod.toSpecSize()
    return generatedLayouts[LayoutSelector(
        type,
        width,
        height,
    )]
        ?: throw IllegalArgumentException(
            "Could not find layout for $type, width=$width, height=$height, canResize=false"
        )
}
