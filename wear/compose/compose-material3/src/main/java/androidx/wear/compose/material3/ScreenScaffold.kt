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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.wear.compose.foundation.ActiveFocusListener
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.lazy.LazyColumnState
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content.
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for [ScalingLazyColumn], used to drive screen transitions
 *   such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param bottomButton Optional slot for a Button (usually an [EdgeButton]) that takes the available
 *   space below a scrolling list. It will scale up and fade in when the user scrolls to the end of
 *   the list, and scale down and fade out as the user scrolls up.
 * @param content The body content for this screen.
 */
@Composable
fun ScreenScaffold(
    scrollState: ScalingLazyListState,
    modifier: Modifier = Modifier,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    bottomButton: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) =
    if (bottomButton != null) {
        ScreenScaffold(
            bottomButton,
            ScrollInfoProvider(scrollState),
            modifier,
            timeText,
            scrollIndicator,
            content
        )
    } else {
        ScreenScaffold(
            modifier,
            timeText,
            ScrollInfoProvider(scrollState),
            scrollIndicator,
            content
        )
    }

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content.
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for [androidx.wear.compose.foundation.lazy.LazyColumn], used
 *   to drive screen transitions such as [TimeText] scroll away and showing/hiding
 *   [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator]. No scroll
 *   indicator is displayed if null is passed.
 * @param bottomButton Optional slot for a Button (usually an [EdgeButton]) that takes the available
 *   space below a scrolling list. It will scale up and fade in when the user scrolls to the end of
 *   the list, and scale down and fade out as the user scrolls up.
 * @param content The body content for this screen.
 */
@Composable
fun ScreenScaffold(
    scrollState: LazyColumnState,
    modifier: Modifier = Modifier,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = null,
    bottomButton: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) =
    if (bottomButton != null) {
        ScreenScaffold(
            bottomButton,
            ScrollInfoProvider(scrollState),
            modifier,
            timeText,
            scrollIndicator,
            content
        )
    } else {
        ScreenScaffold(
            modifier,
            timeText,
            ScrollInfoProvider(scrollState),
            scrollIndicator,
            content
        )
    }

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content.
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for [androidx.compose.foundation.lazy.LazyColumn], used to
 *   drive screen transitions such as [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param bottomButton Optional slot for a Button (usually an [EdgeButton]) that takes the available
 *   space below a scrolling list. It will scale up and fade in when the user scrolls to the end of
 *   the list, and scale down and fade out as the user scrolls up.
 * @param content The body content for this screen.
 */
@Composable
fun ScreenScaffold(
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    bottomButton: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) =
    if (bottomButton != null) {
        ScreenScaffold(
            bottomButton,
            ScrollInfoProvider(scrollState),
            modifier,
            timeText,
            scrollIndicator,
            content
        )
    } else {
        ScreenScaffold(
            modifier,
            timeText,
            ScrollInfoProvider(scrollState),
            scrollIndicator,
            content
        )
    }

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content.
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for a Column, used to drive screen transitions such as
 *   [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param bottomButton Optional slot for a Button (usually an [EdgeButton]) that takes the available
 *   space below a scrolling list. It will scale up and fade in when the user scrolls to the end of
 *   the list, and scale down and fade out as the user scrolls up.
 * @param bottomButtonHeight the maximum height of the space taken by the bottom button.
 * @param modifier The modifier for the screen scaffold.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param content The body content for this screen.
 */
@Composable
fun ScreenScaffold(
    scrollState: ScrollState,
    bottomButton: @Composable BoxScope.() -> Unit,
    bottomButtonHeight: Dp,
    modifier: Modifier = Modifier,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    content: @Composable BoxScope.() -> Unit,
) {
    val bottomButtonHeightPx = with(LocalDensity.current) { bottomButtonHeight.toPx() }
    ScreenScaffold(
        bottomButton,
        ScrollInfoProvider(scrollState, bottomButtonHeightPx),
        modifier,
        timeText,
        scrollIndicator,
        content
    )
}

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollState].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param scrollState The scroll state for a Column, used to drive screen transitions such as
 *   [TimeText] scroll away and showing/hiding [ScrollIndicator].
 * @param modifier The modifier for the screen scaffold.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param content The body content for this screen.
 */
@Composable
fun ScreenScaffold(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = {
        ScrollIndicator(scrollState, modifier = Modifier.align(Alignment.CenterEnd))
    },
    content: @Composable BoxScope.() -> Unit,
) = ScreenScaffold(modifier, timeText, ScrollInfoProvider(scrollState), scrollIndicator, content)

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText], [ScrollIndicator] and the bottom button according to a
 * [scrollInfoProvider].
 *
 * This version of [ScreenScaffold] has a special slot for a button at the bottom, that grows and
 * shrinks to take the available space after the scrollable content. In this overload, both
 * bottomButton and scrollInfoProvider must be specified.
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param bottomButton slot for a Button (usually an [EdgeButton]) that takes the available space
 *   below a scrolling list. It will scale up and fade in when the user scrolls to the end of the
 *   list, and scale down and fade out as the user scrolls up.
 * @param scrollInfoProvider Provider for scroll information used to scroll away screen elements
 *   such as [TimeText] and coordinate showing/hiding the [ScrollIndicator], this needs to be a
 *   [ScrollInfoProvider].
 * @param modifier The modifier for the screen scaffold.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param content The body content for this screen.
 */
