/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.annotation.FloatRange
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.internal.InteractionTracker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A visual and interactive scrollbar that represents the current scroll position of a scrolling
 * component and allows users to scroll the component by dragging the thumb or pressing the track.
 * For a visual-only scrollbar that does not respond to user input, see [nonInteractiveScrollbar].
 *
 * It is drawn at the end edge (respecting layout direction) for a vertically scrollable component,
 * and at the bottom edge for a horizontally scrollable component.
 *
 * The scrollbar is only visible if the content size is strictly greater than the viewport size. By
 * default, the scrollbar is shown when scrolling is active and fades out after a delay when
 * scrolling stops. Hovering over the scrollbar will also keep it fully visible (or fade it in if it
 * had faded out). This fading behavior can be disabled by setting [isFadeEnabled] to false, in
 * which case the scrollbar remains always visible.
 *
 * To use an interactive scrollbar with a [androidx.compose.foundation.lazy.LazyColumn], see:
 *
 * @sample androidx.compose.material3.samples.InteractiveScrollbarWithLazyColumnSample
 *
 * To use an interactive scrollbar with a [androidx.compose.foundation.layout.Column] that uses
 * [androidx.compose.foundation.verticalScroll], see:
 *
 * @sample androidx.compose.material3.samples.InteractiveScrollbarWithVerticalScrollSample
 * @param state the [ScrollIndicatorState] that represents the scroll state.
 * @param orientation the orientation of the scrollbar.
 * @param onThumbDrag a callback invoked when the scrollbar thumb is dragged. The lambda receives
 *   the `positionFraction` representing the position of the thumb along the track (between 0f and
 *   1f), which should be used to scroll the content. Note that pressing the thumb without moving
 *   the pointer does not invoke this callback, it is only invoked once the pointer moves.
 * @param onTrackPress a callback invoked when the scrollbar track is pressed outside the thumb. The
 *   lambda receives the `positionFraction` representing the target position on the track (between
 *   0f and 1f) to jump or navigate to. Note that if [onTrackPress] moves the thumb under the
 *   pressed position, continuous dragging is handled by [onThumbDrag].
 * @param thickness the thickness of the scrollbar.
 * @param thumbMinLength the minimum length of the scrollbar thumb. Note that the scrollbar will not
 *   be shown if the available track length is less than this minimum length. Takes precedence over
 *   [thumbMaxLengthFraction].
 * @param thumbMaxLengthFraction the maximum length of the scrollbar thumb as a fraction of the
 *   track length. This should be between 0f and 1f. Clamped to at least [thumbMinLength].
 * @param isFadeEnabled whether the fade behavior is enabled. If enabled, the scrollbar will be
 *   shown when scrolling is active, and fades out after a delay when scrolling is idle. If
 *   disabled, the scrollbar remains always visible.
 * @param fadeDurationMillis the duration of the fade animation in milliseconds.
 * @param fadeDelayMillis the delay before the scrollbar fades out in milliseconds.
 * @param mainAxisTrackInset the inset to apply to the scrollbar track along the main axis.
 * @param crossAxisTrackInset the inset to apply to the scrollbar track along the cross axis.
 * @param colors the [InteractiveScrollbarColors] used to resolve the colors of the scrollbar thumb
 *   and track in different states.
 * @param thumbInteractionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for the scrollbar thumb. You can use this to change the thumb's
 *   appearance or preview it in different states. Externally emitted interactions only affect the
 *   thumb's appearance and do not affect fade visibility; set [isFadeEnabled] to `false` to preview
 *   interaction states. Interactions are only emitted for the thumb ([PressInteraction],
 *   [DragInteraction], and [HoverInteraction]), not for track presses. Note that if `null` is
 *   provided, interactions will still happen internally.
 * @see nonInteractiveScrollbar
 */
@Composable
public fun Modifier.interactiveScrollbar(
    state: ScrollIndicatorState,
    orientation: Orientation,
    onThumbDrag: (positionFraction: Float) -> Unit,
    onTrackPress: (positionFraction: Float) -> Unit,
    thickness: Dp = InteractiveScrollbarDefaults.Thickness,
    thumbMinLength: Dp = InteractiveScrollbarDefaults.ThumbMinLength,
    @FloatRange(from = 0.0, to = 1.0)
    thumbMaxLengthFraction: Float = InteractiveScrollbarDefaults.ThumbMaxLengthFraction,
    isFadeEnabled: Boolean = true,
    fadeDurationMillis: Int = InteractiveScrollbarDefaults.ThumbFadeDurationMillis,
    fadeDelayMillis: Int = InteractiveScrollbarDefaults.ThumbFadeDelayMillis,
    mainAxisTrackInset: Dp = InteractiveScrollbarDefaults.MainAxisTrackInset,
    crossAxisTrackInset: Dp = InteractiveScrollbarDefaults.CrossAxisTrackInset,
    colors: InteractiveScrollbarColors = InteractiveScrollbarDefaults.colors(),
    thumbInteractionSource: MutableInteractionSource? = null,
): Modifier {
    require(thumbMaxLengthFraction in 0f..1f) { "thumbMaxLengthFraction must be between 0f and 1f" }
    return this.then(
        InteractiveScrollbarElement(
            state = state,
            orientation = orientation,
            onThumbDrag = onThumbDrag,
            onTrackPress = onTrackPress,
            thumbInteractionSource = thumbInteractionSource,
            colors = colors,
            thickness = thickness,
            thumbMinLength = thumbMinLength,
            thumbMaxLengthFraction = thumbMaxLengthFraction,
            isFadeEnabled = isFadeEnabled,
            fadeDurationMillis = fadeDurationMillis,
            fadeDelayMillis = fadeDelayMillis,
            mainAxisTrackInset = mainAxisTrackInset,
            crossAxisTrackInset = crossAxisTrackInset,
        )
    )
}

