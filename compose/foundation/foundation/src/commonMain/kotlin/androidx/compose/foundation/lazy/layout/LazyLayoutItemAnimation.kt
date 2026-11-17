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

package androidx.compose.foundation.lazy.layout

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimation.Companion.NotInitialized
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyLayoutItemAnimation(
    coroutineScope: CoroutineScope,
    graphicsContext: GraphicsContext,
    onLayerPropertyChanged: () -> Unit = {},
): LazyLayoutItemAnimation =
    if (ComposeFoundationFlags.isLazyLayoutItemAnimationEnterExitTransitionsEnabled)
        StateBasedLazyLayoutItemAnimation(
            coroutineScope,
            graphicsContext,
            onLayerPropertyChanged,
        )
    else
        BooleanBasedLazyLayoutItemAnimation(
            coroutineScope,
            graphicsContext,
            onLayerPropertyChanged,
        )

internal sealed interface LazyLayoutItemAnimation {
    var enterTransition: EnterTransition?

    var exitTransition: ExitTransition?

    var placementSpec: FiniteAnimationSpec<IntOffset>?

    var layoutDirection: LayoutDirection

    var reverseLayout: Boolean

    var isVertical: Boolean

    val isRunningMovingAwayAnimation: Boolean

    /**
     * Returns true when the placement animation is currently in progress so the parent should
     * continue composing this item.
     */
    val isPlacementAnimationInProgress: Boolean

    /**
     * Returns true when the appearance animations are in progress. That is to say when any enter
     * transition is in progress.
     */
    val isAppearanceAnimationInProgress: Boolean

    /**
     * Returns true when the disappearance animations are in progress. That is to say when any exit
     * transition is in progress.
     */
    val isDisappearanceAnimationInProgress: Boolean

    /**
     * Returns true when the disappearance animation has been finished. That is to say when all exit
     * transition have finished.
     */
    val isDisappearanceAnimationFinished: Boolean

    /**
     * This property is managed by the animation manager and is not directly used by this class. It
     * represents the last known offset of this item in the lazy layout coordinate space. It will be
     * updated on every scroll and is allowing the manager to track when the item position changes
     * not because of the scroll event in order to start the animation. When there is an active
     * animation it represents the final/target offset.
     */
    var targetOffset: IntOffset

    /**
     * The final offset the placeable associated with this animations was placed at. Unlike
     * [targetOffset] it takes into account things like reverse layout and content padding.
     */
    var placementOffset: IntOffset

    private val isPlacementMainAxisReversed: Boolean
        get() = reverseLayout xor (!isVertical && layoutDirection == LayoutDirection.Rtl)

    /**
     * Tracks the offset of the item in the lookahead pass. When set, this is the animation target
     * that placementDelta should be applied to.
     */
    var lookaheadOffset: IntOffset

    /** Current [GraphicsLayer]. It will be set to null in [release]. */
    val layer: GraphicsLayer?

    /**
     * Current delta to apply for a placement offset. Updates every animation frame. The settled
     * value is [IntOffset.Zero] so the animation is always targeting this value.
     */
    val placementDelta: IntOffset

    /**
     * Moves this item by the scroll [delta] along the main axis, so changes in its position caused
     * by scrolling aren't animated.
     *
     * [placementOffset] is moved as well, so items running a disappearance animation, which are no
     * longer placed, keep moving with the content. For items that are still placed,
     * [placementOffset] is overwritten when the item is next placed.
     */
    fun applyScrollOffset(delta: Int) {
        targetOffset += mainAxisOffset(delta)
        placementOffset += mainAxisOffset(if (isPlacementMainAxisReversed) -delta else delta)
    }

    private fun mainAxisOffset(delta: Int): IntOffset =
        if (isVertical) IntOffset(0, delta) else IntOffset(delta, 0)

    /** Cancels the ongoing placement animation if there is one. */
    fun cancelPlacementAnimation()

    /** Animate the placement by the given [delta] offset. */
    fun animatePlacementDelta(delta: IntOffset, isMovingAway: Boolean)

    fun animateEnterTransition(expands: Boolean)

    fun animateExitTransition()

    fun release()

    companion object {
        val NotInitialized = IntOffset(Int.MAX_VALUE, Int.MAX_VALUE)
    }
}

