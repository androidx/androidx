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

package androidx.compose.material3

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.internal.DraggableAnchors
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.anchoredDraggable
import androidx.compose.material3.internal.draggableAnchors
import androidx.compose.material3.internal.getString
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * [Material Design standard bottom sheet
 * scaffold](https://m3.material.io/components/bottom-sheets/overview)
 *
 * Standard bottom sheets co-exist with the screen’s main UI region and allow for simultaneously
 * viewing and interacting with both regions. They are commonly used to keep a feature or secondary
 * content visible on screen when content in main UI region is frequently scrolled or panned.
 *
 * ![Bottom sheet
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * This component provides API to put together several material components to construct your screen,
 * by ensuring proper layout strategy for them and collecting necessary data so these components
 * will work together correctly.
 *
 * A simple example of a standard bottom sheet looks like this:
 *
 * @sample androidx.compose.material3.samples.SimpleBottomSheetScaffoldSample
 * @param sheetContent the content of the bottom sheet
 * @param modifier the [Modifier] to be applied to the root of the scaffold
 * @param scaffoldState the state of the bottom sheet scaffold
 * @param sheetPeekHeight the height of the bottom sheet when it is collapsed
 * @param sheetMaxWidth [Dp] that defines what the maximum width the sheet will take. Pass in
 *   [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param sheetShape the shape of the bottom sheet
 * @param sheetContainerColor the background color of the bottom sheet
 * @param sheetContentColor the preferred content color provided by the bottom sheet to its
 *   children. Defaults to the matching content color for [sheetContainerColor], or if that is not a
 *   color from the theme, this will keep the same content color set above the bottom sheet.
 * @param sheetTonalElevation when [sheetContainerColor] is [ColorScheme.surface], a translucent
 *   primary color overlay is applied on top of the container. A higher tonal elevation value will
 *   result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param sheetShadowElevation the shadow elevation of the bottom sheet
 * @param sheetDragHandle optional visual marker to pull the scaffold's bottom sheet
 * @param sheetSwipeEnabled whether the sheet swiping is enabled and should react to the user's
 *   input
 * @param topBar top app bar of the screen, typically a [TopAppBar]
 * @param snackbarHost component to host [Snackbar]s that are pushed to be shown via
 *   [SnackbarHostState.showSnackbar], typically a [SnackbarHost]
 * @param containerColor the color used for the background of this scaffold. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this scaffold. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param content content of the screen. The lambda receives a [PaddingValues] that should be
 *   applied to the content root via [Modifier.padding] and [Modifier.consumeWindowInsets] to
 *   properly offset top and bottom bars. If using [Modifier.verticalScroll], apply this modifier to
 *   the child of the scroll, and not on the scroll itself.
 */
@Composable
@ExperimentalMaterial3Api
fun BottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    sheetPeekHeight: Dp = BottomSheetDefaults.SheetPeekHeight,
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetShape: Shape = BottomSheetDefaults.ExpandedShape,
    sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
    sheetContentColor: Color = contentColorFor(sheetContainerColor),
    sheetTonalElevation: Dp = 0.dp,
    sheetShadowElevation: Dp = BottomSheetDefaults.Elevation,
    sheetDragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    sheetSwipeEnabled: Boolean = true,
    topBar: @Composable (() -> Unit)? = null,
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SnackbarHost(it) },
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier.fillMaxSize().background(containerColor)) {
        // Using composition local provider instead of Surface as Surface implements .clip() which
        // intercepts touch events in testing.
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            BottomSheetScaffoldLayout(
                topBar = topBar,
                body = { content(PaddingValues(bottom = sheetPeekHeight)) },
                snackbarHost = { snackbarHost(scaffoldState.snackbarHostState) },
                sheetOffset = { scaffoldState.bottomSheetState.requireOffset() },
                sheetState = scaffoldState.bottomSheetState,
                bottomSheet = {
                    StandardBottomSheet(
                        state = scaffoldState.bottomSheetState,
                        peekHeight = sheetPeekHeight,
                        sheetMaxWidth = sheetMaxWidth,
                        sheetSwipeEnabled = sheetSwipeEnabled,
                        shape = sheetShape,
                        containerColor = sheetContainerColor,
                        contentColor = sheetContentColor,
                        tonalElevation = sheetTonalElevation,
                        shadowElevation = sheetShadowElevation,
                        dragHandle = sheetDragHandle,
                        content = sheetContent,
                    )
                },
            )
        }
    }
}

