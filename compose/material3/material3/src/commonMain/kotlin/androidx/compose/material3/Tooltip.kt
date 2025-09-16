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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.internal.BasicTooltipBox
import androidx.compose.material3.internal.BasicTooltipDefaults
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.PlainTooltipTokens
import androidx.compose.material3.tokens.RichTooltipTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.jvm.JvmInline
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
 * Material TooltipBox that wraps a composable with a tooltip.
 *
 * Tooltips provide a descriptive message for an anchor. It can be used to call the users attention
 * to the anchor.
 *
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
 *   mouse hover, and keyboard focus to trigger the tooltip through the state provided.
 * @param content the composable that the tooltip will anchor to.
 */
@Deprecated(
    "Deprecated in favor of TooltipBox API that contains onDismissRequest and hasAction params.",
    level = DeprecationLevel.HIDDEN,
)
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
) =
    TooltipBox(
        positionProvider = positionProvider,
        tooltip = tooltip,
        state = state,
        modifier = modifier,
        onDismissRequest = null,
        focusable = focusable,
        enableUserInput = enableUserInput,
        hasAction = false,
        content = content,
    )

/**
 * Material TooltipBox that wraps a composable with a tooltip.
 *
 * Tooltips provide a descriptive message for an anchor. It can be used to call the users attention
 * to the anchor.
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaret
 *
 * Plain tooltip with caret shown on long press which is placed below the anchor:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaretBelowAnchor
 *
 * Plain tooltip with caret shown on long press which is placed left of the anchor:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaretLeftOfAnchor
 *
 * Plain tooltip with caret shown on long press which is placed right of the anchor:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaretRightOfAnchor
 *
 * Plain tooltip with caret shown on long press which is placed start of the anchor:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaretStartOfAnchor
 *
 * Plain tooltip with caret shown on long press which is placed end of the anchor:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaretEndOfAnchor
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
 * @param onDismissRequest executes when the user clicks outside of the tooltip. By default, the
 *   tooltip will dismiss when it's being shown when a user clicks outside of the tooltip.
 * @param focusable [Boolean] that determines if the tooltip is focusable. When true, the tooltip
 *   will consume touch events while it's shown and will have accessibility focus move to the first
 *   element of the component. When false, the tooltip won't consume touch events while it's shown
 *   but assistive-tech users will need to swipe or drag to get to the first element of the
 *   component.
 * @param enableUserInput [Boolean] which determines if this TooltipBox will handle long press and
 *   mouse hover to trigger the tooltip through the state provided.
 * @param content the composable that the tooltip will anchor to.
 */
@Deprecated(
    "Deprecated in favor of TooltipBox API that contains hasAction param.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@ExperimentalMaterial3Api
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    focusable: Boolean = true,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = positionProvider,
        tooltip = tooltip,
        state = state,
        modifier = modifier,
        onDismissRequest = null,
        focusable = focusable,
        enableUserInput = enableUserInput,
        hasAction = false,
        content = content,
    )
}

/**
 * Material TooltipBox that wraps a composable with a tooltip.
 *
 * Tooltips provide a descriptive message for an anchor. It can be used to call the users attention
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
 * Plain tooltip with caret shown on long press which is placed below the anchor:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaretBelowAnchor
 *
 * Plain tooltip with caret shown on long press which is placed left of the anchor:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaretLeftOfAnchor
 *
 * Plain tooltip with caret shown on long press which is placed right of the anchor:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaretRightOfAnchor
 *
 * Plain tooltip with caret shown on long press which is placed start of the anchor:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaretStartOfAnchor
 *
 * Plain tooltip with caret shown on long press which is placed end of the anchor:
 *
 * @sample androidx.compose.material3.samples.PlainTooltipWithCaretEndOfAnchor
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
 * @param onDismissRequest executes when the user clicks outside of the tooltip. By default, the
 *   tooltip will dismiss when it's being shown when a user clicks outside of the tooltip.
 * @param focusable [Boolean] that determines if the tooltip is focusable. When true, the tooltip
 *   will consume touch events while it's shown and will have accessibility focus move to the first
 *   element of the component. When false, the tooltip won't consume touch events while it's shown
 *   but assistive-tech users will need to swipe or drag to get to the first element of the
 *   component. For certain a11y cases, such as when the tooltip has an action and Talkback is on,
 *   focusable will be forced to true to allow for the correct a11y behavior.
 * @param enableUserInput [Boolean] which determines if this TooltipBox will handle long press and,
 *   mouse hover, and keyboard focus to trigger the tooltip through the state provided.
 * @param hasAction whether the associated tooltip contains an action.
 * @param content the composable that the tooltip will anchor to.
 */
