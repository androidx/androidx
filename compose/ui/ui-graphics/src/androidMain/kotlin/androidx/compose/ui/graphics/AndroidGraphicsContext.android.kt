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

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayerV29

/**
 * Create a new [GraphicsContext] with the provided [ViewGroup] to contain [View] based layers.
 *
 * @param layerContainer [ViewGroup] used to contain [View] based layers that are created by the
 * returned [GraphicsContext]
 */
fun GraphicsContext(layerContainer: ViewGroup): GraphicsContext =
    AndroidGraphicsContext(layerContainer)

private class AndroidGraphicsContext(private val ownerView: ViewGroup) : GraphicsContext {

    private val lock = Any()

    override fun createGraphicsLayer(): GraphicsLayer {
        synchronized(lock) {
            val ownerId = getUniqueDrawingId(ownerView)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                GraphicsLayer(GraphicsLayerV29(ownerId))
            } else {
                // Temporarily throw unsupported exceptions for API levels < Q as the GraphicsLayer
                // implementations for lower API levels are checked in
                throw UnsupportedOperationException(
                    "GraphicsLayer is currently only supported on Android Q"
                )
            }
        }
    }

    override fun releaseGraphicsLayer(layer: GraphicsLayer) {
        synchronized(lock) {
            layer.release()
        }
    }

    private fun getUniqueDrawingId(view: View): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UniqueDrawingIdApi29.getUniqueDrawingId(view)
        } else {
            -1
        }

    @RequiresApi(29)
    private object UniqueDrawingIdApi29 {
        @JvmStatic
        @androidx.annotation.DoNotInline
        fun getUniqueDrawingId(view: View) = view.uniqueDrawingId
    }
}
