/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.wear.compose.material

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.pow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

/**
 * A state object that can be used to control placeholders. Placeholders are used when the content
 * that needs to be displayed in a component is not yet available, e.g. it is loading
 * asynchronously.
 *
 * A [PlaceholderState] should be created for each component that has placeholder data. The
 * state is used to coordinate all of the different placeholder effects and animations.
 *
 * Placeholder has a number of different effects designed to work together.
 * [Modifier.placeholder] draws a placeholder shape on top of content that is waiting to load. There
 * can be multiple placeholders in a component.
 * [Modifier.placeholderShimmer] does a shimmer animation over the whole component that includes the
 * placeholders. There should only be one placeholderShimmer for each component.
 *
 * NOTE: The order of modifiers is important. If you are adding both [Modifier.placeholder] and
 * [Modifier.placeholderShimmer] to the same composable then the shimmer must be before in the
 * modifier chain. Example of [Text] composable with both placeholderShimmer and placeholder
 * modifiers.
 * @sample androidx.wear.compose.material.samples.TextPlaceholder
 *
 * Background placeholder effects are used to mask the background of components like chips and cards
 * until all of the data has loaded. Use [PlaceholderDefaults.placeholderChipColors]
 * [PlaceholderDefaults.placeholderBackgroundBrush] and
 * [PlaceholderDefaults.painterWithPlaceholderOverlayBackgroundBrush] to draw the component
 * background.
 *
 * Once all of the components content is loaded the shimmer will stop and a wipe off animation will
 * remove the placeholders.
 */
