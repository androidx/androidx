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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BasicTooltipBox
import androidx.compose.foundation.BasicTooltipDefaults
import androidx.compose.foundation.BasicTooltipState
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.tokens.PlainTooltipTokens
import androidx.compose.material3.tokens.RichTooltipTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
 * Plain tooltip that provides a descriptive message for an anchor.
 *
 * Tooltip that is invoked when the anchor is long pressed:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipSample
 *
 * If control of when the tooltip is shown is desired please see
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithManualInvocationSample
 *
 * @param tooltip the composable that will be used to populate the tooltip's content.
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param focusable [Boolean] that determines if the tooltip is focusable. When true,
 * the tooltip will consume touch events while it's shown and will have accessibility
 * focus move to the first element of the component. When false, the tooltip
 * won't consume touch events while it's shown but assistive-tech users will need
 * to swipe or drag to get to the first element of the component.
 * @param tooltipState handles the state of the tooltip's visibility.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param containerColor [Color] that will be applied to the tooltip's container.
 * @param contentColor [Color] that will be applied to the tooltip's content.
 * @param content the composable that the tooltip will anchor to.
 */
@Suppress("DEPRECATION")
@Composable
@ExperimentalMaterial3Api
@Deprecated("Use TooltipBox with PlainTooltip")
fun PlainTooltipBox(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    tooltipState: PlainTooltipState = rememberPlainTooltipState(),
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    content: @Composable TooltipBoxScope.() -> Unit
) {
    Material3TooltipBox(
        tooltipContent = {
            Box(modifier = Modifier.padding(PlainTooltipContentPadding)) {
                val textStyle =
                    MaterialTheme.typography.fromToken(PlainTooltipTokens.SupportingTextFont)
                CompositionLocalProvider(
                    LocalContentColor provides contentColor,
                    LocalTextStyle provides textStyle,
                    content = tooltip
                )
            }
        },
        modifier = modifier,
        focusable = focusable,
        tooltipState = tooltipState,
        shape = shape,
        containerColor = containerColor,
        tooltipPositionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        elevation = 0.dp,
        maxWidth = PlainTooltipMaxWidth,
        content = content
    )
}

/**
 * Rich text tooltip that allows the user to pass in a title, text, and action.
 * Tooltips are used to provide a descriptive message for an anchor.
 *
 * Tooltip that is invoked when the anchor is long pressed:
 *
 * @sample androidx.compose.material3.samples.RichTooltipSample
 *
 * If control of when the tooltip is shown is desired please see
 *
 * @sample androidx.compose.material3.samples.RichTooltipWithManualInvocationSample
 *
 * @param text the message to be displayed in the center of the tooltip.
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param focusable [Boolean] that determines if the tooltip is focusable. When true,
 * the tooltip will consume touch events while it's shown and will have accessibility
 * focus move to the first element of the component. When false, the tooltip
 * won't consume touch events while it's shown but assistive-tech users will need
 * to swipe or drag to get to the first element of the component.
 * @param tooltipState handles the state of the tooltip's visibility.
 * @param title An optional title for the tooltip.
 * @param action An optional action for the tooltip.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param colors [RichTooltipColors] that will be applied to the tooltip's container and content.
 * @param content the composable that the tooltip will anchor to.
 */
