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
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Looper
import android.os.Message
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.collection.ScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.ui.graphics.CanvasHolder
import androidx.core.os.HandlerCompat

/**
 * Class responsible for managing the layer lifecycle to support
 * persisting of displaylist content. HWUI aggressively releases resources
 * from a displaylist if it is not used to render a single frame from a HardwareRenderer
 * instance
 */
internal class LayerManager(val canvasHolder: CanvasHolder) {

    private val activeLayerSet = mutableScatterSetOf<GraphicsLayer>()
    private val nonActiveLayerCache = WeakCache<GraphicsLayer>()

    /**
     * Create a placeholder ImageReader instance that we will use to issue a single draw call
     * for each GraphicsLayer. This placeholder draw will increase the ref count of each
     * RenderNode instance within HWUI therefore persisting it across frames as there is
     * another internal CanvasContext instance owned by the internal HwuiContext instance of
     * a Surface
     */
    private var imageReader: ImageReader? = null

    private val handler = HandlerCompat.createAsync(Looper.getMainLooper()) {
        persistLayers(activeLayerSet)
        true
    }

    fun takeFromCache(ownerId: Long): GraphicsLayer? = nonActiveLayerCache.pop()?.also {
        it.reuse(ownerId)
    }

    fun persist(layer: GraphicsLayer) {
        activeLayerSet.add(layer)
        if (!handler.hasMessages(0)) {
            // we don't run persistLayers() synchronously in order to do less work as there
            // might be a lot of new layers created during one frame. however we also want
            // to execute it as soon as possible to be able to persist the layers before
            // they discard their content. it is possible that there is some other work
            // scheduled on the main thread which is going to change what layers are drawn.
            // we use sendMessageAtFrontOfQueue() in order to be executed before that.
            handler.sendMessageAtFrontOfQueue(Message.obtain())
        }
    }

    fun release(layer: GraphicsLayer) {
        if (activeLayerSet.remove(layer)) {
            layer.discardDisplayList()
            if (SDK_INT >= M) { // L throws during RenderThread when reusing the Views.
                nonActiveLayerCache.push(layer)
            }
        }
    }

    private fun persistLayers(layers: ScatterSet<GraphicsLayer>) {
        /**
         * Create a placeholder ImageReader instance that we will use to issue a single draw call
         * for each GraphicsLayer. This placeholder draw will increase the ref count of each
         * RenderNode instance within HWUI therefore persisting it across frames as there is
         * another internal CanvasContext instance owned by the internal HwuiContext instance of
         * a Surface. This is only necessary for Android M and above.
         */
        val requiredOsVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        // On Robolectric even Surface#lockHardwareCanvas is not hardware accelerated and
        // drawing render nodes are not supported. Additionally robolectric mistakenly flags
        // surfaces as not being released even though the owning ImageReader does release the
        // surface in ImageReader#close
        // See b/340578758
        val shouldPersistLayers = requiredOsVersion && layers.isNotEmpty() && !isRobolectric
        if (shouldPersistLayers) {
            val reader = imageReader ?: ImageReader.newInstance(
                1,
                1,
                PixelFormat.RGBA_8888,
                1
            ).apply {
                // We don't care about the result, but release the buffer back to the queue
                // for subsequent renders to ensure the RenderThread is free as much as possible
                setOnImageAvailableListener({ it?.acquireLatestImage()?.close() }, handler)
            }.also { imageReader = it }
            val surface = reader.surface
            val canvas = LockHardwareCanvasHelper.lockHardwareCanvas(surface)

            canvasHolder.drawInto(canvas) {
                canvas.save()
                canvas.clipRect(0, 0, 1, 1)
                layers.forEach { layer -> layer.drawForPersistence(this) }
                canvas.restore()
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
        persistLayers(activeLayerSet)
    }

    companion object {
        private val isRobolectric = Build.FINGERPRINT.lowercase() == "robolectric"
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private object LockHardwareCanvasHelper {

    @androidx.annotation.DoNotInline
    fun lockHardwareCanvas(surface: Surface): android.graphics.Canvas =
        surface.lockHardwareCanvas()
}
