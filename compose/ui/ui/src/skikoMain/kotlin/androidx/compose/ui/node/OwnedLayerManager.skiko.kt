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

package androidx.compose.ui.node

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.layer.GraphicsLayer

// TODO: It's a good candidate for moving to commonMain because AndroidComposeView reference inside
//  GraphicsLayerOwnerLayer might be limited only to this (AndroidComposeView already compatible)
internal interface OwnedLayerManager {

    // TODO: Remove it from [Owner]
    fun createLayer(
        drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
        invalidateParentLayer: () -> Unit,
        explicitLayer: GraphicsLayer?
    ): OwnedLayer = throw NotImplementedError()

    /**
     * Return [layer] to the layer cache. It can be reused in [createLayer] after this. Returns
     * `true` if it was recycled or `false` if it will be discarded.
     */
    fun recycle(layer: OwnedLayer): Boolean = false

    fun notifyLayerIsDirty(layer: OwnedLayer, isDirty: Boolean) = Unit

    /**
     * Triggers redrawing of Compose content during the next frame.
     */
    fun invalidate() = Unit

    fun voteFrameRate(frameRate: Float) = Unit
}