@ExperimentalWearMaterialApi
@Stable
public class PlaceholderState internal constructor(
    private val isContentReady: State<() -> Boolean>,
    private val maxScreenDimension: Float,
) {

    /**
     * The offset to apply for any background placeholder animations. This is the global offset of
     * the component which is having its background painted with
     * [PlaceholderDefaults.painterWithPlaceholderOverlayBackgroundBrush],
     * [PlaceholderDefaults.placeholderBackgroundBrush] or
     * [PlaceholderDefaults.placeholderChipColors].
     *
     * The offset values should be retrieved with [OnGloballyPositionedModifier].
     *
     * The offset is used to coordinate placeholder effects such as wipe-off between the difference
     * placeholder layers.
     */
    internal var backgroundOffset: Offset = Offset.Zero

    /**
     * Start the animation of the placeholder state.
     */
    public suspend fun startPlaceholderAnimation() {
        coroutineScope {
            while (isActive) {
                withInfiniteAnimationFrameMillis {
                    frameMillis.longValue = it
                }
            }
        }
    }

    /**
     * The current value of the placeholder wipe-off visual effect gradient progression. The
     * progression is a 45 degree angle sweep across the whole screen running from outside of the
     * Top|Left of the screen to Bottom|Right used as the anchor for wipe-off gradient effects.
     *
     * The progression represents the x and y coordinates in pixels of the Top|Left part of the
     * gradient that flows across the screen. The progression will start at -maxScreenDimension (max
     * of height/width to create a 45 degree angle) * 1.75f and progress to the
     * maximumScreenDimension * 0.75f.
     *
     * The time taken for this progression to reach the edge of visible screen is
     * [PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS]
     */
    internal val placeholderWipeOffProgression: Float by derivedStateOf {
        val absoluteProgression = ((frameMillis.longValue - startOfWipeOffAnimation).coerceAtMost(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS).toFloat() /
            PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS).coerceAtMost(1f)
        val easedProgression = wipeOffInterpolator.transform(absoluteProgression)
        lerp(-maxScreenDimension * 1.75f, maxScreenDimension * 0.75f, easedProgression)
    }

    /**
     * The current value of the placeholder wipe off visual effect gradient progression alpha. The
     * progression is a 45 degree angle sweep across the whole screen running from outside of the
     * Top|Left of the screen to Bottom|Right used as the anchor for wipe-off gradient effects.
     *
     * The progression represents the x and y coordinates in pixels of the Top|Left part of the
     * gradient that flows across the screen. The progression will start at -maxScreenDimension (max
     * of height/width to create a 45 degree angle) and progress to the
     * maximumScreenDimension.
     *
     * The time taken for this progression is [PLACEHOLDER_WIPE_OFF_PROGRESSION_ALPHA_DURATION_MS]
     */
    @ExperimentalWearMaterialApi
    internal val placeholderWipeOffAlpha: Float by derivedStateOf {
        val absoluteProgression = ((frameMillis.longValue - startOfWipeOffAnimation).coerceAtMost(
            PLACEHOLDER_WIPE_OFF_PROGRESSION_ALPHA_DURATION_MS).toFloat() /
            PLACEHOLDER_WIPE_OFF_PROGRESSION_ALPHA_DURATION_MS).coerceAtMost(1f)

        val alpha =
            lerp(0f, 1f, absoluteProgression)
        wipeOffInterpolator.transform(alpha)
    }

    /**
     * The current value of the placeholder visual effect gradient progression. The progression
     * gives the x coordinate to be applied to the placeholder gradient as it moves across the
     * screen. Starting off screen to the left and progressing across the screen and finishing off
     * the screen to the right after [PLACEHOLDER_SHIMMER_DURATION_MS].
     */
    @ExperimentalWearMaterialApi
    public val placeholderProgression: Float by derivedStateOf {
        val absoluteProgression =
            (frameMillis.longValue.mod(PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS)
                .coerceAtMost(PLACEHOLDER_SHIMMER_DURATION_MS).toFloat() /
                PLACEHOLDER_SHIMMER_DURATION_MS)
        val easedProgression = progressionInterpolator.transform(absoluteProgression)
        lerp(-maxScreenDimension * 0.5f, maxScreenDimension * 1.5f, easedProgression)
    }

    /**
     * The current value of the placeholder visual effect gradient progression alpha/opacity. The
     * progression gives the alpha to apply during the period of the placeholder effect. This allows
     * the effect to be faded in and then out during the [PLACEHOLDER_SHIMMER_DURATION_MS].
     */
    @ExperimentalWearMaterialApi
    internal val placeholderShimmerAlpha: Float by derivedStateOf {
        val absoluteProgression =
            (frameMillis.longValue.mod(PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS)
                .coerceAtMost(PLACEHOLDER_SHIMMER_DURATION_MS).toFloat() /
                PLACEHOLDER_SHIMMER_DURATION_MS)

        if (absoluteProgression <= 0.5f) {
            val alpha =
                lerp(0f, 0.15f, absoluteProgression * 2f)
            progressionInterpolator.transform(alpha)
        } else {
            val alpha =
                lerp(0.15f, 0f, (absoluteProgression - 0.5f) * 2f)
            progressionInterpolator.transform(alpha)
        }
    }

    /**
     * Returns true if the placeholder content should be shown with no placeholders effects and
     * false if either the placeholder or the wipe-off effect are being shown.
     */
    public val isShowContent: Boolean by derivedStateOf {
        placeholderStage == PlaceholderStage.ShowContent
    }

    /**
     * Should only be called when [isShowContent] is false. Returns true if the wipe-off effect that
     * reveals content should be shown and false if the placeholder effect should be shown.
     */
    public val isWipeOff: Boolean by derivedStateOf {
        placeholderStage == PlaceholderStage.WipeOff
    }

    /**
     * The width of the gradient to use for the placeholder shimmer and wipe-off effects. This is
     * the value in pixels that should be used in either horizontal or vertical direction to
     * be equivalent to a gradient width of 2 x maxScreenDimension rotated through 45 degrees.
     */
    internal val gradientXYWidth: Float by derivedStateOf {
        maxScreenDimension * 2f.pow(1.5f)
    }

    internal var placeholderStage: PlaceholderStage =
        if (isContentReady.value.invoke()) PlaceholderStage.ShowContent
        else PlaceholderStage.ShowPlaceholder
        get() {
            if (field != PlaceholderStage.ShowContent) {
                // WipeOff
                if (startOfWipeOffAnimation != 0L) {
                    if ((frameMillis.longValue - startOfWipeOffAnimation) >=
                        PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS) {
                        field = PlaceholderStage.ShowContent
                    }
                    // Placeholder
                } else if (isContentReady.value()) {
                    startOfWipeOffAnimation = frameMillis.longValue
                    field = PlaceholderStage.WipeOff
                }
            }
            return field
        }

    /**
     * The frame time in milliseconds in the calling context of frame dispatch. Used to coordinate
     * the placeholder state and effects. Usually provided by [withInfiniteAnimationFrameMillis].
     */
    internal val frameMillis = mutableStateOf(0L)

    private var startOfWipeOffAnimation = 0L

    private val progressionInterpolator: Easing = CubicBezierEasing(0.3f, 0f, 0.7f, 1f)
    private val wipeOffInterpolator: Easing = CubicBezierEasing(0f, 0.2f, 1f, 0.6f)
}

