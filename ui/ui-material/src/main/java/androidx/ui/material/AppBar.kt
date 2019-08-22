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
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Density
import androidx.ui.core.Dp
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.PxSize
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.sp
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Outline
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Shape
import androidx.ui.engine.geometry.addOutline
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.SimpleImage
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.layout.Container
import androidx.ui.layout.FlexRow
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Row
import androidx.ui.layout.WidthSpacer
import androidx.ui.material.surface.Surface
import androidx.ui.graphics.Color
import androidx.ui.layout.Alignment
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.Wrap
import androidx.ui.material.BottomAppBar.FabConfiguration
import androidx.ui.material.BottomAppBar.FabPosition
import androidx.ui.material.ripple.Ripple
import androidx.ui.painting.Image
import androidx.ui.painting.Path
import androidx.ui.painting.PathOperation
import androidx.ui.semantics.Semantics
import androidx.ui.text.TextStyle

/**
 * A TopAppBar displays information and actions relating to the current screen and is placed at the
 * top of the screen.
 *
 * @sample androidx.ui.material.samples.SimpleTopAppBarNavIcon
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
// TODO: b/137311217 - type inference for nullable lambdas currently doesn't work
@Suppress("USELESS_CAST")
@Composable
fun <T> TopAppBar(
    title: @Composable() () -> Unit = {},
    color: Color = +themeColor { primary },
    navigationIcon: @Composable() (() -> Unit)? = null as @Composable() (() -> Unit)?,
    contextualActions: List<T>? = null,
    action: @Composable() (T) -> Unit = {}
    // TODO: support overflow menu here with the remainder of the list
) {
    BaseTopAppBar(
        color = color,
        startContent = navigationIcon,
        title = {
            // Text color comes from the underlying Surface
            CurrentTextStyleProvider(value = +themeTextStyle { h6 }) {
                title()
            }
        },
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
    startContent: @Composable() (() -> Unit)?,
    title: @Composable() () -> Unit,
    endContent: @Composable() () -> Unit
) {
    BaseAppBar(color, TopAppBarElevation, RectangleShape) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            // We only want to reserve space here if we have some start content
            if (startContent != null) {
                inflexible {
                    Container(width = AppBarTitleStartPadding, alignment = Alignment.CenterLeft) {
                        startContent()
                    }
                }
            }
            expanded(1f) {
                title()
            }
            inflexible {
                Wrap {
                    endContent()
                }
            }
        }
    }
}

object BottomAppBar {
    /**
     * Configuration for a [FloatingActionButton] in a [BottomAppBar]
     *
     * When [cutoutShape] is provided, a cutout / notch will be 'carved' into the BottomAppBar, with
     * some extra space on all sides.
     *
     * A typical cutout FAB may look like:
     * @sample androidx.ui.material.samples.SimpleBottomAppBarCutoutFab
     *
     * This also works with an extended FAB:
     * @sample androidx.ui.material.samples.SimpleBottomAppBarExtendedCutoutFab
     *
     * A more complex example with a fancy animating FAB that switches between cut corners and
     * rounded corners:
     * @sample androidx.ui.material.samples.SimpleBottomAppBarFancyAnimatingCutoutFab
     *
     * @param fabPosition the position of the [FloatingActionButton] attached to the BottomAppBar
     * @param cutoutShape the shape of the cutout that will be added to the BottomAppBar - this
     * should typically be the same shape used inside the [fab]. This shape will be drawn with an
     * offset around all sides. If `null` no cutout will be drawn, and the [fab] will be placed on
     * top of the BottomAppBar.
     * @param fab the [FloatingActionButton] that will be attached to the BottomAppBar
     */
    data class FabConfiguration(
        internal val fabPosition: FabPosition = FabPosition.Center,
        internal val cutoutShape: Shape? = null,
        internal val fab: @Composable() () -> Unit
    )

    /**
     * The possible positions for a [FloatingActionButton] attached to a [BottomAppBar].
     */
    enum class FabPosition {
        /**
         * Positioned in the center of the [BottomAppBar]
         */
        Center,
        /**
         * Positioned at the end of the [BottomAppBar]
         */
        End
    }
}

