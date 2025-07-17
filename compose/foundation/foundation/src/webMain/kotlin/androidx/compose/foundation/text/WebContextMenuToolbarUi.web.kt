/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.contextmenu.ContextMenuColors
import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuState
import androidx.compose.foundation.contextmenu.DefaultContextMenuColors
import androidx.compose.foundation.contextmenu.close
import androidx.compose.foundation.contextmenu.computeContextMenuColors
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

internal object WebContextMenuToolbarSpec {
    val ContainerHeight = 56.dp
    val MenuContainerElevation = 6.dp
    val CornerShape = RoundedCornerShape(50)
    val LabelVerticalTextAlignment = Alignment.CenterVertically
    val LabelHorizontalTextAlignment = TextAlign.Center
    val ItemsPadding = 8.dp
    val ContainerPadding = 8.dp
    val IconSize = 24.dp

    // text
    val FontSize = 16.sp
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

private val DefaultPopupToolbarProperties = PopupProperties(focusable = false)

@Composable
internal fun WebContextMenuToolbarPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
) {
    WebContextMenuToolbarPopup(
        popupPositionProvider = popupPositionProvider,
        onDismiss = onDismiss,
        modifier = modifier,
        colors = computeContextMenuColors(),
        contextMenuBuilderBlock = contextMenuBuilderBlock
    )
}

@Composable
internal fun WebContextMenuToolbarPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ContextMenuColors,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
) {
    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismiss,
        properties = DefaultPopupToolbarProperties,
    ) {
        WebContextMenuRowBuilder(modifier, colors, contextMenuBuilderBlock)
    }
}

@Composable
internal fun WebContextMenuRowBuilder(
    modifier: Modifier = Modifier,
    colors: ContextMenuColors = DefaultContextMenuColors,
    contextMenuBuilderBlock: ContextMenuScope.() -> Unit,
) {
    WebContextMenuRow(colors, modifier) {
        val scope = remember {
            ContextMenuScope { modifier, label, enabled, colors, leadingIcon, onClick ->
                WebContextMenuToolbarItem(label, enabled, colors, modifier, leadingIcon, onClick)
            }
        }
        with(scope) {
            clear()
            contextMenuBuilderBlock()
            Content(colors)
        }
    }
}

@Composable
internal fun WebContextMenuRow(
    colors: ContextMenuColors,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .padding(horizontal = WebContextMenuToolbarSpec.ContainerPadding)
                .shadow(
                    WebContextMenuToolbarSpec.MenuContainerElevation,
                    WebContextMenuToolbarSpec.CornerShape
                )
                .background(colors.backgroundColor)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = WebContextMenuToolbarSpec.ContainerPadding),
        content = content,
    )
}

@Composable
internal fun WebContextMenuToolbarItem(
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
        verticalAlignment = WebContextMenuToolbarSpec.LabelVerticalTextAlignment,
        horizontalArrangement = Arrangement.spacedBy(WebContextMenuToolbarSpec.ItemsPadding),
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
                .sizeIn(
                    minHeight = WebContextMenuToolbarSpec.ContainerHeight,
                    maxHeight = WebContextMenuToolbarSpec.ContainerHeight,
                )
                .padding(horizontal = WebContextMenuToolbarSpec.ItemsPadding)
    ) {
        leadingIcon?.let { icon ->
            Box(
                modifier =
                    Modifier.requiredSizeIn(
                        minWidth = WebContextMenuToolbarSpec.IconSize,
                        maxWidth = WebContextMenuToolbarSpec.IconSize,
                        maxHeight = WebContextMenuToolbarSpec.IconSize,
                    )
            ) {
                icon(if (enabled) colors.iconColor else colors.disabledIconColor)
            }
        }
        BasicText(
            text = label,
            style =
                WebContextMenuToolbarSpec.textStyle(
                    color = if (enabled) colors.textColor else colors.disabledTextColor,
                ),
            maxLines = 1
        )
    }
}