@Composable
fun ScreenScaffold(
    bottomButton: @Composable BoxScope.() -> Unit,
    scrollInfoProvider: ScrollInfoProvider,
    modifier: Modifier = Modifier,
    timeText: (@Composable () -> Unit)? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) =
    ScreenScaffold(
        modifier,
        timeText,
        scrollInfoProvider,
        scrollIndicator,
        content = {
            content()
            Box(
                Modifier.align(Alignment.BottomCenter).dynamicHeight {
                    scrollInfoProvider.lastItemOffset.coerceAtLeast(0f)
                },
                contentAlignment = Alignment.BottomCenter,
                content = bottomButton
            )
        }
    )

/**
 * [ScreenScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components.
 *
 * [ScreenScaffold] displays the [ScrollIndicator] at the center-end of the screen by default and
 * coordinates showing/hiding [TimeText] and [ScrollIndicator] according to [scrollInfoProvider].
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 * @param modifier The modifier for the screen scaffold.
 * @param timeText Time text (both time and potentially status message) for this screen, if
 *   different to the time text at the [AppScaffold] level. When null, the time text from the
 *   [AppScaffold] is displayed for this screen.
 * @param scrollInfoProvider Provider for scroll information used to scroll away screen elements
 *   such as [TimeText] and coordinate showing/hiding the [ScrollIndicator].
 * @param scrollIndicator The [ScrollIndicator] to display on this screen, which is expected to be
 *   aligned to Center-End. It is recommended to use the Material3 [ScrollIndicator] which is
 *   provided by default. No scroll indicator is displayed if null is passed.
 * @param content The body content for this screen.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    timeText: (@Composable () -> Unit)? = null,
    scrollInfoProvider: ScrollInfoProvider? = null,
    scrollIndicator: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val scaffoldState = LocalScaffoldState.current
    val key = remember { Any() }

    key(scrollInfoProvider) {
        DisposableEffect(key) { onDispose { scaffoldState.removeScreen(key) } }

        ActiveFocusListener { focused ->
            if (focused) {
                scaffoldState.addScreen(key, timeText, scrollInfoProvider)
            } else {
                scaffoldState.removeScreen(key)
            }
        }
    }

    scaffoldState.UpdateIdlingDetectorIfNeeded()

    Box(modifier = modifier.fillMaxSize()) {
        content()
        scrollInfoProvider?.let {
            AnimatedScrollIndicator(
                scrollInfoProvider = scrollInfoProvider,
                content = scrollIndicator,
                stage = { scaffoldState.screenStage.value }
            )
        } ?: scrollIndicator?.let { it() }
    }
}

@Composable
private fun AnimatedScrollIndicator(
    scrollInfoProvider: ScrollInfoProvider,
    stage: () -> ScreenStage,
    content: @Composable (BoxScope.() -> Unit)? = null
) {
    // Skip if no scroll indicator provided
    content?.let { scrollIndicator ->
        val alphaValue = remember { mutableFloatStateOf(0f) }
        val animationSpec: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)
        LaunchedEffect(scrollInfoProvider, scrollIndicator) {
            launch {
                snapshotFlow {
                        if (stage() != ScreenStage.Idle && scrollInfoProvider.isScrollable) 1f
                        else 0f
                    }
                    .distinctUntilChanged()
                    .collectLatest { targetValue ->
                        animate(
                            alphaValue.floatValue,
                            targetValue,
                            animationSpec = animationSpec
                        ) { value, _ ->
                            alphaValue.floatValue = value
                        }
                    }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = alphaValue.floatValue },
            content = scrollIndicator
        )
    }
}

// Sets the height that will be used down the line, using a state as parameter, to avoid
// recompositions when the height changes.
internal fun Modifier.dynamicHeight(heightState: () -> Float) =
    this.then(DynamicHeightElement(heightState))

// Following classes 'inspired' by 'WrapContentElement' / 'WrapContentNode'
private class DynamicHeightElement(val heightState: () -> Float) :
    ModifierNodeElement<DynamicHeightNode>() {
    override fun create(): DynamicHeightNode = DynamicHeightNode(heightState)

    override fun update(node: DynamicHeightNode) {
        node.heightState = heightState
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "MyHeightElement"
    }

    override fun equals(other: Any?) =
        other is DynamicHeightElement && heightState === other.heightState

    override fun hashCode() = heightState.hashCode()
}

private class DynamicHeightNode(var heightState: () -> Float) :
    LayoutModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // Similar to .fillMaxWidth().height(heightState.value) but we observe the state in the
        // measurement pass, not on Composition.
        val height = heightState().roundToInt()

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
                    y = wrapperHeight - placeable.height
                )
            placeable.place(position)
        }
    }
}
