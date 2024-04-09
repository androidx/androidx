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

package androidx.compose.ui.graphics.layer

import android.graphics.PixelFormat
import android.media.ImageReader
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.collection.ObjectList
import androidx.collection.mutableObjectListOf
import androidx.compose.ui.graphics.CanvasHolder

/**
 * Class responsible for managing the layer lifecycle to support
 * persisting of displaylist content. HWUI aggressively releases resources
 * from a displaylist if it is not used to render a single frame from a HardwareRenderer
 * instance
 */
internal class LayerManager(val canvasHolder: CanvasHolder) {

    private val layerList = mutableObjectListOf<GraphicsLayer>()

    /**
     * Create a placeholder ImageReader instance that we will use to issue a single draw call
     * for each GraphicsLayer. This placeholder draw will increase the ref count of each
     * RenderNode instance within HWUI therefore persisting it across frames as there is
     * another internal CanvasContext instance owned by the internal HwuiContext instance of
     * a Surface
     */
    private var imageReader: ImageReader? = null

    fun persist(layer: GraphicsLayer) {
        if (!layerList.contains(layer)) {
            layerList.add(layer)
            persistLayers(layerList)
        }
    }

    fun release(layer: GraphicsLayer) {
        if (layerList.remove(layer)) {
            layer.discardDisplayList()
        }
    }

    private fun persistLayers(layers: ObjectList<GraphicsLayer>) {
        /**
         * Create a placeholder ImageReader instance that we will use to issue a single draw call
         * for each GraphicsLayer. This placeholder draw will increase the ref count of each
         * RenderNode instance within HWUI therefore persisting it across frames as there is
         * another internal CanvasContext instance owned by the internal HwuiContext instance of
         * a Surface. This is only necessary for Android M and above.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val reader = imageReader ?: ImageReader.newInstance(
                1,
                1,
                PixelFormat.RGBA_8888,
                1
            ).also { imageReader = it }
            val surface = reader.surface
            val canvas = LockHardwareCanvasHelper.lockHardwareCanvas(surface)
            canvasHolder.drawInto(canvas) {
                layers.forEach { layer -> layer.draw(this, null) }
            }
            surface.unlockCanvasAndPost(canvas)
        }
    }

    fun destroy() {
        imageReader?.close()
        imageReader = null
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private object LockHardwareCanvasHelper {

    @androidx.annotation.DoNotInline
    fun lockHardwareCanvas(surface: Surface): android.graphics.Canvas =
        surface.lockHardwareCanvas()
}
