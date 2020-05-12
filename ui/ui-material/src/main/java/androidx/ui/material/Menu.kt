/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.material

import androidx.animation.FloatPropKey
import androidx.animation.LinearOutSlowInEasing
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.animation.Transition
import androidx.ui.core.DensityAmbient
import androidx.ui.core.DropdownPopup
import androidx.ui.core.Modifier
import androidx.ui.core.drawLayer
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.foundation.clickable
import androidx.ui.layout.Column
import androidx.ui.layout.ColumnScope
import androidx.ui.layout.IntrinsicSize
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSizeIn
import androidx.ui.layout.preferredWidth
import androidx.ui.material.ripple.ripple
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.dp

/**
 * A Material Design [dropdown menu](https://material.io/components/menus#dropdown-menu).
 *
 * The menu has a [toggle], which is the element generating the menu. For example, this can be
 * an icon which, when tapped, triggers the menu.
 * The content of the [DropdownMenu] can be [DropdownMenuItem]s, as well as custom content.
 * [DropdownMenuItem] can be used to achieve items as defined by the Material Design spec.
 * [onDismissRequest] will be called when the menu should close - for example when there is a
 * tap outside the menu, or when the back key is pressed.
 *
 * Example usage:
 * @sample androidx.ui.material.samples.MenuSample
 *
 * @param toggle The element generating the menu
 * @param expanded Whether the menu is currently open or dismissed
 * @param onDismissRequest Called when the menu should be dismiss
 * @param toggleModifier The modifier to be applied to the toggle
 * @param dropdownModifier Modifier to be applied to the menu content
 */
@Composable
fun DropdownMenu(
    toggle: @Composable () -> Unit,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    toggleModifier: Modifier = Modifier,
    dropdownModifier: Modifier = Modifier,
    dropdownContent: @Composable ColumnScope.() -> Unit
) {
    var visibleMenu by state { expanded }
    if (expanded) visibleMenu = true

    Box(toggleModifier) {
        toggle()

        if (visibleMenu) {
            DropdownPopup(
                isFocusable = true,
                onDismissRequest = onDismissRequest,
                offset = with(DensityAmbient.current) {
                    // Compensate for the padding added below.
                    // TODO(popam, b/156890315): add elevation to Popup
                    IntPxPosition(-MenuElevation.toIntPx(), -MenuElevation.toIntPx())
                }
            ) {
                Transition(
                    definition = DropdownMenuOpenCloseTransition,
                    initState = !expanded,
                    toState = expanded,
                    onStateChangeFinished = {
                        visibleMenu = it
                    }
                ) { state ->
                    val scale = state[Scale]
                    val alpha = state[Alpha]
                    Card(
                        modifier = Modifier
                            .drawLayer(scaleX = scale, scaleY = scale, alpha = alpha, clip = true)
                            // Padding to account for the elevation, otherwise it is clipped.
                            .padding(MenuElevation),
                        elevation = MenuElevation
                    ) {
                        Column(
                            dropdownModifier
                                .padding(vertical = DropdownMenuVerticalPadding)
                                .preferredWidth(IntrinsicSize.Max),
                            children = dropdownContent
                        )
                    }
                }
            }
        }
    }
}

/**
 * A dropdown menu item, as defined by the Material Design spec.
 *
 * Example usage:
 * @sample androidx.ui.material.samples.MenuSample
 *
 * @param onClick Called when the menu item was clicked
 * @param modifier The modifier to be applied to the menu item
 * @param enabled Controls the enabled state of the menu item - when `false`, the menu item
 * will not be clickable and [onClick] will not be invoked
 */
@Composable
fun DropdownMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    // TODO(popam, b/156911853): investigate replacing this Box with ListItem
    Box(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .ripple(enabled = enabled)
            .fillMaxWidth()
            // Preferred min and max width used during the intrinsic measurement.
            .preferredSizeIn(
                minWidth = DropdownMenuItemDefaultMinWidth,
                maxWidth = DropdownMenuItemDefaultMaxWidth,
                minHeight = DropdownMenuItemDefaultMinHeight
            )
            .padding(horizontal = DropdownMenuHorizontalPadding),
        gravity = ContentGravity.CenterStart
    ) {
        // TODO(popam, b/156912039): update emphasis if the menu item is disabled
        val typography = MaterialTheme.typography
        val emphasisLevels = EmphasisAmbient.current
        ProvideTextStyle(typography.subtitle1) {
            ProvideEmphasis(emphasisLevels.high, content)
        }
    }
}

internal val MenuElevation = 8.dp
internal val DropdownMenuHorizontalPadding = 16.dp
internal val DropdownMenuVerticalPadding = 8.dp
internal val DropdownMenuItemDefaultMinWidth = 112.dp
internal val DropdownMenuItemDefaultMaxWidth = 280.dp
internal val DropdownMenuItemDefaultMinHeight = 48.dp

private val Scale = FloatPropKey()
private val Alpha = FloatPropKey()
internal val InTransitionDuration = 120
internal val OutTransitionDuration = 75

private val DropdownMenuOpenCloseTransition = transitionDefinition {
    state(false) {
        // Menu is dismissed.
        this[Scale] = 0f
        this[Alpha] = 0f
    }
    state(true) {
        // Menu is expanded.
        this[Scale] = 1f
        this[Alpha] = 1f
    }
    transition(false, true) {
        // Dismissed to expanded.
        Scale using tween {
            duration = InTransitionDuration
            easing = LinearOutSlowInEasing
        }
        Alpha using tween {
            duration = 30
        }
    }
    transition(true, false) {
        // Expanded to dismissed.
        Scale using tween {
            duration = 1
            delay = OutTransitionDuration - 1
        }
        Alpha using tween {
            duration = OutTransitionDuration
        }
    }
}
