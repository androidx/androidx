/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.wear.compose.foundation.LocalScreenIsActive
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.materialcore.screenHeightPx
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content.
 *
 * Example of using AppScaffold and ScreenScaffold with ScalingLazyColumn:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithSLCEdgeButtonSample
 *
 * Example of using AppScaffold and ScreenScaffold with TransformingLazyColumn:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithTLCEdgeButtonSample
 * @param scrollState The scroll state for [ScalingLazyColumn], used to drive screen transitions
 *   such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param edgeButton Slot for an [EdgeButton] that takes the available space below a scrolling list.
 *   It will scale up and fade in when the user scrolls to the end of the list, and scale down and
 *   fade out as the user scrolls up.
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists. The bottom padding value is always ignored because we instead use
 *   [edgeButtonSpacing] to specify the gap between edge button and content - and the [EdgeButton]
 *   hugs the bottom of the screen.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param edgeButtonSpacing The space between [EdgeButton] and the list content
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. This overscroll effect will be shared with all components within this ScreenScaffold
 *   such as [edgeButton] and [scrollIndicator] through [LocalOverscrollFactory]. If necessary, this
 *   behaviour can be disabled by passing overscrollEffect = null.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists to properly offset the [EdgeButton].
 */
@Composable
public fun ScreenScaffold(
    scrollState: ScalingLazyListState,
    edgeButton: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    edgeButtonSpacing: Dp = ScreenScaffoldDefaults.EdgeButtonSpacing,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        edgeButton = edgeButton,
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        modifier = modifier,
        contentPadding = contentPadding,
        edgeButtonSpacing = edgeButtonSpacing,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        overscrollEffect = overscrollEffect,
        content = content,
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for [ScalingLazyColumn], used to drive screen transitions
 *   such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. This overscroll effect will be shared with all components within this ScreenScaffold
 *   such as [scrollIndicator] through [LocalOverscrollFactory]. If necessary, this behaviour can be
 *   disabled by passing overscrollEffect = null.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists to properly offset the [EdgeButton].
 */
@Composable
public fun ScreenScaffold(
    scrollState: ScalingLazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        scrollIndicator = scrollIndicator,
        overscrollEffect = overscrollEffect,
        content = content,
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content.
 *
 * Example of using AppScaffold and ScreenScaffold with ScalingLazyColumn:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithSLCEdgeButtonSample
 *
 * Example of using AppScaffold and ScreenScaffold with TransformingLazyColumn:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithTLCEdgeButtonSample
 *
 * Example of using ScreenScaffold with a [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.EdgeButtonListSample
 * @param scrollState The scroll state for [TransformingLazyColumn], used to drive screen
 *   transitions such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param edgeButton Slot for an [EdgeButton] that takes the available space below a scrolling list.
 *   It will scale up and fade in when the user scrolls to the end of the list, and scale down and
 *   fade out as the user scrolls up.
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists. The bottom padding value is always ignored because we instead use
 *   [edgeButtonSpacing] to specify the gap between edge button and content - and the [EdgeButton]
 *   hugs the bottom of the screen.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param edgeButtonSpacing The space between [EdgeButton] and the list content
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. This overscroll effect will be shared with all components within this ScreenScaffold
 *   such as [edgeButton] and [scrollIndicator] through [LocalOverscrollFactory]. If necessary, this
 *   behaviour can be disabled by passing overscrollEffect = null.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists to properly offset the [EdgeButton].
 */
@Composable
public fun ScreenScaffold(
    scrollState: TransformingLazyColumnState,
    edgeButton: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    edgeButtonSpacing: Dp = ScreenScaffoldDefaults.EdgeButtonSpacing,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        edgeButton = edgeButton,
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        edgeButtonSpacing = edgeButtonSpacing,
        overscrollEffect = overscrollEffect,
        content = content,
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 *
 * Example of using ScreenScaffold with a [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.EdgeButtonListSample
 * @param scrollState The scroll state for [TransformingLazyColumn], used to drive screen
 *   transitions such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. This overscroll effect will be shared with all components within this ScreenScaffold
 *   such as [scrollIndicator] through [LocalOverscrollFactory]. If necessary, this behaviour can be
 *   disabled by passing overscrollEffect = null.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists.
 */
@Composable
public fun ScreenScaffold(
    scrollState: TransformingLazyColumnState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        overscrollEffect = overscrollEffect,
        content = content,
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content.
 *
 * Example of using AppScaffold and ScreenScaffold with ScalingLazyColumn:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithSLCEdgeButtonSample
 *
 * Example of using AppScaffold and ScreenScaffold with TransformingLazyColumn:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithTLCEdgeButtonSample
 * @param scrollState The scroll state for [androidx.compose.foundation.lazy.LazyColumn], used to
 *   drive screen transitions such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param edgeButton Slot for an [EdgeButton] that takes the available space below a scrolling list.
 *   It will scale up and fade in when the user scrolls to the end of the list, and scale down and
 *   fade out as the user scrolls up.
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists. The bottom padding value is always ignored because we instead use
 *   [edgeButtonSpacing] to specify the gap between edge button and content - and the [EdgeButton]
 *   hugs the bottom of the screen.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param edgeButtonSpacing The space between [EdgeButton] and the list content
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. This overscroll effect will be shared with all components within this ScreenScaffold
 *   such as [edgeButton] and [scrollIndicator] through [LocalOverscrollFactory]. If necessary, this
 *   behaviour can be disabled by passing overscrollEffect = null.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists to properly offset the [EdgeButton].
 */
@Composable
public fun ScreenScaffold(
    scrollState: LazyListState,
    edgeButton: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    edgeButtonSpacing: Dp = ScreenScaffoldDefaults.EdgeButtonSpacing,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        edgeButton = edgeButton,
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        edgeButtonSpacing = edgeButtonSpacing,
        overscrollEffect = overscrollEffect,
        content = content,
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for [androidx.compose.foundation.lazy.LazyColumn], used to
 *   drive screen transitions such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. This overscroll effect will be shared with all components within this ScreenScaffold
 *   such as [scrollIndicator] through [LocalOverscrollFactory]. If necessary, this behaviour can be
 *   disabled by passing overscrollEffect = null.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists.
 */
@Composable
public fun ScreenScaffold(
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        overscrollEffect = overscrollEffect,
        content = content,
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState]. Note that
 * this version doesn't support a bottom button slot, for that use the overload that takes
 * [LazyListState] or the one that takes a [ScalingLazyListState].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for a Column, used to drive screen transitions such as
 *   [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. This overscroll effect will be shared with all components within this ScreenScaffold
 *   such as [scrollIndicator] through [LocalOverscrollFactory]. If necessary, this behaviour can be
 *   disabled by passing overscrollEffect = null.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists.
 */
@Composable
public fun ScreenScaffold(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit =
    ScreenScaffold(
        scrollInfoProvider = ScrollInfoProvider(scrollState),
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollIndicator = scrollIndicator,
        overscrollEffect = overscrollEffect,
        content = content,
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText], [ScrollIndicator] and the bottom button according to a
 * [scrollInfoProvider].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content. In this overload, both
 * edgeButton and scrollInfoProvider must be specified.
 *
 * Example of using AppScaffold and ScreenScaffold with ScalingLazyColumn:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithSLCEdgeButtonSample
 *
 * Example of using AppScaffold and ScreenScaffold with TransformingLazyColumn:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldWithTLCEdgeButtonSample
 * @param scrollInfoProvider Provider for scroll information used to scroll away screen elements
 *   such as [TimeText] and coordinate showing/hiding the [ScrollIndicator], this needs to be a
 *   [ScrollInfoProvider].
 * @param edgeButton slot for a [EdgeButton] that takes the available space below a scrolling list.
 *   It will scale up and fade in when the user scrolls to the end of the list, and scale down and
 *   fade out as the user scrolls up.
 * @param modifier The modifier for the screen scaffold.
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists. The bottom padding value is always ignored because we instead use
 *   [edgeButtonSpacing] to specify the gap between edge button and content - and the [EdgeButton]
 *   hugs the bottom of the screen.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param edgeButtonSpacing The space between [EdgeButton] and the list content. This gap size could
 *   not be smaller then [ScreenScaffoldDefaults.EdgeButtonMinSpacing].
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. This overscroll effect will be shared with all components within this ScreenScaffold
 *   such as [edgeButton] and [scrollIndicator] through [LocalOverscrollFactory]. If necessary, this
 *   behaviour can be disabled by passing overscrollEffect = null.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists to properly offset the [EdgeButton].
 */
@Composable
public fun ScreenScaffold(
    scrollInfoProvider: ScrollInfoProvider,
    edgeButton: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = null,
    edgeButtonSpacing: Dp = ScreenScaffoldDefaults.EdgeButtonSpacing,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    val localDensity = LocalDensity.current
    val effectiveEdgeButtonSpacing =
        (edgeButtonSpacing - ScreenScaffoldDefaults.EdgeButtonMinSpacing).coerceAtLeast(0.dp)
    // Adds the gap between content and edge button.
    val lastItemOffsetCorrection = with(localDensity) { effectiveEdgeButtonSpacing.toPx() }
    val edgeButtonHeightAnimationThresholdPx =
        with(localDensity) { EDGE_BUTTON_HEIGHT_ANIMATION_THRESHOLD.toPx() }

    ScreenScaffold(
        modifier = modifier,
        contentPadding = contentPadding,
        timeText = timeText,
        scrollInfoProvider = scrollInfoProvider,
        scrollIndicator = scrollIndicator,
        overscrollEffect = overscrollEffect,
        content = {
            var intrinsicButtonHeight by remember(edgeButton) { mutableStateOf<Float?>(null) }
            val currentEdgeButtonTargetHeight by
                remember(scrollInfoProvider, lastItemOffsetCorrection) {
                    derivedStateOf {
                        (scrollInfoProvider.lastItemOffset - lastItemOffsetCorrection)
                            .coerceAtLeast(0f)
                    }
                }
            val edgeButtonAnimatedHeight = remember { Animatable(currentEdgeButtonTargetHeight) }

            // Remember lambdas to avoid re-evaluations on recomposition.
            val edgeButtonContent: @Composable () -> Unit =
                remember(edgeButton, scrollInfoProvider) {
                    {
                        Box(
                            contentAlignment = Alignment.BottomCenter,
                            content = edgeButton,
                            modifier =
                                Modifier.align(Alignment.BottomCenter).dynamicHeight(
                                    onIntrinsicHeightMeasured = {
                                        if (intrinsicButtonHeight != it) {
                                            intrinsicButtonHeight = it
                                        }
                                    }
                                ) {
                                    if (scrollInfoProvider.isScrollInProgress) {
                                        currentEdgeButtonTargetHeight
                                    } else {
                                        edgeButtonAnimatedHeight.value
                                    }
                                },
                        )
                    }
                }
            val mainContent: @Composable () -> Unit =
                remember(contentPadding, effectiveEdgeButtonSpacing, content) {
                    {
                        content(
                            // Replace bottom content padding adjusted for the edge button.
                            ReplacePaddingValues(
                                contentPadding,
                                with(localDensity) {
                                    (intrinsicButtonHeight?.toDp() ?: 0.dp) +
                                        effectiveEdgeButtonSpacing
                                },
                            )
                        )
                    }
                }

            SubcomposeLayout() { constraints ->
                // Measure the EdgeButton first, to ensure that intrinsicButtonHeight is updated
                // before we measure the rest of the content.
                val edgeButtonMeasurable =
                    subcompose(SlotsEnum.EdgeButton, edgeButtonContent).first().measure(constraints)

                val mainMeasurables =
                    subcompose(SlotsEnum.Main, mainContent).fastMap { it.measure(constraints) }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    mainMeasurables.fastForEach { it.place(0, 0) }
                    edgeButtonMeasurable.place(0, 0)
                }
            }

            LaunchedEffect(
                scrollInfoProvider,
                lastItemOffsetCorrection,
                edgeButtonAnimatedHeight,
                edgeButtonHeightAnimationThresholdPx,
            ) {
                snapshotFlow {
                        Pair(scrollInfoProvider.isScrollInProgress, currentEdgeButtonTargetHeight)
                    }
                    .collectLatest { (isScrollInProgress, edgeButtonTargetHeight) ->
                        if (isScrollInProgress) {
                            if (edgeButtonAnimatedHeight.isRunning) {
                                edgeButtonAnimatedHeight.stop()
                            }
                            if (edgeButtonAnimatedHeight.value != edgeButtonTargetHeight) {
                                edgeButtonAnimatedHeight.snapTo(edgeButtonTargetHeight)
                            }
                        } else {
                            if (
                                abs(edgeButtonTargetHeight - edgeButtonAnimatedHeight.value) >
                                    edgeButtonHeightAnimationThresholdPx
                            ) {
                                launch {
                                    edgeButtonAnimatedHeight.animateTo(
                                        targetValue = edgeButtonTargetHeight,
                                        animationSpec = DEFAULT_EDGE_BUTTON_ANIMATION_SPEC,
                                    )
                                }
                            } else {
                                if (
                                    edgeButtonAnimatedHeight.value != edgeButtonTargetHeight &&
                                        !edgeButtonAnimatedHeight.isRunning
                                ) {
                                    edgeButtonAnimatedHeight.snapTo(edgeButtonTargetHeight)
                                }
                            }
                        }
                    }
            }
        },
    )
}

private enum class SlotsEnum {
    Main,
    EdgeButton,
}

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components. [AppScaffold] should
 * be at the top of the composition (because it provides [ScaffoldState] and layers [TimeText] on
 * top of all other content) and [ScreenScaffold] should be part of [AppScaffold]'s content. When
 * used in conjunction with SwipeDismissableNavHost, [AppScaffold] remains at the top of the
 * composition, whilst [ScreenScaffold] will be placed for each individual composable route.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollInfoProvider].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param modifier The modifier for the screen scaffold.
 * @param scrollInfoProvider Provider for scroll information used to scroll away screen elements
 *   such as [TimeText] and coordinate showing/hiding the [ScrollIndicator].
 * @param contentPadding The padding to apply around the entire content. This contentPadding is then
 *   received by the [content] and should be consumed by using [Modifier.padding] or contentPadding
 *   parameter of the lazy lists.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. This overscroll effect will be shared with all components within this ScreenScaffold
 *   such as [scrollIndicator] through [LocalOverscrollFactory]. If necessary, this behaviour can be
 *   disabled by passing overscrollEffect = null.
 * @param content The body content for this screen. The lambda receives a [PaddingValues] that
 *   should be applied to the content root via [Modifier.padding] or contentPadding parameter when
 *   used with lists.
 */
@Composable
public fun ScreenScaffold(
    modifier: Modifier = Modifier,
    scrollInfoProvider: ScrollInfoProvider? = null,
    contentPadding: PaddingValues = ScreenScaffoldDefaults.contentPadding,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = null,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    content: @Composable BoxScope.(PaddingValues) -> Unit,
): Unit {
    val scaffoldState = LocalScaffoldState.current
    val key = remember { Any() }

    // Update the timeText & scrollInfoProvider if there is a change and the screen is already
    // present
    scaffoldState.screenContent.updateIfNeeded(key, timeText, scrollInfoProvider)

    DisposableEffect(key) { onDispose { scaffoldState.screenContent.removeScreen(key) } }

    scaffoldState.screenContent.UpdateIdlingDetectorIfNeeded()

    val screenIsActive = LocalScreenIsActive.current
    LaunchedEffect(screenIsActive) {
        if (screenIsActive) {
            scaffoldState.screenContent.addScreen(key, timeText, scrollInfoProvider)
        } else {
            scaffoldState.screenContent.removeScreen(key)
        }
    }

    WrapWithOverscrollFactoryIfRequired(overscrollEffect) {
        Box(modifier.fillMaxSize()) {
            Box(modifier = Modifier.overscroll(overscrollEffect)) { content(contentPadding) }
            scrollInfoProvider?.let {
                AnimatedIndicator(
                    isVisible = {
                        scaffoldState.screenContent.screenStage.value != ScreenStage.Idle &&
                            scrollInfoProvider.isScrollable
                    },
                    content = scrollIndicator,
                )
            } ?: scrollIndicator?.let { it() }
        }
    }
}

/** Contains the default values used by [ScreenScaffold] */
public object ScreenScaffoldDefaults {

    /** The default space between [EdgeButton] and list content in [ScreenScaffold]. */
    public val EdgeButtonSpacing: Dp = 16.dp

    /** The minimum space between [EdgeButton] and list content in [ScreenScaffold]. */
    public val EdgeButtonMinSpacing: Dp = EdgeButtonVerticalPadding

    /** Default contentPadding added to the [ScreenScaffold]. */
    public val contentPadding: PaddingValues
        @Composable
        get() =
            PaddingValues(
                horizontal = PaddingDefaults.horizontalContentPadding(),
                vertical = PaddingDefaults.verticalContentPadding(),
            )
}

// Sets the height that will be used down the line, using a state as parameter, to avoid
// recompositions when the height changes.
internal fun Modifier.dynamicHeight(
    onIntrinsicHeightMeasured: (Float) -> Unit,
    heightState: () -> Float,
) = this.then(DynamicHeightElement(onIntrinsicHeightMeasured, heightState))

@Composable
private fun WrapWithOverscrollFactoryIfRequired(
    overscrollEffect: OverscrollEffect?,
    content: @Composable (() -> Unit),
) {
    val screenHeight = screenHeightPx()

    val overscrollFactory =
        LocalOverscrollFactory.current?.let { factory ->
            overscrollEffect?.let { effect ->
                OffsetOverscrollFactory(factory, effect, screenHeight)
            }
        }

    if (overscrollFactory != null) {
        CompositionLocalProvider(
            LocalOverscrollFactory provides overscrollFactory,
            content = content,
        )
    } else content()
}

// Following classes 'inspired' by 'WrapContentElement' / 'WrapContentNode'
private class DynamicHeightElement(
    val onIntrinsicHeightMeasured: (Float) -> Unit,
    val heightState: () -> Float,
) : ModifierNodeElement<DynamicHeightNode>() {
    override fun create(): DynamicHeightNode =
        DynamicHeightNode(onIntrinsicHeightMeasured, heightState)

    override fun update(node: DynamicHeightNode) {
        node.heightState = heightState
        node.onIntrinsicHeightMeasured = onIntrinsicHeightMeasured
        // Ensure we reset this if the node is reused in a different part of the tree.
        node.lastMeasureHeight = null
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "DynamicHeightElement"
    }

    override fun equals(other: Any?) =
        other is DynamicHeightElement &&
            heightState === other.heightState &&
            onIntrinsicHeightMeasured === other.onIntrinsicHeightMeasured

    override fun hashCode() = 31 * heightState.hashCode() + onIntrinsicHeightMeasured.hashCode()
}

private class DynamicHeightNode(
    var onIntrinsicHeightMeasured: (Float) -> Unit,
    var heightState: () -> Float,
) : LayoutModifierNode, Modifier.Node() {

    var lastMeasureHeight: Int? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Similar to .fillMaxWidth().height(heightState.value) but we observe the state in the
        // measurement pass, not on Composition.
        val height = heightState().roundToInt()
        if (lastMeasureHeight == null || height > 0 && lastMeasureHeight != height) {
            onIntrinsicHeightMeasured(measurable.maxIntrinsicHeight(constraints.maxWidth).toFloat())
            lastMeasureHeight = height
        }
        val wrappedConstraints =
            Constraints(constraints.maxWidth, constraints.maxWidth, height, height)
        val placeable = measurable.measure(wrappedConstraints)
        // Report that we take the full space, and BottomCenter align the content.
        val wrapperWidth = constraints.maxWidth
        val wrapperHeight = constraints.maxHeight
        return layout(wrapperWidth, wrapperHeight) {
            val position =
                IntOffset(
                    x = (wrapperWidth - placeable.width) / 2,
                    y = wrapperHeight - placeable.height,
                )
            placeable.place(position)
        }
    }
}

private class ReplacePaddingValues(paddingValues: PaddingValues, val bottomPadding: Dp) :
    PaddingValues by paddingValues {
    override fun calculateBottomPadding(): Dp = bottomPadding
}

/**
 * A proxy class that wraps an [OverscrollEffect] and monitors the amount of overscroll applied.
 * This proxy does not have a visual effect, and it is not attached to the node tree hierarchy.
 */
internal class OffsetOverscrollEffect(
    private val innerOverscrollEffect: OverscrollEffect,
    private val viewportHeight: Int,
) : OverscrollEffect {

    /**
     * The fraction representing the overscroll offset. It indicates how far the content has been
     * overscrolled. A value of 0.0f signifies no overscroll. Positive or negative values indicate
     * overscroll in the respective direction. The value can be any number in the inclusive range
     * from -1.0f to 1.0f. Components such as [ScrollIndicator] use this value to adjust their
     * behavior in response to overscroll.
     */
    val overscrollFraction: Float
        get() = if (viewportHeight != 0) overscrollOffset.y / viewportHeight.toFloat() else 0f

    private var overscrollOffset by mutableStateOf(Offset.Zero)

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset =
        innerOverscrollEffect.applyToScroll(delta, source) {
            val consumed = performScroll(it)
            val remaining = delta - consumed
            if (remaining.y != 0f) {
                overscrollOffset += remaining
            }
            return@applyToScroll consumed
        }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        innerOverscrollEffect.applyToFling(velocity) {
            val consumed = performFling(it)
            overscrollOffset = Offset.Zero
            return@applyToFling consumed
        }
    }

    override val isInProgress: Boolean
        get() = innerOverscrollEffect.isInProgress

    override val node: DelegatableNode = object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OffsetOverscrollEffect) return false

        if (innerOverscrollEffect != other.innerOverscrollEffect) return false
        if (viewportHeight != other.viewportHeight) return false
        return true
    }

    override fun hashCode(): Int {
        var result = innerOverscrollEffect.hashCode()
        result = 31 * result + viewportHeight.hashCode()
        return result
    }
}

private class OffsetOverscrollFactory(
    private val overscrollFactory: OverscrollFactory,
    private val overscrollEffect: OverscrollEffect,
    viewportHeight: Int,
) : OverscrollFactory {
    val withOverscrollProxy = OffsetOverscrollEffect(overscrollEffect, viewportHeight)

    override fun createOverscrollEffect(): OverscrollEffect {
        return withOverscrollProxy
    }

    override fun hashCode(): Int {
        var result = overscrollFactory.hashCode()
        result = 31 * result + overscrollEffect.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OffsetOverscrollFactory) return false

        if (overscrollFactory != other.overscrollFactory) return false
        if (overscrollEffect != other.overscrollEffect) return false
        return true
    }
}

private val EDGE_BUTTON_HEIGHT_ANIMATION_THRESHOLD = 16.dp
private val DEFAULT_EDGE_BUTTON_ANIMATION_SPEC: AnimationSpec<Float> =
    spring(stiffness = Spring.StiffnessMediumLow)