@Composable
@ExperimentalMaterial3Api
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    hasAction: Boolean = false,
    content: @Composable () -> Unit,
) {
    @Suppress("DEPRECATION")
    val transition = updateTransition(state.transition, label = "tooltip transition")
    val anchorBounds: MutableState<LayoutCoordinates?> = remember { mutableStateOf(null) }
    val scope = remember { TooltipScopeImpl({ anchorBounds.value }, positionProvider) }

    val wrappedContent: @Composable () -> Unit = {
        Box(modifier = Modifier.onGloballyPositioned { anchorBounds.value = it }) { content() }
    }

    BasicTooltipBox(
        positionProvider = positionProvider,
        tooltip = { Box(Modifier.animateTooltip(transition)) { scope.tooltip() } },
        focusable = focusable,
        enableUserInput = enableUserInput,
        onDismissRequest = onDismissRequest,
        state = state,
        modifier = modifier,
        hasAction = hasAction,
        content = wrappedContent,
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
    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    fun Modifier.drawCaret(draw: CacheDrawScope.(LayoutCoordinates?) -> DrawResult): Modifier

    /**
     * Used to obtain the [LayoutCoordinates] of the anchor content. This can be used to help draw
     * the caret pointing to the anchor content.
     */
    fun MeasureScope.obtainAnchorBounds(): LayoutCoordinates?

    /**
     * Used to obtain the [PopupPositionProvider] used. This can be used to help draw the caret
     * pointing to the anchor content.
     */
    fun obtainPositionProvider(): PopupPositionProvider
}

@OptIn(ExperimentalMaterial3Api::class)
internal class TooltipScopeImpl(
    val getAnchorBounds: () -> LayoutCoordinates?,
    val positionProvider: PopupPositionProvider,
) : TooltipScope {
    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    override fun Modifier.drawCaret(
        draw: CacheDrawScope.(LayoutCoordinates?) -> DrawResult
    ): Modifier = this.drawWithCache { draw(getAnchorBounds()) }

    override fun MeasureScope.obtainAnchorBounds(): LayoutCoordinates? = getAnchorBounds()

    override fun obtainPositionProvider(): PopupPositionProvider = positionProvider
}

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
@ExperimentalMaterial3Api
fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    caretSize: DpSize = DpSize.Unspecified,
    maxWidth: Dp = TooltipDefaults.plainTooltipMaxWidth,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) =
    PlainTooltip(
        modifier,
        TooltipDefaults.caretShape(caretSize),
        maxWidth,
        shape,
        contentColor,
        containerColor,
        tonalElevation,
        shadowElevation,
        content,
    )

