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

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

private const val DisabledAlpha = 0.38f
private const val IconAlpha = 0.6f

@VisibleForTesting
internal object ContextMenuSpec {
    // TODO(b/331955999) Determine colors/theming
    private val PrimaryColor = Color.Black
    val BackgroundColor = Color.White

    val TextColor = PrimaryColor
    val IconColor = PrimaryColor.copy(alpha = IconAlpha)
    val DisabledColor = PrimaryColor.copy(DisabledAlpha)

    // Layout constants from https://m3.material.io/components/menus/specs
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
}

private val DefaultPopupProperties = PopupProperties(focusable = true)

@Composable
internal fun ContextMenuPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
) {
    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismiss,
        properties = DefaultPopupProperties,
    ) {
        ContextMenuColumn(modifier) {
            val scope = remember { ContextMenuScope() }
            with(scope) {
                clear()
                contextMenuBuilderBlock()
                Content()
            }
        }
    }
}

@VisibleForTesting
@Composable
internal fun ContextMenuColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(
                ContextMenuSpec.MenuContainerElevation,
                RoundedCornerShape(ContextMenuSpec.CornerRadius)
            )
            .background(ContextMenuSpec.BackgroundColor)
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
    modifier: Modifier = Modifier,
    /**
     * Icon to place in front of the label. If null, the icon will not be rendered
     * and the text will instead be further towards the start. The `iconColor` will
     * change based on whether the item is disabled or not.
     */
    leadingIcon: @Composable ((iconColor: Color) -> Unit)? = null,
    /**
     * Lambda called when this item is clicked.
     *
     * Note: If you want the context menu to close when this item is clicked,
     * you will have to do it in this lambda via [ContextMenuState.close].
     */
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = ContextMenuSpec.LabelVerticalTextAlignment,
        horizontalArrangement = Arrangement.spacedBy(ContextMenuSpec.HorizontalPadding),
        modifier = modifier
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
                modifier = Modifier.requiredSizeIn(
                    minWidth = ContextMenuSpec.IconSize,
                    maxWidth = ContextMenuSpec.IconSize,
                    maxHeight = ContextMenuSpec.IconSize,
                )
            ) { icon(if (enabled) ContextMenuSpec.IconColor else ContextMenuSpec.DisabledColor) }
        }
        BasicText(
            text = label,
            style = TextStyle(
                color = if (enabled) ContextMenuSpec.TextColor else ContextMenuSpec.DisabledColor,
                textAlign = ContextMenuSpec.LabelHorizontalTextAlignment,
            ),
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = true)
        )
    }
}

/**
 * Scope used to add components to a context menu.
 */
// We cannot expose a @Composable in the context menu API because we don't want folks adding
// arbitrary composables into a context menu. Instead, we expose this API which then maps to
// context menu composables.
internal class ContextMenuScope internal constructor() {
    private val composables = mutableListOf<@Composable () -> Unit>()

    @Composable
    internal fun Content() {
        composables.fastForEach { composable -> composable() }
    }

    internal fun clear() {
        composables.clear()
    }

    /**
     * Adds an item to the context menu list.
     *
     * @param label string to display in the text of the item.
     * @param modifier [Modifier] to apply to the item.
     * @param enabled whether or not the item should be enabled.
     * This affects whether the item is clickable, has a hover indication, and the text styling.
     * @param leadingIcon Composable to put in the leading icon position.
     * The color is the color to draw with and will change based on if the item is enabled or not.
     * This will measured with a required width of `24.dp` and a required maxHeight of `24.dp`.
     * The result will be centered vertically in the row.
     * @param onClick Action to perform on the item being clicked.
     * Returns whether or not the context menu should be dismissed.
     */
    fun item(
        label: String,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        /**
         * Icon to place in front of the label. If null, the icon will not be rendered
         * and the text will instead be further towards the start. The `iconColor` will
         * change based on whether the item is disabled or not.
         */
        leadingIcon: @Composable ((iconColor: Color) -> Unit)? = null,
        /**
         * Lambda called when this item is clicked.
         *
         * Note: If you want the context menu to close when this item is clicked,
         * you will have to do it in this lambda via [ContextMenuState.close].
         */
        onClick: () -> Unit,
    ) {
        check(label.isNotBlank()) { "Label must not be blank" }
        composables += {
            ContextMenuItem(
                modifier = modifier,
                label = label,
                enabled = enabled,
                leadingIcon = leadingIcon,
                onClick = onClick
            )
        }
    }
}
