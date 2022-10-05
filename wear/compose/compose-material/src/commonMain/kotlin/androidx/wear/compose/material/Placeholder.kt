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

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.painter.ColorPainter
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

/**
 * A state object that can be used to control placeholders. Placeholders are used when the content
 * that needs to be displayed in a component is not yet available, e.g. it is loading
 * asynchronously.
 *
 */
@ExperimentalWearMaterialApi
@Stable
public class PlaceholderState internal constructor(
    private val isContentReady: () -> Boolean,
    private val maxScreenDimension: Float,
) {
    /**
     * Start the animation of the placeholder state.
     */
    public suspend fun startPlaceholderAnimation() {
        coroutineScope {
            while (isActive) {
                withInfiniteAnimationFrameMillis {
                    frameMillis.value = it
                }
            }
        }
    }

    /**
     * The current value of the placeholder visual effect gradient progression. The progression
     * is a 45 degree angle sweep across the whole screen running from outside of the Top|Left of
     * the screen to Bottom|Right used as the anchor for shimmer and wipe-off gradient effects.
     *
     * The progression represents the x and y coordinates in pixels of the Top|Left part of the
     * gradient that flows across the screen. The progression will start at -gradientWidth and
     * progress to the maximum screen dimension (max of height/width to create a 45 degree angle).
     */
    @ExperimentalWearMaterialApi
    public val placeholderProgression: Float by derivedStateOf {
        val absoluteProgression =
            (frameMillis.value.mod(PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS).coerceAtMost(
                PLACEHOLDER_PROGRESSION_DURATION_MS).toFloat() /
                PLACEHOLDER_PROGRESSION_DURATION_MS)
        val progression = lerp(-maxScreenDimension, maxScreenDimension, absoluteProgression)
        progression
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
     * The width of the gradient to use for the placeholder shimmer and wipe-off effects
     */
    internal val gradientWidth: Float by derivedStateOf {
        maxScreenDimension
    }

    internal var placeholderStage: PlaceholderStage =
        if (isContentReady.invoke()) PlaceholderStage.ShowContent
        else PlaceholderStage.ShowPlaceholder
        get() {
            if (field != PlaceholderStage.ShowContent) {
                if (startOfNextPlaceholderAnimation == 0L) {
                    startOfNextPlaceholderAnimation =
                        (frameMillis.value.div(PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS) + 1) *
                            PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS
                } else if (frameMillis.value >= startOfNextPlaceholderAnimation) {
                    field = checkForStageTransition(
                        field,
                        isContentReady(),
                    )
                    startOfNextPlaceholderAnimation =
                        (frameMillis.value.div(PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS) + 1) *
                            PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS
                }
            }
            return field
        }
    /**
     * The frame time in milliseconds in the calling context of frame dispatch. Used to coordinate
     * the placeholder state and effects. Usually provided by [withInfiniteAnimationFrameMillis].
     */
    internal val frameMillis = mutableStateOf(0L)

    private var startOfNextPlaceholderAnimation = 0L
}

/**
 * Creates a [PlaceholderState] that is remembered across compositions. To start placeholder
 * animations run [PlaceholderState.startPlaceholderAnimation].
 *
 * @param isContentReady a lambda to determine whether all of the data/content has been loaded
 * and is ready to be displayed.
 */
@ExperimentalWearMaterialApi
@Composable
public fun rememberPlaceholderState(isContentReady: () -> Boolean): PlaceholderState {
    val maxScreenDimension = with(LocalDensity.current) {
        Dp(max(screenHeightDp(), screenWidthDp()).toFloat()).toPx()
    }
    return remember { PlaceholderState(isContentReady, maxScreenDimension) }
}

/**
 * Method used to determine whether we should transition to a new [PlaceholderStage] based
 * on the current value and whether the content has loaded.
 *
 * @param placeholderStage the current stage of the placeholder
 * @param contentReady a flag to indicate whether all of content is now available
 */
@OptIn(ExperimentalWearMaterialApi::class)
private fun checkForStageTransition(
    placeholderStage: PlaceholderStage,
    contentReady: Boolean,
): PlaceholderStage {
    return if (placeholderStage == PlaceholderStage.ShowPlaceholder && contentReady) {
        PlaceholderStage.WipeOff
    } else if (placeholderStage == PlaceholderStage.WipeOff) {
        PlaceholderStage.ShowContent
    } else placeholderStage
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
                backgroundPainter = PainterWithBrushOverlay(
                    painter = originalChipColors.background(enabled = true).value,
                    placeholderState = placeholderState,
                    color = color
                ),
                contentColor = originalChipColors.contentColor(enabled = true).value,
                secondaryContentColor = originalChipColors
                    .secondaryContentColor(enabled = true).value,
                iconColor = originalChipColors.iconColor(enabled = true).value,
                disabledBackgroundPainter = PainterWithBrushOverlay(
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
        return placeholderChipColors(
            placeholderState = placeholderState,
            color = color,
            originalChipColors = ChipDefaults.chipColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.Transparent,
                secondaryContentColor = Color.Transparent,
                iconColor = Color.Transparent,
                disabledBackgroundColor = Color.Transparent,
                disabledContentColor = Color.Transparent,
                disabledSecondaryContentColor = Color.Transparent,
                disabledIconColor = Color.Transparent)
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
            PainterWithBrushOverlay(
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
        return PainterWithBrushOverlay(
            painter = ColorPainter(Color.Transparent),
            placeholderState = placeholderState,
            color = color
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
    return Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            color
        ),
        start = Offset(
            x = placeholderState.placeholderProgression - offset.x,
            y = placeholderState.placeholderProgression - offset.y
        ),
        end = Offset(
            x = placeholderState.placeholderProgression - offset.x + placeholderState.gradientWidth,
            y = placeholderState.placeholderProgression - offset.y + placeholderState.gradientWidth
        )
    )
}

/**
 * A painter which takes wraps another [Painter] and takes a [Brush] which
 * is used to create an effect over the [Painter] such as a shim or a placeholder effect.
 */
@ExperimentalWearMaterialApi
internal class PainterWithBrushOverlay(
    val painter: Painter,
    private val placeholderState: PlaceholderState,
    val color: Color,
    private var alpha: Float = 1.0f
) : Painter() {

    private var colorFilter: ColorFilter? = null

    override fun DrawScope.onDraw() {
        val offset = Offset(
            this.center.x - (this.size.width / 2f),
            this.center.y - (this.size.height / 2f)
        )
        val brush = when (placeholderState.placeholderStage) {
            PlaceholderStage.ShowPlaceholder -> {
                SolidColor(color)
            }
            PlaceholderStage.WipeOff -> {
                wipeOffBrush(
                    color,
                    offset,
                    placeholderState
                )
            }
            // For the ShowContent case
            else -> {
                null
            }
        }

        val size = this.size
        with(painter) { draw(size = size, alpha = alpha, colorFilter = colorFilter) }
        if (brush != null) {
            drawRect(brush = brush, alpha = alpha, colorFilter = colorFilter)
        }
    }

    override fun applyAlpha(alpha: Float): Boolean = true.also { this.alpha = alpha }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PainterWithBrushOverlay

        if (painter != other.painter) return false
        if (placeholderState != other.placeholderState) return false
        if (color != other.color) return false
        if (alpha != other.alpha) return false
        if (colorFilter != other.colorFilter) return false
        if (intrinsicSize != other.intrinsicSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = painter.hashCode()
        result = 31 * result + placeholderState.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + (colorFilter?.hashCode() ?: 0)
        result = 31 * result + intrinsicSize.hashCode()
        return result
    }

    override fun toString(): String {
        return "PainterWithBrushOverlay(painter=$painter, placeholderState=$placeholderState, " +
            "color=$color, alpha=$alpha, " +
            "colorFilter=$colorFilter, intrinsicSize=$intrinsicSize)"
    }

    /**
     * Size of the combined painter, return Unspecified to allow us to fill the available space
     */
    override val intrinsicSize: Size = painter.intrinsicSize
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

    private fun ContentDrawScope.drawRect(brush: Brush) {
        drawRect(brush = brush, alpha = alpha)
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
    override fun generateBrush(offset: Offset): Brush? {
        return if (placeholderState.placeholderStage == PlaceholderStage.ShowPlaceholder) {
            Brush.linearGradient(
                start = Offset(x = placeholderState.placeholderProgression - offset.x,
                    y = placeholderState.placeholderProgression - offset.y),
                end = Offset(
                    x = placeholderState.placeholderProgression - offset.x +
                        placeholderState.gradientWidth,
                    y = placeholderState.placeholderProgression - offset.y +
                        placeholderState.gradientWidth
                ),
                colorStops = listOf(
                    0f to color.copy(alpha = 0f),
                    0.65f to color.copy(alpha = 0.13f),
                    1f to color.copy(alpha = 0f),
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

internal const val PLACEHOLDER_PROGRESSION_DURATION_MS = 800L
internal const val PLACEHOLDER_DELAY_BETWEEN_PROGRESSIONS_MS = 800L
internal const val PLACEHOLDER_GAP_BETWEEN_ANIMATION_LOOPS_MS =
    PLACEHOLDER_PROGRESSION_DURATION_MS + PLACEHOLDER_DELAY_BETWEEN_PROGRESSIONS_MS