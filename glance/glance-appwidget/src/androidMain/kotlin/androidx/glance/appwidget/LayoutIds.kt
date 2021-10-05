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

import android.os.Build
import androidx.glance.Modifier
import androidx.glance.findModifier
import androidx.glance.layout.Dimension
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier

/**
 * Set of ids defining a layout.
 *
 * It contains the id of the layout itself, and the ids of the elements within the layout that
 * the code may need to access.
 */
internal data class LayoutIds(
    val layoutId: Int,
    val mainViewId: Int = R.id.glanceView,
    val sizeViewId: Int? = null,
)

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
        Button
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
    type: LayoutSelector.Type,
    modifier: Modifier
): LayoutIds {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return selectApi31Layout(type, modifier)
    }
    val widthMod = modifier.findModifier<WidthModifier>()?.width ?: Dimension.Wrap
    val heightMod = modifier.findModifier<HeightModifier>()?.height ?: Dimension.Wrap
    val needResize = widthMod is Dimension.Dp || heightMod is Dimension.Dp
    val width = widthMod.toSpecSize()
    val height = heightMod.toSpecSize()
    return generatedLayouts[LayoutSelector(type, width, height, needResize)]
        ?: (if (!needResize) generatedLayouts[LayoutSelector(type, width, height, true)] else null)
        ?: throw IllegalArgumentException(
            "Could not find layout for $type, width=$width, height=$height, canResize=$needResize"
        )
}

private fun Dimension.toSpecSize(): LayoutSelector.Size =
    when (this) {
        is Dimension.Dp -> LayoutSelector.Size.Fixed
        is Dimension.Wrap -> LayoutSelector.Size.Wrap
        is Dimension.Expand -> LayoutSelector.Size.Expand
        is Dimension.Fill -> LayoutSelector.Size.MatchParent
    }

/**
 * For API 31, we will always select layouts marked as non-resizable, as starting Android S, we
 * can always resize views and we want the simplest layout possible.
 */
private fun selectApi31Layout(
    type: LayoutSelector.Type,
    modifier: Modifier
): LayoutIds {
    val widthMod = modifier.findModifier<WidthModifier>()?.width ?: Dimension.Wrap
    val heightMod = modifier.findModifier<HeightModifier>()?.height ?: Dimension.Wrap
    val width = widthMod.toSpecSize()
    val height = heightMod.toSpecSize()
    return generatedLayouts[LayoutSelector(type, width, height, canResize = false)]
        ?: throw IllegalArgumentException(
            "Could not find layout for $type, width=$width, height=$height, canResize=false"
        )
}
