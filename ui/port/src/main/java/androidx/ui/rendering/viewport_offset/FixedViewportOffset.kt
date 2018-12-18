/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.viewport_offset

import androidx.ui.animation.Curve
import androidx.ui.core.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * A ViewportOffset implementation for the fixed pixel.
 */
internal class FixedViewportOffset(pixels: Double) : ViewportOffset() {
    companion object {
        fun zero(): FixedViewportOffset {
            return FixedViewportOffset(0.0)
        }
    }

    override var pixels: Double = pixels
        get() = field
        private set(pixels: Double) {
            field = pixels
        }

    override fun applyContentDimensions(minScrollExtent: Double, maxScrollExtent: Double): Boolean =
        true

    override fun applyViewportDimension(viewportDimension: Double): Boolean = true

    override fun correctBy(correction: Double) {
        pixels += correction
    }

    override fun jumpTo(pixels: Double) {
        // Do nothing, viewport is fixed.
    }

    override fun animateTo(to: Double, duration: Duration?, curve: Curve?): Deferred<Unit> {
        return CompletableDeferred(Unit)
    }

    override val userScrollDirection: ScrollDirection = ScrollDirection.IDLE

    override val allowImplicitScrolling: Boolean = false
}