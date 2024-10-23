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

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.material3.internal.BasicTooltipBox
import androidx.compose.material3.internal.BasicTooltipDefaults
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.PlainTooltipTokens
import androidx.compose.material3.tokens.RichTooltipTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
 * Material TooltipBox that wraps a composable with a tooltip.
 *
 * tooltips provide a descriptive message for an anchor. It can be used to call the users attention
 * to the anchor.
 *
 * Tooltip that is invoked when the anchor is long pressed:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipSample
 *
 * If control of when the tooltip is shown is desired please see
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithManualInvocationSample
 *
 * Plain tooltip with caret shown on long press:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaret
 *
 * Plain tooltip shown on long press with a custom caret:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCustomCaret
 *
 * Tooltip that is invoked when the anchor is long pressed:
 *
 * @sample androidx.compose.material3.samples.RichTooltipSample
 *
 * If control of when the tooltip is shown is desired please see
 *
 * @sample androidx.compose.material3.samples.RichTooltipWithManualInvocationSample
 *
 * Rich tooltip with caret shown on long press:
 *
 * @sample androidx.compose.material3.samples.RichTooltipWithCaretSample
 *
 * Rich tooltip shown on long press with a custom caret
 *
 * @sample androidx.compose.material3.samples.RichTooltipWithCustomCaretSample
 * @param positionProvider [PopupPositionProvider] that will be used to place the tooltip relative
 *   to the anchor content.
 * @param tooltip the composable that will be used to populate the tooltip's content.
 * @param state handles the state of the tooltip's visibility.
 * @param modifier the [Modifier] to be applied to the TooltipBox.
 * @param focusable [Boolean] that determines if the tooltip is focusable. When true, the tooltip
 *   will consume touch events while it's shown and will have accessibility focus move to the first
 *   element of the component. When false, the tooltip won't consume touch events while it's shown
 *   but assistive-tech users will need to swipe or drag to get to the first element of the
 *   component.
 * @param enableUserInput [Boolean] which determines if this TooltipBox will handle long press and
 *   mouse hover to trigger the tooltip through the state provided.
 * @param content the composable that the tooltip will anchor to.
 */
@Composable
@ExperimentalMaterial3Api
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {
    @Suppress("DEPRECATION")
    val transition = updateTransition(state.transition, label = "tooltip transition")
    val anchorBounds: MutableState<LayoutCoordinates?> = remember { mutableStateOf(null) }
    val scope = remember { TooltipScopeImpl { anchorBounds.value } }

    val wrappedContent: @Composable () -> Unit = {
        Box(modifier = Modifier.onGloballyPositioned { anchorBounds.value = it }) { content() }
    }

    BasicTooltipBox(
        positionProvider = positionProvider,
        tooltip = { Box(Modifier.animateTooltip(transition)) { scope.tooltip() } },
        focusable = focusable,
        enableUserInput = enableUserInput,
        state = state,
        modifier = modifier,
        content = wrappedContent
    )
}

/**
 * Tooltip scope for [TooltipBox] to be used to obtain the [LayoutCoordinates] of the anchor
 * content, and to draw a caret for the tooltip.
 */
@ExperimentalMaterial3Api
sealed interface TooltipScope {
    /**
     * [Modifier] that is used to draw the caret for the tooltip. A [LayoutCoordinates] will be
     * provided that can be used to obtain the bounds of the anchor content, which can be used to
     * draw the caret more precisely. [PlainTooltip] and [RichTooltip] have default implementations
     * for their caret.
     */
    fun Modifier.drawCaret(draw: CacheDrawScope.(LayoutCoordinates?) -> DrawResult): Modifier
}

@OptIn(ExperimentalMaterial3Api::class)
internal class TooltipScopeImpl(val getAnchorBounds: () -> LayoutCoordinates?) : TooltipScope {
    override fun Modifier.drawCaret(
        draw: CacheDrawScope.(LayoutCoordinates?) -> DrawResult
    ): Modifier = this.drawWithCache { draw(getAnchorBounds()) }
}

