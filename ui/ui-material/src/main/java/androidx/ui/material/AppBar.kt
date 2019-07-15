/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Semantics
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.sp
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.SimpleImage
import androidx.ui.layout.Container
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Row
import androidx.ui.layout.WidthSpacer
import androidx.ui.material.surface.Surface
import androidx.ui.graphics.Color
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.Center
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Stack
import androidx.ui.material.BottomAppBar.FabPosition
import androidx.ui.material.BottomAppBar.FabPosition.Center
import androidx.ui.material.BottomAppBar.FabPosition.End
import androidx.ui.material.ripple.Ripple
import androidx.ui.painting.Image
import androidx.ui.text.TextStyle

/**
 * A TopAppBar displays information and actions relating to the current screen and is placed at the
 * top of the screen.
 *
 * @sample androidx.ui.material.samples.SimpleTopAppBar
 *
 * @param title The title to be displayed in the center of the TopAppBar
 * @param color An optional color for the TopAppBar. By default [MaterialColors.primary] will be
 * used.
 * @param navigationIcon The navigation icon displayed at the start of the TopAppBar
 * @param contextualActions A list representing the contextual actions to be displayed at the end of
 * the TopAppBar. Any remaining actions that do not fit on the TopAppBar should typically be
 * displayed in an overflow menu at the end.
 * @param action A specific item action to be displayed at the end of the TopAppBar - this will be
 * called for items in [contextualActions] up to the maximum number of icons that can be displayed.
 * @param T the type of item in [contextualActions]
 */
@Composable
fun <T> TopAppBar(
    title: @Composable() () -> Unit = {},
    color: Color = +themeColor { primary },
    navigationIcon: @Composable() () -> Unit = {},
    contextualActions: List<T>? = null,
    action: @Composable() (T) -> Unit = {}
    // TODO: support overflow menu here with the remainder of the list
) {
    BaseTopAppBar(
        color = color,
        startContent = navigationIcon,
        title = title,
        endContent = {
            if (contextualActions != null) {
                AppBarActions(MaxIconsInTopAppBar, contextualActions, action)
            }
        }
    )
}

@Composable
private fun BaseTopAppBar(
    color: Color = +themeColor { primary },
    startContent: @Composable() () -> Unit,
    title: @Composable() () -> Unit,
    endContent: @Composable() () -> Unit
) {
    BaseAppBar(color) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            inflexible {
                // TODO: what should the spacing be when there is no icon provided here?
                startContent()
                WidthSpacer(width = 32.dp)
            }
            expanded(1f) {
                CurrentTextStyleProvider(value = +themeTextStyle { h6 }) {
                    title()
                }
            }
            inflexible {
                endContent()
            }
        }
    }
}

object BottomAppBar {
    /**
     * The possible positions for the [FloatingActionButton] embedded in the [BottomAppBar], if a
     * [FloatingActionButton] is specified.
     */
    enum class FabPosition {
        /**
         * Positioned in the center of the [BottomAppBar], overlapping the content of the
         * BottomAppBar
         */
        Center,
        /**
         * Positioned at the end of the [BottomAppBar], overlapping the content of the
         * BottomAppBar
         */
        End,
        /**
         * Positioned in the center of the [BottomAppBar], with an inset cutting into the content of
         * the BottomAppBar
         */
        CenterCut,
    }
}

/**
 * A BottomAppBar displays actions relating to the current screen and is placed at the bottom of
 * the screen. It can also optionally display a [FloatingActionButton], which is either overlaid
 * on top of the BottomAppBar, or inset, carving a notch in the BottomAppBar.
 *
 * The location of the actions displayed by the BottomAppBar depends on the [FabPosition] /
 * existence of the [FloatingActionButton]. When the [FloatingActionButton] is:
 *
 * - not set: the [navigationIcon] is displayed at the start, and the [contextualActions] are
 * displayed at the end
 *
 * @sample androidx.ui.material.samples.SimpleBottomAppBarNoFab
 *
 * - [Center] or [CenterCut] aligned: the [navigationIcon] is displayed at the start, and the
 * [contextualActions] are displayed at the end
 *
 * @sample androidx.ui.material.samples.SimpleBottomAppBarCenterFab
 *
 * - [End] aligned: the [contextualActions] are displayed at the start, and no navigation icon is
 * supported
 *
 * @sample androidx.ui.material.samples.SimpleBottomAppBarEndFab
 *
 * @param color An optional color for the BottomAppBar. By default [MaterialColors.primary]
 * will be used.
 * @param navigationIcon The navigation icon displayed in the BottomAppBar. Note that if
 * [fabPosition] is set to [End], this parameter must be null / not set.
 * @param floatingActionButton The [FloatingActionButton] displayed in the BottomAppBar. The
 * position of this fab will be [Center] aligned by default. You can set [fabPosition] to change
 * the position.
 * @param fabPosition The [FabPosition] of the [floatingActionButton]. This can be [Center],
 * [CenterCut], or [End].
 * @param contextualActions A list representing the contextual actions to be displayed in the
 * BottomAppBar. Any remaining actions that do not fit on the BottomAppBar should typically be
 * displayed in an overflow menu.
 * @param action A specific item action to be displayed in the BottomAppBar - this will be called
 * for items in [contextualActions] up to the maximum number of icons that can be displayed.
 * @param T the type of item in [contextualActions]
 */