@Suppress("DEPRECATION")
@Composable
@ExperimentalMaterial3Api
@Deprecated("Use TooltipBox with RichTooltip")
fun RichTooltipBox(
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    tooltipState: RichTooltipState = rememberRichTooltipState(action != null),
    shape: Shape = TooltipDefaults.richTooltipContainerShape,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    content: @Composable TooltipBoxScope.() -> Unit
) {
    Material3TooltipBox(
        tooltipContent = {
            val actionLabelTextStyle =
                MaterialTheme.typography.fromToken(RichTooltipTokens.ActionLabelTextFont)
            val subheadTextStyle =
                MaterialTheme.typography.fromToken(RichTooltipTokens.SubheadFont)
            val supportingTextStyle =
                MaterialTheme.typography.fromToken(RichTooltipTokens.SupportingTextFont)
            Column(
                modifier = Modifier.padding(horizontal = RichTooltipHorizontalPadding)
            ) {
                title?.let {
                    Box(
                        modifier = Modifier.paddingFromBaseline(top = HeightToSubheadFirstLine)
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides colors.titleContentColor,
                            LocalTextStyle provides subheadTextStyle,
                            content = it
                        )
                    }
                }
                Box(
                    modifier = Modifier.textVerticalPadding(title != null, action != null)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.contentColor,
                        LocalTextStyle provides supportingTextStyle,
                        content = text
                    )
                }
                action?.let {
                    Box(
                        modifier = Modifier
                            .requiredHeightIn(min = ActionLabelMinHeight)
                            .padding(bottom = ActionLabelBottomPadding)
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides colors.actionContentColor,
                            LocalTextStyle provides actionLabelTextStyle,
                            content = it
                        )
                    }
                }
            }
        },
        shape = shape,
        containerColor = colors.containerColor,
        tooltipPositionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltipState = tooltipState,
        elevation = RichTooltipTokens.ContainerElevation,
        maxWidth = RichTooltipMaxWidth,
        modifier = modifier,
        focusable = focusable,
        content = content
    )
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Material3TooltipBox(
    tooltipContent: @Composable () -> Unit,
    tooltipPositionProvider: PopupPositionProvider,
    modifier: Modifier,
    focusable: Boolean,
    shape: Shape,
    tooltipState: BasicTooltipState,
    containerColor: Color,
    elevation: Dp,
    maxWidth: Dp,
    content: @Composable TooltipBoxScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val longPressLabel = getString(string = Strings.TooltipLongPressLabel)

    val scope = remember(tooltipState) {
        object : TooltipBoxScope {
            override fun Modifier.tooltipTrigger(): Modifier {
                val onLongPress = {
                    coroutineScope.launch {
                        tooltipState.show()
                    }
                }
                return pointerInput(tooltipState) {
                    awaitEachGesture {
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        val pass = PointerEventPass.Initial

                        // wait for the first down press
                        awaitFirstDown(pass = pass)

                        try {
                            // listen to if there is up gesture within the longPressTimeout limit
                            withTimeout(longPressTimeout) {
                                waitForUpOrCancellation(pass = pass)
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            // handle long press - Show the tooltip
                            onLongPress()

                            // consume the children's click handling
                            val changes = awaitPointerEvent(pass = pass).changes
                            for (i in 0 until changes.size) { changes[i].consume() }
                        }
                    }
                }.semantics(mergeDescendants = true) {
                    onLongClick(
                        label = longPressLabel,
                        action = {
                            onLongPress()
                            true
                        }
                    )
                }
            }
        }
    }

    Box {
        val transition = updateTransition(tooltipState.isVisible, label = "Tooltip transition")
        if (transition.currentState || transition.targetState) {
            val tooltipPaneDescription = getString(Strings.TooltipPaneDescription)
            TooltipPopup(
                popupPositionProvider = tooltipPositionProvider,
                onDismissRequest = {
                    if (tooltipState.isVisible) {
                        coroutineScope.launch { tooltipState.dismiss() }
                    }
                },
                focusable = focusable
            ) {
                Surface(
                    modifier = modifier
                        .sizeIn(
                            minWidth = TooltipMinWidth,
                            maxWidth = maxWidth,
                            minHeight = TooltipMinHeight
                        )
                        .animateTooltip(transition)
                        .semantics {
                            liveRegion = LiveRegionMode.Assertive
                            paneTitle = tooltipPaneDescription
                        },
                    shape = shape,
                    color = containerColor,
                    shadowElevation = elevation,
                    tonalElevation = elevation,
                    content = tooltipContent
                )
            }
        }

        scope.content()
    }

    DisposableEffect(tooltipState) {
        onDispose { tooltipState.onDispose() }
    }
}

/**
 * Scope of [PlainTooltipBox] and RichTooltipBox
 */
@Suppress("DEPRECATION")
@ExperimentalMaterial3Api
@Deprecated("Use TooltipBox with enableUserInput to handle default gestures")
interface TooltipBoxScope {
    /**
     * [Modifier] that should be applied to the anchor composable when showing the tooltip
     * after long pressing the anchor composable is desired. It appends a long click to
     * the composable that this modifier is chained with.
     */
    fun Modifier.tooltipTrigger(): Modifier
}

@Composable
@ExperimentalMaterial3Api
internal expect fun TooltipPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: () -> Unit,
    focusable: Boolean,
    content: @Composable () -> Unit
)

