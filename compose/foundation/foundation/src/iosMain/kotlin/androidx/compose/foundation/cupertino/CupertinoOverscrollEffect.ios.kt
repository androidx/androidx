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

package androidx.compose.foundation.cupertino

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

private enum class CupertinoScrollSource {
    DRAG, FLING
}

private enum class CupertinoOverscrollDirection {
    UNKNOWN, VERTICAL, HORIZONTAL
}

private enum class CupertinoSpringAnimationReason {
    FLING_FROM_OVERSCROLL, POSSIBLE_SPRING_IN_THE_END
}

/*
 * Encapsulates internal calculation data representing per-dimension change after drag delta is consumed (or not)
 * by [CupertinoOverscrollEffect]
 */
private data class CupertinoOverscrollAvailableDelta(
    // delta which will be used to perform actual content scroll
    val availableDelta: Float,

    // new overscroll value for dimension in context of which calculation returning
    // instance of this type was returned
    val newOverscrollValue: Float
)

/**
 * CupertinoOverscrollEffect
 *
 * @param density to be taken into consideration during computations;
 * Cupertino formulas use DPs, and scroll machinery uses raw values.
 *
 * @param applyClip Some consumers of overscroll effect apply clip by themselves and some don't,
 * thus this flag is needed to update our modifier chain and make the clipping correct in every case while avoiding redundancy
 */
