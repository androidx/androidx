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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.toSize

internal class SharedElementEntry(
    sharedElement: SharedElement,
    boundsAnimation: BoundsAnimation,
    placeHolderSize: SharedTransitionScope.PlaceHolderSize,
    renderOnlyWhenVisible: Boolean,
    overlayClip: SharedTransitionScope.OverlayClip,
    renderInOverlayDuringTransition: Boolean,
    userState: SharedTransitionScope.SharedContentState,
    zIndex: Float,
) : LayerRenderer, RememberObserver {

    var isAttached: Boolean by mutableStateOf(false)
    override var zIndex: Float by mutableFloatStateOf(zIndex)

    var renderInOverlayDuringTransition: Boolean by mutableStateOf(renderInOverlayDuringTransition)
    var sharedElement: SharedElement by mutableStateOf(sharedElement)
    var boundsAnimation: BoundsAnimation by mutableStateOf(boundsAnimation)
    var placeHolderSize: SharedTransitionScope.PlaceHolderSize by mutableStateOf(placeHolderSize)
    var renderOnlyWhenVisible: Boolean by mutableStateOf(renderOnlyWhenVisible)
    var overlayClip: SharedTransitionScope.OverlayClip by mutableStateOf(overlayClip)
    var userState: SharedTransitionScope.SharedContentState by mutableStateOf(userState)

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

    override fun drawInOverlay(drawScope: DrawScope) {
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
                if (SharedTransitionDebug) {
                    println(
                        "SharedTransition, drawing in overlay. key = ${sharedElement.key}," +
                            " at $x, $y current size: ${currentBounds.size} " +
                            "state: $matchState"
                    )
                }
                clipPathInOverlay?.let { clipPath(it) { translate(x, y) { drawLayer(layer) } } }
                    ?: translate(x, y) { drawLayer(layer) }
            }
        }
    }

    override var parentState: SharedElementEntry? = null

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
                sharedElement.scope.isTransitionActive

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
}
