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
import androidx.compose.Model
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Layout
import androidx.ui.core.LayoutTagParentData
import androidx.ui.core.Modifier
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.ParentData
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Stack
import androidx.ui.material.BottomAppBar.FabConfiguration
import androidx.ui.material.BottomAppBar.FabDockedPosition
import androidx.ui.material.Scaffold.FabPosition
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

/**
 * State for [Scaffold] composable component.
 *
 * Contains basic screen state, e.g. Drawer configuration.
 *
 * @param drawerState the state of the Drawer in [Scaffold]. Change it to open/close Drawer
 * programmatically.
 * @param isDrawerGesturesEnabled whether or not drawer can be interacted with via gestures
 */
@Model
class ScaffoldState(
    var drawerState: DrawerState = DrawerState.Closed,
    var isDrawerGesturesEnabled: Boolean = true
) {

    // TODO: add showSnackbar() method here

    internal var fabConfiguration: FabConfiguration? = null
    internal var bottomBarSize: IntPxSize? = null
}

object Scaffold {
    /**
     * The possible positions for a [FloatingActionButton] attached to a [Scaffold].
     */
    enum class FabPosition {
        /**
         * Position FAB at the bottom of the screen in the center, above the [BottomAppBar] (if it
         * exists)
         */
        Center,
        /**
         * Position FAB at the bottom of the screen at the end, above the [BottomAppBar] (if it
         * exists)
         */
        End,
        /**
         * Position FAB on the center of the screen, overlap with [BottomAppBar] (which should
         * exist if this position is used, otherwise an exception will be thrown)
         */
        CenterDocked,
        /**
         * Position FAB on the end of the screen, overlap with [BottomAppBar] (which should
         * exist if this position is used, otherwise an exception will be thrown)
         */
        EndDocked
    }
}

/**
 * Scaffold implements the basic material design visual layout structure.
 *
 * This component provides API to put together several material components to construct your
 * screen, by ensuring proper layout strategy for them and collecting necessary data so these
 * components will work together correctly.
 *
 * Simple example of a Scaffold with [TopAppBar], [FloatingActionButton] and drawer:
 *
 * @sample androidx.ui.material.samples.SimpleScaffoldWithTopBar
 *
 * More fancy usage with [BottomAppBar] with cutout and docked [FloatingActionButton], which
 * animates it's shape when clicked:
 *
 * @sample androidx.ui.material.samples.ScaffoldWithBottomBarAndCutout
 *
 * @param scaffoldState state of this scaffold widget. It contains the state of the screen, e.g.
 * variables to provide manual control over the drawer behavior
 * @param topAppBar top app bar of the screen. Consider using [TopAppBar].
 * @param bottomAppBar bottom bar of the screen. Consider using [BottomAppBar]. The slot
 * Parameter [FabConfiguration] is necessary to be passed as a parameter to [BottomAppBar] in
 * order to ensure proper [FloatingActionButton] + [BottomAppBar] behavior.
 * @param floatingActionButton Main action button of your screen. Consider using
 * [FloatingActionButton] for this slot.
 * @param floatingActionButtonPosition position of the FAB on the screen. See [FabPosition] for
 * possible options available.
 * @param drawerContent content of the Drawer sheet that can be pulled from the left side (right
 * for RTL).
 * @param bodyContent content of your screen. The lambda receives a Modifier that should be
 * applied to the content root to get the desired behavior. If you're using VerticalScroller,
 * apply this modifier to the child of the scroller, and not on the scroller itself.
 */
@Composable
fun Scaffold(
    scaffoldState: ScaffoldState = remember { ScaffoldState() },
    topAppBar: @Composable() (() -> Unit)? = null,
    bottomAppBar: @Composable() ((FabConfiguration?) -> Unit)? = null,
    floatingActionButton: @Composable() (() -> Unit)? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    drawerContent: @Composable() (() -> Unit)? = null,
    bodyContent: @Composable() (Modifier) -> Unit
) {
    val child = @Composable {
        Surface(color = MaterialTheme.colors().background) {
            Column(LayoutSize.Fill) {
                if (topAppBar != null) ScaffoldSlot(children = topAppBar)
                Stack(modifier = LayoutFlexible(1f, tight = true)) {
                    ScaffoldContent(LayoutSize.Fill, scaffoldState, bodyContent)
                    ScaffoldBottom(
                        modifier = LayoutGravity.BottomCenter,
                        scaffoldState = scaffoldState,
                        fabPos = floatingActionButtonPosition,
                        fab = floatingActionButton,
                        bottomBar = bottomAppBar
                    )
                }
            }
        }
    }

    if (drawerContent != null) {
        ModalDrawerLayout(
            drawerState = scaffoldState.drawerState,
            onStateChange = { scaffoldState.drawerState = it },
            gesturesEnabled = scaffoldState.isDrawerGesturesEnabled,
            drawerContent = { ScaffoldSlot(children = drawerContent) },
            bodyContent = child
        )
    } else {
        child()
    }
}

/**
 * Scaffold part that is on the bottom. Includes FAB and BottomBar
 */