/**
 * Plain tooltip that provides a descriptive message.
 *
 * Usually used with [TooltipBox].
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param caretSize [DpSize] for the caret of the tooltip, if a default caret is desired with a
 *   specific dimension. Please see [TooltipDefaults.caretSize] to see the default dimensions. Pass
 *   in Dp.Unspecified for this parameter if no caret is desired.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param contentColor [Color] that will be applied to the tooltip's content.
 * @param containerColor [Color] that will be applied to the tooltip's container.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param content the composable that will be used to populate the tooltip's content.
 */
@Suppress("DEPRECATION")
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Maintained for binary compatibility. " + "Use overload with maxWidth parameter."
)
@Composable
@ExperimentalMaterial3Api
expect fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    caretSize: DpSize = DpSize.Unspecified,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
)

/**
 * Plain tooltip that provides a descriptive message.
 *
 * Usually used with [TooltipBox].
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param caretSize [DpSize] for the caret of the tooltip, if a default caret is desired with a
 *   specific dimension. Please see [TooltipDefaults.caretSize] to see the default dimensions. Pass
 *   in Dp.Unspecified for this parameter if no caret is desired.
 * @param maxWidth the maximum width for the plain tooltip
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param contentColor [Color] that will be applied to the tooltip's content.
 * @param containerColor [Color] that will be applied to the tooltip's container.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param content the composable that will be used to populate the tooltip's content.
 */
@Composable
@ExperimentalMaterial3Api
expect fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    caretSize: DpSize = DpSize.Unspecified,
    maxWidth: Dp = TooltipDefaults.plainTooltipMaxWidth,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
)

/**
 * Rich text tooltip that allows the user to pass in a title, text, and action. Tooltips are used to
 * provide a descriptive message.
 *
 * Usually used with [TooltipBox]
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param title An optional title for the tooltip.
 * @param action An optional action for the tooltip.
 * @param caretSize [DpSize] for the caret of the tooltip, if a default caret is desired with a
 *   specific dimension. Please see [TooltipDefaults.caretSize] to see the default dimensions. Pass
 *   in Dp.Unspecified for this parameter if no caret is desired.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param colors [RichTooltipColors] that will be applied to the tooltip's container and content.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param text the composable that will be used to populate the rich tooltip's text.
 */
@Suppress("DEPRECATION")
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Maintained for binary compatibility. " + "Use overload with maxWidth parameter."
)
@Composable
@ExperimentalMaterial3Api
expect fun TooltipScope.RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretSize: DpSize = DpSize.Unspecified,
    shape: Shape = TooltipDefaults.richTooltipContainerShape,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    tonalElevation: Dp = ElevationTokens.Level0,
    shadowElevation: Dp = RichTooltipTokens.ContainerElevation,
    text: @Composable () -> Unit
)

/**
 * Rich text tooltip that allows the user to pass in a title, text, and action. Tooltips are used to
 * provide a descriptive message.
 *
 * Usually used with [TooltipBox]
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param title An optional title for the tooltip.
 * @param action An optional action for the tooltip.
 * @param caretSize [DpSize] for the caret of the tooltip, if a default caret is desired with a
 *   specific dimension. Please see [TooltipDefaults.caretSize] to see the default dimensions. Pass
 *   in Dp.Unspecified for this parameter if no caret is desired.
 * @param maxWidth the maximum width for the rich tooltip
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param colors [RichTooltipColors] that will be applied to the tooltip's container and content.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param text the composable that will be used to populate the rich tooltip's text.
 */
@Composable
@ExperimentalMaterial3Api
expect fun TooltipScope.RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretSize: DpSize = DpSize.Unspecified,
    maxWidth: Dp = TooltipDefaults.richTooltipMaxWidth,
    shape: Shape = TooltipDefaults.richTooltipContainerShape,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    tonalElevation: Dp = ElevationTokens.Level0,
    shadowElevation: Dp = RichTooltipTokens.ContainerElevation,
    text: @Composable () -> Unit
)

