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
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.IntOffset
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

    fun applyScrollOffset(offset: IntOffset)

    /** Cancels the ongoing placement animation if there is one. */
    fun cancelPlacementAnimation()

    /** Animate the placement by the given [delta] offset. */
    fun animatePlacementDelta(delta: IntOffset, isMovingAway: Boolean)

    fun animateEnterTransition()

    fun animateExitTransition()

    fun release()

    companion object {
        val NotInitialized = IntOffset(Int.MAX_VALUE, Int.MAX_VALUE)
    }
}

private class StateBasedLazyLayoutItemAnimation(
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

    private var transitionState by mutableStateOf(TransitionState.New)

    private var transitionJob: Job? = null

    private val fadeAnimation = EnterExitFadeAnimation { alpha ->
        layer?.let {
            it.alpha = alpha
            onLayerPropertyChanged()
        }
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

    override fun applyScrollOffset(offset: IntOffset) {
        targetOffset += offset
    }

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

    override fun animateEnterTransition() {
        if (layer == null || transitionState == TransitionState.Entering) return
        runTransition(TransitionState.Entering, TransitionState.Placed) {
            launchUndispatched { fadeAnimation.animateFadeIn(enterTransition?.config?.fade) }
        }
    }

    override fun animateExitTransition() {
        if (layer == null || transitionState == TransitionState.Exiting) return
        val config = exitTransition?.config
        runTransition(TransitionState.Exiting, TransitionState.Finished) {
            launchUndispatched { fadeAnimation.animateFadeOut(config?.fade) }
        }
    }

    private fun runTransition(
        startState: TransitionState,
        endState: TransitionState,
        animations: CoroutineScope.() -> Unit,
    ) {
        transitionJob?.cancel()
        transitionState = startState
        transitionJob = coroutineScope.launchUndispatched {
            coroutineScope { animations() }
            transitionState = endState
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
        transitionState = TransitionState.New
        fadeAnimation.release()
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

private class BooleanBasedLazyLayoutItemAnimation(
    private val coroutineScope: CoroutineScope,
    private val graphicsContext: GraphicsContext,
    private val onLayerPropertyChanged: () -> Unit = {},
) : LazyLayoutItemAnimation {
    override var enterTransition: EnterTransition? = null
    override var exitTransition: ExitTransition? = null
    override var placementSpec: FiniteAnimationSpec<IntOffset>? = null

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

    override fun applyScrollOffset(offset: IntOffset) {
        targetOffset += offset
    }

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

    override fun animateEnterTransition() {
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