/**
 * State of the [BottomSheetScaffold] composable.
 *
 * @param bottomSheetState the state of the persistent bottom sheet
 * @param snackbarHostState the [SnackbarHostState] used to show snackbars inside the scaffold
 */
@ExperimentalMaterial3Api
@Stable
class BottomSheetScaffoldState(
    val bottomSheetState: SheetState,
    val snackbarHostState: SnackbarHostState,
)

/**
 * Create and [remember] a [BottomSheetScaffoldState].
 *
 * @param bottomSheetState the state of the standard bottom sheet. See
 *   [rememberStandardBottomSheetState]
 * @param snackbarHostState the [SnackbarHostState] used to show snackbars inside the scaffold
 */
@Composable
@ExperimentalMaterial3Api
fun rememberBottomSheetScaffoldState(
    bottomSheetState: SheetState = rememberStandardBottomSheetState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
): BottomSheetScaffoldState {
    return remember(bottomSheetState, snackbarHostState) {
        BottomSheetScaffoldState(
            bottomSheetState = bottomSheetState,
            snackbarHostState = snackbarHostState,
        )
    }
}

/**
 * Create and [remember] a [SheetState] for [BottomSheetScaffold].
 *
 * @param initialValue the initial value of the state. Should be either [PartiallyExpanded] or
 *   [Expanded] if [skipHiddenState] is true
 * @param confirmValueChange optional callback invoked to confirm or veto a pending state change
 * @param [skipHiddenState] whether Hidden state is skipped for [BottomSheetScaffold]
 */
