/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import android.content.Context
import android.graphics.RenderNode
import android.os.Build
import android.widget.EdgeEffect
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.EdgeEffectCompat.distanceCompat
import androidx.compose.foundation.EdgeEffectCompat.onAbsorbCompat
import androidx.compose.foundation.EdgeEffectCompat.onPullDistanceCompat
import androidx.compose.foundation.EdgeEffectCompat.onReleaseWithOppositeDelta
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal actual fun rememberOverscrollEffect(): OverscrollEffect {
    val context = LocalContext.current
    val config = LocalOverscrollConfiguration.current
    return if (config != null) {
        remember(context, config) { AndroidEdgeEffectOverscrollEffect(context, config) }
    } else {
        NoOpOverscrollEffect
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private class DrawStretchOverscrollModifier(
    private val overscrollEffect: AndroidEdgeEffectOverscrollEffect,
    private val edgeEffectWrapper: EdgeEffectWrapper,
    inspectorInfo: InspectorInfo.() -> Unit
) : DrawModifier, InspectorValueInfo(inspectorInfo) {

    /**
     * There is an unwanted behavior in the stretch overscroll effect we have to workaround:
     * when the effect is started it is getting the current RenderNode bounds and clips the content
     * by those bounds. Even if this RenderNode is not configured to do clipping. Or if it clips,
     * but not within its bounds, but by the outline provided which could have a completely
     * different bounds. That is what happens with our scrolling containers - they all clip by the
     * rect which is larger than the RenderNode bounds in order to not clip the shadows drawn in
     * the cross axis of the scrolling direction. This issue is not that visible in the Views world
     * because Views do clip by default. So adding one more clip doesn't change much. Thus why the
     * whole shadows mechanism in the Views world works differently, the shadows are drawn not
     * in-place, but with the background of the first parent which has a background.
     *
     * To solve this we need to render into a larger area, either by creating a larger layer for the
     * child to draw in, or by manually rendering the stretch into a larger RenderNode, and then
     * drawing that RenderNode into the existing layer. The difficulty here is that we only want to
     * extend the cross axis / clip the main axis (scrolling containers do this already), otherwise
     * the extra layer space will be transformed by the stretch, which results in an incorrect
     * effect that can also end up revealing content underneath the scrolling container, as we
     * stretch the transparent pixels in the extra space. For this to work we would need to know
     * the stretch direction at layer creation time (i.e, placeWithLayer inside placement), but
     * [OverscrollEffect] has no knowledge of directionality until an event is received. Creating a
     * larger layer in this way is also more expensive and requires more parts, as we have to use
     * two layout modifiers to achieve the desired effect.
     *
     * As a result we instead create a RenderNode that we extend in the cross-axis direction by
     * [MaxSupportedElevation] on each side, to allow for non-clipped space without affecting layer
     * size. We then draw the content (translated to be centered) and apply the stretch into this
     * larger RenderNode, and then draw the RenderNode back into the original canvas (translated
     * back to balance the previous translation), allowing for any shadows / other content drawn
     * outside the cross-axis bounds to be unclipped by the RenderNode stretch.
     */
    private var _renderNode: RenderNode? = null
    private val renderNode
        get() = _renderNode ?: RenderNode("AndroidEdgeEffectOverscrollEffect")
            .also { _renderNode = it }

    @Suppress("KotlinConstantConditions")
    override fun ContentDrawScope.draw() {
        overscrollEffect.updateSize(size)
        if (size.isEmpty()) {
            // Draw any out of bounds content
            drawContent()
            return
        }
        overscrollEffect.redrawSignal.value // <-- value read to redraw if needed
        val maxElevation = MaxSupportedElevation.toPx()
        val canvas = drawContext.canvas.nativeCanvas
        var needsInvalidate = false
        with(edgeEffectWrapper) {
            val shouldDrawVerticalStretch = shouldDrawVerticalStretch()
            val shouldDrawHorizontalStretch = shouldDrawHorizontalStretch()
            when {
                // Drawing in both directions, so we need to match canvas size and essentially clip
                // both directions. We don't need the renderNode in this case, but it would
                // complicate the rest of the drawing logic.
                shouldDrawVerticalStretch && shouldDrawHorizontalStretch ->
                    renderNode.setPosition(0, 0, canvas.width, canvas.height)
                // Drawing vertical stretch, so expand the width to prevent clipping
                shouldDrawVerticalStretch ->
                    renderNode.setPosition(
                        0,
                        0,
                        canvas.width + (maxElevation.roundToInt() * 2),
                        canvas.height
                    )
                // Drawing horizontal stretch, so expand the height to prevent clipping
                shouldDrawHorizontalStretch ->
                    renderNode.setPosition(
                        0,
                        0,
                        canvas.width,
                        canvas.height + (maxElevation.roundToInt() * 2)
                    )
                // Not drawing any stretch, so early return - we can draw into the existing canvas
                else -> {
                    drawContent()
                    return
                }
            }
            val recordingCanvas = renderNode.beginRecording()
            // Views call RenderNode.clearStretch() (@hide API) to reset the stretch as part of
            // the draw pass. We can't call this API, so by default the stretch would just keep on
            // increasing for each new delta. Instead, to work around this, we can effectively
            // 'negate' the previously rendered stretch by applying it, rotated 180 degrees, which
            // cancels out the stretch applied to the RenderNode by the real stretch. To do this,
            // we pull the negated stretch by the distance of the real stretch amount in each draw
            // frame. Then in the next draw frame, we draw the negated stretch first, and then
            // finish it so we can pull it by the real effect's distance again.
            // Note that `draw` here isn't really drawing anything, it's applying a stretch to the
            // whole RenderNode, so we can't clip / translate the drawing region here.
            if (isLeftNegationStretched()) {
                val leftEffectNegation = getOrCreateLeftEffectNegation()
                // Invert the stretch
                drawRightStretch(leftEffectNegation, recordingCanvas)
                leftEffectNegation.finish()
            }
            if (isLeftAnimating()) {
                val leftEffect = getOrCreateLeftEffect()
                needsInvalidate = drawLeftStretch(leftEffect, recordingCanvas) || needsInvalidate
                if (isLeftStretched()) {
                    // Displacement isn't currently used in AOSP for stretch, but provide the same
                    // displacement in case any OEMs have custom behavior.
                    val displacementY = overscrollEffect.displacement().y
                    getOrCreateLeftEffectNegation()
                        .onPullDistanceCompat(leftEffect.distanceCompat, 1 - displacementY)
                }
            }
            if (isTopNegationStretched()) {
                val topEffectNegation = getOrCreateTopEffectNegation()
                // Invert the stretch
                drawBottomStretch(topEffectNegation, recordingCanvas)
                topEffectNegation.finish()
            }
            if (isTopAnimating()) {
                val topEffect = getOrCreateTopEffect()
                needsInvalidate = drawTopStretch(topEffect, recordingCanvas) || needsInvalidate
                if (isTopStretched()) {
                    // Displacement isn't currently used in AOSP for stretch, but provide the same
                    // displacement in case any OEMs have custom behavior.
                    val displacementX = overscrollEffect.displacement().x
                    getOrCreateTopEffectNegation()
                        .onPullDistanceCompat(topEffect.distanceCompat, displacementX)
                }
            }
            if (isRightNegationStretched()) {
                val rightEffectNegation = getOrCreateRightEffectNegation()
                // Invert the stretch
                drawLeftStretch(rightEffectNegation, recordingCanvas)
                rightEffectNegation.finish()
            }
            if (isRightAnimating()) {
                val rightEffect = getOrCreateRightEffect()
                needsInvalidate = drawRightStretch(rightEffect, recordingCanvas) || needsInvalidate
                if (isRightStretched()) {
                    // Displacement isn't currently used in AOSP for stretch, but provide the same
                    // displacement in case any OEMs have custom behavior.
                    val displacementY = overscrollEffect.displacement().y
                    getOrCreateRightEffectNegation()
                        .onPullDistanceCompat(rightEffect.distanceCompat, displacementY)
                }
            }
            if (isBottomNegationStretched()) {
                val bottomEffectNegation = getOrCreateBottomEffectNegation()
                // Invert the stretch
                drawTopStretch(bottomEffectNegation, recordingCanvas)
                bottomEffectNegation.finish()
            }
            if (isBottomAnimating()) {
                val bottomEffect = getOrCreateBottomEffect()
                needsInvalidate =
                    drawBottomStretch(bottomEffect, recordingCanvas) || needsInvalidate
                if (isBottomStretched()) {
                    // Displacement isn't currently used in AOSP for stretch, but provide the same
                    // displacement in case any OEMs have custom behavior.
                    val displacementX = overscrollEffect.displacement().x
                    getOrCreateBottomEffectNegation()
                        .onPullDistanceCompat(bottomEffect.distanceCompat, 1 - displacementX)
                }
            }

            if (needsInvalidate) overscrollEffect.invalidateOverscroll()
            // Render the content for ContentDrawScope into the RenderNode, using the same size
            // provided by ContentDrawScope - we only want to prevent clipping, not actually
            // change the size of the content.
            // Since we expand the size of the RenderNode so we don't clip the cross-axis content,
            // we need to re-center the content in the RenderNode.
            // We 'clip' in the direction of the stretch, so in that case there is no extra space
            // and hence no need to translate. Otherwise, add the extra space.
            val left = if (shouldDrawHorizontalStretch) 0f else maxElevation
            val top = if (shouldDrawVerticalStretch) 0f else maxElevation
            val outerDraw = this@draw
            with(outerDraw) {
                draw(this, this.layoutDirection, Canvas(recordingCanvas), size) {
                    translate(left, top) {
                        // Since the stretch effect isn't really 'drawn', but is just set on
                        // the RenderNode, it doesn't really matter when we call this in terms of
                        // draw ordering.
                        outerDraw.drawContent()
                    }
                }
            }
            renderNode.endRecording()
            // Now we can draw the larger RenderNode inside the actual canvas - but we need to
            // translate it back by the amount we previously offset by inside the larger RenderNode.
            val restore = canvas.save()
            canvas.translate(-left, -top)
            canvas.drawRenderNode(renderNode)
            canvas.restoreToCount(restore)
        }
    }

    private fun shouldDrawVerticalStretch() = with(edgeEffectWrapper) {
        isTopAnimating() || isTopNegationStretched() ||
            isBottomAnimating() || isBottomNegationStretched()
    }
    private fun shouldDrawHorizontalStretch() = with(edgeEffectWrapper) {
        isLeftAnimating() || isLeftNegationStretched() ||
            isRightAnimating() || isRightNegationStretched()
    }

    private fun drawLeftStretch(left: EdgeEffect, canvas: NativeCanvas): Boolean {
        return drawWithRotation(rotationDegrees = 270f, edgeEffect = left, canvas = canvas)
    }

    private fun drawTopStretch(top: EdgeEffect, canvas: NativeCanvas): Boolean {
        return drawWithRotation(rotationDegrees = 0f, edgeEffect = top, canvas = canvas)
    }

    private fun drawRightStretch(right: EdgeEffect, canvas: NativeCanvas): Boolean {
        return drawWithRotation(rotationDegrees = 90f, edgeEffect = right, canvas = canvas)
    }

    private fun drawBottomStretch(bottom: EdgeEffect, canvas: NativeCanvas): Boolean {
        return drawWithRotation(rotationDegrees = 180f, edgeEffect = bottom, canvas = canvas)
    }

    private fun drawWithRotation(
        rotationDegrees: Float,
        edgeEffect: EdgeEffect,
        canvas: NativeCanvas
    ): Boolean {
        if (rotationDegrees == 0f) {
            val needsInvalidate = edgeEffect.draw(canvas)
            return needsInvalidate
        }
        val restore = canvas.save()
        canvas.rotate(rotationDegrees)
        val needsInvalidate = edgeEffect.draw(canvas)
        canvas.restoreToCount(restore)
        return needsInvalidate
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class DrawGlowOverscrollModifier(
    private val overscrollEffect: AndroidEdgeEffectOverscrollEffect,
    private val edgeEffectWrapper: EdgeEffectWrapper,
    private val overscrollConfig: OverscrollConfiguration,
    inspectorInfo: InspectorInfo.() -> Unit
) : DrawModifier, InspectorValueInfo(inspectorInfo) {

    @Suppress("KotlinConstantConditions")
    override fun ContentDrawScope.draw() {
        overscrollEffect.updateSize(size)
        if (size.isEmpty()) {
            // Draw any out of bounds content
            drawContent()
            return
        }
        drawContent()
        overscrollEffect.redrawSignal.value // <-- value read to redraw if needed
        val canvas = drawContext.canvas.nativeCanvas
        var needsInvalidate = false
        with(edgeEffectWrapper) {
            if (isLeftAnimating()) {
                val leftEffect = getOrCreateLeftEffect()
                needsInvalidate = drawLeftGlow(leftEffect, canvas) || needsInvalidate
            }
            if (isTopAnimating()) {
                val topEffect = getOrCreateTopEffect()
                needsInvalidate = drawTopGlow(topEffect, canvas) || needsInvalidate
            }
            if (isRightAnimating()) {
                val rightEffect = getOrCreateRightEffect()
                needsInvalidate = drawRightGlow(rightEffect, canvas) || needsInvalidate
            }
            if (isBottomAnimating()) {
                val bottomEffect = getOrCreateBottomEffect()
                needsInvalidate = drawBottomGlow(bottomEffect, canvas) || needsInvalidate
            }
            if (needsInvalidate) overscrollEffect.invalidateOverscroll()
        }
    }

    private fun DrawScope.drawLeftGlow(left: EdgeEffect, canvas: NativeCanvas): Boolean {
        val offset = Offset(
            -size.height,
            overscrollConfig.drawPadding.calculateLeftPadding(layoutDirection).toPx()
        )
        return drawWithRotationAndOffset(
            rotationDegrees = 270f,
            offset = offset,
            edgeEffect = left,
            canvas = canvas
        )
    }

    private fun DrawScope.drawTopGlow(top: EdgeEffect, canvas: NativeCanvas): Boolean {
        val offset = Offset(0f, overscrollConfig.drawPadding.calculateTopPadding().toPx())
        return drawWithRotationAndOffset(
            rotationDegrees = 0f,
            offset = offset,
            edgeEffect = top,
            canvas = canvas
        )
    }

    private fun DrawScope.drawRightGlow(right: EdgeEffect, canvas: NativeCanvas): Boolean {
        val width = size.width.roundToInt()
        val rightPadding = overscrollConfig.drawPadding.calculateRightPadding(layoutDirection)
        val offset = Offset(0f, -width.toFloat() + rightPadding.toPx())
        return drawWithRotationAndOffset(
            rotationDegrees = 90f,
            offset = offset,
            edgeEffect = right,
            canvas = canvas
        )
    }

    private fun DrawScope.drawBottomGlow(bottom: EdgeEffect, canvas: NativeCanvas): Boolean {
        val bottomPadding = overscrollConfig.drawPadding.calculateBottomPadding().toPx()
        val offset = Offset(-size.width, -size.height + bottomPadding)
        return drawWithRotationAndOffset(
            rotationDegrees = 180f,
            offset = offset,
            edgeEffect = bottom,
            canvas = canvas
        )
    }

    private fun drawWithRotationAndOffset(
        rotationDegrees: Float,
        offset: Offset,
        edgeEffect: EdgeEffect,
        canvas: NativeCanvas
    ): Boolean {
        val restore = canvas.save()
        canvas.rotate(rotationDegrees)
        canvas.translate(offset.x, offset.y)
        val needsInvalidate = edgeEffect.draw(canvas)
        canvas.restoreToCount(restore)
        return needsInvalidate
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class AndroidEdgeEffectOverscrollEffect(
    context: Context,
    overscrollConfig: OverscrollConfiguration
) : OverscrollEffect {
    private var pointerPosition: Offset? = null

    private val edgeEffectWrapper = EdgeEffectWrapper(
        context,
        glowColor = overscrollConfig.glowColor.toArgb()
    )

    internal val redrawSignal = mutableStateOf(Unit, neverEqualPolicy())

    @VisibleForTesting
    internal var invalidationEnabled = true

    private var scrollCycleInProgress: Boolean = false

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        // Early return
        if (containerSize.isEmpty()) {
            return performScroll(delta)
        }

        if (!scrollCycleInProgress) {
            stopOverscrollAnimation()
            scrollCycleInProgress = true
        }
        // Relax existing stretches if needed before performing scroll
        val consumedPixelsY = when {
            delta.y == 0f -> 0f
            edgeEffectWrapper.isTopStretched() -> {
                pullTop(delta).also {
                    // Release / reset state if we have fully relaxed the stretch
                    if (!edgeEffectWrapper.isTopStretched()) {
                        edgeEffectWrapper.getOrCreateTopEffect().onRelease()
                    }
                }
            }
            edgeEffectWrapper.isBottomStretched() -> {
                pullBottom(delta).also {
                    // Release / reset state if we have fully relaxed the stretch
                    if (!edgeEffectWrapper.isBottomStretched()) {
                        edgeEffectWrapper.getOrCreateBottomEffect().onRelease()
                    }
                }
            }
            else -> 0f
        }
        val consumedPixelsX = when {
            delta.x == 0f -> 0f
            edgeEffectWrapper.isLeftStretched() -> {
                pullLeft(delta).also {
                    // Release / reset state if we have fully relaxed the stretch
                    if (!edgeEffectWrapper.isLeftStretched()) {
                        edgeEffectWrapper.getOrCreateLeftEffect().onRelease()
                    }
                }
            }
            edgeEffectWrapper.isRightStretched() -> {
                pullRight(delta).also {
                    // Release / reset state if we have fully relaxed the stretch
                    if (!edgeEffectWrapper.isRightStretched()) {
                        edgeEffectWrapper.getOrCreateRightEffect().onRelease()
                    }
                }
            }
            else -> 0f
        }
        val consumedOffset = Offset(consumedPixelsX, consumedPixelsY)
        if (consumedOffset != Offset.Zero) invalidateOverscroll()

        val leftForDelta = delta - consumedOffset
        val consumedByDelta = performScroll(leftForDelta)
        val leftForOverscroll = leftForDelta - consumedByDelta

        var needsInvalidation = false
        if (source == NestedScrollSource.Drag) {
            // Ignore small deltas (< 0.5) as this usually comes from floating point rounding issues
            // and can cause scrolling to lock up (b/265363356)
            val appliedHorizontalOverscroll = if (leftForOverscroll.x > 0.5f) {
                pullLeft(leftForOverscroll)
                true
            } else if (leftForOverscroll.x < -0.5f) {
                pullRight(leftForOverscroll)
                true
            } else {
                false
            }
            val appliedVerticalOverscroll = if (leftForOverscroll.y > 0.5f) {
                pullTop(leftForOverscroll)
                true
            } else if (leftForOverscroll.y < -0.5f) {
                pullBottom(leftForOverscroll)
                true
            } else {
                false
            }
            needsInvalidation = appliedHorizontalOverscroll || appliedVerticalOverscroll
        }
        needsInvalidation = releaseOppositeOverscroll(delta) || needsInvalidation
        if (needsInvalidation) invalidateOverscroll()

        return consumedOffset + consumedByDelta
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        // Early return
        if (containerSize.isEmpty()) {
            performFling(velocity)
            return
        }
        // Relax existing stretches before performing fling
        val consumedX = if (velocity.x > 0f && edgeEffectWrapper.isLeftStretched()) {
            edgeEffectWrapper.getOrCreateLeftEffect().onAbsorbCompat(velocity.x.roundToInt())
            velocity.x
        } else if (velocity.x < 0 && edgeEffectWrapper.isRightStretched()) {
            edgeEffectWrapper.getOrCreateRightEffect().onAbsorbCompat(-velocity.x.roundToInt())
            velocity.x
        } else {
            0f
        }
        val consumedY = if (velocity.y > 0f && edgeEffectWrapper.isTopStretched()) {
            edgeEffectWrapper.getOrCreateTopEffect().onAbsorbCompat(velocity.y.roundToInt())
            velocity.y
        } else if (velocity.y < 0f && edgeEffectWrapper.isBottomStretched()) {
            edgeEffectWrapper.getOrCreateBottomEffect().onAbsorbCompat(-velocity.y.roundToInt())
            velocity.y
        } else {
            0f
        }
        val consumed = Velocity(consumedX, consumedY)
        if (consumed != Velocity.Zero) invalidateOverscroll()

        val remainingVelocity = velocity - consumed
        val consumedByVelocity = performFling(remainingVelocity)
        val leftForOverscroll = remainingVelocity - consumedByVelocity

        scrollCycleInProgress = false
        if (leftForOverscroll.x > 0) {
            edgeEffectWrapper.getOrCreateLeftEffect()
                .onAbsorbCompat(leftForOverscroll.x.roundToInt())
        } else if (leftForOverscroll.x < 0) {
            edgeEffectWrapper.getOrCreateRightEffect()
                .onAbsorbCompat(-leftForOverscroll.x.roundToInt())
        }
        if (leftForOverscroll.y > 0) {
            edgeEffectWrapper.getOrCreateTopEffect()
                .onAbsorbCompat(leftForOverscroll.y.roundToInt())
        } else if (leftForOverscroll.y < 0) {
            edgeEffectWrapper.getOrCreateBottomEffect()
                .onAbsorbCompat(-leftForOverscroll.y.roundToInt())
        }
        if (leftForOverscroll != Velocity.Zero) invalidateOverscroll()
        animateToRelease()
    }

    private var containerSize = Size.Zero

    override val isInProgress: Boolean
        get() {
            edgeEffectWrapper.forEachEffect { if (it.distanceCompat != 0f) return true }
            return false
        }

    private fun stopOverscrollAnimation(): Boolean {
        var stopped = false
        // Displacement doesn't matter here
        if (edgeEffectWrapper.isLeftStretched()) {
            pullLeft(Offset.Zero)
            stopped = true
        }
        if (edgeEffectWrapper.isRightStretched()) {
            pullRight(Offset.Zero)
            stopped = true
        }
        if (edgeEffectWrapper.isTopStretched()) {
            pullTop(Offset.Zero)
            stopped = true
        }
        if (edgeEffectWrapper.isBottomStretched()) {
            pullBottom(Offset.Zero)
            stopped = true
        }
        return stopped
    }

    internal fun updateSize(size: Size) {
        val initialSetSize = containerSize == Size.Zero
        val differentSize = size != containerSize
        containerSize = size
        if (differentSize) {
            edgeEffectWrapper.setSize(IntSize(size.width.roundToInt(), size.height.roundToInt()))
        }
        if (!initialSetSize && differentSize) {
            invalidateOverscroll()
            animateToRelease()
        }
    }

    private var pointerId: PointerId? = null

    /**
     * @return displacement based on the last [pointerPosition] and [containerSize]
     */
    internal fun displacement(): Offset {
        val pointer = pointerPosition ?: containerSize.center
        val x = pointer.x / containerSize.width
        val y = pointer.y / containerSize.height
        return Offset(x, y)
    }

    override val effectModifier: Modifier = Modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                pointerId = down.id
                pointerPosition = down.position
                do {
                    val pressedChanges = awaitPointerEvent().changes.fastFilter { it.pressed }
                    // If the same ID we are already tracking is down, use that. Otherwise, use
                    // the next down, to move the overscroll to the next pointer.
                    val change = pressedChanges
                        .fastFirstOrNull { it.id == pointerId } ?: pressedChanges.firstOrNull()
                    if (change != null) {
                        // Update the id if we are now tracking a new down
                        pointerId = change.id
                        pointerPosition = change.position
                    }
                } while (pressedChanges.isNotEmpty())
                pointerId = null
                // Explicitly not resetting the pointer position until the next down, so we
                // don't change any existing effects
            }
        }
        .then(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                DrawStretchOverscrollModifier(
                    this@AndroidEdgeEffectOverscrollEffect,
                    edgeEffectWrapper,
                    debugInspectorInfo {
                        name = "overscroll"
                        value = this@AndroidEdgeEffectOverscrollEffect
                    }
                )
            } else {
                DrawGlowOverscrollModifier(
                    this@AndroidEdgeEffectOverscrollEffect,
                    edgeEffectWrapper,
                    overscrollConfig,
                    debugInspectorInfo {
                        name = "overscroll"
                        value = this@AndroidEdgeEffectOverscrollEffect
                    }
                )
            }
        )

    internal fun invalidateOverscroll() {
        if (invalidationEnabled) {
            redrawSignal.value = Unit
        }
    }

    // animate the edge effects to 0 (no overscroll). Usually needed when the finger is up.
    private fun animateToRelease() {
        var needsInvalidation = false
        edgeEffectWrapper.forEachEffect {
            it.onRelease()
            needsInvalidation = it.isFinished || needsInvalidation
        }
        if (needsInvalidation) invalidateOverscroll()
    }

    private fun releaseOppositeOverscroll(delta: Offset): Boolean {
        var needsInvalidation = false
        if (edgeEffectWrapper.isLeftAnimating() && delta.x < 0) {
            edgeEffectWrapper.getOrCreateLeftEffect().onReleaseWithOppositeDelta(delta = delta.x)
            needsInvalidation = !edgeEffectWrapper.isLeftAnimating()
        }
        if (edgeEffectWrapper.isRightAnimating() && delta.x > 0) {
            edgeEffectWrapper.getOrCreateRightEffect().onReleaseWithOppositeDelta(delta = delta.x)
            needsInvalidation = needsInvalidation || !edgeEffectWrapper.isRightAnimating()
        }
        if (edgeEffectWrapper.isTopAnimating() && delta.y < 0) {
            edgeEffectWrapper.getOrCreateTopEffect().onReleaseWithOppositeDelta(delta = delta.y)
            needsInvalidation = needsInvalidation || !edgeEffectWrapper.isTopAnimating()
        }
        if (edgeEffectWrapper.isBottomAnimating() && delta.y > 0) {
            edgeEffectWrapper.getOrCreateBottomEffect().onReleaseWithOppositeDelta(delta = delta.y)
            needsInvalidation = needsInvalidation || !edgeEffectWrapper.isBottomAnimating()
        }
        return needsInvalidation
    }

    private fun pullTop(scroll: Offset): Float {
        val displacementX = displacement().x
        val pullY = scroll.y / containerSize.height
        val topEffect = edgeEffectWrapper.getOrCreateTopEffect()
        val consumed = topEffect.onPullDistanceCompat(pullY, displacementX) * containerSize.height
        // If overscroll is showing, assume we have consumed all the provided scroll, and return
        // that amount directly to avoid floating point rounding issues (b/265363356)
        return if (topEffect.distanceCompat != 0f) {
            scroll.y
        } else {
            consumed
        }
    }

    private fun pullBottom(scroll: Offset): Float {
        val displacementX = displacement().x
        val pullY = scroll.y / containerSize.height
        val bottomEffect = edgeEffectWrapper.getOrCreateBottomEffect()
        val consumed = -bottomEffect.onPullDistanceCompat(
            -pullY,
            1 - displacementX
        ) * containerSize.height
        // If overscroll is showing, assume we have consumed all the provided scroll, and return
        // that amount directly to avoid floating point rounding issues (b/265363356)
        return if (bottomEffect.distanceCompat != 0f) {
            scroll.y
        } else {
            consumed
        }
    }

    private fun pullLeft(scroll: Offset): Float {
        val displacementY = displacement().y
        val pullX = scroll.x / containerSize.width
        val leftEffect = edgeEffectWrapper.getOrCreateLeftEffect()
        val consumed = leftEffect.onPullDistanceCompat(
            pullX,
            1 - displacementY
        ) * containerSize.width
        // If overscroll is showing, assume we have consumed all the provided scroll, and return
        // that amount directly to avoid floating point rounding issues (b/265363356)
        return if (leftEffect.distanceCompat != 0f) {
            scroll.x
        } else {
            consumed
        }
    }

    private fun pullRight(scroll: Offset): Float {
        val displacementY = displacement().y
        val pullX = scroll.x / containerSize.width
        val rightEffect = edgeEffectWrapper.getOrCreateRightEffect()
        val consumed = -rightEffect.onPullDistanceCompat(
            -pullX,
            displacementY
        ) * containerSize.width
        // If overscroll is showing, assume we have consumed all the provided scroll, and return
        // that amount directly to avoid floating point rounding issues (b/265363356)
        return if (rightEffect.distanceCompat != 0f) {
            scroll.x
        } else {
            consumed
        }
    }
}

/**
 * Handles lazy creation of [EdgeEffect]s used to render overscroll.
 */
private class EdgeEffectWrapper(
    private val context: Context,
    @ColorInt private val glowColor: Int
) {
    private var size: IntSize = IntSize.Zero
    private var topEffect: EdgeEffect? = null
    private var bottomEffect: EdgeEffect? = null
    private var leftEffect: EdgeEffect? = null
    private var rightEffect: EdgeEffect? = null

    // These are used to negate the previous stretch, since RenderNode#clearStretch() is not public
    // API. See DrawStretchOverscrollModifier for more information.
    private var topEffectNegation: EdgeEffect? = null
    private var bottomEffectNegation: EdgeEffect? = null
    private var leftEffectNegation: EdgeEffect? = null
    private var rightEffectNegation: EdgeEffect? = null

    inline fun forEachEffect(action: (EdgeEffect) -> Unit) {
        topEffect?.let(action)
        bottomEffect?.let(action)
        leftEffect?.let(action)
        rightEffect?.let(action)
    }

    fun isTopStretched(): Boolean = topEffect.isStretched
    fun isBottomStretched(): Boolean = bottomEffect.isStretched
    fun isLeftStretched(): Boolean = leftEffect.isStretched
    fun isRightStretched(): Boolean = rightEffect.isStretched
    fun isTopNegationStretched(): Boolean = topEffectNegation.isStretched
    fun isBottomNegationStretched(): Boolean = bottomEffectNegation.isStretched
    fun isLeftNegationStretched(): Boolean = leftEffectNegation.isStretched
    fun isRightNegationStretched(): Boolean = rightEffectNegation.isStretched

    private val EdgeEffect?.isStretched: Boolean
        get() {
            if (this == null) return false
            return distanceCompat != 0f
        }

    fun isTopAnimating(): Boolean = topEffect.isAnimating
    fun isBottomAnimating(): Boolean = bottomEffect.isAnimating
    fun isLeftAnimating(): Boolean = leftEffect.isAnimating
    fun isRightAnimating(): Boolean = rightEffect.isAnimating

    private val EdgeEffect?.isAnimating: Boolean
        get() {
            if (this == null) return false
            return !isFinished
        }

    fun getOrCreateTopEffect(): EdgeEffect = topEffect
        ?: createEdgeEffect().also { topEffect = it }
    fun getOrCreateBottomEffect(): EdgeEffect = bottomEffect
        ?: createEdgeEffect().also { bottomEffect = it }
    fun getOrCreateLeftEffect(): EdgeEffect = leftEffect
        ?: createEdgeEffect().also { leftEffect = it }
    fun getOrCreateRightEffect(): EdgeEffect = rightEffect
        ?: createEdgeEffect().also { rightEffect = it }
    fun getOrCreateTopEffectNegation(): EdgeEffect = topEffectNegation
        ?: createEdgeEffect().also { topEffectNegation = it }
    fun getOrCreateBottomEffectNegation(): EdgeEffect = bottomEffectNegation
        ?: createEdgeEffect().also { bottomEffectNegation = it }
    fun getOrCreateLeftEffectNegation(): EdgeEffect = leftEffectNegation
        ?: createEdgeEffect().also { leftEffectNegation = it }
    fun getOrCreateRightEffectNegation(): EdgeEffect = rightEffectNegation
        ?: createEdgeEffect().also { rightEffectNegation = it }

    private fun createEdgeEffect() = EdgeEffectCompat.create(context).apply {
        color = glowColor
        if (size != IntSize.Zero) {
            setSize(size.width, size.height)
        }
    }

    fun setSize(size: IntSize) {
        this.size = size
        topEffect?.setSize(size.width, size.height)
        bottomEffect?.setSize(size.width, size.height)
        leftEffect?.setSize(size.height, size.width)
        rightEffect?.setSize(size.height, size.width)

        topEffectNegation?.setSize(size.width, size.height)
        bottomEffectNegation?.setSize(size.width, size.height)
        leftEffectNegation?.setSize(size.height, size.width)
        rightEffectNegation?.setSize(size.height, size.width)
    }
}
