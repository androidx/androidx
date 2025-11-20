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

package androidx.xr.compose.material3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarColors
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.horizontalEnterTransition
import androidx.compose.material3.FloatingToolbarDefaults.horizontalExitTransition
import androidx.compose.material3.FloatingToolbarDefaults.verticalEnterTransition
import androidx.compose.material3.FloatingToolbarDefaults.verticalExitTransition
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.FloatingToolbarVerticalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbarOverride
import androidx.compose.material3.HorizontalFloatingToolbarOverrideScope
import androidx.compose.material3.HorizontalFloatingToolbarWithFabOverride
import androidx.compose.material3.HorizontalFloatingToolbarWithFabOverrideScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.VerticalFloatingToolbarOverride
import androidx.compose.material3.VerticalFloatingToolbarOverrideScope
import androidx.compose.material3.VerticalFloatingToolbarWithFabOverride
import androidx.compose.material3.VerticalFloatingToolbarWithFabOverrideScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.xr.compose.material3.tokens.XrTokens
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape

/**
 * A horizontal floating toolbar displays navigation and key actions in a [Row]. It can be
 * positioned anywhere on the screen and floats over the rest of the content.
 *
 * @param expanded whether the FloatingToolbar is in expanded mode, i.e. showing [leadingContent]
 *   and [trailingContent]. Note that the toolbar will stay expanded in case a touch exploration
 *   service (e.g., TalkBack) is active.
 * @param modifier the [Modifier] to be applied to this FloatingToolbar.
 * @param colors the colors used for this floating toolbar. There are two predefined
 *   [FloatingToolbarColors] at [FloatingToolbarDefaults.standardFloatingToolbarColors] and
 *   [FloatingToolbarDefaults.vibrantFloatingToolbarColors] which you can use or modify.
 * @param contentPadding the padding applied to the content of this FloatingToolbar.
 * @param scrollBehavior a [FloatingToolbarScrollBehavior]. If null, this FloatingToolbar will not
 *   automatically react to scrolling.
 * @param leadingContent the leading content of this FloatingToolbar. The default layout here is a
 *   [Row], so content inside will be placed horizontally. Only showing if [expanded] is true.
 * @param trailingContent the trailing content of this FloatingToolbar. The default layout here is a
 *   [Row], so content inside will be placed horizontally. Only showing if [expanded] is true.
 * @param content the main content of this FloatingToolbar. The default layout here is a [Row], so
 *   content inside will be placed horizontally.
 *
 * TODO(kmost): Add a @sample tag and create a new sample project for XR.
 */
@ExperimentalMaterial3XrApi
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun HorizontalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    leadingContent: @Composable (RowScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    HorizontalOrbiter(LocalHorizontalFloatingToolbarOrbiterProperties.current) {
        Row(
            modifier =
                Modifier.then(
                        scrollBehavior?.let { with(it) { Modifier.floatingScrollBehavior() } }
                            ?: Modifier
                    )
                    .heightIn(min = XrFloatingToolbarTokens.HorizontalToolbarContainerHeight)
                    .background(color = colors.toolbarContainerColor)
                    .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            val expandedState by rememberUpdatedState(expanded)
            CompositionLocalProvider(LocalContentColor provides colors.toolbarContentColor) {
                leadingContent?.let {
                    AnimatedVisibility(
                        visible = expandedState,
                        enter = horizontalEnterTransition(expandFrom = Alignment.End),
                        exit = horizontalExitTransition(shrinkTowards = Alignment.End),
                    ) {
                        Row(content = it)
                    }
                }
                Row(content = content)
                trailingContent?.let {
                    AnimatedVisibility(
                        visible = expandedState,
                        enter = horizontalEnterTransition(expandFrom = Alignment.End),
                        exit = horizontalExitTransition(shrinkTowards = Alignment.End),
                    ) {
                        Row(content = it)
                    }
                }
            }
        }
    }
}