/**
 * Represents the colors used by an interactive scrollbar in different interaction states.
 *
 * @param thumbColor the default color of the scrollbar thumb when idle.
 * @param hoveredThumbColor the color of the scrollbar thumb when hovered.
 * @param pressedThumbColor the color of the scrollbar thumb when pressed.
 * @param draggedThumbColor the color of the scrollbar thumb when actively dragged.
 * @param trackColor the color of the scrollbar track.
 */
@Immutable
public class InteractiveScrollbarColors(
    public val thumbColor: Color,
    public val hoveredThumbColor: Color,
    public val pressedThumbColor: Color,
    public val draggedThumbColor: Color,
    public val trackColor: Color,
) {
    /** Resolves the thumb color based on interaction state. */
    internal fun thumbColor(isHovered: Boolean, isPressed: Boolean, isDragged: Boolean): Color {
        return when {
            isDragged -> draggedThumbColor
            isPressed -> pressedThumbColor
            isHovered -> hoveredThumbColor
            else -> thumbColor
        }
    }

    /**
     * Returns a copy of this [InteractiveScrollbarColors], optionally overriding some of the
     * values.
     */
    public fun copy(
        thumbColor: Color = this.thumbColor,
        hoveredThumbColor: Color = this.hoveredThumbColor,
        pressedThumbColor: Color = this.pressedThumbColor,
        draggedThumbColor: Color = this.draggedThumbColor,
        trackColor: Color = this.trackColor,
    ): InteractiveScrollbarColors =
        InteractiveScrollbarColors(
            thumbColor = thumbColor.takeOrElse { this.thumbColor },
            hoveredThumbColor = hoveredThumbColor.takeOrElse { this.hoveredThumbColor },
            pressedThumbColor = pressedThumbColor.takeOrElse { this.pressedThumbColor },
            draggedThumbColor = draggedThumbColor.takeOrElse { this.draggedThumbColor },
            trackColor = trackColor.takeOrElse { this.trackColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InteractiveScrollbarColors) return false
        if (thumbColor != other.thumbColor) return false
        if (hoveredThumbColor != other.hoveredThumbColor) return false
        if (pressedThumbColor != other.pressedThumbColor) return false
        if (draggedThumbColor != other.draggedThumbColor) return false
        if (trackColor != other.trackColor) return false
        return true
    }

    override fun hashCode(): Int {
        var result = thumbColor.hashCode()
        result = 31 * result + hoveredThumbColor.hashCode()
        result = 31 * result + pressedThumbColor.hashCode()
        result = 31 * result + draggedThumbColor.hashCode()
        result = 31 * result + trackColor.hashCode()
        return result
    }
}

/** Contains the default values used by [Modifier.interactiveScrollbar]. */
public object InteractiveScrollbarDefaults {
    /** Default thickness for an interactive scrollbar thumb. */
    public val Thickness: Dp = 8.dp

    /** Default cross axis inset for the scrollbar track. */
    public val CrossAxisTrackInset: Dp = 2.dp

    /** Default main axis inset for the scrollbar track. */
    public val MainAxisTrackInset: Dp = 2.dp

    /** Default minimum length of the scrollbar thumb. */
    public val ThumbMinLength: Dp = 24.dp

    /** Default maximum length of the scrollbar thumb as a fraction of the track length. */
    public val ThumbMaxLengthFraction: Float = 0.9f

    /** Default duration of the fade animation in milliseconds. */
    public val ThumbFadeDurationMillis: Int = 250

    /** Default delay before the scrollbar fades out in milliseconds. */
    public val ThumbFadeDelayMillis: Int = 400

    /** Alpha value applied to the scrollbar thumb when it is idle. */
    public val ThumbOpacity: Float = 0.70f

    /** Alpha value applied to the scrollbar thumb when it is hovered. */
    public val HoveredThumbOpacity: Float = 0.80f

    /** Alpha value applied to the scrollbar thumb when it is pressed or dragged. */
    public val PressedThumbOpacity: Float = 0.95f

    /**
     * Creates an [InteractiveScrollbarColors] that represents the default colors used in an
     * interactive scrollbar.
     */
    @Composable
    public fun colors(): InteractiveScrollbarColors =
        MaterialTheme.colorScheme.defaultInteractiveScrollbarColors

    /**
     * Creates an [InteractiveScrollbarColors] that represents the default colors used in an
     * interactive scrollbar.
     *
     * @param thumbColor the default color of the scrollbar thumb when idle.
     * @param hoveredThumbColor the color of the scrollbar thumb when hovered.
     * @param pressedThumbColor the color of the scrollbar thumb when pressed.
     * @param draggedThumbColor the color of the scrollbar thumb when actively dragged.
     * @param trackColor the color of the scrollbar track.
     */
    @Composable
    public fun colors(
        thumbColor: Color = Color.Unspecified,
        hoveredThumbColor: Color = Color.Unspecified,
        pressedThumbColor: Color = Color.Unspecified,
        draggedThumbColor: Color = Color.Unspecified,
        trackColor: Color = Color.Unspecified,
    ): InteractiveScrollbarColors =
        MaterialTheme.colorScheme.defaultInteractiveScrollbarColors.copy(
            thumbColor = thumbColor,
            hoveredThumbColor = hoveredThumbColor,
            pressedThumbColor = pressedThumbColor,
            draggedThumbColor = draggedThumbColor,
            trackColor = trackColor,
        )