@Composable
private fun ScaffoldBottom(
    modifier: Modifier,
    scaffoldState: ScaffoldState,
    fabPos: FabPosition,
    fab: @Composable() (() -> Unit)? = null,
    bottomBar: @Composable() ((FabConfiguration?) -> Unit)? = null
) {
    if (fabPos != FabPosition.CenterDocked && fabPos != FabPosition.EndDocked) {
        Column(modifier = modifier + LayoutWidth.Fill) {
            if (fab != null) {
                val fabSpacing =
                    LayoutPadding(bottom = FabSpacing, start = FabSpacing, end = FabSpacing)
                val gravity =
                    if (fabPos == FabPosition.End) LayoutGravity.End else LayoutGravity.Center
                FabContainer(fabPos, gravity + fabSpacing, scaffoldState, fab)
            }
            if (bottomBar != null) {
                BottomBarContainer(scaffoldState, bottomBar)
            }
        }
    } else if (bottomBar != null && fab != null) {
        DockedBottomBar(
            modifier = modifier,
            fabPosition = fabPos,
            fab = { FabContainer(fabPos, Modifier.None, scaffoldState, fab) },
            bottomBar = { BottomBarContainer(scaffoldState, bottomBar) }
        )
    } else {
        throw IllegalArgumentException(
            "To use ${FabPosition.CenterDocked} or ${FabPosition.EndDocked} " +
                    "you need both bottomBar and FAB"
        )
    }
}

/**
 * Simple `Stack` implementation that places [fab] on top (z-axis) of [bottomBar], with the midpoint
 * of the [fab] aligned to the top edge of the [bottomBar].
 *
 * This is needed as we want the total height of the BottomAppBar to be equal to the height of
 * [bottomBar] + half the height of [fab], which is only possible with a custom layout.
 */
@Composable
private fun DockedBottomBar(
    modifier: Modifier,
    fabPosition: FabPosition,
    fab: @Composable() () -> Unit,
    bottomBar: @Composable() () -> Unit
) {
    Layout(
        modifier = modifier,
        children = {
            ParentData(
                object : LayoutTagParentData {
                    override val tag: Any = "bottomBar"
                },
                bottomBar
            )
            ParentData(
                object : LayoutTagParentData {
                    override val tag: Any = "fab"
                },
                fab
            )
        }) { measurables, constraints, _ ->
        val (appBarPlaceable, fabPlaceable) = measurables.map { it.measure(constraints) }

        val layoutWidth = appBarPlaceable.width
        // Total height is the app bar height + half the fab height
        val layoutHeight = appBarPlaceable.height + (fabPlaceable.height / 2)

        val appBarVerticalOffset = layoutHeight - appBarPlaceable.height
        val fabPosX = if (fabPosition == FabPosition.EndDocked) {
            layoutWidth - fabPlaceable.width - DockedFabEndSpacing.toIntPx()
        } else {
            (layoutWidth - fabPlaceable.width) / 2
        }

        layout(layoutWidth, layoutHeight) {
            appBarPlaceable.place(IntPx.Zero, appBarVerticalOffset)
            fabPlaceable.place(fabPosX, IntPx.Zero)
        }
    }
}

@Composable
private fun ScaffoldContent(
    modifier: Modifier,
    scaffoldState: ScaffoldState,
    content: @Composable() (Modifier) -> Unit
) {
    ScaffoldSlot(modifier) {
        val bottomSpace = with(DensityAmbient.current) {
            scaffoldState.bottomBarSize?.height?.toDp() ?: 0.dp
        }
        content(LayoutPadding(bottom = bottomSpace))
    }
}

@Composable
private fun BottomBarContainer(
    scaffoldState: ScaffoldState,
    bottomBar: @Composable() ((FabConfiguration?) -> Unit)
) {
    onDispose(callback = { scaffoldState.bottomBarSize = null })
    OnChildPositioned(
        onPositioned = {
            if (scaffoldState.bottomBarSize != it.size) scaffoldState.bottomBarSize = it.size
        },
        children = {
            ScaffoldSlot(children = {
                bottomBar(scaffoldState.fabConfiguration)
            })
        }
    )
}

@Composable
private fun FabContainer(
    fabPos: FabPosition,
    modifier: Modifier,
    scaffoldState: ScaffoldState,
    fab: @Composable() () -> Unit
) {
    onDispose(callback = { scaffoldState.fabConfiguration = null })
    OnChildPositioned(onPositioned = { coords ->
        // TODO(mount): This should probably use bounding box rather than position/size
        val position = coords.parentCoordinates?.childToLocal(coords, PxPosition.Origin)
            ?: PxPosition.Origin
        val config =
            when (fabPos) {
                FabPosition.CenterDocked -> {
                    FabConfiguration(coords.size, position, FabDockedPosition.Center)
                }
                FabPosition.EndDocked -> {
                    FabConfiguration(coords.size, position, FabDockedPosition.End)
                }
                else -> {
                    null
                }
            }
        if (scaffoldState.fabConfiguration != config) scaffoldState.fabConfiguration = config
    }) {
        ScaffoldSlot(modifier = modifier, children = fab)
    }
}

/**
 * Default slot implementation for Scaffold slots children
 */
@Composable
private fun ScaffoldSlot(modifier: Modifier = Modifier.None, children: @Composable() () -> Unit) {
    Stack(modifier) { children() }
}

private val FabSpacing = 16.dp
private val DockedFabEndSpacing = 16.dp