/**
 * Creates a [PlaceholderState] that is remembered across compositions. To start placeholder
 * animations run [PlaceholderState.startPlaceholderAnimation].
 *
 *  A [PlaceholderState] should be created for each component that has placeholder data. The
 * state is used to coordinate all of the different placeholder effects and animations.
 *
 * Placeholder has a number of different effects designed to work together.
 * [Modifier.placeholder] draws a placeholder shape on top of content that is waiting to load. There
 * can be multiple placeholders in a component.
 * [Modifier.placeholderShimmer] does a shimmer animation over the whole component that includes the
 * placeholders. There should only be one placeholderShimmer for each component.
 *
 * Background placeholder effects are used to mask the background of components like chips and cards
 * until all of the data has loaded. Use [PlaceholderDefaults.placeholderChipColors]
 * [PlaceholderDefaults.placeholderBackgroundBrush] and
 * [PlaceholderDefaults.painterWithPlaceholderOverlayBackgroundBrush] to draw the component
 * background.
 *
 * Once all of the components content is loaded, [isContentReady] is `true` the shimmer will stop
 * and a wipe off animation will remove the placeholders to reveal the content.
 *
 * @param isContentReady a lambda to determine whether all of the data/content has been loaded for a
 * given component and is ready to be displayed.
 */
@ExperimentalWearMaterialApi
@Composable
public fun rememberPlaceholderState(
    isContentReady: () -> Boolean
): PlaceholderState {
    val maxScreenDimension = with(LocalDensity.current) {
        Dp(max(screenHeightDp(), screenWidthDp()).toFloat()).toPx()
    }
    val myLambdaState = rememberUpdatedState(isContentReady)
    return remember { PlaceholderState(myLambdaState, maxScreenDimension) }
}

/**
 * Draws a placeholder shape over the top of a composable and animates a wipe off effect to remove
 * the placeholder. Typically used whilst content is 'loading' and then 'revealed'.
 *
 * Example of a [Chip] with icon and a label that put placeholders over individual content slots:
 * @sample androidx.wear.compose.material.samples.ChipWithIconAndLabelAndPlaceholders
 *
 * Example of a [Chip] with icon and a primary and secondary labels that draws another [Chip] over
 * the top of it when waiting for placeholder data to load:
 * @sample androidx.wear.compose.material.samples.ChipWithIconAndLabelsAndOverlaidPlaceholder
 *
 * The [placeholderState] determines when to 'show' and 'wipe off' the placeholder.
 *
 * NOTE: The order of modifiers is important. If you are adding both [Modifier.placeholder] and
 * [Modifier.placeholderShimmer] to the same composable then the shimmer must be first in the
 * modifier chain. Example of [Text] composable with both placeholderShimmer and placeholder
 * modifiers.
 * @sample androidx.wear.compose.material.samples.TextPlaceholder
 *
 * @param placeholderState determines whether the placeholder is visible and controls animation
 * effects for the placeholder.
 * @param shape the shape to apply to the placeholder
 * @param color the color of the placeholder.
 */
@Suppress("ComposableModifierFactory")
@ExperimentalWearMaterialApi
@Composable
public fun Modifier.placeholder(
    placeholderState: PlaceholderState,
    shape: Shape = MaterialTheme.shapes.small,
    color: Color =
        MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
            .compositeOver(MaterialTheme.colors.surface)
): Modifier = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "placeholder"
        properties["placeholderState"] = placeholderState
        properties["shape"] = shape
        properties["color"] = color
    }
) {
    PlaceholderModifier(
        placeholderState = placeholderState,
        color = color,
        shape = shape
    )
}