    internal val ColorScheme.defaultInteractiveScrollbarColors: InteractiveScrollbarColors
        get() {
            return defaultInteractiveScrollbarColorsCached
                ?: InteractiveScrollbarColors(
                        thumbColor = outline.copy(alpha = ThumbOpacity),
                        hoveredThumbColor = outline.copy(alpha = HoveredThumbOpacity),
                        pressedThumbColor = outline.copy(alpha = PressedThumbOpacity),
                        draggedThumbColor = outline.copy(alpha = PressedThumbOpacity),
                        trackColor = Color.Transparent,
                    )
                    .also { defaultInteractiveScrollbarColorsCached = it }
        }
}

private class InteractiveScrollbarElement(
    private val state: ScrollIndicatorState,
    private val orientation: Orientation,
    private val onThumbDrag: (Float) -> Unit,
    private val onTrackPress: (Float) -> Unit,
    private val thumbInteractionSource: MutableInteractionSource?,
    private val colors: InteractiveScrollbarColors,
    private val thickness: Dp,
    private val thumbMinLength: Dp,
    private val thumbMaxLengthFraction: Float,
    private val isFadeEnabled: Boolean,
    private val fadeDurationMillis: Int,
    private val fadeDelayMillis: Int,
    private val mainAxisTrackInset: Dp,
    private val crossAxisTrackInset: Dp,
) : ModifierNodeElement<InteractiveScrollbarNodeWrapper>() {
    override fun create(): InteractiveScrollbarNodeWrapper {
        return InteractiveScrollbarNodeWrapper(
            state = state,
            orientation = orientation,
            onThumbDrag = onThumbDrag,
            onTrackPress = onTrackPress,
            thumbInteractionSource = thumbInteractionSource,
            colors = colors,
            thickness = thickness,
            thumbMinLength = thumbMinLength,
            thumbMaxLengthFraction = thumbMaxLengthFraction,
            isFadeEnabled = isFadeEnabled,
            fadeDurationMillis = fadeDurationMillis,
            fadeDelayMillis = fadeDelayMillis,
            mainAxisTrackInset = mainAxisTrackInset,
            crossAxisTrackInset = crossAxisTrackInset,
        )
    }

    override fun update(node: InteractiveScrollbarNodeWrapper) {
        node.update(
            state = state,
            orientation = orientation,
            onThumbDrag = onThumbDrag,
            onTrackPress = onTrackPress,
            thumbInteractionSource = thumbInteractionSource,
            colors = colors,
            thickness = thickness,
            thumbMinLength = thumbMinLength,
            thumbMaxLengthFraction = thumbMaxLengthFraction,
            isFadeEnabled = isFadeEnabled,
            fadeDurationMillis = fadeDurationMillis,
            fadeDelayMillis = fadeDelayMillis,
            mainAxisTrackInset = mainAxisTrackInset,
            crossAxisTrackInset = crossAxisTrackInset,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InteractiveScrollbarElement) return false
        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (onThumbDrag !== other.onThumbDrag) return false
        if (onTrackPress !== other.onTrackPress) return false
        if (thumbInteractionSource != other.thumbInteractionSource) return false
        if (colors != other.colors) return false
        if (thickness != other.thickness) return false
        if (thumbMinLength != other.thumbMinLength) return false
        if (thumbMaxLengthFraction != other.thumbMaxLengthFraction) return false
        if (isFadeEnabled != other.isFadeEnabled) return false
        if (fadeDurationMillis != other.fadeDurationMillis) return false
        if (fadeDelayMillis != other.fadeDelayMillis) return false
        if (mainAxisTrackInset != other.mainAxisTrackInset) return false
        if (crossAxisTrackInset != other.crossAxisTrackInset) return false
        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + onThumbDrag.hashCode()
        result = 31 * result + onTrackPress.hashCode()
        result = 31 * result + (thumbInteractionSource?.hashCode() ?: 0)
        result = 31 * result + colors.hashCode()
        result = 31 * result + thickness.hashCode()
        result = 31 * result + thumbMinLength.hashCode()
        result = 31 * result + thumbMaxLengthFraction.hashCode()
        result = 31 * result + isFadeEnabled.hashCode()
        result = 31 * result + fadeDurationMillis.hashCode()
        result = 31 * result + fadeDelayMillis.hashCode()
        result = 31 * result + mainAxisTrackInset.hashCode()
        result = 31 * result + crossAxisTrackInset.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "interactiveScrollbar"
        properties["state"] = state
        properties["orientation"] = orientation
        properties["onThumbDrag"] = onThumbDrag
        properties["onTrackPress"] = onTrackPress
        properties["thumbInteractionSource"] = thumbInteractionSource
        properties["colors"] = colors
        properties["thickness"] = thickness
        properties["thumbMinLength"] = thumbMinLength
        properties["thumbMaxLengthFraction"] = thumbMaxLengthFraction
        properties["isFadeEnabled"] = isFadeEnabled
        properties["fadeDurationMillis"] = fadeDurationMillis
        properties["fadeDelayMillis"] = fadeDelayMillis
        properties["mainAxisTrackInset"] = mainAxisTrackInset
        properties["crossAxisTrackInset"] = crossAxisTrackInset
    }
}