/** Tooltip defaults that contain default values for both [PlainTooltip] and [RichTooltip] */
@ExperimentalMaterial3Api
object TooltipDefaults {
    /** The default [Shape] for a [PlainTooltip]'s container. */
    val plainTooltipContainerShape: Shape
        @Composable get() = PlainTooltipTokens.ContainerShape.value

    /** The default [Color] for a [PlainTooltip]'s container. */
    val plainTooltipContainerColor: Color
        @Composable get() = PlainTooltipTokens.ContainerColor.value

    /** The default [Color] for the content within the [PlainTooltip]. */
    val plainTooltipContentColor: Color
        @Composable get() = PlainTooltipTokens.SupportingTextColor.value

    /** The default [Shape] for a [RichTooltip]'s container. */
    val richTooltipContainerShape: Shape
        @Composable get() = RichTooltipTokens.ContainerShape.value

    /** The default [DpSize] for tooltip carets. */
    val caretSize: DpSize = DpSize(16.dp, 8.dp)

    /** The default maximum width for plain tooltips. */
    val plainTooltipMaxWidth: Dp = 200.dp

    /** The default maximum width for rich tooltips. */
    val richTooltipMaxWidth: Dp = 320.dp

    /**
     * Method to create a [RichTooltipColors] for [RichTooltip] using [RichTooltipTokens] to obtain
     * the default colors.
     */
    @Composable fun richTooltipColors() = MaterialTheme.colorScheme.defaultRichTooltipColors