internal class StateBasedLazyLayoutItemAnimation(
    private val coroutineScope: CoroutineScope,
    private val graphicsContext: GraphicsContext,
    private val onLayerPropertyChanged: () -> Unit = {},
) : LazyLayoutItemAnimation {
    private enum class TransitionState {
        New,
        Placed,
        Entering,
        Exiting,
        Finished,
    }

    override var enterTransition: EnterTransition? = null
    override var exitTransition: ExitTransition? = null
    override var placementSpec: FiniteAnimationSpec<IntOffset>? = null
    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override var reverseLayout: Boolean = false
    override var isVertical: Boolean = true
    var itemSize: IntSize = IntSize.Zero
        set(value) {
            if (field != value) {
                field = value
                enterTransitionScope?.launchUndispatched {
                    sizeAnimation.retargetExpandIn(enterTransition?.config?.changeSize, value)
                }
            }
        }

    private var enterTransitionScope: CoroutineScope? = null

    private val scopeInvalidator = ObservableScopeInvalidator()

    var animatedSize: IntSize = UnspecifiedSize
        get() {
            scopeInvalidator.attachToScope()
            return field
        }
        private set(value) {
            if (field != value) {
                field = value
                scopeInvalidator.invalidateScope()
            }
        }

    fun observeAnimatedSize() {
        scopeInvalidator.attachToScope()
    }

    private var transitionState by mutableStateOf(TransitionState.New)

    private var transitionJob: Job? = null

    private val fadeAnimation = EnterExitFadeAnimation(::updateLayerProperties)

    private val slideAnimation = EnterExitSlideAnimation(::updateLayerProperties)

    private val scaleAnimation = EnterExitScaleAnimation(::updateLayerProperties)

    private val sizeAnimation = EnterExitSizeAnimation(::updateLayerProperties)

    private var placedSize = UnspecifiedSize

    private var placedContentOffset = IntOffset.Zero

    val isClippedToBounds: Boolean
        get() = sizeAnimation.shouldClip

    fun onPlaced(size: IntSize, contentOffset: IntOffset) {
        if (placedSize != size || placedContentOffset != contentOffset) {
            placedSize = size
            placedContentOffset = contentOffset
            updateLayerProperties()
        }
    }

    fun contentOffsetIn(size: IntSize): IntOffset {
        val alignment = sizeAnimation.alignment ?: return IntOffset.Zero
        return alignment.align(itemSize, size, LayoutDirection.Ltr)
    }

    override var isRunningMovingAwayAnimation = false
        private set

    override var isPlacementAnimationInProgress by mutableStateOf(false)
        private set

    override val isAppearanceAnimationInProgress
        get() = transitionState == TransitionState.Entering

    override val isDisappearanceAnimationInProgress
        get() = transitionState == TransitionState.Exiting

    override val isDisappearanceAnimationFinished
        get() = transitionState == TransitionState.Finished

    override var targetOffset: IntOffset = NotInitialized

    override var placementOffset: IntOffset = IntOffset.Zero

    override var lookaheadOffset: IntOffset = NotInitialized

    override var layer: GraphicsLayer? = graphicsContext.createGraphicsLayer()
        private set

    private val placementDeltaAnimation = Animatable(IntOffset.Zero, IntOffset.VectorConverter)

    override var placementDelta by mutableStateOf(IntOffset.Zero)
        private set

    private val containerSize
        get() = layer?.size?.takeIf { it != IntSize.Zero } ?: itemSize

    override fun cancelPlacementAnimation() {
        if (isPlacementAnimationInProgress) {
            coroutineScope.launch {
                placementDeltaAnimation.snapTo(IntOffset.Zero)
                placementDelta = IntOffset.Zero
                isPlacementAnimationInProgress = false
            }
        }
    }

    override fun animatePlacementDelta(delta: IntOffset, isMovingAway: Boolean) {
        val spec = placementSpec ?: return
        val totalDelta = placementDelta - delta
        placementDelta = totalDelta
        isPlacementAnimationInProgress = true
        isRunningMovingAwayAnimation = isMovingAway
        coroutineScope.launch {
            try {
                val finalSpec =
                    if (placementDeltaAnimation.isRunning) {
                        // when interrupted, use the default spring, unless the spec is a spring.
                        spec as? SpringSpec<IntOffset> ?: InterruptionSpec
                    } else {
                        spec
                    }
                if (!placementDeltaAnimation.isRunning) {
                    // if not running we can snap to the initial value and animate to zero
                    placementDeltaAnimation.snapTo(totalDelta)
                    onLayerPropertyChanged()
                }
                // if animation is not currently running the target will be zero, otherwise
                // we have to continue the animation from the current value, but keep the needed
                // total delta for the new animation.
                val animationTarget = placementDeltaAnimation.value - totalDelta
                placementDeltaAnimation.animateTo(animationTarget, finalSpec) {
                    // placementDelta is calculated as if we always animate to target equal to zero
                    placementDelta = value - animationTarget
                    onLayerPropertyChanged()
                }

                isPlacementAnimationInProgress = false
                isRunningMovingAwayAnimation = false
            } catch (_: CancellationException) {
                // we don't reset inProgress in case of cancellation as it means
                // there is a new animation started which would reset it later
            }
        }
    }

    override fun animateEnterTransition(expands: Boolean) = Snapshot.withoutReadObservation {
        if (layer == null || transitionState == TransitionState.Entering) {
            return@withoutReadObservation
        }
        val config = enterTransition?.config
        val size = containerSize
        val isReentering =
            transitionState == TransitionState.Exiting ||
                transitionState == TransitionState.Finished
        runTransition(TransitionState.Entering, TransitionState.Placed) {
            enterTransitionScope = this
            launchUndispatched { fadeAnimation.animateFadeIn(config?.fade) }
            launchUndispatched { slideAnimation.animateSlideIn(config?.slide, size) }
            launchUndispatched { scaleAnimation.animateScaleIn(config?.scale) }
            launchUndispatched {
                sizeAnimation.animateExpandIn(
                    config?.changeSize?.takeIf { expands },
                    itemSize,
                    isReentering,
                )
            }
        }
    }

    override fun animateExitTransition() = Snapshot.withoutReadObservation {
        if (layer == null || transitionState == TransitionState.Exiting) {
            return@withoutReadObservation
        }
        val config = exitTransition?.config
        val size = containerSize
        runTransition(TransitionState.Exiting, TransitionState.Finished) {
            launchUndispatched { fadeAnimation.animateFadeOut(config?.fade) }
            launchUndispatched {
                slideAnimation.animateSlideOut(config?.slide, size, { IntOffset.Zero })
            }
            launchUndispatched { scaleAnimation.animateScaleOut(config?.scale) }
        }
    }

    private inline fun runTransition(
        startState: TransitionState,
        endState: TransitionState,
        crossinline animations: CoroutineScope.() -> Unit,
    ) {
        transitionJob?.cancel()
        enterTransitionScope = null
        transitionState = startState
        updateLayerProperties()
        transitionJob = coroutineScope.launchUndispatched {
            coroutineScope { animations() }
            enterTransitionScope = null
            transitionState = endState
            updateLayerProperties()
        }
    }

    private fun CoroutineScope.launchUndispatched(block: suspend CoroutineScope.() -> Unit) =
        launch(start = CoroutineStart.UNDISPATCHED, block = block)

    override fun release() {
        if (isPlacementAnimationInProgress) {
            isPlacementAnimationInProgress = false
            coroutineScope.launch { placementDeltaAnimation.stop() }
        }
        val job = transitionJob
        transitionJob = null
        job?.cancel()
        enterTransitionScope = null
        transitionState = TransitionState.New
        fadeAnimation.release()
        slideAnimation.release()
        scaleAnimation.release()
        sizeAnimation.release()
        isRunningMovingAwayAnimation = false
        placementDelta = IntOffset.Zero
        targetOffset = NotInitialized
        layer?.let { graphicsContext.releaseGraphicsLayer(it) }
        layer = null
        enterTransition = null
        exitTransition = null
        placementSpec = null
        itemSize = IntSize.Zero
        animatedSize = UnspecifiedSize
        placedSize = UnspecifiedSize
        placedContentOffset = IntOffset.Zero
    }

    private fun updateLayerProperties() {
        val boundsSize = sizeAnimation.value
        animatedSize = boundsSize ?: UnspecifiedSize
        val layer = layer ?: return
        val offset = slideAnimation.value ?: IntOffset.Zero
        val scale = scaleAnimation.value ?: 1f
        val size = containerSize
        val origin = scaleAnimation.transformOrigin

        layer.alpha = fadeAnimation.value ?: 1f
        layer.translationX = offset.x.toFloat()
        layer.translationY = offset.y.toFloat()
        layer.scaleX = scale
        layer.scaleY = scale
        layer.pivotOffset =
            Offset(origin.pivotFractionX * size.width, origin.pivotFractionY * size.height)

        if (boundsSize != null && sizeAnimation.shouldClip) {
            val clipSize = if (placedSize == UnspecifiedSize) boundsSize else placedSize
            val pivot = layer.pivotOffset
            val inverseScale = if (scale > 0f) 1f / scale else 1f
            val boundsLeft = -(offset.x + placedContentOffset.x).toFloat()
            val boundsTop = -(offset.y + placedContentOffset.y).toFloat()
            val left = pivot.x + (boundsLeft - pivot.x) * inverseScale
            val top = pivot.y + (boundsTop - pivot.y) * inverseScale
            val right = pivot.x + (boundsLeft + clipSize.width - pivot.x) * inverseScale
            val bottom = pivot.y + (boundsTop + clipSize.height - pivot.y) * inverseScale
            layer.setRectOutline(
                topLeft = Offset(minOf(left, right), minOf(top, bottom)),
                size = Size(abs(right - left), abs(bottom - top)),
            )
            layer.clip = true
        } else {
            layer.clip = false
        }

        onLayerPropertyChanged()
    }
}