/**
 * Modifier to draw a placeholder shimmer over a component. The placeholder shimmer is a 45 degree
 * gradient from Top|Left of the screen to Bottom|Right. The shimmer is coordinated via the
 * animation frame clock which orchestrates the shimmer so that every component will shimmer as the
 * gradient progresses across the screen.
 *
 * Example of a [Chip] with icon and a label that put placeholders over individual content slots
 * and then draws a placeholder shimmer over the result:
 * @sample androidx.wear.compose.material.samples.ChipWithIconAndLabelAndPlaceholders
 *
 * Example of a [Chip] with icon and a primary and secondary labels that draws another [Chip] over
 * the top of it when waiting for placeholder data to load and then draws a placeholder shimmer over
 * the top:
 * @sample androidx.wear.compose.material.samples.ChipWithIconAndLabelsAndOverlaidPlaceholder
 *
 * NOTE: The order of modifiers is important. If you are adding both [Modifier.placeholder] and
 * [Modifier.placeholderShimmer] to the same composable then the shimmer must be before in the
 * modifier chain. Example of [Text] composable with both placeholderShimmer and placeholder
 * modifiers.
 * @sample androidx.wear.compose.material.samples.TextPlaceholder
 *
 * @param placeholderState the current placeholder state that determine whether the placeholder
 * shimmer should be shown.
 * @param shape the shape of the component.
 * @param color the color to use in the shimmer.
 */
@Suppress("ComposableModifierFactory")
@ExperimentalWearMaterialApi
@Composable
public fun Modifier.placeholderShimmer(
    placeholderState: PlaceholderState,
    shape: Shape = MaterialTheme.shapes.small,
    color: Color = MaterialTheme.colors.onSurface,
): Modifier = inspectable(
    inspectorInfo = debugInspectorInfo {
        name = "placeholderShimmer"
        properties["placeholderState"] = placeholderState
        properties["shape"] = shape
        properties["color"] = color
    }
) {
    PlaceholderShimmerModifier(
        placeholderState = placeholderState,
        color = color,
        shape = shape
    )
}

/**
 * Contains the default values used for providing placeholders.
 *
 * There are three distinct but coordinated aspects to placeholders in Compose for Wear OS.
 * Firstly placeholder [Modifier.placeholder] which is drawn over content that is not yet loaded.
 * Secondly a placeholder background which provides a background brush to cover the usual background
 * of containers such as [Chip] or [Card] until all of the content has loaded.
 * Thirdly a placeholder shimmer effect [Modifier.placeholderShimmer] effect which runs in an
 * animation loop while waiting for the data to load.
 */
@ExperimentalWearMaterialApi
public object PlaceholderDefaults {

    /**
     * Create a [ChipColors] that can be used in placeholder mode. This will provide the placeholder
     * background effect that covers the normal chip background with a solid background of [color]
     * when the [placeholderState] is set to show the placeholder and a wipe off gradient
     * brush when the state is in wipe-off mode. If the state is
     * [PlaceholderState.isShowContent] then the normal background will be used. All other colors
     * will be delegated to [originalChipColors].
     *
     * Example of a [Chip] with icon and a label that put placeholders over individual content slots
     * and then draws a placeholder shimmer over the result and draws over the [Chip]s
     * normal background color with [color] as the placeholder background color which will be wiped
     * away once all of the placeholder data is loaded:
     * @sample androidx.wear.compose.material.samples.ChipWithIconAndLabelAndPlaceholders
     *
     * @param originalChipColors the chip colors to use when not in placeholder mode.
     * @param placeholderState the placeholder state of the component
     * @param color the color to use for the placeholder background brush
     */
    @Composable
    public fun placeholderChipColors(
        originalChipColors: ChipColors,
        placeholderState: PlaceholderState,
        color: Color = MaterialTheme.colors.surface
    ): ChipColors {
        return if (! placeholderState.isShowContent) {
            ChipDefaults.chipColors(
                backgroundPainter = PlaceholderBackgroundPainter(
                    painter = originalChipColors.background(enabled = true).value,
                    placeholderState = placeholderState,
                    color = color
                ),
                contentColor = originalChipColors.contentColor(enabled = true).value,
                secondaryContentColor = originalChipColors
                    .secondaryContentColor(enabled = true).value,
                iconColor = originalChipColors.iconColor(enabled = true).value,
                disabledBackgroundPainter = PlaceholderBackgroundPainter(
                    painter = originalChipColors.background(enabled = false).value,
                    placeholderState = placeholderState,
                    color = color
                ),
                disabledContentColor = originalChipColors.contentColor(enabled = false).value,
                disabledSecondaryContentColor = originalChipColors
                    .secondaryContentColor(enabled = false).value,
                disabledIconColor = originalChipColors.iconColor(enabled = false).value,
            )
        } else {
            originalChipColors
        }
    }