/**
 * Plain tooltip that provides a descriptive message.
 *
 * Usually used with [TooltipBox].
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param caretShape [shape] for the caret of the tooltip. If a default caret is desired with a
 *   specific dimension please use [TooltipDefaults.caretShape]. To see the default dimensions
 *   please see [TooltipDefaults.caretSize]. If no caret is desired, please pass in null.
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
fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    caretShape: (Shape)? = null,
    maxWidth: Dp = TooltipDefaults.plainTooltipMaxWidth,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val tooltipShape: Shape
    val tooltipModifier: Modifier
    if (caretShape != null) {
        val transformationMatrix = remember { mutableStateOf(Matrix()) }
        val density = LocalDensity.current
        val windowContainerSize = LocalWindowInfo.current.containerSize
        tooltipModifier =
            Modifier.layoutCaret(
                    transformationMatrix,
                    density,
                    windowContainerSize,
                    { obtainAnchorBounds() },
                    obtainPositionProvider(),
                )
                .then(modifier)
        tooltipShape =
            remember(shape, caretShape) {
                TooltipCaretShape(transformationMatrix, shape, caretShape)
            }
    } else {
        tooltipShape = shape
        tooltipModifier = modifier
    }

    Surface(
        modifier = tooltipModifier,
        shape = tooltipShape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Box(
            modifier =
                Modifier.sizeIn(
                        minWidth = TooltipMinWidth,
                        maxWidth = maxWidth,
                        minHeight = TooltipMinHeight,
                    )
                    .padding(PlainTooltipContentPadding)
        ) {
            val textStyle = PlainTooltipTokens.SupportingTextFont.value

            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides textStyle,
                content = content,
            )
        }
    }
}

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
@ExperimentalMaterial3Api
fun TooltipScope.RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretSize: DpSize = DpSize.Unspecified,
    maxWidth: Dp = TooltipDefaults.richTooltipMaxWidth,
    shape: Shape = TooltipDefaults.richTooltipContainerShape,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    tonalElevation: Dp = ElevationTokens.Level0,
    shadowElevation: Dp = RichTooltipTokens.ContainerElevation,
    text: @Composable () -> Unit,
) =
    RichTooltip(
        modifier,
        title,
        action,
        TooltipDefaults.caretShape(caretSize),
        maxWidth,
        shape,
        colors,
        tonalElevation,
        shadowElevation,
        text,
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
 * @param caretShape [shape] for the caret of the tooltip. If a default caret is desired with a
 *   specific dimension please use [TooltipDefaults.caretShape]. To see the default dimensions
 *   please see [TooltipDefaults.caretSize]. If no caret is desired, please pass in null.
 * @param maxWidth the maximum width for the plain tooltip
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param colors [RichTooltipColors] that will be applied to the tooltip's container and content.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param text the composable that will be used to populate the rich tooltip's text.
 */