internal class InteractiveScrollbarNodeWrapper(
    private var state: ScrollIndicatorState,
    private var orientation: Orientation,
    private var onThumbDrag: (Float) -> Unit,
    private var onTrackPress: (Float) -> Unit,
    private var thumbInteractionSource: MutableInteractionSource?,
    private var colors: InteractiveScrollbarColors,
    private var thickness: Dp,
    private var thumbMinLength: Dp,
    private var thumbMaxLengthFraction: Float,
    private var isFadeEnabled: Boolean,
    private var fadeDurationMillis: Int,
    private var fadeDelayMillis: Int,
    private var mainAxisTrackInset: Dp,
    private var crossAxisTrackInset: Dp,
) : DelegatingNode() {
    private val _onThumbDrag: (Float) -> Unit = { this.onThumbDrag(it) }
    private val _onTrackPress: (Float) -> Unit = { this.onTrackPress(it) }

    private var scrollbarNode =
        delegate(
            createInteractiveScrollbarModifierNode(
                state = state,
                orientation = orientation,
                onThumbDrag = _onThumbDrag,
                onTrackPress = _onTrackPress,
                thumbInteractionSource = thumbInteractionSource,
                colors = colors,
                thickness = thickness,
                thumbMinLength = thumbMinLength,
                thumbMaxLengthFraction = thumbMaxLengthFraction,
                isFadeEnabled = isFadeEnabled,
                fadeAnimationSpec = tween(fadeDurationMillis),
                fadeDelayMillis = fadeDelayMillis,
                mainAxisTrackInset = mainAxisTrackInset,
                crossAxisTrackInset = crossAxisTrackInset,
            )
        )

    fun update(
        state: ScrollIndicatorState,
        orientation: Orientation,
        onThumbDrag: (Float) -> Unit,
        onTrackPress: (Float) -> Unit,
        thumbInteractionSource: MutableInteractionSource?,
        colors: InteractiveScrollbarColors,
        thickness: Dp,
        thumbMinLength: Dp,
        thumbMaxLengthFraction: Float,
        isFadeEnabled: Boolean,
        fadeDurationMillis: Int,
        fadeDelayMillis: Int,
        mainAxisTrackInset: Dp,
        crossAxisTrackInset: Dp,
    ) {
        // Track lambda changes separately. If only lambdas change, we update the backing
        // properties without forcing a node recreation.
        val lambdaChanged = this.onThumbDrag !== onThumbDrag || this.onTrackPress !== onTrackPress
        val otherChanged =
            this.state != state ||
                this.orientation != orientation ||
                this.thumbInteractionSource != thumbInteractionSource ||
                this.colors != colors ||
                this.thickness != thickness ||
                this.thumbMinLength != thumbMinLength ||
                this.thumbMaxLengthFraction != thumbMaxLengthFraction ||
                this.isFadeEnabled != isFadeEnabled ||
                this.fadeDurationMillis != fadeDurationMillis ||
                this.fadeDelayMillis != fadeDelayMillis ||
                this.mainAxisTrackInset != mainAxisTrackInset ||
                this.crossAxisTrackInset != crossAxisTrackInset

        if (lambdaChanged || otherChanged) {
            this.onThumbDrag = onThumbDrag
            this.onTrackPress = onTrackPress

            if (otherChanged) {
                this.state = state
                this.orientation = orientation
                this.thumbInteractionSource = thumbInteractionSource
                this.colors = colors
                this.thickness = thickness
                this.thumbMinLength = thumbMinLength
                this.thumbMaxLengthFraction = thumbMaxLengthFraction
                this.isFadeEnabled = isFadeEnabled
                this.fadeDurationMillis = fadeDurationMillis
                this.fadeDelayMillis = fadeDelayMillis
                this.mainAxisTrackInset = mainAxisTrackInset
                this.crossAxisTrackInset = crossAxisTrackInset

                undelegate(scrollbarNode)
                scrollbarNode =
                    delegate(
                        createInteractiveScrollbarModifierNode(
                            state = state,
                            orientation = orientation,
                            onThumbDrag = _onThumbDrag,
                            onTrackPress = _onTrackPress,
                            thumbInteractionSource = thumbInteractionSource,
                            colors = colors,
                            thickness = thickness,
                            thumbMinLength = thumbMinLength,
                            thumbMaxLengthFraction = thumbMaxLengthFraction,
                            isFadeEnabled = isFadeEnabled,
                            fadeAnimationSpec = tween(fadeDurationMillis),
                            fadeDelayMillis = fadeDelayMillis,
                            mainAxisTrackInset = mainAxisTrackInset,
                            crossAxisTrackInset = crossAxisTrackInset,
                        )
                    )
            }
        }
    }
}

// TODO: b/505077759 - Publish this as a public API in some lower layer.
internal fun createInteractiveScrollbarModifierNode(
    state: ScrollIndicatorState,
    orientation: Orientation,
    onThumbDrag: (Float) -> Unit,
    onTrackPress: (Float) -> Unit,
    thumbInteractionSource: MutableInteractionSource?,
    colors: InteractiveScrollbarColors,
    thickness: Dp,
    thumbMinLength: Dp,
    @FloatRange(from = 0.0, to = 1.0) thumbMaxLengthFraction: Float,
    isFadeEnabled: Boolean,
    fadeAnimationSpec: FiniteAnimationSpec<Float>,
    fadeDelayMillis: Int,
    mainAxisTrackInset: Dp,
    crossAxisTrackInset: Dp,
): DelegatableNode =
    InteractiveScrollbarNode(
        state = state,
        orientation = orientation,
        onThumbDrag = onThumbDrag,
        onTrackPress = onTrackPress,
        thumbInteractionSource = thumbInteractionSource,
        colors = colors,
        thickness = thickness,
        thumbMinLength = thumbMinLength,
        thumbMaxLengthFraction = thumbMaxLengthFraction,
        isFadeEnabled = isFadeEnabled,
        fadeAnimationSpec = fadeAnimationSpec,
        fadeDelayMillis = fadeDelayMillis,
        mainAxisTrackInset = mainAxisTrackInset,
        crossAxisTrackInset = crossAxisTrackInset,
    )

