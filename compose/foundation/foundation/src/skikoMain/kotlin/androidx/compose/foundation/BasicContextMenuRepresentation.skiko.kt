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

package androidx.compose.foundation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuComponent
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItemWithComposableLeadingIcon
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSeparator
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

// Design of basic representation is from Material specs:
// https://material.io/design/interaction/states.html#hover
// https://material.io/components/menus#specs

@Composable
internal fun DefaultOpenContextMenu(
    session: TextContextMenuSession,
    components: List<TextContextMenuComponent>,
    popupPositionProvider: PopupPositionProvider,
    colors: ContextMenuColors = DefaultContextMenuColors,
) {
    var focusManager: FocusManager? by mutableStateOf(null)
    var inputModeManager: InputModeManager? by mutableStateOf(null)

    Popup(
        properties = PopupProperties(focusable = true),
        onDismissRequest = session::close,
        popupPositionProvider = popupPositionProvider,
        onKeyEvent = {
            if (it.type == KeyEventType.KeyDown) {
                when (it.key) {
                    Key.DirectionDown  -> {
                        inputModeManager!!.requestInputMode(InputMode.Keyboard)
                        focusManager!!.moveFocus(FocusDirection.Next)
                        true
                    }
                    Key.DirectionUp -> {
                        inputModeManager!!.requestInputMode(InputMode.Keyboard)
                        focusManager!!.moveFocus(FocusDirection.Previous)
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        },
    ) {
        Column(
            modifier = Modifier
                .shadow(8.dp)
                .background(colors.backgroundColor)
                .padding(vertical = 4.dp)
                .width(IntrinsicSize.Max)
                .verticalScroll(rememberScrollState())
        ) {
            components.fastForEach { component ->
                when (component) {
                    is TextContextMenuSeparator ->
                        MenuSeparator(colors.textColor)
                    is TextContextMenuItemWithComposableLeadingIcon ->
                        MenuItem(session, colors, component)
                }
            }
        }
    }
}

@Composable
private fun MenuSeparator(color: Color) {
    Box(
        modifier =
            Modifier.padding(vertical = 8.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(color)
    )
}

@Composable
private fun MenuItem(
    session: TextContextMenuSession,
    colors: ContextMenuColors,
    component: TextContextMenuItemWithComposableLeadingIcon
) {
    MenuItemContent(
        colors = colors,
        onClick = { component.onClick(session) },
        enabled = component.enabled
    ) {
        component.leadingIcon?.let { icon ->
            icon(colors.resolveIconColor(component.enabled))
        }
        BasicText(
            text = component.label,
            style = TextStyle(colors.resolveTextColor(component.enabled))
        )
    }
}

@Composable
private fun MenuItemContent(
    colors: ContextMenuColors,
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                enabled = enabled,
            )
            .semantics(mergeDescendants = true) {}
            .then(
                if (enabled) {
                    val hovered = interactionSource.collectIsHoveredAsState().value
                    Modifier
                        .background(if (hovered) colors.hoverColor else Color.Transparent)
                } else {
                    Modifier
                }
            )
            .fillMaxWidth()
            // Preferred min and max width used during the intrinsic measurement.
            .sizeIn(
                minWidth = 112.dp,
                maxWidth = 280.dp,
                minHeight = 32.dp
            )
            .padding(
                PaddingValues(
                    horizontal = 16.dp,
                    vertical = 0.dp
                )
            ),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

private const val DisabledAlpha = 0.38f

internal val DefaultContextMenuColors =
    ContextMenuColors(
        backgroundColor = Color.White,
        textColor = Color.Black,
        iconColor = Color.Black,
        disabledTextColor = Color.Black.copy(alpha = DisabledAlpha),
        disabledIconColor = Color.Black.copy(alpha = DisabledAlpha),
        hoverColor = Color.Black.copy(alpha = 0.04f),
    )


/**
 * Colors to apply to the context menu.
 *
 * @param backgroundColor Color of the background in the context menu
 * @param textColor Color of the text in context menu items
 * @param iconColor Color of the icon in context menu items
 * @param disabledTextColor Color of disabled text in context menu items
 * @param disabledIconColor Color of disabled icon in context menu items
 */
@Stable
internal class ContextMenuColors(
    val backgroundColor: Color,
    val textColor: Color,
    val iconColor: Color,
    val disabledTextColor: Color,
    val disabledIconColor: Color,
    val hoverColor: Color,
) {

    /**
     * Returns the text color to use in the given enabled state.
     */
    fun resolveTextColor(enabled: Boolean): Color =
        if (enabled) textColor else disabledTextColor

    /**
     * Returns the icon color to use in the given enabled state.
     */
    fun resolveIconColor(enabled: Boolean): Color =
        if (enabled) iconColor else disabledIconColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ContextMenuColors) return false

        if (this.backgroundColor != other.backgroundColor) return false
        if (this.textColor != other.textColor) return false
        if (this.iconColor != other.iconColor) return false
        if (this.disabledTextColor != other.disabledTextColor) return false
        if (this.disabledIconColor != other.disabledIconColor) return false
        if (this.hoverColor != other.hoverColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + textColor.hashCode()
        result = 31 * result + iconColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        result = 31 * result + disabledIconColor.hashCode()
        result = 31 * result + hoverColor.hashCode()
        return result
    }

    override fun toString(): String =
        "ContextMenuColors(" +
            "backgroundColor=$backgroundColor, " +
            "textColor=$textColor, " +
            "iconColor=$iconColor, " +
            "disabledTextColor=$disabledTextColor, " +
            "disabledIconColor=$disabledIconColor, " +
            "hoverColor=$hoverColor" +
            ")"
}
