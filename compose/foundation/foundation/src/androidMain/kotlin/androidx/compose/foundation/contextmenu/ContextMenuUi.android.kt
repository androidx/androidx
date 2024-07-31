/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.contextmenu

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Layout constants from the [Material 3 Menu Spec](https://m3.material.io/components/menus/specs).
 */
@VisibleForTesting
internal object ContextMenuSpec {
    // dimensions
    val ContainerWidthMin = 112.dp
    val ContainerWidthMax = 280.dp
    val ListItemHeight = 48.dp
    val MenuContainerElevation = 3.dp
    val CornerRadius = 4.dp
    val LabelVerticalTextAlignment = Alignment.CenterVertically
    val LabelHorizontalTextAlignment = TextAlign.Start
    val HorizontalPadding = 12.dp // left/right of column and between elements in rows
    val VerticalPadding = 8.dp // top/bottom of column and around dividers
    val IconSize = 24.dp

    // text
    val FontSize = 14.sp
    val FontWeight = Medium
    val LineHeight = 20.sp
    val LetterSpacing = 0.1f.sp

    fun textStyle(color: Color): TextStyle =
        TextStyle(
            color = color,
            textAlign = LabelHorizontalTextAlignment,
            fontSize = FontSize,
            fontWeight = FontWeight,
            lineHeight = LineHeight,
            letterSpacing = LetterSpacing,
        )
}

private val DefaultPopupProperties = PopupProperties(focusable = true)

@Composable
internal fun ContextMenuPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
) {
    ContextMenuPopup(
        popupPositionProvider = popupPositionProvider,
        onDismiss = onDismiss,
        modifier = modifier,
        colors = computeContextMenuColors(),
        contextMenuBuilderBlock = contextMenuBuilderBlock
    )
}

@VisibleForTesting
@Composable
internal fun ContextMenuPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ContextMenuColors,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
) {
    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismiss,
        properties = DefaultPopupProperties,
    ) {
        ContextMenuColumn(colors, modifier) {
            val scope = remember { ContextMenuScope() }
            with(scope) {
                clear()
                contextMenuBuilderBlock()
                Content(colors)
            }
        }
    }
}

@VisibleForTesting
@Composable
internal fun ContextMenuColumn(
    colors: ContextMenuColors,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .shadow(
                    ContextMenuSpec.MenuContainerElevation,
                    RoundedCornerShape(ContextMenuSpec.CornerRadius)
                )
                .background(colors.backgroundColor)
                .width(IntrinsicSize.Max)
                .padding(vertical = ContextMenuSpec.VerticalPadding)
                .verticalScroll(rememberScrollState()),
        content = content,
    )
}

// Very similar to M3 DropdownMenuItemContent
@SuppressLint("ComposableLambdaParameterPosition")
@VisibleForTesting
@Composable
internal fun ContextMenuItem(
    label: String,
    enabled: Boolean,
    colors: ContextMenuColors,
    modifier: Modifier = Modifier,
    /**
     * Icon to place in front of the label. If null, the icon will not be rendered and the text will
     * instead be further towards the start. The `iconColor` will change based on whether the item
     * is disabled or not.
     */
    leadingIcon: @Composable ((iconColor: Color) -> Unit)? = null,
    /**
     * Lambda called when this item is clicked.
     *
     * Note: If you want the context menu to close when this item is clicked, you will have to do it
     * in this lambda via [ContextMenuState.close].
     */
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = ContextMenuSpec.LabelVerticalTextAlignment,
        horizontalArrangement = Arrangement.spacedBy(ContextMenuSpec.HorizontalPadding),
        modifier =
            modifier
                .clickable(
                    enabled = enabled,
                    onClickLabel = label,
                ) {
                    // Semantics can call this even if it is disabled (at least in tests),
                    // so check enabled status again before invoking any callbacks.
                    if (enabled) onClick()
                }
                .fillMaxWidth()
                .sizeIn(
                    minWidth = ContextMenuSpec.ContainerWidthMin,
                    maxWidth = ContextMenuSpec.ContainerWidthMax,
                    minHeight = ContextMenuSpec.ListItemHeight,
                    maxHeight = ContextMenuSpec.ListItemHeight,
                )
                .padding(horizontal = ContextMenuSpec.HorizontalPadding)
    ) {
        leadingIcon?.let { icon ->
            Box(
                modifier =
                    Modifier.requiredSizeIn(
                        minWidth = ContextMenuSpec.IconSize,
                        maxWidth = ContextMenuSpec.IconSize,
                        maxHeight = ContextMenuSpec.IconSize,
                    )
            ) {
                icon(if (enabled) colors.iconColor else colors.disabledIconColor)
            }
        }
        BasicText(
            text = label,
            style =
                ContextMenuSpec.textStyle(
                    color = if (enabled) colors.textColor else colors.disabledTextColor,
                ),
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = true)
        )
    }
}

/** Scope used to add components to a context menu. */
// We cannot expose a @Composable in the context menu API because we don't want folks adding
// arbitrary composables into a context menu. Instead, we expose this API which then maps to
// context menu composables.
internal class ContextMenuScope internal constructor() {
    private val composables = mutableStateListOf<@Composable (colors: ContextMenuColors) -> Unit>()

    @Composable
    internal fun Content(colors: ContextMenuColors) {
        composables.fastForEach { composable -> composable(colors) }
    }

    internal fun clear() {
        composables.clear()
    }