/**
 * Create and remember the default [PlainTooltipState].
 *
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated
 * with the mutator mutex, only one will be shown on the screen at any time.
 */
@Suppress("DEPRECATION")
@Composable
@ExperimentalMaterial3Api
@Deprecated("Use rememberTooltipState with TooltipBox.")
fun rememberPlainTooltipState(
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex
): PlainTooltipState =
    remember(mutatorMutex) { PlainTooltipStateImpl(mutatorMutex) }

/**
 * Create and remember the default [RichTooltipState].
 *
 * @param isPersistent [Boolean] that determines if the tooltip associated with this
 * [RichTooltipState] will be persistent or not. If isPersistent is true, then the tooltip will
 * only be dismissed when the user clicks outside the bounds of the tooltip or if
 * [TooltipState.dismiss] is called. When isPersistent is false, the tooltip will dismiss after
 * a short duration. Ideally, this should be set to true when an action is provided to the
 * [RichTooltipBox] that this [RichTooltipState] is associated with.
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated
 * with the mutator mutex, only one will be shown on the screen at any time.
 */
@Suppress("DEPRECATION")
@Composable
@ExperimentalMaterial3Api
@Deprecated("Use rememberTooltipState with TooltipBox")
fun rememberRichTooltipState(
    isPersistent: Boolean,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex
): RichTooltipState =
    remember(
        isPersistent,
        mutatorMutex
    ) { RichTooltipStateImpl(isPersistent = isPersistent, mutatorMutex = mutatorMutex) }

/**
 * The default implementation for [RichTooltipState]
 *
 * @param isPersistent [Boolean] that determines if the tooltip associated with this
 * [RichTooltipState] will be persistent or not. If isPersistent is true, then the tooltip will
 * only be dismissed when the user clicks outside the bounds of the tooltip or if
 * [TooltipState.dismiss] is called. When isPersistent is false, the tooltip will dismiss after
 * a short duration. Ideally, this should be set to true when an action is provided to the
 * [RichTooltipBox] that this [RichTooltipState] is associated with.
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated
 * with the mutator mutex, only one will be shown on the screen at any time.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Stable
internal class RichTooltipStateImpl(
    override val isPersistent: Boolean,
    private val mutatorMutex: MutatorMutex
) : RichTooltipState {

    /**
     * [Boolean] that will be used to update the visibility
     * state of the associated tooltip.
     */
    override var isVisible: Boolean by mutableStateOf(false)
        private set

    /**
     * Show the tooltip associated with the current [RichTooltipState].
     * It will persist or dismiss after a short duration depending on [isPersistent].
     * When this method is called, all of the other tooltips currently
     * being shown will dismiss.
     */
    override suspend fun show(mutatePriority: MutatePriority) {
        val cancellableShow: suspend () -> Unit = {
            suspendCancellableCoroutine { continuation ->
                isVisible = true
                job = continuation
            }
        }

        // Show associated tooltip for [TooltipDuration] amount of time
        // or until tooltip is explicitly dismissed depending on [isPersistent].
        mutatorMutex.mutate(mutatePriority) {
            try {
                if (isPersistent) {
                    cancellableShow()
                } else {
                    withTimeout(BasicTooltipDefaults.TooltipDuration) {
                        cancellableShow()
                    }
                }
            } finally {
                // timeout or cancellation has occurred
                // and we close out the current tooltip.
                isVisible = false
            }
        }
    }

    /**
     * continuation used to clean up
     */
    private var job: (CancellableContinuation<Unit>)? = null

    /**
     * Dismiss the tooltip associated with
     * this [RichTooltipState] if it's currently being shown.
     */
    override fun dismiss() {
        isVisible = false
    }

    /**
     * Cleans up [MutatorMutex] when the tooltip associated
     * with this state leaves Composition.
     */
    override fun onDispose() {
        job?.cancel()
    }
}

