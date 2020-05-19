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
import androidx.compose.Providers
import androidx.compose.Stable
import androidx.compose.StructurallyEqual
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.staticAmbientOf
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.boundsInParent
import androidx.ui.core.onPositioned
import androidx.ui.core.zIndex
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shape
import androidx.ui.layout.Column
import androidx.ui.layout.InnerPadding
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.material.Scaffold.FabPosition
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.height
import androidx.ui.unit.toSize

/**
 * State for [Scaffold] composable component.
 *
 * Contains basic screen state, e.g. Drawer configuration, as well as sizes of components after
 * layout has happened
 *
 * @param drawerState initial state of the Drawer in [Scaffold].
 * @param isDrawerGesturesEnabled whether or not drawer can be interacted with via gestures
 */
@Stable
class ScaffoldState(
    drawerState: DrawerState = DrawerState.Closed,
    isDrawerGesturesEnabled: Boolean = true
) {

    /**
     * drawer state position. Change this value to programmatically open or close drawer sheet
     * in Scaffold (if set).
     */
    var drawerState by mutableStateOf(drawerState)

    /**
     * Whether or not drawer sheet in scaffold (if set) can be interacted by gestures.
     */
    var isDrawerGesturesEnabled by mutableStateOf(isDrawerGesturesEnabled)
    // TODO: add showSnackbar() method here

    /**
     * Get current size of the topBar in [Scaffold], if known. `null` if this unknown or topBar
     * parameter in scaffold is not set
     */
    val topBarSize: PxSize?
        get() = scaffoldGeometry.topBarBounds?.toSize()

    /**
     * Get current size of the bottomBar in [Scaffold], if known. `null` if this unknown or
     * bottomBar parameter in scaffold is not set
     */
    val bottomBarSize: PxSize?
        get() = scaffoldGeometry.bottomBarBounds?.toSize()

    /**
     * Get current size of the floatingActionButton in [Scaffold], if known. `null` if this unknown
     * or floatingActionButton parameter in scaffold is not set
     */
    val floatingActionButtonSize: PxSize?
        get() = scaffoldGeometry.fabBounds?.toSize()

    internal val scaffoldGeometry = ScaffoldGeometry()
}

@Stable
internal class ScaffoldGeometry {
    var topBarBounds by mutableStateOf<PxBounds?>(null, StructurallyEqual)
    var bottomBarBounds by mutableStateOf<PxBounds?>(null, StructurallyEqual)
    var fabBounds by mutableStateOf<PxBounds?>(null, StructurallyEqual)

    var isFabDocked by mutableStateOf(false)
}

internal val ScaffoldGeometryAmbient = staticAmbientOf { ScaffoldGeometry() }

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
        End
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
 * variables to provide manual control over the drawer behavior, sizes of components, etc
 * @param topBar top app bar of the screen. Consider using [TopAppBar].
 * @param bottomBar bottom bar of the screen. Consider using [BottomAppBar].
 * @param floatingActionButton Main action button of your screen. Consider using
 * [FloatingActionButton] for this slot.
 * @param floatingActionButtonPosition position of the FAB on the screen. See [FabPosition] for
 * possible options available.
 * @param isFloatingActionButtonDocked whether [floatingActionButton] should overlap with
 * [bottomBar] for half a height, if [bottomBar] exists. Ignored if there's no [bottomBar] or no
 * [floatingActionButton].
 * @param drawerContent content of the Drawer sheet that can be pulled from the left side (right
 * for RTL).
 * @param drawerShape shape of the drawer sheet (if set)
 * @param drawerElevation drawer sheet elevation. This controls the size of the shadow
 * below the drawer sheet (if set)
 * @param bodyContent content of your screen. The lambda receives an [InnerPadding] that should be
 * applied to the content root via [Modifier.padding] to properly offset top and bottom bars. If
 * you're using VerticalScroller, apply this modifier to the child of the scroller, and not on
 * the scroller itself.
 */