internal class BooleanBasedLazyLayoutItemAnimation(
    private val coroutineScope: CoroutineScope,
    private val graphicsContext: GraphicsContext,
    private val onLayerPropertyChanged: () -> Unit = {},
) : LazyLayoutItemAnimation {
    override var enterTransition: EnterTransition? = null
    override var exitTransition: ExitTransition? = null
    override var placementSpec: FiniteAnimationSpec<IntOffset>? = null

    override var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override var reverseLayout: Boolean = false
    override var isVertical: Boolean = true

    override var isRunningMovingAwayAnimation = false
        private set

    override var isPlacementAnimationInProgress by mutableStateOf(false)
        private set

    /** Returns true when the fade in animation is currently in progress. */
    private var isFadeInAnimationInProgress by mutableStateOf(false)

    override val isAppearanceAnimationInProgress
        get() = isFadeInAnimationInProgress

    /** Returns true when the fade out animation is currently in progress. */
    private var isFadeOutAnimationInProgress by mutableStateOf(false)

    override val isDisappearanceAnimationInProgress
        get() = isFadeOutAnimationInProgress

    /** Returns true when the fade out animation has been finished. */
    private var isFadeOutAnimationFinished by mutableStateOf(false)

    override val isDisappearanceAnimationFinished
        get() = isFadeOutAnimationFinished

    override var targetOffset: IntOffset = NotInitialized

    override var placementOffset: IntOffset = IntOffset.Zero

    override var lookaheadOffset: IntOffset = NotInitialized

    override var layer: GraphicsLayer? = graphicsContext.createGraphicsLayer()
        private set

    private val placementDeltaAnimation = Animatable(IntOffset.Zero, IntOffset.VectorConverter)

    private val fadeAnimation = Animatable(1f, Float.VectorConverter)

    override var placementDelta by mutableStateOf(IntOffset.Zero)
        private set

    override fun cancelPlacementAnimation() {
        if (isPlacementAnimationInProgress) {
            coroutineScope.launch {
                placementDeltaAnimation.snapTo(IntOffset.Zero)
                placementDelta = IntOffset.Zero
                isPlacementAnimationInProgress = false
            }
        }
    }

    override fun animatePlacementDelta(delta: IntOffset, isMovingAway: Boolean) {
        val spec = placementSpec ?: return
        val totalDelta = placementDelta - delta
        placementDelta = totalDelta
        isPlacementAnimationInProgress = true
        isRunningMovingAwayAnimation = isMovingAway
        coroutineScope.launch {
            try {
                val finalSpec =
                    if (placementDeltaAnimation.isRunning) {
                        // when interrupted, use the default spring, unless the spec is a spring.
                        spec as? SpringSpec<IntOffset> ?: InterruptionSpec
                    } else {
                        spec
                    }
                if (!placementDeltaAnimation.isRunning) {
                    // if not running we can snap to the initial value and animate to zero
                    placementDeltaAnimation.snapTo(totalDelta)
                    onLayerPropertyChanged()
                }
                // if animation is not currently running the target will be zero, otherwise
                // we have to continue the animation from the current value, but keep the needed
                // total delta for the new animation.
                val animationTarget = placementDeltaAnimation.value - totalDelta
                placementDeltaAnimation.animateTo(animationTarget, finalSpec) {
                    // placementDelta is calculated as if we always animate to target equal to zero
                    placementDelta = value - animationTarget
                    onLayerPropertyChanged()
                }

                isPlacementAnimationInProgress = false
                isRunningMovingAwayAnimation = false
            } catch (_: CancellationException) {
                // we don't reset inProgress in case of cancellation as it means
                // there is a new animation started which would reset it later
            }
        }
    }

    override fun animateEnterTransition(expands: Boolean) {
        layer?.let { layer -> animateFadeIn(layer) }
    }

    private fun animateFadeIn(layer: GraphicsLayer) {
        if (isFadeInAnimationInProgress) return

        val fadeInConfig = enterTransition?.config?.fade
        if (fadeInConfig == null) {
            if (isFadeOutAnimationInProgress) {
                layer.alpha = 1f
                coroutineScope.launch { fadeAnimation.snapTo(1f) }
            }
            return
        }

        isFadeInAnimationInProgress = true
        val isFadeOutAnimationInProgress = isFadeOutAnimationInProgress

        if (!isFadeOutAnimationInProgress) {
            layer.alpha = fadeInConfig.alpha
        }

        coroutineScope.launch {
            try {
                if (!isFadeOutAnimationInProgress) {
                    fadeAnimation.snapTo(fadeInConfig.alpha)
                }
                fadeAnimation.animateTo(1f, fadeInConfig.animationSpec) {
                    layer.alpha = value
                    onLayerPropertyChanged()
                }
            } finally {
                isFadeInAnimationInProgress = false
            }
        }
    }

    override fun animateExitTransition() {
        layer?.let { layer -> animateFadeOut(layer) }
    }

    private fun animateFadeOut(layer: GraphicsLayer) {
        val fadeOutConfig = exitTransition?.config?.fade ?: return
        if (isFadeOutAnimationInProgress) return

        isFadeOutAnimationInProgress = true
        coroutineScope.launch {
            try {
                fadeAnimation.animateTo(fadeOutConfig.alpha, fadeOutConfig.animationSpec) {
                    layer.alpha = value
                    onLayerPropertyChanged()
                }
                isFadeOutAnimationFinished = true
            } finally {
                isFadeOutAnimationInProgress = false
            }
        }
    }

    override fun release() {
        if (isPlacementAnimationInProgress) {
            isPlacementAnimationInProgress = false
            coroutineScope.launch { placementDeltaAnimation.stop() }
        }
        if (isFadeInAnimationInProgress) {
            isFadeInAnimationInProgress = false
            coroutineScope.launch { fadeAnimation.stop() }
        }
        if (isFadeOutAnimationInProgress) {
            isFadeOutAnimationInProgress = false
            coroutineScope.launch { fadeAnimation.stop() }
        }
        isRunningMovingAwayAnimation = false
        placementDelta = IntOffset.Zero
        targetOffset = NotInitialized
        layer?.let { graphicsContext.releaseGraphicsLayer(it) }
        layer = null
        enterTransition = null
        exitTransition = null
        placementSpec = null
    }
}

/** We switch to this spec when a duration based animation is being interrupted. */
private val InterruptionSpec =
    spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

internal val UnspecifiedSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)

internal fun IntOffset.mirroredForRtl(layoutWidth: Int, itemWidth: Int): IntOffset =
    IntOffset(layoutWidth - x - itemWidth, y)