@Composable
@ExperimentalMaterial3Api
fun rememberStandardBottomSheetState(
    initialValue: SheetValue = PartiallyExpanded,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    skipHiddenState: Boolean = true,
) =
    rememberSheetState(
        confirmValueChange = confirmValueChange,
        initialValue = initialValue,
        skipHiddenState = skipHiddenState,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardBottomSheet(
    state: SheetState,
    peekHeight: Dp,
    sheetMaxWidth: Dp,
    sheetSwipeEnabled: Boolean,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    dragHandle: @Composable (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    // TODO Load the motionScheme tokens from the component tokens file
    val anchoredDraggableMotion: FiniteAnimationSpec<Float> =
        MotionSchemeKeyTokens.DefaultSpatial.value()
    val showMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.DefaultSpatial.value()
    val hideMotion: FiniteAnimationSpec<Float> = MotionSchemeKeyTokens.FastEffects.value()

    SideEffect {
        state.showMotionSpec = showMotion
        state.hideMotionSpec = hideMotion
        state.anchoredDraggableMotionSpec = anchoredDraggableMotion
    }

    val scope = rememberCoroutineScope()
    val orientation = Orientation.Vertical
    val peekHeightPx = with(LocalDensity.current) { peekHeight.toPx() }
    val nestedScroll =
        if (sheetSwipeEnabled) {
            Modifier.nestedScroll(
                remember(state.anchoredDraggableState) {
                    ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                        sheetState = state,
                        orientation = orientation,
                        onFling = { scope.launch { state.settle(it) } },
                    )
                }
            )
        } else {
            Modifier
        }
    Surface(
        modifier =
            Modifier.widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
                .requiredHeightIn(min = peekHeight)
                .then(nestedScroll)
                .draggableAnchors(state.anchoredDraggableState, orientation) {
                    sheetSize,
                    constraints ->
                    val layoutHeight = constraints.maxHeight.toFloat()
                    val sheetHeight = sheetSize.height.toFloat()
                    val newAnchors = DraggableAnchors {
                        if (!state.skipPartiallyExpanded) {
                            PartiallyExpanded at (layoutHeight - peekHeightPx)
                        }
                        // Ensure when there is no content, there is just one anchor set to
                        // layoutHeight. Hidden overrides skipHiddenState in this use case.
                        if (sheetHeight == 0f || !state.skipHiddenState) {
                            Hidden at layoutHeight
                        }
                        if (sheetHeight > 0f) {
                            Expanded at layoutHeight - sheetHeight
                        }
                    }
                    val newTarget =
                        when (val oldTarget = state.anchoredDraggableState.targetValue) {
                            Hidden -> if (newAnchors.hasAnchorFor(Hidden)) Hidden else oldTarget
                            PartiallyExpanded ->
                                when {
                                    newAnchors.hasAnchorFor(PartiallyExpanded) -> PartiallyExpanded
                                    newAnchors.hasAnchorFor(Expanded) -> Expanded
                                    newAnchors.hasAnchorFor(Hidden) -> Hidden
                                    else -> oldTarget
                                }

                            Expanded -> if (newAnchors.hasAnchorFor(Expanded)) Expanded else Hidden
                        }
                    return@draggableAnchors newAnchors to newTarget
                }
                .anchoredDraggable(
                    state = state.anchoredDraggableState,
                    orientation = orientation,
                    enabled = sheetSwipeEnabled,
                )
                // Scale up the Surface vertically in case the sheet's offset overflows below the
                // min anchor. This is done to avoid showing a gap when the sheet opens and bounces
                // when it's applied with a bouncy motion. Note that the content inside the Surface
                // is scaled back down to maintain its aspect ratio (see below).
                .verticalScaleUp(state),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Column(
            Modifier.fillMaxWidth()
                // Scale the content down in case the sheet offset overflows below the min anchor.
                // The wrapping Surface is scaled up, so this is done to maintain the content's
                // aspect ratio.
                .verticalScaleDown(state)
        ) {
            if (dragHandle != null) {
                val partialExpandActionLabel =
                    getString(Strings.BottomSheetPartialExpandDescription)
                val dismissActionLabel = getString(Strings.BottomSheetDismissDescription)
                val expandActionLabel = getString(Strings.BottomSheetExpandDescription)
                DragHandleWithTooltip {
                    Box(
                        modifier =
                            Modifier.clickable {
                                    when (state.currentValue) {
                                        Expanded ->
                                            scope.launch {
                                                if (!state.skipHiddenState) {
                                                    state.hide()
                                                } else {
                                                    state.partialExpand()
                                                }
                                            }

                                        PartiallyExpanded -> scope.launch { state.expand() }
                                        else -> scope.launch { state.show() }
                                    }
                                }
                                .semantics(mergeDescendants = true) {
                                    with(state) {
                                        // Provides semantics to interact with the bottomsheet if
                                        // there is more than one anchor to swipe to and swiping is
                                        // enabled.
                                        if (
                                            anchoredDraggableState.anchors.size > 1 &&
                                                sheetSwipeEnabled
                                        ) {
                                            if (currentValue == PartiallyExpanded) {
                                                if (
                                                    anchoredDraggableState.confirmValueChange(
                                                        Expanded
                                                    )
                                                ) {
                                                    expand(expandActionLabel) {
                                                        scope.launch { expand() }
                                                        true
                                                    }
                                                }
                                            } else {
                                                if (
                                                    anchoredDraggableState.confirmValueChange(
                                                        PartiallyExpanded
                                                    )
                                                ) {
                                                    collapse(partialExpandActionLabel) {
                                                        scope.launch { partialExpand() }
                                                        true
                                                    }
                                                }
                                            }
                                            if (!state.skipHiddenState) {
                                                dismiss(dismissActionLabel) {
                                                    scope.launch { hide() }
                                                    true
                                                }
                                            }
                                        }
                                    }
                                }
                    ) {
                        dragHandle()
                    }
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetScaffoldLayout(
    topBar: @Composable (() -> Unit)?,
    body: @Composable () -> Unit,
    bottomSheet: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    sheetOffset: () -> Float,
    sheetState: SheetState,
) {
    Layout(
        contents = listOf<@Composable () -> Unit>(topBar ?: {}, body, bottomSheet, snackbarHost)
    ) {
        (topBarMeasurables, bodyMeasurables, bottomSheetMeasurables, snackbarHostMeasurables),
        constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val sheetPlaceables = bottomSheetMeasurables.fastMap { it.measure(looseConstraints) }

        val topBarPlaceables = topBarMeasurables.fastMap { it.measure(looseConstraints) }
        val topBarHeight = topBarPlaceables.fastMaxOfOrNull { it.height } ?: 0

        val bodyConstraints = looseConstraints.copy(maxHeight = layoutHeight - topBarHeight)
        val bodyPlaceables = bodyMeasurables.fastMap { it.measure(bodyConstraints) }

        val snackbarPlaceables = snackbarHostMeasurables.fastMap { it.measure(looseConstraints) }

        layout(layoutWidth, layoutHeight) {
            val sheetWidth = sheetPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val sheetOffsetX = max(0, (layoutWidth - sheetWidth) / 2)

            val snackbarWidth = snackbarPlaceables.fastMaxOfOrNull { it.width } ?: 0
            val snackbarHeight = snackbarPlaceables.fastMaxOfOrNull { it.height } ?: 0
            val snackbarOffsetX = (layoutWidth - snackbarWidth) / 2
            val snackbarOffsetY =
                when (sheetState.currentValue) {
                    PartiallyExpanded -> sheetOffset().roundToInt() - snackbarHeight
                    Expanded,
                    Hidden -> layoutHeight - snackbarHeight
                }

            // Placement order is important for elevation
            bodyPlaceables.fastForEach { it.placeRelative(0, topBarHeight) }
            topBarPlaceables.fastForEach { it.placeRelative(0, 0) }
            sheetPlaceables.fastForEach { it.placeRelative(sheetOffsetX, 0) }
            snackbarPlaceables.fastForEach { it.placeRelative(snackbarOffsetX, snackbarOffsetY) }
        }
    }
}

/**
 * A [Modifier] that scales up the drawing layer on the Y axis in case the [SheetState]'s
 * anchoredDraggableState offset overflows below the min anchor coordinates. The scaling will ensure
 * that there is no visible gap between the sheet and the edge of the screen in case the sheet
 * bounces when it opens due to a more expressive motion setting.
 *
 * A [verticalScaleDown] should be applied to the content of the sheet to maintain the content
 * aspect ratio as the container scales up.
 *
 * @param state a [SheetState]
 * @see verticalScaleDown
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.verticalScaleUp(state: SheetState) = graphicsLayer {
    val offset = state.anchoredDraggableState.offset
    val anchor = state.anchoredDraggableState.anchors.minAnchor()
    val overflow = if (offset < anchor) anchor - offset else 0f
    scaleY = if (overflow > 0f) (size.height + overflow) / size.height else 1f
    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
}

/**
 * A [Modifier] that scales down the drawing layer on the Y axis in case the [SheetState]'s
 * anchoredDraggableState offset overflows below the min anchor coordinates. This modifier should be
 * applied to the content inside a component that was scaled up with a [verticalScaleUp] modifier.
 * It will ensure that the content maintains its aspect ratio as the container scales up.
 *
 * @param state a [SheetState]
 * @see verticalScaleUp
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun Modifier.verticalScaleDown(state: SheetState) = graphicsLayer {
    val offset = state.anchoredDraggableState.offset
    val anchor = state.anchoredDraggableState.anchors.minAnchor()
    val overflow = if (offset < anchor) anchor - offset else 0f
    scaleY = if (overflow > 0f) 1 / ((size.height + overflow) / size.height) else 1f
    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
}
