/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.embedded.draw

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalGraphicsContext

/**
 * A [GraphicsLayer] that is only created the first time it's needed in the draw phase, and released
 * when it leaves the composition.
 *
 * Unlike `rememberGraphicsLayer`, which allocates a layer (a RenderNode on most devices) up front,
 * this costs only a small holder for components that may, but usually don't, draw through a layer,
 * such as a DRAW_CONTENT with a group alpha or a rotating visibility animation.
 */
internal class LazyGraphicsLayer(private val graphicsContext: GraphicsContext) : RememberObserver {
    private var layer: GraphicsLayer? = null

    /** Returns the layer, creating it on first use. */
    fun obtain(): GraphicsLayer = layer ?: graphicsContext.createGraphicsLayer().also { layer = it }

    override fun onRemembered() {}

    override fun onForgotten() {
        release()
    }

    override fun onAbandoned() {
        release()
    }

    private fun release() {
        layer?.let { graphicsContext.releaseGraphicsLayer(it) }
        layer = null
    }
}

/** Remembers a [LazyGraphicsLayer] bound to the current [LocalGraphicsContext]. */
@Composable
internal fun rememberLazyGraphicsLayer(): LazyGraphicsLayer {
    val graphicsContext = LocalGraphicsContext.current
    return remember(graphicsContext) { LazyGraphicsLayer(graphicsContext) }
}