/**
 * The default implementation for [PlainTooltipState]
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Stable
internal class PlainTooltipStateImpl(private val mutatorMutex: MutatorMutex) : PlainTooltipState {

    /**
     * [Boolean] that will be used to update the visibility
     * state of the associated tooltip.
     */
    override var isVisible by mutableStateOf(false)
        private set

    override val isPersistent: Boolean = false

    /**
     * Show the tooltip associated with the current [PlainTooltipState].
     * It will dismiss after a short duration. When this method is called,
     * all of the other tooltips currently being shown will dismiss.
     */
    override suspend fun show(mutatePriority: MutatePriority) {
        mutatorMutex.mutate(mutatePriority) {
            try {
                withTimeout(BasicTooltipDefaults.TooltipDuration) {
                    suspendCancellableCoroutine { continuation ->
                        isVisible = true
                        job = continuation
                    }
                }
            } finally {
                // timeout or cancellation has occurred
                // and we close out the current tooltip.
                isVisible = false
            }
        }
    }

    /**
     * continuation used to clean up
     */
    private var job: (CancellableContinuation<Unit>)? = null

    /**
     * Dismiss the tooltip associated with
     * this [PlainTooltipState] if it's currently being shown.
     */
    override fun dismiss() {
        isVisible = false
    }

    /**
     * Cleans up [MutatorMutex] when the tooltip associated
     * with this state leaves Composition.
     */
    override fun onDispose() {
        job?.cancel()
    }
}

/**
 * The [TooltipState] that should be used with [RichTooltipBox]
 */
@Suppress("DEPRECATION")
@Stable
@ExperimentalMaterial3Api
@Deprecated("Use TooltipState with TooltipBox")
interface PlainTooltipState : BasicTooltipState

/**
 * The [TooltipState] that should be used with [RichTooltipBox]
 */
@Suppress("DEPRECATION")
@Stable
@ExperimentalMaterial3Api
@Deprecated("Use TooltipState with TooltipBox")
interface RichTooltipState : BasicTooltipState

/**
 * Material TooltipBox that wraps a composable with a tooltip.
 *
 * tooltips provide a descriptive message for an anchor.
 * It can be used to call the users attention to the anchor.
 *
 * Tooltip that is invoked when the anchor is long pressed:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipSample
 *
 * If control of when the tooltip is shown is desired please see
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithManualInvocationSample
 *
 * Tooltip that is invoked when the anchor is long pressed:
 *
 * @sample androidx.compose.material3.samples.RichTooltipSample
 *
 * If control of when the tooltip is shown is desired please see
 *
 * @sample androidx.compose.material3.samples.RichTooltipWithManualInvocationSample
 *
 * @param positionProvider [PopupPositionProvider] that will be used to place the tooltip
 * relative to the anchor content.
 * @param tooltip the composable that will be used to populate the tooltip's content.
 * @param state handles the state of the tooltip's visibility.
 * @param modifier the [Modifier] to be applied to the TooltipBox.
 * @param focusable [Boolean] that determines if the tooltip is focusable. When true,
 * the tooltip will consume touch events while it's shown and will have accessibility
 * focus move to the first element of the component. When false, the tooltip
 * won't consume touch events while it's shown but assistive-tech users will need
 * to swipe or drag to get to the first element of the component.
 * @param enableUserInput [Boolean] which determines if this TooltipBox will handle
 * long press and mouse hover to trigger the tooltip through the state provided.
 * @param content the composable that the tooltip will anchor to.
 */
@Composable
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable () -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {
    val transition = updateTransition(state.transition, label = "tooltip transition")
    BasicTooltipBox(
        positionProvider = positionProvider,
        tooltip = { Box(Modifier.animateTooltip(transition)) { tooltip() } },
        focusable = focusable,
        enableUserInput = enableUserInput,
        state = state,
        modifier = modifier,
        content = content
    )
}