// TODO: b/137311217 - type inference for nullable lambdas currently doesn't work
@Suppress("USELESS_CAST")
@Composable
fun <T> BottomAppBar(
    color: Color = +themeColor { primary },
    navigationIcon: (@Composable() () -> Unit)? = null as @Composable() (() -> Unit)?,
    floatingActionButton: (@Composable() () -> Unit)? = null as @Composable() (() -> Unit)?,
    fabPosition: FabPosition = Center,
    contextualActions: List<T>? = null,
    action: @Composable() (T) -> Unit = {}
    // TODO: support overflow menu here with the remainder of the list
) {
    require(navigationIcon == null || fabPosition != End) {
        "Using a navigation icon with an end-aligned FloatingActionButton is not supported"
    }

    val actions = { maxIcons: Int ->
        @Composable {
            if (contextualActions != null) {
                AppBarActions(maxIcons, contextualActions, action)
            }
        }
    }

    val navigationIconComposable = @Composable {
        if (navigationIcon != null) {
            navigationIcon()
        }
    }

    if (floatingActionButton == null) {
        BaseBottomAppBar(
            color = color,
            startContent = navigationIconComposable,
            endContent = actions(MaxIconsInBottomAppBarNoFab)
        )
        return
    }

    when (fabPosition) {
        End -> BaseBottomAppBar(
            color = color,
            startContent = actions(MaxIconsInBottomAppBarEndFab),
            fab = { Align(Alignment.CenterRight) { floatingActionButton() } }
        )
        // TODO: support CenterCut
        else -> BaseBottomAppBar(
            color = color,
            startContent = navigationIconComposable,
            fab = { Center { floatingActionButton() } },
            endContent = actions(MaxIconsInBottomAppBarCenterFab)
        )
    }
}

// TODO: b/137311217 - type inference for nullable lambdas currently doesn't work
@Suppress("USELESS_CAST")
@Composable
private fun BaseBottomAppBar(
    color: Color = +themeColor { primary },
    startContent: @Composable() () -> Unit = {},
    fab: (@Composable() () -> Unit)? = null as @Composable() (() -> Unit)?,
    endContent: @Composable() () -> Unit = {}
) {
    val appBar = @Composable { BaseBottomAppBarWithoutFab(color, startContent, endContent) }
    if (fab == null) {
        appBar()
    } else {
        ConstrainedBox(
            constraints = DpConstraints(
                minHeight = BottomAppBarHeightWithFab,
                maxHeight = BottomAppBarHeightWithFab
            )
        ) {
            Stack {
                aligned(Alignment.BottomCenter) {
                    appBar()
                }
                aligned(Alignment.TopCenter) {
                    Container(
                        height = AppBarHeight,
                        padding = EdgeInsets(left = AppBarPadding, right = AppBarPadding)
                    ) {
                        fab()
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseBottomAppBarWithoutFab(
    color: Color,
    startContent: @Composable() () -> Unit,
    endContent: @Composable() () -> Unit
) {
    BaseAppBar(color) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            inflexible {
                startContent()
                // TODO: if startContent() doesn't have any layout, then the endContent won't be
                // placed at the end, so we need to trick it with a spacer
                WidthSpacer(width = 1.dp)
            }
            inflexible { endContent() }
        }
    }
}

/**
 * An empty App Bar that expands to the parent's width.
 *
 * For an App Bar that follows Material spec guidelines to be placed on the top of the screen, see
 * [TopAppBar].
 */
@Composable
private fun BaseAppBar(color: Color, children: @Composable() () -> Unit) {
    Semantics(
        container = true
    ) {
        Surface(color = color) {
            Container(height = AppBarHeight, expanded = true, padding = EdgeInsets(AppBarPadding)) {
                children()
            }
        }
    }
}

@Composable
private fun <T> AppBarActions(
    actionsToDisplay: Int,
    contextualActions: List<T>,
    action: @Composable() (T) -> Unit
) {
    if (contextualActions.isEmpty()) {
        return
    }

    // Split the list depending on how many actions we are displaying - if actionsToDisplay is
    // greater than or equal to the number of actions provided, overflowActions will be empty.
    val (shownActions, overflowActions) = contextualActions.withIndex().partition {
        it.index < actionsToDisplay
    }

    Row {
        shownActions.forEach { (index, shownAction) ->
            action(shownAction)
            if (index != shownActions.lastIndex) {
                WidthSpacer(width = 24.dp)
            }
        }
        if (overflowActions.isNotEmpty()) {
            WidthSpacer(width = 24.dp)
            // TODO: use overflowActions to build menu here
            Container(width = 12.dp) {
                Text(text = "${overflowActions.size}", style = TextStyle(fontSize = 15.sp))
            }
        }
    }
}

/**
 * A correctly sized clickable icon that can be used inside [TopAppBar] and [BottomAppBar] for
 * either the navigation icon or the actions.
 *
 * @param icon The icon to be displayed
 * @param onClick the lambda to be invoked when this icon is pressed
 */
@Composable
fun AppBarIcon(icon: Image, onClick: () -> Unit) {
    Ripple(bounded = false) {
        Clickable(onClick = onClick) {
            Center {
                Container(width = ActionIconDiameter, height = ActionIconDiameter) {
                    SimpleImage(icon)
                }
            }
        }
    }
}

private val ActionIconDiameter = 24.dp

private val AppBarHeight = 56.dp
private val BottomAppBarHeightWithFab = 84.dp
private val AppBarPadding = 16.dp

private const val MaxIconsInTopAppBar = 2
private const val MaxIconsInBottomAppBarCenterFab = 2
private const val MaxIconsInBottomAppBarEndFab = 4
private const val MaxIconsInBottomAppBarNoFab = 4