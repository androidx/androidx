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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A scrollbar that represents the current scroll position of a scrolling component.
 *
 * This scrollbar cannot be interacted with to scroll the component, it is a visual only
 * representation of the scroll position. It is drawn at the end edge (respecting layout direction)
 * for a vertically scrollable component, and at the bottom edge for a horizontally scrollable
 * component.
 *
 * The scrollbar is only visible if the content size is strictly greater than the viewport size. By
 * default, the scrollbar is shown when scrolling is active and fades out after a delay when
 * scrolling stops. This fading behavior can be disabled by setting [isFadeEnabled] to false, in
 * which case the scrollbar remains always visible.
 *
 * To use a scrollbar with a [androidx.compose.foundation.lazy.LazyColumn], see:
 *
 * @sample androidx.compose.material3.samples.ScrollbarWithLazyColumnSample
 *
 * To use a scrollbar with a [androidx.compose.foundation.layout.Column] that uses
 * [androidx.compose.foundation.verticalScroll], see:
 *
 * @sample androidx.compose.material3.samples.ScrollbarWithVerticalScrollSample
 * @param state the [ScrollIndicatorState] that represents the scroll state. If null, the scrollbar
 *   will not be drawn.
 * @param orientation the orientation of the scrollbar.
 * @param thumbColor the color of the scrollbar thumb.
 * @param trackColor the color of the scrollbar track.
 * @param thickness the thickness of the scrollbar.
 * @param thumbMinLength the minimum length of the scrollbar thumb. Note that the scrollbar will not
 *   be shown if the available track length is less than this minimum length.
 * @param thumbMaxLengthFraction the maximum length of the scrollbar thumb as a fraction of the
 *   viewport length. This should be between 0f and 1f.
 * @param isFadeEnabled whether the fade behavior is enabled. If enabled, the scrollbar will be
 *   shown when scrolling is active, and fades out after a delay when scrolling is idle. If
 *   disabled, the scrollbar remains always visible.
 * @param fadeDurationMillis the duration of the fade animation in milliseconds.
 * @param fadeDelayMillis the delay before the scrollbar fades out in milliseconds.
 * @param mainAxisTrackInset the inset to apply to the scrollbar track along the main axis.
 * @param crossAxisTrackInset the inset to apply to the scrollbar track along the cross axis.
 */