    /**
     * Adds an item to the context menu list.
     *
     * @param label string to display in the text of the item.
     * @param modifier [Modifier] to apply to the item.
     * @param enabled whether or not the item should be enabled. This affects whether the item is
     *   clickable, has a hover indication, and the text styling.
     * @param leadingIcon Composable to put in the leading icon position. The color is the color to
     *   draw with and will change based on if the item is enabled or not. This will measured with a
     *   required width of `24.dp` and a required maxHeight of `24.dp`. The result will be centered
     *   vertically in the row.
     * @param onClick Action to perform on the item being clicked. Returns whether or not the
     *   context menu should be dismissed.
     */
    fun item(
        label: @Composable () -> String,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        /**
         * Icon to place in front of the label. If null, the icon will not be rendered and the text
         * will instead be further towards the start. The `iconColor` will change based on whether
         * the item is disabled or not. The size of this composable will be
         * [ContextMenuSpec.IconSize].
         */
        leadingIcon: @Composable ((iconColor: Color) -> Unit)? = null,
        /**
         * Lambda called when this item is clicked.
         *
         * Note: If you want the context menu to close when this item is clicked, you will have to
         * do it in this lambda via [ContextMenuState.close].
         */
        onClick: () -> Unit,
    ) {
        composables += { colors ->
            val resolvedLabel = label()
            checkPrecondition(resolvedLabel.isNotBlank()) { "Label must not be blank" }
            ContextMenuItem(
                modifier = modifier,
                label = resolvedLabel,
                enabled = enabled,
                colors = colors,
                leadingIcon = leadingIcon,
                onClick = onClick
            )
        }
    }
}

private const val DisabledAlpha = 0.38f

@VisibleForTesting
internal val DefaultContextMenuColors =
    ContextMenuColors(
        backgroundColor = Color.White,
        textColor = Color.Black,
        iconColor = Color.Black,
        disabledTextColor = Color.Black.copy(alpha = DisabledAlpha),
        disabledIconColor = Color.Black.copy(alpha = DisabledAlpha),
    )

/**
 * Colors to apply to the context menu.
 *
 * @param backgroundColor Color of the background in the context menu
 * @param textColor Color of the text in context menu items
 * @param iconColor Color of any icons in context menu items
 * @param disabledTextColor Color of disabled text in context menu items
 * @param disabledIconColor Color of any disabled icons in context menu items
 */
@VisibleForTesting
@Stable
internal class ContextMenuColors(
    val backgroundColor: Color,
    val textColor: Color,
    val iconColor: Color,
    val disabledTextColor: Color,
    val disabledIconColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ContextMenuColors) return false

        if (this.backgroundColor != other.backgroundColor) return false
        if (this.textColor != other.textColor) return false
        if (this.iconColor != other.iconColor) return false
        if (this.disabledTextColor != other.disabledTextColor) return false
        if (this.disabledIconColor != other.disabledIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + textColor.hashCode()
        result = 31 * result + iconColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        result = 31 * result + disabledIconColor.hashCode()
        return result
    }

    override fun toString(): String =
        "ContextMenuColors(" +
            "backgroundColor=$backgroundColor, " +
            "textColor=$textColor, " +
            "iconColor=$iconColor, " +
            "disabledTextColor=$disabledTextColor, " +
            "disabledIconColor=$disabledIconColor" +
            ")"
}

@VisibleForTesting
@Composable
internal fun computeContextMenuColors(
    @StyleRes backgroundStyleId: Int = R.style.Widget_PopupMenu,
    @StyleRes foregroundStyleId: Int = R.style.TextAppearance_Widget_PopupMenu_Large,
): ContextMenuColors {
    val context = LocalContext.current
    return remember(context, LocalConfiguration.current) {
        val backgroundColor =
            context.resolveColor(
                backgroundStyleId,
                R.attr.colorBackground,
                DefaultContextMenuColors.backgroundColor,
            )

        val textColorStateList =
            context.resolveColorStateList(
                foregroundStyleId,
                R.attr.textColorPrimary,
            )
        val enabledColor = textColorStateList.enabledColor(DefaultContextMenuColors.textColor)
        val disabledColor =
            textColorStateList.disabledColor(DefaultContextMenuColors.disabledTextColor)

        ContextMenuColors(
            backgroundColor = backgroundColor,
            textColor = enabledColor,
            iconColor = enabledColor,
            disabledTextColor = disabledColor,
            disabledIconColor = disabledColor,
        )
    }
}

private fun Context.resolveColor(
    @StyleRes resId: Int,
    @AttrRes attrId: Int,
    defaultColor: Color
): Color {
    val typedArray = obtainStyledAttributes(resId, intArrayOf(attrId))
    val defaultColorAndroid = defaultColor.toArgb()
    val colorInt = typedArray.getColor(0, defaultColorAndroid)
    typedArray.recycle()
    return if (colorInt == defaultColorAndroid) defaultColor else Color(colorInt)
}

private fun Context.resolveColorStateList(
    @StyleRes resId: Int,
    @AttrRes attrId: Int,
): ColorStateList? {
    val typedArray = obtainStyledAttributes(resId, intArrayOf(attrId))
    val colorStateList = typedArray.getColorStateList(0)
    typedArray.recycle()
    return colorStateList
}

private fun ColorStateList?.enabledColor(defaultColor: Color): Color {
    val defaultColorArgb = defaultColor.toArgb()
    val color = this?.getColorForState(intArrayOf(R.attr.state_enabled), defaultColorArgb)
    return if (color == null || color == defaultColorArgb) defaultColor else Color(color)
}

private fun ColorStateList?.disabledColor(defaultColor: Color): Color {
    val defaultColorArgb = defaultColor.toArgb()
    val color = this?.getColorForState(intArrayOf(-R.attr.state_enabled), defaultColorArgb)
    return if (color == null || color == defaultColorArgb) defaultColor else Color(color)
}