/**
 * A floating toolbar that displays horizontally. The bar features its content within a [Row], and
 * an adjacent floating icon button. It can be positioned anywhere on the screen, floating above
 * other content, and even in a `Scaffold`'s floating action button slot. Its [expanded] flag
 * controls the visibility of the actions with a slide animations.
 *
 * Note: This component will stay expanded to maintain the toolbar visibility for users with touch
 * exploration services enabled (e.g., TalkBack).
 *
 * In case the toolbar is aligned to the right or the left of the screen, you may apply a
 * [FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll] `Modifier` to update the [expanded]
 * state when scrolling occurs, as this sample shows:
 *
 * @sample androidx.compose.material3.samples.HorizontalFloatingToolbarWithFabSample
 *
 * Note that if your app uses a `Snackbar`, it's best to position the toolbar in a `Scaffold`'s FAB
 * slot. This ensures the `Snackbar` appears above the toolbar, preventing any visual overlap or
 * interference. See this sample:
 *
 * @sample androidx.compose.material3.samples.HorizontalFloatingToolbarAsScaffoldFabSample
 * @param expanded whether the floating toolbar is expanded or not. In its expanded state, the FAB
 *   and the toolbar content are organized horizontally. Otherwise, only the FAB is visible.
 * @param floatingActionButton a floating action button to be displayed by the toolbar. It's
 *   recommended to use a [FloatingToolbarDefaults.VibrantFloatingActionButton] or
 *   [FloatingToolbarDefaults.StandardFloatingActionButton] that is styled to match the [colors].
 *   Note that the provided FAB's size is controlled by the floating toolbar and animates according
 *   to its state. In case a custom FAB is provided, make sure it's set with a
 *   [Modifier.fillMaxSize] to be sized correctly.
 * @param modifier the [Modifier] to be applied to this floating toolbar.
 * @param colors the colors used for this floating toolbar. There are two predefined
 *   [FloatingToolbarColors] at [FloatingToolbarDefaults.standardFloatingToolbarColors] and
 *   [FloatingToolbarDefaults.vibrantFloatingToolbarColors] which you can use or modify. See also
 *   [floatingActionButton] for more information on the right FAB to use for proper styling.
 * @param contentPadding the padding applied to the content of this floating toolbar.
 * @param scrollBehavior a [FloatingToolbarScrollBehavior]. If provided, this FloatingToolbar will
 *   automatically react to scrolling.
 * @param shape the shape used for this floating toolbar content.
 * @param floatingActionButtonPosition the position of the floating toolbar's floating action
 *   button. By default, the FAB is placed at the end of the toolbar (i.e. aligned to the right in
 *   left-to-right layout, or to the left in right-to-left layout).
 * @param content the main content of this floating toolbar. The default layout here is a [Row], so
 *   content inside will be placed horizontally.
 */
