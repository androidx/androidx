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
package androidx.compose.ui.layout

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.spatial.RelativeLayoutBounds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Registers a callback to monitor whether or not the node is inside of the viewport of the window
 * or not. Example use cases for this include, auto-playing videos in a feed, logging how long an
 * item was visible, and starting/stopping animations.
 *
 * @sample androidx.compose.ui.samples.OnVisibilityChangedAutoplaySample
 * @sample androidx.compose.ui.samples.OnVisibilityChangedAutoplayWithViewportSample
 * @sample androidx.compose.ui.samples.OnVisibilityChangedDurationLoggingSample
 * @param minDurationMs the amount of time in milliseconds that this node should be considered
 *   visible before invoking the callback with (true). Depending on your use case, it might be
 *   useful to provide a non-zero number here if it is desirable to avoid triggering the callback on
 *   elements during really fast scrolls where they went from visible to invisible in a really short
 *   amount of time.
 * @param minFractionVisible the fraction of the node which should be inside the viewport for the
 *   callback to get called with a value of true. A value of 1f means that the entire bounds of the
 *   rect need to be inside of the viewport, or that the rect fills 100% of the viewport. A value of
 *   0f means that this will get triggered as soon as a non-zero amount of pixels are inside of the
 *   viewport.
 * @param viewportBounds a reference to the bounds to use as a "viewport" with which to calculate
 *   the amount of visibility this element has *inside* of that viewport. This is most commonly used
 *   to account for UI elements such as navigation bars which are drawn on top of the content that
 *   this modifier is applied to. It is required that this be passed in to a [layoutBounds]
 *   somewhere else in order for this parameter to get used properly. If null is provided, the
 *   window of the application will be used as the viewport.
 * @param callback lambda that is invoked when the fraction of this node inside of the specified
 *   viewport crosses the [minFractionVisible]. The boolean argument passed into this lambda will be
 *   true in cases where the fraction visible is greater, and false when it is not.
 * @see onFirstVisible
 * @see onLayoutRectChanged
 * @see registerOnLayoutRectChanged
 * @see RelativeLayoutBounds.fractionVisibleIn
 * @see layoutBounds
 */
@Stable
fun Modifier.onVisibilityChanged(
    @IntRange(from = 0) minDurationMs: Long = 0,
    @FloatRange(from = 0.0, to = 1.0) minFractionVisible: Float = 1f,
    viewportBounds: LayoutBoundsHolder? = null,
    callback: (Boolean) -> Unit,
) =
    this then
        OnVisibilityChangedElement(minDurationMs, minFractionVisible, viewportBounds, callback)

private class OnVisibilityChangedElement(
    val minDurationMs: Long,
    val minFractionVisible: Float,
    val viewportBounds: LayoutBoundsHolder?,
    val callback: (Boolean) -> Unit,
) : ModifierNodeElement<OnVisibilityChangedNode>() {
    override fun create() =
        OnVisibilityChangedNode(minDurationMs, minFractionVisible, viewportBounds, callback)

    override fun update(node: OnVisibilityChangedNode) {
        node.minDurationMs = minDurationMs
        node.minFractionVisible = minFractionVisible
        node.callback = callback
        node.viewportBounds = viewportBounds
        node.forceUpdate()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onViewportVisibilityChanged"
        properties["minDurationMs"] = minDurationMs
        properties["minFractionVisible"] = minFractionVisible
        properties["viewportRef"] = viewportBounds
        properties["callback"] = callback
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as OnVisibilityChangedElement

        if (minDurationMs != other.minDurationMs) return false
        if (minFractionVisible != other.minFractionVisible) return false
        if (viewportBounds != other.viewportBounds) return false
        if (callback !== other.callback) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minDurationMs.hashCode()
        result = 31 * result + minFractionVisible.hashCode()
        result = 31 * result + (viewportBounds?.hashCode() ?: 0)
        result = 31 * result + callback.hashCode()
        return result
    }
}

internal class OnVisibilityChangedNode(
    var minDurationMs: Long,
    var minFractionVisible: Float,
    viewportBounds: LayoutBoundsHolder?,
    var callback: (Boolean) -> Unit,
) : Modifier.Node(), ObserverModifierNode {
    var viewportBounds: LayoutBoundsHolder? = viewportBounds
        set(value) {
            field = value
            updateViewport()
        }

    var handle: DelegatableNode.RegistrationHandle? = null
    var job: Job? = null
    var lastResult = false
    var firedOnce = false
    var lastBounds: RelativeLayoutBounds? = null
    var lastViewport: RelativeLayoutBounds? = null
        set(value) {
            if (field != value) {
                field = value
                forceUpdate()
            }
        }

    val rectChanged = { bounds: RelativeLayoutBounds ->
        checkVisibility(minFractionVisible, bounds, lastViewport)
    }

    fun checkVisibility(
        minFractionVisible: Float,
        bounds: RelativeLayoutBounds,
        viewport: RelativeLayoutBounds?,
    ) {
        lastBounds = bounds
        if (viewport == null && viewportBounds != null) {
            // this means that the viewport bounds state hasn't been set yet, but the user did
            // provide a viewportBounds object, meaning they do want to constrain it to something
            // other than the window. In this case, we exit early and wait until the viewport
            // bounds gets set.
            return
        }
        val fractionVisible =
            if (viewport != null) bounds.fractionVisibleIn(viewport)
            else bounds.fractionVisibleInWindow()
        val newResult = fractionVisible > minFractionVisible || fractionVisible == 1f
        if (!firedOnce || newResult != lastResult) {
            lastResult = newResult
            firedOnce = true
            startTimer()
        }
    }

    fun startTimer() {
        val minDurationMs = minDurationMs
        if (minDurationMs == 0L) triggerCallback()
        else {
            job?.cancel()
            job =
                coroutineScope.launch {
                    delay(minDurationMs)
                    triggerCallback()
                }
        }
    }

    fun triggerCallback() {
        job?.cancel()
        callback(lastResult)
    }

    fun forceUpdate() {
        val lastBounds = lastBounds
        if (lastBounds != null) {
            checkVisibility(minFractionVisible, lastBounds, lastViewport)
        }
    }

    fun fireExitIfNeeded() {
        if (lastResult && firedOnce) {
            job?.cancel()
            lastResult = false
            callback(false)
        }
    }

    override fun onReset() {
        fireExitIfNeeded()
        job?.cancel()
        job = null
        lastResult = false
        lastBounds = null
        lastViewport = null
        firedOnce = false
    }

    fun updateViewport() {
        if (viewportBounds == null) {
            lastViewport = null
            return
        }
        observeReads { lastViewport = viewportBounds?.bounds }
    }

    override fun onAttach() {
        handle?.unregister()
        handle = registerOnLayoutRectChanged(0, 0, rectChanged)
        updateViewport()
    }

    override fun onDetach() {
        handle?.unregister()
        fireExitIfNeeded()
    }

    override fun onObservedReadsChanged() {
        updateViewport()
    }
}
