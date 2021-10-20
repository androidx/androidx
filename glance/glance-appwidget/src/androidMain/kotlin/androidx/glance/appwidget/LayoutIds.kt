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
import androidx.annotation.LayoutRes
import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.glance.layout.Dimension
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.unit.dp

/**
 * Information about a generated layout, including the layout id, ids of elements within, and other
 * details about the layout contents.
 */
internal data class LayoutInfo(
    @LayoutRes val layoutId: Int,
    val mainViewId: Int = R.id.glanceView,
    val isComplex: Boolean,
)

internal val LayoutInfo.isSimple get() = !isComplex

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
    val canResize: Boolean,
    val childCount: Int
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
        List1,
        List2,
        List3,
        ListItem,
        CheckBox,
        CheckBoxBackport,
        Button,
        // Note: Java keywords, such as 'switch', can't be used for layout ids.
        Swtch,
        SwtchBackport
    }
}

/**
 * Select the layout based on the specification.
 *
 * @param type Type of layout
 * @param modifier Modifier applied to the layout. Modifiers of interest will be extracted to get
 * the layout they can be applied on.
 */
internal fun selectLayout(
    translationContext: TranslationContext,
    type: LayoutSelector.Type,
    modifier: GlanceModifier
): LayoutInfo {
    val context = translationContext.context
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return selectApi31Layout(type, modifier)
    }
    val widthMod = modifier.findModifier<WidthModifier>()?.width ?: Dimension.Wrap
    val heightMod = modifier.findModifier<HeightModifier>()?.height ?: Dimension.Wrap
    val width = widthMod.resolveDimension(context).toSpecSize()
    val height = heightMod.resolveDimension(context).toSpecSize()
    val needResize = width == LayoutSelector.Size.Fixed || height == LayoutSelector.Size.Fixed
    // TODO(b/202868171): Add the real child count.
    return generatedLayouts[LayoutSelector(type, width, height, needResize, childCount = 0)]
        ?: (if (!needResize) generatedLayouts[LayoutSelector(
            type,
            width,
            height,
            true,
            // TODO(b/202868171): Add the real child count.
            childCount = 0
        )] else null)
        ?: throw IllegalArgumentException(
            "Could not find layout for $type, width=$width, height=$height, canResize=$needResize"
        )
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
        canResize = false,
        // TODO(b/202868171): Add the real child count.
        childCount = 0
    )]
        ?: throw IllegalArgumentException(
            "Could not find layout for $type, width=$width, height=$height, canResize=false"
        )
}