    /**
     * Method to create a [RichTooltipColors] for [RichTooltip] using [RichTooltipTokens] to obtain
     * the default colors.
     */
    @Composable
    fun richTooltipColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        actionContentColor: Color = Color.Unspecified,
    ): RichTooltipColors =
        MaterialTheme.colorScheme.defaultRichTooltipColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            titleContentColor = titleContentColor,
            actionContentColor = actionContentColor
        )

    internal val ColorScheme.defaultRichTooltipColors: RichTooltipColors
        get() {
            return defaultRichTooltipColorsCached
                ?: RichTooltipColors(
                        containerColor = fromToken(RichTooltipTokens.ContainerColor),
                        contentColor = fromToken(RichTooltipTokens.SupportingTextColor),
                        titleContentColor = fromToken(RichTooltipTokens.SubheadColor),
                        actionContentColor = fromToken(RichTooltipTokens.ActionLabelTextColor),
                    )
                    .also { defaultRichTooltipColorsCached = it }
        }

    /**
     * [PopupPositionProvider] that should be used with [PlainTooltip]. It correctly positions the
     * tooltip in respect to the anchor content.
     *
     * @param spacingBetweenTooltipAndAnchor the spacing between the tooltip and the anchor content.
     */
    @Deprecated(
        "Deprecated in favor of rememberTooltipPositionProvider API.",
        replaceWith =
            ReplaceWith("rememberTooltipPositionProvider(spacingBetweenTooltipAndAnchor)"),
        level = DeprecationLevel.HIDDEN
    )
    @Composable
    fun rememberPlainTooltipPositionProvider(
        spacingBetweenTooltipAndAnchor: Dp = SpacingBetweenTooltipAndAnchor
    ): PopupPositionProvider {
        val tooltipAnchorSpacing =
            with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
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
                    if (y < 0) y = anchorBounds.bottom + tooltipAnchorSpacing
                    return IntOffset(x, y)
                }
            }
        }
    }

    /**
     * [PopupPositionProvider] that should be used with [RichTooltip]. It correctly positions the
     * tooltip in respect to the anchor content.
     *
     * @param spacingBetweenTooltipAndAnchor the spacing between the tooltip and the anchor content.
     */
    @Deprecated(
        "Deprecated in favor of rememberTooltipPositionProvider API.",
        replaceWith =
            ReplaceWith("rememberTooltipPositionProvider(spacingBetweenTooltipAndAnchor)"),
        level = DeprecationLevel.HIDDEN
    )
    @Composable
    fun rememberRichTooltipPositionProvider(
        spacingBetweenTooltipAndAnchor: Dp = SpacingBetweenTooltipAndAnchor
    ): PopupPositionProvider {
        val tooltipAnchorSpacing =
            with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
        return remember(tooltipAnchorSpacing) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    var x = anchorBounds.left
                    // Try to shift it to the left of the anchor
                    // if the tooltip would collide with the right side of the screen
                    if (x + popupContentSize.width > windowSize.width) {
                        x = anchorBounds.right - popupContentSize.width
                        // Center if it'll also collide with the left side of the screen
                        if (x < 0)
                            x =
                                anchorBounds.left +
                                    (anchorBounds.width - popupContentSize.width) / 2
                    }

                    // Tooltip prefers to be above the anchor,
                    // but if this causes the tooltip to overlap with the anchor
                    // then we place it below the anchor
                    var y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
                    if (y < 0) y = anchorBounds.bottom + tooltipAnchorSpacing
                    return IntOffset(x, y)
                }
            }
        }
    }

    /**
     * [PopupPositionProvider] that should be used with either [RichTooltip] or [PlainTooltip]. It
     * correctly positions the tooltip in respect to the anchor content.
     *
     * @param spacingBetweenTooltipAndAnchor the spacing between the tooltip and the anchor content.
     */
    @Composable
    fun rememberTooltipPositionProvider(
        spacingBetweenTooltipAndAnchor: Dp = SpacingBetweenTooltipAndAnchor
    ): PopupPositionProvider {
        val tooltipAnchorSpacing =
            with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
        return remember(tooltipAnchorSpacing) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    // Horizontal alignment preference: middle -> start -> end
                    // Vertical preference: above -> below

                    // Tooltip prefers to be center aligned horizontally.
                    var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2

                    if (x < 0) {
                        // Make tooltip start aligned if colliding with the
                        // left side of the screen
                        x = anchorBounds.left
                    } else if (x + popupContentSize.width > windowSize.width) {
                        // Make tooltip end aligned if colliding with the
                        // right side of the screen
                        x = anchorBounds.right - popupContentSize.width
                    }

                    // Tooltip prefers to be above the anchor,
                    // but if this causes the tooltip to overlap with the anchor
                    // then we place it below the anchor
                    var y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
                    if (y < 0) y = anchorBounds.bottom + tooltipAnchorSpacing
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
    /**
     * Returns a copy of this RichTooltipColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        titleContentColor: Color = this.titleContentColor,
        actionContentColor: Color = this.actionContentColor,
    ) =
        RichTooltipColors(
            containerColor.takeOrElse { this.containerColor },
            contentColor.takeOrElse { this.contentColor },
            titleContentColor.takeOrElse { this.titleContentColor },
            actionContentColor.takeOrElse { this.actionContentColor },
        )

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
 * @param isPersistent [Boolean] that determines if the tooltip associated with this will be
 *   persistent or not. If isPersistent is true, then the tooltip will only be dismissed when the
 *   user clicks outside the bounds of the tooltip or if [TooltipState.dismiss] is called. When
 *   isPersistent is false, the tooltip will dismiss after a short duration. Ideally, this should be
 *   set to true when there is actionable content being displayed within a tooltip.
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated with
 *   the mutator mutex, only one will be shown on the screen at any time.
 */
@Composable
@ExperimentalMaterial3Api
fun rememberTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = false,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex
): TooltipState =
    remember(isPersistent, mutatorMutex) {
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
 * @param isPersistent [Boolean] that determines if the tooltip associated with this will be
 *   persistent or not. If isPersistent is true, then the tooltip will only be dismissed when the
 *   user clicks outside the bounds of the tooltip or if [TooltipState.dismiss] is called. When
 *   isPersistent is false, the tooltip will dismiss after a short duration. Ideally, this should be
 *   set to true when there is actionable content being displayed within a tooltip.
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated with
 *   the mutator mutex, only one will be shown on the screen at any time.
 */
@ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
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

    /** continuation used to clean up */
    private var job: (CancellableContinuation<Unit>)? = null

    /**
     * Show the tooltip associated with the current [TooltipState]. When this method is called, all
     * of the other tooltips associated with [mutatorMutex] will be dismissed.
     *
     * @param mutatePriority [MutatePriority] to be used with [mutatorMutex].
     */
    override suspend fun show(mutatePriority: MutatePriority) {
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
                    withTimeout(BasicTooltipDefaults.TooltipDuration) { cancellableShow() }
                }
            } finally {
                if (mutatePriority != MutatePriority.PreventUserInput) {
                    // timeout or cancellation has occurred and we close out the current tooltip.
                    dismiss()
                }
            }
        }
    }

    /** Dismiss the tooltip associated with this [TooltipState] if it's currently being shown. */
    override fun dismiss() {
        transition.targetState = false
    }

    /** Cleans up [mutatorMutex] when the tooltip associated with this state leaves Composition. */
    override fun onDispose() {
        job?.cancel()
    }
}