/**
 * A BottomAppBar displays actions relating to the current screen and is placed at the bottom of
 * the screen. It can also optionally display a [FloatingActionButton], which is either overlaid
 * on top of the BottomAppBar, or inset, carving a cutout in the BottomAppBar.
 *
 * The location of the actions displayed by the BottomAppBar depends on the position / existence
 * of a [FloatingActionButton], configured with [fabConfiguration]. When [fabConfiguration] is:
 *
 * - `null`: the [navigationIcon] is displayed at the start, and the [contextualActions] are
 * displayed at the end
 *
 * @sample androidx.ui.material.samples.SimpleBottomAppBarNoFab
 *
 * - [FabPosition.Center] aligned: the [navigationIcon] is displayed at the start, and the
 * [contextualActions] are displayed at the end
 *
 * @sample androidx.ui.material.samples.SimpleBottomAppBarCenterFab
 *
 * - [FabPosition.End] aligned: the [contextualActions] are displayed at the start, and no
 * navigation icon is supported - setting a navigation icon here will throw an exception.
 *
 * @sample androidx.ui.material.samples.SimpleBottomAppBarEndFab
 *
 * For examples using a cutout FAB, see [FabConfiguration], which controls the shape of the cutout.
 *
 * @param color An optional color for the BottomAppBar. By default [MaterialColors.primary]
 * will be used.
 * @param navigationIcon The navigation icon displayed in the BottomAppBar. Note that if
 * [fabConfiguration] is [FabPosition.End] aligned, this parameter must be null / not set.
 * @param fabConfiguration The [FabConfiguration] that controls how / where
 * the [FloatingActionButton] is placed inside the BottomAppBar.
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
    fabConfiguration: FabConfiguration? = null,
    contextualActions: List<T>? = null,
    action: @Composable() (T) -> Unit = {}
    // TODO: support overflow menu here with the remainder of the list
) {
    require(navigationIcon == null || fabConfiguration?.fabPosition != FabPosition.End) {
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

    if (fabConfiguration == null) {
        BaseBottomAppBar(
            color = color,
            startContent = navigationIconComposable,
            fabContainer = null as @Composable() (() -> Unit)?,
            endContent = actions(MaxIconsInBottomAppBarNoFab)
        )
        return
    }

    // TODO: this causes an unfortunate frame lag as we need to position the fab before we can
    // know where to draw the cutout - when we have a better way of doing this synchronously we
    // should fix this.
    val bottomAppBarCutoutShape = +state<BottomAppBarCutoutShape?> { null }

    val fab = if (fabConfiguration.cutoutShape == null) {
        fabConfiguration.fab
    } else {
        @Composable {
            OnChildPositioned(onPositioned = { coords ->
                val shape = BottomAppBarCutoutShape(
                    fabConfiguration.cutoutShape,
                    coords.position,
                    coords.size
                )
                if (bottomAppBarCutoutShape.value != shape) {
                    bottomAppBarCutoutShape.value = shape
                }
            }) { fabConfiguration.fab() }
        }
    }

    val shape = bottomAppBarCutoutShape.value ?: RectangleShape

    when (fabConfiguration.fabPosition) {
        FabPosition.End -> BaseBottomAppBar(
            color = color,
            startContent = actions(MaxIconsInBottomAppBarEndFab),
            fabContainer = { FabContainerLayout(Alignment.CenterRight, fab) },
            endContent = {},
            shape = shape
        )
        FabPosition.Center -> BaseBottomAppBar(
            color = color,
            startContent = navigationIconComposable,
            fabContainer = { FabContainerLayout(Alignment.Center, fab) },
            endContent = actions(MaxIconsInBottomAppBarCenterFab),
            shape = shape
        )
    }
}

/**
 * Helper layout that takes up the full width of the app bar, with height equal to the [fab] height.
 * This allows us to use [OnChildPositioned] to get the fab position relative to the app bar, so
 * we can position the cutout in the correct place.
 */
@Composable
private fun FabContainerLayout(alignment: Alignment, fab: @Composable() () -> Unit) {
    Layout(fab) { measurables, constraints ->
        check(measurables.size == 1) { "Only one child is supported in the FAB container." }
        val fabPlaceable = measurables.first().measure(constraints)
        val width = constraints.maxWidth
        val height = fabPlaceable.height

        // FAB should be offset from the start / end of the app bar
        val padding = AppBarPadding.toIntPx()

        val fabAlignmentSpace = IntPxSize(
            width = width - fabPlaceable.width - padding - padding,
            height = height - fabPlaceable.height
        )

        val fabPosition = alignment.align(fabAlignmentSpace)
        layout(width, height) {
            // Adjust for the padding we added
            val xPosition = fabPosition.x + padding
            fabPlaceable.place(xPosition, fabPosition.y)
        }
    }
}

// TODO: consider exposing this in the shape package, for a generic cutout shape - might be useful
// for custom components.
/**
 * A [Shape] that represents a bottom app bar with a cutout. The cutout drawn will be [cutoutShape]
 * increased in size by [BottomAppBarCutoutOffset] on all sides.
 */