    /**
     * Create a [ChipColors] that can be used for a [Chip] that is used as a placeholder drawn
     * on top of another [Chip]. When not drawing a placeholder background brush the chip
     * will be transparent allowing the contents of the chip below to be displayed.
     *
     * Example of a [Chip] with icon and a primary and secondary labels that draws another [Chip]
     * over the top of it when waiting for placeholder data to load and draws over the [Chip]s
     * normal background color with [color] as the placeholder background color which will be wiped
     * away once all of the placeholder data is loaded:
     * @sample androidx.wear.compose.material.samples.ChipWithIconAndLabelsAndOverlaidPlaceholder
     *
     * @param color the color to use for the placeholder background brush.
     * @param placeholderState the current placeholder state.
     */
    @Composable
    public fun placeholderChipColors(
        placeholderState: PlaceholderState,
        color: Color = MaterialTheme.colors.surface,
    ): ChipColors {
        return ChipDefaults.chipColors(
            backgroundPainter = PlaceholderBackgroundPainter(
                painter = null,
                placeholderState = placeholderState,
                color = color
            ),
            contentColor = Color.Transparent,
            secondaryContentColor = Color.Transparent,
            iconColor = Color.Transparent,
            disabledBackgroundPainter = PlaceholderBackgroundPainter(
                painter = null,
                placeholderState = placeholderState,
                color = color
            ),
            disabledContentColor = Color.Transparent,
            disabledSecondaryContentColor = Color.Transparent,
            disabledIconColor = Color.Transparent,
        )
    }

    /**
     * Create a [Painter] that wraps another painter and overlays a placeholder background brush
     * on top. If the [placeholderState] is [PlaceholderState.isShowContent] the original painter
     * will be used. Otherwise the [painter] will be drawn and then a placeholder background will be
     * drawn over it or a wipe-off brush will be used to reveal the background
     * when the state is [PlaceholderState.isWipeOff].
     *
     * @param placeholderState the state of the placeholder
     * @param painter the original painter that will be drawn over when in placeholder mode.
     * @param color the color to use for the placeholder background brush
     */
    @Composable
    public fun painterWithPlaceholderOverlayBackgroundBrush(
        placeholderState: PlaceholderState,
        painter: Painter,
        color: Color = MaterialTheme.colors.surface,
    ): Painter {
        return if (! placeholderState.isShowContent) {
            PlaceholderBackgroundPainter(
                painter = painter,
                placeholderState = placeholderState,
                color = color
            )
        } else {
            painter
        }
    }

    /**
     * Create a [Painter] that paints with a placeholder background brush.
     * If the [placeholderState] is [PlaceholderState.isShowContent] then a transparent background
     * will be shown. Otherwise a placeholder background will be drawn or a wipe-off brush
     * will be used to reveal the content underneath when [PlaceholderState.isWipeOff] is true.
     *
     * @param placeholderState the state of the placeholder
     * @param color the color to use for the placeholder background brush
     */
    @Composable
    public fun placeholderBackgroundBrush(
        placeholderState: PlaceholderState,
        color: Color = MaterialTheme.colors.surface,
    ): Painter {
        return PlaceholderBackgroundPainter(
            painter = null,
            placeholderState = placeholderState,
            color = color,
        )
    }
}

