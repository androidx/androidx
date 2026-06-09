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

package androidx.compose.ui.graphics

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.layer.GraphicsLayer
import org.jetbrains.skiko.node.RenderNode
import org.jetbrains.skiko.node.RenderNodeContext

@InternalComposeUiApi
class SkiaGraphicsContext(
    measureDrawBounds: Boolean = false,
): GraphicsContext {
    private val renderNodeContext = RenderNodeContext(
        measureDrawBounds = measureDrawBounds,
    )
    private var isClosed = false

    // Temporary workaround to disable state tracking workaround inside old internal layers
    var activeGraphicsLayersCount = 0
        private set

    fun dispose() {
        require(!isClosed) { "GraphicsContext is already closed" }
        isClosed = true
        renderNodeContext.close()
    }

    fun setLightingInfo(
        centerX: Float = Float.MIN_VALUE,
        centerY: Float = Float.MIN_VALUE,
        centerZ: Float = Float.MIN_VALUE,
        radius: Float = 0f,
        ambientShadowAlpha: Float = 0f,
        spotShadowAlpha: Float = 0f
    ) {
        require(!isClosed) { "GraphicsContext is already closed" }
        renderNodeContext.setLightingInfo(
            centerX,
            centerY,
            centerZ,
            radius,
            ambientShadowAlpha,
            spotShadowAlpha
        )
    }

    override fun createGraphicsLayer(): GraphicsLayer {
        require(!isClosed) { "GraphicsContext is already closed" }
        activeGraphicsLayersCount++
        return GraphicsLayer(
            renderNode = RenderNode(renderNodeContext)
        )
    }

    override fun releaseGraphicsLayer(layer: GraphicsLayer) {
        if (!layer.isReleased) {
            activeGraphicsLayersCount--
        }
        layer.release()
    }
}