internal class CupertinoOverscrollEffect(
    private val density: Float,
    val applyClip: Boolean
) : OverscrollEffect {
    /*
     * Direction of scrolling for this overscroll effect, derived from arguments during
     * [applyToScroll] calls. Technically this effect supports both dimensions, but current API requires
     * that different stages of animations spawned by this effect for both dimensions
     * end at the same time, which is not the case:
     * Spring->Fling->Spring, Fling->Spring, Spring->Fling effects can have different timing per dimension
     * (see Notes of https://github.com/JetBrains/compose-multiplatform-core/pull/609),
     * which is not possible to express without changing API. Hence this effect will be fixed to latest
     * received delta.
     */
    private var direction: CupertinoOverscrollDirection = CupertinoOverscrollDirection.UNKNOWN

    /*
     * Size of container is taking into consideration when computing rubber banding
     */
    private var scrollSize: Size = Size.Zero

    /*
     * Current offset in overscroll area
     * Negative for bottom-right
     * Positive for top-left
     * Zero if within the scrollable range
     * It will be mapped to the actual visible offset using the rubber banding rule inside
     * [Modifier.offset] within [effectModifier]
     */
    private var overscrollOffsetState = mutableStateOf(Offset.Zero)
    private var overscrollOffset: Offset
        get () = overscrollOffsetState.value
        set(value) {
            overscrollOffsetState.value = value
            drawCallScheduledByOffsetChange = true
        }
    private var drawCallScheduledByOffsetChange = true

    private var lastFlingUnconsumedDelta: Offset = Offset.Zero
    private val visibleOverscrollOffset: IntOffset
        get() = overscrollOffsetState.value.rubberBanded().round()

    override val isInProgress: Boolean
        get() =
            // If visible overscroll offset has at least one pixel
            // this effect is considered to be in progress
            visibleOverscrollOffset.toOffset().getDistance() > 0.5f

    private val overscrollNode = CupertinoOverscrollNode(
        offset = { visibleOverscrollOffset },
        onNodeRemeasured = { scrollSize = it.toSize() },
        onDraw = ::onDraw,
        applyClip = applyClip
    )
    override val node: DelegatableNode get() = overscrollNode

    private fun onDraw() {
        // Fix an issue where scrolling was cancelled but the overscroll effect was not completed.
        // Reset the overscroll effect when no ongoing animation or interaction is applied.
        if (!drawCallScheduledByOffsetChange && isInProgress && overscrollNode.pointersDown == 0) {
            overscrollOffsetState.value = Offset.Zero
        }

        drawCallScheduledByOffsetChange = false
    }

    private fun NestedScrollSource.toCupertinoScrollSource(): CupertinoScrollSource? =
        when (this) {
            NestedScrollSource.UserInput -> CupertinoScrollSource.DRAG
            NestedScrollSource.SideEffect -> CupertinoScrollSource.FLING
            else -> null
        }

    /*
     * Takes input scroll delta, current overscroll value, and scroll source, return [CupertinoOverscrollAvailableDelta]
     */
    @Stable
    private fun availableDelta(
        delta: Float,
        overscroll: Float,
        source: CupertinoScrollSource
    ): CupertinoOverscrollAvailableDelta {
        // if source is fling:
        // 1. no delta will be consumed
        // 2. overscroll will stay the same
        if (source == CupertinoScrollSource.FLING) {
            return CupertinoOverscrollAvailableDelta(delta, overscroll)
        }

        val newOverscroll = overscroll + delta

        return if (delta >= 0f && overscroll <= 0f) {
            if (newOverscroll > 0f) {
                CupertinoOverscrollAvailableDelta(newOverscroll, 0f)
            } else {
                CupertinoOverscrollAvailableDelta(0f, newOverscroll)
            }
        } else if (delta <= 0f && overscroll >= 0f) {
            if (newOverscroll < 0f) {
                CupertinoOverscrollAvailableDelta(newOverscroll, 0f)
            } else {
                CupertinoOverscrollAvailableDelta(0f, newOverscroll)
            }
        } else {
            CupertinoOverscrollAvailableDelta(0f, newOverscroll)
        }
    }

    /*
     * Returns the amount of scroll delta available after user performed scroll inside overscroll area
     * It will update [overscroll] resulting in visual change because of [Modifier.offset] depending on it
     */
    private fun availableDelta(delta: Offset, source: CupertinoScrollSource): Offset {
        val (x, overscrollX) = availableDelta(delta.x, overscrollOffset.x, source)
        val (y, overscrollY) = availableDelta(delta.y, overscrollOffset.y, source)

        overscrollOffset = Offset(overscrollX, overscrollY)

        return Offset(x, y)
    }

    /*
     * Semantics of this method match the [OverscrollEffect.applyToScroll] one,
     * The only difference is NestedScrollSource being remapped to CupertinoScrollSource to narrow
     * processed states invariant
     */
    private fun applyToScroll(
        delta: Offset,
        source: CupertinoScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        // Calculate how much delta is available after being consumed by scrolling inside overscroll area
        val deltaLeftForPerformScroll = availableDelta(delta, source)

        // Then pass remaining delta to scroll closure
        val deltaConsumedByPerformScroll = performScroll(deltaLeftForPerformScroll)

        // Delta which is left after `performScroll` was invoked with availableDelta
        val unconsumedDelta = deltaLeftForPerformScroll - deltaConsumedByPerformScroll

        return when (source) {
            CupertinoScrollSource.DRAG -> {
                // [unconsumedDelta] is going into overscroll again in case a user drags and hits the
                // overscroll->content->overscroll or content->overscroll scenario within single frame
                overscrollOffset += unconsumedDelta
                lastFlingUnconsumedDelta = Offset.Zero
                delta - unconsumedDelta
            }

            CupertinoScrollSource.FLING -> {
                // If unconsumedDelta is not Zero, [CupertinoOverscrollEffect] will cancel fling and
                // start spring animation instead
                lastFlingUnconsumedDelta = unconsumedDelta
                delta - unconsumedDelta
            }
        }
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset
    ): Offset {
        springAnimationScope?.cancel()
        springAnimationScope = null

        direction = direction.combinedWith(delta.toCupertinoOverscrollDirection())

        return source.toCupertinoScrollSource()?.let {
            applyToScroll(delta, it, performScroll)
        } ?: performScroll(delta)
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity
    ) {
        val availableFlingVelocity = playInitialSpringAnimationIfNeeded(velocity)
        val velocityConsumedByFling = performFling(availableFlingVelocity)
        val postFlingVelocity = availableFlingVelocity - velocityConsumedByFling

        val unconsumedDelta = lastFlingUnconsumedDelta.toFloat()
        if (unconsumedDelta == 0f && overscrollOffset == Offset.Zero) {
            return
        }

        playSpringAnimation(
            unconsumedDelta,
            postFlingVelocity.toFloat(),
            CupertinoSpringAnimationReason.POSSIBLE_SPRING_IN_THE_END
        )
    }

    private fun Offset.toCupertinoOverscrollDirection(): CupertinoOverscrollDirection {
        val hasXPart = abs(x) > 0f
        val hasYPart = abs(y) > 0f

        return if (hasXPart xor hasYPart) {
            if (hasXPart) {
                CupertinoOverscrollDirection.HORIZONTAL
            } else {
                // hasYPart != hasXPart and hasXPart is false
                CupertinoOverscrollDirection.VERTICAL
            }
        } else {
            // hasXPart and hasYPart are equal
            CupertinoOverscrollDirection.UNKNOWN
        }
    }

    private fun CupertinoOverscrollDirection.combinedWith(other: CupertinoOverscrollDirection): CupertinoOverscrollDirection =
        when (this) {
            CupertinoOverscrollDirection.UNKNOWN -> when (other) {
                CupertinoOverscrollDirection.UNKNOWN -> CupertinoOverscrollDirection.UNKNOWN
                CupertinoOverscrollDirection.VERTICAL -> CupertinoOverscrollDirection.VERTICAL
                CupertinoOverscrollDirection.HORIZONTAL -> CupertinoOverscrollDirection.HORIZONTAL
            }

            CupertinoOverscrollDirection.VERTICAL -> when (other) {
                CupertinoOverscrollDirection.UNKNOWN, CupertinoOverscrollDirection.VERTICAL -> CupertinoOverscrollDirection.VERTICAL
                CupertinoOverscrollDirection.HORIZONTAL -> CupertinoOverscrollDirection.HORIZONTAL
            }

            CupertinoOverscrollDirection.HORIZONTAL -> when (other) {
                CupertinoOverscrollDirection.UNKNOWN, CupertinoOverscrollDirection.HORIZONTAL -> CupertinoOverscrollDirection.HORIZONTAL
                CupertinoOverscrollDirection.VERTICAL -> CupertinoOverscrollDirection.VERTICAL
            }
        }

    private fun Velocity.toFloat(): Float =
        toOffset().toFloat()

    private fun Float.toVelocity(): Velocity =
        toOffset().toVelocity()

    private fun Offset.toFloat(): Float =
        when (direction) {
            CupertinoOverscrollDirection.UNKNOWN -> 0f
            CupertinoOverscrollDirection.VERTICAL -> y
            CupertinoOverscrollDirection.HORIZONTAL -> x
        }

    private fun Float.toOffset(): Offset =
        when (direction) {
            CupertinoOverscrollDirection.UNKNOWN -> Offset.Zero
            CupertinoOverscrollDirection.VERTICAL -> Offset(0f, this)
            CupertinoOverscrollDirection.HORIZONTAL -> Offset(this, 0f)
        }

    private suspend fun playInitialSpringAnimationIfNeeded(initialVelocity: Velocity): Velocity {
        val velocity = initialVelocity.toFloat()
        val overscroll = overscrollOffset.toFloat()

        return if ((velocity <= 0f && overscroll > 0f) || (velocity >= 0f && overscroll < 0f)) {
            playSpringAnimation(
                unconsumedDelta = 0f,
                velocity,
                CupertinoSpringAnimationReason.FLING_FROM_OVERSCROLL
            ).toVelocity()
        } else {
            initialVelocity
        }
    }

    private var springAnimationScope: CoroutineScope? = null

    private suspend fun playSpringAnimation(
        unconsumedDelta: Float,
        initialVelocity: Float,
        reason: CupertinoSpringAnimationReason
    ): Float {
        val initialValue = overscrollOffset.toFloat() + unconsumedDelta
        val initialSign = sign(initialValue)
        var currentVelocity = initialVelocity

        // All input values are divided by density so all internal calculations are performed as if
        // they operated on DPs. Callback value is then scaled back to raw pixels.
        val visibilityThreshold = 0.5f / density

        val spec = when (reason) {
            CupertinoSpringAnimationReason.FLING_FROM_OVERSCROLL -> {
                spring(
                    stiffness = 300f,
                    visibilityThreshold = visibilityThreshold
                )
            }

            CupertinoSpringAnimationReason.POSSIBLE_SPRING_IN_THE_END -> {
                spring(
                    stiffness = 120f,
                    visibilityThreshold = visibilityThreshold
                )
            }
        }

        springAnimationScope?.cancel()
        springAnimationScope = CoroutineScope(currentCoroutineContext())
        springAnimationScope?.run {
            AnimationState(
                Float.VectorConverter,
                initialValue / density,
                initialVelocity / density
            ).animateTo(
                targetValue = 0f,
                animationSpec = spec
            ) {
                overscrollOffset = (value * density).toOffset()
                currentVelocity = velocity * density

                // If it was fling from overscroll, cancel animation and return velocity
                if (reason == CupertinoSpringAnimationReason.FLING_FROM_OVERSCROLL &&
                    initialSign != 0f &&
                    sign(value) != initialSign
                ) {
                    this.cancelAnimation()
                }
            }
            springAnimationScope = null
        }

        if (currentCoroutineContext().isActive) {
            // The spring is critically damped, so in case spring-fling-spring sequence is slightly
            // offset and velocity is of the opposite sign, it will end up with no animation
            overscrollOffset = Offset.Zero
        }

        if (reason == CupertinoSpringAnimationReason.POSSIBLE_SPRING_IN_THE_END) {
            currentVelocity = 0f
        }

        return currentVelocity
    }

    private fun Offset.rubberBanded(): Offset {
        if (scrollSize.width == 0f || scrollSize.height == 0f) {
            return Offset.Zero
        }

        val dpOffset = this / density
        val dpSize = scrollSize / density

        return Offset(
            rubberBandedValue(dpOffset.x, dpSize.width, RUBBER_BAND_COEFFICIENT),
            rubberBandedValue(dpOffset.y, dpSize.height, RUBBER_BAND_COEFFICIENT)
        ) * density
    }

    /*
     * Maps raw delta offset [value] on an axis within scroll container with [dimension]
     * to actual visible offset
     */
    private fun rubberBandedValue(value: Float, dimension: Float, coefficient: Float) =
        sign(value) * (1f - (1f / (abs(value) * coefficient / dimension + 1f))) * dimension

    companion object Companion {
        private const val RUBBER_BAND_COEFFICIENT = 0.55f
    }
}