private data class BottomAppBarCutoutShape(
    val cutoutShape: Shape,
    val fabPosition: PxPosition,
    val fabSize: PxSize
) : Shape {

    override fun createOutline(size: PxSize, density: Density): Outline {
        val boundingRectangle = Path().apply {
            addRect(Rect.fromLTRB(0f, 0f, size.width.value, size.height.value))
        }
        val path = Path().apply {
            addCutoutShape(density)
            // Subtract this path from the bounding rectangle
            op(boundingRectangle, this, PathOperation.difference)
        }
        return Outline.Generic(path)
    }

    /**
     * Adds the filled [cutoutShape] to the [Path]. The path can the be subtracted from the main
     * rectangle path used for the app bar, to create the resulting cutout shape.
     */
    private fun Path.addCutoutShape(density: Density) {
        // The gap on all sides between the FAB and the cutout
        val cutoutOffset = withDensity(density) { BottomAppBarCutoutOffset.toPx() }

        val cutoutSize = PxSize(
            width = fabSize.width + (cutoutOffset * 2),
            height = fabSize.height + (cutoutOffset * 2)
        )

        val cutoutStartX = fabPosition.x.value - cutoutOffset.value

        val cutoutRadius = cutoutSize.height.value / 2f
        // Shift the cutout up by half its height, so only the bottom half of the cutout is actually
        // cut into the app bar
        val cutoutStartY = -cutoutRadius

        addOutline(cutoutShape.createOutline(cutoutSize, density))
        shift(Offset(cutoutStartX, cutoutStartY))
    }
}

@Composable
private fun BaseBottomAppBar(
    color: Color = +themeColor { primary },
    startContent: @Composable() () -> Unit,
    fabContainer: @Composable() (() -> Unit)?,
    shape: Shape = RectangleShape,
    endContent: @Composable() () -> Unit
) {
    val appBar = @Composable {
        BaseBottomAppBarWithoutFab(color, shape, startContent, endContent)
    }

    if (fabContainer == null) {
        appBar()
    } else {
        BottomAppBarStack(appBar = appBar) {
            fabContainer()
        }
    }
}

@Composable
private fun BaseBottomAppBarWithoutFab(
    color: Color,
    shape: Shape,
    startContent: @Composable() () -> Unit,
    endContent: @Composable() () -> Unit
) {
    BaseAppBar(color, BottomAppBarElevation, shape) {
        FlexRow(mainAxisAlignment = MainAxisAlignment.SpaceBetween) {
            inflexible {
                // Using a wrap so that even if startContent() is empty, we will still force
                // end content to be placed at the end of the row.
                Wrap {
                    startContent()
                }
            }
            inflexible {
                Wrap {
                    endContent()
                }
            }
        }
    }
}

/**
 * Simple `Stack` implementation that places [fab] on top (z-axis) of [appBar], with the midpoint
 * of the [fab] aligned to the top edge of the [appBar].
 *
 * This is needed as we want the total height of the BottomAppBar to be equal to the height of
 * [appBar] + half the height of [fab], which is only possible with a custom layout.
 */
@Composable
private fun BottomAppBarStack(appBar: @Composable() () -> Unit, fab: @Composable() () -> Unit) {
    Layout(appBar, fab) { measurables, constraints ->
        val (appBarPlaceable, fabPlaceable) = measurables.map { it.measure(constraints) }

        val layoutWidth = appBarPlaceable.width
        // Total height is the app bar height + half the fab height
        val layoutHeight = appBarPlaceable.height + (fabPlaceable.height / 2)

        val appBarVerticalOffset = layoutHeight - appBarPlaceable.height

        // Position the children.
        layout(layoutWidth, layoutHeight) {
            // Place app bar in the bottom left
            appBarPlaceable.place(IntPx.Zero, appBarVerticalOffset)

            // Place fab in the top left
            fabPlaceable.place(IntPx.Zero, IntPx.Zero)
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
private fun BaseAppBar(
    color: Color,
    elevation: Dp,
    shape: Shape,
    children: @Composable() () -> Unit
) {
    Semantics(
        container = true
    ) {
        Surface(color = color, elevation = elevation, shape = shape) {
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
    Container(width = ActionIconDiameter, height = ActionIconDiameter) {
        Ripple(bounded = false) {
            Clickable(onClick = onClick) {
                SimpleImage(icon)
            }
        }
    }
}

private val ActionIconDiameter = 24.dp

private val AppBarHeight = 56.dp
private val AppBarPadding = 16.dp
private val AppBarTitleStartPadding = 72.dp - AppBarPadding

// TODO: should this have elevation? Spec says 8.dp but since shadows aren't shown on the top it
//  isn't really visible
private val BottomAppBarElevation = 0.dp
private val TopAppBarElevation = 4.dp

// The gap on all sides between the FAB and the cutout
private val BottomAppBarCutoutOffset = 8.dp

private const val MaxIconsInTopAppBar = 2
private const val MaxIconsInBottomAppBarCenterFab = 2
private const val MaxIconsInBottomAppBarEndFab = 4
private const val MaxIconsInBottomAppBarNoFab = 4
