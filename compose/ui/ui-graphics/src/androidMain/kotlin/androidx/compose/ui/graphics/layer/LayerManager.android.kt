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
import android.os.Looper
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.collection.ObjectList
import androidx.collection.mutableObjectListOf
import androidx.compose.ui.graphics.CanvasHolder
import androidx.core.os.HandlerCompat

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

    private val handler = HandlerCompat.createAsync(Looper.getMainLooper()) {
        persistLayers(layerList)
        true
    }

    fun persist(layer: GraphicsLayer) {
        if (!layerList.contains(layer)) {
            layerList.add(layer)
            if (!handler.hasMessages(0)) {
                handler.sendEmptyMessage(0)
            }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && layers.isNotEmpty()) {
            val reader = imageReader ?: ImageReader.newInstance(
                1,
                1,
                PixelFormat.RGBA_8888,
                1
            ).also { imageReader = it }
            val surface = reader.surface
            val canvas = LockHardwareCanvasHelper.lockHardwareCanvas(surface)
            // on Robolectric even this canvas is not hardware accelerated and drawing render nodes
            // are not supported
            if (canvas.isHardwareAccelerated) {
                canvasHolder.drawInto(canvas) {
                    canvas.save()
                    canvas.clipRect(0, 0, 1, 1)
                    layers.forEach { layer -> layer.drawForPersistence(this) }
                    canvas.restore()
                }
            }
            surface.unlockCanvasAndPost(canvas)
        }
    }

    fun destroy() {
        imageReader?.close()
        imageReader = null
    }

    /**
     * Discards the corresponding ImageReader used to increment the ref count of each layer
     * and persists the current layer list creating a new ImageReader. This is useful in scenarios
     * where HWUI releases graphics resources in response to onTrimMemory often when the application
     * is backgrounded
     */
    fun updateLayerPersistence() {
        destroy()
        persistLayers(layerList)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private object LockHardwareCanvasHelper {

    @androidx.annotation.DoNotInline
    fun lockHardwareCanvas(surface: Surface): android.graphics.Canvas =
        surface.lockHardwareCanvas()
}