private class CupertinoOverscrollNode(
    val offset: Density.() -> IntOffset,
    val onNodeRemeasured: (IntSize) -> Unit,
    val onDraw: () -> Unit,
    val applyClip: Boolean
) : Modifier.Node(),
    LayoutModifierNode,
    LayoutAwareModifierNode,
    DrawModifierNode,
    PointerInputModifierNode {
    override fun onRemeasured(size: IntSize) = onNodeRemeasured(size)

    var pointersDown by mutableStateOf(0)

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass == PointerEventPass.Initial) {
            pointerEvent.changes.forEach { change ->
                if (change.changedToDownIgnoreConsumed()) {
                    pointersDown++
                } else if (change.changedToUpIgnoreConsumed()) {
                    pointersDown--
                }
            }
            assert(pointersDown >= 0) { "pointersDown cannot be negative" }
        }
    }

    override fun onCancelPointerInput() {
        pointersDown = 0
    }

    override fun ContentDrawScope.draw() {
        onDraw()
        if (applyClip) {
            val bounds = Rect(-offset().toOffset(), size)
            val rect = size.toRect().intersect(bounds)
            clipRect(
                left = rect.left,
                top = rect.top,
                right = rect.right,
                bottom = rect.bottom,
            ) { this@draw.drawContent() }
        } else {
            this@draw.drawContent()
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(offset())
        }
    }
}

private fun Velocity.toOffset(): Offset =
    Offset(x, y)

private fun Offset.toVelocity(): Velocity =
    Velocity(x, y)