private class InteractiveScrollbarNode(
    private var state: ScrollIndicatorState,
    private var orientation: Orientation,
    private var onThumbDrag: (Float) -> Unit,
    private var onTrackPress: (Float) -> Unit,
    private var thumbInteractionSource: MutableInteractionSource?,
    private var colors: InteractiveScrollbarColors,
    private var thickness: Dp,
    private var thumbMinLength: Dp,
    private var thumbMaxLengthFraction: Float,
    isFadeEnabled: Boolean,
    fadeAnimationSpec: FiniteAnimationSpec<Float>,
    fadeDelayMillis: Int,
    private var mainAxisTrackInset: Dp,
    private var crossAxisTrackInset: Dp,
) :
    AbstractScrollbarNode(isFadeEnabled, fadeAnimationSpec, fadeDelayMillis),
    PointerInputModifierNode,
    DrawModifierNode {
    private var cachedThicknessPx = -1f
    private var cachedCornerRadius = CornerRadius.Zero
    private var lastOffset = -1

    // The ID of the pointer currently captured for thumb dragging or track scrubbing.
    private var capturedPointerId: PointerId? = null
    // The initial pointer position for a track press, used to check if onTrackPress moved the
    // thumb under the pointer before transitioning into a thumb drag.
    private var trackPressPosition: Offset = Offset.Unspecified
    // Tracks the pointer's offset relative to the start edge of the thumb along the track
    // (0f to thumbLengthPx) at the start of an active drag gesture.
    private var dragStartOffsetInThumbPx = 0f
    private var thumbPressInteraction: PressInteraction.Press? = null
    private var thumbDragInteraction: DragInteraction.Start? = null
    private var thumbHoverInteraction: HoverInteraction.Enter? = null
    // Only allocate and collect when a hoisted thumbInteractionSource is provided; drives thumb
    // state colors without interfering with auto-fade visibility.
    private val interactionTracker: InteractionTracker? =
        if (thumbInteractionSource != null) {
            object :
                InteractionTracker(
                    this@InteractiveScrollbarNode,
                    Hover or Press or Drag,
                ) {
                override fun onChanged() = invalidateDraw()
            }
        } else {
            null
        }
    private var isHoveringTrack = false
    private var lastHoverPosition: Offset = Offset.Unspecified

    // Cached bounds calculated during draw() to be reused in onPointerEvent()
    private var trackOffset: Offset = Offset.Unspecified
    private var trackSize: Size = Size.Zero
    private var thumbSize: Size = Size.Zero

    private val trackLengthPx: Float
        get() = if (orientation == Orientation.Vertical) trackSize.height else trackSize.width

    private val thumbLengthPx: Float
        get() = if (orientation == Orientation.Vertical) thumbSize.height else thumbSize.width

    private val maxTrackOffsetPx: Float
        get() = trackLengthPx - thumbLengthPx

    private fun resetCachedBounds() {
        trackOffset = Offset.Unspecified
        trackSize = Size.Zero
        thumbSize = Size.Zero
    }

    // Computed dynamically from state rather than cached in draw() because onTrackPress may update
    // state.scrollOffset and a subsequent pointer move can arrive before the next draw() pass.
    private fun currentThumbStartOnTrack(): Float {
        val maxOffset = state.contentSize - state.viewportSize
        return if (maxOffset > 0) {
            (state.scrollOffset.toFloat() / maxOffset).fastCoerceIn(0f, 1f) * maxTrackOffsetPx
        } else {
            0f
        }
    }

    private fun isPointInTrack(position: Offset): Boolean =
        position.x >= trackOffset.x &&
            position.x < trackOffset.x + trackSize.width &&
            position.y >= trackOffset.y &&
            position.y < trackOffset.y + trackSize.height

    private fun isPointInThumb(position: Offset): Boolean {
        if (!isPointInTrack(position)) return false
        val pointerOnTrack = getTrackAxisPosition(position)
        val thumbStart = currentThumbStartOnTrack()
        return pointerOnTrack >= thumbStart && pointerOnTrack <= thumbStart + thumbLengthPx
    }

    /**
     * Brings the scrollbar back to fully visible while it's being interacted with, cancelling any
     * fade out that is scheduled or in progress.
     */
    private fun fadeIn() {
        val currentAlpha = alpha ?: return
        if (!isFadeEnabled) return

        // Fade out isn't scheduled, and it's animating back to visible.
        if (fadeJob == null && currentAlpha.targetValue == 1f) return

        fadeJob?.cancel()
        fadeJob = null
        coroutineScope.launch { currentAlpha.snapTo(1f) }
    }

    /**
     * Schedules the scrollbar to fade out once the user is no longer interacting with it, i.e. no
     * pointer is captured and neither the thumb nor the track is hovered.
     *
     * Calling this again while a fade out is pending restarts the delay rather than scheduling a
     * second one.
     */
    private fun fadeOut() {
        if (capturedPointerId == null && thumbHoverInteraction == null && !isHoveringTrack) {
            scheduleFadeOut()
        }
    }

    override fun onAttach() {
        super.onAttach()
        lastOffset = state.scrollOffset
        thumbInteractionSource?.let { interactionTracker?.track(it) }
        // Needed for node reuse: onReuse() skips draw invalidation when inputs are unchanged, and
        // replacing alpha in onAttach() doesn't invalidate the previous draw pass.
        if (isFadeEnabled) invalidateDraw()
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        val changes = pointerEvent.changes
        if (changes.isEmpty()) return

        // Process scrollbar hover and gestures on the  Initial pass. This ensures the scrollbar
        // gets first priority to intercept and consume pointer events before the underlying
        // scrollable content consumes these in the Main pass.
        if (pass == PointerEventPass.Initial) {
            val type = pointerEvent.type

            // Since this node spans the entire list container, we must intercept `Move` events to
            // manually check against the thumb's bounds.
            if (type == PointerEventType.Enter || type == PointerEventType.Move) {
                // Filter out any down (pressed) pointers to ensure hover effects are only triggered
                // by hover movements and never by touch drags or pressed mouse drags.
                val hoverChange = changes.fastFirstOrNull { !it.pressed }
                if (hoverChange != null) {
                    val pos = hoverChange.position
                    val inTrack = isPointInTrack(pos)
                    if (inTrack) {
                        lastHoverPosition = pos
                        if (!isHoveringTrack) {
                            isHoveringTrack = true
                            fadeIn()
                        }
                        updateThumbHover(pos, invalidateDraw = true)
                    } else {
                        lastHoverPosition = Offset.Unspecified
                        if (thumbHoverInteraction != null) {
                            emitHoverExit()
                            invalidateDraw()
                        }
                        if (isHoveringTrack) {
                            isHoveringTrack = false
                            fadeOut()
                        }
                    }
                }
            } else if (type == PointerEventType.Exit) {
                // Remove hover effects if the cursor leaves the node entirely
                lastHoverPosition = Offset.Unspecified
                if (thumbHoverInteraction != null) {
                    emitHoverExit()
                    invalidateDraw()
                }
                if (isHoveringTrack) {
                    isHoveringTrack = false
                    fadeOut()
                }
            }

            // Process press and drag gestures.
            if (capturedPointerId == null) {
                // Ignore touch events when the scrollbar is hidden/faded-out (non-touch inputs wake
                // the scrollbar on hover first; also check isHoveringTrack in case a press arrives
                // in the same frame before fadeIn() snaps alpha).
                val currentAlpha = alpha?.value ?: 1f
                if (currentAlpha > 0f || isHoveringTrack) {
                    // For mouse input, only respond to the primary button.
                    val downChange = changes.fastFirstOrNull {
                        !it.isConsumed &&
                            it.changedToDown() &&
                            it.isPrimaryPressed(pointerEvent) &&
                            isPointInTrack(it.position)
                    }
                    if (downChange != null) {
                        // Capture the pointer for both thumb drags and track clicks.
                        // This allows the user to click the track to jump, and then seamlessly
                        // drag the mouse without releasing to continue scrubbing the thumb.
                        capturedPointerId = downChange.id
                        handleDownEvent(downChange)
                    }
                }
            } else {
                // Always track an already-captured pointer regardless of alpha so its release or
                // cancellation (e.g. if consumed by an ancestor in Initial pass) is never missed.
                val activeChange = changes.fastFirstOrNull { it.id == capturedPointerId }
                if (activeChange == null || activeChange.isConsumed) {
                    releasePointerAndInteractions(isCancelled = true)
                } else if (!activeChange.isPrimaryPressed(pointerEvent)) {
                    releasePointerAndInteractions(isCancelled = false)
                } else if (activeChange.positionChanged()) {
                    processThumbDragMove(activeChange)
                }
            }
        }
    }

    // For mouse input, only treat the pointer as pressed when the primary button is held.
    private fun PointerInputChange.isPrimaryPressed(event: PointerEvent): Boolean =
        pressed && (type != PointerType.Mouse || event.buttons.isPrimaryPressed)

    private fun emitPress(position: Offset) {
        val press = PressInteraction.Press(position)
        thumbPressInteraction = press
        thumbInteractionSource?.let { source -> coroutineScope.launch { source.emit(press) } }
    }

    private fun emitDragStart() {
        if (thumbDragInteraction == null) {
            val start = DragInteraction.Start()
            thumbDragInteraction = start
            thumbInteractionSource?.let { source -> coroutineScope.launch { source.emit(start) } }
        }
    }

    /** Converts a physical pointer coordinate into the position along the track axis. */
    private fun getTrackAxisPosition(position: Offset): Float {
        val reverseDirection =
            orientation == Orientation.Horizontal && requireLayoutDirection() == LayoutDirection.Rtl
        val trackAxisPosition =
            if (orientation == Orientation.Vertical) {
                position.y - trackOffset.y
            } else {
                position.x - trackOffset.x
            }
        return if (reverseDirection) {
            trackLengthPx - trackAxisPosition
        } else {
            trackAxisPosition
        }
    }

    /** Handles a pointer down event on the scrollbar thumb or track. */
    private fun handleDownEvent(change: PointerInputChange) {
        fadeIn()
        change.consume()
        if (isPointInThumb(change.position)) {
            emitPress(change.position)
            // When grabbing the thumb, record the pointer offset relative to the thumb's start edge
            val pointerOnTrack = getTrackAxisPosition(change.position)
            val thumbStartOnTrack = currentThumbStartOnTrack()
            dragStartOffsetInThumbPx =
                (pointerOnTrack - thumbStartOnTrack).fastCoerceIn(0f, thumbLengthPx)
            invalidateDraw()
        } else {
            trackPressPosition = change.position
            jumpToTrackPosition(change.position)
        }
    }

    /** Propagates the gesture delta during an active thumb drag event. */
    private fun processThumbDragMove(change: PointerInputChange) {
        change.consume()
        // If the gesture started as a track press, only start dragging the thumb if onTrackPress
        // moved the thumb under the initial press position.
        if (
            thumbPressInteraction == null &&
                thumbDragInteraction == null &&
                !isPointInThumb(trackPressPosition)
        ) {
            return
        }
        fadeIn()
        if (thumbDragInteraction == null) {
            if (thumbPressInteraction == null) {
                // Recompute grab offset from the actual thumb position after onTrackPress (e.g. if
                // the thumb clamped at a track end rather than centering under trackPressPosition).
                dragStartOffsetInThumbPx =
                    (getTrackAxisPosition(trackPressPosition) - currentThumbStartOnTrack())
                        .fastCoerceIn(0f, thumbLengthPx)
            }
            emitDragStart()
            invalidateDraw()
        }
        val pointerOnTrack = getTrackAxisPosition(change.position)
        val targetThumbOffsetPx =
            (pointerOnTrack - dragStartOffsetInThumbPx).fastCoerceIn(0f, maxTrackOffsetPx)

        // Normalize the thumb position to a fraction in [0f, 1f]
        val positionFraction =
            if (maxTrackOffsetPx > 0f) targetThumbOffsetPx / maxTrackOffsetPx else 0f

        onThumbDrag(positionFraction)
    }

    /**
     * Computes the clamped start offset of the thumb on the track for a given pointer coordinate,
     * centering the thumb over the coordinate.
     */
    private fun clampThumbCenteredTo(position: Offset): Float {
        val trackAxisPosition = getTrackAxisPosition(position)
        return (trackAxisPosition - thumbLengthPx / 2f).fastCoerceIn(0f, maxTrackOffsetPx)
    }

    /** Jumps the thumb to the pointer's location for a track press event. */
    private fun jumpToTrackPosition(position: Offset) {
        // Find the thumb start offset that centers the thumb over the press position on track
        val trackPressOffset = clampThumbCenteredTo(position)

        // Normalize the thumb position to a fraction in [0f, 1f]
        val positionFraction =
            if (maxTrackOffsetPx > 0f) trackPressOffset / maxTrackOffsetPx else 0f

        onTrackPress(positionFraction)
    }

    /**
     * Releases the captured pointer, finishes or cancels active interactions, and schedules a
     * fade-out.
     */
    private fun releasePointerAndInteractions(isCancelled: Boolean) {
        releaseDragAndPress(isCancelled)
        capturedPointerId = null
        trackPressPosition = Offset.Unspecified
        dragStartOffsetInThumbPx = 0f
        if (isCancelled) {
            lastHoverPosition = Offset.Unspecified
            if (thumbHoverInteraction != null) {
                emitHoverCancel()
            }
            isHoveringTrack = false
        }
        invalidateDraw()
        fadeOut()
    }

    private fun emitCancelInteraction(interaction: Interaction) {
        val source = thumbInteractionSource ?: return
        // Launch to keep cancellations ordered after start interactions emitted in the same frame,
        // falling back to tryEmit if coroutineScope is cancelled on detach before emit() completes.
        val handler =
            coroutineScope.coroutineContext[Job]?.invokeOnCompletion { source.tryEmit(interaction) }
        coroutineScope.launch {
            source.emit(interaction)
            handler?.dispose()
        }
    }

    private fun releaseDragAndPress(isCancelled: Boolean) {
        val drag = thumbDragInteraction
        val press = thumbPressInteraction
        if (drag != null || press != null) {
            val source = thumbInteractionSource
            if (source != null) {
                if (isCancelled) {
                    drag?.let { emitCancelInteraction(DragInteraction.Cancel(it)) }
                    press?.let { emitCancelInteraction(PressInteraction.Cancel(it)) }
                } else {
                    coroutineScope.launch {
                        drag?.let { source.emit(DragInteraction.Stop(it)) }
                        press?.let { source.emit(PressInteraction.Release(it)) }
                    }
                }
            }
        }
        thumbDragInteraction = null
        thumbPressInteraction = null
    }

    private fun updateThumbHover(position: Offset, invalidateDraw: Boolean) {
        val inThumb = isPointInThumb(position)
        if (inThumb && thumbHoverInteraction == null) {
            emitHoverEnter()
            if (invalidateDraw) invalidateDraw()
        } else if (!inThumb && thumbHoverInteraction != null) {
            emitHoverExit()
            if (invalidateDraw) invalidateDraw()
        }
    }

    private fun emitHoverEnter() {
        if (thumbHoverInteraction == null) {
            val enter = HoverInteraction.Enter()
            thumbHoverInteraction = enter
            thumbInteractionSource?.let { source -> coroutineScope.launch { source.emit(enter) } }
        }
    }

    private fun emitHoverExit() {
        val hover = thumbHoverInteraction
        if (hover != null) {
            thumbInteractionSource?.let { source ->
                coroutineScope.launch { source.emit(HoverInteraction.Exit(hover)) }
            }
            thumbHoverInteraction = null
        }
    }

    private fun emitHoverCancel() {
        val hover = thumbHoverInteraction
        if (hover != null) {
            // Avoid allocating HoverInteraction.Exit when no thumbInteractionSource is provided.
            if (thumbInteractionSource != null) {
                emitCancelInteraction(HoverInteraction.Exit(hover))
            }
            thumbHoverInteraction = null
        }
    }

    override fun onCancelPointerInput() {
        releasePointerAndInteractions(isCancelled = true)
    }

    override fun onDetach() {
        super.onDetach()
        releaseDragAndPress(isCancelled = true)
        emitHoverCancel()
        capturedPointerId = null
        trackPressPosition = Offset.Unspecified
        dragStartOffsetInThumbPx = 0f
        isHoveringTrack = false
        lastHoverPosition = Offset.Unspecified
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        val contentSize = state.contentSize.toFloat()
        val viewportSize = state.viewportSize.toFloat()
        if (contentSize <= viewportSize || viewportSize == 0f || contentSize == 0f) {
            // Reset cached hit-test bounds so stale bounds don't intercept pointer events if
            // content shrinks dynamically to fit the viewport.
            resetCachedBounds()
            return
        }

        val currentOffset = state.scrollOffset
        if (currentOffset != lastOffset) {
            lastOffset = currentOffset
            // Re-evaluate thumb hover against the last known cursor position in case the thumb
            // scrolled into or out from under a stationary pointer (e.g. via mouse wheel).
            if (isHoveringTrack && capturedPointerId == null && lastHoverPosition.isSpecified) {
                updateThumbHover(lastHoverPosition, invalidateDraw = false)
            }
            fadeOut()
        }

        val currentAlpha = alpha?.value ?: 1f
        val tracker = interactionTracker
        val isHovered = tracker?.isHovered ?: (thumbHoverInteraction != null)
        val isPressed = tracker?.isPressed ?: (thumbPressInteraction != null)
        val isDragged = tracker?.isDragged ?: (thumbDragInteraction != null)
        val currentThumbColor =
            colors.thumbColor(isHovered = isHovered, isPressed = isPressed, isDragged = isDragged)
        val currentTrackColor = colors.trackColor
        val resolvedThumbColor =
            currentThumbColor.copy(alpha = currentThumbColor.alpha * currentAlpha)
        val resolvedTrackColor =
            currentTrackColor.copy(alpha = currentTrackColor.alpha * currentAlpha)

        // Calculate Geometry
        val thicknessPx = thickness.toPx()

        val mainAxisTrackInsetPx = mainAxisTrackInset.toPx()
        val crossAxisTrackInsetPx = crossAxisTrackInset.toPx()

        val trackLengthPx =
            if (orientation == Orientation.Vertical) {
                size.height - 2 * mainAxisTrackInsetPx
            } else {
                size.width - 2 * mainAxisTrackInsetPx
            }

        val minThumbLengthPx = thumbMinLength.toPx()
        // Skip drawing if the track length is too small to accommodate the min thumb length.
        if (trackLengthPx <= 0f || trackLengthPx < minThumbLengthPx) {
            resetCachedBounds()
            return
        }

        val maxThumbLengthPx =
            (trackLengthPx * thumbMaxLengthFraction).fastCoerceAtLeast(minThumbLengthPx)
        val thumbLengthFraction = viewportSize / contentSize
        val thumbLengthPx =
            (trackLengthPx * thumbLengthFraction).coerceIn(minThumbLengthPx, maxThumbLengthPx)

        val maxOffset = contentSize - viewportSize
        val thumbOffsetValue =
            if (maxOffset > 0) {
                (currentOffset.toFloat() / maxOffset) * (trackLengthPx - thumbLengthPx)
            } else {
                0f
            }

        if (orientation == Orientation.Vertical) {
            val trackX =
                if (layoutDirection == LayoutDirection.Ltr) {
                    size.width - crossAxisTrackInsetPx - thicknessPx
                } else {
                    crossAxisTrackInsetPx
                }
            val thumbY = mainAxisTrackInsetPx + thumbOffsetValue

            // Calculate bounds for pointer input handling.
            trackOffset = Offset(trackX, mainAxisTrackInsetPx)
            trackSize = Size(thicknessPx, trackLengthPx)
            thumbSize = Size(thicknessPx, thumbLengthPx)

            if (cachedThicknessPx != thicknessPx) {
                cachedThicknessPx = thicknessPx
                cachedCornerRadius = CornerRadius(thicknessPx / 2f, thicknessPx / 2f)
            }

            val isTrackVisible = resolvedTrackColor.alpha > 0f
            val isThumbVisible = resolvedThumbColor.alpha > 0f
            if (!isTrackVisible && !isThumbVisible) return

            if (isTrackVisible) {
                drawRoundRect(
                    color = resolvedTrackColor,
                    topLeft = Offset(trackX, mainAxisTrackInsetPx),
                    size = Size(thicknessPx, trackLengthPx),
                )
            }

            if (isThumbVisible) {
                drawRoundRect(
                    color = resolvedThumbColor,
                    topLeft = Offset(trackX, thumbY),
                    size = Size(thicknessPx, thumbLengthPx),
                    cornerRadius = cachedCornerRadius,
                )
            }
        } else {
            val trackY = size.height - crossAxisTrackInsetPx - thicknessPx
            val thumbX =
                if (layoutDirection == LayoutDirection.Ltr) {
                    mainAxisTrackInsetPx + thumbOffsetValue
                } else {
                    size.width - mainAxisTrackInsetPx - thumbLengthPx - thumbOffsetValue
                }

            // Calculate bounds for pointer input handling.
            trackOffset = Offset(mainAxisTrackInsetPx, trackY)
            trackSize = Size(trackLengthPx, thicknessPx)
            thumbSize = Size(thumbLengthPx, thicknessPx)

            if (cachedThicknessPx != thicknessPx) {
                cachedThicknessPx = thicknessPx
                cachedCornerRadius = CornerRadius(thicknessPx / 2f, thicknessPx / 2f)
            }

            val isTrackVisible = resolvedTrackColor.alpha > 0f
            val isThumbVisible = resolvedThumbColor.alpha > 0f
            if (!isTrackVisible && !isThumbVisible) return

            if (isTrackVisible) {
                drawRoundRect(
                    color = resolvedTrackColor,
                    topLeft = Offset(mainAxisTrackInsetPx, trackY),
                    size = Size(trackLengthPx, thicknessPx),
                )
            }

            if (isThumbVisible) {
                drawRoundRect(
                    color = resolvedThumbColor,
                    topLeft = Offset(thumbX, trackY),
                    size = Size(thumbLengthPx, thicknessPx),
                    cornerRadius = cachedCornerRadius,
                )
            }
        }
    }
}