@Composable
fun Scaffold(
    scaffoldState: ScaffoldState = remember { ScaffoldState() },
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    floatingActionButton: @Composable (() -> Unit)? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isFloatingActionButtonDocked: Boolean = false,
    drawerContent: @Composable (() -> Unit)? = null,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerConstants.DefaultElevation,
    backgroundColor: Color = MaterialTheme.colors.background,
    bodyContent: @Composable (InnerPadding) -> Unit
) {
    scaffoldState.scaffoldGeometry.isFabDocked = isFloatingActionButtonDocked
    val child = @Composable {
        Surface(color = backgroundColor) {
            Column(Modifier.fillMaxSize()) {
                if (topBar != null) {
                    TopBarContainer(Modifier.zIndex(TopAppBarZIndex), scaffoldState, topBar)
                }
                Stack(Modifier.weight(1f, fill = true)) {
                    ScaffoldContent(Modifier.fillMaxSize(), scaffoldState, bodyContent)
                    ScaffoldBottom(
                        Modifier.gravity(Alignment.BottomCenter),
                        scaffoldState = scaffoldState,
                        fabPos = floatingActionButtonPosition,
                        isFabDocked = isFloatingActionButtonDocked,
                        fab = floatingActionButton,
                        bottomBar = bottomBar
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
            drawerContent = { ScaffoldSlot(content = drawerContent) },
            drawerShape = drawerShape,
            drawerElevation = drawerElevation,
            bodyContent = child
        )
    } else {
        child()
    }
}

private fun FabPosition.toColumnAlign() =
    if (this == FabPosition.End) Alignment.End else Alignment.CenterHorizontally

/**
 * Scaffold part that is on the bottom. Includes FAB and BottomBar
 */
@Composable
private fun ScaffoldBottom(
    modifier: Modifier,
    scaffoldState: ScaffoldState,
    fabPos: FabPosition,
    isFabDocked: Boolean,
    fab: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null
) {
    if (isFabDocked && bottomBar != null && fab != null) {
        DockedBottomBar(
            modifier = modifier,
            fabPosition = fabPos,
            fab = { FabContainer(Modifier, scaffoldState, fab) },
            bottomBar = { BottomBarContainer(scaffoldState, bottomBar) }
        )
    } else {
        Column(modifier.fillMaxWidth()) {
            if (fab != null) {
                FabContainer(
                    Modifier.gravity(fabPos.toColumnAlign())
                        .padding(start = FabSpacing, end = FabSpacing, bottom = FabSpacing),
                    scaffoldState,
                    fab
                )
            }
            if (bottomBar != null) {
                BottomBarContainer(scaffoldState, bottomBar)
            }
        }
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
    fab: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        children = {
            bottomBar()
            fab()
        }) { measurables, constraints, _ ->
        val (appBarPlaceable, fabPlaceable) = measurables.map { it.measure(constraints) }

        val layoutWidth = appBarPlaceable.width
        // Total height is the app bar height + half the fab height
        val layoutHeight = appBarPlaceable.height + (fabPlaceable.height / 2)

        val appBarVerticalOffset = layoutHeight - appBarPlaceable.height
        val fabPosX = if (fabPosition == FabPosition.End) {
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
    content: @Composable (InnerPadding) -> Unit
) {
    ScaffoldSlot(modifier) {
        val innerPadding = with(DensityAmbient.current) {
            val bottom = scaffoldState.scaffoldGeometry.bottomBarBounds?.height?.toDp() ?: 0.dp
            InnerPadding(bottom = bottom)
        }
        content(innerPadding)
    }
}

@Composable
private fun BottomBarContainer(
    scaffoldState: ScaffoldState,
    bottomBar: @Composable () -> Unit
) {
    BoundsAwareScaffoldSlot(
        Modifier,
        { scaffoldState.scaffoldGeometry.bottomBarBounds = it },
        slotContent = {
            Providers(ScaffoldGeometryAmbient provides scaffoldState.scaffoldGeometry) {
                bottomBar()
            }
        }
    )
}

@Composable
private fun FabContainer(
    modifier: Modifier,
    scaffoldState: ScaffoldState,
    fab: @Composable () -> Unit
) {
    BoundsAwareScaffoldSlot(modifier, { scaffoldState.scaffoldGeometry.fabBounds = it }, fab)
}

@Composable
private fun TopBarContainer(
    modifier: Modifier,
    scaffoldState: ScaffoldState,
    topBar: @Composable () -> Unit
) {
    BoundsAwareScaffoldSlot(modifier, { scaffoldState.scaffoldGeometry.topBarBounds = it }, topBar)
}

@Composable
private fun BoundsAwareScaffoldSlot(
    modifier: Modifier,
    onBoundsKnown: (PxBounds?) -> Unit,
    slotContent: @Composable () -> Unit
) {
    onDispose {
        onBoundsKnown(null)
    }
    ScaffoldSlot(
        modifier = modifier.onPositioned { coords -> onBoundsKnown(coords.boundsInParent) },
        content = slotContent
    )
}

/**
 * Default slot implementation for Scaffold slots content
 */
@Composable
private fun ScaffoldSlot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Stack(modifier) { content() }
}

private val FabSpacing = 16.dp
private val DockedFabEndSpacing = 16.dp
private const val TopAppBarZIndex = 1f