/**
 * Plain tooltip that provides a descriptive message.
 *
 * Usually used with [TooltipBox].
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param contentColor [Color] that will be applied to the tooltip's content.
 * @param containerColor [Color] that will be applied to the tooltip's container.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param content the composable that will be used to populate the tooltip's content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainTooltip(
    modifier: Modifier = Modifier,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    content: @Composable () -> Unit
) {
    Surface(
        shape = shape,
        color = containerColor
    ) {
        Box(modifier = modifier
            .sizeIn(
                minWidth = TooltipMinWidth,
                maxWidth = PlainTooltipMaxWidth,
                minHeight = TooltipMinHeight
            )
            .padding(PlainTooltipContentPadding)
        ) {
            val textStyle =
                MaterialTheme.typography.fromToken(PlainTooltipTokens.SupportingTextFont)
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides textStyle,
                content = content
            )
        }
    }
}

/**
 * Rich text tooltip that allows the user to pass in a title, text, and action.
 * Tooltips are used to provide a descriptive message.
 *
 * Usually used with [TooltipBox]
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param title An optional title for the tooltip.
 * @param action An optional action for the tooltip.
 * @param colors [RichTooltipColors] that will be applied to the tooltip's container and content.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param text the composable that will be used to populate the rich tooltip's text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    shape: Shape = TooltipDefaults.richTooltipContainerShape,
    text: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .sizeIn(
                minWidth = TooltipMinWidth,
                maxWidth = RichTooltipMaxWidth,
                minHeight = TooltipMinHeight
            ),
        shape = shape,
        color = colors.containerColor,
        shadowElevation = RichTooltipTokens.ContainerElevation,
        tonalElevation = RichTooltipTokens.ContainerElevation
    ) {
        val actionLabelTextStyle =
            MaterialTheme.typography.fromToken(RichTooltipTokens.ActionLabelTextFont)
        val subheadTextStyle =
            MaterialTheme.typography.fromToken(RichTooltipTokens.SubheadFont)
        val supportingTextStyle =
            MaterialTheme.typography.fromToken(RichTooltipTokens.SupportingTextFont)

        Column(
            modifier = Modifier.padding(horizontal = RichTooltipHorizontalPadding)
        ) {
            title?.let {
                Box(
                    modifier = Modifier.paddingFromBaseline(top = HeightToSubheadFirstLine)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.titleContentColor,
                        LocalTextStyle provides subheadTextStyle,
                        content = it
                    )
                }
            }
            Box(
                modifier = Modifier.textVerticalPadding(title != null, action != null)
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.contentColor,
                    LocalTextStyle provides supportingTextStyle,
                    content = text
                )
            }
            action?.let {
                Box(
                    modifier = Modifier
                        .requiredHeightIn(min = ActionLabelMinHeight)
                        .padding(bottom = ActionLabelBottomPadding)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.actionContentColor,
                        LocalTextStyle provides actionLabelTextStyle,
                        content = it
                    )
                }
            }
        }
    }
}

/**
 * Tooltip defaults that contain default values for both [PlainTooltip] and [RichTooltip]
 */
@ExperimentalMaterial3Api
object TooltipDefaults {
    /**
     * The default [Shape] for a [PlainTooltip]'s container.
     */
    val plainTooltipContainerShape: Shape
        @Composable get() = PlainTooltipTokens.ContainerShape.value

    /**
     * The default [Color] for a [PlainTooltip]'s container.
     */
    val plainTooltipContainerColor: Color
        @Composable get() = PlainTooltipTokens.ContainerColor.value

    /**
     * The default [Color] for the content within the [PlainTooltip].
     */
    val plainTooltipContentColor: Color
        @Composable get() = PlainTooltipTokens.SupportingTextColor.value

    /**
     * The default [Shape] for a [RichTooltip]'s container.
     */
    val richTooltipContainerShape: Shape @Composable get() =
        RichTooltipTokens.ContainerShape.value

    /**
     * Method to create a [RichTooltipColors] for [RichTooltip]
     * using [RichTooltipTokens] to obtain the default colors.
     */
    @Composable
    fun richTooltipColors(
        containerColor: Color = RichTooltipTokens.ContainerColor.value,
        contentColor: Color = RichTooltipTokens.SupportingTextColor.value,
        titleContentColor: Color = RichTooltipTokens.SubheadColor.value,
        actionContentColor: Color = RichTooltipTokens.ActionLabelTextColor.value,
    ): RichTooltipColors =
        RichTooltipColors(
            containerColor = containerColor,
            contentColor = contentColor,
            titleContentColor = titleContentColor,
            actionContentColor = actionContentColor
        )