@Composable
fun Modifier.scrollbar(
    state: ScrollIndicatorState?,
    orientation: Orientation,
    thumbColor: Color = ScrollbarDefaults.thumbColor,
    trackColor: Color = Color.Transparent,
    thickness: Dp = ScrollbarDefaults.Thickness,
    thumbMinLength: Dp = ScrollbarDefaults.ThumbMinLength,
    @FloatRange(from = 0.0, to = 1.0)
    thumbMaxLengthFraction: Float = ScrollbarDefaults.ThumbMaxLengthFraction,
    isFadeEnabled: Boolean = true,
    fadeDurationMillis: Int = ScrollbarDefaults.ThumbFadeDurationMillis,
    fadeDelayMillis: Int = ScrollbarDefaults.ThumbFadeDelayMillis,
    mainAxisTrackInset: Dp = ScrollbarDefaults.MainAxisTrackInset,
    crossAxisTrackInset: Dp = ScrollbarDefaults.CrossAxisTrackInset,
): Modifier {
    if (state == null) return this
    require(thumbMaxLengthFraction in 0f..1f) { "thumbMaxLengthFraction must be between 0f and 1f" }
    return this.then(
        ScrollbarElement(
            state = state,
            orientation = orientation,
            thumbColor = thumbColor,
            trackColor = trackColor,
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

/** Contains the default values used by [Modifier.scrollbar]. */
object ScrollbarDefaults {
    /** Default opacity for the scrollbar thumb. */
    val ThumbOpacity = 0.7f

    /** Default duration in milliseconds for the fade animation. */
    val ThumbFadeDurationMillis: Int = 250

    /** Default delay in milliseconds before the scrollbar fades out. */
    val ThumbFadeDelayMillis: Int = 400

    /** Default color for the scrollbar thumb. */
    val thumbColor: Color
        @Composable get() = MaterialTheme.colorScheme.outline.copy(alpha = ThumbOpacity)

    /** Default thickness for a scrollbar. */
    val Thickness: Dp = 4.dp

    /** Default minimum height for the scrollbar thumb. */
    val ThumbMinLength: Dp = 24.dp

    /** Default maximum length for the scrollbar thumb as a fraction of the viewport length. */
    val ThumbMaxLengthFraction: Float = 0.9f

    /** Default main axis inset for the scrollbar track. */
    val MainAxisTrackInset: Dp = 2.dp

    /** Default cross axis inset for the scrollbar track. */
    val CrossAxisTrackInset: Dp = 0.dp
}

private class ScrollbarElement(
    private val state: ScrollIndicatorState,
    private val orientation: Orientation,
    private val thumbColor: Color,
    private val trackColor: Color,
    private val thickness: Dp,
    private val thumbMinLength: Dp,
    private val thumbMaxLengthFraction: Float,
    private val isFadeEnabled: Boolean,
    private val fadeDurationMillis: Int,
    private val fadeDelayMillis: Int,
    private val mainAxisTrackInset: Dp,
    private val crossAxisTrackInset: Dp,
) : ModifierNodeElement<ScrollbarNodeWrapper>() {

    override fun create(): ScrollbarNodeWrapper {
        return ScrollbarNodeWrapper(
            state,
            orientation,
            thumbColor,
            trackColor,
            thickness,
            thumbMinLength,
            thumbMaxLengthFraction,
            isFadeEnabled,
            fadeDurationMillis,
            fadeDelayMillis,
            mainAxisTrackInset,
            crossAxisTrackInset,
        )
    }

    override fun update(node: ScrollbarNodeWrapper) {
        node.update(
            state,
            orientation,
            thumbColor,
            trackColor,
            thickness,
            thumbMinLength,
            thumbMaxLengthFraction,
            isFadeEnabled,
            fadeDurationMillis,
            fadeDelayMillis,
            mainAxisTrackInset,
            crossAxisTrackInset,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScrollbarElement) return false
        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (thumbColor != other.thumbColor) return false
        if (trackColor != other.trackColor) return false
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
        result = 31 * result + thumbColor.hashCode()
        result = 31 * result + trackColor.hashCode()
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
        name = "scrollbar"
        properties["state"] = state
        properties["orientation"] = orientation
        properties["thumbColor"] = thumbColor
        properties["trackColor"] = trackColor
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

internal class ScrollbarNodeWrapper(
    private var state: ScrollIndicatorState,
    private var orientation: Orientation,
    private var thumbColor: Color,
    private var trackColor: Color,
    private var thickness: Dp,
    private var thumbMinLength: Dp,
    private var thumbMaxLengthFraction: Float,
    private var isFadeEnabled: Boolean,
    private var fadeDurationMillis: Int,
    private var fadeDelayMillis: Int,
    private var mainAxisTrackInset: Dp,
    private var crossAxisTrackInset: Dp,
) : DelegatingNode() {

    private var scrollbarNode =
        delegate(
            createScrollbarModifierNode(
                state = state,
                orientation = orientation,
                thumbColor = { thumbColor },
                trackColor = { trackColor },
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
        thumbColor: Color,
        trackColor: Color,
        thickness: Dp,
        thumbMinLength: Dp,
        thumbMaxLengthFraction: Float,
        isFadeEnabled: Boolean,
        fadeDurationMillis: Int,
        fadeDelayMillis: Int,
        mainAxisTrackInset: Dp,
        crossAxisTrackInset: Dp,
    ) {
        this.state = state
        this.orientation = orientation
        this.thumbColor = thumbColor
        this.trackColor = trackColor
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
                createScrollbarModifierNode(
                    state = state,
                    orientation = orientation,
                    thumbColor = { thumbColor },
                    trackColor = { trackColor },
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

/**
 * Creates a [DelegatableNode] that adds a visual-only scrollbar to a scrollable component.
 *
 * This scrollbar cannot be interacted with to scroll the component, it is a visual only
 * representation of the scroll position. It is drawn at the end edge (respecting layout direction)
 * for a vertically scrollable component, and at the bottom edge for a horizontally scrollable
 * component.
 *
 * The scrollbar is only visible if the content size is strictly greater than the viewport size. By
 * default, the scrollbar is shown when scrolling is active and fades out after a delay when
 * scrolling stops. This fading behavior can be disabled by setting [isFadeEnabled] to false, in
 * which case the scrollbar remains always visible.
 *
 * @param state the [ScrollIndicatorState] that represents the scroll state.
 * @param orientation the orientation of the scrollbar.
 * @param thumbColor a [ColorProducer] for the thumb color.
 * @param trackColor a [ColorProducer] for the track color.
 * @param thickness the thickness of the scrollbar.
 * @param thumbMinLength the minimum length of the scrollbar thumb. Note that the scrollbar will not
 *   be shown if the available track length is less than this minimum length.
 * @param thumbMaxLengthFraction the maximum length of the scrollbar thumb as a fraction of the
 *   viewport length. This should be between 0f and 1f.
 * @param isFadeEnabled whether the fade behavior is enabled. If enabled, the scrollbar will be
 *   shown when scrolling is active, and fades out after a delay when scrolling is idle. If
 *   disabled, the scrollbar remains always visible.
 * @param fadeAnimationSpec the animation spec for the fade animation.
 * @param fadeDelayMillis the delay before the scrollbar fades out in milliseconds.
 * @param mainAxisTrackInset the inset to apply to the scrollbar track along the main axis.
 * @param crossAxisTrackInset the inset to apply to the scrollbar track along the cross axis.
 */
// TODO: b/505077759 - Publish this as a public API in some lower layer.
internal fun createScrollbarModifierNode(
    state: ScrollIndicatorState,
    orientation: Orientation,
    thumbColor: ColorProducer,
    trackColor: ColorProducer,
    thickness: Dp,
    thumbMinLength: Dp,
    @FloatRange(from = 0.0, to = 1.0) thumbMaxLengthFraction: Float,
    isFadeEnabled: Boolean,
    fadeAnimationSpec: FiniteAnimationSpec<Float>,
    fadeDelayMillis: Int,
    mainAxisTrackInset: Dp,
    crossAxisTrackInset: Dp,
): DelegatableNode =
    ScrollbarNode(
        state,
        orientation,
        thumbColor,
        trackColor,
        thickness,
        thumbMinLength,
        thumbMaxLengthFraction,
        isFadeEnabled,
        fadeAnimationSpec,
        fadeDelayMillis,
        mainAxisTrackInset,
        crossAxisTrackInset,
    )

private class ScrollbarNode(
    private val state: ScrollIndicatorState,
    private val orientation: Orientation,
    private val thumbColor: ColorProducer,
    private val trackColor: ColorProducer,
    private val thickness: Dp,
    private val thumbMinLength: Dp,
    private val thumbMaxLengthFraction: Float,
    private val isFadeEnabled: Boolean,
    private val fadeAnimationSpec: FiniteAnimationSpec<Float>,
    private val fadeDelayMillis: Int,
    private val mainAxisTrackInset: Dp,
    private val crossAxisTrackInset: Dp,
) : DelegatingNode(), DrawModifierNode {
    private val alpha = Animatable(if (isFadeEnabled) 0f else 1f)
    private var cachedThicknessPx = -1f
    private var cachedCornerRadius = CornerRadius.Zero
    private var lastOffset = -1
    private var fadeJob: Job? = null

    override fun onDetach() {
        fadeJob?.cancel()
        fadeJob = null
        lastOffset = -1
        super.onDetach()
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        val contentSize = state.contentSize.toFloat()
        val viewportSize = state.viewportSize.toFloat()

        // Skip drawing the scrollbar if the content fits the viewport or sizes are invalid.
        // Also avoids reading scrollOffset when not needed, preventing redundant invalidations.
        if (contentSize <= viewportSize || viewportSize == 0f || contentSize == 0f) {
            return
        }

        val currentOffset = state.scrollOffset
        if (isFadeEnabled && currentOffset != lastOffset) {
            lastOffset = currentOffset

            fadeJob?.cancel()
            fadeJob =
                coroutineScope.launch {
                    if (alpha.value != 1f) {
                        alpha.snapTo(1f)
                    }

                    // We use coroutine delay instead of AnimationSpec delay because system
                    // animations might be disabled (MotionDurationScale = 0f). A coroutine delay
                    // ensures the scrollbar remains visible for the configured fade delay period
                    // before fading out, whereas an AnimationSpec's delay would scale to 0f and
                    // cause the scrollbar to disappear instantly.
                    delay(fadeDelayMillis.milliseconds)
                    alpha.animateTo(0f, fadeAnimationSpec)
                }
        }

        val currentAlpha = alpha.value
        val currentThumbColor = thumbColor()
        val currentTrackColor = trackColor()
        val resolvedThumbColor =
            currentThumbColor.copy(alpha = currentThumbColor.alpha * currentAlpha)
        val resolvedTrackColor =
            currentTrackColor.copy(alpha = currentTrackColor.alpha * currentAlpha)

        val isTrackVisible = resolvedTrackColor.alpha > 0f
        val isThumbVisible = resolvedThumbColor.alpha > 0f

        if (!isTrackVisible && !isThumbVisible) {
            return
        }

        // Calculate geometry
        val offset = currentOffset.toFloat()

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
            return
        }

        val maxThumbLengthPx = trackLengthPx * thumbMaxLengthFraction

        val thicknessPx = thickness.toPx()
        val thumbLengthFraction = viewportSize / contentSize
        val thumbLengthPx =
            (trackLengthPx * thumbLengthFraction).coerceIn(minThumbLengthPx, maxThumbLengthPx)

        val maxOffset = contentSize - viewportSize
        val thumbOffset =
            if (maxOffset > 0) {
                (offset / maxOffset) * (trackLengthPx - thumbLengthPx)
            } else {
                0f
            }

        if (thicknessPx != cachedThicknessPx) {
            cachedThicknessPx = thicknessPx
            cachedCornerRadius = CornerRadius(thicknessPx / 2, thicknessPx / 2)
        }
        val cornerRadius = cachedCornerRadius

        // Draw track and thumb
        if (orientation == Orientation.Vertical) {
            val trackX =
                if (layoutDirection == LayoutDirection.Ltr) {
                    size.width - crossAxisTrackInsetPx - thicknessPx
                } else {
                    crossAxisTrackInsetPx
                }
            val thumbY = mainAxisTrackInsetPx + thumbOffset

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
                    cornerRadius = cornerRadius,
                )
            }
        } else {
            val trackY = size.height - crossAxisTrackInsetPx - thicknessPx
            val thumbX =
                if (layoutDirection == LayoutDirection.Ltr) {
                    mainAxisTrackInsetPx + thumbOffset
                } else {
                    size.width - mainAxisTrackInsetPx - thumbLengthPx - thumbOffset
                }

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
                    cornerRadius = cornerRadius,
                )
            }
        }
    }
}