@Composable
@ExperimentalMaterial3Api
fun TooltipScope.RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretShape: (Shape)? = null,
    maxWidth: Dp = TooltipDefaults.richTooltipMaxWidth,
    shape: Shape = TooltipDefaults.richTooltipContainerShape,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    tonalElevation: Dp = ElevationTokens.Level0,
    shadowElevation: Dp = RichTooltipTokens.ContainerElevation,
    text: @Composable () -> Unit,
) {
    val tooltipShape: Shape
    val tooltipModifier: Modifier
    if (caretShape != null) {
        val transformationMatrix = remember { mutableStateOf(Matrix()) }
        val density = LocalDensity.current
        val windowContainerSize = LocalWindowInfo.current.containerSize
        tooltipModifier =
            Modifier.layoutCaret(
                    transformationMatrix,
                    density,
                    windowContainerSize,
                    { obtainAnchorBounds() },
                    obtainPositionProvider(),
                )
                .then(modifier)
        tooltipShape =
            remember(shape, caretShape) {
                TooltipCaretShape(transformationMatrix, shape, caretShape)
            }
    } else {
        tooltipShape = shape
        tooltipModifier = modifier
    }

    Surface(
        modifier =
            tooltipModifier.sizeIn(
                minWidth = TooltipMinWidth,
                maxWidth = maxWidth,
                minHeight = TooltipMinHeight,
            ),
        shape = tooltipShape,
        color = colors.containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        val actionLabelTextStyle = RichTooltipTokens.ActionLabelTextFont.value
        val subheadTextStyle = RichTooltipTokens.SubheadFont.value
        val supportingTextStyle = RichTooltipTokens.SupportingTextFont.value

        Column(modifier = Modifier.padding(horizontal = RichTooltipHorizontalPadding)) {
            title?.let {
                Box(modifier = Modifier.paddingFromBaseline(top = HeightToSubheadFirstLine)) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.titleContentColor,
                        LocalTextStyle provides subheadTextStyle,
                        content = it,
                    )
                }
            }
            Box(modifier = Modifier.textVerticalPadding(title != null, action != null)) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.contentColor,
                    LocalTextStyle provides supportingTextStyle,
                    content = text,
                )
            }
            action?.let {
                Box(
                    modifier =
                        Modifier.requiredHeightIn(min = ActionLabelMinHeight)
                            .padding(bottom = ActionLabelBottomPadding)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.actionContentColor,
                        LocalTextStyle provides actionLabelTextStyle,
                        content = it,
                    )
                }
            }
        }
    }
}

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

    /** The default caret shape used for tooltips and is [TooltipDefaults.caretSize] dimensions. */
    fun caretShape() = DefaultCaretShape

    /**
     * The caret shape used for tooltips.
     *
     * @param caretSize [DpSize] used to draw the caret shape
     */
    fun caretShape(caretSize: DpSize = TooltipDefaults.caretSize): Shape =
        DefaultTooltipCaretShape(caretSize)

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
            actionContentColor = actionContentColor,
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
        level = DeprecationLevel.WARNING,
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
                    popupContentSize: IntSize,
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
        level = DeprecationLevel.WARNING,
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
                    popupContentSize: IntSize,
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
    @Deprecated(
        "Deprecated in favor of rememberTooltipPositionProvider API that " +
            "takes a preferred positioning. Please use rememberTooltipPositionProvider with " +
            "TooltipAnchorPosition.Above if this same behavior is desired.",
        replaceWith =
            ReplaceWith(
                "rememberTooltipPositionProvider(TooltipAnchorPosition.ABOVE, spacingBetweenTooltipAndAnchor)"
            ),
        level = DeprecationLevel.WARNING,
    )
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
                    popupContentSize: IntSize,
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

    /**
     * [PopupPositionProvider] that should be used with either [RichTooltip] or [PlainTooltip]. It
     * correctly positions the tooltip in respect to the anchor content.
     *
     * @param positioning [TooltipAnchorPosition] that determines where the tooltip is placed
     *   relative to the anchor.
     * @param spacingBetweenTooltipAndAnchor the spacing between the tooltip and the anchor content.
     */
    @Composable
    fun rememberTooltipPositionProvider(
        positioning: TooltipAnchorPosition,
        spacingBetweenTooltipAndAnchor: Dp = SpacingBetweenTooltipAndAnchor,
    ): PopupPositionProvider {
        val tooltipAnchorSpacing =
            with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
        return remember(tooltipAnchorSpacing, positioning) {
            TooltipPositionProviderImpl(positioning, tooltipAnchorSpacing)
        }
    }

    internal val DefaultCaretShape = DefaultTooltipCaretShape(caretSize)
}