    /**
     * [PopupPositionProvider] that should be used with [PlainTooltip].
     * It correctly positions the tooltip in respect to the anchor content.
     *
     * @param spacingBetweenTooltipAndAnchor the spacing between the tooltip and the anchor content.
     */
    @Composable
    fun rememberPlainTooltipPositionProvider(
        spacingBetweenTooltipAndAnchor: Dp = SpacingBetweenTooltipAndAnchor
    ): PopupPositionProvider {
        val tooltipAnchorSpacing = with(LocalDensity.current) {
            spacingBetweenTooltipAndAnchor.roundToPx()
        }
        return remember(tooltipAnchorSpacing) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2

                    // Tooltip prefers to be above the anchor,
                    // but if this causes the tooltip to overlap with the anchor
                    // then we place it below the anchor
                    var y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
                    if (y < 0)
                        y = anchorBounds.bottom + tooltipAnchorSpacing
                    return IntOffset(x, y)
                }
            }
        }
    }

    /**
     * [PopupPositionProvider] that should be used with [RichTooltip].
     * It correctly positions the tooltip in respect to the anchor content.
     *
     * @param spacingBetweenTooltipAndAnchor the spacing between the tooltip and the anchor content.
     */
    @Composable
    fun rememberRichTooltipPositionProvider(
        spacingBetweenTooltipAndAnchor: Dp = SpacingBetweenTooltipAndAnchor
    ): PopupPositionProvider {
        val tooltipAnchorSpacing = with(LocalDensity.current) {
            spacingBetweenTooltipAndAnchor.roundToPx()
        }
        return remember(tooltipAnchorSpacing) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    var x = anchorBounds.right
                    // Try to shift it to the left of the anchor
                    // if the tooltip would collide with the right side of the screen
                    if (x + popupContentSize.width > windowSize.width) {
                        x = anchorBounds.left - popupContentSize.width
                        // Center if it'll also collide with the left side of the screen
                        if (x < 0)
                            x = anchorBounds.left +
                                (anchorBounds.width - popupContentSize.width) / 2
                    }

                    // Tooltip prefers to be above the anchor,
                    // but if this causes the tooltip to overlap with the anchor
                    // then we place it below the anchor
                    var y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
                    if (y < 0)
                        y = anchorBounds.bottom + tooltipAnchorSpacing
                    return IntOffset(x, y)
                }
            }
        }
    }
}

@Stable
@Immutable
@ExperimentalMaterial3Api
class RichTooltipColors(
    val containerColor: Color,
    val contentColor: Color,
    val titleContentColor: Color,
    val actionContentColor: Color
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RichTooltipColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (titleContentColor != other.titleContentColor) return false
        if (actionContentColor != other.actionContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + titleContentColor.hashCode()
        result = 31 * result + actionContentColor.hashCode()
        return result
    }
}

/**
 * Create and remember the default [TooltipState] for [TooltipBox].
 *
 * @param initialIsVisible the initial value for the tooltip's visibility when drawn.
 * @param isPersistent [Boolean] that determines if the tooltip associated with this
 * will be persistent or not. If isPersistent is true, then the tooltip will
 * only be dismissed when the user clicks outside the bounds of the tooltip or if
 * [TooltipState.dismiss] is called. When isPersistent is false, the tooltip will dismiss after
 * a short duration. Ideally, this should be set to true when there is actionable content
 * being displayed within a tooltip.
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated
 * with the mutator mutex, only one will be shown on the screen at any time.
 *
 */
@Composable
@ExperimentalMaterial3Api
fun rememberTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = false,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex
): TooltipState =
    remember(
        isPersistent,
        mutatorMutex
    ) {
        TooltipStateImpl(
            initialIsVisible = initialIsVisible,
            isPersistent = isPersistent,
            mutatorMutex = mutatorMutex
        )
    }

/**
 * Constructor extension function for [TooltipState]
 *
 * @param initialIsVisible the initial value for the tooltip's visibility when drawn.
 * @param isPersistent [Boolean] that determines if the tooltip associated with this
 * will be persistent or not. If isPersistent is true, then the tooltip will
 * only be dismissed when the user clicks outside the bounds of the tooltip or if
 * [TooltipState.dismiss] is called. When isPersistent is false, the tooltip will dismiss after
 * a short duration. Ideally, this should be set to true when there is actionable content
 * being displayed within a tooltip.
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated
 * with the mutator mutex, only one will be shown on the screen at any time.
 */
fun TooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = true,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex
): TooltipState =
    TooltipStateImpl(
        initialIsVisible = initialIsVisible,
        isPersistent = isPersistent,
        mutatorMutex = mutatorMutex
    )