/**
 * The state that is associated with a [TooltipBox]. Each instance of [TooltipBox] should have its
 * own [TooltipState].
 */
@ExperimentalMaterial3Api
interface TooltipState {
    /**
     * The current transition state of the tooltip. Used to start the transition of the tooltip when
     * fading in and out.
     */
    val transition: MutableTransitionState<Boolean>

    /** [Boolean] that indicates if the tooltip is currently being shown or not. */
    val isVisible: Boolean

    /**
     * [Boolean] that determines if the tooltip associated with this will be persistent or not. If
     * isPersistent is true, then the tooltip will only be dismissed when the user clicks outside
     * the bounds of the tooltip or if [TooltipState.dismiss] is called. When isPersistent is false,
     * the tooltip will dismiss after a short duration. Ideally, this should be set to true when
     * there is actionable content being displayed within a tooltip.
     */
    val isPersistent: Boolean

    /**
     * Show the tooltip associated with the current [TooltipState]. When this method is called all
     * of the other tooltips currently being shown will dismiss.
     *
     * @param mutatePriority [MutatePriority] to be used.
     */
    suspend fun show(mutatePriority: MutatePriority = MutatePriority.Default)

    /** Dismiss the tooltip associated with this [TooltipState] if it's currently being shown. */
    fun dismiss()

    /** Clean up when the this state leaves Composition. */
    fun onDispose()
}

@Stable
internal fun Modifier.textVerticalPadding(subheadExists: Boolean, actionExists: Boolean): Modifier {
    return if (!subheadExists && !actionExists) {
        this.padding(vertical = PlainTooltipVerticalPadding)
    } else {
        this.paddingFromBaseline(top = HeightFromSubheadToTextFirstLine)
            .padding(bottom = TextBottomPadding)
    }
}

internal fun Modifier.animateTooltip(transition: Transition<Boolean>): Modifier =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "animateTooltip"
                properties["transition"] = transition
            }
    ) {
        // TODO Load the motionScheme tokens from the component tokens file
        val inOutScaleAnimationSpec = MotionSchemeKeyTokens.FastSpatial.value<Float>()
        val inOutAlphaAnimationSpec = MotionSchemeKeyTokens.FastEffects.value<Float>()
        val scale by
            transition.animateFloat(
                transitionSpec = { inOutScaleAnimationSpec },
                label = "tooltip transition: scaling"
            ) {
                if (it) 1f else 0.8f
            }

        val alpha by
            transition.animateFloat(
                transitionSpec = { inOutAlphaAnimationSpec },
                label = "tooltip transition: alpha"
            ) {
                if (it) 1f else 0f
            }

        this.graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
    }

internal val SpacingBetweenTooltipAndAnchor = 4.dp
internal val TooltipMinHeight = 24.dp
internal val TooltipMinWidth = 40.dp
private val PlainTooltipVerticalPadding = 4.dp
private val PlainTooltipHorizontalPadding = 8.dp
internal val PlainTooltipContentPadding =
    PaddingValues(PlainTooltipHorizontalPadding, PlainTooltipVerticalPadding)
internal val RichTooltipHorizontalPadding = 16.dp
internal val HeightToSubheadFirstLine = 28.dp
private val HeightFromSubheadToTextFirstLine = 24.dp
private val TextBottomPadding = 16.dp
internal val ActionLabelMinHeight = 36.dp
internal val ActionLabelBottomPadding = 8.dp