@Stable
@Immutable
@ExperimentalMaterial3Api
class RichTooltipColors(
    val containerColor: Color,
    val contentColor: Color,
    val titleContentColor: Color,
    val actionContentColor: Color,
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

@JvmInline
@ExperimentalMaterial3Api
value class TooltipAnchorPosition private constructor(private val value: Int) {
    override fun toString(): String {
        return when (this) {
            Above -> "Above"
            Below -> "Below"
            Left -> "Left"
            Right -> "Right"
            Start -> "Start"
            End -> "End"
            else -> "Invalid"
        }
    }

    companion object {
        /** Places the tooltip above the anchor */
        val Above = TooltipAnchorPosition(1)

        /** Places the tooltip below the anchor */
        val Below = TooltipAnchorPosition(2)

        /** Places the tooltip on the left of the anchor */
        val Left = TooltipAnchorPosition(3)

        /** Places the tooltip on the right of the anchor */
        val Right = TooltipAnchorPosition(4)

        /** Places the tooltip at the start of the anchor */
        val Start = TooltipAnchorPosition(5)

        /** Places the tooltip at the end of the anchor */
        val End = TooltipAnchorPosition(6)
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
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex,
): TooltipState =
    remember(isPersistent, mutatorMutex) {
        TooltipStateImpl(
            initialIsVisible = initialIsVisible,
            isPersistent = isPersistent,
            mutatorMutex = mutatorMutex,
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
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex,
): TooltipState =
    TooltipStateImpl(
        initialIsVisible = initialIsVisible,
        isPersistent = isPersistent,
        mutatorMutex = mutatorMutex,
    )

@OptIn(ExperimentalMaterial3Api::class)
private class TooltipPositionProviderImpl(
    val type: TooltipAnchorPosition,
    val tooltipAnchorSpacing: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        return when (type) {
            TooltipAnchorPosition.Left -> leftPositioning(anchorBounds, popupContentSize)
            TooltipAnchorPosition.Right ->
                rightPositioning(anchorBounds, popupContentSize, windowSize)
            TooltipAnchorPosition.Above ->
                abovePositioning(anchorBounds, popupContentSize, windowSize)
            TooltipAnchorPosition.Below ->
                belowPositioning(anchorBounds, popupContentSize, windowSize)
            TooltipAnchorPosition.Start ->
                startPositioning(layoutDirection, anchorBounds, popupContentSize, windowSize)
            TooltipAnchorPosition.End ->
                endPositioning(layoutDirection, anchorBounds, popupContentSize, windowSize)
            else -> abovePositioning(anchorBounds, popupContentSize, windowSize)
        }
    }

    fun leftPositioning(anchorBounds: IntRect, popupContentSize: IntSize): IntOffset {
        // Horizontal alignment preference: left -> right
        // Vertical preference: center

        // Tooltip prefers to be to the left of the anchor
        var x = anchorBounds.left - (popupContentSize.width + tooltipAnchorSpacing)

        if (x < 0) {
            // Flip the tooltip to be on the right if
            // it collides with the left side of the screen
            x = anchorBounds.right + tooltipAnchorSpacing
        }

        // We vertically center the tooltip with the anchor
        var y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }

    fun rightPositioning(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        // Horizontal alignment preference: right -> left
        // Vertical preference: center

        // Tooltip prefers to be to the right of the anchor
        var x = anchorBounds.right + tooltipAnchorSpacing

        if (x + popupContentSize.width > windowSize.width) {
            // Flip the tooltip to be on the left if
            // it collides with the right side of the screen
            x = anchorBounds.left - (popupContentSize.width + tooltipAnchorSpacing)
        }

        // We vertically center the tooltip with the anchor
        var y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }

    fun abovePositioning(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
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

    fun belowPositioning(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        // Horizontal alignment preference: middle -> start -> end
        // Vertical preference: below -> above

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

        // Tooltip prefers to be below the anchor,
        // but if this causes the tooltip to overlap with the anchor
        // then we place it above the anchor
        var y = anchorBounds.bottom + tooltipAnchorSpacing
        if (y + popupContentSize.height > windowSize.height) {
            y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
        }
        return IntOffset(x, y)
    }

    fun startPositioning(
        layoutDirection: LayoutDirection,
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        return if (layoutDirection == LayoutDirection.Ltr) {
            leftPositioning(anchorBounds, popupContentSize)
        } else {
            rightPositioning(anchorBounds, popupContentSize, windowSize)
        }
    }

    fun endPositioning(
        layoutDirection: LayoutDirection,
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        return if (layoutDirection == LayoutDirection.Ltr) {
            rightPositioning(anchorBounds, popupContentSize, windowSize)
        } else {
            leftPositioning(anchorBounds, popupContentSize)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Stable
private class TooltipStateImpl(
    initialIsVisible: Boolean,
    override val isPersistent: Boolean,
    private val mutatorMutex: MutatorMutex,
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
                if (isPersistent || mutatePriority == MutatePriority.UserInput) {
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
        if (isPersistent) {
            job?.cancel()
        }
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
                label = "tooltip transition: scaling",
            ) {
                if (it) 1f else 0.8f
            }

        val alpha by
            transition.animateFloat(
                transitionSpec = { inOutAlphaAnimationSpec },
                label = "tooltip transition: alpha",
            ) {
                if (it) 1f else 0f
            }

        this.graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
    }

internal fun caretX(tooltipWidth: Float, screenWidthPx: Int, anchorBounds: Rect): Float {
    val anchorLeft = anchorBounds.left
    val anchorRight = anchorBounds.right
    val anchorMid = (anchorLeft + anchorRight) / 2
    return if (tooltipWidth >= screenWidthPx) {
        // Tooltip is greater than or equal to the width of the screen
        // The horizontal placement just needs to be in the center of the anchor
        anchorMid
    } else if (anchorMid - tooltipWidth / 2 < 0) {
        // The tooltip needs to be start aligned if it would
        // collide with the left side of screen when attempting to center.
        // We have a horizontal correction for the caret if the tooltip will
        // also collide with the right edge of the screen when start aligned
        val horizontalCorrection = maxOf(tooltipWidth - screenWidthPx, -anchorLeft)
        anchorMid + horizontalCorrection
    } else if (anchorMid + tooltipWidth / 2 > screenWidthPx) {
        // The tooltip needs to be end aligned if it would
        // collide with the right side of the screen when attempting to center.
        // We have a horizontal correction for the caret if the tooltip will
        // also collide with the left edge of the screen when end aligned
        val horizontalCorrection = minOf(tooltipWidth - anchorRight, 0f)
        anchorMid + horizontalCorrection
    } else {
        // Tooltip can centered neatly without colliding with screen edge
        tooltipWidth / 2
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.layoutCaret(
    transformationMatrix: MutableState<Matrix>,
    density: Density,
    windowContainerSize: IntSize,
    getAnchorLayoutCoordinates: MeasureScope.() -> LayoutCoordinates?,
    positionProvider: PopupPositionProvider,
): Modifier =
    this.layout { measurables, constraints ->
        val placeable = measurables.measure(constraints)
        val width = placeable.width
        val height = placeable.height
        val windowContainerWidthInPx = windowContainerSize.width
        val windowContainerHeightInPx = windowContainerSize.height
        val tooltipWidth = width.toFloat()
        val tooltipHeight = height.toFloat()
        val anchorLayoutCoordinates = getAnchorLayoutCoordinates()

        if (anchorLayoutCoordinates != null) {
            val screenWidthPx: Int
            val tooltipAnchorSpacing: Int
            with(density) {
                screenWidthPx = windowContainerWidthInPx
                tooltipAnchorSpacing = SpacingBetweenTooltipAndAnchor.roundToPx()
            }
            val anchorBounds = anchorLayoutCoordinates.boundsInWindow()
            val anchorTop = anchorBounds.top
            val anchorBottom = anchorBounds.bottom
            val anchorRight = anchorBounds.right
            val anchorLeft = anchorBounds.left
            val tooltipWidth: Float = tooltipWidth
            val tooltipHeight: Float = tooltipHeight
            val caretY =
                if (positionProvider is TooltipPositionProviderImpl) {
                    when (positionProvider.type) {
                        TooltipAnchorPosition.Left,
                        TooltipAnchorPosition.Right,
                        TooltipAnchorPosition.Start,
                        TooltipAnchorPosition.End -> {
                            tooltipHeight / 2
                        }
                        TooltipAnchorPosition.Above -> {
                            if (anchorTop - tooltipHeight - tooltipAnchorSpacing < 0) {
                                0f
                            } else {
                                tooltipHeight
                            }
                        }
                        TooltipAnchorPosition.Below -> {
                            if (
                                anchorBottom + tooltipHeight + tooltipAnchorSpacing >
                                    windowContainerHeightInPx
                            ) {
                                tooltipHeight
                            } else {
                                0f
                            }
                        }
                        else -> {
                            if (anchorTop - tooltipHeight - tooltipAnchorSpacing < 0) {
                                0f
                            } else {
                                tooltipHeight
                            }
                        }
                    }
                } else {
                    // If a custom position provider is given
                    // we treat it like AbovePositionProvider.
                    if (anchorTop - tooltipHeight - tooltipAnchorSpacing < 0) {
                        0f
                    } else {
                        tooltipHeight
                    }
                }

            val position =
                if (positionProvider is TooltipPositionProviderImpl) {
                    when (positionProvider.type) {
                        TooltipAnchorPosition.Left -> {
                            val caretX =
                                if (anchorLeft - tooltipAnchorSpacing - tooltipWidth < 0) {
                                    // We are placing the tooltip to the right of the anchor
                                    0f
                                } else {
                                    tooltipWidth
                                }
                            Offset(x = caretX, y = caretY)
                        }
                        TooltipAnchorPosition.Right -> {
                            val caretX =
                                if (
                                    anchorRight + tooltipAnchorSpacing + tooltipWidth >
                                        windowContainerWidthInPx
                                ) {
                                    // We are placing the tooltip to the left of the anchor
                                    tooltipWidth
                                } else {
                                    0f
                                }
                            Offset(x = caretX, y = caretY)
                        }
                        TooltipAnchorPosition.Start -> {
                            val caretX =
                                if (layoutDirection == LayoutDirection.Ltr) {
                                    if (anchorLeft - tooltipAnchorSpacing - tooltipWidth < 0) {
                                        // We are placing the tooltip to the right of the anchor
                                        0f
                                    } else {
                                        tooltipWidth
                                    }
                                } else {
                                    if (
                                        anchorRight + tooltipAnchorSpacing + tooltipWidth >
                                            windowContainerWidthInPx
                                    ) {
                                        // We are placing the tooltip to the left of the anchor
                                        tooltipWidth
                                    } else {
                                        0f
                                    }
                                }
                            Offset(x = caretX, y = caretY)
                        }
                        TooltipAnchorPosition.End -> {
                            val caretX =
                                if (layoutDirection == LayoutDirection.Ltr) {
                                    if (
                                        anchorRight + tooltipAnchorSpacing + tooltipWidth >
                                            windowContainerWidthInPx
                                    ) {
                                        // We are placing the tooltip to the left of the anchor
                                        tooltipWidth
                                    } else {
                                        0f
                                    }
                                } else {
                                    if (anchorLeft - tooltipAnchorSpacing - tooltipWidth < 0) {
                                        // We are placing the tooltip to the right of the anchor
                                        0f
                                    } else {
                                        tooltipWidth
                                    }
                                }
                            Offset(x = caretX, y = caretY)
                        }
                        else -> {
                            Offset(
                                x = caretX(tooltipWidth, screenWidthPx, anchorBounds),
                                y = caretY,
                            )
                        }
                    }
                } else {
                    Offset(x = caretX(tooltipWidth, screenWidthPx, anchorBounds), y = caretY)
                }

            // Translate matrix to position
            val matrix = Matrix()
            matrix.translate(x = position.x, y = position.y)

            // We rotate matrix depending on positioning of the tooltip
            if (positionProvider is TooltipPositionProviderImpl) {
                when (positionProvider.type) {
                    TooltipAnchorPosition.Left -> {
                        // Need to rotate it about the z axis by 90 degrees
                        if (anchorLeft - tooltipAnchorSpacing - tooltipWidth < 0) {
                            // Tooltip is being placed to the right of the anchor
                            matrix.rotateZ(90f)
                        } else {
                            matrix.rotateZ(-90f)
                        }
                    }
                    TooltipAnchorPosition.Right -> {
                        // Need to rotate it about the z axis by 90 degrees
                        if (
                            anchorRight + tooltipAnchorSpacing + tooltipWidth >
                                windowContainerWidthInPx
                        ) {
                            // Tooltip is being placed to the left of the anchor
                            matrix.rotateZ(-90f)
                        } else {
                            matrix.rotateZ(90f)
                        }
                    }
                    TooltipAnchorPosition.Start -> {
                        if (layoutDirection == LayoutDirection.Ltr) {
                            // Need to rotate it about the z axis by 90 degrees
                            if (anchorLeft - tooltipAnchorSpacing - tooltipWidth < 0) {
                                // Tooltip is being placed to the right of the anchor
                                matrix.rotateZ(90f)
                            } else {
                                matrix.rotateZ(-90f)
                            }
                        } else {
                            // Need to rotate it about the z axis by 90 degrees
                            if (
                                anchorRight + tooltipAnchorSpacing + tooltipWidth >
                                    windowContainerWidthInPx
                            ) {
                                // Tooltip is being placed to the left of the anchor
                                matrix.rotateZ(-90f)
                            } else {
                                matrix.rotateZ(90f)
                            }
                        }
                    }
                    TooltipAnchorPosition.End -> {
                        if (layoutDirection == LayoutDirection.Ltr) {
                            // Need to rotate it about the z axis by 90 degrees
                            if (
                                anchorRight + tooltipAnchorSpacing + tooltipWidth >
                                    windowContainerWidthInPx
                            ) {
                                // Tooltip is being placed to the left of the anchor
                                matrix.rotateZ(-90f)
                            } else {
                                matrix.rotateZ(90f)
                            }
                        } else {
                            // Need to rotate it about the z axis by 90 degrees
                            if (anchorLeft - tooltipAnchorSpacing - tooltipWidth < 0) {
                                // Tooltip is being placed to the right of the anchor
                                matrix.rotateZ(90f)
                            } else {
                                matrix.rotateZ(-90f)
                            }
                        }
                    }
                    else -> {
                        if (caretY == 0f) {
                            // caret needs to be placed above tooltip
                            // Need to rotate it about the x axis by 180 degrees
                            matrix.rotateX(180f)
                        }
                    }
                }
            } else {
                if (caretY == 0f) {
                    // caret needs to be placed above tooltip
                    // Need to rotate it about the x axis by 180 degrees
                    matrix.rotateX(180f)
                }
            }
            transformationMatrix.value = matrix
        }
        layout(width, height) { placeable.place(0, 0) }
    }

@ExperimentalMaterial3Api
/**
 * Default [Shape] of the caret used by tooltips.
 *
 * @param caretSize the size of the caret used
 */
class DefaultTooltipCaretShape(val caretSize: DpSize = TooltipDefaults.caretSize) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val caretPath = Path()
        val caretWidthPx: Float
        val caretHeightPx: Float
        with(density) {
            caretWidthPx = caretSize.width.toPx()
            caretHeightPx = caretSize.height.toPx()
        }

        caretPath.apply {
            moveTo(x = 0f, 0f)
            lineTo(x = caretWidthPx / 2, y = 0f)
            lineTo(x = 0f, y = caretHeightPx)
            lineTo(x = -caretWidthPx / 2, y = 0f)
            close()
        }

        return Outline.Generic(caretPath)
    }
}

private class TooltipCaretShape(
    private val transformationMatrix: MutableState<Matrix>,
    private val tooltipShape: Shape,
    private val caretShape: Shape,
) : Shape {
    val tooltipPath = Path()
    val combinedPath = Path()
    val caretPath = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        tooltipPath.reset()
        combinedPath.reset()
        caretPath.reset()

        val tooltipOutline = tooltipShape.createOutline(size, layoutDirection, density)
        val caretOutline = caretShape.createOutline(size, layoutDirection, density)

        when (tooltipOutline) {
            is Outline.Generic -> tooltipPath.addPath(tooltipOutline.path)
            is Outline.Rounded -> tooltipPath.addRoundRect(tooltipOutline.roundRect)
            is Outline.Rectangle -> tooltipPath.addRect(tooltipOutline.rect)
        }

        // Applies the given caret shape to the caret path that will be manipulated
        when (caretOutline) {
            is Outline.Generic -> caretPath.addPath(caretOutline.path)
            is Outline.Rounded -> caretPath.addRoundRect(caretOutline.roundRect)
            is Outline.Rectangle -> caretPath.addRect(caretOutline.rect)
        }

        caretPath.transform(transformationMatrix.value)

        combinedPath.op(path1 = tooltipPath, path2 = caretPath, operation = PathOperation.Union)

        return Outline.Generic(combinedPath)
    }
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
