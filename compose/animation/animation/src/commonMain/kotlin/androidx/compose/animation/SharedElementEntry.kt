/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.animation.core.ExperimentalDeferredTransitionApi
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastFirstOrNull

@OptIn(ExperimentalDeferredTransitionApi::class)
internal class SharedElementEntry(
    sharedElement: SharedElement,
    boundsAnimation: BoundsAnimation,
    placeholderSize: SharedTransitionScope.PlaceholderSize,
    renderOnlyWhenVisible: Boolean,
    overlayClip: SharedTransitionScope.OverlayClip,
    renderInOverlayDuringTransition: Boolean,
    userState: SharedTransitionScope.SharedContentState,
    zIndex: Float,
) : LayerRenderer, RememberObserver {

    var isAttached: Boolean by mutableStateOf(false)
    private var _zIndex by mutableFloatStateOf(zIndex)
    override var zIndex: Float
        get() = _zIndex
        set(value) {
            if (_zIndex != value) {
                _zIndex = value
                sharedElement.scope.zOrderChanged()
            }
        }

    var renderInOverlayDuringTransition: Boolean by mutableStateOf(renderInOverlayDuringTransition)
    var sharedElement: SharedElement by mutableStateOf(sharedElement)
    var boundsAnimation: BoundsAnimation by mutableStateOf(boundsAnimation)
    var placeholderSize: SharedTransitionScope.PlaceholderSize by mutableStateOf(placeholderSize)
    var renderOnlyWhenVisible: Boolean by mutableStateOf(renderOnlyWhenVisible)
    var overlayClip: SharedTransitionScope.OverlayClip by mutableStateOf(overlayClip)
    var userState: SharedTransitionScope.SharedContentState by mutableStateOf(userState)

    /**
     * Resolves the active [SharedMutableTransformState] that is currently driving the deferred
     * transformations.
     *
     * If the shared element does not participate in deferred transformations, this returns null.
     *
     * During a deferred phase, we apply the outgoing content's transformations to both shared
     * elements (the incoming and the outgoing). Therefore, if this entry represents the incoming
     * element, we must resolve the state from the corresponding exiting element to ensure they
     * transform perfectly in sync.
     */
    internal val activeMutableTransformState: SharedMutableTransformState?
        get() {
            if (!userState.config.permitTransformDuringDeferredTransition) return null

            val transformState = boundsProvider?.modifierLocalTransformState

            // During a deferred phase, the underlying transition state is held back at the original
            // state. This means the `target` property is temporarily inverted (exiting=true,
            // incoming=false).
            val isIncoming = if (isMutating) !target else target

            if (isIncoming) {
                val exitingEntry =
                    sharedElement.enabledEntries.fastFirstOrNull {
                        if (it.isMutating) it.target else !it.target
                    }
                return exitingEntry?.boundsProvider?.modifierLocalTransformState ?: transformState
            }

            return transformState
        }

    /**
     * Indicates whether the parent container is currently undergoing manual transformations during
     * the deferred phase of a transition (e.g., during a predictive back gesture).
     */
    private val isMutating: Boolean
        get() = boundsProvider?.modifierLocalTransformState?.isMutating == true

    internal var hasHandoffOccurred = false

    val isEnabled: Boolean
        get() = with(userState) { isAttached && isEnabledByUser }

    fun calculateTargetBounds(previousTargetBoundsBeforeLosingTarget: Rect): Rect? {
        return with(userState.config) {
            userState.alternativeTargetBoundsInTransitionScopeAfterRemoval(
                previousTargetBoundsBeforeLosingTarget,
                sharedElement.scope.lookaheadRoot.size.toSize(),
            )
        }
    }

    internal var clipPathInOverlay: Path? = null

    @OptIn(ExperimentalDeferredTransitionApi::class)
    override fun drawInOverlay(drawScope: DrawScope, graphicsContext: GraphicsContext) {
        sharedTransitionDebug {
            "Rendering in overlay for key ${sharedElement.key}, becoming visible? $target"
        }
        if (shouldRenderInOverlay) {
            // Some intermediate parent between overlay and SharedBoundsNode may record its
            // DisplayList before the SharedBoundsNode runs its draw block. By lazily creating the
            // layer here, we ensure the overlay has a valid RenderNode reference to record. This
            // layer will be populated with content in the same frame by the SharedBoundsNode in
            // the case of out-of-order rendering due to layers, hence preventing flickering.
            if (layer == null) {
                layer = graphicsContext.createGraphicsLayer()
            }
        }
        val layer = layer ?: return
        // If currentBoundsWhenMatched == null, it means the shared element has not been properly
        // placed since foundMatch is set. This could be due to some nodes being composed but
        // not measured or laid out e.g. precompose. Such a node would not be rendered in place
        // either. Hence skip rendering in overlay.
        val matchState = sharedElement.state
        val currentBounds: Rect = matchState.currentBounds ?: return

        if (shouldRenderInOverlay) {
            with(drawScope) {
                val (x, y) = currentBounds.topLeft

                var scale = 1f
                var offsetX = 0f
                var offsetY = 0f
                var pivotX = 0f
                var pivotY = 0f

                val mutableTransformState = activeMutableTransformState
                val parentCoords = mutableTransformState?.parentLayoutCoordinates
                val rootCoords = sharedElement.scope.root
                if (
                    mutableTransformState?.isMutating == true &&
                        parentCoords != null &&
                        parentCoords.isAttached &&
                        rootCoords.isAttached
                ) {
                    scale = mutableTransformState.activeScale
                    val offset = mutableTransformState.activeOffset
                    offsetX = offset.x.toFloat()
                    offsetY = offset.y.toFloat()
                    val transformOrigin = mutableTransformState.activeTransformOrigin

                    val pivot = calculatePivot(parentCoords, rootCoords, transformOrigin)
                    pivotX = pivot.x
                    pivotY = pivot.y
                }

                sharedTransitionDebug {
                    "drawing in overlay. key = ${sharedElement.key}," +
                        " at $x, $y current size: ${currentBounds.size} " +
                        "state: $matchState"
                }
                val clipPath = clipPathInOverlay
                translate(offsetX, offsetY) {
                    scale(scale, scale, pivot = Offset(pivotX, pivotY)) {
                        if (clipPath != null) {
                            clipPath(clipPath) { translate(x, y) { drawLayer(layer) } }
                        } else {
                            translate(x, y) { drawLayer(layer) }
                        }
                    }
                }
            }
        }
    }

    private var _parentState: SharedElementEntry? = null
    override var parentState: SharedElementEntry?
        get() = _parentState
        set(value) {
            if (_parentState != value) {
                _parentState = value
                sharedElement.scope.zOrderChanged()
            }
        }

    val target: Boolean
        get() = boundsAnimation.target

    var boundsProvider: BoundsProvider? = null

    // Delegate the property to a mutable state, so that when layer is updated, the rendering
    // gets invalidated.
    var layer: GraphicsLayer? by mutableStateOf(null)

    // Do not need to render the content of an outgoing `sharedElement`.
    private val shouldRenderAtAll: Boolean
        get() =
            boundsAnimation.target ||
                // This && only evaluates to true when the active match is removed during
                // transition
                (sharedElement.state.matchIsOrHasBeenConfigured &&
                    !sharedElement.state.activeMatchFound) ||
                !renderOnlyWhenVisible

    internal val shouldRenderInOverlay: Boolean
        get() =
            shouldRenderAtAll &&
                sharedElement.boundsTransformIsActive &&
                isEnabled &&
                // Render in overlay during transition only takes effect during transition (i.e.
                // when transition is active)
                renderInOverlayDuringTransition &&
                (sharedElement.scope.isTransitionActive || isMutating)

    val shouldRenderInPlace: Boolean
        get() =
            !sharedElement.boundsTransformIsActive || (!shouldRenderInOverlay && shouldRenderAtAll)

    override fun onRemembered() {
        sharedElement.scope.onEntryAdded(this)
        sharedElement.invalidateTargetBoundsProvider()
    }

    override fun onForgotten() {
        sharedElement.scope.onEntryRemoved(this)
        sharedElement.invalidateTargetBoundsProvider()
    }

    override fun onAbandoned() {}
}

internal interface BoundsProvider {
    val lastBoundsInSharedTransitionScope: Rect?

    fun calculateAlternativeTargetBounds(targetBoundsBeforeDisposed: Rect): Rect?

    val modifierLocalTransformState: SharedMutableTransformState?
        get() = null
}
