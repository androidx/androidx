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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed

internal class SharedElement(val key: Any, val scope: SharedTransitionScopeImpl) {
    fun isAnimating(): Boolean = states.fastAny { it.boundsAnimation.isRunning } && foundMatch

    private var _targetBounds: Rect? by mutableStateOf(null)

    /**
     * This should be only read only in the post-lookahead placement pass. It returns null when
     * there's no shared element/bounds becoming visible (i.e. when only exiting shared elements are
     * defined, which is an incorrect state).
     */
    val targetBounds: Rect?
        get() {
            _targetBounds =
                targetBoundsProvider?.run { Rect(calculateLookaheadOffset(), nonNullLookaheadSize) }
            return _targetBounds
        }

    fun updateMatch() {
        val hasVisibleContent = hasVisibleContent()
        if (states.size > 1 && hasVisibleContent) {
            foundMatch = true
        } else if (scope.isTransitionActive) {
            // Unrecoverable state when the shared element/bound that is becoming visible
            // is removed.
            if (!hasVisibleContent) {
                foundMatch = false
            }
        } else {
            // Transition not active
            foundMatch = false
        }
        if (states.isNotEmpty()) {
            scope.observeReads(this, updateMatch, observingVisibilityChange)
        }
    }

    var foundMatch: Boolean by mutableStateOf(false)
        private set

    // Tracks current size, should be continuous
    var currentBounds: Rect? by mutableStateOf(null)

    internal var targetBoundsProvider: SharedElementInternalState? = null
        private set

    fun onLookaheadResult(state: SharedElementInternalState, lookaheadSize: Size, topLeft: Offset) {
        if (state.boundsAnimation.target) {
            targetBoundsProvider = state

            // Only update bounds when offset is updated so as to not accidentally fire
            // up animations, only to interrupt them in the same frame later on.
            if (_targetBounds?.topLeft != topLeft || _targetBounds?.size != lookaheadSize) {
                val target = Rect(topLeft, lookaheadSize)
                _targetBounds = target
                states.fastForEach { it.boundsAnimation.animate(currentBounds!!, target) }
            }
        }
    }

    /**
     * Each state comes from a call site of sharedElement/sharedBounds of the same key. In most
     * cases there will be 1 (i.e. no match) or 2 (i.e. match found) states. In the interrupted
     * cases, there may be multiple scenes showing simultaneously, resulting in more than 2 shared
     * element states for the same key to be present. In those cases, we expect there to be only 1
     * state that is becoming visible, which we will use to derive target bounds. If none is
     * becoming visible, then we consider this an error case for the lack of target, and
     * consequently animate none of them.
     */
    val states = mutableStateListOf<SharedElementInternalState>()

    private fun hasVisibleContent(): Boolean = states.fastAny { it.boundsAnimation.target }

    /**
     * This gets called to update the target bounds. The 3 scenarios where
     * [updateTargetBoundsProvider] is needed are: when a shared element is 1) added, 2) removed,
     * or 3) getting a target state change.
     *
     * This is always called from an effect. Assume all compositional changes have been made in this
     * call.
     */
    fun updateTargetBoundsProvider() {
        var targetProvider: SharedElementInternalState? = null
        states.fastForEachReversed {
            if (it.boundsAnimation.target) {
                targetProvider = it
                return@fastForEachReversed
            }
        }

        if (targetProvider == this.targetBoundsProvider) return
        // Update provider
        this.targetBoundsProvider = targetProvider
        _targetBounds = null
    }

    fun onSharedTransitionFinished() {
        foundMatch = states.size > 1 && hasVisibleContent()
        _targetBounds = null
    }

    private val updateMatch: (SharedElement) -> Unit = { updateMatch() }

    private val observingVisibilityChange: () -> Unit = { hasVisibleContent() }

    fun addState(sharedElementState: SharedElementInternalState) {
        states.add(sharedElementState)
        scope.observeReads(this, updateMatch, observingVisibilityChange)
    }

    fun removeState(sharedElementState: SharedElementInternalState) {
        states.remove(sharedElementState)
        if (states.isEmpty()) {
            updateMatch()
            scope.clearObservation(scope = this)
        } else {
            scope.observeReads(scope = this, updateMatch, observingVisibilityChange)
        }
    }
}

internal class SharedElementInternalState(
    sharedElement: SharedElement,
    boundsAnimation: BoundsAnimation,
    placeHolderSize: SharedTransitionScope.PlaceHolderSize,
    renderOnlyWhenVisible: Boolean,
    overlayClip: SharedTransitionScope.OverlayClip,
    renderInOverlayDuringTransition: Boolean,
    userState: SharedTransitionScope.SharedContentState,
    zIndex: Float
) : LayerRenderer, RememberObserver {

    override var zIndex: Float by mutableFloatStateOf(zIndex)

    var renderInOverlayDuringTransition: Boolean by mutableStateOf(renderInOverlayDuringTransition)
    var sharedElement: SharedElement by mutableStateOf(sharedElement)
    var boundsAnimation: BoundsAnimation by mutableStateOf(boundsAnimation)
    var placeHolderSize: SharedTransitionScope.PlaceHolderSize by mutableStateOf(placeHolderSize)
    var renderOnlyWhenVisible: Boolean by mutableStateOf(renderOnlyWhenVisible)
    var overlayClip: SharedTransitionScope.OverlayClip by mutableStateOf(overlayClip)
    var userState: SharedTransitionScope.SharedContentState by mutableStateOf(userState)

    internal var clipPathInOverlay: Path? = null

    override fun drawInOverlay(drawScope: DrawScope) {
        val layer = layer ?: return
        if (shouldRenderInOverlay) {
            with(drawScope) {
                requireNotNull(sharedElement.currentBounds) { "Error: current bounds not set yet." }
                val (x, y) = sharedElement.currentBounds?.topLeft!!
                clipPathInOverlay?.let { clipPath(it) { translate(x, y) { drawLayer(layer) } } }
                    ?: translate(x, y) { drawLayer(layer) }
            }
        }
    }

    val nonNullLookaheadSize: Size
        get() =
            requireNotNull(lookaheadCoords()) {
                    "Error: lookahead coordinates is null for ${sharedElement.key}."
                }
                .size
                .toSize()

    var lookaheadCoords: () -> LayoutCoordinates? = { null }
    override var parentState: SharedElementInternalState? = null

    // This can only be accessed during placement
    fun calculateLookaheadOffset(): Offset {
        val c = requireNotNull(lookaheadCoords()) { "Error: lookahead coordinates is null." }
        return sharedElement.scope.lookaheadRoot.localPositionOf(c, Offset.Zero)
    }

    val target: Boolean
        get() = boundsAnimation.target

    // Delegate the property to a mutable state, so that when layer is updated, the rendering
    // gets invalidated.
    var layer: GraphicsLayer? by mutableStateOf(null)

    private val shouldRenderBasedOnTarget: Boolean
        get() = sharedElement.targetBoundsProvider == this || !renderOnlyWhenVisible

    internal val shouldRenderInOverlay: Boolean
        get() =
            shouldRenderBasedOnTarget && sharedElement.foundMatch && renderInOverlayDuringTransition

    val shouldRenderInPlace: Boolean
        get() = !sharedElement.foundMatch || (!shouldRenderInOverlay && shouldRenderBasedOnTarget)

    override fun onRemembered() {
        sharedElement.scope.onStateAdded(this)
        sharedElement.updateTargetBoundsProvider()
    }

    override fun onForgotten() {
        sharedElement.scope.onStateRemoved(this)
        sharedElement.updateTargetBoundsProvider()
    }

    override fun onAbandoned() {}
}
