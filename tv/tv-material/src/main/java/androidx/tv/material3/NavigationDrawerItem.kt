/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * TV Material Design navigation drawer item.
 *
 * A [NavigationDrawerItem] represents a destination within drawers, either [NavigationDrawer] or
 * [ModalNavigationDrawer]
 *
 * @sample androidx.tv.samples.SampleNavigationDrawer
 * @sample androidx.tv.samples.SampleModalNavigationDrawerWithSolidScrim
 * @sample androidx.tv.samples.SampleModalNavigationDrawerWithGradientScrim
 *
 * @param selected defines whether this composable is selected or not
 * @param onClick called when this composable is clicked
 * @param leadingContent the leading content of the list item
 * @param modifier to be applied to the list item
 * @param enabled controls the enabled state of this composable. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services
 * @param onLongClick called when this composable is long clicked (long-pressed)
 * @param supportingContent the content displayed below the headline content
 * @param trailingContent the trailing meta badge or icon
 * @param tonalElevation the tonal elevation of this composable
 * @param shape defines the shape of Composable's container in different interaction states
 * @param colors defines the background and content colors used in the composable
 * for different interaction states
 * @param scale defines the size of the composable relative to its original size in
 * different interaction states
 * @param border defines a border around the composable in different interaction states
 * @param glow defines a shadow to be shown behind the composable for different interaction states
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this component. You can create and pass in your own [remember]ed instance
 * to observe [Interaction]s and customize the appearance / behavior of this composable in different
 * states
 * @param content main content of this composable
 */
@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
@Composable
fun NavigationDrawerScope.NavigationDrawerItem(
    selected: Boolean,
    onClick: () -> Unit,
    leadingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tonalElevation: Dp = NavigationDrawerItemDefaults.NavigationDrawerItemElevation,
    shape: NavigationDrawerItemShape = NavigationDrawerItemDefaults.shape(),
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    scale: NavigationDrawerItemScale = NavigationDrawerItemScale.None,
    border: NavigationDrawerItemBorder = NavigationDrawerItemDefaults.border(),
    glow: NavigationDrawerItemGlow = NavigationDrawerItemDefaults.glow(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val animatedWidth by animateDpAsState(
        targetValue = if (hasFocus) {
            NavigationDrawerItemDefaults.ExpandedDrawerItemWidth
        } else {
            NavigationDrawerItemDefaults.CollapsedDrawerItemWidth
        },
        label = "NavigationDrawerItem width open/closed state of the drawer item"
    )
    val navDrawerItemHeight = if (supportingContent == null) {
        NavigationDrawerItemDefaults.ContainerHeightOneLine
    } else {
        NavigationDrawerItemDefaults.ContainerHeightTwoLine
    }
    ListItem(
        selected = selected,
        onClick = onClick,
        headlineContent = {
            AnimatedVisibility(
                visible = hasFocus,
                enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                exit = NavigationDrawerItemDefaults.ContentAnimationExit,
            ) {
                content()
            }
        },
        leadingContent = {
            Box(Modifier.size(NavigationDrawerItemDefaults.IconSize)) {
                leadingContent()
            }
        },
        trailingContent = trailingContent?.let {
            {
                AnimatedVisibility(
                    visible = hasFocus,
                    enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                    exit = NavigationDrawerItemDefaults.ContentAnimationExit,
                ) {
                    it()
                }
            }
        },
        supportingContent = supportingContent?.let {
            {
                AnimatedVisibility(
                    visible = hasFocus,
                    enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                    exit = NavigationDrawerItemDefaults.ContentAnimationExit,
                ) {
                    it()
                }
            }
        },
        modifier = modifier
            .layout { measurable, constraints ->
                val width = animatedWidth.roundToPx()
                val height = navDrawerItemHeight.roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = width,
                        maxWidth = width,
                        minHeight = height,
                        maxHeight = height,
                    )
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            },
        enabled = enabled,
        onLongClick = onLongClick,
        tonalElevation = tonalElevation,
        shape = shape.toToggleableListItemShape(),
        colors = colors.toToggleableListItemColors(hasFocus),
        scale = scale.toToggleableListItemScale(),
        border = border.toToggleableListItemBorder(),
        glow = glow.toToggleableListItemGlow(),
        interactionSource = interactionSource,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerItemShape.toToggleableListItemShape() =
    ListItemDefaults.shape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        selectedShape = selectedShape,
        disabledShape = disabledShape,
        focusedSelectedShape = focusedSelectedShape,
        focusedDisabledShape = focusedDisabledShape,
        pressedSelectedShape = pressedSelectedShape,
    )

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerItemColors.toToggleableListItemColors(
    doesNavigationDrawerHaveFocus: Boolean
) =
    ListItemDefaults.colors(
        containerColor = containerColor,
        contentColor = if (doesNavigationDrawerHaveFocus) contentColor else inactiveContentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor =
        if (doesNavigationDrawerHaveFocus) disabledContentColor else disabledInactiveContentColor,
        focusedSelectedContainerColor = focusedSelectedContainerColor,
        focusedSelectedContentColor = focusedSelectedContentColor,
        pressedSelectedContainerColor = pressedSelectedContainerColor,
        pressedSelectedContentColor = pressedSelectedContentColor,
    )

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerItemScale.toToggleableListItemScale() =
    ListItemDefaults.scale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        selectedScale = selectedScale,
        disabledScale = disabledScale,
        focusedSelectedScale = focusedSelectedScale,
        focusedDisabledScale = focusedDisabledScale,
        pressedSelectedScale = pressedSelectedScale,
    )

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerItemBorder.toToggleableListItemBorder() =
    ListItemDefaults.border(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        selectedBorder = selectedBorder,
        disabledBorder = disabledBorder,
        focusedSelectedBorder = focusedSelectedBorder,
        focusedDisabledBorder = focusedDisabledBorder,
        pressedSelectedBorder = pressedSelectedBorder,
    )

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationDrawerItemGlow.toToggleableListItemGlow() =
    ListItemDefaults.glow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow,
        selectedGlow = selectedGlow,
        focusedSelectedGlow = focusedSelectedGlow,
        pressedSelectedGlow = pressedSelectedGlow,
    )