@ExperimentalWearMaterialApi
@Immutable
@JvmInline
/**
 * Enumerate the possible stages (states) that a placeholder can be in.
 */
internal value class PlaceholderStage internal constructor(internal val type: Int) {

    companion object {
        /**
         * Show placeholders and placeholder effects. Use when waiting for content to load.
         */
        val ShowPlaceholder = PlaceholderStage(0)

        /**
         * Wipe off placeholder effects. Used to animate the wiping away of placeholders and
         * revealing the content underneath. Enter this stage from [ShowPlaceholder] when the
         * next animation loop is started and the content is ready.
         */
        val WipeOff = PlaceholderStage(1)

        /**
         * Indicates that placeholders no longer to be shown. Enter this stage from
         * [WipeOff] in the loop after the wire-off animation.
         */
        val ShowContent = PlaceholderStage(2)
    }

    override fun toString(): String {
        return when (this) {
            ShowPlaceholder -> "PlaceholderStage.ShowPlaceholder"
            WipeOff -> "PlaceholderStage.WipeOff"
            else -> "PlaceholderStage.ShowContent"
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
private fun wipeOffBrush(
    color: Color,
    offset: Offset,
    placeholderState: PlaceholderState
): Brush {
    val halfGradientWidth = placeholderState.gradientXYWidth / 2f
    return Brush.linearGradient(
        colorStops = listOf(
            0f to Color.Transparent,
            0.75f to color,
        ).toTypedArray(),
        start = Offset(
            x = placeholderState.placeholderWipeOffProgression - halfGradientWidth - offset.x,
            y = placeholderState.placeholderWipeOffProgression - halfGradientWidth - offset.y
        ),
        end = Offset(
            x = placeholderState.placeholderWipeOffProgression + halfGradientWidth - offset.x,
            y = placeholderState.placeholderWipeOffProgression + halfGradientWidth - offset.y
        ),
    )
}

/**
 * A painter which wraps an optional [Painter] and is used to create an effect over the [Painter]
 * such as a solid placeholder color or a placeholder wipe off effect.
 */
@ExperimentalWearMaterialApi
internal class PlaceholderBackgroundPainter(
    val painter: Painter?,
    private val placeholderState: PlaceholderState,
    val color: Color,
    private var alpha: Float = 1.0f
) : Painter() {
    override fun DrawScope.onDraw() {
        // Due to anti aliasing we can not use a SolidColor brush over the top of the background
        // painter without seeing some background color bleeding through. As a result we use
        // the colorFilter to tint the normal background painter instead - b/253667329
        val (brush, colorFilter) = when (placeholderState.placeholderStage) {
            PlaceholderStage.WipeOff -> {
                wipeOffBrush(
                    color,
                    placeholderState.backgroundOffset,
                    placeholderState
                ) to null
            }
            PlaceholderStage.ShowPlaceholder -> {
                if (painter == null) {
                    SolidColor(color) to null
                } else {
                    null to ColorFilter.tint(color = color)
                }
            }
            // For the ShowContent case
            else -> {
                null to null
            }
        }

        val size = this.size
        if (painter != null) {
            with(painter) { draw(size = size, alpha = alpha, colorFilter = colorFilter) }
        }
        if (brush != null) {
            drawRect(brush = brush, alpha = alpha, colorFilter = colorFilter)
        }
    }

    override fun applyAlpha(alpha: Float): Boolean = true.also { this.alpha = alpha }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        // This is not a generic painter that we want to be configurable from the outside.
        // We need to control the colorFilter to do the painting over of normal background color
        // to avoid anti-aliasing
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaceholderBackgroundPainter

        if (painter != other.painter) return false
        if (placeholderState != other.placeholderState) return false
        if (color != other.color) return false
        if (alpha != other.alpha) return false
        if (intrinsicSize != other.intrinsicSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = painter.hashCode()
        result = 31 * result + placeholderState.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + intrinsicSize.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlaceholderBackgroundPainter(painter=$painter, " +
            "placeholderState=$placeholderState, color=$color, alpha=$alpha, " +
            "intrinsicSize=$intrinsicSize)"
    }

    /**
     * Size of the combined painter, return Unspecified to allow us to fill the available space
     */
    override val intrinsicSize: Size = painter?.intrinsicSize ?: Size.Unspecified
}

private abstract class AbstractPlaceholderModifier(
    private val alpha: Float = 1.0f,
    private val shape: Shape
) : DrawModifier, OnGloballyPositionedModifier {

    private var offset by mutableStateOf(Offset.Zero)
    // naive cache outline calculation if size is the same
    private var lastSize: Size? = null
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        offset = coordinates.positionInRoot()
    }

    abstract fun generateBrush(offset: Offset): Brush?

    override fun ContentDrawScope.draw() {
        val brush = generateBrush(offset)

        drawContent()
        if (brush != null) {
            if (shape === RectangleShape) {
                // shortcut to avoid Outline calculation and allocation
                drawRect(brush)
            } else {
                drawOutline(brush)
            }
        }
    }

    private fun ContentDrawScope.drawOutline(brush: Brush) {
        val outline =
            if (size == lastSize && layoutDirection == lastLayoutDirection) {
                lastOutline!!
            } else {
                shape.createOutline(size, layoutDirection, this)
            }
        drawOutline(outline, brush = brush, alpha = alpha)
        lastOutline = outline
        lastSize = size
    }
}

@ExperimentalWearMaterialApi
private class PlaceholderModifier constructor(
    private val placeholderState: PlaceholderState,
    private val color: Color,
    alpha: Float = 1.0f,
    val shape: Shape
) : AbstractPlaceholderModifier(alpha, shape) {
    override fun generateBrush(offset: Offset): Brush? {
            return when (placeholderState.placeholderStage) {
                PlaceholderStage.ShowPlaceholder -> {
                    SolidColor(color)
                }
                PlaceholderStage.WipeOff -> {
                    wipeOffBrush(color, offset, placeholderState)
                }
                else -> {
                    null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaceholderModifier

        if (placeholderState != other.placeholderState) return false
        if (color != other.color) return false
        if (shape != other.shape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = placeholderState.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }
}

@ExperimentalWearMaterialApi
private class PlaceholderShimmerModifier constructor(
    private val placeholderState: PlaceholderState,
    private val color: Color,
    alpha: Float = 1.0f,
    val shape: Shape
) : AbstractPlaceholderModifier(alpha, shape) {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        placeholderState.backgroundOffset = coordinates.positionInRoot()
        super.onGloballyPositioned(coordinates)
    }
    override fun generateBrush(offset: Offset): Brush? {
        return if (placeholderState.placeholderStage == PlaceholderStage.ShowPlaceholder) {
            val halfGradientWidth = placeholderState.gradientXYWidth / 2f
            Brush.linearGradient(
                start = Offset(
                    x = placeholderState.placeholderProgression - halfGradientWidth - offset.x,
                    y = placeholderState.placeholderProgression - halfGradientWidth - offset.y
                ),
                end = Offset(
                    x = placeholderState.placeholderProgression + halfGradientWidth - offset.x,
                    y = placeholderState.placeholderProgression + halfGradientWidth - offset.y
                ),
                colorStops = listOf(
                    0.1f to color.copy(alpha = 0f),
                    0.65f to color.copy(alpha = placeholderState.placeholderShimmerAlpha),
                    0.9f to color.copy(alpha = 0f),
                ).toTypedArray()
            )
        } else {
            null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaceholderShimmerModifier

        if (placeholderState != other.placeholderState) return false
        if (color != other.color) return false
        if (shape != other.shape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = placeholderState.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }
}

internal const val PLACEHOLDER_SHIMMER_DURATION_MS = 800L
internal const val PLACEHOLDER_WIPE_OFF_PROGRESSION_DURATION_MS = 300L
internal const val PLACEHOLDER_SHIMMER_GAP_BETWEEN_ANIMATION_LOOPS_MS = 2000L
internal const val PLACEHOLDER_WIPE_OFF_PROGRESSION_ALPHA_DURATION_MS = 80L