@Stable
private class TooltipStateImpl(
    initialIsVisible: Boolean,
    override val isPersistent: Boolean,
    private val mutatorMutex: MutatorMutex
) : TooltipState {
    override val transition: MutableTransitionState<Boolean> =
        MutableTransitionState(initialIsVisible)

    override val isVisible: Boolean
        get() = transition.currentState || transition.targetState

            /**
     * continuation used to clean up
     */
    private var job: (CancellableContinuation<Unit>)? = null

    /**
     * Show the tooltip associated with the current [BasicTooltipState].
     * When this method is called, all of the other tooltips associated
     * with [mutatorMutex] will be dismissed.
     *
     * @param mutatePriority [MutatePriority] to be used with [mutatorMutex].
     */
    override suspend fun show(
        mutatePriority: MutatePriority
    ) {
        val cancellableShow: suspend () -> Unit = {
            suspendCancellableCoroutine { continuation ->
                transition.targetState = true
                job = continuation
            }
        }

        // Show associated tooltip for [TooltipDuration] amount of time
        // or until tooltip is explicitly dismissed depending on [isPersistent].
        mutatorMutex.mutate(mutatePriority) {
            try {
                if (isPersistent) {
                    cancellableShow()
                } else {
                    withTimeout(BasicTooltipDefaults.TooltipDuration) {
                        cancellableShow()
                    }
                }
            } finally {
                // timeout or cancellation has occurred
                // and we close out the current tooltip.
                dismiss()
            }
        }
    }

    /**
     * Dismiss the tooltip associated with
     * this [TooltipState] if it's currently being shown.
     */
    override fun dismiss() {
        transition.targetState = false
    }

    /**
     * Cleans up [mutatorMutex] when the tooltip associated
     * with this state leaves Composition.
     */
    override fun onDispose() {
        job?.cancel()
    }
}

/**
 * The state that is associated with a [TooltipBox].
 * Each instance of [TooltipBox] should have its own [TooltipState].
 */
interface TooltipState : BasicTooltipState {
    /**
     * The current transition state of the tooltip.
     * Used to start the transition of the tooltip when fading in and out.
     */
    val transition: MutableTransitionState<Boolean>
}

private fun Modifier.textVerticalPadding(
    subheadExists: Boolean,
    actionExists: Boolean
): Modifier {
    return if (!subheadExists && !actionExists) {
        this.padding(vertical = PlainTooltipVerticalPadding)
    } else {
        this
            .paddingFromBaseline(top = HeightFromSubheadToTextFirstLine)
            .padding(bottom = TextBottomPadding)
    }
}

private fun Modifier.animateTooltip(
    transition: Transition<Boolean>
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "animateTooltip"
        properties["transition"] = transition
    }
) {
    val scale by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // show tooltip
                tween(
                    durationMillis = TooltipFadeInDuration,
                    easing = LinearOutSlowInEasing
                )
            } else {
                // dismiss tooltip
                tween(
                    durationMillis = TooltipFadeOutDuration,
                    easing = LinearOutSlowInEasing
                )
            }
        },
        label = "tooltip transition: scaling"
    ) { if (it) 1f else 0.8f }

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // show tooltip
                tween(
                    durationMillis = TooltipFadeInDuration,
                    easing = LinearEasing
                )
            } else {
                // dismiss tooltip
                tween(
                    durationMillis = TooltipFadeOutDuration,
                    easing = LinearEasing
                )
            }
        },
        label = "tooltip transition: alpha"
    ) { if (it) 1f else 0f }

    this.graphicsLayer(
        scaleX = scale,
        scaleY = scale,
        alpha = alpha
    )
}

private val SpacingBetweenTooltipAndAnchor = 4.dp
internal val TooltipMinHeight = 24.dp
internal val TooltipMinWidth = 40.dp
private val PlainTooltipMaxWidth = 200.dp
private val PlainTooltipVerticalPadding = 4.dp
private val PlainTooltipHorizontalPadding = 8.dp
private val PlainTooltipContentPadding =
    PaddingValues(PlainTooltipHorizontalPadding, PlainTooltipVerticalPadding)
private val RichTooltipMaxWidth = 320.dp
internal val RichTooltipHorizontalPadding = 16.dp
private val HeightToSubheadFirstLine = 28.dp
private val HeightFromSubheadToTextFirstLine = 24.dp
private val TextBottomPadding = 16.dp
private val ActionLabelMinHeight = 36.dp
private val ActionLabelBottomPadding = 8.dp
// No specification for fade in and fade out duration, so aligning it with the behavior for snack bar
internal const val TooltipFadeInDuration = 150
internal const val TooltipFadeOutDuration = 75