@ExperimentalMaterial3XrApi
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun HorizontalFloatingToolbar(
    expanded: Boolean,
    floatingActionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = FloatingToolbarDefaults.ContainerShape,
    floatingActionButtonPosition: FloatingToolbarHorizontalFabPosition =
        FloatingToolbarHorizontalFabPosition.End,
    content: @Composable RowScope.() -> Unit,
) {
    HorizontalOrbiter(
        LocalHorizontalFloatingToolbarOrbiterProperties.current.copy(
            shape = SpatialRoundedCornerShape(CornerSize(percent = 0))
        )
    ) {
        Row(
            modifier =
                Modifier.heightIn(XrFloatingToolbarTokens.HorizontalToolbarContainerHeight)
                    .then(
                        scrollBehavior?.let { with(it) { Modifier.floatingScrollBehavior() } }
                            ?: Modifier
                    ),
            horizontalArrangement = Arrangement.spacedBy(XrFloatingToolbarTokens.ToolbarToFabGap),
        ) {
            val expandedState by rememberUpdatedState(expanded)
            CompositionLocalProvider(LocalContentColor provides colors.toolbarContentColor) {
                if (floatingActionButtonPosition == FloatingToolbarHorizontalFabPosition.End) {
                    AnimatedVisibility(
                        visible = expandedState,
                        enter = horizontalEnterTransition(Alignment.End),
                        exit = horizontalExitTransition(Alignment.End),
                    ) {
                        Row(
                            modifier =
                                Modifier.background(
                                        color = colors.toolbarContainerColor,
                                        shape = shape,
                                    )
                                    .padding(contentPadding)
                                    .verticalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            content = content,
                        )
                    }
                }
                Box(
                    modifier = Modifier.size(XrFloatingToolbarTokens.VerticalToolbarContainerWidth)
                ) {
                    floatingActionButton()
                }
                if (floatingActionButtonPosition == FloatingToolbarHorizontalFabPosition.Start) {
                    AnimatedVisibility(
                        visible = expandedState,
                        enter = horizontalEnterTransition(Alignment.Start),
                        exit = horizontalExitTransition(Alignment.Start),
                    ) {
                        Row(
                            modifier =
                                Modifier.background(
                                        color = colors.toolbarContainerColor,
                                        shape = shape,
                                    )
                                    .padding(contentPadding)
                                    .verticalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

/**
 * XR-specific Vertical Floating Toolbar that displays content in a [Column] in an end-aligned
 * [VerticalOrbiter].
 *
 * Note: This component will stay expanded to maintain the toolbar visibility for users with touch
 * exploration services enabled (e.g., TalkBack).
 *
 * @param expanded whether the FloatingToolbar is in expanded mode, i.e. showing [leadingContent]
 *   and [trailingContent]. Note that the toolbar will stay expanded in case a touch exploration
 *   service (e.g., TalkBack) is active.
 * @param modifier the [Modifier] to be applied to this FloatingToolbar.
 * @param colors the colors used for this floating toolbar. There are two predefined
 *   [FloatingToolbarColors] at [FloatingToolbarDefaults.standardFloatingToolbarColors] and
 *   [FloatingToolbarDefaults.vibrantFloatingToolbarColors] which you can use or modify.
 * @param contentPadding the padding applied to the content of this FloatingToolbar.
 * @param scrollBehavior a [FloatingToolbarScrollBehavior]. If null, this FloatingToolbar will not
 *   automatically react to scrolling. Note that the toolbar will not react to scrolling in case a
 *   touch exploration service (e.g., TalkBack) is active.
 * @param leadingContent the leading content of this FloatingToolbar. The default layout here is a
 *   [Column], so content inside will be placed vertically. Only showing if [expanded] is true.
 * @param trailingContent the trailing content of this FloatingToolbar. The default layout here is a
 *   [Column], so content inside will be placed vertically. Only showing if [expanded] is true.
 * @param content the main content of this FloatingToolbar. The default layout here is a [Column],
 *   so content inside will be placed vertically.
 */
@ExperimentalMaterial3ExpressiveApi
@ExperimentalMaterial3XrApi
@Composable
public fun VerticalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    leadingContent: @Composable (ColumnScope.() -> Unit)? = null,
    trailingContent: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val orbiterProperties = LocalVerticalFloatingToolbarOrbiterProperties.current
    VerticalOrbiter(properties = orbiterProperties) {
        Column(
            modifier =
                Modifier.then(
                        scrollBehavior?.let { with(it) { Modifier.floatingScrollBehavior() } }
                            ?: Modifier
                    )
                    .widthIn(min = XrFloatingToolbarTokens.VerticalToolbarContainerWidth)
                    .background(color = colors.toolbarContainerColor)
                    .padding(contentPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val expandedState by rememberUpdatedState(expanded)
            CompositionLocalProvider(LocalContentColor provides colors.toolbarContentColor) {
                leadingContent?.let {
                    AnimatedVisibility(
                        visible = expandedState,
                        enter = verticalEnterTransition(expandFrom = Alignment.Bottom),
                        exit = verticalExitTransition(shrinkTowards = Alignment.Bottom),
                    ) {
                        Column(content = it)
                    }
                }
                Column(content = content)
                trailingContent?.let {
                    AnimatedVisibility(
                        visible = expandedState,
                        enter = verticalEnterTransition(expandFrom = Alignment.Top),
                        exit = verticalExitTransition(shrinkTowards = Alignment.Top),
                    ) {
                        Column(content = it)
                    }
                }
            }
        }
    }
}

/**
 * XR-specific Vertical Floating Toolbar that displays content in a [Column] in an end-aligned
 * [VerticalOrbiter] alongside an adjacent floating action button.
 *
 * Note: This component will stay expanded to maintain the toolbar visibility for users with touch
 * exploration services enabled (e.g., TalkBack).
 *
 * @param expanded whether the floating toolbar is expanded or not. In its expanded state, the FAB
 *   and the toolbar content are organized vertically. Otherwise, only the FAB is visible. Note that
 *   the toolbar will stay expanded in case a touch exploration service (e.g., TalkBack) is active.
 * @param floatingActionButton a floating action button to be displayed by the toolbar. It's
 *   recommended to use a [FloatingToolbarDefaults.VibrantFloatingActionButton] or
 *   [FloatingToolbarDefaults.StandardFloatingActionButton] that is styled to match the [colors].
 *   Note that the provided FAB's size is controlled by the floating toolbar and animates according
 *   to its state. In case a custom FAB is provided, make sure it's set with a
 *   [Modifier.fillMaxSize] to be sized correctly.
 * @param modifier the [Modifier] to be applied to this floating toolbar.
 * @param colors the colors used for this floating toolbar. There are two predefined
 *   [FloatingToolbarColors] at [FloatingToolbarDefaults.standardFloatingToolbarColors] and
 *   [FloatingToolbarDefaults.vibrantFloatingToolbarColors] which you can use or modify. See also
 *   [floatingActionButton] for more information on the right FAB to use for proper styling.
 * @param contentPadding the padding applied to the content of this floating toolbar.
 * @param scrollBehavior a [FloatingToolbarScrollBehavior]. If provided, this FloatingToolbar will
 *   automatically react to scrolling.
 * @param shape the shape used for this floating toolbar content.
 * @param floatingActionButtonPosition the position of the floating toolbar's floating action
 *   button. By default, the FAB is placed at the bottom of the toolbar (i.e. aligned to the
 *   bottom).
 * @param content the main content of this floating toolbar. The default layout here is a [Column],
 *   so content inside will be placed vertically.
 */
@ExperimentalMaterial3ExpressiveApi
@ExperimentalMaterial3XrApi
@Composable
public fun VerticalFloatingToolbar(
    expanded: Boolean,
    floatingActionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = FloatingToolbarDefaults.ContainerShape,
    floatingActionButtonPosition: FloatingToolbarVerticalFabPosition =
        FloatingToolbarVerticalFabPosition.Bottom,
    content: @Composable ColumnScope.() -> Unit,
) {
    VerticalOrbiter(
        LocalVerticalFloatingToolbarOrbiterProperties.current.copy(
            shape = SpatialRoundedCornerShape(CornerSize(percent = 0))
        )
    ) {
        Column(
            modifier =
                Modifier.widthIn(XrFloatingToolbarTokens.VerticalToolbarContainerWidth)
                    .then(
                        scrollBehavior?.let { with(it) { Modifier.floatingScrollBehavior() } }
                            ?: Modifier
                    ),
            verticalArrangement = Arrangement.spacedBy(XrFloatingToolbarTokens.ToolbarToFabGap),
        ) {
            val expandedState by rememberUpdatedState(expanded)
            CompositionLocalProvider(LocalContentColor provides colors.toolbarContentColor) {
                if (floatingActionButtonPosition == FloatingToolbarVerticalFabPosition.Bottom) {
                    AnimatedVisibility(
                        visible = expandedState,
                        enter = verticalEnterTransition(Alignment.Bottom),
                        exit = verticalExitTransition(Alignment.Bottom),
                    ) {
                        Column(
                            modifier =
                                Modifier.background(
                                        color = colors.toolbarContainerColor,
                                        shape = shape,
                                    )
                                    .padding(contentPadding)
                                    .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            content = content,
                        )
                    }
                }
                Box(
                    modifier = Modifier.size(XrFloatingToolbarTokens.VerticalToolbarContainerWidth)
                ) {
                    floatingActionButton()
                }
                if (floatingActionButtonPosition == FloatingToolbarVerticalFabPosition.Top) {
                    AnimatedVisibility(
                        visible = expandedState,
                        enter = verticalEnterTransition(Alignment.Top),
                        exit = verticalExitTransition(Alignment.Top),
                    ) {
                        Column(
                            modifier =
                                Modifier.background(
                                        color = colors.toolbarContainerColor,
                                        shape = shape,
                                    )
                                    .padding(contentPadding)
                                    .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

private object XrFloatingToolbarTokens {
    val OrbiterOffset = 24.dp

    val VerticalToolbarContainerWidth = 64.dp
    val HorizontalToolbarContainerHeight = 64.dp

    val ToolbarToFabGap = 8.dp
}

/** [HorizontalFloatingToolbarOverride] that uses the XR-specific [HorizontalFloatingToolbar]. */
@OptIn(
    ExperimentalMaterial3ComponentOverrideApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3XrApi::class,
)
internal object XrHorizontalFloatingToolbarOverride : HorizontalFloatingToolbarOverride {
    @Composable
    override fun HorizontalFloatingToolbarOverrideScope.HorizontalFloatingToolbar() {
        HorizontalFloatingToolbar(
            expanded = isExpanded,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            content = content,
        )
    }
}

/**
 * [HorizontalFloatingToolbarWithFabOverride] that uses the XR-specific [HorizontalFloatingToolbar],
 * with a floating action button (FAB).
 */
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3ComponentOverrideApi::class,
    ExperimentalMaterial3XrApi::class,
)
internal object XrHorizontalFloatingToolbarWithFabOverride :
    HorizontalFloatingToolbarWithFabOverride {
    @Composable
    override fun HorizontalFloatingToolbarWithFabOverrideScope.HorizontalFloatingToolbarWithFab() {
        HorizontalFloatingToolbar(
            expanded = isExpanded,
            floatingActionButton = floatingActionButton,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            shape = shape,
            floatingActionButtonPosition = floatingActionButtonPosition,
            content = content,
        )
    }
}

/** [VerticalFloatingToolbarOverride] that uses the XR-specific [VerticalFloatingToolbar]. */
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3ComponentOverrideApi::class,
    ExperimentalMaterial3XrApi::class,
)
internal object XrVerticalFloatingToolbarOverride : VerticalFloatingToolbarOverride {
    @Composable
    override fun VerticalFloatingToolbarOverrideScope.VerticalFloatingToolbar() {
        VerticalFloatingToolbar(
            expanded = isExpanded,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            content = content,
        )
    }
}

/**
 * [VerticalFloatingToolbarWithFabOverride] that uses the XR-specific [VerticalFloatingToolbar],
 * with a floating action button (FAB).
 */
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3ComponentOverrideApi::class,
    ExperimentalMaterial3XrApi::class,
)
internal object XrVerticalFloatingToolbarWithFabOverride : VerticalFloatingToolbarWithFabOverride {
    @Composable
    override fun VerticalFloatingToolbarWithFabOverrideScope.VerticalFloatingToolbarWithFab() {
        VerticalFloatingToolbar(
            expanded = isExpanded,
            floatingActionButton = floatingActionButton,
            modifier = modifier,
            colors = colors,
            contentPadding = contentPadding,
            scrollBehavior = scrollBehavior,
            shape = shape,
            floatingActionButtonPosition = floatingActionButtonPosition,
            content = content,
        )
    }
}

/**
 * The default [HorizontalOrbiterProperties] used by [HorizontalFloatingToolbar] if none is
 * specified in [LocalHorizontalFloatingToolbarOrbiterProperties].
 */
@OptIn(ExperimentalMaterial3XrApi::class)
public val DefaultHorizontalFloatingToolbarOrbiterProperties: HorizontalOrbiterProperties =
    HorizontalOrbiterProperties(
        position = ContentEdge.Horizontal.Bottom,
        offset = XrFloatingToolbarTokens.OrbiterOffset,
        offsetType = OrbiterOffsetType.InnerEdge,
        alignment = Alignment.CenterHorizontally,
        shape = XrTokens.ContainerShape,
    )

/** The [HorizontalOrbiterProperties] used by [HorizontalFloatingToolbar]. */
@OptIn(ExperimentalMaterial3XrApi::class)
public val LocalHorizontalFloatingToolbarOrbiterProperties:
    ProvidableCompositionLocal<HorizontalOrbiterProperties> =
    compositionLocalOf {
        DefaultHorizontalFloatingToolbarOrbiterProperties
    }

/**
 * The default [VerticalOrbiterProperties] used by [VerticalFloatingToolbar] if none is specified in
 * [LocalVerticalFloatingToolbarOrbiterProperties].
 */
@OptIn(ExperimentalMaterial3XrApi::class)
public val DefaultVerticalFloatingToolbarOrbiterProperties: VerticalOrbiterProperties =
    VerticalOrbiterProperties(
        position = ContentEdge.Vertical.End,
        offset = XrFloatingToolbarTokens.OrbiterOffset,
        offsetType = OrbiterOffsetType.InnerEdge,
        alignment = Alignment.CenterVertically,
        shape = XrTokens.ContainerShape,
    )

/** The [VerticalOrbiterProperties] used by [VerticalFloatingToolbar]. */
@OptIn(ExperimentalMaterial3XrApi::class)
public val LocalVerticalFloatingToolbarOrbiterProperties:
    ProvidableCompositionLocal<VerticalOrbiterProperties> =
    compositionLocalOf {
        DefaultVerticalFloatingToolbarOrbiterProperties
    }
