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

package androidx.compose.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.launch

internal class SharedElement(val key: Any, val scope: SharedTransitionScopeImpl) {

    private val stateMachine = SharedTransitionStateMachine(this)

    internal val state
        get() = stateMachine.state

    // Read-only entries
    val enabledEntries: List<SharedElementEntry>
        get() = _enabledEntries

    // Read-only entries
    val allEntries: List<SharedElementEntry>
        get() = _allEntries

    fun isAnimating(): Boolean = enabledEntries.fastAny { it.boundsAnimation.isRunning }

    private val momentumAnimation = Animatable(Offset.Zero, Offset.VectorConverter)

    internal fun updateMatch() {
        @Suppress("VisibleForTests") scope.testBlockToRun?.invoke()
        _enabledEntries.removeAll { !allEntries.contains(it) || !it.isEnabled }
        allEntries.fastForEach {
            if (it.isEnabled && !enabledEntries.contains(it)) {
                _enabledEntries.add(it)
            }
        }
        val hasVisibleContent = _enabledEntries.hasVisibleContent()
        stateMachine.checkForAndDeferStateUpdates(hasVisibleContent)
    }

    private var animationSpecFinalized = false

    internal fun updateExitVelocity(velocity: Velocity) {
        // Start the momentum animation right away, in order to use the next frame as the start
        // time. In contrast, the actual shared element will need to wait for the next composition
        // to start the animation. The shared element transition will then acquire its official
        // start time the frame after the composition. For velocity related animations, a 2-frame
        // delay on the start will cause visual jank.
        scope.coroutineScope.launch {
            // Start the animation right away. The expectation is in the first frame we will
            // finalize the animation spec.
            momentumAnimation.animateTo(
                Offset.Zero,
                DefaultMomentumSpring,
                initialVelocity = velocity.toOffset(),
            )
            animationSpecFinalized = true
        }
    }

    val momentumAnimationOffset: () -> Offset = {
        if (!animationSpecFinalized && scope.isTransitionActive && momentumAnimation.isRunning) {
            enabledEntries
                .firstOrNull { it.target }
                ?.let {
                    val targetSpec = it.boundsAnimation.animationSpec
                    // New target animation acquired. Finalize the animation spec for the momentum
                    // animation.
                    if (targetSpec is SpringSpec) {
                        val spring =
                            spring(
                                targetSpec.dampingRatio,
                                targetSpec.stiffness,
                                Offset.VisibilityThreshold,
                            )
                        scope.coroutineScope.launch {
                            momentumAnimation.animateTo(Offset.Zero, spring)
                        }
                    }
                    animationSpecFinalized = true
                }
        }
        momentumAnimation.value
    }

    fun invalidateTargetBoundsProvider() = stateMachine.invalidateTargetBoundsProvider()

    fun tryInitializingCurrentBounds() = stateMachine.tryInitializingCurrentBounds()

    fun onSharedTransitionFinished() {
        if (enabledEntries.size <= 1 || !enabledEntries.hasVisibleContent()) {
            stateMachine.resetState()
        }
    }

    /**
     * This is queried by developers for active match. We need to therefore return the possibility
     * of active match as soon as possible by peeking into deferred request if needed. This allows
     * callers to set up animations in composition based on the returned value.
     */
    val foundMatch: Boolean
        get() =
            state.activeMatchFound ||
                state.matchIsOrHasBeenConfigured ||
                stateMachine.activeMatchDeferred

    val boundsTransformIsActive: Boolean
        get() = state.matchIsOrHasBeenConfigured

    fun onLookaheadPlaced(placementScope: Placeable.PlacementScope, state: SharedElementEntry) {
        stateMachine.processPendingRequest()
        if (this@SharedElement.state == NoMatchFound || !state.isEnabled) return

        val matchState = this@SharedElement.state
        if (state.boundsAnimation.target && matchState.activeMatchFound) {
            with(placementScope) {
                coordinates?.let {
                    val lookaheadSize = it.size.toSize()
                    val topLeft =
                        with(state.sharedElement.scope) {
                            state.sharedElement.scope.lookaheadRoot.localLookaheadPositionOf(it)
                        }
                    val structuralOffset =
                        with(state.sharedElement.scope) {
                            state.sharedElement.scope.lookaheadRoot.localPositionOf(
                                it,
                                includeMotionFrameOfReference = false,
                            )
                        }

                    stateMachine.configureActiveMatch(lookaheadSize, topLeft, structuralOffset)
                }
            }
        }
    }

    /**
     * Each entry comes from a call site of sharedElement/sharedBounds of the same key. In most
     * cases there will be 1 (i.e. no match) or 2 (i.e. match found) entries. In the interrupted
     * cases, there may be multiple scenes showing simultaneously, resulting in more than 2 shared
     * element entries for the same key to be present. In those cases, we expect there to be only 1
     * state that is becoming visible, which we will use to derive target bounds. If none is
     * becoming visible, then we consider this an error case for the lack of target, and
     * consequently animate none of them.
     */
    private val _allEntries = mutableStateListOf<SharedElementEntry>()
    private val _enabledEntries = mutableStateListOf<SharedElementEntry>()

    internal val observingVisibilityChange: () -> Unit = {
        allEntries.fastAny { it.target && it.isEnabled }
    }

    fun addEntry(sharedElementState: SharedElementEntry) {
        _allEntries.add(sharedElementState)
        updateMatch()
    }

    fun removeEntry(sharedElementState: SharedElementEntry) {
        _allEntries.remove(sharedElementState)
        _enabledEntries.remove(sharedElementState)
        updateMatch()
    }
}

private fun List<SharedElementEntry>.hasVisibleContent(): Boolean = fastAny {
    it.boundsAnimation.target
}

private val DefaultMomentumSpring =
    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = Offset(3f, 3f))

internal fun Velocity.toOffset(): Offset = Offset(x, y)
