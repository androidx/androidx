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
import androidx.collection.MutableObjectList
import androidx.collection.mutableObjectListOf
import androidx.collection.mutableScatterMapOf
import androidx.compose.ui.graphics.CanvasHolder
import androidx.core.os.HandlerCompat

/**
 * Class responsible for managing the layer lifecycle to support persisting of displaylist content.
 * HWUI aggressively releases resources from a displaylist if it is not used to render a single
 * frame from a HardwareRenderer instance
 */
internal class LayerManager(val canvasHolder: CanvasHolder) {

    /**
     * Create a placeholder ImageReader instance that we will use to issue a single draw call for
     * each GraphicsLayer. This placeholder draw will increase the ref count of each RenderNode
     * instance within HWUI therefore persisting it across frames as there is another internal
     * CanvasContext instance owned by the internal HwuiContext instance of a Surface
     */
    private val layerMap = mutableScatterMapOf<GraphicsLayer, ImageReader?>()

    private val handler = HandlerCompat.createAsync(Looper.getMainLooper())

    private var postponedReleaseRequests: MutableObjectList<GraphicsLayer>? = null
    private var persistenceIterationInProgress = false

    fun persist(layer: GraphicsLayer) {
        /**
         * Create a placeholder ImageReader instance that we will use to issue a single draw call
         * for each GraphicsLayer. This placeholder draw will increase the ref count of each
         * RenderNode instance within HWUI therefore persisting it across frames as there is another
         * internal CanvasContext instance owned by the internal HwuiContext instance of a Surface.
         * This is only necessary for Android M and above.
         */
        val requiredOsVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        // On Robolectric even Surface#lockHardwareCanvas is not hardware accelerated and
        // drawing render nodes are not supported. Additionally robolectric mistakenly flags
        // surfaces as not being released even though the owning ImageReader does release the
        // surface in ImageReader#close
        // See b/340578758
        val shouldPersistLayers = requiredOsVersion && !isRobolectric
        if (shouldPersistLayers) {
            val reader =
                layerMap[layer]
                    // 3 buffers is the default max buffers amount for a swapchain. The buffers are
                    // lazily allocated only if one is not available when it is requested.
                    ?: ImageReader.newInstance(1, 1, PixelFormat.RGBA_8888, 3)
                        .apply {
                            // We don't care about the result, but release the buffer back to the
                            // queue
                            // for subsequent renders to ensure the RenderThread is free as much as
                            // possible
                            setOnImageAvailableListener(
                                { it?.acquireLatestImage()?.close() },
                                handler
                            )
                        }
                        .also { layerMap[layer] = it }
            val surface = reader.surface
            val canvas = LockHardwareCanvasHelper.lockHardwareCanvas(surface)

            canvasHolder.drawInto(canvas) {
                canvas.save()
                canvas.clipRect(0, 0, 1, 1)
                layer.drawForPersistence(this)
                canvas.restore()
            }
            surface.unlockCanvasAndPost(canvas)
        } else {
            layerMap[layer] = null
        }
    }

    fun release(layer: GraphicsLayer) {
        if (!persistenceIterationInProgress) {
            if (layer.layerManager === this) {
                layer.discardDisplayList()
                layerMap.remove(layer)?.close()
            }
        } else {
            // we can't remove an item from a list, which is currently being iterated.
            // so we use a second list to remember such requests
            val requests =
                postponedReleaseRequests
                    ?: mutableObjectListOf<GraphicsLayer>().also { postponedReleaseRequests = it }
            requests.add(layer)
        }
    }

    fun closeImageReaders() {
        layerMap.forEach { layer, reader ->
            reader?.apply {
                close()
                layerMap[layer] = null
            }
        }
    }

    fun hasImageReaders(): Boolean = layerMap.any { _, reader -> reader != null }

    /**
     * Discards the corresponding ImageReader used to increment the ref count of each layer and
     * persists the current layer list creating a new ImageReader. This is useful in scenarios where
     * HWUI releases graphics resources in response to onTrimMemory often when the application is
     * backgrounded
     */
    fun updateLayerPersistence() {
        closeImageReaders()
        persistenceIterationInProgress = true
        try {
            layerMap.forEachKey { layer -> persist(layer) }
        } finally {
            persistenceIterationInProgress = false
        }
        val requests = postponedReleaseRequests
        if (requests != null && requests.isNotEmpty()) {
            requests.forEach { release(it) }
            requests.clear()
        }
    }

    companion object {
        val isRobolectric = Build.FINGERPRINT.lowercase() == "robolectric"
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private object LockHardwareCanvasHelper {

    @androidx.annotation.DoNotInline
    fun lockHardwareCanvas(surface: Surface): android.graphics.Canvas = surface.lockHardwareCanvas()
}
