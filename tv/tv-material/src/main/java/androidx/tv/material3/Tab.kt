/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tv.material3

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/**
 * Material Design tab.
 *
 * A default Tab, also known as a Primary Navigation Tab. Tabs organize content across different
 * screens, data sets, and other interactions.
 *
 * This should typically be used inside of a [TabRow], see the corresponding documentation for
 * example usage.
 *
 * @param selected whether this tab is selected or not
 * @param onFocus called when this tab is focused
 * @param modifier the [Modifier] to be applied to this tab
 * @param onClick called when this tab is clicked (with D-Pad Center)
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param colors these will be used by the tab when in different states (focused,
 * selected, etc.)
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this tab. You can create and pass in your own `remember`ed instance to observe [Interaction]s
 * and customize the appearance / behavior of this tab in different states.
 * @param content content of the [Tab]
 */
@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
@Composable
fun Tab(
    selected: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
    enabled: Boolean = true,
    colors: TabColors = TabDefaults.pillIndicatorTabColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isTabRowActive = LocalTabRowHasFocus.current
    Surface(
        checked = selected,
        onCheckedChange = { onClick() },
        modifier = modifier
            .onFocusChanged {
                if (it.isFocused) {
                    onFocus()
                }
            }
            .semantics {
                this.selected = selected
                this.role = Role.Tab
            },
        colors = colors.toToggleableSurfaceColors(
            isTabRowActive = isTabRowActive,
            enabled = enabled,
        ),
        enabled = enabled,
        scale = ToggleableSurfaceScale.None,
        shape = ToggleableSurfaceDefaults.shape(shape = RectangleShape),
        interactionSource = interactionSource,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Represents the colors used in a tab in different states.
 *
 * - See [TabDefaults.pillIndicatorTabColors] for the default colors used in a [Tab] when using a
 * Pill indicator.
 * - See [TabDefaults.underlinedIndicatorTabColors] for the default colors used in a [Tab] when
 * using an Underlined indicator
 */
@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
class TabColors
internal constructor(
    internal val activeContentColor: Color,
    internal val contentColor: Color = activeContentColor.copy(alpha = 0.4f),
    internal val selectedContentColor: Color,
    internal val focusedContentColor: Color,
    internal val focusedSelectedContentColor: Color,
    internal val disabledActiveContentColor: Color,
    internal val disabledContentColor: Color = disabledActiveContentColor.copy(alpha = 0.4f),
    internal val disabledSelectedContentColor: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TabColors) return false

        if (activeContentColor != other.activeContentColor) return false
        if (contentColor != other.contentColor) return false
        if (selectedContentColor != other.selectedContentColor) return false
        if (focusedContentColor != other.focusedContentColor) return false
        if (focusedSelectedContentColor != other.focusedSelectedContentColor) return false
        if (disabledActiveContentColor != other.disabledActiveContentColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledSelectedContentColor != other.disabledSelectedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activeContentColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + focusedContentColor.hashCode()
        result = 31 * result + focusedSelectedContentColor.hashCode()
        result = 31 * result + disabledActiveContentColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledSelectedContentColor.hashCode()
        return result
    }
}

@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
object TabDefaults {
    /**
     * [Tab]'s content colors to in conjunction with underlined indicator
     *
     * @param activeContentColor applied when the any of the other tabs is focused
     * @param contentColor the default color of the tab's content when none of the tabs are focused
     * @param selectedContentColor applied when the current tab is selected
     * @param focusedContentColor applied when the current tab is focused
     * @param focusedSelectedContentColor applied when the current tab is both focused and selected
     * @param disabledActiveContentColor applied when any of the other tabs is focused and the
     * current tab is disabled
     * @param disabledContentColor applied when the current tab is disabled and none of the tabs are
     * focused
     * @param disabledSelectedContentColor applied when the current tab is disabled and selected
     */
    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun underlinedIndicatorTabColors(
        activeContentColor: Color = LocalContentColor.current,
        contentColor: Color = activeContentColor.copy(alpha = 0.4f),
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        focusedContentColor: Color = MaterialTheme.colorScheme.primary,
        focusedSelectedContentColor: Color = focusedContentColor,
        disabledActiveContentColor: Color = activeContentColor,
        disabledContentColor: Color = disabledActiveContentColor.copy(alpha = 0.4f),
        disabledSelectedContentColor: Color = selectedContentColor,
    ): TabColors =
        TabColors(
            activeContentColor = activeContentColor,
            contentColor = contentColor,
            selectedContentColor = selectedContentColor,
            focusedContentColor = focusedContentColor,
            focusedSelectedContentColor = focusedSelectedContentColor,
            disabledActiveContentColor = disabledActiveContentColor,
            disabledContentColor = disabledContentColor,
            disabledSelectedContentColor = disabledSelectedContentColor,
        )

    /**
     * [Tab]'s content colors to in conjunction with pill indicator
     *
     * @param activeContentColor applied when the any of the other tabs is focused
     * @param contentColor the default color of the tab's content when none of the tabs are focused
     * @param selectedContentColor applied when the current tab is selected
     * @param focusedContentColor applied when the current tab is focused
     * @param focusedSelectedContentColor applied when the current tab is both focused and selected
     * @param disabledActiveContentColor applied when any of the other tabs is focused and the
     * current tab is disabled
     * @param disabledContentColor applied when the current tab is disabled and none of the tabs are
     * focused
     * @param disabledSelectedContentColor applied when the current tab is disabled and selected
     */
    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable
    fun pillIndicatorTabColors(
        activeContentColor: Color = LocalContentColor.current,
        contentColor: Color = activeContentColor.copy(alpha = 0.4f),
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        focusedContentColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        focusedSelectedContentColor: Color = focusedContentColor,
        disabledActiveContentColor: Color = activeContentColor,
        disabledContentColor: Color = disabledActiveContentColor.copy(alpha = 0.4f),
        disabledSelectedContentColor: Color = selectedContentColor,
    ): TabColors =
        TabColors(
            activeContentColor = activeContentColor,
            contentColor = contentColor,
            selectedContentColor = selectedContentColor,
            focusedContentColor = focusedContentColor,
            focusedSelectedContentColor = focusedSelectedContentColor,
            disabledActiveContentColor = disabledActiveContentColor,
            disabledContentColor = disabledContentColor,
            disabledSelectedContentColor = disabledSelectedContentColor,
        )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun TabColors.toToggleableSurfaceColors(
    isTabRowActive: Boolean,
    enabled: Boolean,
) =
    ToggleableSurfaceDefaults.colors(
        contentColor = if (isTabRowActive) activeContentColor else contentColor,
        selectedContentColor = if (enabled) selectedContentColor else disabledSelectedContentColor,
        focusedContentColor = focusedContentColor,
        focusedSelectedContentColor = focusedSelectedContentColor,
        disabledContentColor =
        if (isTabRowActive) disabledActiveContentColor else disabledContentColor,
        containerColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        pressedContainerColor = Color.Transparent,
        focusedSelectedContainerColor = Color.Transparent,
        selectedContainerColor = Color.Transparent,
        pressedSelectedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
    )
